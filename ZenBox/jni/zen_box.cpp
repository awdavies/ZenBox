#include "./zen_box.hpp"

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(
		JNIEnv*, jobject, jlong addrPrevMat, jlong addrCurMat, jlong addrCurMatGray, jlong addrPrevFeat, jlong addrCurFeat) {
	// Extract member variables from JVM.
	Mat& prevMat = *(Mat *) addrPrevMat;			// Previous Matrix.
	Mat& curMat = *(Mat *) addrCurMat;				// Current Matrix.
	Mat& curMatGray = *(Mat *) addrCurMatGray;		// Current Matrix, Gray.
	vector<KeyPoint> kpVec;

	OrbFeatureDetector detector(MAX_FEATURES);
	detector.detect(prevMat, kpVec);
	for (unsigned int i = 0; i < kpVec.size(); ++i) {
		const KeyPoint& kp = kpVec[i];
		circle(curMat, kp.pt, 10, Scalar(255, 255, 255, 255));
	}
}
