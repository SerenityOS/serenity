/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibIPC/ClientConnection.h>

namespace Clipboard {

class ClientConnection final
    : public IPC::ClientConnection<ClipboardClientEndpoint, ClipboardServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    virtual ~ClientConnection() override;

    virtual void die() override;

    static void for_each_client(Function<void(ClientConnection&)>);

    void notify_about_clipboard_change();

private:
    explicit ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id);

    virtual Messages::ClipboardServer::GetClipboardDataResponse get_clipboard_data() override;
    virtual void set_clipboard_data(Core::AnonymousBuffer const&, String const&, IPC::Dictionary const&) override;
};

}
