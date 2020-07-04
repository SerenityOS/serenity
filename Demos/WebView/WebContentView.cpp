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
    client().post_message(Messages::WebContentServer::LoadURL(url));
}

void WebContentView::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    ASSERT(m_bitmap);
    painter.blit({ 0, 0 }, *m_bitmap, m_bitmap->rect());
}

void WebContentView::resize_event(GUI::ResizeEvent& event)
{
    GUI::Widget::resize_event(event);
    auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, event.size());
    m_bitmap = bitmap->to_bitmap_backed_by_shared_buffer();
    m_bitmap->shared_buffer()->share_with(client().server_pid());
    client().post_message(Messages::WebContentServer::SetViewportRect(Gfx::IntRect({ horizontal_scrollbar().value(), vertical_scrollbar().value() }, event.size())));
    request_repaint();
}

void WebContentView::mousedown_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseDown(event.position(), event.button(), event.buttons(), event.modifiers()));
}

void WebContentView::mouseup_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseUp(event.position(), event.button(), event.buttons(), event.modifiers()));
}

void WebContentView::mousemove_event(GUI::MouseEvent& event)
{
    client().post_message(Messages::WebContentServer::MouseMove(event.position(), event.button(), event.buttons(), event.modifiers()));
}

void WebContentView::notify_server_did_paint(Badge<WebContentClient>, i32 shbuf_id)
{
    if (m_bitmap->shbuf_id() == shbuf_id)
        update();
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

void WebContentView::did_scroll()
{
    client().post_message(Messages::WebContentServer::SetViewportRect(Gfx::IntRect({ horizontal_scrollbar().value(), vertical_scrollbar().value() }, size())));
    request_repaint();
}

void WebContentView::request_repaint()
{
    client().post_message(Messages::WebContentServer::Paint(m_bitmap->rect().translated(horizontal_scrollbar().value(), vertical_scrollbar().value()), m_bitmap->shbuf_id()));
}

WebContentClient& WebContentView::client()
{
    return *m_client;
}
