/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
