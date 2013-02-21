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

using std::vector;
using namespace cv;

/* Constants. */
const int MAX_FEATURES = 10;
const Scalar FEATURE_COLOR = Scalar(255, 255, 255, 255);
const int FLOW_MAX_LEVEL = 3;
const uint32_t FLOW_MAX_ITER = 10;
const double FLOW_MIN_EPSILON = 0.5;

// Terminate optical flow after one of these events has occurred first.
// We're not driving a robot car, so none of these values need to be
// extremely robust (hence why all of this is native).
TermCriteria FLOW_TERM_CRITERIA(TermCriteria::COUNT + TermCriteria::EPS,
		FLOW_MAX_ITER, FLOW_MIN_EPSILON);

#ifndef ZEN_BOX_HPP_
#define ZEN_BOX_HPP_

#ifdef __cplusplus   // <--- for name mangling and such.
extern "C" {
#endif

/**
 * Detects the optical flow of the image, centered on the set of features
 * detected between frames.  Requires the previous image, as well as the
 * current image (and the features detected from both frames).  Note that
 * any null or invalid values passed to this function will only cause havoc.
 * There will be no address checking; you have been warned.
 *
 * @param addrPrevMat The address of the matrix of the previous frame.
 *
 * @param addrCurMat The address of the matrix of the current frame.
 *
 * @param addrCurMatGray The address of the space to be used for converting
 * 	the current image frame to grayscale.  This is to aid with robust feature
 * 	detection.  Note this matrix need not be used outside of this function, and
 * 	is simply necessary for space reasons (preventing allocation per every
 * 	frame).
 *
 * @param addrPrevFeat The address of the previous image's features.  Note this will be
 * 	modified by this function, and it is not intended for the caller to do any changes,
 * 	and will only need to allocate the space for the object.
 *
 * @param addrCurFeat The address of the current image's features.  Note this will be
 * 	modified by this function, and it is not intended for the caller to do any changes,
 * 	and will only need to allocate the space for the object.
 */
JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv*,
		jobject, jlong addrPrevMat, jlong addrCurMat, jlong addrPrevMatGray,
		jlong addrCurMatGray, jlong addrPrevFeat, jlong addrCurFeat);

#ifdef __cplusplus
}
#endif

#endif /* ZEN_BOX_HPP_ */
