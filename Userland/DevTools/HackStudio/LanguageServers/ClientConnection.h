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

#include "../AutoCompleteResponse.h"
#include "AutoCompleteEngine.h"
#include "FileDB.h"
#include <AK/HashMap.h>
#include <AK/LexicalPath.h>
#include <LibIPC/ClientConnection.h>

#include <Userland/DevTools/HackStudio/LanguageServers/LanguageClientEndpoint.h>
#include <Userland/DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>

namespace LanguageServers {

class ClientConnection
    : public IPC::ClientConnection<LanguageClientEndpoint, LanguageServerEndpoint>
    , public LanguageServerEndpoint {

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

protected:
    virtual OwnPtr<Messages::LanguageServer::GreetResponse> handle(const Messages::LanguageServer::Greet&) override;
    virtual void handle(const Messages::LanguageServer::FileOpened&) override;
    virtual void handle(const Messages::LanguageServer::FileEditInsertText&) override;
    virtual void handle(const Messages::LanguageServer::FileEditRemoveText&) override;
    virtual void handle(const Messages::LanguageServer::SetFileContent&) override;
    virtual void handle(const Messages::LanguageServer::AutoCompleteSuggestions&) override;
    virtual void handle(const Messages::LanguageServer::FindDeclaration&) override;
    virtual void handle(const Messages::LanguageServer::SetAutoCompleteMode&) override = 0;

    static void set_declarations_of_document_callback(ClientConnection&, const String&, Vector<GUI::AutocompleteProvider::Declaration>&&);

    FileDB m_filedb;
    OwnPtr<AutoCompleteEngine> m_autocomplete_engine;
};

}
