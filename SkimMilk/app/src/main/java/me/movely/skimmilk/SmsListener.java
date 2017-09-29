package me.movely.skimmilk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * Created by min on 2017-01-17.
 */
public class SmsListener extends BroadcastReceiver{

    String dataFromArduino = "default";
    String port, ip;
    String TAG ="SMSO";
    Context mContext;
    UploadTask uploadTask;
    ArrayList<String> dataValuesArray;
    String variableNames ="ID,v1,v2,v3,v4,v5,v6,v7,v8,v9,v10,v11,v12,v13,v14,v15,v16,v17,time,date";

    @Override
    public void onReceive(Context context,Intent intent){
        if(Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())){
            Log.d(TAG, "MESSAGE RECEIVED");
            dataFromArduino="";
            for(SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)){
               dataFromArduino += smsMessage.getMessageBody();
            }
             Toast.makeText(context, "*" + dataFromArduino +" *", Toast.LENGTH_LONG).show();
             mContext = context;
            try{
                String dataFromArduinoConverted = dataConversionHexToDec(dataFromArduino, variableNames);
                uploadDataToServer(dataFromArduinoConverted);
            }catch (Exception e){
                Log.d(TAG, "unknown field detected",e);
            }

        }
    }
    public String dataConversionHexToDec(String hexDataJsonArray,String variableNames){

        String[] dataNamesArray = variableNames.split(",");
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
            if(1<=i && i<=hexDataStringArray.length-2){

                beforeStringValue = Integer.parseInt(hexDataStringArray[i],16);
                ///TODO: use the following line for debugging
              //  stringValue = String.valueOf(beforeStringValue + randomNumberGenerator(i*79-i+1,1));
                ///TODO: use the following line for production
                stringValue = String.valueOf(beforeStringValue);
                dataValuesArray.add(stringValue);
            }else if(i==0){
                stringValue = hexDataStringArray[i];
                dataValuesArray.add(stringValue);
            }else{
                stringValue = hexDataStringArray[i];
                dataValuesArray.add(stringValue);
            }
            Log.d(TAG, stringValue);
        }
        dataValuesArray.add(decideDate());

        String parenthesisLeft="{";
        String parenthesisRight="}";
        String stringDataContents;
        String colonMark =":";
        String doubleQuote="\"";
        String commaMark=",";
        StringBuilder str = new StringBuilder();


        for(int i=0; i< dataValuesArray.size(); i++){

            str.append(doubleQuote);
            str.append(dataNamesArray[i]);
            str.append(doubleQuote);
            str.append(colonMark);
            str.append(doubleQuote);
            str.append(dataValuesArray.get(i));
            str.append(doubleQuote);
            if(i!= dataValuesArray.size()-1){
                str.append(commaMark);
            }
            Log.d(TAG,"dataValuesArray" + i +  " : " + dataValuesArray.get(i));
        }
            stringDataContents = parenthesisLeft + str.toString() + parenthesisRight;
            Log.d(TAG, stringDataContents);
        return stringDataContents;
    }

    private String decideDate(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        Date dateSaved = new Date();
        String dateStringSaved = formatter.format(dateSaved);
        return dateStringSaved;
    }

    public int randomNumberGenerator(int max, int min){
        Random number = new Random();
        int i = number.nextInt(max - min +1) + min;
        return i;

    }
    public void udpDataFunction(String rawMessage){
        try {
            originConfiguration();
            Log.d(TAG, "<udpDataFunction> " + "Connecting To.." + ip + " at Port " + port);
            DatagramSocket datagramSocket = new DatagramSocket();
            InetAddress local = InetAddress.getByName(ip);
            int dataLength = rawMessage.length();
            byte[] message = rawMessage.getBytes();
            int portInt = Integer.parseInt(port);
            DatagramPacket datagramPacket = new DatagramPacket(message, dataLength, local, portInt);
            datagramSocket.send(datagramPacket);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "UDP NOT SENT", e);
        }
    };
    public void originConfiguration(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        port = sharedPreferences.getString("portValue", "default");
        ip =  sharedPreferences.getString("ipValue", "default");
    }
    public void uploadDataToServer(String Data) {
        uploadTask = new UploadTask(Data);
        uploadTask.execute();
    }
    public class UploadTask extends AsyncTask<Void, Void, String> {
        private final String mData;
        Boolean checker = true;
        UploadTask(String Data) {
            mData = Data;
        }
        @Override
        protected void onCancelled() {
            uploadTask = null;
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
            try {
                  udpDataFunction(mData);
                return "sent";
            } catch (Exception e) {
                Log.d(TAG, "INSIDE ASYNCTASK doINBACKGROUND: ", e);
                return "error";
            }
        }
        @Override
        protected void onPostExecute(final String receivedLine) {
            super.onPostExecute(receivedLine);
            if (isCancelled()) {
                uploadTask = null;
            }
            if(receivedLine.equals("sent")){
                 Toast.makeText(mContext, "UDP DATA SENT", Toast.LENGTH_SHORT).show();
            }else if(receivedLine.equals("error")){
                Toast.makeText(mContext, "DATA NOT SENT: Error", Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "INSIDE ASYNCTASK onPOST EXECUTE: "+ receivedLine);
            checker = false;
        }
    }
}
