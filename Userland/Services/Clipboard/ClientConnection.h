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
    : public IPC::ClientConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>
    , public ClipboardServerEndpoint {

    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    virtual ~ClientConnection() override;

    virtual void die() override;

    static void for_each_client(Function<void(ClientConnection&)>);

    void notify_about_clipboard_change();

private:
    virtual OwnPtr<Messages::ClipboardServer::GreetResponse> handle(const Messages::ClipboardServer::Greet&) override;
    virtual OwnPtr<Messages::ClipboardServer::GetClipboardDataResponse> handle(const Messages::ClipboardServer::GetClipboardData&) override;
    virtual OwnPtr<Messages::ClipboardServer::SetClipboardDataResponse> handle(const Messages::ClipboardServer::SetClipboardData&) override;
};

}
