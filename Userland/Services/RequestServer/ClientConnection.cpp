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

Messages::RequestServer::IsSupportedProtocolResponse ClientConnection::is_supported_protocol(String const& protocol)
{
    bool supported = Protocol::find_by_name(protocol.to_lowercase());
    return supported;
}

Messages::RequestServer::StartRequestResponse ClientConnection::start_request(String const& method, URL const& url, IPC::Dictionary const& request_headers, ByteBuffer const& request_body)
{
    if (!url.is_valid()) {
        dbgln("StartRequest: Invalid URL requested: '{}'", url);
        return { -1, Optional<IPC::File> {} };
    }
    auto* protocol = Protocol::find_by_name(url.protocol());
    if (!protocol) {
        dbgln("StartRequest: No protocol handler for URL: '{}'", url);
        return { -1, Optional<IPC::File> {} };
    }
    auto request = protocol->start_request(*this, method, url, request_headers.entries(), request_body);
    if (!request) {
        dbgln("StartRequest: Protocol handler failed to start request: '{}'", url);
        return { -1, Optional<IPC::File> {} };
    }
    auto id = request->id();
    auto fd = request->request_fd();
    m_requests.set(id, move(request));
    return { id, IPC::File(fd, IPC::File::CloseAfterSending) };
}

Messages::RequestServer::StopRequestResponse ClientConnection::stop_request(i32 request_id)
{
    auto* request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr));
    bool success = false;
    if (request) {
        request->stop();
        m_requests.remove(request_id);
        success = true;
    }
    return success;
}

void ClientConnection::did_receive_headers(Badge<Request>, Request& request)
{
    IPC::Dictionary response_headers;
    for (auto& it : request.response_headers())
        response_headers.add(it.key, it.value);

    async_headers_became_available(request.id(), move(response_headers), request.status_code());
}

void ClientConnection::did_finish_request(Badge<Request>, Request& request, bool success)
{
    VERIFY(request.total_size().has_value());

    async_request_finished(request.id(), success, request.total_size().value());

    m_requests.remove(request.id());
}

void ClientConnection::did_progress_request(Badge<Request>, Request& request)
{
    async_request_progress(request.id(), request.total_size(), request.downloaded_size());
}

void ClientConnection::did_request_certificates(Badge<Request>, Request& request)
{
    async_certificate_requested(request.id());
}

Messages::RequestServer::SetCertificateResponse ClientConnection::set_certificate(i32 request_id, String const& certificate, String const& key)
{
    auto* request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr));
    bool success = false;
    if (request) {
        request->set_certificate(certificate, key);
        success = true;
    }
    return success;
}

}
