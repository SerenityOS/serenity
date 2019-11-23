#include <LibProtocol/Client.h>
#include <SharedBuffer.h>

namespace LibProtocol {

Client::Client()
    : ConnectionNG(*this, "/tmp/psportal")
{
}

void Client::handshake()
{
    auto response = send_sync<ProtocolServer::Greet>(getpid());
    set_server_pid(response->server_pid());
    set_my_client_id(response->client_id());
}

bool Client::is_supported_protocol(const String& protocol)
{
    return send_sync<ProtocolServer::IsSupportedProtocol>(protocol)->supported();
}

i32 Client::start_download(const String& url)
{
    return send_sync<ProtocolServer::StartDownload>(url)->download_id();
}

bool Client::stop_download(i32 download_id)
{
    return send_sync<ProtocolServer::StopDownload>(download_id)->success();
}

void Client::handle(const ProtocolClient::DownloadFinished& message)
{
    if (on_download_finish)
        on_download_finish(message.download_id(), message.success());
}

void Client::handle(const ProtocolClient::DownloadProgress& message)
{
    if (on_download_progress)
        on_download_progress(message.download_id(), message.total_size(), message.downloaded_size());
}

}
