/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <Ladybird/Types.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibGfx/StandardCursor.h>
#include <LibWeb/CSS/PreferredColorScheme.h>
#include <LibWebView/ViewImplementation.h>

// FIXME: These should not be included outside of Serenity.
#include <Kernel/API/KeyCode.h>
#include <LibGUI/Event.h>

namespace Ladybird {

class WebViewBridge final : public WebView::ViewImplementation {
public:
    static ErrorOr<NonnullOwnPtr<WebViewBridge>> create(Vector<Gfx::IntRect> screen_rects, float device_pixel_ratio, WebContentOptions const&, Optional<StringView> webdriver_content_ipc_path, Web::CSS::PreferredColorScheme);
    virtual ~WebViewBridge() override;

    float device_pixel_ratio() const { return m_device_pixel_ratio; }
    void set_device_pixel_ratio(float device_pixel_ratio);
    float inverse_device_pixel_ratio() const { return 1.0f / m_device_pixel_ratio; }

    void set_system_visibility_state(bool is_visible);

    enum class ForResize {
        Yes,
        No,
    };
    void set_viewport_rect(Gfx::IntRect, ForResize = ForResize::No);

    void update_palette();
    void set_preferred_color_scheme(Web::CSS::PreferredColorScheme);

    void mouse_wheel_event(Gfx::IntPoint, Gfx::IntPoint, GUI::MouseButton, KeyModifier, int, int);
    void mouse_down_event(Gfx::IntPoint, Gfx::IntPoint, GUI::MouseButton, KeyModifier);
    void mouse_up_event(Gfx::IntPoint, Gfx::IntPoint, GUI::MouseButton, KeyModifier);
    void mouse_move_event(Gfx::IntPoint, Gfx::IntPoint, GUI::MouseButton, KeyModifier);
    void mouse_double_click_event(Gfx::IntPoint, Gfx::IntPoint, GUI::MouseButton, KeyModifier);

    void key_down_event(KeyCode, KeyModifier, u32);
    void key_up_event(KeyCode, KeyModifier, u32);

    struct Paintable {
        Gfx::Bitmap& bitmap;
        Gfx::IntSize bitmap_size;
    };
    Optional<Paintable> paintable();

    Function<void()> on_zoom_level_changed;
    Function<void(Gfx::IntPoint)> on_scroll;

private:
    WebViewBridge(Vector<Gfx::IntRect> screen_rects, float device_pixel_ratio, WebContentOptions const&, Optional<StringView> webdriver_content_ipc_path, Web::CSS::PreferredColorScheme);

    virtual void update_zoom() override;
    virtual Gfx::IntRect viewport_rect() const override;
    virtual Gfx::IntPoint to_content_position(Gfx::IntPoint widget_position) const override;
    virtual Gfx::IntPoint to_widget_position(Gfx::IntPoint content_position) const override;

    virtual void create_client() override;

    Vector<Gfx::IntRect> m_screen_rects;
    Gfx::IntRect m_viewport_rect;

    WebContentOptions m_web_content_options;
    Optional<StringView> m_webdriver_content_ipc_path;

    Web::CSS::PreferredColorScheme m_preferred_color_scheme { Web::CSS::PreferredColorScheme::Auto };
};

}
