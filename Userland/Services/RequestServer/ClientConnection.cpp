/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <RequestServer/ClientConnection.h>
#include <RequestServer/Protocol.h>
#include <RequestServer/Request.h>
#include <RequestServer/RequestClientEndpoint.h>

namespace RequestServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<RequestClientEndpoint, RequestServerEndpoint>(*this, move(socket), client_id)
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

OwnPtr<Messages::RequestServer::IsSupportedProtocolResponse> ClientConnection::handle(const Messages::RequestServer::IsSupportedProtocol& message)
{
    bool supported = Protocol::find_by_name(message.protocol().to_lowercase());
    return make<Messages::RequestServer::IsSupportedProtocolResponse>(supported);
}

OwnPtr<Messages::RequestServer::StartRequestResponse> ClientConnection::handle(const Messages::RequestServer::StartRequest& message)
{
    const auto& url = message.url();
    if (!url.is_valid()) {
        dbgln("StartRequest: Invalid URL requested: '{}'", url);
        return make<Messages::RequestServer::StartRequestResponse>(-1, Optional<IPC::File> {});
    }
    auto* protocol = Protocol::find_by_name(url.protocol());
    if (!protocol) {
        dbgln("StartRequest: No protocol handler for URL: '{}'", url);
        return make<Messages::RequestServer::StartRequestResponse>(-1, Optional<IPC::File> {});
    }
    auto request = protocol->start_request(*this, message.method(), url, message.request_headers().entries(), message.request_body());
    if (!request) {
        dbgln("StartRequest: Protocol handler failed to start request: '{}'", url);
        return make<Messages::RequestServer::StartRequestResponse>(-1, Optional<IPC::File> {});
    }
    auto id = request->id();
    auto fd = request->request_fd();
    m_requests.set(id, move(request));
    return make<Messages::RequestServer::StartRequestResponse>(id, IPC::File(fd, IPC::File::CloseAfterSending));
}

OwnPtr<Messages::RequestServer::StopRequestResponse> ClientConnection::handle(const Messages::RequestServer::StopRequest& message)
{
    auto* request = const_cast<Request*>(m_requests.get(message.request_id()).value_or(nullptr));
    bool success = false;
    if (request) {
        request->stop();
        m_requests.remove(message.request_id());
        success = true;
    }
    return make<Messages::RequestServer::StopRequestResponse>(success);
}

void ClientConnection::did_receive_headers(Badge<Request>, Request& request)
{
    IPC::Dictionary response_headers;
    for (auto& it : request.response_headers())
        response_headers.add(it.key, it.value);

    post_message(Messages::RequestClient::HeadersBecameAvailable(request.id(), move(response_headers), request.status_code()));
}

void ClientConnection::did_finish_request(Badge<Request>, Request& request, bool success)
{
    VERIFY(request.total_size().has_value());

    post_message(Messages::RequestClient::RequestFinished(request.id(), success, request.total_size().value()));

    m_requests.remove(request.id());
}

void ClientConnection::did_progress_request(Badge<Request>, Request& request)
{
    post_message(Messages::RequestClient::RequestProgress(request.id(), request.total_size(), request.downloaded_size()));
}

void ClientConnection::did_request_certificates(Badge<Request>, Request& request)
{
    post_message(Messages::RequestClient::CertificateRequested(request.id()));
}

OwnPtr<Messages::RequestServer::GreetResponse> ClientConnection::handle(const Messages::RequestServer::Greet&)
{
    return make<Messages::RequestServer::GreetResponse>();
}

OwnPtr<Messages::RequestServer::SetCertificateResponse> ClientConnection::handle(const Messages::RequestServer::SetCertificate& message)
{
    auto* request = const_cast<Request*>(m_requests.get(message.request_id()).value_or(nullptr));
    bool success = false;
    if (request) {
        request->set_certificate(message.certificate(), message.key());
        success = true;
    }
    return make<Messages::RequestServer::SetCertificateResponse>(success);
}

}
