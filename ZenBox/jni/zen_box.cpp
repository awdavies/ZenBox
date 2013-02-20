#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <iostream>

using std::vector;
using namespace cv;

extern "C" {

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv*, jobject, jlong addrPrevMat, jlong addrCurMat, jlong addrPrevFeat, jlong addrCurFeat);

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv*,
		jobject, jlong addrPrevMat, jlong addrCurMat, jlong addrPrevFeat, jlong addrCurFeat) {
	Mat& prevMat = *(Mat *) addrPrevMat;
	Mat& curMat = *(Mat *) addrCurMat;
	vector<KeyPoint> kpVec;

	OrbFeatureDetector detector(35);
	detector.detect(prevMat, kpVec);
	for (unsigned int i = 0; i < kpVec.size(); ++i) {
		const KeyPoint& kp = kpVec[i];
		circle(curMat, kp.pt, 10, Scalar(255, 255, 255, 255));
	}
}
}


