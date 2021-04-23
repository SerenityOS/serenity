/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FileStream.h>
#include <LibProtocol/Download.h>
#include <LibProtocol/DownloadClient.h>

namespace Protocol {

DownloadClient::DownloadClient()
    : IPC::ServerConnection<DownloadClientEndpoint, DownloadServerEndpoint>(*this, "/tmp/portal/download")
{
    handshake();
}

void DownloadClient::handshake()
{
    send_sync<Messages::DownloadServer::Greet>();
}

bool DownloadClient::is_supported_protocol(const String& protocol)
{
    return send_sync<Messages::DownloadServer::IsSupportedProtocol>(protocol)->supported();
}

template<typename RequestHashMapTraits>
RefPtr<Download> DownloadClient::start_download(const String& method, const String& url, const HashMap<String, String, RequestHashMapTraits>& request_headers, ReadonlyBytes request_body)
{
    IPC::Dictionary header_dictionary;
    for (auto& it : request_headers)
        header_dictionary.add(it.key, it.value);

    auto response = send_sync<Messages::DownloadServer::StartDownload>(method, url, header_dictionary, ByteBuffer::copy(request_body));
    auto download_id = response->download_id();
    if (download_id < 0 || !response->response_fd().has_value())
        return nullptr;
    auto response_fd = response->response_fd().value().take_fd();
    auto download = Download::create_from_id({}, *this, download_id);
    download->set_download_fd({}, response_fd);
    m_downloads.set(download_id, download);
    return download;
}

bool DownloadClient::stop_download(Badge<Download>, Download& download)
{
    if (!m_downloads.contains(download.id()))
        return false;
    return send_sync<Messages::DownloadServer::StopDownload>(download.id())->success();
}

bool DownloadClient::set_certificate(Badge<Download>, Download& download, String certificate, String key)
{
    if (!m_downloads.contains(download.id()))
        return false;
    return send_sync<Messages::DownloadServer::SetCertificate>(download.id(), move(certificate), move(key))->success();
}

void DownloadClient::handle(const Messages::DownloadClient::DownloadFinished& message)
{
    RefPtr<Download> download;
    if ((download = m_downloads.get(message.download_id()).value_or(nullptr))) {
        download->did_finish({}, message.success(), message.total_size());
    }
    m_downloads.remove(message.download_id());
}

void DownloadClient::handle(const Messages::DownloadClient::DownloadProgress& message)
{
    if (auto download = const_cast<Download*>(m_downloads.get(message.download_id()).value_or(nullptr))) {
        download->did_progress({}, message.total_size(), message.downloaded_size());
    }
}

void DownloadClient::handle(const Messages::DownloadClient::HeadersBecameAvailable& message)
{
    if (auto download = const_cast<Download*>(m_downloads.get(message.download_id()).value_or(nullptr))) {
        HashMap<String, String, CaseInsensitiveStringTraits> headers;
        message.response_headers().for_each_entry([&](auto& name, auto& value) { headers.set(name, value); });
        download->did_receive_headers({}, headers, message.status_code());
    }
}

OwnPtr<Messages::DownloadClient::CertificateRequestedResponse> DownloadClient::handle(const Messages::DownloadClient::CertificateRequested& message)
{
    if (auto download = const_cast<Download*>(m_downloads.get(message.download_id()).value_or(nullptr))) {
        download->did_request_certificates({});
    }

    return make<Messages::DownloadClient::CertificateRequestedResponse>();
}

}

template RefPtr<Protocol::Download> Protocol::DownloadClient::start_download(const String& method, const String& url, const HashMap<String, String>& request_headers, ReadonlyBytes request_body);
template RefPtr<Protocol::Download> Protocol::DownloadClient::start_download(const String& method, const String& url, const HashMap<String, String, CaseInsensitiveStringTraits>& request_headers, ReadonlyBytes request_body);
