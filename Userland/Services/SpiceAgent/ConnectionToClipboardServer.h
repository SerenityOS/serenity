/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibGfx/Bitmap.h>
#include <LibIPC/ConnectionToServer.h>

#pragma once

class ConnectionToClipboardServer final
    : public IPC::ConnectionToServer<ClipboardClientEndpoint, ClipboardServerEndpoint>
    , public ClipboardClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionToClipboardServer, "/tmp/portal/clipboard")

public:
    Function<void()> on_data_changed;
    RefPtr<Gfx::Bitmap> get_bitmap();
    void set_bitmap(Gfx::Bitmap const& bitmap);

private:
    ConnectionToClipboardServer(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ConnectionToServer<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, move(socket))
    {
    }
    virtual void clipboard_data_changed(String const&) override
    {
        on_data_changed();
    }
};
