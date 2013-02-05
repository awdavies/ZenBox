package com.zenbox;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class ZenBoxActivity extends Activity implements CvCameraViewListener {
	// tag for this class
	private static final String TAG = "ZenBox:Acticity";
	// open CV camera
	private CameraBridgeViewBase mOpenCvCameraView;

	// need this callback in order to enable the openCV camera
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "ZenBox loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	}; 

	private Mat mRgba;

	public ZenBoxActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "called onCreate");
		// turn off the title on the screen
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Window flag: as long as this window is visible to the user, keep the
		// device's screen turned on and bright.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_zen_box);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_zen_box_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// get the data from camera
		// type: Array type. Use CV_8UC1,..., CV_64FC4 to create 1-4 channel
		// matrices, or CV_8UC(n),..., CV_64FC(n) to create multi-channel (up to
		// CV_MAX_CN channels) matrices.
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		
		
		// mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
	}

	/*
	 * bring something onto the screen. So this is called every time
	 * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener#onCameraFrame(org.opencv.core.Mat)
	 */
	@Override
	public Mat onCameraFrame(Mat inputFrame) {
		 inputFrame.copyTo(mRgba);
		// put the image into the screen
		return mRgba;
	}

	@Override
	protected void onPause() {
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// this statement is to active the mLoaderCallBack so it can enable the
		// camera
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

}
