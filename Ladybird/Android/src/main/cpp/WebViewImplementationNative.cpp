/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebView/ViewImplementation.h>
#include <android/log.h>
#include <jni.h>

using namespace WebView;

namespace {
class WebViewImplementationNative : public WebView::ViewImplementation {
public:
    virtual Gfx::IntRect viewport_rect() const override { return {}; }
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint) const override { return {}; }
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint) const override { return {}; }
    virtual void update_zoom() override { }

    static jclass global_class_reference;
    static jfieldID instance_pointer_field;
};
jclass WebViewImplementationNative::global_class_reference;
jfieldID WebViewImplementationNative::instance_pointer_field;
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementationNative_00024Companion_nativeClassInit(JNIEnv* env, jobject /* thiz */)
{
    auto local_class = env->FindClass("org/serenityos/ladybird/WebViewImplementationNative");
    if (!local_class)
        TODO();
    WebViewImplementationNative::global_class_reference = reinterpret_cast<jclass>(env->NewGlobalRef(local_class));

    auto field = env->GetFieldID(WebViewImplementationNative::global_class_reference, "nativeInstance", "J");
    if (!field)
        TODO();
    WebViewImplementationNative::instance_pointer_field = field;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_serenityos_ladybird_WebViewImplementationNative_nativeObjectInit(JNIEnv*, jobject /* thiz */)
{
    auto instance = reinterpret_cast<jlong>(new WebViewImplementationNative);
    __android_log_print(ANDROID_LOG_DEBUG, "Ladybird", "New WebViewImplementationNative at %p", reinterpret_cast<void*>(instance));
    return instance;
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementationNative_nativeObjectDispose(JNIEnv*, jobject /* thiz */, jlong instance)
{
    delete reinterpret_cast<WebViewImplementationNative*>(instance);
    __android_log_print(ANDROID_LOG_DEBUG, "Ladybird", "Destroyed WebViewImplementationNative at %p", reinterpret_cast<void*>(instance));
}
