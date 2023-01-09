package android.taxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class PassangerSignIn extends AppCompatActivity {

    private FirebaseAuth auth;

    private TextInputLayout textInputEmail, textInputName, textInputPassword, textInputConfirmPassword;
    private Button regButton;
    private TextView textViewLogin, textViewTitle;

    private boolean isLoginModeActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passanger_sign_in);
        Initialization();

        //Если пользователь уже залогинен, то сразу переходит в map;
        if(auth.getCurrentUser() != null){
            startActivity(new Intent(PassangerSignIn.this, PassangerMapsActivity.class));
        }
    }

    private void Initialization(){
        auth = FirebaseAuth.getInstance();

        textInputEmail = findViewById(R.id.textInputEmail);
        textInputName = findViewById(R.id.textInputName);
        textInputPassword = findViewById(R.id.textInputPassword);
        textInputConfirmPassword = findViewById(R.id.textInputConfirmPassword);
        regButton = findViewById(R.id.regButton);
        textViewLogin = findViewById(R.id.textViewLogin);
        textViewTitle = findViewById(R.id.textViewTitle);
        isLoginModeActive = true;
    }


    public void SignInUser(View view) {
        if (isLoginModeActive){
            auth.signInWithEmailAndPassword(textInputEmail.getEditText().getText().toString().trim(),
                            textInputPassword.getEditText().getText().toString().trim())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                try {
                                    throw task.getException();
                                }
                                catch (FirebaseAuthInvalidUserException e) {
                                    Toast.makeText(PassangerSignIn.this, "Неверный email", Toast.LENGTH_SHORT).show();
                                }
                                catch (FirebaseAuthInvalidCredentialsException e) {
                                    Toast.makeText(PassangerSignIn.this, "Неверный пароль", Toast.LENGTH_SHORT).show();
                                }
                                catch (Exception e) {
                                    Toast.makeText(PassangerSignIn.this, "Ошибка входа", Toast.LENGTH_SHORT).show();
                                    Log.d("vhod", "" + e.getMessage());
                                }
                            }
                            else {
                                //Вход удачно
                                Toast.makeText(PassangerSignIn.this, "Успешный вход", Toast.LENGTH_SHORT).show();
                                FirebaseUser user = auth.getCurrentUser();
                                startActivity(new Intent(PassangerSignIn.this, PassangerMapsActivity.class));
                            }
                        }
                    });
        }
        else {
            if (!validateEmail() | !validateName() | !validatePassword()){ //| - одна черта, значит проверяются все методы
                return;                                                    //|| - две, значит проверяются по порядку
            }
            else {
                auth.createUserWithEmailAndPassword(textInputEmail.getEditText().getText().toString().trim(),
                                textInputPassword.getEditText().getText().toString().trim())
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(Task<AuthResult> task) {
                                if (!task.isSuccessful()) {
                                    try {
                                        throw task.getException();
                                    }
                                    catch (FirebaseAuthUserCollisionException e) {
                                        Toast.makeText(PassangerSignIn.this, "Данный email уже существует", Toast.LENGTH_SHORT).show();
                                    }
                                    catch (FirebaseAuthWeakPasswordException e) {
                                        Toast.makeText(PassangerSignIn.this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
                                    }
                                    catch (Exception e) {
                                        Toast.makeText(PassangerSignIn.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                                        Log.d("reg", "" + e.getMessage());
                                    }
                                }
                                else {
                                    // Регистрация прошла успешна
                                    Toast.makeText(PassangerSignIn.this, "Регистрация прошла успешна", Toast.LENGTH_SHORT).show();
                                    FirebaseUser user = auth.getCurrentUser();
                                    startActivity(new Intent(PassangerSignIn.this, PassangerMapsActivity.class));
                                }
                            }
                        });
            }
        }
    }

    public void ChangeLogin(View view) {
        if (isLoginModeActive){
            isLoginModeActive = false;
            textViewTitle.setText("Регистрация пассажира");
            textInputName.setVisibility(View.VISIBLE);
            textInputConfirmPassword.setVisibility(View.VISIBLE);
            regButton.setText("Зарегистрироваться");
            textViewLogin.setText("Войти в аккаунт");
        }
        else {
            isLoginModeActive = true;
            textViewTitle.setText("Аудентификация пассажира");
            textInputName.setVisibility(View.GONE);
            textInputConfirmPassword.setVisibility(View.GONE);
            regButton.setText("Вход");
            textViewLogin.setText("Зарегистрироваться");
        }
    }

    private boolean validateEmail(){

        String emailInput = textInputEmail.getEditText().getText().toString().trim();
        if (emailInput.isEmpty()){
            textInputEmail.setError("Введите email");
            return false;
        }
        else {
            textInputEmail.setError("");
            return true;
        }
    }

    private boolean validateName(){

        String nameInput = textInputName.getEditText().getText().toString().trim();
        if (nameInput.isEmpty()){
            textInputName.setError("Введите имя");
            return false;
        }
        else if (nameInput.length() > 10){
            textInputName.setError("Имя слишком длинное");
            return false;
        }
        else {
            textInputName.setError("");
            return true;
        }
    }

    private boolean validatePassword(){
        String passwordInput = textInputPassword.getEditText().getText().toString().trim();
        String confirmPasswordInput = textInputConfirmPassword.getEditText().getText().toString().trim();
        if (passwordInput.isEmpty()){
            textInputPassword.setError("Введите пароль");
            return false;
        }
        else if (!passwordInput.equals(confirmPasswordInput)){ //Если пароли не равны, то будет false, значит ставим !, чтобы было true и мы попали в эту ветку
            textInputPassword.setError("Пароли не совпадают");
            return false;
        }
        else {
            textInputPassword.setError("");
            return true;
        }
    }

}