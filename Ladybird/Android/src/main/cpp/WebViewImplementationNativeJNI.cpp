/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebViewImplementationNative.h"
#include <jni.h>

using namespace Ladybird;

jclass WebViewImplementationNative::global_class_reference;
jmethodID WebViewImplementationNative::bind_webcontent_method;
jmethodID WebViewImplementationNative::invalidate_layout_method;

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_00024Companion_nativeClassInit(JNIEnv* env, jobject /* thiz */)
{
    auto local_class = env->FindClass("org/serenityos/ladybird/WebViewImplementation");
    if (!local_class)
        TODO();
    WebViewImplementationNative::global_class_reference = reinterpret_cast<jclass>(env->NewGlobalRef(local_class));
    env->DeleteLocalRef(local_class);

    auto method = env->GetMethodID(WebViewImplementationNative::global_class_reference, "bindWebContentService", "(II)V");
    if (!method)
        TODO();
    WebViewImplementationNative::bind_webcontent_method = method;

    method = env->GetMethodID(WebViewImplementationNative::global_class_reference, "invalidateLayout", "()V");
    if (!method)
        TODO();
    WebViewImplementationNative::invalidate_layout_method = method;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeObjectInit(JNIEnv* env, jobject thiz)
{
    auto ref = env->NewGlobalRef(thiz);
    auto instance = reinterpret_cast<jlong>(new WebViewImplementationNative(ref));
    return instance;
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeObjectDispose(JNIEnv* env, jobject /* thiz */, jlong instance)
{
    auto* impl = reinterpret_cast<WebViewImplementationNative*>(instance);
    env->DeleteGlobalRef(impl->java_instance());
    delete impl;
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeDrawIntoBitmap(JNIEnv* env, jobject /* thiz */, jlong instance, jobject bitmap)
{
    auto* impl = reinterpret_cast<WebViewImplementationNative*>(instance);

    AndroidBitmapInfo bitmap_info = {};
    void* pixels = nullptr;
    AndroidBitmap_getInfo(env, bitmap, &bitmap_info);
    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    if (pixels)
        impl->paint_into_bitmap(pixels, bitmap_info);

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeSetViewportGeometry(JNIEnv*, jobject /* thiz */, jlong instance, jint w, jint h)
{
    auto* impl = reinterpret_cast<WebViewImplementationNative*>(instance);
    impl->set_viewport_geometry(w, h);
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeLoadURL(JNIEnv* env, jobject /* thiz */, jlong instance, jstring url)
{
    auto* impl = reinterpret_cast<WebViewImplementationNative*>(instance);
    char const* raw_url = env->GetStringUTFChars(url, nullptr);
    auto ak_url = AK::URL::create_with_url_or_path(StringView { raw_url, strlen(raw_url) });
    env->ReleaseStringUTFChars(url, raw_url);
    impl->load(ak_url);
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeSetDevicePixelRatio(JNIEnv*, jobject /* thiz */, jlong instance, jfloat ratio)
{
    auto* impl = reinterpret_cast<WebViewImplementationNative*>(instance);
    impl->set_device_pixel_ratio(ratio);
}
