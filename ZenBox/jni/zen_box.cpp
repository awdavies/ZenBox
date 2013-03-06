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
		jobject, jlong addrCurImg, jlong addrPrevGrayImg, jlong addrCurGrayImg,
		jlong addrPrevFeatures, jlong addrPredictedFeatures, jlong addrFrame) {
	static vector<float> error(MAX_FEATURES);
	static Mat pyrDownMat;

	// Prepare data.
	Mat& curImg = *(Mat *) addrCurImg;
	Mat& prevGrayImg = *(Mat *) addrPrevGrayImg;
	Mat& curGrayImg = *(Mat *) addrCurGrayImg;
	Mat& prevFeatures = *(Mat *) addrPrevFeatures;
	Mat &frame = *(Mat *) addrFrame;

	// Detect the features of the previous image, then predict the location
	// on the next image.
	pyrDown(curImg, pyrDownMat);
	cvtColor(pyrDownMat, curGrayImg, CV_RGBA2GRAY);
	calcOpticalFlowPyrLK(prevGrayImg, curGrayImg, prevFeatures, predicted_buf,
			status, error, Size(21, 21), FLOW_MAX_LEVEL, FLOW_TERM_CRITERIA, 0);
	*(Mat *) addrPredictedFeatures = Mat(predicted_buf);  // store predicted features.
	draw_arrows(&predicted_buf, &p_buf, status, &frame);
	draw_flow_vector(&frame);
}

JNIEXPORT void JNICALL Java_com_zenbox_ZenBoxActivity_GetClusters(JNIEnv*,
		jobject, jlong addrImg, jlong addrFrame) {
	Mat& img = *(Mat *) addrImg;
	Mat& frame = *(Mat *) addrFrame;

	// Ultra-down sampling.
	//pyrDown(img, img);
	//pyrDown(img, img);

	Mat meanImg(img.rows, img.cols, CV_32FC3);
	Mat fgImg(img.rows, img.cols, CV_8UC3);

	Mat floatSource;
	img.convertTo(floatSource, CV_32F);

	// convert float image to column vector.
	Mat samples(img.rows * img.cols, 3, CV_32FC1);
	int idx = 0;
	for (int y = 0; y < img.rows; ++y) {
		Vec3f* row = floatSource.ptr<Vec3f>(y);
		for (int x = 0; x < img.cols; ++x) {
			samples.at<Vec3f>(idx++, 0) = row[x];
		}
	}

	// Hard coded 2 or so clusters.
	EM em(2);
	em.train(samples);
	const std::string param1("means");
	const std::string param2("weights");
	Mat means = em.get<Mat>(param1);
	Mat weights = em.get<Mat>(param2);

	const int fgID = weights.at<float>(0) > weights.at<float>(1) ? 0 : 1;

	idx = 0;
	for (int y = 0; y < img.rows; ++y) {
		for (int x = 0; x < img.cols; ++x) {

			// idx 0 is the log likelihood value for the sample.
			// the first element is the index of the most probable
			// mixture component.
			const int result = cvRound(em.predict(samples.row(idx++))[1]);
			const double* ps = means.ptr<double>(result, 0);

			float *pd = meanImg.ptr<float>(y, x);
			pd[0] = ps[0] / 255.0;
			pd[1] = ps[1] / 255.0;
			pd[2] = ps[2] / 255.0;

			if (result == fgID) {
				fgImg.at<Point3_<uchar> >(y, x, 0) = img.at<Point3_<uchar> >(y, x, 0);
			}
		}
	}

	fgImg.convertTo(frame, CV_8UC4);
}
