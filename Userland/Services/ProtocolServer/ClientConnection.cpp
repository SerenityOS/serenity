/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
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
    const auto& url = message.url();
    if (!url.is_valid()) {
        dbgln("StartDownload: Invalid URL requested: '{}'", url);
        return make<Messages::ProtocolServer::StartDownloadResponse>(-1, Optional<IPC::File> {});
    }
    auto* protocol = Protocol::find_by_name(url.protocol());
    if (!protocol) {
        dbgln("StartDownload: No protocol handler for URL: '{}'", url);
        return make<Messages::ProtocolServer::StartDownloadResponse>(-1, Optional<IPC::File> {});
    }
    auto download = protocol->start_download(*this, message.method(), url, message.request_headers().entries(), message.request_body());
    if (!download) {
        dbgln("StartDownload: Protocol handler failed to start download: '{}'", url);
        return make<Messages::ProtocolServer::StartDownloadResponse>(-1, Optional<IPC::File> {});
    }
    auto id = download->id();
    auto fd = download->download_fd();
    m_downloads.set(id, move(download));
    return make<Messages::ProtocolServer::StartDownloadResponse>(id, IPC::File(fd, IPC::File::CloseAfterSending));
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

void ClientConnection::did_receive_headers(Badge<Download>, Download& download)
{
    IPC::Dictionary response_headers;
    for (auto& it : download.response_headers())
        response_headers.add(it.key, it.value);

    post_message(Messages::ProtocolClient::HeadersBecameAvailable(download.id(), move(response_headers), download.status_code()));
}

void ClientConnection::did_finish_download(Badge<Download>, Download& download, bool success)
{
    VERIFY(download.total_size().has_value());

    post_message(Messages::ProtocolClient::DownloadFinished(download.id(), success, download.total_size().value()));

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
    return make<Messages::ProtocolServer::GreetResponse>();
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
