APP_PLATFORM := android-21
APP_STL := c++_shared
NDK_MODULE_PATH=$(APP_PROJECT_PATH)/jni
APP_ABI := arm64-v8a
# Newer clang (NDK r26+) turns these legacy C warnings into errors
APP_CFLAGS += -Wno-error=implicit-function-declaration -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-function-pointer-types
# Align native libraries for 16 KB page size devices (Pixel 8 and later, Android 15+)
APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true
