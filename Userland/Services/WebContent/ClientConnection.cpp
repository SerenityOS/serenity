/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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
#include <AK/Debug.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/SystemTheme.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Page/Frame.h>
#include <WebContent/ClientConnection.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <pthread.h>

namespace WebContent {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<WebContentClientEndpoint, WebContentServerEndpoint>(*this, move(socket), client_id)
    , m_page_host(PageHost::create(*this))
{
    s_connections.set(client_id, *this);
    m_paint_flush_timer = Core::Timer::create_single_shot(0, [this] { flush_pending_paint_requests(); });
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
    if (s_connections.is_empty())
        Core::EventLoop::current().quit(0);
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
    return make<Messages::WebContentServer::GreetResponse>();
}

void ClientConnection::handle(const Messages::WebContentServer::UpdateSystemTheme& message)
{
    Gfx::set_system_theme(message.theme_buffer());
    auto impl = Gfx::PaletteImpl::create_with_anonymous_buffer(message.theme_buffer());
    m_page_host->set_palette_impl(*impl);
}

void ClientConnection::handle(const Messages::WebContentServer::LoadURL& message)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadURL: url={}", message.url());

    String process_name;
    if (message.url().host().is_empty())
        process_name = "WebContent";
    else
        process_name = String::formatted("WebContent: {}", message.url().host());

    pthread_setname_np(pthread_self(), process_name.characters());

    page().load(message.url());
}

void ClientConnection::handle(const Messages::WebContentServer::LoadHTML& message)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadHTML: html={}, url={}", message.html(), message.url());
    page().load_html(message.html(), message.url());
}

void ClientConnection::handle(const Messages::WebContentServer::SetViewportRect& message)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::SetViewportRect: rect={}", message.rect());
    m_page_host->set_viewport_rect(message.rect());
}

void ClientConnection::handle(const Messages::WebContentServer::AddBackingStore& message)
{
    m_backing_stores.set(message.backing_store_id(), *message.bitmap().bitmap());
}

void ClientConnection::handle(const Messages::WebContentServer::RemoveBackingStore& message)
{
    m_backing_stores.remove(message.backing_store_id());
}

void ClientConnection::handle(const Messages::WebContentServer::Paint& message)
{
    for (auto& pending_paint : m_pending_paint_requests) {
        if (pending_paint.bitmap_id == message.backing_store_id()) {
            pending_paint.content_rect = message.content_rect();
            return;
        }
    }

    auto it = m_backing_stores.find(message.backing_store_id());
    if (it == m_backing_stores.end()) {
        did_misbehave("Client requested paint with backing store ID");
        return;
    }

    auto& bitmap = *it->value;
    m_pending_paint_requests.append({ message.content_rect(), bitmap, message.backing_store_id() });
    m_paint_flush_timer->start();
}

void ClientConnection::flush_pending_paint_requests()
{
    for (auto& pending_paint : m_pending_paint_requests) {
        m_page_host->paint(pending_paint.content_rect, *pending_paint.bitmap);
        post_message(Messages::WebContentClient::DidPaint(pending_paint.content_rect, pending_paint.bitmap_id));
    }
    m_pending_paint_requests.clear();
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

void ClientConnection::handle(const Messages::WebContentServer::MouseWheel& message)
{
    page().handle_mousewheel(message.position(), message.button(), message.modifiers(), message.wheel_delta());
}

void ClientConnection::handle(const Messages::WebContentServer::KeyDown& message)
{
    page().handle_keydown((KeyCode)message.key(), message.modifiers(), message.code_point());
}

void ClientConnection::handle(const Messages::WebContentServer::DebugRequest& message)
{
    if (message.request() == "dump-dom-tree") {
        if (auto* doc = page().main_frame().document())
            Web::dump_tree(*doc);
    }

    if (message.request() == "dump-layout-tree") {
        if (auto* doc = page().main_frame().document()) {
            if (auto* icb = doc->layout_node())
                Web::dump_tree(*icb);
        }
    }

    if (message.request() == "dump-style-sheets") {
        if (auto* doc = page().main_frame().document()) {
            for (auto& sheet : doc->style_sheets().sheets()) {
                Web::dump_sheet(sheet);
            }
        }
    }

    if (message.request() == "collect-garbage") {
        Web::Bindings::main_thread_vm().heap().collect_garbage(JS::Heap::CollectionType::CollectGarbage, true);
    }

    if (message.request() == "set-line-box-borders") {
        bool state = message.argument() == "on";
        m_page_host->set_should_show_line_box_borders(state);
        page().main_frame().set_needs_display(page().main_frame().viewport_rect());
    }
}

}
