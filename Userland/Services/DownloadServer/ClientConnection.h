/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <DownloadServer/DownloadClientEndpoint.h>
#include <DownloadServer/DownloadServerEndpoint.h>
#include <DownloadServer/Forward.h>
#include <LibIPC/ClientConnection.h>

namespace DownloadServer {

class ClientConnection final
    : public IPC::ClientConnection<DownloadClientEndpoint, DownloadServerEndpoint>
    , public DownloadServerEndpoint {
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
    virtual OwnPtr<Messages::DownloadServer::GreetResponse> handle(const Messages::DownloadServer::Greet&) override;
    virtual OwnPtr<Messages::DownloadServer::IsSupportedProtocolResponse> handle(const Messages::DownloadServer::IsSupportedProtocol&) override;
    virtual OwnPtr<Messages::DownloadServer::StartDownloadResponse> handle(const Messages::DownloadServer::StartDownload&) override;
    virtual OwnPtr<Messages::DownloadServer::StopDownloadResponse> handle(const Messages::DownloadServer::StopDownload&) override;
    virtual OwnPtr<Messages::DownloadServer::SetCertificateResponse> handle(const Messages::DownloadServer::SetCertificate&) override;

    HashMap<i32, OwnPtr<Download>> m_downloads;
};

}
