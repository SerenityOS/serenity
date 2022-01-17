/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <RequestServer/Forward.h>
#include <RequestServer/RequestClientEndpoint.h>
#include <RequestServer/RequestServerEndpoint.h>

namespace RequestServer {

class ClientConnection final
    : public IPC::ClientConnection<RequestClientEndpoint, RequestServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    ~ClientConnection() override;

    virtual void die() override;

    void did_receive_headers(Badge<Request>, Request&);
    void did_finish_request(Badge<Request>, Request&, bool success);
    void did_progress_request(Badge<Request>, Request&);
    void did_request_certificates(Badge<Request>, Request&);

private:
    explicit ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual Messages::RequestServer::IsSupportedProtocolResponse is_supported_protocol(String const&) override;
    virtual Messages::RequestServer::StartRequestResponse start_request(String const&, URL const&, IPC::Dictionary const&, ByteBuffer const&) override;
    virtual Messages::RequestServer::StopRequestResponse stop_request(i32) override;
    virtual Messages::RequestServer::SetCertificateResponse set_certificate(i32, String const&, String const&) override;
    virtual void ensure_connection(URL const& url, ::RequestServer::CacheLevel const& cache_level) override;

    HashMap<i32, OwnPtr<Request>> m_requests;
};

}
