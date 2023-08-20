/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Ladybird/HelperProcess.h>
#include <Ladybird/Types.h>
#include <Ladybird/Utilities.h>
#include <LibCore/File.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Rect.h>
#include <LibIPC/File.h>
#include <LibWeb/Crypto/Crypto.h>
#include <UI/LadybirdWebViewBridge.h>

namespace Ladybird {

template<typename T>
static T scale_for_device(T size, float device_pixel_ratio)
{
    return size.template to_type<float>().scaled(device_pixel_ratio).template to_type<int>();
}

ErrorOr<NonnullOwnPtr<WebViewBridge>> WebViewBridge::create(Vector<Gfx::IntRect> screen_rects, float device_pixel_ratio, Optional<StringView> webdriver_content_ipc_path)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) WebViewBridge(move(screen_rects), device_pixel_ratio, move(webdriver_content_ipc_path)));
}

WebViewBridge::WebViewBridge(Vector<Gfx::IntRect> screen_rects, float device_pixel_ratio, Optional<StringView> webdriver_content_ipc_path)
    : m_screen_rects(move(screen_rects))
    , m_webdriver_content_ipc_path(move(webdriver_content_ipc_path))
{
    m_device_pixel_ratio = device_pixel_ratio;
    m_inverse_device_pixel_ratio = 1.0 / device_pixel_ratio;

    create_client(WebView::EnableCallgrindProfiling::No);
}

WebViewBridge::~WebViewBridge() = default;

void WebViewBridge::set_system_visibility_state(bool is_visible)
{
    client().async_set_system_visibility_state(is_visible);
}

void WebViewBridge::set_viewport_rect(Gfx::IntRect viewport_rect, ForResize for_resize)
{
    viewport_rect.set_size(scale_for_device(viewport_rect.size(), m_device_pixel_ratio));
    m_viewport_rect = viewport_rect;

    client().async_set_viewport_rect(m_viewport_rect);
    request_repaint();

    if (for_resize == ForResize::Yes) {
        handle_resize();
    }
}

void WebViewBridge::mouse_down_event(Gfx::IntPoint position, GUI::MouseButton button, KeyModifier modifiers)
{
    client().async_mouse_down(to_content_position(position), to_underlying(button), to_underlying(button), modifiers);
}

void WebViewBridge::mouse_up_event(Gfx::IntPoint position, GUI::MouseButton button, KeyModifier modifiers)
{
    client().async_mouse_up(to_content_position(position), to_underlying(button), to_underlying(button), modifiers);
}

void WebViewBridge::mouse_move_event(Gfx::IntPoint position, GUI::MouseButton button, KeyModifier modifiers)
{
    client().async_mouse_move(to_content_position(position), 0, to_underlying(button), modifiers);
}

void WebViewBridge::mouse_double_click_event(Gfx::IntPoint position, GUI::MouseButton button, KeyModifier modifiers)
{
    client().async_doubleclick(to_content_position(position), button, to_underlying(button), modifiers);
}

void WebViewBridge::key_down_event(KeyCode key_code, KeyModifier modifiers, u32 code_point)
{
    client().async_key_down(key_code, modifiers, code_point);
}

void WebViewBridge::key_up_event(KeyCode key_code, KeyModifier modifiers, u32 code_point)
{
    client().async_key_up(key_code, modifiers, code_point);
}

Optional<WebViewBridge::Paintable> WebViewBridge::paintable()
{
    Gfx::Bitmap* bitmap = nullptr;
    Gfx::IntSize bitmap_size;

    if (m_client_state.has_usable_bitmap) {
        bitmap = m_client_state.front_bitmap.bitmap.ptr();
        bitmap_size = m_client_state.front_bitmap.last_painted_size;
    } else {
        bitmap = m_backup_bitmap.ptr();
        bitmap_size = m_backup_bitmap_size;
    }

    if (!bitmap)
        return {};
    return Paintable { *bitmap, bitmap_size };
}

void WebViewBridge::notify_server_did_layout(Badge<WebView::WebContentClient>, Gfx::IntSize content_size)
{
    if (on_layout) {
        content_size = scale_for_device(content_size, m_inverse_device_pixel_ratio);
        on_layout(content_size);
    }
}

void WebViewBridge::notify_server_did_paint(Badge<WebView::WebContentClient>, i32 bitmap_id, Gfx::IntSize size)
{
    if (m_client_state.back_bitmap.id == bitmap_id) {
        m_client_state.has_usable_bitmap = true;
        m_client_state.back_bitmap.pending_paints--;
        m_client_state.back_bitmap.last_painted_size = size;
        swap(m_client_state.back_bitmap, m_client_state.front_bitmap);
        // We don't need the backup bitmap anymore, so drop it.
        m_backup_bitmap = nullptr;

        if (on_ready_to_paint)
            on_ready_to_paint();

        if (m_client_state.got_repaint_requests_while_painting) {
            m_client_state.got_repaint_requests_while_painting = false;
            request_repaint();
        }
    }
}

void WebViewBridge::notify_server_did_invalidate_content_rect(Badge<WebView::WebContentClient>, Gfx::IntRect const&)
{
    request_repaint();
}

void WebViewBridge::notify_server_did_change_selection(Badge<WebView::WebContentClient>)
{
    request_repaint();
}

void WebViewBridge::notify_server_did_request_cursor_change(Badge<WebView::WebContentClient>, Gfx::StandardCursor cursor)
{
    if (on_cursor_change)
        on_cursor_change(cursor);
}

void WebViewBridge::notify_server_did_request_scroll(Badge<WebView::WebContentClient>, i32 x_delta, i32 y_delta)
{
    // FIXME: This currently isn't reached because we do not yet propagate mouse wheel events to WebContent.
    //        When that is implemented, make sure our mutations to the viewport position here are correct.
    auto position = m_viewport_rect.location();
    position.set_x(position.x() + x_delta);
    position.set_y(position.y() + y_delta);

    if (on_scroll)
        on_scroll(position);
}

void WebViewBridge::notify_server_did_request_scroll_to(Badge<WebView::WebContentClient>, Gfx::IntPoint position)
{
    if (on_scroll)
        on_scroll(position);
}

void WebViewBridge::notify_server_did_request_scroll_into_view(Badge<WebView::WebContentClient>, Gfx::IntRect const& rect)
{
    if (m_viewport_rect.contains(rect))
        return;

    auto position = m_viewport_rect.location();

    if (rect.top() < m_viewport_rect.top()) {
        position.set_y(rect.top());
    } else if (rect.top() > m_viewport_rect.top() && rect.bottom() > m_viewport_rect.bottom()) {
        position.set_y(rect.bottom() - m_viewport_rect.height());
    } else {
        return;
    }

    if (on_scroll)
        on_scroll(position);
}

void WebViewBridge::notify_server_did_enter_tooltip_area(Badge<WebView::WebContentClient>, Gfx::IntPoint, DeprecatedString const& tooltip)
{
    if (on_tooltip_entered)
        on_tooltip_entered(tooltip);
}

void WebViewBridge::notify_server_did_leave_tooltip_area(Badge<WebView::WebContentClient>)
{
    if (on_tooltip_left)
        on_tooltip_left();
}

void WebViewBridge::notify_server_did_request_alert(Badge<WebView::WebContentClient>, String const& message)
{
    if (on_alert)
        on_alert(message);
}

void WebViewBridge::alert_closed()
{
    client().async_alert_closed();
}

void WebViewBridge::notify_server_did_request_confirm(Badge<WebView::WebContentClient>, String const& message)
{
    if (on_confirm)
        on_confirm(message);
}

void WebViewBridge::confirm_closed(bool accepted)
{
    client().async_confirm_closed(accepted);
}

void WebViewBridge::notify_server_did_request_prompt(Badge<WebView::WebContentClient>, String const& message, String const& default_)
{
    if (on_prompt)
        on_prompt(message, default_);
}

void WebViewBridge::prompt_closed(Optional<String> response)
{
    client().async_prompt_closed(move(response));
}

void WebViewBridge::notify_server_did_request_set_prompt_text(Badge<WebView::WebContentClient>, String const& message)
{
    if (on_prompt_text_changed)
        on_prompt_text_changed(message);
}

void WebViewBridge::notify_server_did_request_accept_dialog(Badge<WebView::WebContentClient>)
{
    if (on_dialog_accepted)
        on_dialog_accepted();
}

void WebViewBridge::notify_server_did_request_dismiss_dialog(Badge<WebView::WebContentClient>)
{
    if (on_dialog_dismissed)
        on_dialog_dismissed();
}

void WebViewBridge::notify_server_did_request_file(Badge<WebView::WebContentClient>, DeprecatedString const& path, i32 request_id)
{
    auto file = Core::File::open(path, Core::File::OpenMode::Read);

    if (file.is_error())
        client().async_handle_file_return(file.error().code(), {}, request_id);
    else
        client().async_handle_file_return(0, IPC::File(*file.value()), request_id);
}

void WebViewBridge::notify_server_did_finish_handling_input_event(bool)
{
}

void WebViewBridge::update_zoom()
{
}

Gfx::IntRect WebViewBridge::viewport_rect() const
{
    return m_viewport_rect;
}

Gfx::IntPoint WebViewBridge::to_content_position(Gfx::IntPoint widget_position) const
{
    return scale_for_device(widget_position, m_device_pixel_ratio);
}

Gfx::IntPoint WebViewBridge::to_widget_position(Gfx::IntPoint content_position) const
{
    return scale_for_device(content_position, m_inverse_device_pixel_ratio);
}

void WebViewBridge::create_client(WebView::EnableCallgrindProfiling enable_callgrind_profiling)
{
    m_client_state = {};

    auto candidate_web_content_paths = MUST(get_paths_for_helper_process("WebContent"sv));
    auto new_client = MUST(launch_web_content_process(*this, candidate_web_content_paths, enable_callgrind_profiling, WebView::IsLayoutTestMode::No, Ladybird::UseLagomNetworking::Yes));

    m_client_state.client = new_client;
    m_client_state.client->on_web_content_process_crash = [this] {
        Core::deferred_invoke([this] {
            handle_web_content_process_crash();
        });
    };

    m_client_state.client_handle = MUST(Web::Crypto::generate_random_uuid());
    client().async_set_window_handle(m_client_state.client_handle);

    client().async_set_device_pixels_per_css_pixel(m_device_pixel_ratio);
    client().async_update_system_fonts(Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), Gfx::FontDatabase::window_title_font_query());
    update_palette();

    if (!m_screen_rects.is_empty()) {
        // FIXME: Update the screens again if they ever change.
        client().async_update_screen_rects(m_screen_rects, 0);
    }

    if (m_webdriver_content_ipc_path.has_value()) {
        client().async_connect_to_webdriver(*m_webdriver_content_ipc_path);
    }
}

void WebViewBridge::update_palette()
{
    auto theme = MUST(Gfx::load_system_theme(DeprecatedString::formatted("{}/res/themes/Default.ini", s_serenity_resource_root)));
    auto palette_impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme);
    auto palette = Gfx::Palette(move(palette_impl));

    client().async_update_system_theme(move(theme));
}

}
