package com.zenbox;

import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

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
	private Scalar CONTOUR_COLOR;
	private Scalar RECT_COLOR;

	// need this callback in order to enable the openCV camera
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "ZenBox loaded successfully");
				mOpenCvCameraView.enableView();
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
		AudioMessenger.getInstance(this); // should start the sound up
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// get the data from camera
		// type: Array type. Use CV_8UC1,..., CV_64FC4 to create 1-4 channel
		// matrices, or CV_8UC(n),..., CV_64FC(n) to create multi-channel (up to
		// CV_MAX_CN channels) matrices.
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mObjDetector = new BlobDetector();
		mSpectrum = new Mat();
		mBlobColorRGBA = new Scalar(255);
		mBlobColorHSV = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255, 0, 255, 255);
		RECT_COLOR = new Scalar(255, 0, 0, 255);

		// mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
	}

	public boolean onTouch(View v, MotionEvent event) {
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
		// TODO: Turn off the sounds!
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
		AudioMessenger msgr = AudioMessenger.getInstance(this);
		JavaCameraView v = (JavaCameraView) findViewById(R.id.activity_zen_box_view);
		
		inputFrame.copyTo(mRgba);
		mObjDetector.process(mRgba);
		
		List<MatOfPoint> contours = mObjDetector.getContours();
		Log.i(TAG, "Contours count: " + contours.size());
		Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
		
		Rect biggestRect = null;
		
		for (MatOfPoint contour : contours) {
			Rect boundingRect = Imgproc.boundingRect(contour);
			
			if (biggestRect == null || boundingRect.area() > biggestRect.area())
				biggestRect = boundingRect;
			
			Core.rectangle(mRgba, boundingRect.tl(), boundingRect.br(), 
				 RECT_COLOR);
		}
		
		float x, y;
		if (biggestRect != null) {
			x = AudioMessenger.normalize(biggestRect.x + biggestRect.width / 2, 0.5f, 1.5f, v.getWidth());
			y = AudioMessenger.normalize(biggestRect.y + biggestRect.height / 2, 0.3f, 0.8f, v.getHeight());
			msgr.sendList("change", x, y);
		}
		
		// These just show up in the corner of the screen (I think). And show the color
		// of the selected point.
		Mat colorLabel = mRgba.submat(4, 68, 4, 68);
		colorLabel.setTo(mBlobColorRGBA);
		 
		Mat spectrumLabel = mRgba.submat(4,  4+ mSpectrum.rows(), 70, 70 + mSpectrum.cols());
		mSpectrum.copyTo(spectrumLabel);
		 
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
		AudioMessenger.getInstance(this).cleanup();
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL,
				4);

		return new Scalar(pointMatRgba.get(0, 0));
	}

}
