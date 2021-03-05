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
#include "HackStudio.h"
#include "Locator.h"
#include <AK/String.h>
#include <AK/Vector.h>
#include <DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>
#include <LibGUI/Notification.h>

namespace HackStudio {

void ServerConnection::handle(const Messages::LanguageClient::AutoCompleteSuggestions& message)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    m_current_language_client->provide_autocomplete_suggestions(message.suggestions());
}

void ServerConnection::handle(const Messages::LanguageClient::DeclarationLocation& message)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    m_current_language_client->declaration_found(message.location().file, message.location().line, message.location().column);
}

void ServerConnection::die()
{
    VERIFY(m_wrapper);
    // Wrapper destructs us here
    m_wrapper->on_crash();
}

void LanguageClient::open_file(const String& path, int fd)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->post_message(Messages::LanguageServer::FileOpened(path, fd));
}

void LanguageClient::set_file_content(const String& path, const String& content)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->post_message(Messages::LanguageServer::SetFileContent(path, content));
}

void LanguageClient::insert_text(const String& path, const String& text, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    //    set_active_client();
    m_connection_wrapper.connection()->post_message(Messages::LanguageServer::FileEditInsertText(path, text, line, column));
}

void LanguageClient::remove_text(const String& path, size_t from_line, size_t from_column, size_t to_line, size_t to_column)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->post_message(Messages::LanguageServer::FileEditRemoveText(path, from_line, from_column, to_line, to_column));
}

void LanguageClient::request_autocomplete(const String& path, size_t cursor_line, size_t cursor_column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->post_message(Messages::LanguageServer::AutoCompleteSuggestions(GUI::AutocompleteProvider::ProjectLocation { path, cursor_line, cursor_column }));
}

void LanguageClient::provide_autocomplete_suggestions(const Vector<GUI::AutocompleteProvider::Entry>& suggestions) const
{
    if (on_autocomplete_suggestions)
        on_autocomplete_suggestions(suggestions);

    // Otherwise, drop it on the floor :shrug:
}

void LanguageClient::set_autocomplete_mode(const String& mode)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->post_message(Messages::LanguageServer::SetAutoCompleteMode(mode));
}

void LanguageClient::set_active_client()
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.set_active_client(*this);
}

HashMap<String, NonnullOwnPtr<ServerConnectionWrapper>> ServerConnectionInstances::s_instance_for_language;

void ServerConnection::handle(const Messages::LanguageClient::DeclarationsInDocument& message)
{
    locator().set_declared_symbols(message.filename(), message.declarations());
}

void LanguageClient::search_declaration(const String& path, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->post_message(Messages::LanguageServer::FindDeclaration(GUI::AutocompleteProvider::ProjectLocation { path, line, column }));
}

void LanguageClient::declaration_found(const String& file, size_t line, size_t column) const
{
    if (!on_declaration_found) {
        dbgln("on_declaration_found callback is not set");
        return;
    }
    on_declaration_found(file, line, column);
}

void ServerConnectionInstances::set_instance_for_language(const String& language_name, NonnullOwnPtr<ServerConnectionWrapper>&& connection_wrapper)
{
    s_instance_for_language.set(language_name, move(connection_wrapper));
}

void ServerConnectionInstances::remove_instance_for_language(const String& language_name)
{
    s_instance_for_language.remove(language_name);
}

ServerConnectionWrapper* ServerConnectionInstances::get_instance_wrapper(const String& language_name)
{
    if (auto instance = s_instance_for_language.get(language_name); instance.has_value()) {
        return const_cast<ServerConnectionWrapper*>(instance.value());
    }
    return nullptr;
}

void ServerConnectionWrapper::on_crash()
{
    show_crash_notification();
    m_connection.clear();

    static constexpr int max_crash_frequency_seconds = 3;
    if (m_last_crash_timer.is_valid() && m_last_crash_timer.elapsed() / 1000 < max_crash_frequency_seconds) {
        dbgln("LanguageServer crash frequency is too high");
        m_respawn_allowed = false;

        show_frequenct_crashes_notification();
    } else {
        m_last_crash_timer.start();
        try_respawn_connection();
    }
}
void ServerConnectionWrapper::show_frequenct_crashes_notification() const
{
    auto notification = GUI::Notification::construct();
    notification->set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"));
    notification->set_title("LanguageServer Crashes too much!");
    notification->set_text("LanguageServer aided features will not be available in this session");
    notification->show();
}
void ServerConnectionWrapper::show_crash_notification() const
{
    auto notification = GUI::Notification::construct();
    notification->set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"));
    notification->set_title("Oops!");
    notification->set_text(String::formatted("LanguageServer has crashed"));
    notification->show();
}

ServerConnectionWrapper::ServerConnectionWrapper(const String& language_name, Function<NonnullRefPtr<ServerConnection>()> connection_creator)
    : m_language(language_from_name(language_name))
    , m_connection_creator(move(connection_creator))
{
    create_connection();
}

void ServerConnectionWrapper::create_connection()
{
    VERIFY(m_connection.is_null());
    m_connection = m_connection_creator();
    m_connection->set_wrapper(*this);
    m_connection->handshake();
}

ServerConnection* ServerConnectionWrapper::connection()
{
    return m_connection.ptr();
}

void ServerConnectionWrapper::attach(LanguageClient& client)
{
    m_connection->m_current_language_client = &client;
}

void ServerConnectionWrapper::detach()
{
    m_connection->m_current_language_client.clear();
}

void ServerConnectionWrapper::set_active_client(LanguageClient& client)
{
    m_connection->m_current_language_client = &client;
}

void ServerConnectionWrapper::try_respawn_connection()
{
    if (!m_respawn_allowed)
        return;

    dbgln("Respawning ServerConnection");
    create_connection();

    // After respawning the language-server, we have to flush the content of the project files
    // so the server's FileDB will be up-to-date.
    project().for_each_text_file([this](const ProjectFile& file) {
        if (file.code_document().language() != m_language)
            return;
        m_connection->post_message(Messages::LanguageServer::SetFileContent(file.code_document().file_path(), file.document().text()));
    });
}

}
