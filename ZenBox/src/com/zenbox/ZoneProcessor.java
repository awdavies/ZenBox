package com.zenbox;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class ZoneProcessor {
	
	private static final int GRID_WIDTH = 4;
	private static final int GRID_HEIGHT = 4;
	private static final int GRID_AREA = GRID_WIDTH * GRID_HEIGHT;
	
	// Buffers for the number of features in each zone, as well
	// as the overall HSV of the zone.
	public Float[] mNumFeatures;
	public Float[] mHue;
	public Float[] mSat;
	public Float[] mVal;
	
	// Buffers that allow us to call the native batch method.
	// this is here so the floats can be copied to the float object array,
	// which in turn is passable to the audio messenger.  Ugly? Yes.  Fast?  Doubly so!
	private float[] mHueBuf;
	private float[] mSatBuf;
	private float[] mValBuf;
	
	// The cells for each frame.  One frame fills a cell.
	private Mat[] mCells;
	private long[] mCellAddrs;
	
	// Intermediate matrix for image manipulation.
	private Mat mHSV;
	
	public ZoneProcessor(int height, int width) {
		mNumFeatures = new Float[GRID_AREA];
		mHue = new Float[GRID_AREA];
		mSat = new Float[GRID_AREA];
		mVal = new Float[GRID_AREA];
		mHueBuf = new float[GRID_AREA];
		mSatBuf = new float[GRID_AREA];
		mValBuf = new float[GRID_AREA];
		mCells = new Mat[GRID_AREA];
		mCellAddrs = new long[GRID_AREA];
		

		mHSV = new Mat(height, width, CvType.CV_8UC3);
		
		// Chop up img into submats.
		for (int i = 0; i < GRID_HEIGHT; ++i) {
			for (int j = 0; j < GRID_WIDTH; ++j) {
				int k = i * GRID_WIDTH + j;
				mCells[k] = mHSV.submat(i * mHSV.rows() / GRID_HEIGHT,
						(i + 1) * mHSV.rows() / GRID_HEIGHT,
						j * mHSV.cols() / GRID_WIDTH,
						(j + 1) * mHSV.cols() / GRID_WIDTH);
				
				// Init each cell address.
				mCellAddrs[k] = mCells[k].getNativeObjAddr();
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
		AvgHSVBatch(mCellAddrs, mHueBuf, mSatBuf, mValBuf, GRID_AREA);
		for (int i = 0; i < GRID_AREA; ++i) {
			Log.i("ZoneProcessor", "Hue: " + mHueBuf[i]);
			mHue[i] = mHueBuf[i];
			mSat[i] = mSatBuf[i];
			mVal[i] = mValBuf[i];
		}
        return img;
	}
	
	public synchronized void setNumFeatures(Mat img, MatOfPoint2f features) {
		// (little hacky), get the total width and height of each one of the columns.
		int width = mCells[0].width();
		int height = mCells[0].height();
		
		// Zero out array first.
		for (int i = 0; i < mNumFeatures.length; ++i) {
			mNumFeatures[i] = 0f;
		}
		
		// Set the values for each square appropriately.  This is also a bit of a hack,
		// as all of the points accrued through the image have been downsampled;  hence the
		// *2 at the end of the x/y values.
		Point[] points = features.toArray();
		for (int i = 0; i < points.length; ++i) {
			final Point p = points[i];
			int x = (int) Math.floor((p.x * 2) / width);
			int y = (int) Math.floor((p.y * 2) / height);
			int k = y * GRID_WIDTH + x;
			++mNumFeatures[k];
		}
	}
	
	/**
	 * Cleans up resources used by the ZoneProcessor matrices.
	 */
	public void cleanup() {
		mHSV.release();
	}
	
	// Native method for getting avg of each cell, because 16+ native calls are slow as
	// frozen tar....
	public native void AvgHSVBatch(long[] cellAddrs, float[] hAvg, float[] sAvg, float[] vAvg, long len);
}
