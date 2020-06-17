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

#include <AK/Badge.h>
#include <AK/SharedBuffer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/SystemTheme.h>
#include <WebContent/ClientConnection.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebContentClientEndpoint.h>

namespace WebContent {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(Core::LocalSocket& socket, int client_id)
    : IPC::ClientConnection<WebContentServerEndpoint>(*this, socket, client_id)
    , m_page_host(PageHost::create(*this))
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

Web::Page& ClientConnection::page()
{
    return m_page_host->page();
}

const Web::Page& ClientConnection::page() const
{
    return m_page_host->page();
}

OwnPtr<Messages::WebContentServer::GreetResponse> ClientConnection::handle(const Messages::WebContentServer::Greet&)
{
    return make<Messages::WebContentServer::GreetResponse>(client_id());
}

void ClientConnection::handle(const Messages::WebContentServer::UpdateSystemTheme& message)
{
    auto shared_buffer = SharedBuffer::create_from_shbuf_id(message.shbuf_id());
    if (!shared_buffer) {
        dbg() << "WebContentServer::UpdateSystemTheme: SharedBuffer already gone! Ignoring :^)";
        return;
    }
    Gfx::set_system_theme(*shared_buffer);
    auto impl = Gfx::PaletteImpl::create_with_shared_buffer(*shared_buffer);
    m_page_host->set_palette_impl(*impl);
}

void ClientConnection::handle(const Messages::WebContentServer::LoadURL& message)
{
    dbg() << "handle: WebContentServer::LoadURL: url=" << message.url();
    page().load(message.url());
}

void ClientConnection::handle(const Messages::WebContentServer::SetViewportRect& message)
{
    dbg() << "handle: WebContentServer::SetViewportRect: rect=" << message.rect();
    m_page_host->set_viewport_rect(message.rect());
}

void ClientConnection::handle(const Messages::WebContentServer::Paint& message)
{
    dbg() << "handle: WebContentServer::Paint: content_rect=" << message.content_rect() << ", shbuf_id=" << message.shbuf_id();

    auto shared_buffer = SharedBuffer::create_from_shbuf_id(message.shbuf_id());
    if (!shared_buffer) {
        dbg() << "WebContentServer::Paint: SharedBuffer already gone! Ignoring :^)";
        return;
    }
    auto shared_bitmap = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGB32, shared_buffer.release_nonnull(), message.content_rect().size());
    if (!shared_bitmap) {
        did_misbehave("WebContentServer::Paint: Cannot create Gfx::Bitmap wrapper around SharedBuffer");
        return;
    }
    m_page_host->paint(message.content_rect(), *shared_bitmap);
    post_message(Messages::WebContentClient::DidPaint(message.content_rect(), message.shbuf_id()));
}

void ClientConnection::handle(const Messages::WebContentServer::MouseDown& message)
{
    page().handle_mousedown(message.position(), message.button(), message.modifiers());
}

void ClientConnection::handle(const Messages::WebContentServer::MouseMove& message)
{
    page().handle_mousemove(message.position(), message.buttons(), message.modifiers());
}

void ClientConnection::handle(const Messages::WebContentServer::MouseUp& message)
{
    page().handle_mouseup(message.position(), message.button(), message.modifiers());
}

}
