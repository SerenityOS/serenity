/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "JNIHelpers.h"
#include <Userland/Libraries/LibGfx/Bitmap.h>
#include <Userland/Libraries/LibGfx/Painter.h>
#include <Userland/Libraries/LibWeb/Crypto/Crypto.h>
#include <Userland/Libraries/LibWebView/ViewImplementation.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>

namespace {

Gfx::BitmapFormat to_gfx_bitmap_format(i32 f)
{
    switch (f) {
    case ANDROID_BITMAP_FORMAT_RGBA_8888:
        return Gfx::BitmapFormat::BGRA8888;
    default:
        VERIFY_NOT_REACHED();
    }
}

class WebViewImplementationNative : public WebView::ViewImplementation {
public:
    WebViewImplementationNative(jobject thiz)
        : m_java_instance(thiz)
    {
        // NOTE: m_java_instance's global ref is controlled by the JNI bindings
        create_client(WebView::EnableCallgrindProfiling::No);
    }

    virtual Gfx::IntRect viewport_rect() const override { return m_viewport_rect; }
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint p) const override { return p; }
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint p) const override { return p; }
    virtual void update_zoom() override { }

    NonnullRefPtr<WebView::WebContentClient> bind_web_content_client();

    virtual void create_client(WebView::EnableCallgrindProfiling) override
    {
        m_client_state = {};

        auto new_client = bind_web_content_client();

        m_client_state.client = new_client;
        m_client_state.client->on_web_content_process_crash = [] {
            __android_log_print(ANDROID_LOG_ERROR, "Ladybird", "WebContent crashed!");
            // FIXME: launch a new client
        };

        m_client_state.client_handle = MUST(Web::Crypto::generate_random_uuid());
        client().async_set_window_handle(m_client_state.client_handle);

        client().async_set_device_pixels_per_css_pixel(m_device_pixel_ratio);

        // FIXME: update_palette, update system fonts
    }

    void paint_into_bitmap(void* android_bitmap_raw, AndroidBitmapInfo const& info)
    {
        // Software bitmaps only for now!
        VERIFY((info.flags & ANDROID_BITMAP_FLAGS_IS_HARDWARE) == 0);

        auto android_bitmap = MUST(Gfx::Bitmap::create_wrapper(to_gfx_bitmap_format(info.format), { info.width, info.height }, 1, info.stride, android_bitmap_raw));
        Gfx::Painter painter(android_bitmap);
        if (auto* bitmap = m_client_state.has_usable_bitmap ? m_client_state.front_bitmap.bitmap.ptr() : m_backup_bitmap.ptr())
            painter.blit({ 0, 0 }, *bitmap, bitmap->rect());
        else
            painter.clear_rect(painter.clip_rect(), Gfx::Color::Magenta);

        // Convert our internal BGRA into RGBA. This will be slowwwwwww
        // FIXME: Don't do a color format swap here.
        for (auto y = 0; y < android_bitmap->height(); ++y) {
            auto* scanline = android_bitmap->scanline(y);
            for (auto x = 0; x < android_bitmap->width(); ++x) {
                auto current_pixel = scanline[x];
                u32 alpha = (current_pixel & 0xFF000000U) >> 24;
                u32 red = (current_pixel & 0x00FF0000U) >> 16;
                u32 green = (current_pixel & 0x0000FF00U) >> 8;
                u32 blue = (current_pixel & 0x000000FFU);
                scanline[x] = (alpha << 24U) | (blue << 16U) | (green << 8U) | red;
            }
        }
    }

    void set_viewport_geometry(int w, int h)
    {
        m_viewport_rect = { { 0, 0 }, { w, h } };
    }

    static jclass global_class_reference;
    static jfieldID instance_pointer_field;
    static jmethodID bind_webcontent_method;
    static JavaVM* global_vm;

    jobject java_instance() const { return m_java_instance; }

private:
    jobject m_java_instance = nullptr;
    Gfx::IntRect m_viewport_rect;
};
jclass WebViewImplementationNative::global_class_reference;
jfieldID WebViewImplementationNative::instance_pointer_field;
jmethodID WebViewImplementationNative::bind_webcontent_method;
JavaVM* WebViewImplementationNative::global_vm;

NonnullRefPtr<WebView::WebContentClient> WebViewImplementationNative::bind_web_content_client()
{
    JavaEnvironment env(WebViewImplementationNative::global_vm);

    int socket_fds[2] {};
    MUST(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int wc_fd = socket_fds[1];

    int fd_passing_socket_fds[2] {};
    MUST(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fd_passing_socket_fds));

    int ui_fd_passing_fd = fd_passing_socket_fds[0];
    int wc_fd_passing_fd = fd_passing_socket_fds[1];

    // NOTE: The java object takes ownership of the socket fds
    env.get()->CallVoidMethod(m_java_instance, bind_webcontent_method, wc_fd, wc_fd_passing_fd);

    auto socket = MUST(Core::LocalSocket::adopt_fd(ui_fd));
    MUST(socket->set_blocking(true));

    auto new_client = make_ref_counted<WebView::WebContentClient>(move(socket), *this);
    new_client->set_fd_passing_socket(MUST(Core::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    return new_client;
}

}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_00024Companion_nativeClassInit(JNIEnv* env, jobject /* thiz */)
{
    auto ret = env->GetJavaVM(&WebViewImplementationNative::global_vm);
    if (ret != 0)
        TODO();

    auto local_class = env->FindClass("org/serenityos/ladybird/WebViewImplementation");
    if (!local_class)
        TODO();
    WebViewImplementationNative::global_class_reference = reinterpret_cast<jclass>(env->NewGlobalRef(local_class));
    env->DeleteLocalRef(local_class);

    auto field = env->GetFieldID(WebViewImplementationNative::global_class_reference, "nativeInstance", "J");
    if (!field)
        TODO();
    WebViewImplementationNative::instance_pointer_field = field;

    auto method = env->GetMethodID(WebViewImplementationNative::global_class_reference, "bindWebContentService", "(II)V");
    if (!method)
        TODO();
    WebViewImplementationNative::bind_webcontent_method = method;
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeObjectInit(JNIEnv* env, jobject thiz)
{
    auto ref = env->NewGlobalRef(thiz);
    auto instance = reinterpret_cast<jlong>(new WebViewImplementationNative(ref));
    __android_log_print(ANDROID_LOG_DEBUG, "Ladybird", "New WebViewImplementationNative at %p", reinterpret_cast<void*>(instance));
    return instance;
}

extern "C" JNIEXPORT void JNICALL
Java_org_serenityos_ladybird_WebViewImplementation_nativeObjectDispose(JNIEnv* env, jobject /* thiz */, jlong instance)
{
    auto* impl = reinterpret_cast<WebViewImplementationNative*>(instance);
    env->DeleteGlobalRef(impl->java_instance());
    delete impl;
    __android_log_print(ANDROID_LOG_DEBUG, "Ladybird", "Destroyed WebViewImplementationNative at %p", reinterpret_cast<void*>(instance));
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
