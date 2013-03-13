package com.zenbox;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ZoneProcessor {
	
	private static final int GRID_SIZE = 4;
	private static final int GRID_AREA = GRID_SIZE * GRID_SIZE;
	private static final String TAG = "ZoneProcessor";
	
	// Buffers for the number of features in each zone, as well
	// as the overall HSV of the zone.
	private int[] mNumFeatures;
	public Float[] mHue;
	public Float[] mSat;
	public Float[] mVal;
	
	// The cells for each frame.  One frame fills a cell.
	private Mat[] mCells;
	
	// Intermediate matrix for image manipulation.
	private Mat mHSV;
	
	public ZoneProcessor(int height, int width) {
		mNumFeatures = new int[GRID_AREA];
		mHue = new Float[GRID_AREA];
		mSat = new Float[GRID_AREA];
		mVal = new Float[GRID_AREA];
		mCells = new Mat[GRID_AREA];
		

		mHSV = new Mat(height, width, CvType.CV_8UC3);
		
		// Chop up img into submats.
		for (int i = 0; i < GRID_SIZE; ++i) {
			for (int j = 0; j < GRID_SIZE; ++j) {
				int k = i * GRID_SIZE + j;
				mCells[k] = mHSV.submat(i * mHSV.rows() / GRID_SIZE,
						(i + 1) * mHSV.rows() / GRID_SIZE,
						j * mHSV.cols() / GRID_SIZE,
						(j + 1) * mHSV.cols() / GRID_SIZE);
			}
		}
	}
	
	/**
	 * Processes the zones of the image and stores the average Hue, Saturation, and Values within the
	 * internal buffers.
	 * @param img
	 * @return The image (for now) with numbers drawn on each cell.
	 */
	public synchronized Mat processZones(Mat img) {
		Imgproc.cvtColor(img, mHSV, Imgproc.COLOR_RGB2HSV_FULL);
		int rows = img.rows();
		int cols = img.cols();

		// Process avg HSV of each submat.
		for (int i = 0; i < GRID_AREA; ++i) {
			double[] hsv = Core.mean(mCells[i]).val;
			mHue[i] = AudioMessenger.normalize((float) hsv[0], 3f, 0f, 255f);
			mSat[i] = AudioMessenger.normalize((float) hsv[1], 10f, 0f, 255f);
			mVal[i] = AudioMessenger.normalize((float) hsv[2], 1f, 0.2f, 255f);
		}
        return img;
	}
	
	/**
	 * Processes the location of all features, assigning a number of features to each grid that contains a 
	 * feature.
	 * @param features  The locations of the features as a series of points.
	 */
	public synchronized void processFeatures(MatOfPoint2f features) {
		// TODO: Process features.
	}
}
