package com.wpdo.sendsms;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

/**
 * Created by WPDO on 20200428
 */

public class SendSMS extends ReactContextBaseJavaModule {
	private final ReactApplicationContext reactContext;
	private Callback callback = null;

	public SendSMS(ReactApplicationContext reactContext) {
		super(reactContext);
		this.reactContext = reactContext;
	}

	@Override
	public String getName() {
		return "SendSMS";
	}

	private void sendCallback(Integer messageId, String message){
		if (callback != null) {
			callback.invoke(messageId, message);
			callback = null;
		}
	}

	//---sends an SMS message to another device---
	@ReactMethod
	public void send(final Integer messageId, String phoneNumber, String message, final Callback cb ){

		try{

			this.callback = cb;
			String SENT = "SMS_SENT";
			String DELIVERED = "SMS_DELIVERED";

			IntentFilter sendIntentFilter = new IntentFilter(SENT);
			IntentFilter receiveIntentFilter = new IntentFilter(DELIVERED);

			PendingIntent sentPI = PendingIntent.getBroadcast(reactContext, 0,new Intent(SENT), 0);
			PendingIntent deliveredPI = PendingIntent.getBroadcast(reactContext, 0,new Intent(DELIVERED), 0);

			BroadcastReceiver messageSentReceiver = new BroadcastReceiver(){
				@Override
				public void onReceive(Context arg0, Intent arg1) {
					switch (getResultCode())
					{
						case Activity.RESULT_OK:
							sendCallback(messageId, "SMS sent");
							break;
						case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
							sendCallback(messageId, "Generic failure");
							break;
						case SmsManager.RESULT_ERROR_NO_SERVICE:
							sendCallback(messageId, "No service");
							break;
						case SmsManager.RESULT_ERROR_NULL_PDU:
							sendCallback(messageId, "Null PDU");
							break;
						case SmsManager.RESULT_ERROR_RADIO_OFF:
							sendCallback(messageId, "Radio off");
							break;
					}
				}
			};

			BroadcastReceiver messageReceiveReceiver = new BroadcastReceiver(){
				@Override
				public void onReceive(Context arg0, Intent arg1) {
					switch (getResultCode())
					{
						case Activity.RESULT_OK:
							System.out.println("SMS delivered");
							sendCallback(messageId, "SMS delivered");
							break;
						case Activity.RESULT_CANCELED:
							System.out.println("SMS not delivered");
							sendCallback(messageId, "SMS not delivered");
							break;
					}
				}
			};

			//---when the SMS has been sent---
			reactContext.registerReceiver(messageSentReceiver, sendIntentFilter);

			//---when the SMS has been delivered---
			reactContext.registerReceiver(messageReceiveReceiver, receiveIntentFilter);

			SmsManager sms = SmsManager.getDefault();

			ArrayList<String> parts = sms.divideMessage(message);

			ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
			ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();

			for (int i = 0; i < parts.size(); i++)
			{
				sentIntents.add(sentPI);
				deliveryIntents.add(deliveredPI);
			}

			sms.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);

			// sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

		}catch (Exception e) {

			sendCallback(messageId, "Unknown error");
			throw e;

		}

	}

}