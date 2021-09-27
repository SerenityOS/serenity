/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FileStream.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>

namespace Protocol {

RequestClient::RequestClient()
    : IPC::ServerConnection<RequestClientEndpoint, RequestServerEndpoint>(*this, "/tmp/portal/request")
{
}

void RequestClient::ensure_connection(URL const& url, ::RequestServer::CacheLevel cache_level)
{
    async_ensure_connection(url, cache_level);
}

template<typename RequestHashMapTraits>
RefPtr<Request> RequestClient::start_request(String const& method, URL const& url, HashMap<String, String, RequestHashMapTraits> const& request_headers, ReadonlyBytes request_body)
{
    IPC::Dictionary header_dictionary;
    for (auto& it : request_headers)
        header_dictionary.add(it.key, it.value);

    auto body_result = ByteBuffer::copy(request_body);
    if (!body_result.has_value())
        return nullptr;

    auto response = IPCProxy::start_request(method, url, header_dictionary, body_result.release_value());
    auto request_id = response.request_id();
    if (request_id < 0 || !response.response_fd().has_value())
        return nullptr;
    auto response_fd = response.response_fd().value().take_fd();
    auto request = Request::create_from_id({}, *this, request_id);
    request->set_request_fd({}, response_fd);
    m_requests.set(request_id, request);
    return request;
    return nullptr;
}

bool RequestClient::stop_request(Badge<Request>, Request& request)
{
    if (!m_requests.contains(request.id()))
        return false;
    return IPCProxy::stop_request(request.id());
}

bool RequestClient::set_certificate(Badge<Request>, Request& request, String certificate, String key)
{
    if (!m_requests.contains(request.id()))
        return false;
    return IPCProxy::set_certificate(request.id(), move(certificate), move(key));
}

void RequestClient::request_finished(i32 request_id, bool success, u32 total_size)
{
    RefPtr<Request> request;
    if ((request = m_requests.get(request_id).value_or(nullptr))) {
        request->did_finish({}, success, total_size);
    }
    m_requests.remove(request_id);
}

void RequestClient::request_progress(i32 request_id, Optional<u32> const& total_size, u32 downloaded_size)
{
    if (auto request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr))) {
        request->did_progress({}, total_size, downloaded_size);
    }
}

void RequestClient::headers_became_available(i32 request_id, IPC::Dictionary const& response_headers, Optional<u32> const& status_code)
{
    if (auto request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr))) {
        HashMap<String, String, CaseInsensitiveStringTraits> headers;
        response_headers.for_each_entry([&](auto& name, auto& value) { headers.set(name, value); });
        request->did_receive_headers({}, headers, status_code);
    }
}

void RequestClient::certificate_requested(i32 request_id)
{
    if (auto request = const_cast<Request*>(m_requests.get(request_id).value_or(nullptr))) {
        request->did_request_certificates({});
    }
}

}

template RefPtr<Protocol::Request> Protocol::RequestClient::start_request(String const& method, URL const&, HashMap<String, String> const& request_headers, ReadonlyBytes request_body);
template RefPtr<Protocol::Request> Protocol::RequestClient::start_request(String const& method, URL const&, HashMap<String, String, CaseInsensitiveStringTraits> const& request_headers, ReadonlyBytes request_body);
