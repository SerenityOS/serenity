/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "OutOfProcessWebView.h"
#include "WebContentClient.h"
#include <AK/String.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

REGISTER_WIDGET(Web, OutOfProcessWebView)

namespace Web {

OutOfProcessWebView::OutOfProcessWebView()
{
    set_should_hide_unnecessary_scrollbars(true);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);

    create_client();
}

OutOfProcessWebView::~OutOfProcessWebView()
{
}

void OutOfProcessWebView::handle_web_content_process_crash()
{
    create_client();
    VERIFY(m_client_state.client);

    // Don't keep a stale backup bitmap around.
    m_backup_bitmap = nullptr;

    handle_resize();
    StringBuilder builder;
    builder.append("<html><head><title>Crashed: ");
    builder.append(escape_html_entities(m_url.to_string()));
    builder.append("</title></head><body>");
    builder.append("<h1>Web page crashed");
    if (!m_url.host().is_empty()) {
        builder.appendff(" on {}", escape_html_entities(m_url.host()));
    }
    builder.append("</h1>");
    auto escaped_url = escape_html_entities(m_url.to_string());
    builder.appendff("The web page <a href=\"{}\">{}</a> has crashed.<br><br>You can reload the page to try again.", escaped_url, escaped_url);
    builder.append("</body></html>");
    load_html(builder.to_string(), m_url);
}

void OutOfProcessWebView::create_client()
{
    m_client_state = {};

    m_client_state.client = WebContentClient::construct(*this);
    m_client_state.client->on_web_content_process_crash = [this] {
        deferred_invoke([this](auto&) {
            handle_web_content_process_crash();
        });
    };

    client().async_update_system_theme(Gfx::current_system_theme_buffer());
    client().async_update_system_fonts(Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query());
    client().async_update_screen_rect(GUI::Desktop::the().rect());
}

void OutOfProcessWebView::load(const URL& url)
{
    m_url = url;
    client().async_load_url(url);
}

void OutOfProcessWebView::load_html(const StringView& html, const URL& url)
{
    m_url = url;
    client().async_load_html(html, url);
}

void OutOfProcessWebView::load_empty_document()
{
    m_url = {};
    client().async_load_html("", {});
}

void OutOfProcessWebView::paint_event(GUI::PaintEvent& event)
{
    GUI::AbstractScrollableWidget::paint_event(event);

    // If the available size is empty, we don't have a front or back bitmap to draw.
    if (available_size().is_empty())
        return;

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (auto* bitmap = m_client_state.has_usable_bitmap ? m_client_state.front_bitmap.ptr() : m_backup_bitmap.ptr()) {
        painter.add_clip_rect(frame_inner_rect());
        painter.translate(frame_thickness(), frame_thickness());
        painter.blit({ 0, 0 }, *bitmap, bitmap->rect());
        return;
    }

    painter.fill_rect(frame_inner_rect(), palette().base());
}

void OutOfProcessWebView::resize_event(GUI::ResizeEvent& event)
{
    GUI::AbstractScrollableWidget::resize_event(event);
    handle_resize();
}

void OutOfProcessWebView::handle_resize()
{
    client().async_set_viewport_rect(Gfx::IntRect({ horizontal_scrollbar().value(), vertical_scrollbar().value() }, available_size()));

    if (m_client_state.has_usable_bitmap) {
        // NOTE: We keep the outgoing front bitmap as a backup so we have something to paint until we get a new one.
        m_backup_bitmap = m_client_state.front_bitmap;
    }

    if (m_client_state.front_bitmap) {
        m_client_state.front_bitmap = nullptr;
        client().async_remove_backing_store(m_client_state.front_bitmap_id);
    }

    if (m_client_state.back_bitmap) {
        m_client_state.back_bitmap = nullptr;
        client().async_remove_backing_store(m_client_state.back_bitmap_id);
    }

    m_client_state.front_bitmap_id = -1;
    m_client_state.back_bitmap_id = -1;
    m_client_state.has_usable_bitmap = false;

    if (available_size().is_empty())
        return;

    if (auto new_bitmap = Gfx::Bitmap::create_shareable(Gfx::BitmapFormat::BGRx8888, available_size())) {
        m_client_state.front_bitmap = move(new_bitmap);
        m_client_state.front_bitmap_id = m_client_state.next_bitmap_id++;
        client().async_add_backing_store(m_client_state.front_bitmap_id, m_client_state.front_bitmap->to_shareable_bitmap());
    }

    if (auto new_bitmap = Gfx::Bitmap::create_shareable(Gfx::BitmapFormat::BGRx8888, available_size())) {
        m_client_state.back_bitmap = move(new_bitmap);
        m_client_state.back_bitmap_id = m_client_state.next_bitmap_id++;
        client().async_add_backing_store(m_client_state.back_bitmap_id, m_client_state.back_bitmap->to_shareable_bitmap());
    }

    request_repaint();
}

void OutOfProcessWebView::keydown_event(GUI::KeyEvent& event)
{
    client().async_key_down(event.key(), event.modifiers(), event.code_point());
}

void OutOfProcessWebView::mousedown_event(GUI::MouseEvent& event)
{
    client().async_mouse_down(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers());
}

void OutOfProcessWebView::mouseup_event(GUI::MouseEvent& event)
{
    client().async_mouse_up(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers());
}

void OutOfProcessWebView::mousemove_event(GUI::MouseEvent& event)
{
    client().async_mouse_move(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers());
}

void OutOfProcessWebView::mousewheel_event(GUI::MouseEvent& event)
{
    client().async_mouse_wheel(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers(), event.wheel_delta());
}

void OutOfProcessWebView::theme_change_event(GUI::ThemeChangeEvent& event)
{
    GUI::AbstractScrollableWidget::theme_change_event(event);
    client().async_update_system_theme(Gfx::current_system_theme_buffer());
    request_repaint();
}

void OutOfProcessWebView::screen_rect_change_event(GUI::ScreenRectChangeEvent& event)
{
    client().async_update_screen_rect(event.rect());
}

void OutOfProcessWebView::notify_server_did_paint(Badge<WebContentClient>, i32 bitmap_id)
{
    if (m_client_state.back_bitmap_id == bitmap_id) {
        m_client_state.has_usable_bitmap = true;
        swap(m_client_state.back_bitmap, m_client_state.front_bitmap);
        swap(m_client_state.back_bitmap_id, m_client_state.front_bitmap_id);
        // We don't need the backup bitmap anymore, so drop it.
        m_backup_bitmap = nullptr;
        update();
    }
}

void OutOfProcessWebView::notify_server_did_invalidate_content_rect(Badge<WebContentClient>, [[maybe_unused]] const Gfx::IntRect& content_rect)
{
    request_repaint();
}

void OutOfProcessWebView::notify_server_did_change_selection(Badge<WebContentClient>)
{
    request_repaint();
}

void OutOfProcessWebView::notify_server_did_request_cursor_change(Badge<WebContentClient>, Gfx::StandardCursor cursor)
{
    set_override_cursor(cursor);
}

void OutOfProcessWebView::notify_server_did_layout(Badge<WebContentClient>, const Gfx::IntSize& content_size)
{
    set_content_size(content_size);
}

void OutOfProcessWebView::notify_server_did_change_title(Badge<WebContentClient>, const String& title)
{
    if (on_title_change)
        on_title_change(title);
}

void OutOfProcessWebView::notify_server_did_request_scroll(Badge<WebContentClient>, int wheel_delta)
{
    vertical_scrollbar().set_value(vertical_scrollbar().value() + wheel_delta * 20);
}

void OutOfProcessWebView::notify_server_did_request_scroll_into_view(Badge<WebContentClient>, const Gfx::IntRect& rect)
{
    scroll_into_view(rect, true, true);
}

void OutOfProcessWebView::notify_server_did_enter_tooltip_area(Badge<WebContentClient>, const Gfx::IntPoint&, const String& title)
{
    GUI::Application::the()->show_tooltip(title, nullptr);
}

void OutOfProcessWebView::notify_server_did_leave_tooltip_area(Badge<WebContentClient>)
{
    GUI::Application::the()->hide_tooltip();
}

void OutOfProcessWebView::notify_server_did_hover_link(Badge<WebContentClient>, const URL& url)
{
    if (on_link_hover)
        on_link_hover(url);
}

void OutOfProcessWebView::notify_server_did_unhover_link(Badge<WebContentClient>)
{
    set_override_cursor(Gfx::StandardCursor::None);
    if (on_link_hover)
        on_link_hover({});
}

void OutOfProcessWebView::notify_server_did_click_link(Badge<WebContentClient>, const URL& url, const String& target, unsigned int modifiers)
{
    if (on_link_click)
        on_link_click(url, target, modifiers);
}

void OutOfProcessWebView::notify_server_did_middle_click_link(Badge<WebContentClient>, const URL& url, const String& target, unsigned int modifiers)
{
    if (on_link_middle_click)
        on_link_middle_click(url, target, modifiers);
}

void OutOfProcessWebView::notify_server_did_start_loading(Badge<WebContentClient>, const URL& url)
{
    if (on_load_start)
        on_load_start(url);
}

void OutOfProcessWebView::notify_server_did_finish_loading(Badge<WebContentClient>, const URL& url)
{
    if (on_load_finish)
        on_load_finish(url);
}

void OutOfProcessWebView::notify_server_did_request_context_menu(Badge<WebContentClient>, const Gfx::IntPoint& content_position)
{
    if (on_context_menu_request)
        on_context_menu_request(screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void OutOfProcessWebView::notify_server_did_request_link_context_menu(Badge<WebContentClient>, const Gfx::IntPoint& content_position, const URL& url, const String&, unsigned)
{
    if (on_link_context_menu_request)
        on_link_context_menu_request(url, screen_relative_rect().location().translated(to_widget_position(content_position)));
}

void OutOfProcessWebView::notify_server_did_request_image_context_menu(Badge<WebContentClient>, const Gfx::IntPoint& content_position, const URL& url, const String&, unsigned, const Gfx::ShareableBitmap& bitmap)
{
    if (on_image_context_menu_request)
        on_image_context_menu_request(url, screen_relative_rect().location().translated(to_widget_position(content_position)), bitmap);
}

void OutOfProcessWebView::notify_server_did_request_alert(Badge<WebContentClient>, const String& message)
{
    GUI::MessageBox::show(window(), message, "Alert", GUI::MessageBox::Type::Information);
}

bool OutOfProcessWebView::notify_server_did_request_confirm(Badge<WebContentClient>, const String& message)
{
    auto confirm_result = GUI::MessageBox::show(window(), message, "Confirm", GUI::MessageBox::Type::Warning, GUI::MessageBox::InputType::OKCancel);
    return confirm_result == GUI::Dialog::ExecResult::ExecOK;
}

String OutOfProcessWebView::notify_server_did_request_prompt(Badge<WebContentClient>, const String& message, const String& default_)
{
    String response { default_ };
    if (GUI::InputBox::show(window(), response, message, "Prompt") == GUI::InputBox::ExecOK)
        return response;
    return {};
}

void OutOfProcessWebView::notify_server_did_get_source(const URL& url, const String& source)
{
    if (on_get_source)
        on_get_source(url, source);
}

void OutOfProcessWebView::notify_server_did_js_console_output(const String& method, const String& line)
{
    if (on_js_console_output)
        on_js_console_output(method, line);
}

void OutOfProcessWebView::notify_server_did_change_favicon(const Gfx::Bitmap& favicon)
{
    if (on_favicon_change)
        on_favicon_change(favicon);
}

String OutOfProcessWebView::notify_server_did_request_cookie(Badge<WebContentClient>, const URL& url, Cookie::Source source)
{
    if (on_get_cookie)
        return on_get_cookie(url, source);
    return {};
}

void OutOfProcessWebView::notify_server_did_set_cookie(Badge<WebContentClient>, const URL& url, const Cookie::ParsedCookie& cookie, Cookie::Source source)
{
    if (on_set_cookie)
        on_set_cookie(url, cookie, source);
}

void OutOfProcessWebView::did_scroll()
{
    client().async_set_viewport_rect(visible_content_rect());
    request_repaint();
}

void OutOfProcessWebView::request_repaint()
{
    // If this widget was instantiated but not yet added to a window,
    // it won't have a back bitmap yet, so we can just skip repaint requests.
    if (!m_client_state.back_bitmap)
        return;
    client().async_paint(m_client_state.back_bitmap->rect().translated(horizontal_scrollbar().value(), vertical_scrollbar().value()), m_client_state.back_bitmap_id);
}

WebContentClient& OutOfProcessWebView::client()
{
    VERIFY(m_client_state.client);
    return *m_client_state.client;
}

void OutOfProcessWebView::debug_request(const String& request, const String& argument)
{
    client().async_debug_request(request, argument);
}

void OutOfProcessWebView::get_source()
{
    client().async_get_source();
}

void OutOfProcessWebView::js_console_initialize()
{
    client().async_js_console_initialize();
}

void OutOfProcessWebView::js_console_input(const String& js_source)
{
    client().async_js_console_input(js_source);
}

}
