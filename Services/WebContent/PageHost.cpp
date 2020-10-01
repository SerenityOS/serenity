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

#include "PageHost.h"
#include "ClientConnection.h"
#include <AK/SharedBuffer.h>
#include <LibGfx/Painter.h>
#include <LibGfx/SystemTheme.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Page/Frame.h>
#include <WebContent/WebContentClientEndpoint.h>

namespace WebContent {

PageHost::PageHost(ClientConnection& client)
    : m_client(client)
    , m_page(make<Web::Page>(*this))
{
    setup_palette();
}

PageHost::~PageHost()
{
}

void PageHost::setup_palette()
{
    // FIXME: Get the proper palette from our peer somehow
    auto buffer = SharedBuffer::create_with_size(sizeof(Gfx::SystemTheme));
    auto* theme = buffer->data<Gfx::SystemTheme>();
    theme->color[(int)Gfx::ColorRole::Window] = Color::Magenta;
    theme->color[(int)Gfx::ColorRole::WindowText] = Color::Cyan;
    m_palette_impl = Gfx::PaletteImpl::create_with_shared_buffer(*buffer);
}

Gfx::Palette PageHost::palette() const
{
    return Gfx::Palette(*m_palette_impl);
}

void PageHost::set_palette_impl(const Gfx::PaletteImpl& impl)
{
    m_palette_impl = impl;
}

Web::LayoutDocument* PageHost::layout_root()
{
    auto* document = page().main_frame().document();
    if (!document)
        return nullptr;
    return document->layout_node();
}

void PageHost::paint(const Gfx::IntRect& content_rect, Gfx::Bitmap& target)
{
    Gfx::Painter painter(target);
    Gfx::IntRect bitmap_rect { {}, content_rect.size() };

    auto* layout_root = this->layout_root();
    if (!layout_root) {
        painter.fill_rect(bitmap_rect, Color::White);
        return;
    }

    painter.fill_rect(bitmap_rect, layout_root->document().background_color(palette()));

    if (auto background_bitmap = layout_root->document().background_image()) {
        painter.draw_tiled_bitmap(bitmap_rect, *background_bitmap);
    }

    painter.translate(-content_rect.x(), -content_rect.y());

    Web::PaintContext context(painter, palette(), Gfx::IntPoint());
    context.set_viewport_rect(content_rect);
    layout_root->paint_all_phases(context);
}

void PageHost::set_viewport_rect(const Gfx::IntRect& rect)
{
    page().main_frame().set_size(rect.size());
    if (page().main_frame().document())
        page().main_frame().document()->layout();
    page().main_frame().set_viewport_rect(rect);
}

void PageHost::page_did_invalidate(const Gfx::IntRect& content_rect)
{
    m_client.post_message(Messages::WebContentClient::DidInvalidateContentRect(content_rect));
}

void PageHost::page_did_change_selection()
{
    m_client.post_message(Messages::WebContentClient::DidChangeSelection());
}

void PageHost::page_did_layout()
{
    auto* layout_root = this->layout_root();
    ASSERT(layout_root);
    auto content_size = enclosing_int_rect(layout_root->absolute_rect()).size();
    m_client.post_message(Messages::WebContentClient::DidLayout(content_size));
}

void PageHost::page_did_change_title(const String& title)
{
    m_client.post_message(Messages::WebContentClient::DidChangeTitle(title));
}

void PageHost::page_did_request_scroll_into_view(const Gfx::IntRect& rect)
{
    m_client.post_message(Messages::WebContentClient::DidRequestScrollIntoView(rect));
}

void PageHost::page_did_hover_link(const URL& url)
{
    m_client.post_message(Messages::WebContentClient::DidHoverLink(url));
}

void PageHost::page_did_unhover_link()
{
    m_client.post_message(Messages::WebContentClient::DidUnhoverLink());
}

void PageHost::page_did_click_link(const URL& url, const String& target, unsigned modifiers)
{
    m_client.post_message(Messages::WebContentClient::DidClickLink(url, target, modifiers));
}

void PageHost::page_did_middle_click_link(const URL& url, [[maybe_unused]] const String& target, [[maybe_unused]] unsigned modifiers)
{
    m_client.post_message(Messages::WebContentClient::DidMiddleClickLink(url, target, modifiers));
}

void PageHost::page_did_start_loading(const URL& url)
{
    m_client.post_message(Messages::WebContentClient::DidStartLoading(url));
}

void PageHost::page_did_request_context_menu(const Gfx::IntPoint& content_position)
{
    m_client.post_message(Messages::WebContentClient::DidRequestContextMenu(content_position));
}

void PageHost::page_did_request_link_context_menu(const Gfx::IntPoint& content_position, const URL& url, const String& target, unsigned modifiers)
{
    m_client.post_message(Messages::WebContentClient::DidRequestLinkContextMenu(content_position, url, target, modifiers));
}

void PageHost::page_did_request_alert(const String& message)
{
    m_client.send_sync<Messages::WebContentClient::DidRequestAlert>(message);
}

}
