package com.example.client;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.Status;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.client.R;




public class MainActivity extends ActionBarActivity {

	static {
		System.loadLibrary("alljoyn_java");
	}
	
	BusHandler mBusHandler;
	private ArrayAdapter<String> mListViewArrayAdapter;
	private ListView mListView;
	
	//private Handler mhandler = new Handler(){
	//};
		
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.d("hoge","起動");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mListViewArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);
        
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new BusHandler(busThread.getLooper());
                
        mBusHandler.sendEmptyMessage(BusHandler.CONNECT);
        //mHandler.sendEmptyMessage(MESSAGE_START_PROGRESS_DIALOG);
        
        startService(new Intent(getBaseContext(),MyService.class));
        
        Button btn = (Button)findViewById(R.id.send);
        btn.setOnClickListener(new View.OnClickListener(){
        	@Override
        	public void onClick(View v){
        		Log.d("hoge","クリック");
        		
        		Message msg = mBusHandler.obtainMessage(BusHandler.PING,"UUID:XXXXXXXXX");
        		String ping = (String) msg.obj;
        		mListViewArrayAdapter.add("ping: "+ping);
        		mBusHandler.sendMessage(msg);
        	}
        });
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	mBusHandler.sendEmptyMessage(BusHandler.DISCONNECT);
    	Log.d("hoge","ですとろーい");
    }
    
    class BusHandler extends Handler {
    	private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";
    	private static final short CONTACT_PORT=42;
    	
    	private BusAttachment mBus;
    	private ProxyBusObject mProxyObj;
    	private SimpleInterface mClientInterface;
    	
    	private int mSessionId;
    	private boolean mIsInASession;
    	private boolean mIsConnected;
    	private boolean mIsStoppringDiscovery;
    	
    	public static final int CONNECT = 1;
    	public static final int JOIN_SESSION = 2;
    	public static final int DISCONNECT = 3;
    	public static final int PING = 4;
    	
    	   public BusHandler(Looper looper) {
    	      super(looper);
    	      
    	      mIsInASession = false;
    	      mIsConnected = false;
    	      mIsStoppringDiscovery = false;
    	   }

    	   @Override
    	   public void handleMessage(Message msg) {
    	      switch (msg.what) {
    	      case CONNECT:{
    	    	      	    	  
    	    	  org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
      	    	  
    	    	  mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);


    	    	  mBus.registerBusListener(new BusListener() {
    	    		  
    	    		  //つながったら呼ばれる
                      @Override
                      public void foundAdvertisedName(String name, short transport, String namePrefix) {
                      	Log.d("hoge","foundadvertisedname呼ばれた");
                      	if(!mIsConnected) {
                      	    Message msg = obtainMessage(JOIN_SESSION);
                      	    msg.arg1 = transport;
                      	    msg.obj = name;
                      	    sendMessage(msg);
                      	}
                      }
                  });
    	    	  
    	    	  //ここでpermission怒られてるっぽい
    	    	  Status status = mBus.connect();
    	    	  Log.d("hoge","connect: "+status);
    	    	  if(Status.OK != status){
    	    		  finish();
    	    		  return;
    	    	  }
    	    	  
    	    	  status = mBus.findAdvertisedName(SERVICE_NAME);
    	    	  Log.d("hoge","findadvertisedname: "+status);
    	    	  if(Status.OK != status){
    	    		  finish();
    	    		  return;
    	    	  }
    	    	  
    	    	      	    	  
    	    	  break;
    	      }
    	      
    	      case JOIN_SESSION:{
    	    	  Log.d("hoge","join_session呼ばれた");
    	    	  if(mIsStoppringDiscovery){
    	    		  Log.d("hoge","stoppingdiscovery");
    	    		  break;
    	    	  }
    	    	  short contactPort = CONTACT_PORT;
    	    	  SessionOpts sessionOpts = new SessionOpts();
    	    	  sessionOpts.transports = (short)msg.arg1;
    	    	  //追加multipoint
    	    	  sessionOpts.isMultipoint = true;
    	    	  Mutable.IntegerValue sessionId = new Mutable.IntegerValue();
    	    	  
    	    	  Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts, new SessionListener(){
    	    		 @Override
    	    		 public void sessionLost(int sessionId, int reason){
    	    			 mIsConnected = false;
    	    		 }
    	    	  });
    	    	  Log.d("hoge","joinnsesson: "+status);
    	    	  
    	    	  if(status == Status.OK){
    	    		  mProxyObj = mBus.getProxyBusObject(SERVICE_NAME, "/Service", sessionId.value, new Class<?>[]{ SimpleInterface.class});
    	    		  mClientInterface = mProxyObj.getInterface(SimpleInterface.class);
    	    		  
    	    		  mSessionId = sessionId.value;
    	    		  mIsConnected = true;
    	    	  }
    	    	  break;
    	      }
    	      
    	      case DISCONNECT:{
    	    	  mIsStoppringDiscovery = true;
    	    	  if(mIsConnected){
    	    		  //Status status =  mBus.leaveSession(mSessionId);
    	    		  Log.d("hoge","leavesession");
    	    	  }
    	    	  mBus.disconnect();
    	    	  getLooper().quit();
    	    	  break;
    	      }
    	      case PING:{
    	    	  try{
    	    		  if(mClientInterface != null){
    	    			  String reply = mClientInterface.Ping((String) msg.obj);
    	    			  Log.d("hoge","reply= "+reply);
    	    		  }
    	    	  } catch(BusException ex){
    	    		  Log.d("hoge","exception "+ex);
    	    	  }
    	    	  break;
    	      }
    	      default:
    	         break;
    	      }
    	   }
    	}
    
}
