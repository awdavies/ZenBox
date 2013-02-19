#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <iostream>

using std::vector;

extern "C" {

JNIEXPORT void JNICALL Java_com_zenbox_ZenboxActivity_OpticalFlow();

JNIEXPORT void JNICALL Java_com_zenbox_ZenboxActicity_OpticalFlow() {
	std::cout << "THIS IS A TEST" << std::endl;
}
}


