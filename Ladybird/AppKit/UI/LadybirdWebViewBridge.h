/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/StandardCursor.h>
#include <LibWebView/ViewImplementation.h>

// FIXME: These should not be included outside of Serenity.
#include <Kernel/API/KeyCode.h>
#include <LibGUI/Event.h>

namespace Ladybird {

class WebViewBridge final : public WebView::ViewImplementation {
public:
    static ErrorOr<NonnullOwnPtr<WebViewBridge>> create(Vector<Gfx::IntRect> screen_rects, float device_pixel_ratio, Optional<StringView> webdriver_content_ipc_path);
    virtual ~WebViewBridge() override;

    float device_pixel_ratio() const { return m_device_pixel_ratio; }
    float inverse_device_pixel_ratio() const { return m_inverse_device_pixel_ratio; }

    void set_system_visibility_state(bool is_visible);

    enum class ForResize {
        Yes,
        No,
    };
    void set_viewport_rect(Gfx::IntRect, ForResize = ForResize::No);

    void mouse_down_event(Gfx::IntPoint, GUI::MouseButton, KeyModifier);
    void mouse_up_event(Gfx::IntPoint, GUI::MouseButton, KeyModifier);
    void mouse_move_event(Gfx::IntPoint, GUI::MouseButton, KeyModifier);
    void mouse_double_click_event(Gfx::IntPoint, GUI::MouseButton, KeyModifier);

    void key_down_event(KeyCode, KeyModifier, u32);
    void key_up_event(KeyCode, KeyModifier, u32);

    struct Paintable {
        Gfx::Bitmap& bitmap;
        Gfx::IntSize bitmap_size;
    };
    Optional<Paintable> paintable();

private:
    WebViewBridge(Vector<Gfx::IntRect> screen_rects, float device_pixel_ratio, Optional<StringView> webdriver_content_ipc_path);

    virtual void notify_server_did_finish_handling_input_event(bool event_was_accepted) override;

    virtual void update_zoom() override;
    virtual Gfx::IntRect viewport_rect() const override;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override;

    virtual void create_client(WebView::EnableCallgrindProfiling) override;

    void update_palette();

    Vector<Gfx::IntRect> m_screen_rects;
    Gfx::IntRect m_viewport_rect;

    float m_inverse_device_pixel_ratio { 1.0 };

    Optional<StringView> m_webdriver_content_ipc_path;
};

}
