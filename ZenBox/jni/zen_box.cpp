#include "./zen_box.hpp"

// This is a terrible function that draws some arrows very poorly.  It also changes the global image
// movement vector.
static inline void draw_arrows(vector<Point2f> *p_cur, vector<Point2f> *p_prev, const vector<uchar> &status, Mat *img) {
	flow_vector_q = Point(0, 0);

	//  This right here draws some arrows (poorly).
	for (uint32_t i = 0; i < p_cur->size(); ++i) {
		if (status[i] == 0)
			continue;

		Point2f q = (*p_cur)[i];
		Point2f p = (*p_prev)[i];
		q *= 2;
		p *= 2;
		double angle = atan2(p.y - q.y, p.x - q.x);
		double hyp = sqrt(pow(p.y - q.y, 2) + pow(p.x - q.x, 2));

		// Add the total to the vector sum.
		Point vec(q.x - p.x, q.y - p.y);
		flow_vector_q += vec;

		// Triple the length of the arrow!
		q = Point2f(p.x - 3 * hyp * cos(angle), p.y - 3 * hyp * sin(angle));
		line(*img, Point(p.x, p.y), Point(q.x, q.y),
				Scalar(255, 255, 255, 255));

		// Draw the tips of the arrow.
		circle(*img, Point(q.x, q.y), 5, Scalar(255, 255, 255, 255), 1);
	}
}

inline void draw_flow_vector(Mat *img) {
	// Draw the total vector up in ze center.
	Point _q = Point(flow_vector_q.x / 20.0f, flow_vector_q.y / 20.0f);
	_q += flow_vector_p;
	line(*img, flow_vector_p, _q, Scalar(255, 0, 255, 255), 3);
	circle(*img, _q, 15, Scalar(255, 0, 255, 255), 3);
	std::stringstream ss(std::stringstream::in | std::stringstream::out);
	ss << p_buf.size();
	putText(*img, ss.str(), Point(60, 60), FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255, 0, 255, 255));
}

// Does a great job of drawing some dots....
static inline void draw_dots(const vector<KeyPoint> &points, Mat *img) {
	for (uint32_t i = 0; i < points.size(); ++i) {
		const KeyPoint& p = points[i];
		circle(*img, Point(p.pt.x * 4, p.pt.y * 4), 10, Scalar(255, 255, 255, 255));
	}
}

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_DetectFeatures(JNIEnv*,
		jobject, jlong addrImg, jlong addrGrayImg, jlong addrFeatures, jlong addrFrame) {
		static Mat pyrDownMat;

		// Prepare data.
		Mat& img = *(Mat *) addrImg;
		Mat& grayImg = *(Mat *) addrGrayImg;
		Mat& frame = *(Mat *) addrFrame;

		// Downsample the image.
		pyrDown(img, pyrDownMat);
		cvtColor(pyrDownMat, grayImg, CV_RGBA2GRAY);

		// Detect features and draw dots.
		DETECTOR.detect(grayImg, kp_buf);
		KeyPoint::convert(kp_buf, p_buf);
		*(Mat *) addrFeatures = Mat(p_buf);
		draw_flow_vector(&frame);
}

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv*,
		jobject, jlong addrCurImg, jlong addrPrevGrayImg, jlong addrCurGrayImg, jlong addrPrevFeatures, jlong addrPredictedFeatures) {
	static vector<float> error(MAX_FEATURES);
	static Mat pyrDownMat;

	// Prepare data.
	Mat& curImg = *(Mat *) addrCurImg;
	Mat& prevGrayImg = *(Mat *) addrPrevGrayImg;
	Mat& curGrayImg = *(Mat *) addrCurGrayImg;
	Mat& prevFeatures = *(Mat *) addrPrevFeatures;

	// Detect the features of the previous image, then predict the location
	// on the next image.
	pyrDown(curImg, pyrDownMat);
	cvtColor(pyrDownMat, curGrayImg, CV_RGBA2GRAY);
	calcOpticalFlowPyrLK(prevGrayImg, curGrayImg, prevFeatures, predicted_buf,
			status, error, Size(21, 21), FLOW_MAX_LEVEL, FLOW_TERM_CRITERIA, 0);
	*(Mat *) addrPredictedFeatures = Mat(predicted_buf);  // store predicted features.
	draw_arrows(&predicted_buf, &p_buf, status, &curImg);
	draw_flow_vector(&curImg);
}

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_GetClusters(JNIEnv*,
		jobject, jlong addrImg, jlong addrFrame) {
	Mat& img = *(Mat *) addrImg;
	Mat& frame = *(Mat *) addrFrame;
}
