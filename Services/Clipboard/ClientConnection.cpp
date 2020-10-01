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
#include <AK/SharedBuffer.h>
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
    return make<Messages::ClipboardServer::GreetResponse>(client_id());
}

OwnPtr<Messages::ClipboardServer::SetClipboardDataResponse> ClientConnection::handle(const Messages::ClipboardServer::SetClipboardData& message)
{
    auto shared_buffer = SharedBuffer::create_from_shbuf_id(message.shbuf_id());
    if (!shared_buffer) {
        did_misbehave("SetClipboardData: Bad shared buffer ID");
        return nullptr;
    }
    Storage::the().set_data(*shared_buffer, message.data_size(), message.mime_type(), message.metadata().entries());
    return make<Messages::ClipboardServer::SetClipboardDataResponse>();
}

OwnPtr<Messages::ClipboardServer::GetClipboardDataResponse> ClientConnection::handle(const Messages::ClipboardServer::GetClipboardData&)
{
    auto& storage = Storage::the();

    i32 shbuf_id = -1;
    if (storage.data_size()) {
        // FIXME: Optimize case where an app is copy/pasting within itself.
        //        We can just reuse the SharedBuffer then, since it will have the same peer PID.
        //        It would be even nicer if a SharedBuffer could have an arbitrary number of clients..
        RefPtr<SharedBuffer> shared_buffer = SharedBuffer::create_with_size(storage.data_size());
        ASSERT(shared_buffer);
        memcpy(shared_buffer->data<void>(), storage.data(), storage.data_size());
        shared_buffer->seal();
        shared_buffer->share_with(client_pid());
        shbuf_id = shared_buffer->shbuf_id();

        // FIXME: This is a workaround for the fact that SharedBuffers will go away if neither side is retaining them.
        //        After we respond to GetClipboardData, we have to wait for the client to ref the buffer on his side.
        m_last_sent_buffer = move(shared_buffer);
    }
    return make<Messages::ClipboardServer::GetClipboardDataResponse>(shbuf_id, storage.data_size(), storage.mime_type(), storage.metadata());
}

void ClientConnection::notify_about_clipboard_change()
{
    post_message(Messages::ClipboardClient::ClipboardDataChanged(Storage::the().mime_type()));
}

}
