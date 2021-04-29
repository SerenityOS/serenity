/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <LibCore/File.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<LanguageClientEndpoint, LanguageServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
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
    if (m_filedb.is_open(message.filename())) {
        return;
    }
    m_filedb.add(message.filename(), message.file().take_fd());
    m_autocomplete_engine->file_opened(message.filename());
}

void ClientConnection::handle(const Messages::LanguageServer::FileEditInsertText& message)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "InsertText for file: {}", message.filename());
    dbgln_if(LANGUAGE_SERVER_DEBUG, "Text: {}", message.text());
    dbgln_if(LANGUAGE_SERVER_DEBUG, "[{}:{}]", message.start_line(), message.start_column());
    m_filedb.on_file_edit_insert_text(message.filename(), message.text(), message.start_line(), message.start_column());
    m_autocomplete_engine->on_edit(message.filename());
}

void ClientConnection::handle(const Messages::LanguageServer::FileEditRemoveText& message)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "RemoveText for file: {}", message.filename());
    dbgln_if(LANGUAGE_SERVER_DEBUG, "[{}:{} - {}:{}]", message.start_line(), message.start_column(), message.end_line(), message.end_column());
    m_filedb.on_file_edit_remove_text(message.filename(), message.start_line(), message.start_column(), message.end_line(), message.end_column());
    m_autocomplete_engine->on_edit(message.filename());
}

void ClientConnection::handle(const Messages::LanguageServer::AutoCompleteSuggestions& message)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "AutoCompleteSuggestions for: {} {}:{}", message.location().file, message.location().line, message.location().column);

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
    dbgln_if(LANGUAGE_SERVER_DEBUG, "SetFileContent: {}", message.filename());
    auto document = m_filedb.get(message.filename());
    if (!document) {
        m_filedb.add(message.filename(), message.content());
        VERIFY(m_filedb.is_open(message.filename()));
    } else {
        const auto& content = message.content();
        document->set_text(content.view());
    }
    VERIFY(m_filedb.is_open(message.filename()));
    m_autocomplete_engine->on_edit(message.filename());
}

void ClientConnection::handle(const Messages::LanguageServer::FindDeclaration& message)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "FindDeclaration: {} {}:{}", message.location().file, message.location().line, message.location().column);
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

    dbgln_if(LANGUAGE_SERVER_DEBUG, "declaration location: {} {}:{}", location.value().file, location.value().line, location.value().column);
    post_message(Messages::LanguageClient::DeclarationLocation(GUI::AutocompleteProvider::ProjectLocation { location.value().file, location.value().line, location.value().column }));
}

void ClientConnection::set_declarations_of_document_callback(ClientConnection& instance, const String& filename, Vector<GUI::AutocompleteProvider::Declaration>&& declarations)
{
    instance.post_message(Messages::LanguageClient::DeclarationsInDocument(filename, move(declarations)));
}

}
