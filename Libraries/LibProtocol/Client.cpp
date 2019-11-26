#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>
#include <SharedBuffer.h>

namespace LibProtocol {

Client::Client()
    : ConnectionNG(*this, "/tmp/portal/protocol")
{
    handshake();
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

RefPtr<Download> Client::start_download(const String& url)
{
    i32 download_id = send_sync<ProtocolServer::StartDownload>(url)->download_id();
    auto download = Download::create_from_id({}, *this, download_id);
    m_downloads.set(download_id, download);
    return download;
}

bool Client::stop_download(Badge<Download>, Download& download)
{
    if (!m_downloads.contains(download.id()))
        return false;
    return send_sync<ProtocolServer::StopDownload>(download.id())->success();
}

void Client::handle(const ProtocolClient::DownloadFinished& message)
{
    RefPtr<Download> download;
    if ((download = m_downloads.get(message.download_id()).value_or(nullptr))) {
        download->did_finish({}, message.success(), message.total_size(), message.shared_buffer_id());
    }
    send_sync<ProtocolServer::DisownSharedBuffer>(message.shared_buffer_id());
    m_downloads.remove(message.download_id());
}

void Client::handle(const ProtocolClient::DownloadProgress& message)
{
    if (auto download = m_downloads.get(message.download_id()).value_or(nullptr)) {
        download->did_progress({}, message.total_size(), message.downloaded_size());
    }
}

}
