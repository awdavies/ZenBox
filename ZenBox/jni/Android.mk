LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

OPENCV_INSTALL_MODULES := on
OPENCV_CAMERA_MODULES := on

include $(OPENCVROOT)/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := zen_box
LOCAL_SRC_FILES := \
			zen_box.cpp
LOCAL_LDLIBS += -llog -ldl

include $(BUILD_SHARED_LIBRARY)
