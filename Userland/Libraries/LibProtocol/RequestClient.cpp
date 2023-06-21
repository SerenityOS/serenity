/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>

namespace Protocol {

RequestClient::RequestClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<RequestClientEndpoint, RequestServerEndpoint>(*this, move(socket))
{
}

void RequestClient::ensure_connection(URL const& url, ::RequestServer::CacheLevel cache_level)
{
    async_ensure_connection(url, cache_level);
}

template<typename RequestHashMapTraits>
RefPtr<Request> RequestClient::start_request(DeprecatedString const& method, URL const& url, HashMap<DeprecatedString, DeprecatedString, RequestHashMapTraits> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const& proxy_data)
{
    auto headers_or_error = request_headers.template clone<Traits<DeprecatedString>>();
    if (headers_or_error.is_error())
        return nullptr;
    auto body_result = ByteBuffer::copy(request_body);
    if (body_result.is_error())
        return nullptr;

    auto response = IPCProxy::start_request(method, url, headers_or_error.release_value(), body_result.release_value(), proxy_data);
    auto request_id = response.request_id();
    if (request_id < 0 || !response.response_fd().has_value())
        return nullptr;
    auto response_fd = response.response_fd().value().take_fd();
    auto request = Request::create_from_id({}, *this, request_id);
    request->set_request_fd({}, response_fd);
    m_requests.set(request_id, request);
    return request;
}

bool RequestClient::stop_request(Badge<Request>, Request& request)
{
    if (!m_requests.contains(request.id()))
        return false;
    return IPCProxy::stop_request(request.id());
}

bool RequestClient::set_certificate(Badge<Request>, Request& request, DeprecatedString certificate, DeprecatedString key)
{
    if (!m_requests.contains(request.id()))
        return false;
    return IPCProxy::set_certificate(request.id(), move(certificate), move(key));
}

void RequestClient::request_finished(i32 request_id, bool success, u64 total_size)
{
    RefPtr<Request> request;
    if ((request = m_requests.get(request_id).value_or(nullptr))) {
        request->did_finish({}, success, total_size);
    }
    m_requests.remove(request_id);
}

void RequestClient::request_progress(i32 request_id, Optional<u64> const& total_size, u64 downloaded_size)
{
    if (auto request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr))) {
        request->did_progress({}, total_size, downloaded_size);
    }
}

void RequestClient::headers_became_available(i32 request_id, HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> const& status_code)
{
    auto request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr));
    if (!request) {
        warnln("Received headers for non-existent request {}", request_id);
        return;
    }
    auto response_headers_clone_or_error = response_headers.clone();
    if (response_headers_clone_or_error.is_error()) {
        warnln("Error while receiving headers for request {}: {}", request_id, response_headers_clone_or_error.error());
        return;
    }

    request->did_receive_headers({}, response_headers_clone_or_error.release_value(), status_code);
}

void RequestClient::certificate_requested(i32 request_id)
{
    if (auto request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr))) {
        request->did_request_certificates({});
    }
}

}

template RefPtr<Protocol::Request> Protocol::RequestClient::start_request(DeprecatedString const& method, URL const&, HashMap<DeprecatedString, DeprecatedString> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&);
template RefPtr<Protocol::Request> Protocol::RequestClient::start_request(DeprecatedString const& method, URL const&, HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> const& request_headers, ReadonlyBytes request_body, Core::ProxyData const&);
