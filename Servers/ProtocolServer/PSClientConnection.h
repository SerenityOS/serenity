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
#include <LibIPC/ClientConnection.h>
#include <ProtocolServer/ProtocolServerEndpoint.h>

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
    virtual OwnPtr<Messages::ProtocolServer::GreetResponse> handle(const Messages::ProtocolServer::Greet&) override;
    virtual OwnPtr<Messages::ProtocolServer::IsSupportedProtocolResponse> handle(const Messages::ProtocolServer::IsSupportedProtocol&) override;
    virtual OwnPtr<Messages::ProtocolServer::StartDownloadResponse> handle(const Messages::ProtocolServer::StartDownload&) override;
    virtual OwnPtr<Messages::ProtocolServer::StopDownloadResponse> handle(const Messages::ProtocolServer::StopDownload&) override;
    virtual OwnPtr<Messages::ProtocolServer::DisownSharedBufferResponse> handle(const Messages::ProtocolServer::DisownSharedBuffer&) override;

    HashMap<i32, RefPtr<AK::SharedBuffer>> m_shared_buffers;
};
