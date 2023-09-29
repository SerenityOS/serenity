/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Userland/Libraries/LibWebView/ViewImplementation.h>
#include <android/bitmap.h>
#include <jni.h>

namespace Ladybird {
class WebViewImplementationNative : public WebView::ViewImplementation {
public:
    WebViewImplementationNative(jobject thiz);

    virtual Gfx::IntRect viewport_rect() const override { return m_viewport_rect; }
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint p) const override { return p; }
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint p) const override { return p; }
    virtual void update_zoom() override { }

    NonnullRefPtr<WebView::WebContentClient> bind_web_content_client();

    virtual void create_client(WebView::EnableCallgrindProfiling) override;

    void paint_into_bitmap(void* android_bitmap_raw, AndroidBitmapInfo const& info);

    void set_viewport_geometry(int w, int h);
    void set_device_pixel_ratio(float f);

    static jclass global_class_reference;
    static jmethodID bind_webcontent_method;
    static jmethodID invalidate_layout_method;
    static jmethodID on_load_start_method;

    jobject java_instance() const { return m_java_instance; }

private:
    jobject m_java_instance = nullptr;
    Gfx::IntRect m_viewport_rect;
};
}
