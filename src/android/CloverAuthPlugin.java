package com.sripra.termtegrity.cloverplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.Intent;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.MenuItem;
import android.view.View;
import android.net.Uri;
import android.widget.Button;
import android.widget.TextView;
import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.ResultStatus;
import com.clover.sdk.v1.ServiceConnector;
import com.clover.sdk.v1.merchant.Merchant;
import com.clover.sdk.v1.merchant.MerchantAddress;
import com.clover.sdk.v1.merchant.MerchantConnector;
import com.clover.sdk.v1.merchant.MerchantIntent;
import com.clover.sdk.v1.app.AppNotificationIntent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.app.NotificationCompat;
//import com.termtegrity.spotskim.cloverplugin.R;
//import com.termtegrity.spotskim.cloverplugin.SpotskimNotification;

import java.text.DateFormat;
import java.util.Date;

public class CloverAuthPlugin extends CordovaPlugin implements MerchantConnector.OnMerchantChangedListener, ServiceConnector.OnServiceConnectedListener{
    public static final String ACTION_GET_MERCHANT = "getMerchant";
    private static final String TAG = "CloverPlugin";
    private CallbackContext notificationCallbackContext = null;
    BroadcastReceiver receiver;
    
    @Override
    public void onMerchantChanged(Merchant merchant) {
        updateMerchant("merchant changed", null, merchant);
    }
    
    @Override
    public void onServiceConnected(ServiceConnector connector) {
        Log.i(TAG, "service connected: " + connector);
    }
    
    @Override
    public void onServiceDisconnected(ServiceConnector connector) {
        Log.i(TAG, "service disconnected: " + connector);
    }
    
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
    
    
    //    public void onResume() {
    //        super.onResume(true);
    //
    //        cordova.getActivity().registerReceiver(this.receiver,
    //                                                                 new IntentFilter(AppNotificationIntent.ACTION_APP_NOTIFICATION));
    //    }
    //
    //
    //    public void onPause() {
    //        cordova.getActivity().unregisterReceiver(this.receiver);
    //        super.onPause(true);
    //    }
    
    
    /**
     * Creates a JSONObject with the current battery information
     *
     * @param batteryIntent the current battery information
     * @return a JSONObject containing the battery status information
     */
    private JSONObject getBatteryInfo(Intent batteryIntent) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("level", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0));
            obj.put("isPlugged", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) > 0 ? true : false);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return obj;
    }
    
    /**
     * Updates the JavaScript side whenever the battery changes
     *
     * @param batteryIntent the current battery information
     * @return
     */
    private void updateBatteryInfo(Intent batteryIntent) {
        sendUpdate(this.getBatteryInfo(batteryIntent), true);
    }
    
    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param connection the network info to set as navigator.connection
     */
    private void sendUpdate(JSONObject info, boolean keepCallback) {
        if (this.notificationCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
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
                //JSONObject arg_object = args.getJSONObject(0);
                
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
                
                startAccountChooser();
                connect();
                getMerchant();
                //------------------------------------
                JSONObject obj = new JSONObject();
                try {
                    obj.put("MerchantID", merchantID);
                    obj.put("DeviceID", deviceID);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
                
                PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
                //-------------------------------
                
                // if(merchantConnector == null)
                //                     callbackContext.error("MerchantConnector is null");
                //                 else
                //                     callbackContext.success(merchantID+ "/" +deviceID);
                //                 disconnect();
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
    
    private static final int REQUEST_ACCOUNT = 0;
    private MerchantConnector merchantConnector;
    private Account account;
    private String merchantID;
    private String deviceID;
    
    private void startAccountChooser() {
        //        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{CloverAccount.CLOVER_ACCOUNT_TYPE}, false, null, null, null, null);
        //        startActivityForResult(intent, REQUEST_ACCOUNT);
        AccountManager accountManager = AccountManager.get(this.cordova.getActivity().getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType(CloverAccount.CLOVER_ACCOUNT_TYPE);
        for (Account acc : accounts){
            account = acc;
            break;
        }
        
    }
    
    
    private void connect() {
        disconnect();
        if (account != null) {
            merchantConnector = new MerchantConnector(cordova.getActivity(), account, this);
            merchantConnector.setOnMerchantChangedListener(this);
            merchantConnector.connect();
        }
    }
    
    private void disconnect() {
        if (merchantConnector != null) {
            merchantConnector.disconnect();
            merchantConnector = null;
        }
    }
    
    
    private void getMerchant() {
        Merchant merchant = null;
        try {
            merchant = merchantConnector.getMerchant();
        }
        catch (Exception e){
            
        }
        merchantID = merchant.getId();
        deviceID = merchant.getDeviceId();
        
    }
    
    private void updateMerchant(String status, ResultStatus resultStatus, Merchant result) {
        merchantID = result.getId();
        deviceID = result.getDeviceId();
    }
    
    //    /**
    //     * Stop notification receiver.
    //     */
    //    public void onDestroy() {
    //        removeNotificationListener();
    //    }
    //
    //    /**
    //     * Stop notification receiver.
    //     */
    //    public void onReset() {
    //        removeNotificationListener();
    //    }
    //
    //    /**
    //     * Stop the notification receiver and set it to null.
    //     */
    //    private void removeNotificationListener() {
    //        if (this.receiver != null) {
    //            try {
    //                this.cordova.getActivity().unregisterReceiver(this.receiver);
    //                this.receiver = null;
    //            } catch (Exception e) {
    //                Log.e(TAG, "Error unregistering notification receiver: " + e.getMessage(), e);
    //            }
    //        }
    //    }
}
