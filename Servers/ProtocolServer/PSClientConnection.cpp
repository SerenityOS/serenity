/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <ProtocolServer/Download.h>
#include <ProtocolServer/PSClientConnection.h>
#include <ProtocolServer/Protocol.h>
#include <ProtocolServer/ProtocolClientEndpoint.h>
#include <AK/SharedBuffer.h>

static HashMap<int, RefPtr<PSClientConnection>> s_connections;

PSClientConnection::PSClientConnection(Core::LocalSocket& socket, int client_id)
    : IPC::ClientConnection<ProtocolServerEndpoint>(*this, socket, client_id)
{
    s_connections.set(client_id, *this);
}

PSClientConnection::~PSClientConnection()
{
}

void PSClientConnection::die()
{
    s_connections.remove(client_id());
}

OwnPtr<Messages::ProtocolServer::IsSupportedProtocolResponse> PSClientConnection::handle(const Messages::ProtocolServer::IsSupportedProtocol& message)
{
    bool supported = Protocol::find_by_name(message.protocol().to_lowercase());
    return make<Messages::ProtocolServer::IsSupportedProtocolResponse>(supported);
}

OwnPtr<Messages::ProtocolServer::StartDownloadResponse> PSClientConnection::handle(const Messages::ProtocolServer::StartDownload& message)
{
    URL url(message.url());
    ASSERT(url.is_valid());
    auto* protocol = Protocol::find_by_name(url.protocol());
    ASSERT(protocol);
    auto download = protocol->start_download(*this, url);
    return make<Messages::ProtocolServer::StartDownloadResponse>(download->id());
}

OwnPtr<Messages::ProtocolServer::StopDownloadResponse> PSClientConnection::handle(const Messages::ProtocolServer::StopDownload& message)
{
    auto* download = Download::find_by_id(message.download_id());
    bool success = false;
    if (download) {
        download->stop();
    }
    return make<Messages::ProtocolServer::StopDownloadResponse>(success);
}

void PSClientConnection::did_finish_download(Badge<Download>, Download& download, bool success)
{
    RefPtr<SharedBuffer> buffer;
    if (success && !download.payload().is_null()) {
        buffer = SharedBuffer::create_with_size(download.payload().size());
        memcpy(buffer->data(), download.payload().data(), download.payload().size());
        buffer->seal();
        buffer->share_with(client_pid());
        m_shared_buffers.set(buffer->shared_buffer_id(), buffer);
    }
    post_message(Messages::ProtocolClient::DownloadFinished(download.id(), success, download.total_size(), buffer ? buffer->shared_buffer_id() : -1));
}

void PSClientConnection::did_progress_download(Badge<Download>, Download& download)
{
    post_message(Messages::ProtocolClient::DownloadProgress(download.id(), download.total_size(), download.downloaded_size()));
}

OwnPtr<Messages::ProtocolServer::GreetResponse> PSClientConnection::handle(const Messages::ProtocolServer::Greet&)
{
    return make<Messages::ProtocolServer::GreetResponse>(client_id());
}

OwnPtr<Messages::ProtocolServer::DisownSharedBufferResponse> PSClientConnection::handle(const Messages::ProtocolServer::DisownSharedBuffer& message)
{
    m_shared_buffers.remove(message.shared_buffer_id());
    return make<Messages::ProtocolServer::DisownSharedBufferResponse>();
}
