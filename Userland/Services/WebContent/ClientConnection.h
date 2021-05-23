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
    : public IPC::ClientConnection<WebContentClientEndpoint, WebContentServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

private:
    Web::Page& page();
    const Web::Page& page() const;

    virtual void update_system_theme(Core::AnonymousBuffer const&) override;
    virtual void update_system_fonts(String const&, String const&) override;
    virtual void update_screen_rect(Gfx::IntRect const&) override;
    virtual void load_url(URL const&) override;
    virtual void load_html(String const&, URL const&) override;
    virtual void paint(Gfx::IntRect const&, i32) override;
    virtual void set_viewport_rect(Gfx::IntRect const&) override;
    virtual void mouse_down(Gfx::IntPoint const&, unsigned, unsigned, unsigned) override;
    virtual void mouse_move(Gfx::IntPoint const&, unsigned, unsigned, unsigned) override;
    virtual void mouse_up(Gfx::IntPoint const&, unsigned, unsigned, unsigned) override;
    virtual void mouse_wheel(Gfx::IntPoint const&, unsigned, unsigned, unsigned, i32) override;
    virtual void key_down(i32, unsigned, u32) override;
    virtual void add_backing_store(i32, Gfx::ShareableBitmap const&) override;
    virtual void remove_backing_store(i32) override;
    virtual void debug_request(String const&, String const&) override;
    virtual void get_source() override;
    virtual void js_console_initialize() override;
    virtual void js_console_input(String const&) override;

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
