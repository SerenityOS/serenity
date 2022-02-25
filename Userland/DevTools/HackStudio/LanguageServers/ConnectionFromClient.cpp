/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <LibCore/File.h>
#include <LibGUI/TextDocument.h>

namespace LanguageServers {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : IPC::ConnectionFromClient<LanguageClientEndpoint, LanguageServerEndpoint>(*this, move(socket), 1)
{
    s_connections.set(1, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
    exit(0);
}

void ConnectionFromClient::greet(String const& project_root)
{
    m_filedb.set_project_root(project_root);
    if (unveil(project_root.characters(), "r") < 0) {
        perror("unveil");
        exit(1);
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        exit(1);
    }
}

void ConnectionFromClient::file_opened(String const& filename, IPC::File const& file)
{
    if (m_filedb.is_open(filename)) {
        return;
    }
    m_filedb.add(filename, file.take_fd());
    m_autocomplete_engine->file_opened(filename);
}

void ConnectionFromClient::file_edit_insert_text(String const& filename, String const& text, i32 start_line, i32 start_column)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "InsertText for file: {}", filename);
    dbgln_if(LANGUAGE_SERVER_DEBUG, "Text: {}", text);
    dbgln_if(LANGUAGE_SERVER_DEBUG, "[{}:{}]", start_line, start_column);
    m_filedb.on_file_edit_insert_text(filename, text, start_line, start_column);
    m_autocomplete_engine->on_edit(filename);
}

void ConnectionFromClient::file_edit_remove_text(String const& filename, i32 start_line, i32 start_column, i32 end_line, i32 end_column)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "RemoveText for file: {}", filename);
    dbgln_if(LANGUAGE_SERVER_DEBUG, "[{}:{} - {}:{}]", start_line, start_column, end_line, end_column);
    m_filedb.on_file_edit_remove_text(filename, start_line, start_column, end_line, end_column);
    m_autocomplete_engine->on_edit(filename);
}

void ConnectionFromClient::auto_complete_suggestions(GUI::AutocompleteProvider::ProjectLocation const& location)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "AutoCompleteSuggestions for: {} {}:{}", location.file, location.line, location.column);

    auto document = m_filedb.get(location.file);
    if (!document) {
        dbgln("file {} has not been opened", location.file);
        return;
    }

    GUI::TextPosition autocomplete_position = { (size_t)location.line, (size_t)max(location.column, location.column - 1) };
    Vector<GUI::AutocompleteProvider::Entry> suggestions = m_autocomplete_engine->get_suggestions(location.file, autocomplete_position);
    async_auto_complete_suggestions(move(suggestions));
}

void ConnectionFromClient::set_file_content(String const& filename, String const& content)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "SetFileContent: {}", filename);
    auto document = m_filedb.get(filename);
    if (!document) {
        m_filedb.add(filename, content);
        VERIFY(m_filedb.is_open(filename));
    } else {
        document->set_text(content.view());
    }
    VERIFY(m_filedb.is_open(filename));
    m_autocomplete_engine->on_edit(filename);
}

void ConnectionFromClient::find_declaration(GUI::AutocompleteProvider::ProjectLocation const& location)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "FindDeclaration: {} {}:{}", location.file, location.line, location.column);
    auto document = m_filedb.get(location.file);
    if (!document) {
        dbgln("file {} has not been opened", location.file);
        return;
    }

    GUI::TextPosition identifier_position = { (size_t)location.line, (size_t)location.column };
    auto decl_location = m_autocomplete_engine->find_declaration_of(location.file, identifier_position);
    if (!decl_location.has_value()) {
        dbgln("could not find declaration");
        return;
    }

    dbgln_if(LANGUAGE_SERVER_DEBUG, "declaration location: {} {}:{}", decl_location.value().file, decl_location.value().line, decl_location.value().column);
    async_declaration_location(GUI::AutocompleteProvider::ProjectLocation { decl_location.value().file, decl_location.value().line, decl_location.value().column });
}

void ConnectionFromClient::get_parameters_hint(GUI::AutocompleteProvider::ProjectLocation const& location)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "GetParametersHint: {} {}:{}", location.file, location.line, location.column);
    auto document = m_filedb.get(location.file);
    if (!document) {
        dbgln("file {} has not been opened", location.file);
        return;
    }

    GUI::TextPosition identifier_position = { (size_t)location.line, (size_t)location.column };
    auto params = m_autocomplete_engine->get_function_params_hint(location.file, identifier_position);
    if (!params.has_value()) {
        dbgln("could not get parameters hint");
        return;
    }

    dbgln_if(LANGUAGE_SERVER_DEBUG, "parameters hint:");
    for (auto& param : params->params) {
        dbgln_if(LANGUAGE_SERVER_DEBUG, "{}", param);
    }
    dbgln_if(LANGUAGE_SERVER_DEBUG, "Parameter index: {}", params->current_index);

    async_parameters_hint_result(params->params, params->current_index);
}

void ConnectionFromClient::get_tokens_info(String const& filename)
{
    dbgln_if(LANGUAGE_SERVER_DEBUG, "GetTokenInfo: {}", filename);
    auto document = m_filedb.get(filename);
    if (!document) {
        dbgln("file {} has not been opened", filename);
        return;
    }

    auto token_info = m_autocomplete_engine->get_tokens_info(filename);
    async_tokens_info_result(move(token_info));
}

}
