/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <LibJS/Forward.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/Forward.h>
#include <WebContent/Forward.h>
#include <WebContent/WebContentClientEndpoint.h>
#include <WebContent/WebContentConsoleClient.h>
#include <WebContent/WebContentServerEndpoint.h>

namespace WebContent {

class ClientConnection final
    : public IPC::ClientConnection<WebContentClientEndpoint, WebContentServerEndpoint>
    , public WebContentServerEndpoint {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

private:
    Web::Page& page();
    const Web::Page& page() const;

    virtual OwnPtr<Messages::WebContentServer::GreetResponse> handle(const Messages::WebContentServer::Greet&) override;
    virtual void handle(const Messages::WebContentServer::UpdateSystemTheme&) override;
    virtual void handle(const Messages::WebContentServer::UpdateScreenRect&) override;
    virtual void handle(const Messages::WebContentServer::LoadURL&) override;
    virtual void handle(const Messages::WebContentServer::LoadHTML&) override;
    virtual void handle(const Messages::WebContentServer::Paint&) override;
    virtual void handle(const Messages::WebContentServer::SetViewportRect&) override;
    virtual void handle(const Messages::WebContentServer::MouseDown&) override;
    virtual void handle(const Messages::WebContentServer::MouseMove&) override;
    virtual void handle(const Messages::WebContentServer::MouseUp&) override;
    virtual void handle(const Messages::WebContentServer::MouseWheel&) override;
    virtual void handle(const Messages::WebContentServer::KeyDown&) override;
    virtual void handle(const Messages::WebContentServer::AddBackingStore&) override;
    virtual void handle(const Messages::WebContentServer::RemoveBackingStore&) override;
    virtual void handle(const Messages::WebContentServer::DebugRequest&) override;
    virtual void handle(const Messages::WebContentServer::GetSource&) override;
    virtual void handle(const Messages::WebContentServer::JSConsoleInitialize&) override;
    virtual void handle(const Messages::WebContentServer::JSConsoleInput&) override;

    void flush_pending_paint_requests();

    NonnullOwnPtr<PageHost> m_page_host;
    struct PaintRequest {
        Gfx::IntRect content_rect;
        NonnullRefPtr<Gfx::Bitmap> bitmap;
        i32 bitmap_id { -1 };
    };
    Vector<PaintRequest> m_pending_paint_requests;
    RefPtr<Core::Timer> m_paint_flush_timer;

    HashMap<i32, NonnullRefPtr<Gfx::Bitmap>> m_backing_stores;

    WeakPtr<JS::Interpreter> m_interpreter;
    OwnPtr<WebContentConsoleClient> m_console_client;
};

}
