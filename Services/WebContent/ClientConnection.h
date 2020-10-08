/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <LibWeb/Forward.h>
#include <WebContent/Forward.h>
#include <WebContent/WebContentClientEndpoint.h>
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
    virtual void handle(const Messages::WebContentServer::LoadURL&) override;
    virtual void handle(const Messages::WebContentServer::LoadHTML&) override;
    virtual void handle(const Messages::WebContentServer::Paint&) override;
    virtual void handle(const Messages::WebContentServer::SetViewportRect&) override;
    virtual void handle(const Messages::WebContentServer::MouseDown&) override;
    virtual void handle(const Messages::WebContentServer::MouseMove&) override;
    virtual void handle(const Messages::WebContentServer::MouseUp&) override;
    virtual void handle(const Messages::WebContentServer::KeyDown&) override;

    void flush_pending_paint_requests();

    NonnullOwnPtr<PageHost> m_page_host;

    struct PaintRequest {
        Gfx::IntRect content_rect;
        NonnullRefPtr<Gfx::Bitmap> bitmap;
    };
    Vector<PaintRequest> m_pending_paint_requests;
    RefPtr<Core::Timer> m_paint_flush_timer;
};

}
