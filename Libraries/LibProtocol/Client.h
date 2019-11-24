#pragma once

#include <LibCore/CoreIPCClient.h>
#include <ProtocolServer/ProtocolClientEndpoint.h>
#include <ProtocolServer/ProtocolServerEndpoint.h>

namespace LibProtocol {

class Download;

class Client : public IPC::Client::ConnectionNG<ProtocolClientEndpoint, ProtocolServerEndpoint>
    , public ProtocolClientEndpoint {
    C_OBJECT(Client)
public:
    Client();

    virtual void handshake() override;

    bool is_supported_protocol(const String&);
    RefPtr<Download> start_download(const String& url);

    bool stop_download(Badge<Download>, Download&);

private:
    virtual void handle(const ProtocolClient::DownloadProgress&) override;
    virtual void handle(const ProtocolClient::DownloadFinished&) override;

    HashMap<i32, RefPtr<Download>> m_downloads;
};

}
