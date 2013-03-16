/*
 * zen_box.hpp
 *
 * This file contains a few auxiliary functions for extracting image
 * data using native OpenCV code (to be used for basic Optical Flow
 * detection initially.  This code will likely implement the majority of
 * the grunt work for the image processing, returning only a few necessary
 * values).
 *
 *      Author: Andrew Davies
 */
#ifndef ZEN_BOX_HPP_
#define ZEN_BOX_HPP_
#include <jni.h>
#include <math.h>
#include <stdint.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/video/tracking.hpp>
#include <opencv2/video/video.hpp>
#include <vector>
#include <iostream>
#include <sstream>

using std::vector;
using namespace cv;

/* Constants. */
static const int MAX_FEATURES = 64;
static const int INTENSITY_THRESHOLD = 55;
static const Scalar FEATURE_COLOR = Scalar(255, 255, 255, 255);
static const int FLOW_MAX_LEVEL = 3;
static const uint32_t FLOW_MAX_ITER = 9;
static const double FLOW_MIN_EPSILON = 0.35;
static const OrbFeatureDetector DETECTOR(MAX_FEATURES, 1.6f, 3, 26, 0, 2, ORB::FAST_SCORE, 26);

// Some global buffers.  This is a bit of a hack, but it will (read might) add some speedup,
// since optical flow is awfully slow, especially on an android device.
static vector<KeyPoint> kp_buf(MAX_FEATURES);
static vector<Point2f> p_buf(MAX_FEATURES);
static vector<Point2f> predicted_buf(MAX_FEATURES);
static vector<uchar> status(MAX_FEATURES);
static const Point flow_vector_p(320, 240);
static Point flow_vector_q(0, 0);
static bool debug_enabled = false;

// Terminate optical flow after one of these events has occurred first.
// We're not driving a robot car, so none of these values need to be
// extremely robust (hence why all of this is native).
TermCriteria FLOW_TERM_CRITERIA(TermCriteria::COUNT + TermCriteria::EPS,
		FLOW_MAX_ITER, FLOW_MIN_EPSILON);

#ifdef __cplusplus   // <--- for name mangling and such.
extern "C" {
#endif

/**
 * Gets the average HSV of a group of cells within a larger cell.  Meant to
 * speed up (if only slightly) the computation.  There are 'len' cells
 * calculated, and the averages are subsequently stored in the hAvg, sAvg, vAvg
 * arrays.
 */
JNIEXPORT void JNICALL Java_com_zenbox_ZoneProcessor_AvgHSVBatch(JNIEnv* env, jobject,
		jlongArray cellAddrs, jfloatArray hAvg, jfloatArray sAvg, jfloatArray vAvg, jlong len);

/**
 * Toggles debugging in the native code.  This should be called in conjunction
 * with whatever Java code that is intended to toggle debugging in the main
 * activity.
 */
JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_ToggleDebug(JNIEnv*, jobject);

/**
 * Estimates the optical flow between two images.  Requires the current image,
 * the previous and current matrices of the same size for storing the gray
 * version of their respective images (for performance reasons), as well as
 * an array of the currently detected features and a two element array for storing
 * the subsequent flow vector calculated from the process.
 *
 * Warning: there is no parameter checking as of yet.
 */
JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv*,
		jobject, jlong addrCurMat, jlong addrPrevMatGray,
		jlong addrCurMatGray, jlong addrPrevFeat, jlong addrCurFeat, jintArray flowVector);

/**
 * Detects the features using a global feature checker.  Requires the address
 * of the image in question, as well as the address of the gray matrix of the
 * image (it doesn't need to be calculated, it's simply for storage).  After the
 * features are calculated, they are stored in the address of the addrFeatures param.
 *
 * Any drawing will be done to the addrFrame matrix.
 */
JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_DetectFeatures(JNIEnv*,
		jobject, jlong addrImg, jlong addrGrayImg, jlong addrFeatures, jlong addrFrame);

#ifdef __cplusplus
}
#endif

#endif /* ZEN_BOX_HPP_ */
