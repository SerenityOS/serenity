#pragma once

#include <AK/Badge.h>
#include <LibCore/CoreIPCServer.h>
#include <ProtocolServer/ProtocolServerEndpoint.h>

class Download;

class PSClientConnection final : public IPC::Server::ConnectionNG<ProtocolServerEndpoint>
    , public ProtocolServerEndpoint {
    C_OBJECT(PSClientConnection)
public:
    explicit PSClientConnection(CLocalSocket&, int client_id);
    ~PSClientConnection() override;

    virtual void die() override;

    void did_finish_download(Badge<Download>, Download&, bool success);
    void did_progress_download(Badge<Download>, Download&);

private:
    virtual OwnPtr<ProtocolServer::GreetResponse> handle(const ProtocolServer::Greet&) override;
    virtual OwnPtr<ProtocolServer::IsSupportedProtocolResponse> handle(const ProtocolServer::IsSupportedProtocol&) override;
    virtual OwnPtr<ProtocolServer::StartDownloadResponse> handle(const ProtocolServer::StartDownload&) override;
    virtual OwnPtr<ProtocolServer::StopDownloadResponse> handle(const ProtocolServer::StopDownload&) override;
};
