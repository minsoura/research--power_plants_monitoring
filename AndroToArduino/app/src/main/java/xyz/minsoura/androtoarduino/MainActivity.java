package xyz.minsoura.androtoarduino;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
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

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    Button clearButton, stopButton, displayButton, batteryButton;
    TextView textView;

    Button startButton;
    Button serialButton;
    String trigger ="on!";

    UsbManager usbManager;
    UsbDevice device;
     UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    Dialog dialog;
    EditText dialogSetNumber;
    TextView dialogUpdate;
    TextView dialogCancel;



    String dataToArduino = "default";
    String port, ip;
    String TAG ="SMSO";
    Context mContext;

    ArrayList<String> dataValuesArray;
    String variableNames ="ID,v1,v2,v3,v4,v5,v6,v7,v8,v9,v10,v11,v12,v13,v14,v15,v16,time,date";

    static String targetMobileNumber;
    static String data;
    static Integer batteryLevel;
    static Boolean batteryWarningMessageSent = false;
    static Boolean batterySafeMessageSent = false;

    IntentFilter filter = new IntentFilter();
    private BroadcastReceiver broadcastReceiver;


    private BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
             if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())){
                Toast.makeText(getBaseContext(), "message received", Toast.LENGTH_LONG).show();
                for(SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)){
                    dataToArduino = smsMessage.getMessageBody();
                }
                Toast.makeText(context, "*RECEiVED! " + dataToArduino +"? *", Toast.LENGTH_LONG).show();
                mContext = context;
                try{
                    //String dataFromArduinoConverted = dataConversionHexToDec(dataToArduino, variableNames);
                        if(!dataToArduino.contains("{") && !dataToArduino.contains("{")){
                            write(dataToArduino.getBytes());
                            Toast.makeText(context, "trigger", Toast.LENGTH_LONG).show();
                            Log.d(TAG, "SIGNAL ACCEPTED");
                        }

                    //Toast.makeText(context, dataFromArduinoConverted, Toast.LENGTH_LONG).show();
                }catch (Exception e){
                    Log.d(TAG, "unknown field detected",e);
                }

            }
        }
    };


    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {

            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            if(batteryLevel == 15){
                batteryWarningMessageSent= false;//리셋이 가능하도록
            }
            if(batteryLevel == 35){
                batterySafeMessageSent = false;
            }
            if(batteryLevel <= 10 && !batteryWarningMessageSent){
                sendSMS(targetMobileNumber, "{\"status\":\"warning\", \"batteryLevel\":\""+batteryLevel + "\"}");
                batteryWarningMessageSent = true;

            }else if(40 <= batteryLevel && !batterySafeMessageSent){
                sendSMS(targetMobileNumber, "{\"status\":\"green\", \"batteryLevel\":\""+batteryLevel + "\"}");
                batterySafeMessageSent = true;
            }


        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new Dialog(MainActivity.this);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_set_destination);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        dialogSetNumber = (EditText) dialog.findViewById(R.id.targetNumber);
        dialogUpdate = (TextView) dialog.findViewById(R.id.setNumber);
        dialogCancel = (TextView) dialog.findViewById(R.id.setCancel);

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

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);

        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        //TODO: erase the following line after debugging
        serialButton =(Button) findViewById(R.id.serialButton);
        serialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              //  String msgs ="{\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\",\"1\"}";
                String msgs ="0000000000000000";
            sendSMS("01025899276",msgs);
            }
        });

        textView = (TextView) findViewById(R.id.textView);
        displayButton =(Button) findViewById(R.id.displayButton);
        batteryButton =(Button)findViewById(R.id.checkBatteryButton);

        setUiEnabled(false);
       // IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);

      //  Intent serviceIntent  = NotificationIntentService.createIntentStartNotificationService(getApplicationContext());

        //startService(serviceIntent);

        ;
           displayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
           String testString = readFromFile(getApplicationContext());
           String refinedString = testString.replaceAll(",$", "");
                String startChar ="[";
                String endChar ="]";
                String jsonString = startChar + refinedString + endChar;
                Toast.makeText(getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();

            }
        });

        batteryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Battery Level:" + batteryLevel + "percent", Toast.LENGTH_LONG).show();
            }
        });




        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        this.registerReceiver(this.smsReceiver, new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
        broadcastRegisterFunction();
        onClickStart(startButton);

    }


    public String bringTargetMobileNumber(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        return sharedPreferences.getString("targetNumber", "01025899276");
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


    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {



            try {


                data = new String(arg0, "UTF-8");


                if(data.contains("s")){
                    data = data.replace("s", "");
                    writeToFile(data, getApplicationContext());
                    readTextFileAndSendSMS();

                }else{
                    data = data.replace("s", "");
                    writeToFile(data, getApplicationContext());

                }


                tvAppend(textView, data);



            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    };

    public void readTextFileAndSendSMS(){



        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                String testString = readFromFile(getApplicationContext());
                String refinedString = testString.replaceAll(",$", "");
                refinedString = testString.replace("}", "");

                SimpleDateFormat formatter2 = new SimpleDateFormat("hh:mm", Locale.KOREA);
                Date timeSaved = new Date();
                String timeStringSaved = formatter2.format(timeSaved);


                String jsonString = refinedString + ",\"" + timeStringSaved + "\"}";


                ///TODO:핸드폰 번호 변경

                sendSMS(targetMobileNumber, jsonString);
                clearFileContent(getApplicationContext());


                Toast.makeText(getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();


            }
        });








    }


    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);

        stopButton.setEnabled(bool);
        textView.setEnabled(bool);


    }
    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("IDIM.txt", Context.MODE_APPEND));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private void clearFileContent(Context context) {

        String clearString ="";
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("IDIM.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(clearString);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    private String readFromFile(Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("IDIM.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
    public void onClickStart(View view) {



        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
               // Toast.makeText(getApplicationContext(),Integer.toString(deviceVID), Toast.LENGTH_LONG).show();
                if (deviceVID == 10755)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    Toast.makeText(getApplicationContext(),"DEVICE DOCKING...", Toast.LENGTH_LONG).show();
                    setUiEnabled(true);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }else{
            Toast.makeText(getApplicationContext(),"DEVICE EMPTY", Toast.LENGTH_LONG).show();
        }


    }



    public void onClickStop(View view) {
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        serialPort.close();
        tvAppend(textView, "\nSerial Connection Closed! \n");


    }





    public void onClickClear(View view) {

        textView.setText(" ");
        clearFileContent(getApplicationContext());

    }




    private void tvAppend(TextView tv, String text) {


        final TextView ftv = tv;
        final String ftext = text;


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ftv.append(ftext);


                //   sendSMS("01025899276", ftext);
                // Toast.makeText(getApplicationContext(), getFilesDir().getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        unregisterReceiver(this.broadcastReceiver);

    }

    @Override
    public void onPause(){
        super.onPause();


    }
    public void sendSMS(String phoneNo, String msg){
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);

            Toast.makeText(getApplicationContext(), "Message Sent",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }



    @Override
    public void onResume(){
        super.onResume();
        broadcastRegisterFunction();
    }
    public void  broadcastRegisterFunction(){
        broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                    boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        connection = usbManager.openDevice(device);
                        serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);

                        if (serialPort != null) {

                            if (serialPort.open()) { //Set Serial Connection Parameters.

                                setUiEnabled(true);
                                serialPort.setBaudRate(115200);
                                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                                serialPort.read(mCallback);
                                tvAppend(textView,"Waiting for an Incoming Data..\n");

                            } else {
                                Log.d("SERIAL", "PORT NOT OPEN");
                                Toast.makeText(getApplicationContext(),"PORT NOT OPEN", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Log.d("SERIAL", "PORT IS NULL");
                            Toast.makeText(getApplicationContext(),"PORT IS NULL", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d("SERIAL", "PERM NOT GRANTED");
                        Toast.makeText(getApplicationContext(),"PERM NOT GRANTED", Toast.LENGTH_LONG).show();
                    }
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                    onClickStart(startButton);
                } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                    serialPort.close();
                    finish();
                  }
            }
        };
        registerReceiver(broadcastReceiver, filter);
    }

    public void write(byte[] data) {

        if (serialPort != null)
            serialPort.write(data);
    }



    public String dataConversionHexToDec(String hexDataJsonArray,String variableNames){


        String hexDataString = hexDataJsonArray.replace("{", "");
        hexDataString = hexDataString.replace("}","");
        hexDataString = hexDataString.replace("\"","");
        Log.d(TAG, hexDataString);
        String[] hexDataStringArray = hexDataString.split(",");
        Integer beforeStringValue;
        String stringValue;
        Log.d(TAG, String.valueOf(hexDataStringArray.length));
        dataValuesArray = new ArrayList<String>();
        dataValuesArray.clear();

        for(int i=0; i<hexDataStringArray.length; i++){


            beforeStringValue = Integer.parseInt(hexDataStringArray[i],16);
            ///TODO: use the following line for debugging
            // stringValue = String.valueOf(beforeStringValue + randomNumberGenerator(i*79-i+1,1));
            ///TODO: use the following line for production
            stringValue = String.valueOf(beforeStringValue);
            dataValuesArray.add(stringValue);



            Log.d(TAG, stringValue);
        }

        String parenthesisLeft="{";
        String parenthesisRight="}";
        String stringDataContents;
        String doubleQuote="\"";
        String commaMark=",";
        StringBuilder str = new StringBuilder();

        for(int i=0; i< dataValuesArray.size(); i++){
            str.append(doubleQuote);
            str.append(dataValuesArray.get(i));
            str.append(doubleQuote);
            if(i!= 15){
                str.append(commaMark);
            }
            Log.d(TAG,"dataValuesArray" + i +  " : " + dataValuesArray.get(i));
        }
        stringDataContents = parenthesisLeft + str.toString() + parenthesisRight;
        Log.d(TAG, stringDataContents);
        return stringDataContents;
    }





}
