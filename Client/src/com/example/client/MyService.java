package com.example.client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service{

	@Override
	public void onCreate(){
		Log.d("hoge","oncreate");	
	}
	
	@Override
	public int onStartCommand(Intent intent,int flags, int startId){
		Log.d("hoge","onStartCommand");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy(){
		Log.d("hoge","onDestroy");
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
