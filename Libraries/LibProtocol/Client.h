/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    RefPtr<Download> start_download(const String& method, const String& url, const HashMap<String, String>& request_headers = {}, const ByteBuffer& request_body = {});

    bool stop_download(Badge<Download>, Download&);
    bool set_certificate(Badge<Download>, Download&, String, String);

private:
    Client();

    virtual void handle(const Messages::ProtocolClient::DownloadProgress&) override;
    virtual void handle(const Messages::ProtocolClient::DownloadFinished&) override;
    virtual OwnPtr<Messages::ProtocolClient::CertificateRequestedResponse> handle(const Messages::ProtocolClient::CertificateRequested&) override;

    HashMap<i32, RefPtr<Download>> m_downloads;
};

}
