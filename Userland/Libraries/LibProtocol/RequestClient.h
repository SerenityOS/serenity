/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ServerConnection.h>
#include <RequestServer/RequestClientEndpoint.h>
#include <RequestServer/RequestServerEndpoint.h>

namespace Protocol {

class Request;

class RequestClient
    : public IPC::ServerConnection<RequestClientEndpoint, RequestServerEndpoint>
    , public RequestClientEndpoint {
    C_OBJECT(RequestClient);

public:
    virtual void handshake() override;

    bool is_supported_protocol(const String&);
    template<typename RequestHashMapTraits = Traits<String>>
    RefPtr<Request> start_request(const String& method, const String& url, const HashMap<String, String, RequestHashMapTraits>& request_headers = {}, ReadonlyBytes request_body = {});

    bool stop_request(Badge<Request>, Request&);
    bool set_certificate(Badge<Request>, Request&, String, String);

private:
    RequestClient();

    virtual void handle(const Messages::RequestClient::RequestProgress&) override;
    virtual void handle(const Messages::RequestClient::RequestFinished&) override;
    virtual OwnPtr<Messages::RequestClient::CertificateRequestedResponse> handle(const Messages::RequestClient::CertificateRequested&) override;
    virtual void handle(const Messages::RequestClient::HeadersBecameAvailable&) override;

    HashMap<i32, RefPtr<Request>> m_requests;
};

}
