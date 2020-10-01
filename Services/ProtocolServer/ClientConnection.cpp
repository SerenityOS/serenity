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

#include <AK/Badge.h>
#include <AK/SharedBuffer.h>
#include <ProtocolServer/ClientConnection.h>
#include <ProtocolServer/Download.h>
#include <ProtocolServer/Protocol.h>
#include <ProtocolServer/ProtocolClientEndpoint.h>

namespace ProtocolServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<ProtocolClientEndpoint, ProtocolServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
    if (s_connections.is_empty())
        Core::EventLoop::current().quit(0);
}

OwnPtr<Messages::ProtocolServer::IsSupportedProtocolResponse> ClientConnection::handle(const Messages::ProtocolServer::IsSupportedProtocol& message)
{
    bool supported = Protocol::find_by_name(message.protocol().to_lowercase());
    return make<Messages::ProtocolServer::IsSupportedProtocolResponse>(supported);
}

OwnPtr<Messages::ProtocolServer::StartDownloadResponse> ClientConnection::handle(const Messages::ProtocolServer::StartDownload& message)
{
    URL url(message.url());
    if (!url.is_valid())
        return make<Messages::ProtocolServer::StartDownloadResponse>(-1);
    auto* protocol = Protocol::find_by_name(url.protocol());
    if (!protocol)
        return make<Messages::ProtocolServer::StartDownloadResponse>(-1);
    auto download = protocol->start_download(*this, message.method(), url, message.request_headers().entries(), message.request_body().to_byte_buffer());
    if (!download)
        return make<Messages::ProtocolServer::StartDownloadResponse>(-1);
    auto id = download->id();
    m_downloads.set(id, move(download));
    return make<Messages::ProtocolServer::StartDownloadResponse>(id);
}

OwnPtr<Messages::ProtocolServer::StopDownloadResponse> ClientConnection::handle(const Messages::ProtocolServer::StopDownload& message)
{
    auto* download = const_cast<Download*>(m_downloads.get(message.download_id()).value_or(nullptr));
    bool success = false;
    if (download) {
        download->stop();
        m_downloads.remove(message.download_id());
        success = true;
    }
    return make<Messages::ProtocolServer::StopDownloadResponse>(success);
}

void ClientConnection::did_finish_download(Badge<Download>, Download& download, bool success)
{
    RefPtr<SharedBuffer> buffer;
    if (success && download.payload().size() > 0 && !download.payload().is_null()) {
        buffer = SharedBuffer::create_with_size(download.payload().size());
        memcpy(buffer->data<void>(), download.payload().data(), download.payload().size());
        buffer->seal();
        buffer->share_with(client_pid());
        m_shared_buffers.set(buffer->shbuf_id(), buffer);
    }
    ASSERT(download.total_size().has_value());

    IPC::Dictionary response_headers;
    for (auto& it : download.response_headers())
        response_headers.add(it.key, it.value);
    post_message(Messages::ProtocolClient::DownloadFinished(download.id(), success, download.status_code(), download.total_size().value(), buffer ? buffer->shbuf_id() : -1, response_headers));

    m_downloads.remove(download.id());
}

void ClientConnection::did_progress_download(Badge<Download>, Download& download)
{
    post_message(Messages::ProtocolClient::DownloadProgress(download.id(), download.total_size(), download.downloaded_size()));
}

void ClientConnection::did_request_certificates(Badge<Download>, Download& download)
{
    post_message(Messages::ProtocolClient::CertificateRequested(download.id()));
}

OwnPtr<Messages::ProtocolServer::GreetResponse> ClientConnection::handle(const Messages::ProtocolServer::Greet&)
{
    return make<Messages::ProtocolServer::GreetResponse>(client_id());
}

OwnPtr<Messages::ProtocolServer::DisownSharedBufferResponse> ClientConnection::handle(const Messages::ProtocolServer::DisownSharedBuffer& message)
{
    m_shared_buffers.remove(message.shbuf_id());
    return make<Messages::ProtocolServer::DisownSharedBufferResponse>();
}

OwnPtr<Messages::ProtocolServer::SetCertificateResponse> ClientConnection::handle(const Messages::ProtocolServer::SetCertificate& message)
{
    auto* download = const_cast<Download*>(m_downloads.get(message.download_id()).value_or(nullptr));
    bool success = false;
    if (download) {
        download->set_certificate(message.certificate(), message.key());
        success = true;
    }
    return make<Messages::ProtocolServer::SetCertificateResponse>(success);
}

}
