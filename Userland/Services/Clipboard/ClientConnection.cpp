/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
