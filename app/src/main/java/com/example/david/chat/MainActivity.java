package com.example.david.chat;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    private Intent intent;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Parse.initialize(this, "wn2YDMZB87LYk4EaZttD8NGzM2L9W4jvSCNm8Y7w", "qtPB5ciYzQe6s884K9KnCreqArjWTXrvUJnFFhPG");
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
        intent = new Intent(getApplicationContext(), ListUsersActivity.class);
        serviceIntent = new Intent(getApplicationContext(), MessageService.class);
        class RegisterGcmTask extends AsyncTask<Void, Void, String> {
            String msg = "";
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    msg = gcm.register("335299842124");
                    Log.v("REG", msg);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    Log.v("REG", msg);
//                    Toast.makeText(getApplicationContext(),
//                            msg
//                            , Toast.LENGTH_LONG).show();
                }
                return msg;
            }
            @Override
            protected void onPostExecute(String msg) {
                intent = new Intent(getApplicationContext(), ListUsersActivity.class);
                serviceIntent = new Intent(getApplicationContext(), MessageService.class);
//                Toast.makeText(getApplicationContext(),
//                        msg
//                        , Toast.LENGTH_LONG).show();
                serviceIntent.putExtra("regId", msg);
                startActivity(intent);
                startService(serviceIntent);
            }
        }
        setContentView(R.layout.activity_main);

        Button loginButton = (Button) findViewById(R.id.loginButton);
        Button signUpButton = (Button) findViewById(R.id.signupButton);
        final EditText usernameField = (EditText) findViewById(R.id.loginUsername);
        final EditText passwordField = (EditText) findViewById(R.id.loginPassword);
        final String[] username = new String[1];
        final String[] password = new String[1];
        if (ParseUser.getCurrentUser() != null) {
            (new RegisterGcmTask()).execute();
        }
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                username[0] = usernameField.getText().toString();
                password[0] = passwordField.getText().toString();
                ParseUser.logInInBackground(username[0], password[0], new LogInCallback() {
                    public void done(ParseUser user, com.parse.ParseException e) {
                        if (user != null) {
                            (new RegisterGcmTask()).execute();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "There was an error logging in.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                username[0] = usernameField.getText().toString();
                password[0] = passwordField.getText().toString();
                ParseUser user = new ParseUser();
                user.setUsername(username[0]);
                user.setPassword(password[0]);
                user.signUpInBackground(new SignUpCallback() {
                    public void done(com.parse.ParseException e) {
                        if (e == null) {
                            (new RegisterGcmTask()).execute();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "There was an error signing up."
                                    , Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, MessageService.class));
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
