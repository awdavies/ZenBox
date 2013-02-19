LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include Z:/eclipse_proj/OpenCV-2.4.3.2-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := zen_box
LOCAL_SRC_FILES := zen_box.cpp

include $(BUILD_SHARED_LIBRARY)
