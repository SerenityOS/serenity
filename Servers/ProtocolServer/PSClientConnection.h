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

#include <AK/Badge.h>
#include <LibIPC/IClientConnection.h>
#include <ProtocolServer/ProtocolServerEndpoint.h>

namespace AK {
class SharedBuffer;
}

class Download;

class PSClientConnection final : public IPC::ClientConnection<ProtocolServerEndpoint>
    , public ProtocolServerEndpoint {
    C_OBJECT(PSClientConnection)
public:
    explicit PSClientConnection(Core::LocalSocket&, int client_id);
    ~PSClientConnection() override;

    virtual void die() override;

    void did_finish_download(Badge<Download>, Download&, bool success);
    void did_progress_download(Badge<Download>, Download&);

private:
    virtual OwnPtr<ProtocolServer::GreetResponse> handle(const ProtocolServer::Greet&) override;
    virtual OwnPtr<ProtocolServer::IsSupportedProtocolResponse> handle(const ProtocolServer::IsSupportedProtocol&) override;
    virtual OwnPtr<ProtocolServer::StartDownloadResponse> handle(const ProtocolServer::StartDownload&) override;
    virtual OwnPtr<ProtocolServer::StopDownloadResponse> handle(const ProtocolServer::StopDownload&) override;
    virtual OwnPtr<ProtocolServer::DisownSharedBufferResponse> handle(const ProtocolServer::DisownSharedBuffer&) override;

    HashMap<i32, RefPtr<AK::SharedBuffer>> m_shared_buffers;
};
