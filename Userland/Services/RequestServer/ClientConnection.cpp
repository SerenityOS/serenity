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
#include <netdb.h>

namespace RequestServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : IPC::ClientConnection<RequestClientEndpoint, RequestServerEndpoint>(*this, move(socket), 1)
{
    s_connections.set(1, *this);
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

void ClientConnection::ensure_connection(URL const& url, ::RequestServer::CacheLevel const& cache_level)
{
    if (!url.is_valid()) {
        dbgln("EnsureConnection: Invalid URL requested: '{}'", url);
        return;
    }

    if (cache_level == CacheLevel::ResolveOnly) {
        return Core::deferred_invoke([host = url.host()] {
            dbgln("EnsureConnection: DNS-preload for {}", host);
            (void)gethostbyname(host.characters());
        });
    }

    struct {
        URL const& m_url;
        void start(Core::Stream::Socket& socket)
        {
            auto is_connected = socket.is_open();
            VERIFY(is_connected);
            ConnectionCache::request_did_finish(m_url, &socket);
        }
        void fail(Core::NetworkJob::Error error)
        {
            dbgln("Pre-connect to {} failed: {}", m_url, Core::to_string(error));
        }
    } job { url };

    dbgln("EnsureConnection: Pre-connect to {}", url);
    auto do_preconnect = [&](auto& cache) {
        auto it = cache.find({ url.host(), url.port_or_default() });
        if (it == cache.end() || it->value->is_empty())
            ConnectionCache::get_or_create_connection(cache, url, job);
    };

    if (url.scheme() == "http"sv)
        do_preconnect(ConnectionCache::g_tcp_connection_cache);
    else if (url.scheme() == "https"sv)
        do_preconnect(ConnectionCache::g_tls_connection_cache);
    else
        dbgln("EnsureConnection: Invalid URL scheme: '{}'", url.scheme());
}

}
