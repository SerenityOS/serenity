/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/NonnullOwnPtr.h>
#include <LibCore/Proxy.h>
#include <RequestServer/ConnectionFromClient.h>
#include <RequestServer/Protocol.h>
#include <RequestServer/Request.h>
#include <RequestServer/RequestClientEndpoint.h>
#include <netdb.h>

namespace RequestServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionFromClient<RequestClientEndpoint, RequestServerEndpoint>(*this, move(socket), 1)
{
    s_connections.set(1, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
    if (s_connections.is_empty())
        Core::EventLoop::current().quit(0);
}

Messages::RequestServer::IsSupportedProtocolResponse ConnectionFromClient::is_supported_protocol(DeprecatedString const& protocol)
{
    bool supported = Protocol::find_by_name(protocol.to_lowercase());
    return supported;
}

Messages::RequestServer::StartRequestResponse ConnectionFromClient::start_request(DeprecatedString const& method, URL const& url, HashMap<DeprecatedString, DeprecatedString> const& request_headers, ByteBuffer const& request_body, Core::ProxyData const& proxy_data)
{
    if (!url.is_valid()) {
        dbgln("StartRequest: Invalid URL requested: '{}'", url);
        return { -1, Optional<IPC::File> {} };
    }
    auto* protocol = Protocol::find_by_name(url.scheme().to_deprecated_string());
    if (!protocol) {
        dbgln("StartRequest: No protocol handler for URL: '{}'", url);
        return { -1, Optional<IPC::File> {} };
    }
    auto request = protocol->start_request(*this, method, url, request_headers, request_body, proxy_data);
    if (!request) {
        dbgln("StartRequest: Protocol handler failed to start request: '{}'", url);
        return { -1, Optional<IPC::File> {} };
    }
    auto id = request->id();
    auto fd = request->request_fd();
    m_requests.set(id, move(request));
    return { id, IPC::File(fd, IPC::File::CloseAfterSending) };
}

Messages::RequestServer::StopRequestResponse ConnectionFromClient::stop_request(i32 request_id)
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

void ConnectionFromClient::did_receive_headers(Badge<Request>, Request& request)
{
    auto response_headers = request.response_headers().clone().release_value_but_fixme_should_propagate_errors();
    async_headers_became_available(request.id(), move(response_headers), request.status_code());
}

void ConnectionFromClient::did_finish_request(Badge<Request>, Request& request, bool success)
{
    if (request.total_size().has_value())
        async_request_finished(request.id(), success, request.total_size().value());

    m_requests.remove(request.id());
}

void ConnectionFromClient::did_progress_request(Badge<Request>, Request& request)
{
    async_request_progress(request.id(), request.total_size(), request.downloaded_size());
}

void ConnectionFromClient::did_request_certificates(Badge<Request>, Request& request)
{
    async_certificate_requested(request.id());
}

Messages::RequestServer::SetCertificateResponse ConnectionFromClient::set_certificate(i32 request_id, DeprecatedString const& certificate, DeprecatedString const& key)
{
    auto* request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr));
    bool success = false;
    if (request) {
        request->set_certificate(certificate, key);
        success = true;
    }
    return success;
}

struct Job {
    explicit Job(URL url)
        : m_url(move(url))
    {
    }

    static Job& ensure(URL const& url)
    {
        if (auto it = s_jobs.find(url); it == s_jobs.end()) {
            auto job = make<Job>(url);
            s_jobs.set(url, move(job));
        }
        return *s_jobs.find(url)->value;
    }

    void start(Core::Socket& socket)
    {
        auto is_connected = socket.is_open();
        VERIFY(is_connected);
        ConnectionCache::request_did_finish(m_url, &socket);
        s_jobs.remove(m_url);
    }
    void fail(Core::NetworkJob::Error error)
    {
        dbgln("Pre-connect to {} failed: {}", m_url, Core::to_string(error));
        s_jobs.remove(m_url);
    }
    URL m_url;

private:
    static HashMap<URL, NonnullOwnPtr<Job>> s_jobs;
};
HashMap<URL, NonnullOwnPtr<Job>> Job::s_jobs {};

void ConnectionFromClient::ensure_connection(URL const& url, ::RequestServer::CacheLevel const& cache_level)
{
    if (!url.is_valid()) {
        dbgln("EnsureConnection: Invalid URL requested: '{}'", url);
        return;
    }

    if (cache_level == CacheLevel::ResolveOnly) {
        return Core::deferred_invoke([host = url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string()] {
            dbgln("EnsureConnection: DNS-preload for {}", host);
            (void)gethostbyname(host.characters());
        });
    }

    auto& job = Job::ensure(url);
    dbgln("EnsureConnection: Pre-connect to {}", url);
    auto do_preconnect = [&](auto& cache) {
        auto serialized_host = url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string();
        auto it = cache.find({ serialized_host, url.port_or_default() });
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
