#include "./zen_box.hpp"

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv*,
		jobject, jlong addrPrevMat, jlong addrCurMat, jlong addrPrevMatGray,
		jlong addrCurMatGray, jlong addrPrevFeat, jlong addrCurFeat) {

	// Some static function variables.  Since this is not 2011 standard, this likely isn't thread
	// safe, so watch out...  Also, there's probably a better way to deal with this stuff.
	static vector<Point2f> p_prev(MAX_FEATURES);
	static vector<Point2f> p_cur(MAX_FEATURES);
	static vector<Point2f> predicted_buf(MAX_FEATURES);
	static vector<KeyPoint> kp_prev(MAX_FEATURES);
	static vector<KeyPoint> kp_cur(MAX_FEATURES);
	static vector<uchar> status(MAX_FEATURES);
	static vector<float> error(MAX_FEATURES);
	static ORB detector(MAX_FEATURES);

	// Extract member variables from JVM.
	Mat& prevMat = *(Mat *) addrPrevMat;			// Previous Matrix.
	Mat& curMat = *(Mat *) addrCurMat;			// Current Matrix.
	Mat& curMatGray = *(Mat *) addrCurMatGray;		// Current Matrix, Gray.
	Mat& prevMatGray = *(Mat *) addrPrevMatGray;

	// Detect the features of the current array.
	cvtColor(curMat, curMatGray, CV_RGBA2GRAY);
	cvtColor(prevMat, prevMatGray, CV_RGBA2GRAY);
	detector.detect(prevMatGray, kp_prev);
	detector.detect(curMatGray, kp_cur);
	for (uint32_t i = 0; i < kp_prev.size(); ++i) {
		const KeyPoint& p = kp_prev[i];
		circle(curMat, Point(p.pt.x, p.pt.y), 10, Scalar(255, 255, 255, 255));
	}

	KeyPoint::convert(kp_prev, p_prev);
	KeyPoint::convert(kp_cur, p_cur);
	Mat prevFeat(p_prev);
	Mat curFeat(p_cur);
	calcOpticalFlowPyrLK(prevMatGray, curMatGray, prevFeat, curFeat,
			status, error, Size(21, 21), FLOW_MAX_LEVEL, FLOW_TERM_CRITERIA, 0);
	for (uint32_t i = 0; i < p_cur.size(); ++i) {
		if (status[i] == 0)
			continue;

		Point2f q = p_cur[i];
		Point2f p = p_prev[i];
		double angle = atan2(p.y - q.y, p.x - q.x);
		double hyp = sqrt(pow(p.y - q.y, 2) + pow(p.x - q.x, 2));

		// Double the length of the arrow!
		q = Point2f(p.x - 3 * hyp * cos(angle), p.y - 3 * hyp * sin(angle));
		line(curMat, Point(p.x, p.y), Point(q.x, q.y),
				Scalar(255, 255, 255, 255));

		// Draw the tips of the arrow.
		p = Point2f(q.x + 9 * cos(angle + 3.14159 / 4),
				(q.y + 9 * sin(angle + 3.14159 / 4)));
		line(curMat, Point(p.x, p.y), Point(q.x, q.y),
				Scalar(255, 255, 255, 255));
		q = Point2f((q.x + 9 * cos(angle - 3.14159 / 4)),
				(q.y + 9 * sin(angle - 3.14159 / 4)));
		line(curMat, Point(p.x, q.y), Point(q.x, q.y),
				Scalar(255, 255, 255, 255));
	}
	*(Mat *) addrCurFeat = curFeat;
}
