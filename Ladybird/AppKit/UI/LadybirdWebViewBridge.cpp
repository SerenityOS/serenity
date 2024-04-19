/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Ladybird/HelperProcess.h>
#include <Ladybird/Types.h>
#include <Ladybird/Utilities.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Rect.h>
#include <LibIPC/File.h>
#include <LibWeb/Crypto/Crypto.h>
#include <UI/LadybirdWebViewBridge.h>

#import <UI/Palette.h>

namespace Ladybird {

template<typename T>
static T scale_for_device(T size, float device_pixel_ratio)
{
    return size.template to_type<float>().scaled(device_pixel_ratio).template to_type<int>();
}

ErrorOr<NonnullOwnPtr<WebViewBridge>> WebViewBridge::create(Vector<Web::DevicePixelRect> screen_rects, float device_pixel_ratio, WebContentOptions const& web_content_options, Optional<StringView> webdriver_content_ipc_path, Web::CSS::PreferredColorScheme preferred_color_scheme)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) WebViewBridge(move(screen_rects), device_pixel_ratio, web_content_options, move(webdriver_content_ipc_path), preferred_color_scheme));
}

WebViewBridge::WebViewBridge(Vector<Web::DevicePixelRect> screen_rects, float device_pixel_ratio, WebContentOptions const& web_content_options, Optional<StringView> webdriver_content_ipc_path, Web::CSS::PreferredColorScheme preferred_color_scheme)
    : m_screen_rects(move(screen_rects))
    , m_web_content_options(web_content_options)
    , m_webdriver_content_ipc_path(move(webdriver_content_ipc_path))
    , m_preferred_color_scheme(preferred_color_scheme)
{
    m_device_pixel_ratio = device_pixel_ratio;

    on_scroll_by_delta = [this](auto x_delta, auto y_delta) {
        auto position = m_viewport_rect.location();
        position.set_x(position.x() + x_delta);
        position.set_y(position.y() + y_delta);

        if (on_scroll_to_point)
            on_scroll_to_point(position);
    };

    on_scroll_to_point = [this](auto position) {
        if (on_scroll)
            on_scroll(to_widget_position(position));
    };
}

WebViewBridge::~WebViewBridge() = default;

void WebViewBridge::set_device_pixel_ratio(float device_pixel_ratio)
{
    m_device_pixel_ratio = device_pixel_ratio;
    client().async_set_device_pixels_per_css_pixel(m_client_state.page_index, m_device_pixel_ratio * m_zoom_level);
}

void WebViewBridge::set_system_visibility_state(bool is_visible)
{
    client().async_set_system_visibility_state(m_client_state.page_index, is_visible);
}

void WebViewBridge::set_viewport_rect(Gfx::IntRect viewport_rect, ForResize for_resize)
{
    viewport_rect.set_size(scale_for_device(viewport_rect.size(), m_device_pixel_ratio));
    m_viewport_rect = viewport_rect;

    client().async_set_viewport_rect(m_client_state.page_index, m_viewport_rect.to_type<Web::DevicePixels>());

    if (for_resize == ForResize::Yes) {
        handle_resize();
    }
}

void WebViewBridge::update_palette()
{
    auto theme = create_system_palette();
    client().async_update_system_theme(m_client_state.page_index, move(theme));
}

void WebViewBridge::set_preferred_color_scheme(Web::CSS::PreferredColorScheme color_scheme)
{
    m_preferred_color_scheme = color_scheme;
    client().async_set_preferred_color_scheme(m_client_state.page_index, color_scheme);
}

void WebViewBridge::enqueue_input_event(Web::MouseEvent event)
{
    event.position = to_content_position(event.position.to_type<int>()).to_type<Web::DevicePixels>();
    event.screen_position = to_content_position(event.screen_position.to_type<int>()).to_type<Web::DevicePixels>();
    ViewImplementation::enqueue_input_event(move(event));
}

void WebViewBridge::enqueue_input_event(Web::KeyEvent event)
{
    ViewImplementation::enqueue_input_event(move(event));
}

Optional<WebViewBridge::Paintable> WebViewBridge::paintable()
{
    Gfx::Bitmap* bitmap = nullptr;
    Gfx::IntSize bitmap_size;

    if (m_client_state.has_usable_bitmap) {
        bitmap = m_client_state.front_bitmap.bitmap.ptr();
        bitmap_size = m_client_state.front_bitmap.last_painted_size.to_type<int>();
    } else {
        bitmap = m_backup_bitmap.ptr();
        bitmap_size = m_backup_bitmap_size.to_type<int>();
    }

    if (!bitmap)
        return {};
    return Paintable { *bitmap, bitmap_size };
}

void WebViewBridge::update_zoom()
{
    client().async_set_device_pixels_per_css_pixel(m_client_state.page_index, m_device_pixel_ratio * m_zoom_level);

    if (on_zoom_level_changed)
        on_zoom_level_changed();
}

Web::DevicePixelRect WebViewBridge::viewport_rect() const
{
    return m_viewport_rect.to_type<Web::DevicePixels>();
}

Gfx::IntPoint WebViewBridge::to_content_position(Gfx::IntPoint widget_position) const
{
    return scale_for_device(widget_position, m_device_pixel_ratio);
}

Gfx::IntPoint WebViewBridge::to_widget_position(Gfx::IntPoint content_position) const
{
    return scale_for_device(content_position, inverse_device_pixel_ratio());
}

void WebViewBridge::initialize_client(CreateNewClient)
{
    VERIFY(on_request_web_content);

    // FIXME: Don't create a new process when CreateNewClient is false
    //        We should create a new tab/window in the UI instead, and re-use the existing WebContentClient object.
    m_client_state = {};

    m_client_state.client = on_request_web_content();
    m_client_state.client->on_web_content_process_crash = [this] {
        Core::deferred_invoke([this] {
            handle_web_content_process_crash();
        });
    };

    m_client_state.client_handle = MUST(Web::Crypto::generate_random_uuid());
    client().async_set_window_handle(m_client_state.page_index, m_client_state.client_handle);

    client().async_set_device_pixels_per_css_pixel(m_client_state.page_index, m_device_pixel_ratio);
    client().async_update_system_fonts(m_client_state.page_index, Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), Gfx::FontDatabase::window_title_font_query());
    client().async_set_preferred_color_scheme(m_client_state.page_index, m_preferred_color_scheme);
    update_palette();

    if (!m_screen_rects.is_empty()) {
        // FIXME: Update the screens again if they ever change.
        client().async_update_screen_rects(m_client_state.page_index, m_screen_rects, 0);
    }

    if (m_webdriver_content_ipc_path.has_value()) {
        client().async_connect_to_webdriver(m_client_state.page_index, *m_webdriver_content_ipc_path);
    }
}

}
