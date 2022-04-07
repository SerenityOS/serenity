/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
// Need to include this before RequestClientEndpoint.h as that one includes LibIPC/(De En)coder.h, which would bomb if included before this.
#include <LibCore/Proxy.h>
#include <LibIPC/ConnectionToServer.h>
#include <RequestServer/RequestClientEndpoint.h>
#include <RequestServer/RequestServerEndpoint.h>

namespace Protocol {

class Request;

class RequestClient final
    : public IPC::ConnectionToServer<RequestClientEndpoint, RequestServerEndpoint>
    , public RequestClientEndpoint {
    IPC_CLIENT_CONNECTION(RequestClient, "/tmp/portal/request")

public:
    template<typename RequestHashMapTraits = Traits<String>>
    RefPtr<Request> start_request(String const& method, URL const&, HashMap<String, String, RequestHashMapTraits> const& request_headers = {}, ReadonlyBytes request_body = {}, Core::ProxyData const& = {});

    void ensure_connection(URL const&, ::RequestServer::CacheLevel);

    bool stop_request(Badge<Request>, Request&);
    bool set_certificate(Badge<Request>, Request&, String, String);

private:
    RequestClient(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual void request_progress(i32, Optional<u32> const&, u32) override;
    virtual void request_finished(i32, bool, u32) override;
    virtual void certificate_requested(i32) override;
    virtual void headers_became_available(i32, IPC::Dictionary const&, Optional<u32> const&) override;

    HashMap<i32, RefPtr<Request>> m_requests;
};

}
