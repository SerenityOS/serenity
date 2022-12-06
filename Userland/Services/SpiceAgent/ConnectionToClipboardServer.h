/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibGfx/Bitmap.h>
#include <LibIPC/ConnectionToServer.h>

class ConnectionToClipboardServer final
    : public IPC::ConnectionToServer<ClipboardClientEndpoint, ClipboardServerEndpoint>
    , public ClipboardClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionToClipboardServer, "/tmp/session/%sid/portal/clipboard"sv)

public:
    Function<void()> on_data_changed;
    RefPtr<Gfx::Bitmap> get_bitmap();
    void set_bitmap(Gfx::Bitmap const& bitmap);

private:
    ConnectionToClipboardServer(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, move(socket))
    {
    }
    virtual void clipboard_data_changed(DeprecatedString const&) override
    {
        on_data_changed();
    }
};
