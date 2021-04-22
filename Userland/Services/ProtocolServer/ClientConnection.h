/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <ProtocolServer/Forward.h>
#include <ProtocolServer/ProtocolClientEndpoint.h>
#include <ProtocolServer/ProtocolServerEndpoint.h>

namespace ProtocolServer {

class ClientConnection final
    : public IPC::ClientConnection<ProtocolClientEndpoint, ProtocolServerEndpoint>
    , public ProtocolServerEndpoint {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

    void did_receive_headers(Badge<Download>, Download&);
    void did_finish_download(Badge<Download>, Download&, bool success);
    void did_progress_download(Badge<Download>, Download&);
    void did_request_certificates(Badge<Download>, Download&);

private:
    virtual OwnPtr<Messages::ProtocolServer::GreetResponse> handle(const Messages::ProtocolServer::Greet&) override;
    virtual OwnPtr<Messages::ProtocolServer::IsSupportedProtocolResponse> handle(const Messages::ProtocolServer::IsSupportedProtocol&) override;
    virtual OwnPtr<Messages::ProtocolServer::StartDownloadResponse> handle(const Messages::ProtocolServer::StartDownload&) override;
    virtual OwnPtr<Messages::ProtocolServer::StopDownloadResponse> handle(const Messages::ProtocolServer::StopDownload&) override;
    virtual OwnPtr<Messages::ProtocolServer::SetCertificateResponse> handle(const Messages::ProtocolServer::SetCertificate&) override;

    HashMap<i32, OwnPtr<Download>> m_downloads;
};

}
