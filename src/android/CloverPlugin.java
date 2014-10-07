package com.sripra.termtegrity.cloverplugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import java.text.DateFormat;
import java.util.Date;

public class CloverPlugin extends CordovaPlugin implements MerchantConnector.OnMerchantChangedListener, ServiceConnector.OnServiceConnectedListener{
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
            
            this.notificationCallbackContext.success("Notification:" + notificationIntent.getStringExtra(AppNotificationIntent.EXTRA_APP_EVENT) +
                                                     "/" + notificationIntent.getStringExtra(AppNotificationIntent.EXTRA_PAYLOAD));
        }
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
                intentFilter.addAction(AppNotificationIntent.ACTION_APP_NOTIFICATION);
                if (this.receiver == null) {
                    this.receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            sendNotification(intent);
                        }
                    };
                    cordova.getActivity().registerReceiver(this.receiver, intentFilter);
                }
                
                startAccountChooser();
                connect();
                getMerchant();
                
                if(merchantConnector == null)
                    callbackContext.error("MerchantConnector is null");
                else
                    callbackContext.success(merchantID+ "/" +deviceID);
                disconnect();
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
    
    //
    //    private void startAccountChooser() {
    //        Intent intent = AccountManager.newChooseAccountIntent(null, null, new String[]{CloverAccount.CLOVER_ACCOUNT_TYPE}, false, null, null, null, null);
    //        startActivityForResult(intent, REQUEST_ACCOUNT);
    //    }
    //
    //    @Override
    //    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    //        if (requestCode == REQUEST_ACCOUNT) {
    //            if (resultCode == RESULT_OK) {
    //                String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
    //                String type = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
    //                account = new Account(name, type);
    //            } else {
    //                if (account == null) {
    //                    finish();
    //                }
    //            }
    //        }
    //    }
    
    
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
    
    /**
     * Stop notification receiver.
     */
    public void onDestroy() {
        removeNotificationListener();
    }
    
    /**
     * Stop notification receiver.
     */
    public void onReset() {
        removeNotificationListener();
    }
    
    /**
     * Stop the notification receiver and set it to null.
     */
    private void removeNotificationListener() {
        if (this.receiver != null) {
            try {
                this.cordova.getActivity().unregisterReceiver(this.receiver);
                this.receiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering notification receiver: " + e.getMessage(), e);
            }
        }
    }
}