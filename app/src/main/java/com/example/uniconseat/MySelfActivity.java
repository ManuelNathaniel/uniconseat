package com.example.uniconseat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MySelfActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_self);

        final EditText nickname = findViewById(R.id.edit_nickname);
        final EditText motto = findViewById(R.id.edit_motto);
        final EditText per_sign = findViewById(R.id.edit_per_sign);
        Button personbutton = findViewById(R.id.personButton);
        Button defaultbutton = findViewById(R.id.personDefault);

        NavigationView navigationView = findViewById(R.id.nav_view);
        View nav_header = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        final TextView textNickname = nav_header.findViewById(R.id.headerTitle);
        final TextView textMotto = nav_header.findViewById(R.id.textView);
        final TextView textPersign = nav_header.findViewById(R.id.personal_signature);

        personbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nicknameContent = nickname.getText().toString();
                String mottoContent = motto.getText().toString();
                String persignContent = per_sign.getText().toString();
                SharedPreferences sharedPreferences = getSharedPreferences("CustomSignature",MODE_PRIVATE);
                SharedPreferences.Editor personEditor = sharedPreferences.edit();
                personEditor.putString("nickname",nicknameContent);
                personEditor.putString("motto",mottoContent);
                personEditor.putString("per_sign",persignContent);
                personEditor.commit();
                textNickname.setText(nicknameContent);
                textMotto.setText(mottoContent);
                textPersign.setText(persignContent);
                Intent intentMySelf = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intentMySelf);
                finish();
            }
        });
        defaultbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nicknameContent = "Manuel Nathaniel";
                String mottoContent = "厚德 博学 载物";
                String persignContent = "天行健，君子以自强不息；地势坤，君子以厚德载物。";
                SharedPreferences sharedPreferences = getSharedPreferences("CustomSignature",MODE_PRIVATE);
                SharedPreferences.Editor personEditor = sharedPreferences.edit();
                personEditor.putString("nickname",nicknameContent);
                personEditor.putString("motto",mottoContent);
                personEditor.putString("per_sign",persignContent);
                personEditor.commit();
                textNickname.setText(nicknameContent);
                textMotto.setText(mottoContent);
                textPersign.setText(persignContent);
                Intent intentMySelf = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intentMySelf);
                finish();
            }
        });
    }
    @Override
    public void onBackPressed(){
        if (MainActivity.mainActivityStart){
            super.onBackPressed();
        }else {
            Intent intentMain= new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intentMain);
        }
    }
}
