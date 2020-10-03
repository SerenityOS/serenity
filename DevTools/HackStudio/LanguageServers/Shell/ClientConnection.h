/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "AutoComplete.h"
#include <AK/HashMap.h>
#include <AK/LexicalPath.h>
#include <DevTools/HackStudio/AutoCompleteResponse.h>
#include <LibGUI/TextDocument.h>
#include <LibIPC/ClientConnection.h>

#include <DevTools/HackStudio/LanguageServers/LanguageClientEndpoint.h>
#include <DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>

namespace LanguageServers::Shell {

class ClientConnection final
    : public IPC::ClientConnection<LanguageClientEndpoint, LanguageServerEndpoint>
    , public LanguageServerEndpoint {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

private:
    virtual OwnPtr<Messages::LanguageServer::GreetResponse> handle(const Messages::LanguageServer::Greet&) override;
    virtual void handle(const Messages::LanguageServer::FileOpened&) override;
    virtual void handle(const Messages::LanguageServer::FileEditInsertText&) override;
    virtual void handle(const Messages::LanguageServer::FileEditRemoveText&) override;
    virtual void handle(const Messages::LanguageServer::SetFileContent&) override;
    virtual void handle(const Messages::LanguageServer::AutoCompleteSuggestions&) override;

    RefPtr<GUI::TextDocument> document_for(const String& file_name);

    LexicalPath m_project_root;
    HashMap<String, NonnullRefPtr<GUI::TextDocument>> m_open_files;

    AutoComplete m_autocomplete;
};

}
