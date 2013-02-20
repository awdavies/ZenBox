#include "./zen_box.hpp"

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(
		JNIEnv*, jobject, jlong addrPrevMat, jlong addrCurMat, jlong addrCurMatGray, jlong addrPrevFeat, jlong addrCurFeat) {
	static vector<Point_<float> > p_buf(MAX_FEATURES);
	static vector<KeyPoint> kp_buf(MAX_FEATURES);

	// Extract member variables from JVM.
	Mat& prevMat    		= *(Mat *) addrPrevMat;			// Previous Matrix.
	Mat& curMat    		 	= *(Mat *) addrCurMat;			// Current Matrix.
	Mat& curMatGray 		= *(Mat *) addrCurMatGray;		// Current Matrix, Gray.
	Mat& prevFeat   		= *(Mat *) addrPrevFeat;		// Previous features.
	Mat& curFeat 			= *(Mat *) addrCurFeat;			// Current features.

	// Detect the features of the current array.
	OrbFeatureDetector detector(MAX_FEATURES);
	cvtColor(curMat, curMatGray, CV_RGB2GRAY);
	detector.detect(curMatGray, kp_buf);

	// Set the current data to be equal to the previous data.
	prevFeat = curFeat;
	prevMat = curMat;

	// Convert the key points ( O(n) ) to point2f and then send them to the
	// feature array.  Note that the Point2f conversion has to do with the
	// fact that the Mat class can only have sent to it information with
	// subclasses of DataType.  KeyPoint, sadly, is not a subclass of DataType.
	kpVecToPVec(&kp_buf, &p_buf);
	curFeat = Mat_<Point_<float> >(p_buf);
}
