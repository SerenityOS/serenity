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
    handshake();
}

void RequestClient::handshake()
{
    send_sync<Messages::RequestServer::Greet>();
}

bool RequestClient::is_supported_protocol(const String& protocol)
{
    return send_sync<Messages::RequestServer::IsSupportedProtocol>(protocol)->supported();
}

template<typename RequestHashMapTraits>
RefPtr<Request> RequestClient::start_request(const String& method, const String& url, const HashMap<String, String, RequestHashMapTraits>& request_headers, ReadonlyBytes request_body)
{
    IPC::Dictionary header_dictionary;
    for (auto& it : request_headers)
        header_dictionary.add(it.key, it.value);

    auto response = send_sync<Messages::RequestServer::StartRequest>(method, url, header_dictionary, ByteBuffer::copy(request_body));
    auto request_id = response->request_id();
    if (request_id < 0 || !response->response_fd().has_value())
        return nullptr;
    auto response_fd = response->response_fd().value().take_fd();
    auto request = Request::create_from_id({}, *this, request_id);
    request->set_request_fd({}, response_fd);
    m_requests.set(request_id, request);
    return request;
}

bool RequestClient::stop_request(Badge<Request>, Request& request)
{
    if (!m_requests.contains(request.id()))
        return false;
    return send_sync<Messages::RequestServer::StopRequest>(request.id())->success();
}

bool RequestClient::set_certificate(Badge<Request>, Request& request, String certificate, String key)
{
    if (!m_requests.contains(request.id()))
        return false;
    return send_sync<Messages::RequestServer::SetCertificate>(request.id(), move(certificate), move(key))->success();
}

void RequestClient::handle(const Messages::RequestClient::RequestFinished& message)
{
    RefPtr<Request> request;
    if ((request = m_requests.get(message.request_id()).value_or(nullptr))) {
        request->did_finish({}, message.success(), message.total_size());
    }
    m_requests.remove(message.request_id());
}

void RequestClient::handle(const Messages::RequestClient::RequestProgress& message)
{
    if (auto request = const_cast<Request*>(m_requests.get(message.request_id()).value_or(nullptr))) {
        request->did_progress({}, message.total_size(), message.downloaded_size());
    }
}

void RequestClient::handle(const Messages::RequestClient::HeadersBecameAvailable& message)
{
    if (auto request = const_cast<Request*>(m_requests.get(message.request_id()).value_or(nullptr))) {
        HashMap<String, String, CaseInsensitiveStringTraits> headers;
        message.response_headers().for_each_entry([&](auto& name, auto& value) { headers.set(name, value); });
        request->did_receive_headers({}, headers, message.status_code());
    }
}

OwnPtr<Messages::RequestClient::CertificateRequestedResponse> RequestClient::handle(const Messages::RequestClient::CertificateRequested& message)
{
    if (auto request = const_cast<Request*>(m_requests.get(message.request_id()).value_or(nullptr))) {
        request->did_request_certificates({});
    }

    return make<Messages::RequestClient::CertificateRequestedResponse>();
}

}

template RefPtr<Protocol::Request> Protocol::RequestClient::start_request(const String& method, const String& url, const HashMap<String, String>& request_headers, ReadonlyBytes request_body);
template RefPtr<Protocol::Request> Protocol::RequestClient::start_request(const String& method, const String& url, const HashMap<String, String, CaseInsensitiveStringTraits>& request_headers, ReadonlyBytes request_body);
