/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Clipboard/ClipboardClientEndpoint.h>
#include <Clipboard/ConnectionFromClient.h>
#include <Clipboard/Storage.h>

namespace Clipboard {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

void ConnectionFromClient::for_each_client(Function<void(ConnectionFromClient&)> callback)
{
    for (auto& it : s_connections) {
        callback(*it.value);
    }
}

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket, int client_id)
    : IPC::ConnectionFromClient<ClipboardClientEndpoint, ClipboardServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ConnectionFromClient::~ConnectionFromClient()
{
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

void ConnectionFromClient::set_clipboard_data(Core::AnonymousBuffer const& data, String const& mime_type, IPC::Dictionary const& metadata)
{
    Storage::the().set_data(data, mime_type, metadata.entries());
}

Messages::ClipboardServer::GetClipboardDataResponse ConnectionFromClient::get_clipboard_data()
{
    auto& storage = Storage::the();
    return { storage.buffer(), storage.mime_type(), storage.metadata() };
}

void ConnectionFromClient::notify_about_clipboard_change()
{
    async_clipboard_data_changed(Storage::the().mime_type());
}

}
