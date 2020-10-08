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
#include <AK/SharedBuffer.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Window.h>
#include <LibGfx/SystemTheme.h>

REGISTER_WIDGET(Web, OutOfProcessWebView)

namespace Web {

OutOfProcessWebView::OutOfProcessWebView()
{
    set_should_hide_unnecessary_scrollbars(true);
    m_client = WebContentClient::construct(*this);
    client().post_message(Messages::WebContentServer::UpdateSystemTheme(Gfx::current_system_theme_buffer_id()));
}

OutOfProcessWebView::~OutOfProcessWebView()
{
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

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.translate(frame_thickness(), frame_thickness());

    ASSERT(m_front_bitmap);
    painter.blit({ 0, 0 }, *m_front_bitmap, m_front_bitmap->rect());
}

void OutOfProcessWebView::resize_event(GUI::ResizeEvent& event)
{
    GUI::ScrollableWidget::resize_event(event);

    m_front_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, available_size())->to_bitmap_backed_by_shared_buffer();
    m_front_bitmap->shared_buffer()->share_with(client().server_pid());

    m_back_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, available_size())->to_bitmap_backed_by_shared_buffer();
    m_back_bitmap->shared_buffer()->share_with(client().server_pid());

    client().post_message(Messages::WebContentServer::SetViewportRect(Gfx::IntRect({ horizontal_scrollbar().value(), vertical_scrollbar().value() }, available_size())));
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
    client().post_message(Messages::WebContentServer::UpdateSystemTheme(Gfx::current_system_theme_buffer_id()));
    request_repaint();
}

void OutOfProcessWebView::notify_server_did_paint(Badge<WebContentClient>, i32 shbuf_id)
{
    if (m_back_bitmap->shbuf_id() == shbuf_id) {
        swap(m_back_bitmap, m_front_bitmap);
        update();
    }
}

void OutOfProcessWebView::notify_server_did_invalidate_content_rect(Badge<WebContentClient>, [[maybe_unused]] const Gfx::IntRect& content_rect)
{
#ifdef DEBUG_SPAM
    dbg() << "server did invalidate content_rect: " << content_rect << ", current front_shbuf_id=" << m_front_bitmap->shbuf_id() << ", current back_shbuf_id=" << m_back_bitmap->shbuf_id();
#endif
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

void OutOfProcessWebView::did_scroll()
{
    client().post_message(Messages::WebContentServer::SetViewportRect(visible_content_rect()));
    request_repaint();
}

void OutOfProcessWebView::request_repaint()
{
    client().post_message(Messages::WebContentServer::Paint(m_back_bitmap->rect().translated(horizontal_scrollbar().value(), vertical_scrollbar().value()), m_back_bitmap->shbuf_id()));
}

WebContentClient& OutOfProcessWebView::client()
{
    return *m_client;
}

}
