/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/Debug.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/SystemTheme.h>
#include <LibJS/Console.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/BrowsingContext.h>
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

void ClientConnection::update_system_theme(const Core::AnonymousBuffer& theme_buffer)
{
    Gfx::set_system_theme(theme_buffer);
    auto impl = Gfx::PaletteImpl::create_with_anonymous_buffer(theme_buffer);
    m_page_host->set_palette_impl(*impl);
}

void ClientConnection::update_system_fonts(String const& default_font_query, String const& fixed_width_font_query)
{
    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);
}

void ClientConnection::update_screen_rect(const Gfx::IntRect& rect)
{
    m_page_host->set_screen_rect(rect);
}

void ClientConnection::load_url(const URL& url)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadURL: url={}", url);

    String process_name;
    if (url.host().is_empty())
        process_name = "WebContent";
    else
        process_name = String::formatted("WebContent: {}", url.host());

    pthread_setname_np(pthread_self(), process_name.characters());

    page().load(url);
}

void ClientConnection::load_html(const String& html, const URL& url)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::LoadHTML: html={}, url={}", html, url);
    page().load_html(html, url);
}

void ClientConnection::set_viewport_rect(const Gfx::IntRect& rect)
{
    dbgln_if(SPAM_DEBUG, "handle: WebContentServer::SetViewportRect: rect={}", rect);
    m_page_host->set_viewport_rect(rect);
}

void ClientConnection::add_backing_store(i32 backing_store_id, const Gfx::ShareableBitmap& bitmap)
{
    m_backing_stores.set(backing_store_id, *bitmap.bitmap());
}

void ClientConnection::remove_backing_store(i32 backing_store_id)
{
    m_backing_stores.remove(backing_store_id);
}

void ClientConnection::paint(const Gfx::IntRect& content_rect, i32 backing_store_id)
{
    for (auto& pending_paint : m_pending_paint_requests) {
        if (pending_paint.bitmap_id == backing_store_id) {
            pending_paint.content_rect = content_rect;
            return;
        }
    }

    auto it = m_backing_stores.find(backing_store_id);
    if (it == m_backing_stores.end()) {
        did_misbehave("Client requested paint with backing store ID");
        return;
    }

    auto& bitmap = *it->value;
    m_pending_paint_requests.append({ content_rect, bitmap, backing_store_id });
    m_paint_flush_timer->start();
}

void ClientConnection::flush_pending_paint_requests()
{
    for (auto& pending_paint : m_pending_paint_requests) {
        m_page_host->paint(pending_paint.content_rect, *pending_paint.bitmap);
        async_did_paint(pending_paint.content_rect, pending_paint.bitmap_id);
    }
    m_pending_paint_requests.clear();
}

void ClientConnection::mouse_down(const Gfx::IntPoint& position, unsigned int button, [[maybe_unused]] unsigned int buttons, unsigned int modifiers)
{
    page().handle_mousedown(position, button, modifiers);
}

void ClientConnection::mouse_move(const Gfx::IntPoint& position, [[maybe_unused]] unsigned int button, unsigned int buttons, unsigned int modifiers)
{
    page().handle_mousemove(position, buttons, modifiers);
}

void ClientConnection::mouse_up(const Gfx::IntPoint& position, unsigned int button, [[maybe_unused]] unsigned int buttons, unsigned int modifiers)
{
    page().handle_mouseup(position, button, modifiers);
}

void ClientConnection::mouse_wheel(const Gfx::IntPoint& position, unsigned int button, [[maybe_unused]] unsigned int buttons, unsigned int modifiers, i32 wheel_delta)
{
    page().handle_mousewheel(position, button, modifiers, wheel_delta);
}

void ClientConnection::key_down(i32 key, unsigned int modifiers, u32 code_point)
{
    page().handle_keydown((KeyCode)key, modifiers, code_point);
}

void ClientConnection::debug_request(const String& request, const String& argument)
{
    if (request == "dump-dom-tree") {
        if (auto* doc = page().top_level_browsing_context().document())
            Web::dump_tree(*doc);
    }

    if (request == "dump-layout-tree") {
        if (auto* doc = page().top_level_browsing_context().document()) {
            if (auto* icb = doc->layout_node())
                Web::dump_tree(*icb);
        }
    }

    if (request == "dump-style-sheets") {
        if (auto* doc = page().top_level_browsing_context().document()) {
            for (auto& sheet : doc->style_sheets().sheets()) {
                Web::dump_sheet(sheet);
            }
        }
    }

    if (request == "collect-garbage") {
        Web::Bindings::main_thread_vm().heap().collect_garbage(JS::Heap::CollectionType::CollectGarbage, true);
    }

    if (request == "set-line-box-borders") {
        bool state = argument == "on";
        m_page_host->set_should_show_line_box_borders(state);
        page().top_level_browsing_context().set_needs_display(page().top_level_browsing_context().viewport_rect());
    }

    if (request == "clear-cache") {
        Web::ResourceLoader::the().clear_cache();
    }

    if (request == "spoof-user-agent") {
        Web::ResourceLoader::the().set_user_agent(argument);
    }
}

void ClientConnection::get_source()
{
    if (auto* doc = page().top_level_browsing_context().document()) {
        async_did_get_source(doc->url(), doc->source());
    }
}

void ClientConnection::js_console_initialize()
{
    if (auto* document = page().top_level_browsing_context().document()) {
        auto interpreter = document->interpreter().make_weak_ptr();
        if (m_interpreter.ptr() == interpreter.ptr())
            return;

        m_interpreter = interpreter;
        m_console_client = make<WebContentConsoleClient>(interpreter->global_object().console(), interpreter, *this);
        interpreter->global_object().console().set_client(*m_console_client.ptr());
    }
}

void ClientConnection::js_console_input(const String& js_source)
{
    if (m_console_client)
        m_console_client->handle_input(js_source);
}

}
