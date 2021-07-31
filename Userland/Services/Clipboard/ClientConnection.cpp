/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Clipboard/ClientConnection.h>
#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/Storage.h>

namespace Clipboard {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

void ClientConnection::for_each_client(Function<void(ClientConnection&)> callback)
{
    for (auto& it : s_connections) {
        callback(*it.value);
    }
}

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

void ClientConnection::set_clipboard_data(Core::AnonymousBuffer const& data, String const& mime_type, IPC::Dictionary const& metadata)
{
    Storage::the().set_data(data, mime_type, metadata.entries());
}

Messages::ClipboardServer::GetClipboardDataResponse ClientConnection::get_clipboard_data()
{
    auto& storage = Storage::the();
    return { storage.buffer(), storage.mime_type(), storage.metadata() };
}

void ClientConnection::notify_about_clipboard_change()
{
    async_clipboard_data_changed(Storage::the().mime_type());
}

}
