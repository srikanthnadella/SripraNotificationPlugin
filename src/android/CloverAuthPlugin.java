package com.termtegrity.spotskim.cloverplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.pm.PackageManager;


import android.accounts.Account;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.util.CloverAuth;
import com.clover.sdk.v1.ResultStatus;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v1.merchant.Merchant;
import com.clover.sdk.v1.merchant.MerchantAddress;
import com.clover.sdk.v1.merchant.MerchantConnector;
import com.clover.sdk.v1.merchant.MerchantIntent;
import com.clover.sdk.v1.app.AppNotificationIntent;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CloverAuthPlugin extends CordovaPlugin{
    public static final String ACTION_GET_MERCHANT = "getMerchant";
    private static final String TAG = "CloverPlugin";
      private CallbackContext notificationCallbackContext = null;
    BroadcastReceiver receiver;
    //private static final int REQUEST_ACCOUNT = 0;
    //private MerchantConnector merchantConnector;
    private Account account;
    private String merchantID = "TEST";
    private String deviceID = "TEST1";
    
     private void sendNotification(Intent notificationIntent){
        if (this.notificationCallbackContext != null) {
            
            //this.notificationCallbackContext.success("Notification:" + notificationIntent.getStringExtra(AppNotificationIntent.EXTRA_APP_EVENT) +
            //                                         "/" + notificationIntent.getStringExtra(AppNotificationIntent.EXTRA_PAYLOAD));
            //updateBatteryInfo(notificationIntent);
            JSONObject obj = new JSONObject();
            try {
                obj.put("level", notificationIntent.getStringExtra("appEvent"));
                obj.put("isPlugged", notificationIntent.getStringExtra("payload"));
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
            result.setKeepCallback(true);
            this.notificationCallbackContext.sendPluginResult(result);
        }
        
    }
    
    @SuppressWarnings("deprecation")
    private void Notify(String notificationTitle, String notificationMessage) {
        
        
        NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(cordova.getActivity()).setAutoCancel(true)
        .setSmallIcon(cordova.getActivity().getApplicationInfo().icon)
        .setContentTitle("Spotskim Notification")
        .setContentText("A notification has been received from the Spotskim server").setTicker("Spotskim notification");
        
        // PackageManager pm = cordova.getActivity().getPackageManager();
        // Intent resultIntent = pm.getLaunchIntentForPackage(cordova.getActivity().getApplicationContext().getPackageName());
        
        String packageName = cordova.getActivity().getPackageName();
        Intent resultIntent = cordova.getActivity().getPackageManager().getLaunchIntentForPackage(packageName);
        
        // Creates an explicit intent for an Activity in your app
        //Intent resultIntent = new Intent(cordova.getActivity(), SpotskimNotification.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        resultIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        //TaskStackBuilder stackBuilder = TaskStackBuilder.create(cordova.getActivity());
        // Adds the back stack for the Intent (but not the Intent itself)
        //stackBuilder.addParentStack(SpotskimNotification.class);
        // Adds the Intent that starts the Activity to the top of the stack
        //stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(cordova.getActivity(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);//stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
        (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(1, mBuilder.build());
    }
    
 
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            if (ACTION_GET_MERCHANT.equals(action)) {
            
            if (this.notificationCallbackContext == null) {
                    this.notificationCallbackContext = callbackContext;
                }
                
                IntentFilter intentFilter = new IntentFilter();
                //intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
                intentFilter.addAction(AppNotificationIntent.ACTION_APP_NOTIFICATION);
                if (this.receiver == null) {
                    this.receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            sendNotification(intent);
                            Notify("Title: Meeting with Business",
                                   "Msg:Pittsburg 10:00 AM EST ");
                        }
                    };
                    cordova.getActivity().registerReceiver(this.receiver, intentFilter);
                }
                
                
                account = CloverAccount.getAccount(this.cordova.getActivity().getApplicationContext());
                if (account == null) {
            callbackContext.error("Account not found");
            return false;
        }
        //getMerchant();
        queryWebService();
        JSONObject obj = new JSONObject();
        //if(merchantID != null && deviceID != null){
		
        try {
            obj.put("MerchantID", merchantID);
            obj.put("DeviceID", deviceID);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        //}
        //else{
        //callbackContext.error("Merchant not found");
        //    return false;
        //}
        
	PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);

        return true;
        }
            callbackContext.error("Invalid action");
            return false;
        } catch(Exception e) {
            System.err.println("Exception: " + e.getMessage());
            callbackContext.error("Exception: " + e.getMessage());
            return false;
        }
    }
    
    
//     private void getMerchant() {
//         Merchant merchant = null;
//         try {
//             merchant = merchantConnector.getMerchant();
//         }
//         catch (Exception e){
//             
//         }
//         updateMerchant();
//         
//     }
//     
//     private void updateMerchant() {
//         merchantID = result.getId();
//         deviceID = result.getDeviceId();
//     }

    private void log(String text) {
        Log.i(TAG, text);
    }
    
    private void queryWebService() {
        new AsyncTask<Void, String, Void>() {

            @Override
            protected void onProgressUpdate(String... values) {
                String logString = values[0];
                log(logString);
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    publishProgress("Requesting auth token");
                    CloverAuth.AuthResult authResult = CloverAuth.authenticate(cordova.getActivity(), account);
                    publishProgress("Successfully authenticated as " + account + ".  authToken=" + authResult.authToken + ", authData=" + authResult.authData);

                    if (authResult.authToken != null && authResult.baseUrl != null) {
                        CustomHttpClient httpClient = CustomHttpClient.getHttpClient();
                        String getNameUri = "/v2/merchant/name";
                        String url = authResult.baseUrl + getNameUri + "?access_token=" + authResult.authToken;
                        publishProgress("requesting merchant id using: " + url);
                        String result = httpClient.get(url);
                        JSONTokener jsonTokener = new JSONTokener(result);
                        JSONObject root = (JSONObject) jsonTokener.nextValue();
                        merchantID = root.getString("merchantId");
                        deviceID = root.getString("deviceId");
                        publishProgress("received merchant id: " + merchantID);

                    }
                } catch (Exception e) {
                    publishProgress("Error retrieving merchant info from server" + e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                // mButton.setEnabled(true);
            }
        }.execute();
    }

    static class CustomHttpClient extends DefaultHttpClient {
        private static final int CONNECT_TIMEOUT = 60000;
        private static final int READ_TIMEOUT = 60000;
        private static final int MAX_TOTAL_CONNECTIONS = 5;
        private static final int MAX_CONNECTIONS_PER_ROUTE = 3;
        private static final int SOCKET_BUFFER_SIZE = 8192;
        private static final boolean FOLLOW_REDIRECT = false;
        private static final boolean STALE_CHECKING_ENABLED = true;
        private static final String CHARSET = HTTP.UTF_8;
        private static final HttpVersion HTTP_VERSION = HttpVersion.HTTP_1_1;
        private static final String USER_AGENT = "CustomHttpClient"; // + version

        public static CustomHttpClient getHttpClient() {
            CustomHttpClient httpClient = new CustomHttpClient();
            final HttpParams params = httpClient.getParams();
            HttpProtocolParams.setUserAgent(params, USER_AGENT);
            HttpProtocolParams.setContentCharset(params, CHARSET);
            HttpProtocolParams.setVersion(params, HTTP_VERSION);
            HttpClientParams.setRedirecting(params, FOLLOW_REDIRECT);
            HttpConnectionParams.setConnectionTimeout(params, CONNECT_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, READ_TIMEOUT);
            HttpConnectionParams.setSocketBufferSize(params, SOCKET_BUFFER_SIZE);
            HttpConnectionParams.setStaleCheckingEnabled(params, STALE_CHECKING_ENABLED);
            ConnManagerParams.setTimeout(params, CONNECT_TIMEOUT);
            ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);
            ConnManagerParams.setMaxConnectionsPerRoute(params, new ConnPerRouteBean(MAX_CONNECTIONS_PER_ROUTE));

            return httpClient;
        }

        public String get(String url) throws IOException, HttpException {
            String result;
            HttpGet request = new HttpGet(url);
            HttpResponse response = execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                } else {
                    throw new HttpException("Received empty body from HTTP response");
                }
            } else {
                throw new HttpException("Received non-OK status from server: " + response.getStatusLine());
            }
            return result;
        }

        @SuppressWarnings("unused")
        public String post(String url, String body) throws IOException, HttpException {
            String result;
            HttpPost request = new HttpPost(url);
            HttpEntity bodyEntity = new StringEntity(body);
            request.setEntity(bodyEntity);
            HttpResponse response = execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    result = EntityUtils.toString(entity);
                } else {
                    throw new HttpException("Received empty body from HTTP response");
                }
            } else {
                throw new HttpException("Received non-OK status from server: " + response.getStatusLine());
            }
            return result;
        }
    }
}
