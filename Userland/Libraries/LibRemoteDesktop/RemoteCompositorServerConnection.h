/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <LibIPC/ServerConnection.h>
#include <LibRemoteDesktop/RemoteCompositor.h>
#include <LibRemoteDesktop/RemoteCompositorClientEndpoint.h>
#include <LibRemoteDesktop/RemoteCompositorServerEndpoint.h>

namespace RemoteDesktop {

class RemoteDesktopClientConnection;

class RemoteCompositorServerConnection final
    : public IPC::ServerConnection<RemoteCompositorClientEndpoint, RemoteCompositorServerEndpoint>
    , public RemoteCompositorClientEndpoint {
    C_OBJECT(RemoteCompositorServerConnection)
public:
    RemoteCompositorServerConnection(RefPtr<RemoteDesktopClientConnection>);
    ~RemoteCompositorServerConnection();

    void die() override;

    void set_forwarding(bool forwarding) { m_forwarding = forwarding; }

    Function<void()> on_disconnect;

private:
    virtual void fast_greet(Vector<Gfx::IntRect> const&, Gfx::Color const&, Gfx::IntPoint const&) override;
    virtual void associate_window_client(int, u64) override;
    virtual void disassociate_window_client(int) override;
    virtual void update_display(Vector<Compositor::WindowId> const&, Vector<Compositor::Window> const&, Vector<Compositor::WindowId> const&, Vector<Compositor::WindowDirtyRects> const&) override;
    virtual void cursor_position_changed(Gfx::IntPoint const&) override;

    RefPtr<RemoteDesktopClientConnection> m_client_connection;
    bool m_forwarding { false };
    bool m_should_request_mode { false };
};

}
