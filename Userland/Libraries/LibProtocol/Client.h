/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ServerConnection.h>
#include <ProtocolServer/ProtocolClientEndpoint.h>
#include <ProtocolServer/ProtocolServerEndpoint.h>

namespace Protocol {

class Download;

class Client
    : public IPC::ServerConnection<ProtocolClientEndpoint, ProtocolServerEndpoint>
    , public ProtocolClientEndpoint {
    C_OBJECT(Client);

public:
    virtual void handshake() override;

    bool is_supported_protocol(const String&);
    template<typename RequestHashMapTraits = Traits<String>>
    RefPtr<Download> start_download(const String& method, const String& url, const HashMap<String, String, RequestHashMapTraits>& request_headers = {}, ReadonlyBytes request_body = {});

    bool stop_download(Badge<Download>, Download&);
    bool set_certificate(Badge<Download>, Download&, String, String);

private:
    Client();

    virtual void handle(const Messages::ProtocolClient::DownloadProgress&) override;
    virtual void handle(const Messages::ProtocolClient::DownloadFinished&) override;
    virtual OwnPtr<Messages::ProtocolClient::CertificateRequestedResponse> handle(const Messages::ProtocolClient::CertificateRequested&) override;
    virtual void handle(const Messages::ProtocolClient::HeadersBecameAvailable&) override;

    HashMap<i32, RefPtr<Download>> m_downloads;
};

}
