#include "./zen_box.hpp"

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv*,
																  jobject,
																  jlong addrPrevMat,
																  jlong addrCurMat,
																  jlong addrPrevMatGray,
																  jlong addrCurMatGray,
																  jlong addrPrevFeat,
																  jlong addrCurFeat) {
	static vector<Point2f> p_buf(MAX_FEATURES);
	static vector<Point2f> predicted_buf(MAX_FEATURES);
	static vector<KeyPoint> kp_buf(MAX_FEATURES);
	static vector<uchar> status(MAX_FEATURES);
	static vector<float> error(MAX_FEATURES);

	// Extract member variables from JVM.
	Mat& prevMat    		= *(Mat *) addrPrevMat;			// Previous Matrix.
	Mat& curMat    		 	= *(Mat *) addrCurMat;			// Current Matrix.
	Mat& curMatGray 		= *(Mat *) addrCurMatGray;		// Current Matrix, Gray.
	Mat& prevMatGray 		= *(Mat *) addrPrevMatGray;

	// Detect the features of the current array.
	cvtColor(curMat, curMatGray, CV_RGBA2GRAY);
	cvtColor(prevMat, prevMatGray, CV_RGBA2GRAY);
	OrbFeatureDetector detector(MAX_FEATURES);
	detector.detect(prevMatGray, kp_buf);
	for (uint32_t i = 0; i < kp_buf.size(); ++i) {
		const KeyPoint& p = kp_buf[i];
		circle(curMat, Point(p.pt.x, p.pt.y), 10, Scalar(255, 255, 255, 255));
	}

	KeyPoint::convert(kp_buf, p_buf);
	Mat prevFeat(p_buf);
	calcOpticalFlowPyrLK(prevMatGray, curMatGray, prevFeat, predicted_buf, status, error);
	for (uint32_t i = 0; i < predicted_buf.size(); ++i) {
		if (status[i] == 0)
			continue;

		Point2f q = predicted_buf[i];
		Point2f p = p_buf[i];
		double angle = atan2(p.y - q.y, p.x - q.x);
		double hyp   = sqrt(pow(p.y - q.y, 2) + pow(p.x - q.x, 2));

		// Double the length of the arrow!
		q = Point2f(p.x - 3 * hyp * cos(angle), p.y - 3 * hyp * sin(angle));
		line(curMat, Point(p.x, p.y), Point(q.x, q.y), Scalar(255, 255, 255, 255));

		// Draw the tips of the arrow.
		p = Point2f(q.x + 9 * cos(angle + 3.14159 / 4), (q.y + 9 * sin(angle + 3.14159 / 4)));
		line(curMat, Point(p.x, p.y), Point(q.x, q.y), Scalar(255, 255, 255, 255));
		q = Point2f((q.x + 9 * cos(angle - 3.14159 / 4)), (q.y + 9 * sin(angle - 3.14159 / 4)));
		line(curMat, Point(p.x, p.y), Point(q.x, q.y), Scalar(255, 255, 255, 255));
	}
	*(Mat *) addrCurFeat = Mat(predicted_buf);
}
