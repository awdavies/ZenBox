package com.zenbox;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

public class ZenBoxActivity extends Activity implements OnTouchListener,
		CvCameraViewListener {
	// tag for this class
	private static final String TAG = "ZenBox::Activity";
	// open CV camera
	private CameraBridgeViewBase mOpenCvCameraView;

	// Used to detect large spaces of the same color. For no there will only be
	// one section
	// of the image detected at a time. The following members are used to
	// display/locate this
	// blob.
	private BlobDetector mObjDetector;
	private Mat mSpectrum;
	private Scalar mBlobColorRGBA;
	private Scalar mBlobColorHSV;
	private Size SPECTRUM_SIZE;
	private int FEATURE_CIRCLE_RADIUS;
	private Scalar FEATURE_COLOR;
	
	// Feature detector.
	private FeatureDetector mFeatureDetector;
	private MatOfKeyPoint mFeatures;
	
	// The audio manager member.
	private AudioMessenger mAudioMsgr;
	
	// The main Rgba matrix.
	private Mat mRgba;
	
	// An intermediate grayscale matrix meant for mRgba.
	private Mat mGray;

	// need this callback in order to enable the openCV camera
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.e(TAG, "ZenBox loaded successfully");
				mOpenCvCameraView.enableView();
				// This may be the perfect size the displaying, change this size
				// may change the GUI. at land: 720-> width, 640-> height
				mOpenCvCameraView.setMaxFrameSize(720, 640);
				mOpenCvCameraView.setOnTouchListener(ZenBoxActivity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

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
		
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
		spinnerListener(spinner);
		

		SeekBar vol = (SeekBar) findViewById(R.id.volume);
		volumeListener(vol);
		
		
		mAudioMsgr = AudioMessenger.getInstance(this); // should start the sound up
	}

	/*
	 * Edit this one in order to change the volume
	 */
	private void volumeListener(SeekBar vol) {
		
		
		//vol.setBackgroundColor(Color.rgb(255, 245, 238));
		vol.setMax(100);
		vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// change this to change the sound 
				// get the value form the progress
				mAudioMsgr.sendFloat("Volume", 0.9f);
			}
		});		
	}

	/*
	 * get the file and change the sound sample
	 */
	private void spinnerListener(Spinner spinner) {
		// TODO Auto-generated method stub
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				// this is selecting the file from the spinner
				mAudioMsgr.sendNextFileName(pos);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// get the data from camera
		// type: Array type. Use CV_8UC1,..., CV_64FC4 to create 1-4 channel
		// matrices, or CV_8UC(n),..., CV_64FC(n) to create multi-channel (up to
		// CV_MAX_CN channels) matrices.
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mGray = new Mat(height, width, CvType.CV_8UC4);
		mObjDetector = new BlobDetector();
		//mAudioMsgr = AudioMessenger.getInstance(ZenBoxActivity.this);
		mSpectrum = new Mat();
		mBlobColorRGBA = new Scalar(255);
		mBlobColorHSV = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		
        // Create an orb feature detector.
        mFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
        mFeatures = new MatOfKeyPoint();
        FEATURE_CIRCLE_RADIUS = 4;
        FEATURE_COLOR = new Scalar(255, 255, 255, 255);
	}

	public boolean onTouch(View v, MotionEvent event) {
		//mAudioMsgr.sendNextFileName();
		int cols = mRgba.cols();
		int rows = mRgba.rows();

		int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
		int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

		int x = (int) event.getX() - xOffset;
		int y = (int) event.getY() - yOffset;

		Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

		if ((x < 0) || (y < 0) || (x > cols) || (y > rows))
			return false;

		Rect touchedRect = new Rect();

		touchedRect.x = (x > 4) ? x - 4 : 0;
		touchedRect.y = (y > 4) ? y - 4 : 0;

		touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols
				- touchedRect.x;
		touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows
				- touchedRect.y;

		Mat touchedRegionRgba = mRgba.submat(touchedRect);

		Mat touchedRegionHsv = new Mat();
		Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv,
				Imgproc.COLOR_RGB2HSV_FULL);

		// Calculate average color of touched region
		mBlobColorHSV = Core.sumElems(touchedRegionHsv);
		int pointCount = touchedRect.width * touchedRect.height;
		for (int i = 0; i < mBlobColorHSV.val.length; i++)
			mBlobColorHSV.val[i] /= pointCount;

		mBlobColorRGBA = converScalarHsv2Rgba(mBlobColorHSV);

		Log.i(TAG, "Touched rgba color: (" + mBlobColorRGBA.val[0] + ", "
				+ mBlobColorRGBA.val[1] + ", " + mBlobColorRGBA.val[2] + ", "
				+ mBlobColorRGBA.val[3] + ")");

		mObjDetector.setHsvColor(mBlobColorHSV);

		Imgproc.resize(mObjDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

		touchedRegionRgba.release();
		touchedRegionHsv.release();

		return false; // don't need subsequent touch events
	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
	}

	/*
	 * bring something onto the screen. So this is called every time
	 * 
	 * @see
	 * org.opencv.android.CameraBridgeViewBase.CvCameraViewListener#onCameraFrame
	 * (org.opencv.core.Mat)
	 */
	@Override
	public Mat onCameraFrame(Mat inputFrame) {
		// Grab a frame and process it with the object detector.
		inputFrame.copyTo(mRgba);

		this.drawFeatures(inputFrame);
		double[] val = Core.mean(inputFrame).val;

		float grainstart = AudioMessenger.normalize((float)val[0], 1.0f, 0.0f, 255.0f);
		float graindur = AudioMessenger.normalize((float)val[1], 2000.0f, 10.0f, 255.0f);
		float grainpitch = AudioMessenger.normalize((float)val[2], 2.0f, 0.3f, 255.0f);
		
		mAudioMsgr.sendFloat("grainstart_in", grainstart);
		mAudioMsgr.sendFloat("graindur_in", graindur);
		mAudioMsgr.sendFloat("grainpitch_in", grainpitch);
		 
		return mRgba;
	}
	
	/**
	 * Detects and draws the features found in the input frame using the mFeatureDetectorOrb member.
	 * As of current, this detector uses the ORB feature detector.
	 * @param inputFrame
	 */
	private void drawFeatures(Mat inputFrame) {		
		// Create gray image for feature detection.
        Imgproc.cvtColor(inputFrame, mGray, Imgproc.COLOR_RGBA2GRAY);
		mFeatureDetector.detect(inputFrame, mFeatures);
		KeyPoint[] points = mFeatures.toArray();  // TODO: This might be slow. Check under profiler.
		for (KeyPoint kp : points) {
			Core.circle(mRgba, kp.pt, FEATURE_CIRCLE_RADIUS, FEATURE_COLOR);
		}
	}

	@Override
	protected void onPause() {
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		super.onPause();
		mAudioMsgr.cleanup();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// this statement is to active the mLoaderCallBack so it can enable the
		// camera
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
		mAudioMsgr = AudioMessenger.getInstance(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
		mAudioMsgr.cleanup();
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL,
				4);
		return new Scalar(pointMatRgba.get(0, 0));
	}

}
