/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibCore/IPCSockets.h>
#include <LibGfx/Bitmap.h>
#include <LibIPC/ServerConnection.h>

#pragma once

class ClipboardServerConnection final
    : public IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>
    , public ClipboardClientEndpoint {
    C_OBJECT(ClipboardServerConnection);

public:
    Function<void()> on_data_changed;
    RefPtr<Gfx::Bitmap> get_bitmap();
    void set_bitmap(Gfx::Bitmap const& bitmap);

private:
    // FIXME: This should be a user socket, but this breaks LoginServer.
    ClipboardServerConnection()
        : IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, Core::IPCSockets::system_socket("clipboard"))
    {
    }
    virtual void clipboard_data_changed(String const&) override
    {
        on_data_changed();
    }
};
