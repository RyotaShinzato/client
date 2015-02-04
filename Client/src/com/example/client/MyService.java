package com.example.client;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;


public class MyService extends Service{

	static{
		System.loadLibrary("alljoyn_java");
	}
	
	final static String TAG = "MyService";
	private static final int MESSAGE_PING_REPLY = 1;
	
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case MESSAGE_PING_REPLY:
				String reply = (String) msg.obj;
				Log.d(TAG,"reply: "+reply);
				break;
			default:
				break;
			}
		}
	};
	
	private SimpleService mSimpleService;
	private Handler mBusHandler;
	
	@Override
	public void onCreate(){
		Log.d(TAG,"oncreate");	
		
		HandlerThread busThread = new HandlerThread("BusHandler");
		busThread.start();
		mBusHandler = new BusHandler(busThread.getLooper());
		
		mSimpleService = new SimpleService();
		mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
	}
	
	@Override
	public int onStartCommand(Intent intent,int flags, int startId){
		Log.d(TAG,"onStartCommand");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy(){
		Log.d(TAG,"onDestroy");
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	class SimpleService implements SimpleInterface, BusObject{
		public String Ping(String inStr) {
		
			sendUiMessage(MESSAGE_PING_REPLY,inStr);
			return inStr;
		}
		
		private void sendUiMessage(int what, Object obj){
			mHandler.sendMessage(mHandler.obtainMessage(what, obj));
		}
	}
	
	class BusHandler extends Handler{
		
		private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";
		private static final short CONTACT_PORT = 42;
		
		private BusAttachment mBus;
		
		public static final int CONNECT = 1;
		public static final int DISCONNECT = 2;
		
		public BusHandler(Looper looper){
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
				case CONNECT: {
					org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
					
					mBus = new BusAttachment(getPackageName(),BusAttachment.RemoteMessage.Receive);
    				mBus.registerBusListener(new BusListener());
    				Status status = mBus.registerBusObject(mSimpleService, "/Service");
    				
    				Log.d(TAG,"registerBusObject: "+status);
    				if(status != Status.OK){
    					Log.d(TAG,"service_registerbusobjectダメ");
    					return;
    				}
    				    				
    				status = mBus.connect();
    				Log.d(TAG,"connect: "+status);
    				if(status != Status.OK){
    					Log.d(TAG,"service_connectだめ");
    					return;
    				}
    				
    				
    				Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
    				
    				SessionOpts sessionOpts = new SessionOpts();
    				sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
    				sessionOpts.isMultipoint = true; //change ture
    				sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
    				
    				sessionOpts.transports = SessionOpts.TRANSPORT_ANY + SessionOpts.TRANSPORT_WFD;
    				
    				status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener(){
    					@Override
    					public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts){
    						if(sessionPort == CONTACT_PORT){
    							return true;
    						} else{
    							return false;
    						}
    					}
    				});
    				
    				int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;
    				
    				status = mBus.requestName(SERVICE_NAME, flag);
    				if(status == Status.OK){
    					status = mBus.advertiseName(SERVICE_NAME, sessionOpts.transports);
    					
    					if(status != Status.OK){
    						status = mBus.releaseName(SERVICE_NAME);
    						return;
    					}
    				}
    				Log.d(TAG,"service_connect終わり");
    				break;
    			}
    			case DISCONNECT :{
    				mBus.unregisterBusObject(mSimpleService);
    				mBus.disconnect();
    				mBusHandler.getLooper().quit();
    				break;
    			}
    			
    			default:
    				break;
				
			}
		}
	}
}
