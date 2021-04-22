/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
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

OwnPtr<Messages::ClipboardServer::GreetResponse> ClientConnection::handle(const Messages::ClipboardServer::Greet&)
{
    return make<Messages::ClipboardServer::GreetResponse>();
}

OwnPtr<Messages::ClipboardServer::SetClipboardDataResponse> ClientConnection::handle(const Messages::ClipboardServer::SetClipboardData& message)
{
    Storage::the().set_data(message.data(), message.mime_type(), message.metadata().entries());
    return make<Messages::ClipboardServer::SetClipboardDataResponse>();
}

OwnPtr<Messages::ClipboardServer::GetClipboardDataResponse> ClientConnection::handle(const Messages::ClipboardServer::GetClipboardData&)
{
    auto& storage = Storage::the();
    return make<Messages::ClipboardServer::GetClipboardDataResponse>(storage.buffer(), storage.mime_type(), storage.metadata());
}

void ClientConnection::notify_about_clipboard_change()
{
    post_message(Messages::ClipboardClient::ClipboardDataChanged(Storage::the().mime_type()));
}

}
