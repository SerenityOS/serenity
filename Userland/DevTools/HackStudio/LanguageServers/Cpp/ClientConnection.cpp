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

#include "ClientConnection.h"
#include "LexerAutoComplete.h"
#include "ParserAutoComplete.h"
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <LibCore/File.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers::Cpp {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<LanguageClientEndpoint, LanguageServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
    m_autocomplete_engine = make<ParserAutoComplete>(m_filedb);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
    exit(0);
}

OwnPtr<Messages::LanguageServer::GreetResponse> ClientConnection::handle(const Messages::LanguageServer::Greet& message)
{
    m_filedb.set_project_root(message.project_root());
    if (unveil(message.project_root().characters(), "r") < 0) {
        perror("unveil");
        exit(1);
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        exit(1);
    }
    return make<Messages::LanguageServer::GreetResponse>();
}

void ClientConnection::handle(const Messages::LanguageServer::FileOpened& message)
{
    if (m_filedb.is_open(message.file_name())) {
        return;
    }
    m_filedb.add(message.file_name(), message.file().take_fd());
    m_autocomplete_engine->file_opened(message.file_name());
}

void ClientConnection::handle(const Messages::LanguageServer::FileEditInsertText& message)
{
#if CPP_LANGUAGE_SERVER_DEBUG
    dbgln("InsertText for file: {}", message.file_name());
    dbgln("Text: {}", message.text());
    dbgln("[{}:{}]", message.start_line(), message.start_column());
#endif
    m_filedb.on_file_edit_insert_text(message.file_name(), message.text(), message.start_line(), message.start_column());
    m_autocomplete_engine->on_edit(message.file_name());
}

void ClientConnection::handle(const Messages::LanguageServer::FileEditRemoveText& message)
{
#if CPP_LANGUAGE_SERVER_DEBUG
    dbgln("RemoveText for file: {}", message.file_name());
    dbgln("[{}:{} - {}:{}]", message.start_line(), message.start_column(), message.end_line(), message.end_column());
#endif
    m_filedb.on_file_edit_remove_text(message.file_name(), message.start_line(), message.start_column(), message.end_line(), message.end_column());
    m_autocomplete_engine->on_edit(message.file_name());
}

void ClientConnection::handle(const Messages::LanguageServer::AutoCompleteSuggestions& message)
{
#if CPP_LANGUAGE_SERVER_DEBUG
    dbgln("AutoCompleteSuggestions for: {} {}:{}", message.location().file, message.location().line, message.location().column);
#endif

    auto document = m_filedb.get(message.location().file);
    if (!document) {
        dbgln("file {} has not been opened", message.location().file);
        return;
    }

    GUI::TextPosition autocomplete_position = { (size_t)message.location().line, (size_t)max(message.location().column, message.location().column - 1) };
    Vector<GUI::AutocompleteProvider::Entry> suggestions = m_autocomplete_engine->get_suggestions(message.location().file, autocomplete_position);
    post_message(Messages::LanguageClient::AutoCompleteSuggestions(move(suggestions)));
}

void ClientConnection::handle(const Messages::LanguageServer::SetFileContent& message)
{
    auto document = m_filedb.get(message.file_name());
    if (!document) {
        dbgln("file {} has not been opened", message.file_name());
        return;
    }
    auto content = message.content();
    document->set_text(content.view());
    m_autocomplete_engine->on_edit(message.file_name());
}

void ClientConnection::handle(const Messages::LanguageServer::SetAutoCompleteMode& message)
{
#ifdef CPP_LANGUAGE_SERVER_DEBUG
    dbgln("SetAutoCompleteMode: {}", message.mode());
#endif
    if (message.mode() == "Parser")
        m_autocomplete_engine = make<ParserAutoComplete>(m_filedb);
    else
        m_autocomplete_engine = make<LexerAutoComplete>(m_filedb);
}

void ClientConnection::handle(const Messages::LanguageServer::FindDeclaration& message)
{
    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "FindDeclaration: {} {}:{}", message.location().file, message.location().line, message.location().column);
    auto document = m_filedb.get(message.location().file);
    if (!document) {
        dbgln("file {} has not been opened", message.location().file);
        return;
    }

    GUI::TextPosition identifier_position = { (size_t)message.location().line, (size_t)message.location().column };
    auto location = m_autocomplete_engine->find_declaration_of(message.location().file, identifier_position);
    if (!location.has_value()) {
        dbgln("could not find declaration");
        return;
    }
    dbgln_if(CPP_LANGUAGE_SERVER_DEBUG, "declaration location: {} {}:{}", location.value().file, location.value().line, location.value().column);
    post_message(Messages::LanguageClient::DeclarationLocation(GUI::AutocompleteProvider::ProjectLocation { location.value().file, location.value().line, location.value().column }));
}

}
