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

#include "LanguageClient.h"
#include <AK/String.h>
#include <AK/Vector.h>
#include <DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>
#include <LibGUI/Notification.h>

namespace HackStudio {

void ServerConnection::handle(const Messages::LanguageClient::AutoCompleteSuggestions& message)
{
    if (!m_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    m_language_client->provide_autocomplete_suggestions(message.suggestions());
}

void ServerConnection::die()
{
    dbgln("ServerConnection::die()");
    if (!m_language_client)
        return;
    m_language_client->on_server_crash();
}

void LanguageClient::open_file(const String& path, int fd)
{
    if (!m_server_connection)
        return;
    m_server_connection->post_message(Messages::LanguageServer::FileOpened(path, fd));
}

void LanguageClient::set_file_content(const String& path, const String& content)
{
    if (!m_server_connection)
        return;
    m_server_connection->post_message(Messages::LanguageServer::SetFileContent(path, content));
}

void LanguageClient::insert_text(const String& path, const String& text, size_t line, size_t column)
{
    if (!m_server_connection)
        return;
    m_server_connection->post_message(Messages::LanguageServer::FileEditInsertText(path, text, line, column));
}

void LanguageClient::remove_text(const String& path, size_t from_line, size_t from_column, size_t to_line, size_t to_column)
{
    if (!m_server_connection)
        return;
    m_server_connection->post_message(Messages::LanguageServer::FileEditRemoveText(path, from_line, from_column, to_line, to_column));
}

void LanguageClient::request_autocomplete(const String& path, size_t cursor_line, size_t cursor_column)
{
    if (!m_server_connection)
        return;
    set_active_client();
    m_server_connection->post_message(Messages::LanguageServer::AutoCompleteSuggestions(path, cursor_line, cursor_column));
}

void LanguageClient::provide_autocomplete_suggestions(const Vector<GUI::AutocompleteProvider::Entry>& suggestions)
{
    if (!m_server_connection)
        return;
    if (on_autocomplete_suggestions)
        on_autocomplete_suggestions(suggestions);

    // Otherwise, drop it on the floor :shrug:
}

void LanguageClient::set_autocomplete_mode(const String& mode)
{
    if (!m_server_connection)
        return;
    m_server_connection->post_message(Messages::LanguageServer::SetAutoCompleteMode(mode));
}

void LanguageClient::set_active_client()
{
    if (!m_server_connection)
        return;
    m_server_connection->attach(*this);
}

void LanguageClient::on_server_crash()
{
    ASSERT(m_server_connection);
    auto project_path = m_server_connection->projcet_path();
    ServerConnection::remove_instance_for_project(project_path);
    m_server_connection = nullptr;

    auto notification = GUI::Notification::construct();

    notification->set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"));
    notification->set_title("Oops!");
    notification->set_text(String::formatted("LanguageServer for {} crashed", project_path));
    notification->show();
}

HashMap<String, NonnullRefPtr<ServerConnection>> ServerConnection::s_instances_for_projects;

RefPtr<ServerConnection> ServerConnection::instance_for_project(const String& project_path)
{
    auto key = LexicalPath { project_path }.string();
    auto value = s_instances_for_projects.get(key);
    if (!value.has_value())
        return nullptr;
    return *value.value();
}

void ServerConnection::set_instance_for_project(const String& project_path, NonnullRefPtr<ServerConnection>&& instance)
{
    auto key = LexicalPath { project_path }.string();
    s_instances_for_projects.set(key, move(instance));
}

void ServerConnection::remove_instance_for_project(const String& project_path)
{
    auto key = LexicalPath { project_path }.string();
    s_instances_for_projects.remove(key);
}

}
