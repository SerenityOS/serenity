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
#include <LibWeb/Frame/Frame.h>
#include <LibWeb/Layout/LayoutDocument.h>
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
    auto* theme = (Gfx::SystemTheme*)buffer->data();
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

void PageHost::paint(const Gfx::IntRect& content_rect, Gfx::Bitmap& target)
{
    Gfx::Painter painter(target);

    auto* document = page().main_frame().document();
    if (!document)
        return;

    auto* layout_root = document->layout_node();
    if (!layout_root) {
        painter.fill_rect(content_rect, Color::White);
        return;
    }

    painter.fill_rect(content_rect, document->background_color(palette()));

    if (auto background_bitmap = document->background_image()) {
        painter.draw_tiled_bitmap(content_rect, *background_bitmap);
    }

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

}
