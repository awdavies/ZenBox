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
#include <stdint.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <iostream>

using std::vector;
using namespace cv;

/* Constants. */
const uint16_t MAX_FEATURES = 100;
const Scalar FEATURE_COLOR = Scalar(255, 255, 255, 255);

#ifndef ZEN_BOX_HPP_
#define ZEN_BOX_HPP_

/* Prototype Definitions. */
#ifdef __cplusplus   // <--- for name mangling and such.
extern "C" {
#endif

/**
 * Detects the optical flow of the image, centered on the set of features
 * detected between frames.  Requires the previous image, as well as the
 * current image (and the features detected from both frames).  Note it is
 * alright to call this function with some null values, as it will simply
 * return not having done anything if any null values have been passed.
 * This is because most of the variables in this function are required for
 * storage.
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
JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(
		JNIEnv*, jobject, jlong addrPrevMat, jlong addrCurMat, jlong addrCurMatGray, jlong addrPrevFeat, jlong addrCurFeat);

#ifdef __cplusplus
}
#endif

#endif /* ZEN_BOX_HPP_ */
