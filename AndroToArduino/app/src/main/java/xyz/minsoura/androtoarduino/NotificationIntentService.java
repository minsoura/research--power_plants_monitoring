package xyz.minsoura.androtoarduino;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by min on 2016-02-15.
 */



public class NotificationIntentService extends Service {
    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";

    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    static String data;

    UserDecisionTask decisionTask;
    static String thisCurrentValue;

    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_START = "ACTION_START";
    private static final String ACTION_DELETE = "ACTION_DELETE";



    public static Intent createIntentStartNotificationService(Context context) {
        Intent intent = new Intent(context, NotificationIntentService.class);
        intent.setAction(ACTION_START);



        return intent;
    }

    public static Intent createIntentDeleteNotification(Context context) {
        Intent intent = new Intent(context, NotificationIntentService.class);
        intent.setAction(ACTION_DELETE);
        return intent;

    }




    public IBinder onBind(Intent intent) {
        return null;
        }
    @Override
     public void onCreate() {
         Toast.makeText(this, "Service Created", Toast.LENGTH_LONG).show();
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
      // filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

      }
   @Override
    public void onStart(Intent intent, int startid) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
       Log.e(getClass().getSimpleName(), "onHandleIntent, started handling a notification event");
       try {
           String action = intent.getAction();
           if (ACTION_START.equals(action)) {
               usbDeviceConnected();
               processStartNotification();
           }

           if (ACTION_DELETE.equals(action)) {
               processDeleteNotification(intent);
           }
       } finally {
           //WakefulBroadcastReceiver.completeWakefulIntent(intent);
       }

        }


   @Override
    public void onDestroy() {
      Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();

   }


    private void processDeleteNotification(Intent intent) {
        // Log something?
    }

    private void processStartNotification() {
        Log.e(getClass().getSimpleName(), "inside processStartNotification");




/*
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        Date dateSaved = new Date();
        String dateStringSaved = formatter.format(dateSaved);


        SimpleDateFormat formatter2 = new SimpleDateFormat("hh:mm", Locale.KOREA);
        Date timeSaved = new Date();
        String timeStringSaved = formatter2.format(timeSaved);


        sendDecisionVersion(dateStringSaved, timeStringSaved, data);

*/


    }






    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {



            try {
                data = new String(arg0, "UTF-8");
                data.trim();


              //  Toast.makeText(getApplicationContext(), "inside Call back", Toast.LENGTH_LONG).show();

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
                    Date dateSaved = new Date();
                    String dateStringSaved = formatter.format(dateSaved);


                    SimpleDateFormat formatter2 = new SimpleDateFormat("hh:mm", Locale.KOREA);
                    Date timeSaved = new Date();
                    String timeStringSaved = formatter2.format(timeSaved);

//TODO: we need a function that collects all the incoming  data and send them to the web server all at once;
                //TODO; we could use JSON or XML as data delivery format
                //TODO; How will the data be sent form the arduino?
                //TODO: another function which might send the data periodically;  by the mobile side or by the arduino side?
                //TODO: Catch all the error ;
                if(NetworkServiceFunction()){

                    sendDecisionVersion(dateStringSaved, timeStringSaved, data);

                }else{

                     sendSMS("01025899276",data);


                }














            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };
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

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
         if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {


             HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
             if (!usbDevices.isEmpty()) {
                 boolean keep = true;
                 for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                     device = entry.getValue();

                 }
                 connection = usbManager.openDevice(device);
                 serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                 if (serialPort != null) {
                     if (serialPort.open()) { //Set Serial Connection Parameters.
                         Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                         serialPort.setBaudRate(9600);
                         serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                         serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                         serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                         serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                         serialPort.read(mCallback);



                     } else {
                         Log.d("SERIAL", "PORT NOT OPEN");
                     }
                 } else {
                     Log.d("SERIAL", "PORT IS NULL");
                 }
             }





            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                serialPort.close();

            }
        }

        ;
    };

    public boolean NetworkServiceFunction(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        Boolean checker = false;
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to wifi
                 checker = true;

            }
        } else {

            checker = false;
        }
        return checker;
    }
    public void usbDeviceConnected(){

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();

            }   connection = usbManager.openDevice(device);
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.open()) { //Set Serial Connection Parameters.
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
                    serialPort.setBaudRate(9600);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);



                } else {
                    Log.d("SERIAL", "PORT NOT OPEN");
                }
            } else {
                Log.d("SERIAL", "PORT IS NULL");
            }

        }





    }

    public void onClickStart() {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 9025)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }


    }



















    public void sendDecisionVersion(String Date, String Time, String currentValue) {


        decisionTask = new UserDecisionTask(Date, Time, currentValue);
        decisionTask.execute();


    }


    public class UserDecisionTask extends AsyncTask<Void, Void, String> {
        private final String mDate;
        private final String mTime;
        private final String mCurrentValue;


        Boolean checker = true;

        UserDecisionTask(String Date, String Time, String Current) {
            mDate = Date;
            mTime = Time;
            mCurrentValue = Current;

        }

        @Override
        protected void onCancelled() {
            decisionTask = null;


        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //showProgress(true);
        }

        @Override
        protected String doInBackground(Void... params) {
            if (!checker) {
                Log.e("Checker", "Checker is Working");
                return null;

            }
            String URL = "";


            URL = "http://minsoura.xyz/powerMonitoringWeb.php";


            HashMap<String, String> SendSet = new HashMap<>();
            SendSet.put("dateStamp", mDate);
            SendSet.put("timeStamp", mTime);
            SendSet.put("currentValue", mCurrentValue);



            try {
                requestHandler DeliverHandler = new requestHandler();
                String result = DeliverHandler.sendPostRequest(URL, SendSet);
                if (!checker) {
                    Log.e("Checker", "Checker is Working");
                    return null;

                }
                return result;

            } catch (Exception e) {

                return null;
            }


        }

        @Override
        protected void onPostExecute(final String receivedLine) {
            super.onPostExecute(receivedLine);
            if (isCancelled()) {
                decisionTask = null;
            }

            // showProgress(false);
            if (receivedLine.equals("yes")) {

                Toast.makeText(getApplicationContext(),"sent",Toast.LENGTH_LONG);


                //TODO: I could make a intent that delivers retrieved data from the DB such as the ones for the USER PROFILE;

            } else if (receivedLine.equals("no")) {

                Toast.makeText(getApplicationContext(),"error",Toast.LENGTH_LONG);


            }

            checker = false;
        }


    }
}
