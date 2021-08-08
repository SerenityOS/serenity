/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Remote/RemoteFontDatabase.h>
#include <LibGfx/Remote/RemoteGfx.h>
#include <LibIPC/ServerConnection.h>

#ifdef __serenity__
#    include <LibGfx/Remote/RemoteGfxClientEndpoint.h>
#    include <LibGfx/Remote/RemoteGfxServerEndpoint.h>
#endif

namespace RemoteGfx {

class RemoteGfxSession : public RefCounted<RemoteGfxSession>
    , public Weakable<RemoteGfxSession> {
    friend class RemoteGfxServerConnection;

public:
    auto& connection() { return m_connection; }
    auto& connection() const { return m_connection; }

private:
    RemoteGfxSession(RemoteGfxServerConnection& connection)
        : m_connection(connection)
    {
    }

    RemoteGfxServerConnection& m_connection;
};

#ifdef __serenity__
class RemoteGfxServerConnection final
    : public IPC::ServerConnection<RemoteGfxClientEndpoint, RemoteGfxServerEndpoint>
    , public RemoteGfxClientEndpoint {
    C_OBJECT(RemoteGfxServerConnection)
public:
    RemoteGfxServerConnection();
    static RemoteGfxServerConnection& the();

    virtual void enable_remote_gfx(u64) override;
    virtual void disable_remote_gfx() override;
    virtual void notify_remote_fonts(Vector<ByteBuffer> const&) override;

    void create_font_and_send_if_needed(i32, Gfx::Font const&);

    u64 cookie() const { return m_cookie; }
    bool is_enabled() const { return m_enabled; }
    RefPtr<RemoteGfxSession> session() { return m_session; }

    Function<void(RemoteGfxSession&)> on_new_session;
    Function<void(RemoteGfxSession&)> on_session_end;

private:
    static RemoteGfxServerConnection* s_the;
    HashTable<Crypto::Hash::HashableDigest<RemoteGfx::RemoteGfxFontDatabase::FontDigestType>> m_remote_fonts;
    u64 m_cookie { 0 };
    RefPtr<RemoteGfxSession> m_session;
    bool m_enabled { false };
};
#endif

}
