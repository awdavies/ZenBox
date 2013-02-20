package com.zenbox;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
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
	
	private static final Size IMAGE_SIZE = new Size(640, 480);
	
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
	private int FEATURE_CIRCLE_RADIUS;
	private Scalar FEATURE_COLOR;
	
	// Members for handling histogram calculation.
	private MatOfInt[] mChannels;
	private MatOfInt mHistSize;
	private Mat mMat0;
	private int mHistSizeNum;
	private MatOfFloat mRanges;
	private Mat mHist;
	private float[] mBuf;
	private Point mP1;
	private Point mP2;
	private Scalar[] mColorsRGB;
	private Scalar[] mColorsHue;
	
	// Feature detector.
	private FeatureDetector mFeatureDetector;
	private MatOfPoint2f mFeatures;
	private MatOfPoint2f mPrevFeatures;
	
	// Pyramid matrices for optical flow detection.
	private Mat mPrevPyr;
	private Mat mCurPyr;
	
	// The audio manager member.
	private AudioMessenger mAudioMsgr;
	
	// The main Rgba matrix.
	private Mat mRgba;
	private Mat mPrevRgba;
	// An intermediate grayscale matrix meant for mRgba.
	private Mat mGray;
	private Mat mPrevGray;

	// need this callback in order to enable the openCV camera
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "ZenBox loaded successfully");
				System.loadLibrary("zen_box");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView.setMaxFrameSize((int)IMAGE_SIZE.width, (int)IMAGE_SIZE.height);
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
		AudioMessenger.getInstance(this); // should start the sound up
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// get the data from camera
		// type: Array type. Use CV_8UC1,..., CV_64FC4 to create 1-4 channel
		// matrices, or CV_8UC(n),..., CV_64FC(n) to create multi-channel (up to
		// CV_MAX_CN channels) matrices.
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mPrevRgba = new Mat(height, width, CvType.CV_8UC4);
		mGray = new Mat(height, width, CvType.CV_8UC1);
		mPrevGray = new Mat(height, width, CvType.CV_8UC1);
		mObjDetector = new BlobDetector();
		mAudioMsgr = AudioMessenger.getInstance(ZenBoxActivity.this);
		mSpectrum = new Mat();
		mBlobColorRGBA = new Scalar(255);
		mBlobColorHSV = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255, 0, 255, 255);
		RECT_COLOR = new Scalar(255, 0, 0, 255);
		
		// Init histogram members.
		mP1 = new Point();
		mP2 = new Point();
		mHistSizeNum = 25;
		mHist = new  Mat();
		mHistSize = new MatOfInt(mHistSizeNum);
		mMat0 = new Mat();
		mRanges = new MatOfFloat(0f, 256f);
		mChannels = new MatOfInt[] { new MatOfInt(0), new MatOfInt(1), new MatOfInt(2) };
		mBuf = new float[mHistSizeNum];
		mColorsRGB = new Scalar[] { new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255) };
        mColorsHue = new Scalar[] {
                new Scalar(255, 0, 0, 255),   new Scalar(255, 60, 0, 255),  new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255),  new Scalar(20, 255, 0, 255),  new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255),  new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255),  new Scalar(0, 0, 255, 255),   new Scalar(64, 0, 255, 255),  new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255),  new Scalar(255, 0, 0, 255)
        };
        
        mPrevPyr = new Mat();
        mCurPyr = new Mat();
        
        // Create an orb feature detector.
        mFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
		mPrevFeatures = new MatOfPoint2f();
		mFeatures = new MatOfPoint2f();
        FEATURE_CIRCLE_RADIUS = 10;
        FEATURE_COLOR = new Scalar(255, 255, 255, 255);
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
		mRgba.release();
		mPrevFeatures.release();
		mPrevRgba.release();
		mFeatures.release();
		mGray.release();
		mSpectrum.release();
		mMat0.release();
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
		
		// Detect objects.
//		mObjDetector.process(mRgba);
//		List<MatOfPoint> contours = mObjDetector.getContours();
//		Log.i(TAG, "Contours count: " + contours.size());
//		Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
//		
//		// This is a simple implementation of tracking the bounding shapes.
//		// For now only one sound is played for a single rectangle on the screen.
//		if (contours.size() > 0) {
//			List<Rect> rectangles = this.createBoundingShapes(contours);
//			this.playRectangleSound(rectangles.get(0));
//		}
		OpticalFlow(
				mPrevRgba.getNativeObjAddr(), 
				mRgba.getNativeObjAddr(),
				mPrevGray.getNativeObjAddr(),
				mGray.getNativeObjAddr(),
				mPrevFeatures.getNativeObjAddr(),
				mFeatures.getNativeObjAddr());
		Log.i(TAG, "mFeatures: " + mFeatures.dump());
		//this.drawFeatures();
		this.drawRGBHist(inputFrame);
		
		// These just show up in the corner of the screen (I think). And show the color
		// of the selected point.
		//		Mat colorLabel = mRgba.submat(4, 68, 4, 68);
		//		colorLabel.setTo(mBlobColorRGBA);
		//		 
		//		Mat spectrumLabel = mRgba.submat(4,  4+ mSpectrum.rows(), 70, 70 + mSpectrum.cols());
		//		mSpectrum.copyTo(spectrumLabel);
		
		inputFrame.copyTo(mPrevRgba);
		return mRgba;
	}
	
	/**
	 * Detects and assigns a vector to each located feature from the mLastFrame member.  This will be
	 * used to modulate the sound... somehow.
	 * 
	 * @param inputFrame
	 */
	private void detectOpticalFlow(Mat inputFrame) {
		// In here we're going to have the native method call to cvCalcOpticalFlowPyrLK
		// as outlined in this rockin paper: http://robots.stanford.edu/cs223b05/notes/CS%20223-B%20T1%20stavens_opencv_optical_flow.pdf
		
		// TODO: For some reason the includes don't seem to work particularly well.
		// It doesn't seem to know where to look.
	}
	
	/**
	 * Detects and draws the features found in the input frame using the mFeatureDetectorOrb member.
	 * As of current, this detector uses the ORB feature detector.
	 * @param inputFrame
	 */
	private void drawFeatures() {
		// This can be sped up by simply accessing each one of the elements of the array.
		// this might be useful when handling pure data.  For now, though, this is just for debugging.
		for (Point p : mPrevFeatures.toArray()) {
			Core.circle(mRgba, p, FEATURE_CIRCLE_RADIUS, FEATURE_COLOR);
		}
	}
	
	/**
	 * Draws an RGB histogram based on the input frame.  This draws on top of mRgb, so make
	 * sure to choose the time at which this is called properly.
	 * @param inputFrame
	 */
	private void drawRGBHist(Mat inputFrame) {
		// Draw RGB Histogram.
		Size rgbaSize = mRgba.size();
		int thickness = (int) (rgbaSize.height / (mHistSizeNum + 10) / 5);
		int offset = (int) (rgbaSize.height);
        if(thickness > 5)
        	thickness = 5;
		for (int i = 0; i < 3; ++i) {
			// Calculate hist from input frame so as to avoid handling other input.
			Imgproc.calcHist(Arrays.asList(inputFrame), mChannels[i], mMat0, mHist, mHistSize, mRanges);
			Core.normalize(mHist, mHist, rgbaSize.height/2, 0, Core.NORM_INF);
			mHist.get(0, 0, mBuf);
			for(int j = 0; j < mHistSizeNum; ++j) {
                mP1.y = mP2.y = offset - (i * (mHistSizeNum + 10) + j) * thickness;
                mP1.x = rgbaSize.width-1;
                mP2.x = mP1.x - 2 - (int) mBuf[j];
                Core.line(mRgba, mP1, mP2, mColorsRGB[i], thickness);
            }
		}
	}
	
	/**
	 * Draws the bounding rectangles around all of the passed contours.  Draws the the main image
	 * member "mRgba."
	 * @param contours  The list of contours.
	 * @return A list of the boundingRectangles created on the screen.
	 */
	private List<Rect> createBoundingShapes(List<MatOfPoint> contours) {
		List<Rect> boundingRectangles = new ArrayList<Rect>(contours.size());
		for (int i = 0; i < contours.size(); ++i) {
			Rect boundingRect = Imgproc.boundingRect(contours.get(i));
			boundingRectangles.add(boundingRect);
			Core.rectangle(mRgba, boundingRect.tl(), boundingRect.br(),
					RECT_COLOR);
		}
		return boundingRectangles;
	}
	
	/**
	 * Plays a sound according to some bounding rectangle on the screen.  The method
	 * is currently a bit ad-lib, so just passing a single rectangle per frame should create
	 * a steady sound.
	 */
	private void playRectangleSound(Rect rectangle) {
		if (rectangle != null) {
			JavaCameraView v = (JavaCameraView) findViewById(R.id.activity_zen_box_view);
			float x = AudioMessenger.normalize(rectangle.x + rectangle.width / 2, 0.5f, 1.5f, v.getWidth());
			float y = AudioMessenger.normalize(rectangle.y + rectangle.height / 2, 0.3f, 0.8f, v.getHeight());
			mAudioMsgr.sendMessage("change", "a", x, y);
		}
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
		mAudioMsgr.cleanup();
	}

	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL,
				4);
		return new Scalar(pointMatRgba.get(0, 0));
	}

	public native void OpticalFlow(long addrPrevMat,
								   long addrCurMat,
								   long addrPrevMatGray,
								   long addrCurMatGray,
								   long addrPrevFeat,
								   long addrCurFeat);
}
