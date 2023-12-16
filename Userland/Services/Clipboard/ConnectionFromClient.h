/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ClipboardServerEndpoint.h>
#include <LibIPC/ConnectionFromClient.h>

namespace Clipboard {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<ClipboardClientEndpoint, ClipboardServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    virtual ~ConnectionFromClient() override = default;

    virtual void die() override;

    static void for_each_client(Function<void(ConnectionFromClient&)>);

    void notify_about_clipboard_change();

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>, int client_id);

    virtual Messages::ClipboardServer::GetClipboardDataResponse get_clipboard_data() override;
    virtual void set_clipboard_data(Core::AnonymousBuffer const&, ByteString const&, HashMap<ByteString, ByteString> const&) override;
};

}
