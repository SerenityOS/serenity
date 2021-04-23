/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <DownloadServer/DownloadClientEndpoint.h>
#include <DownloadServer/DownloadServerEndpoint.h>
#include <LibIPC/ServerConnection.h>

namespace Protocol {

class Download;

class DownloadClient
    : public IPC::ServerConnection<DownloadClientEndpoint, DownloadServerEndpoint>
    , public DownloadClientEndpoint {
    C_OBJECT(DownloadClient);

public:
    virtual void handshake() override;

    bool is_supported_protocol(const String&);
    template<typename RequestHashMapTraits = Traits<String>>
    RefPtr<Download> start_download(const String& method, const String& url, const HashMap<String, String, RequestHashMapTraits>& request_headers = {}, ReadonlyBytes request_body = {});

    bool stop_download(Badge<Download>, Download&);
    bool set_certificate(Badge<Download>, Download&, String, String);

private:
    DownloadClient();

    virtual void handle(const Messages::DownloadClient::DownloadProgress&) override;
    virtual void handle(const Messages::DownloadClient::DownloadFinished&) override;
    virtual OwnPtr<Messages::DownloadClient::CertificateRequestedResponse> handle(const Messages::DownloadClient::CertificateRequested&) override;
    virtual void handle(const Messages::DownloadClient::HeadersBecameAvailable&) override;

    HashMap<i32, RefPtr<Download>> m_downloads;
};

}
