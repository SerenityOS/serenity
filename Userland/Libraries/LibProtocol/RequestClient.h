/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionToServer.h>
#include <RequestServer/RequestClientEndpoint.h>
#include <RequestServer/RequestServerEndpoint.h>

namespace Protocol {

class Request;

class RequestClient final
    : public IPC::ConnectionToServer<RequestClientEndpoint, RequestServerEndpoint>
    , public RequestClientEndpoint {
    IPC_CLIENT_CONNECTION(RequestClient, "/tmp/session/%sid/portal/request"sv)

public:
    explicit RequestClient(NonnullOwnPtr<Core::LocalSocket>);

    template<typename RequestHashMapTraits = Traits<DeprecatedString>>
    RefPtr<Request> start_request(DeprecatedString const& method, URL const&, HashMap<DeprecatedString, DeprecatedString, RequestHashMapTraits> const& request_headers = {}, ReadonlyBytes request_body = {}, Core::ProxyData const& = {});

    void ensure_connection(URL const&, ::RequestServer::CacheLevel);

    bool stop_request(Badge<Request>, Request&);
    bool set_certificate(Badge<Request>, Request&, DeprecatedString, DeprecatedString);

private:
    virtual void request_progress(i32, Optional<u64> const&, u64) override;
    virtual void request_finished(i32, bool, u64) override;
    virtual void certificate_requested(i32) override;
    virtual void headers_became_available(i32, HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> const&, Optional<u32> const&) override;

    HashMap<i32, RefPtr<Request>> m_requests;
};

}
