/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include <AK/LexicalPath.h>
#include <DevTools/HackStudio/LanguageServers/Cpp/CppLanguageClientEndpoint.h>
#include <DevTools/HackStudio/LanguageServers/Cpp/CppLanguageServerEndpoint.h>
#include <LibIPC/ServerConnection.h>

namespace LanguageClients {
namespace Cpp {

class ServerConnection : public IPC::ServerConnection<CppLanguageClientEndpoint, CppLanguageServerEndpoint>
    , public CppLanguageClientEndpoint {
    C_OBJECT(ServerConnection)
public:
    virtual void handshake() override
    {
        auto response = send_sync<Messages::CppLanguageServer::Greet>(m_project_path.string());
        set_my_client_id(response->client_id());
    }

private:
    ServerConnection(const String& project_path)
        : IPC::ServerConnection<CppLanguageClientEndpoint, CppLanguageServerEndpoint>(*this, "/tmp/portal/language/cpp")
        , m_project_path(project_path)
    {
    }

    virtual void handle(const Messages::CppLanguageClient::Dummy&) override { }

    LexicalPath m_project_path;
};

}
}
