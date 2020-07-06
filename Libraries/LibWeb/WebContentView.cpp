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

#include "WebContentView.h"
#include "WebContentClient.h"
#include <AK/SharedBuffer.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGfx/SystemTheme.h>

WebContentView::WebContentView()
{
    m_client = WebContentClient::construct(*this);
    client().post_message(Messages::WebContentServer::UpdateSystemTheme(Gfx::current_system_theme_buffer_id()));
}

WebContentView::~WebContentView()
{
}

void WebContentView::load(const URL& url)
{
    m_url = url;
    client().post_message(Messages::WebContentServer::LoadURL(url));
}

void WebContentView::paint_event(GUI::PaintEvent& event)
{
    GUI::ScrollableWidget::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.translate(frame_thickness(), frame_thickness());

    ASSERT(m_front_bitmap);
    painter.blit({ 0, 0 }, *m_front_bitmap, m_front_bitmap->rect());
}

void WebContentView::resize_event(GUI::ResizeEvent& event)
{
    GUI::ScrollableWidget::resize_event(event);

    m_front_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, available_size())->to_bitmap_backed_by_shared_buffer();
    m_front_bitmap->shared_buffer()->share_with(client().server_pid());

    m_back_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, available_size())->to_bitmap_backed_by_shared_buffer();
    m_back_bitmap->shared_buffer()->share_with(client().server_pid());

    client().post_message(Messages::WebContentServer::SetViewportRect(Gfx::IntRect({ horizontal_scrollbar().value(), vertical_scrollbar().value() }, available_size())));
    request_repaint();
}

void WebContentView::mousedown_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseDown(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers()));
}

void WebContentView::mouseup_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseUp(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers()));
}

void WebContentView::mousemove_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseMove(to_content_position(event.position()), event.button(), event.buttons(), event.modifiers()));
}

void WebContentView::notify_server_did_paint(Badge<WebContentClient>, i32 shbuf_id)
{
    if (m_back_bitmap->shbuf_id() == shbuf_id) {
        swap(m_back_bitmap, m_front_bitmap);
        update();
    }
}

void WebContentView::notify_server_did_invalidate_content_rect(Badge<WebContentClient>, [[maybe_unused]] const Gfx::IntRect& content_rect)
{
#ifdef DEBUG_SPAM
    dbg() << "server did invalidate content_rect: " << content_rect << ", current shbuf_id=" << m_bitmap->shbuf_id();
#endif
    request_repaint();
}

void WebContentView::notify_server_did_change_selection(Badge<WebContentClient>)
{
    request_repaint();
}

void WebContentView::notify_server_did_layout(Badge<WebContentClient>, const Gfx::IntSize& content_size)
{
    set_content_size(content_size);
}

void WebContentView::notify_server_did_change_title(Badge<WebContentClient>, const String& title)
{
    if (on_title_change)
        on_title_change(title);
}

void WebContentView::notify_server_did_request_scroll_into_view(Badge<WebContentClient>, const Gfx::IntRect& rect)
{
    scroll_into_view(rect, true, true);
}

void WebContentView::notify_server_did_hover_link(Badge<WebContentClient>, const URL& url)
{
    if (on_link_hover)
        on_link_hover(url);
}

void WebContentView::notify_server_did_unhover_link(Badge<WebContentClient>)
{
    if (on_link_hover)
        on_link_hover({});
}

void WebContentView::notify_server_did_click_link(Badge<WebContentClient>, const URL& url, const String& target, unsigned int modifiers)
{
    if (on_link_click)
        on_link_click(url, target, modifiers);
}

void WebContentView::notify_server_did_middle_click_link(Badge<WebContentClient>, const URL& url, const String& target, unsigned int modifiers)
{
    if (on_link_middle_click)
        on_link_middle_click(url, target, modifiers);
}

void WebContentView::did_scroll()
{
    client().post_message(Messages::WebContentServer::SetViewportRect(visible_content_rect()));
    request_repaint();
}

void WebContentView::request_repaint()
{
    client().post_message(Messages::WebContentServer::Paint(m_back_bitmap->rect().translated(horizontal_scrollbar().value(), vertical_scrollbar().value()), m_back_bitmap->shbuf_id()));
}

WebContentClient& WebContentView::client()
{
    return *m_client;
}
