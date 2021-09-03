/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Function.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibGfx/Bitmap.h>
#include <LibIPC/ServerConnection.h>

#pragma once

class ClipboardServerConnection final
    : public IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>
    , public ClipboardClientEndpoint {
    C_OBJECT(ClipboardServerConnection);

    Function<void()> on_data_changed;
    RefPtr<Gfx::Bitmap> get_bitmap();
    void set_bitmap(Gfx::Bitmap const& bitmap);

private:
    ClipboardServerConnection()
        : IPC::ServerConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, "/tmp/portal/clipboard")
    {
    }
    virtual void clipboard_data_changed(String const&) override
    {
        on_data_changed();
    }
};
