package android.taxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FirebaseAuth auth;
    private FirebaseUser nowUser;

    private static final int CHECK_SETTINGS_CODE = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;

    private FusedLocationProviderClient fusedLocationClient; //Создание клиента службы определения местоположения
    private SettingsClient settingsClient; //Для доступа к настройкам
    private LocationRequest locationRequest; //Для сохранения данных запроса FusedLocationAPI
    private LocationSettingsRequest locationSettingsRequest; //Для определения настроек девайся пользователя в данный момент
    private LocationCallback locationCallback; //Для событий определния местоположения
    private Location nowLocation; //Тут хранятся долгота и широта местонахожения объекта

    private boolean isLocationActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);
        Initialization();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buildLocationRequest();
        buildLocationCallBack();
        buildLocationSettingsRequest();
        startLocation();

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Координаты маркера на карте
        if (nowLocation != null){
            LatLng driverLocation = new LatLng(nowLocation.getLatitude(), nowLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(driverLocation).title("Водитель"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation));
        }
    }

    private void Initialization() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        auth = FirebaseAuth.getInstance();
        nowUser = auth.getCurrentUser();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLocationActive && checkLocationPermission()){
            startLocation();
        }
        else if (!checkLocationPermission()){
            requestLocationPermission();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocation();
    }

    private void exitDriver(){
        String driverUserId = nowUser.getUid();
        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference().child("Drivers");
        //Удаляет локацию водителя в FB
        GeoFire geoFire = new GeoFire(drivers);
        geoFire.removeLocation(driverUserId);

        Intent intent = new Intent(DriverMapsActivity.this, ChoiceModeActivity.class);
        //Строки ниже для того, чтобы пользователь, при нажатии на стрелку назад, не возвращался в map
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    //Создать запрос геолокации
    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000); //Интервал зпроса , в мс
        locationRequest.setFastestInterval(1000); //Частота обновления данных
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //С высокой точностью
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                nowLocation = locationResult.getLastLocation(); //Получаем текущее местоположение
                updateLocationUI();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void startLocation() {
        isLocationActive = true;

        settingsClient.checkLocationSettings(locationSettingsRequest).
                addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        //Вначале идёт проверка: дал ли пользователь разрешение на использование геопозиции
                        if (ActivityCompat.checkSelfPermission(DriverMapsActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(DriverMapsActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        fusedLocationClient.requestLocationUpdates(locationRequest,
                                locationCallback, Looper.myLooper());

                        updateLocationUI(); //Если разрешение дано, то получаем геопозицию
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode){
                            case LocationSettingsStatusCodes
                                    .RESOLUTION_REQUIRED: //Если пользователь не дал разрешение, то уведомляем его об этом
                                try {
                                    ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                    resolvableApiException.startResolutionForResult(DriverMapsActivity.this,
                                            CHECK_SETTINGS_CODE); //Эту const сами вводим какую захотим (см. вверху в объявлении переменных)
                                } catch (IntentSender.SendIntentException sie){
                                    sie.printStackTrace();
                                }
                                break;

                            case LocationSettingsStatusCodes
                                    .SETTINGS_CHANGE_UNAVAILABLE: //Когда невозможно из приложения эти настройки
                                // и нужно устанавливать их вручную, поэтому необходимо уведомить об этом пользователя
                                String message = "Дайте разрешение местоположения в настройках телефона";
                                Toast.makeText(DriverMapsActivity.this, message, Toast.LENGTH_LONG).show();
                                isLocationActive = false;
                        }
                        updateLocationUI();
                    }
                });
    }

    private void stopLocation(){
        if (!isLocationActive) return;
        else fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        isLocationActive = false;
                    }
                });
    }

    @Override //Переопредляем данный метод, т.к. в методе выше используем "resolvableApiException.startResolutionForResult"
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHECK_SETTINGS_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:  //Пользователь дал согласие на переключение настроек своего девайса
                        Log.d("setLoc", "Пользователь дал согласие на обработку геолокации");
                        startLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.d("setLoc", "Пользователь не дал согласие на обработку геолокации");
                        isLocationActive = false;
                        updateLocationUI();
                        break;
                }
        }
    }

    //Обновление долготы/широты и времени
    private void updateLocationUI(){
        if (nowLocation != null){
            LatLng driverLocation = new LatLng(nowLocation.getLatitude(), nowLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(driverLocation).title("Водитель"));
            //mMap.animateCamera(CameraUpdateFactory.zoomTo(4)); //Увеличение масштаба карты
            mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation));

            String driverUserId = nowUser.getUid();
            DatabaseReference drivers = FirebaseDatabase.getInstance().getReference().child("Drivers");
            //Добавляет локацию водителя в FB, при этом обновляет её каждый раз при смещении
            GeoFire geoFire = new GeoFire(drivers);
            geoFire.setLocation(driverUserId, new GeoLocation(nowLocation.getLatitude(), nowLocation.getLongitude()));
        }
    }

    //Если разрешение получено, то возвращаем true, иначе false
    private boolean checkLocationPermission(){
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    //Должны ли мы предоставить пользователю объяснение, почему требуем данное разрещшение на геолокацию
    private void requestLocationPermission(){
        //Если true (т.е. в первый раз пользовател отклонил разрешение),то показываем доп разъяснение пользователю
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationale) {
            //Показываем сообщение при помощи класса: com.google.android.material:material:1.6.1 (в gradle)
            showSnackBar("Доступ к геолокации необходим для работы приложени", "OK",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(DriverMapsActivity.this,
                                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION);
                        }
                    }
            );
        }
        else {
            ActivityCompat.requestPermissions(DriverMapsActivity.this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override //После того, как ответ на запрос разрешение будет получен, его нужно обработать в методе:
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION){
            if (grantResults.length <= 0) Log.d("requesstResult", "Запрос отклонён");
            else if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if (isLocationActive) startLocation();
                else showSnackBar("Включи геолокацию в настройках", "Settings",
                        new View.OnClickListener() {
                            @Override //Переходим через intent в настройки телефона
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    //Параметры: 1. Текст, который будет показываться, 2. Текст на кнопке подтвержения (ОК), 3. Кнопка
    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener){
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE)
                .setAction(action, listener).show();
    }

    public void Exit(View view) {
        auth.signOut();
        exitDriver();
    }
}