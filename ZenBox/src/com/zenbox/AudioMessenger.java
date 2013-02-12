package com.zenbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.util.Log;

/**
 * Singleton class to set up a connection to the PD service
 * and facilitate message passing.
 * 
 * @author brucec5
 *
 */
public class AudioMessenger {
	private static final String TAG = "ZenBox::AudioMessenger";
	
	private static AudioMessenger messenger = null;
	
	private PdService pdService;
	
	private final ServiceConnection connection;
	
	private Activity act; 
	
	private AudioMessenger(Activity act) {
		this.act = act;
		connection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder svc) {
				pdService = ((PdService.PdBinder)svc).getService();
				initPd();
			}
		
			@Override
			public void onServiceDisconnected(ComponentName name) {
				// Never run, or so I am told (required for interface)
			}
		};
		
		act.bindService(new Intent(act, PdService.class),
				connection, Activity.BIND_AUTO_CREATE);
	}
	
	/**
	 * Return the instance of AudioMessenger
	 * 
	 * @return		the AudioMessenger instance 
	 */
	public static AudioMessenger getInstance(Activity act) {
		if (messenger == null) {
			messenger = new AudioMessenger(act);
		}
		return messenger;
	}

	/**
	 * Sends a bang to an object in the PD patch.
	 * 
	 * @param recv	symbol associated with the receiver
	 * @return		error code, 0 on success
	 */
	public int sendBang(String recv) {
		return PdBase.sendBang(recv);
	}
	
	/**
	 * Sends a float to an object in the PD patch.
	 *  
	 * @param recv	symbol associated with the receiver
	 * @param x		the float to send
	 * @return		error code, 0 on success
	 */
	public int sendFloat(String recv, float x) {
		return PdBase.sendFloat(recv, x);
	}
	
	/**
	 * Sends a list to an object in the PD patch.
	 * 
	 * @param recv	symbol associated with the receiver
	 * @param args	A list of arguments of type Integer, Float, or String
	 * @return		error code, 0 on success
	 */
	public int sendList(String recv, Object... args) {
		return PdBase.sendList(recv, args);
	}
	
	/**
	 * Sends a typed message to an object in the PD patch.
	 * 
	 * @param recv	symbol associated with the receiver
	 * @param msg	first symbol of message
	 * @param args	list of arguments to the message
	 * 				of type Integer, Float, or String
	 * @return		error code, 0 on success
	 */
	public int sendMessage(String recv, String msg, Object... args) {
		return PdBase.sendMessage(recv, msg, args);
	}
	
	/**
	 * Cleans up the audio messenger.  To be called upon exiting the activity.
	 */
	public void cleanup() {
		try {
			act.unbindService(connection);
			messenger = null;
		} catch (IllegalArgumentException e) {
			// already unbound
			pdService = null;
		}
	}
	
	private void initPd() {
		Resources res = act.getResources();
		File patch = null, audio = null;
		
		try {
			PdBase.subscribe("android");
			
			InputStream inp = res.openRawResource(R.raw.synth);
			InputStream ina = res.openRawResource(R.raw.icke);
			
			patch = IoUtils.extractResource(inp, "synth.pd", act.getCacheDir());
			audio = IoUtils.extractResource(ina, "icke.wav", act.getCacheDir());
			
			PdBase.openPatch(patch);
			
			String name = res.getString(R.string.app_name);
			
			// -1 means use default, which should work for us.
			pdService.initAudio(-1, -1, -1, -1);
			pdService.startAudio(new Intent(act, ZenBoxActivity.class),
					R.drawable.icon, name, name);
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		} finally {
			if (patch != null)
				patch.delete();
			if (audio != null)
				patch.delete();
		}
	}
	
	public static float normalize(float in, float oMax, float oMin, float inMax) {
		return oMin + in * (oMax - oMin) / inMax;
	}
}
