package me.movely.skimmilk;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class MainActivity extends AppCompatActivity {

    EditText portValue, ipValue, testValue;
    Button saveButton, smsButton, clearButton;
    String port, ip, testDummy;

    Dialog dialog;
    EditText dialogSetNumber;
    TextView dialogUpdate;
    TextView dialogCancel;



    static String targetMobileNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        componentInitialization();
        originConfiguration();
        listenerRegistration();


    }
    public void originConfiguration(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        port = sharedPreferences.getString("portValue", "default");
        ip =  sharedPreferences.getString("ipValue", "default");
        testDummy =  sharedPreferences.getString("testValue", "default");
        portValue.setText(port);
        ipValue.setText(ip);

    }
    public void componentInitialization(){
        portValue =(EditText) findViewById(R.id.portValue);
        ipValue =(EditText) findViewById(R.id.ipValue);
        testValue =(EditText) findViewById(R.id.testValue);

        saveButton =(Button) findViewById(R.id.buttonSave);
        smsButton =(Button) findViewById(R.id.buttonSMS);
        clearButton =(Button) findViewById(R.id.buttonClear);


        dialog = new Dialog(MainActivity.this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_set_destination);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        dialogSetNumber = (EditText) dialog.findViewById(R.id.targetNumber);
        dialogUpdate = (TextView) dialog.findViewById(R.id.setNumber);
        dialogCancel = (TextView) dialog.findViewById(R.id.setCancel);


    }

    public void listenerRegistration(){
        smsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                testDummy = testValue.getText().toString();
                sendSMS(bringTargetMobileNumber(), testDummy);
            }
        });



        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                port = portValue.getText().toString();
                ip = ipValue.getText().toString();
                testDummy = testValue.getText().toString();

                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final SharedPreferences.Editor editor = pref.edit();
                editor.putString("portValue", port);
                editor.putString("ipValue", ip);
                editor.putString("testValue", testDummy);
                editor.apply();
                Toast.makeText(getApplicationContext(),"successfully changed",Toast.LENGTH_SHORT).show();

            }
        });


        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                portValue.setText("");
                ipValue.setText("");
                Toast.makeText(getApplicationContext(),"SETTINGS CLEAR", Toast.LENGTH_SHORT).show();

            }
        });




        dialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialogUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String numberValue = dialogSetNumber.getText().toString();
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final SharedPreferences.Editor editor = pref.edit();
                editor.putString("targetNumber", numberValue);
                editor.apply();
                Toast.makeText(getApplicationContext(),"Target number has been successfully changed",Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });


        targetMobileNumber = bringTargetMobileNumber();

        dialogSetNumber.setText(targetMobileNumber);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);



        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        int id = item.getItemId();

        switch (id){
            case R.id.menu_change_number:

                dialog.show();
                return true;
        }

        return super.onOptionsItemSelected(item);

    };

    public void sendSMS(String phoneNo, String msg){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);

            Toast.makeText(getApplicationContext(), "Message Sent",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    public String bringTargetMobileNumber(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        return sharedPreferences.getString("targetNumber", "01025899276");
    }



}
