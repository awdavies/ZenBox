#include "./zen_box.hpp"

// This is a terrible function that draws some arrows very poorly.  It also changes the global image
// movement vector.
static inline void calc_flow_vector(vector<Point2f> *p_cur, vector<Point2f> *p_prev, const vector<uchar> &status, Mat *img) {
	flow_vector_q = Point(0, 0);

	//  This right here draws some arrows (poorly).
	for (uint32_t i = 0; i < p_cur->size(); ++i) {
		if (status[i] == 0)
			continue;

		// Add the total to the vector sum.
		Point2f q = (*p_cur)[i];
		Point2f p = (*p_prev)[i];
		Point vec(q.x - p.x, q.y - p.y);
		flow_vector_q += vec;

		if (!debug_enabled)
			continue;

		q *= 2;
		p *= 2;
		double angle = atan2(p.y - q.y, p.x - q.x);
		double hyp = sqrt(pow(p.y - q.y, 2) + pow(p.x - q.x, 2));

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
		circle(*img, Point(p.pt.x * 2, p.pt.y * 2), 10, Scalar(255, 255, 255, 255));
	}
}

JNIEXPORT void JNICALL Java_com_zenbox_ZoneProcessor_AvgHSVBatch(JNIEnv* env, jobject,
		jlongArray cellAddrs, jfloatArray hAvg, jfloatArray sAvg, jfloatArray vAvg, jlong len) {
	jlong *cells = env->GetLongArrayElements(cellAddrs, NULL);
	float *h = env->GetFloatArrayElements(hAvg, NULL);
	float *s = env->GetFloatArrayElements(sAvg, NULL);
	float *v = env->GetFloatArrayElements(vAvg, NULL);
	for (long i = 0; i < len; ++i) {
		Mat &m = *(Mat *) cells[i];
		Scalar hsvMean = mean(m);
		h[i] = hsvMean.val[0];
		s[i] = hsvMean.val[1];
		v[i] = hsvMean.val[2];
	}
	env->ReleaseLongArrayElements(cellAddrs, cells, 0);
	env->ReleaseFloatArrayElements(hAvg, h, 0);
	env->ReleaseFloatArrayElements(sAvg, s, 0);
	env->ReleaseFloatArrayElements(vAvg, v, 0);
}

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_ToggleDebug(JNIEnv*, jobject) {
	debug_enabled = !debug_enabled;
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
		if (debug_enabled)
			draw_dots(kp_buf, &frame);
}

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_OpticalFlow(JNIEnv* env,
		jobject, jlong addrCurImg, jlong addrPrevGrayImg, jlong addrCurGrayImg, jlong addrPrevFeatures, jlong addrPredictedFeatures, jintArray flowVector) {
	static vector<float> error(MAX_FEATURES);
	static Mat pyrDownMat;
	jint *nativeFlowVector;

	// Prepare data.
	Mat& curImg = *(Mat *) addrCurImg;
	Mat& prevGrayImg = *(Mat *) addrPrevGrayImg;
	Mat& curGrayImg = *(Mat *) addrCurGrayImg;
	Mat& prevFeatures = *(Mat *) addrPrevFeatures;

	{
		nativeFlowVector = env->GetIntArrayElements(flowVector, NULL);
		nativeFlowVector[0] = flow_vector_q.x;
		nativeFlowVector[1] = flow_vector_q.y;
		env->ReleaseIntArrayElements(flowVector, nativeFlowVector, 0);
	}

	// Detect the features of the previous image, then predict the location
	// on the next image.
	pyrDown(curImg, pyrDownMat);
	cvtColor(pyrDownMat, curGrayImg, CV_RGBA2GRAY);
	calcOpticalFlowPyrLK(prevGrayImg, curGrayImg, prevFeatures, predicted_buf,
			status, error, Size(21, 21), FLOW_MAX_LEVEL, FLOW_TERM_CRITERIA, 0);
	*(Mat *) addrPredictedFeatures = Mat(predicted_buf);  // store predicted features.

	calc_flow_vector(&predicted_buf, &p_buf, status, &curImg);
	if (debug_enabled) {
		draw_flow_vector(&curImg);
	}
}
