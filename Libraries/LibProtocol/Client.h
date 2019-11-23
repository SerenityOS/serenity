#pragma once

#include <LibCore/CoreIPCClient.h>
#include <ProtocolServer/ProtocolClientEndpoint.h>
#include <ProtocolServer/ProtocolServerEndpoint.h>

namespace LibProtocol {

class Client : public IPC::Client::ConnectionNG<ProtocolClientEndpoint, ProtocolServerEndpoint>
    , public ProtocolClientEndpoint {
    C_OBJECT(Client)
public:
    Client();

    virtual void handshake() override;

    bool is_supported_protocol(const String&);
    i32 start_download(const String& url);
    bool stop_download(i32 download_id);

    Function<void(i32 download_id, bool success)> on_download_finish;
    Function<void(i32 download_id, u64 total_size, u64 downloaded_size)> on_download_progress;

private:
    virtual void handle(const ProtocolClient::DownloadProgress&) override;
    virtual void handle(const ProtocolClient::DownloadFinished&) override;
};

}
