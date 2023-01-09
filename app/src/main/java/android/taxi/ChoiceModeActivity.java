package android.taxi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;

public class ChoiceModeActivity extends AppCompatActivity {

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choice_mode);
    }

    public void PassangerSignIn(View view) {
        startActivity(new Intent(ChoiceModeActivity.this, PassangerSignIn.class));
    }

    public void DriverSignIn(View view) {
        startActivity(new Intent(ChoiceModeActivity.this, DriverSignIn.class));
    }
}