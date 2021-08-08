/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <LibIPC/Connection.h>
#include <LibRemoteDesktop/RemoteCompositor.h>
#include <LibRemoteDesktop/RemoteDesktopClientEndpoint.h>
#include <LibRemoteDesktop/RemoteDesktopServerEndpoint.h>

namespace RemoteDesktop {

class RemoteDesktopClientConnection : public IPC::Connection<RemoteDesktopServerEndpoint, RemoteDesktopClientEndpoint, Core::TCPSocket>
    , public RemoteDesktopServerEndpoint::Stub
    , public RemoteDesktopClientProxy<RemoteDesktopServerEndpoint, RemoteDesktopClientEndpoint, RemoteDesktopClientConnection> {
public:
    RemoteDesktopClientConnection(NonnullRefPtr<Core::TCPSocket> socket)
        : IPC::Connection<RemoteDesktopServerEndpoint, RemoteDesktopClientEndpoint, Core::TCPSocket>(*this, move(socket))
        , RemoteDesktopClientProxy<RemoteDesktopServerEndpoint, RemoteDesktopClientEndpoint, RemoteDesktopClientConnection>(*this, {})
    {
        this->socket().set_blocking(true);
    }
};

}
