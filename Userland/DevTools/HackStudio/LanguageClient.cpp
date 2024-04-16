/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LanguageClient.h"
#include "HackStudio.h"
#include "ProjectDeclarations.h"
#include "ToDoEntries.h"
#include <AK/ByteString.h>
#include <AK/Vector.h>
#include <LibGUI/Notification.h>

namespace HackStudio {

void ConnectionToServer::auto_complete_suggestions(Vector<CodeComprehension::AutocompleteResultEntry> const& suggestions)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    m_current_language_client->provide_autocomplete_suggestions(suggestions);
}

void ConnectionToServer::declaration_location(CodeComprehension::ProjectLocation const& location)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    m_current_language_client->declaration_found(location.file, location.line, location.column);
}

void ConnectionToServer::parameters_hint_result(Vector<ByteString> const& params, int argument_index)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }

    VERIFY(argument_index >= 0);
    m_current_language_client->parameters_hint_result(params, static_cast<size_t>(argument_index));
}

void ConnectionToServer::tokens_info_result(Vector<CodeComprehension::TokenInfo> const& tokens_info)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    VERIFY(m_current_language_client->on_tokens_info_result);
    m_current_language_client->on_tokens_info_result(tokens_info);
}

void ConnectionToServer::die()
{
    VERIFY(m_wrapper);
    // Wrapper destructs us here
    m_wrapper->on_crash();
}

void LanguageClient::open_file(ByteString const& path, int fd)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->async_file_opened(path, MUST(IPC::File::clone_fd(fd)));
}

void LanguageClient::set_file_content(ByteString const& path, ByteString const& content)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->async_set_file_content(path, content);
}

void LanguageClient::insert_text(ByteString const& path, ByteString const& text, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->async_file_edit_insert_text(path, text, line, column);
}

void LanguageClient::remove_text(ByteString const& path, size_t from_line, size_t from_column, size_t to_line, size_t to_column)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->async_file_edit_remove_text(path, from_line, from_column, to_line, to_column);
}

void LanguageClient::request_autocomplete(ByteString const& path, size_t cursor_line, size_t cursor_column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->async_auto_complete_suggestions(CodeComprehension::ProjectLocation { path, cursor_line, cursor_column });
}

void LanguageClient::provide_autocomplete_suggestions(Vector<CodeComprehension::AutocompleteResultEntry> const& suggestions) const
{
    if (on_autocomplete_suggestions)
        on_autocomplete_suggestions(suggestions);

    // Otherwise, drop it on the floor :shrug:
}

void LanguageClient::set_active_client()
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.set_active_client(*this);
}

bool LanguageClient::is_active_client() const
{
    if (!m_connection_wrapper.connection())
        return false;
    return m_connection_wrapper.connection()->active_client() == this;
}

HashMap<ByteString, NonnullOwnPtr<ConnectionToServerWrapper>> ConnectionToServerInstances::s_instance_for_language;

void ConnectionToServer::declarations_in_document(ByteString const& filename, Vector<CodeComprehension::Declaration> const& declarations)
{
    ProjectDeclarations::the().set_declared_symbols(filename, declarations);
}

void ConnectionToServer::todo_entries_in_document(ByteString const& filename, Vector<CodeComprehension::TodoEntry> const& todo_entries)
{
    ToDoEntries::the().set_entries(filename, move(todo_entries));
}

void LanguageClient::search_declaration(ByteString const& path, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->async_find_declaration(CodeComprehension::ProjectLocation { path, line, column });
}

void LanguageClient::get_parameters_hint(ByteString const& path, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->async_get_parameters_hint(CodeComprehension::ProjectLocation { path, line, column });
}

void LanguageClient::get_tokens_info(ByteString const& filename)
{
    if (!m_connection_wrapper.connection())
        return;
    VERIFY(is_active_client());
    m_connection_wrapper.connection()->async_get_tokens_info(filename);
}

void LanguageClient::declaration_found(ByteString const& file, size_t line, size_t column) const
{
    if (!on_declaration_found) {
        dbgln("on_declaration_found callback is not set");
        return;
    }
    on_declaration_found(file, line, column);
}

void LanguageClient::parameters_hint_result(Vector<ByteString> const& params, size_t argument_index) const
{
    if (!on_function_parameters_hint_result) {
        dbgln("on_function_parameters_hint_result callback is not set");
        return;
    }
    on_function_parameters_hint_result(params, argument_index);
}

void ConnectionToServerInstances::set_instance_for_language(ByteString const& language_name, NonnullOwnPtr<ConnectionToServerWrapper>&& connection_wrapper)
{
    s_instance_for_language.set(language_name, move(connection_wrapper));
}

void ConnectionToServerInstances::remove_instance_for_language(ByteString const& language_name)
{
    s_instance_for_language.remove(language_name);
}

ConnectionToServerWrapper* ConnectionToServerInstances::get_instance_wrapper(ByteString const& language_name)
{
    if (auto instance = s_instance_for_language.get(language_name); instance.has_value()) {
        return const_cast<ConnectionToServerWrapper*>(instance.value());
    }
    return nullptr;
}

void ConnectionToServerWrapper::on_crash()
{
    using namespace AK::TimeLiterals;

    show_crash_notification();
    m_connection.clear();

    static constexpr Duration max_crash_frequency = 10_sec;
    if (m_last_crash_timer.is_valid() && m_last_crash_timer.elapsed_time() < max_crash_frequency) {
        dbgln("LanguageServer crash frequency is too high");
        m_respawn_allowed = false;

        show_frequent_crashes_notification();
    } else {
        m_last_crash_timer.start();
        try_respawn_connection();
    }
}
void ConnectionToServerWrapper::show_frequent_crashes_notification() const
{
    auto notification = GUI::Notification::construct();
    notification->set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"sv).release_value_but_fixme_should_propagate_errors());
    notification->set_title("LanguageServer Crashes too much!"_string);
    notification->set_text("LanguageServer aided features will not be available in this session"_string);
    notification->show();
}
void ConnectionToServerWrapper::show_crash_notification() const
{
    auto notification = GUI::Notification::construct();
    notification->set_icon(Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"sv).release_value_but_fixme_should_propagate_errors());
    notification->set_title("Oops!"_string);
    notification->set_text("LanguageServer has crashed"_string);
    notification->show();
}

ConnectionToServerWrapper::ConnectionToServerWrapper(ByteString const& language_name, Function<NonnullRefPtr<ConnectionToServer>()> connection_creator)
    : m_language(Syntax::language_from_name(language_name).value())
    , m_connection_creator(move(connection_creator))
{
    create_connection();
}

void ConnectionToServerWrapper::create_connection()
{
    VERIFY(m_connection.is_null());
    m_connection = m_connection_creator();
    m_connection->set_wrapper(*this);
}

ConnectionToServer* ConnectionToServerWrapper::connection()
{
    return m_connection.ptr();
}

void ConnectionToServerWrapper::attach(LanguageClient& client)
{
    m_connection->m_current_language_client = &client;
}

void ConnectionToServerWrapper::detach()
{
    m_connection->m_current_language_client.clear();
}

void ConnectionToServerWrapper::set_active_client(LanguageClient& client)
{
    m_connection->m_current_language_client = &client;
}

void ConnectionToServerWrapper::try_respawn_connection()
{
    if (!m_respawn_allowed)
        return;

    dbgln("Respawning ConnectionToServer");
    create_connection();

    // After respawning the language-server, we have to send the content of open project files
    // so the server's FileDB will be up-to-date.
    for_each_open_file([this](ProjectFile const& file) {
        if (file.code_document().language() != m_language)
            return;
        m_connection->async_set_file_content(file.code_document().file_path(), file.document().text());
    });
}

}
