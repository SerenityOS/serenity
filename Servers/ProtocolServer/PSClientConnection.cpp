#include <ProtocolServer/Download.h>
#include <ProtocolServer/PSClientConnection.h>
#include <ProtocolServer/Protocol.h>
#include <ProtocolServer/ProtocolClientEndpoint.h>
#include <LibC/SharedBuffer.h>

static HashMap<int, RefPtr<PSClientConnection>> s_connections;

PSClientConnection::PSClientConnection(CLocalSocket& socket, int client_id)
    : IClientConnection(*this, socket, client_id)
{
    s_connections.set(client_id, *this);
}

PSClientConnection::~PSClientConnection()
{
}

void PSClientConnection::die()
{
    s_connections.remove(client_id());
}

OwnPtr<ProtocolServer::IsSupportedProtocolResponse> PSClientConnection::handle(const ProtocolServer::IsSupportedProtocol& message)
{
    bool supported = Protocol::find_by_name(message.protocol().to_lowercase());
    return make<ProtocolServer::IsSupportedProtocolResponse>(supported);
}

OwnPtr<ProtocolServer::StartDownloadResponse> PSClientConnection::handle(const ProtocolServer::StartDownload& message)
{
    URL url(message.url());
    ASSERT(url.is_valid());
    auto* protocol = Protocol::find_by_name(url.protocol());
    ASSERT(protocol);
    auto download = protocol->start_download(*this, url);
    return make<ProtocolServer::StartDownloadResponse>(download->id());
}

OwnPtr<ProtocolServer::StopDownloadResponse> PSClientConnection::handle(const ProtocolServer::StopDownload& message)
{
    auto* download = Download::find_by_id(message.download_id());
    bool success = false;
    if (download) {
        download->stop();
    }
    return make<ProtocolServer::StopDownloadResponse>(success);
}

void PSClientConnection::did_finish_download(Badge<Download>, Download& download, bool success)
{
    RefPtr<SharedBuffer> buffer;
    if (success && !download.payload().is_null()) {
        buffer = SharedBuffer::create_with_size(download.payload().size());
        memcpy(buffer->data(), download.payload().data(), download.payload().size());
        buffer->seal();
        buffer->share_with(client_pid());
        m_shared_buffers.set(buffer->shared_buffer_id(), buffer);
    }
    post_message(ProtocolClient::DownloadFinished(download.id(), success, download.total_size(), buffer ? buffer->shared_buffer_id() : -1));
}

void PSClientConnection::did_progress_download(Badge<Download>, Download& download)
{
    post_message(ProtocolClient::DownloadProgress(download.id(), download.total_size(), download.downloaded_size()));
}

OwnPtr<ProtocolServer::GreetResponse> PSClientConnection::handle(const ProtocolServer::Greet&)
{
    return make<ProtocolServer::GreetResponse>(client_id());
}

OwnPtr<ProtocolServer::DisownSharedBufferResponse> PSClientConnection::handle(const ProtocolServer::DisownSharedBuffer& message)
{
    m_shared_buffers.remove(message.shared_buffer_id());
    return make<ProtocolServer::DisownSharedBufferResponse>();
}
