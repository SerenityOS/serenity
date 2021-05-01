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
    : public IPC::ClientConnection<RequestClientEndpoint, RequestServerEndpoint>
    , public RequestServerEndpoint {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

    void did_receive_headers(Badge<Request>, Request&);
    void did_finish_request(Badge<Request>, Request&, bool success);
    void did_progress_request(Badge<Request>, Request&);
    void did_request_certificates(Badge<Request>, Request&);

private:
    virtual OwnPtr<Messages::RequestServer::GreetResponse> handle(const Messages::RequestServer::Greet&) override;
    virtual OwnPtr<Messages::RequestServer::IsSupportedProtocolResponse> handle(const Messages::RequestServer::IsSupportedProtocol&) override;
    virtual OwnPtr<Messages::RequestServer::StartRequestResponse> handle(const Messages::RequestServer::StartRequest&) override;
    virtual OwnPtr<Messages::RequestServer::StopRequestResponse> handle(const Messages::RequestServer::StopRequest&) override;
    virtual OwnPtr<Messages::RequestServer::SetCertificateResponse> handle(const Messages::RequestServer::SetCertificate&) override;

    HashMap<i32, OwnPtr<Request>> m_requests;
};

}
