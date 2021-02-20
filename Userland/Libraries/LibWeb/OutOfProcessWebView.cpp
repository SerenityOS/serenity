/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "OutOfProcessWebView.h"
#include "WebContentClient.h"
#include <AK/String.h>
#include <AK/URLParser.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Window.h>
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

void OutOfProcessWebView::create_client()
{
    m_client_state = {};

    m_client_state.client = WebContentClient::construct(*this);
    m_client_state.client->on_web_content_process_crash = [this] {
        create_client();
        ASSERT(m_client_state.client);

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
        builder.appendff("The web page <a href=\"{}\">{}</a> has crashed.<br><br>You can reload the page to try again.", escape_html_entities(m_url.to_string_encoded()), escape_html_entities(m_url.to_string()));
        builder.append("</body></html>");
        load_html(builder.to_string(), m_url);
    };

    client().post_message(Messages::WebContentServer::UpdateSystemTheme(Gfx::current_system_theme_buffer()));
}

void OutOfProcessWebView::load(const URL& url)
{
    m_url = url;
    client().post_message(Messages::WebContentServer::LoadURL(url));
}

void OutOfProcessWebView::load_html(const StringView& html, const URL& url)
{
    m_url = url;
    client().post_message(Messages::WebContentServer::LoadHTML(html, url));
}

void OutOfProcessWebView::load_empty_document()
{
    m_url = {};
    client().post_message(Messages::WebContentServer::LoadHTML("", {}));
}

void OutOfProcessWebView::paint_event(GUI::PaintEvent& event)
{
    GUI::ScrollableWidget::paint_event(event);

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
    GUI::ScrollableWidget::resize_event(event);
    handle_resize();
}

void OutOfProcessWebView::handle_resize()
{
    client().post_message(Messages::WebContentServer::SetViewportRect(Gfx::IntRect({ horizontal_scrollbar().value(), vertical_scrollbar().value() }, available_size())));

    if (m_client_state.has_usable_bitmap) {
        // NOTE: We keep the outgoing front bitmap as a backup so we have something to paint until we get a new one.
        m_backup_bitmap = m_client_state.front_bitmap;
    }

    if (m_client_state.front_bitmap) {
        m_client_state.front_bitmap = nullptr;
        client().post_message(Messages::WebContentServer::RemoveBackingStore(m_client_state.front_bitmap_id));
    }

    if (m_client_state.back_bitmap) {
        m_client_state.back_bitmap = nullptr;
        client().post_message(Messages::WebContentServer::RemoveBackingStore(m_client_state.back_bitmap_id));
    }

    m_client_state.front_bitmap_id = -1;
    m_client_state.back_bitmap_id = -1;
    m_client_state.has_usable_bitmap = false;

    if (available_size().is_empty())
        return;

    if (auto new_bitmap = Gfx::Bitmap::create_shareable(Gfx::BitmapFormat::RGB32, available_size())) {
        m_client_state.front_bitmap = move(new_bitmap);
        m_client_state.front_bitmap_id = m_client_state.next_bitmap_id++;
        client().post_message(Messages::WebContentServer::AddBackingStore(m_client_state.front_bitmap_id, m_client_state.front_bitmap->to_shareable_bitmap()));
    }

    if (auto new_bitmap = Gfx::Bitmap::create_shareable(Gfx::BitmapFormat::RGB32, available_size())) {
        m_client_state.back_bitmap = move(new_bitmap);
        m_client_state.back_bitmap_id = m_client_state.next_bitmap_id++;
        client().post_message(Messages::WebContentServer::AddBackingStore(m_client_state.back_bitmap_id, m_client_state.back_bitmap->to_shareable_bitmap()));
    }

    request_repaint();
}

void OutOfProcessWebView::keydown_event(GUI::KeyEvent& event)
{
    client().post_message(Messages::WebContentServer::KeyDown(event.key(), event.modifiers(), event.code_point()));
}

void OutOfProcessWebView::mousedown_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseDown(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers()));
}

void OutOfProcessWebView::mouseup_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseUp(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers()));
}

void OutOfProcessWebView::mousemove_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseMove(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers()));
}

void OutOfProcessWebView::theme_change_event(GUI::ThemeChangeEvent& event)
{
    GUI::ScrollableWidget::theme_change_event(event);
    client().post_message(Messages::WebContentServer::UpdateSystemTheme(Gfx::current_system_theme_buffer()));
    request_repaint();
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

void OutOfProcessWebView::notify_server_did_layout(Badge<WebContentClient>, const Gfx::IntSize& content_size)
{
    set_content_size(content_size);
}

void OutOfProcessWebView::notify_server_did_change_title(Badge<WebContentClient>, const String& title)
{
    if (on_title_change)
        on_title_change(title);
}

void OutOfProcessWebView::notify_server_did_request_scroll_into_view(Badge<WebContentClient>, const Gfx::IntRect& rect)
{
    scroll_into_view(rect, true, true);
}

void OutOfProcessWebView::notify_server_did_hover_link(Badge<WebContentClient>, const URL& url)
{
    set_override_cursor(Gfx::StandardCursor::Hand);
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

void OutOfProcessWebView::did_scroll()
{
    client().post_message(Messages::WebContentServer::SetViewportRect(visible_content_rect()));
    request_repaint();
}

void OutOfProcessWebView::request_repaint()
{
    // If this widget was instantiated but not yet added to a window,
    // it won't have a back bitmap yet, so we can just skip repaint requests.
    if (!m_client_state.back_bitmap)
        return;
    client().post_message(Messages::WebContentServer::Paint(m_client_state.back_bitmap->rect().translated(horizontal_scrollbar().value(), vertical_scrollbar().value()), m_client_state.back_bitmap_id));
}

WebContentClient& OutOfProcessWebView::client()
{
    ASSERT(m_client_state.client);
    return *m_client_state.client;
}

void OutOfProcessWebView::debug_request(const String& request, const String& argument)
{
    client().post_message(Messages::WebContentServer::DebugRequest(request, argument));
}

}
