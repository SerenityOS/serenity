/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LanguageClient.h"
#include "HackStudio.h"
#include "ProjectDeclarations.h"
#include "ToDoEntries.h"
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/Notification.h>

namespace HackStudio {

void ServerConnection::auto_complete_suggestions(const Vector<GUI::AutocompleteProvider::Entry>& suggestions)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    m_current_language_client->provide_autocomplete_suggestions(suggestions);
}

void ServerConnection::declaration_location(const GUI::AutocompleteProvider::ProjectLocation& location)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }
    m_current_language_client->declaration_found(location.file, location.line, location.column);
}

void ServerConnection::parameters_hint_result(Vector<String> const& params, int argument_index)
{
    if (!m_current_language_client) {
        dbgln("Language Server connection has no attached language client");
        return;
    }

    VERIFY(argument_index >= 0);
    m_current_language_client->parameters_hint_result(params, static_cast<size_t>(argument_index));
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
    m_connection_wrapper.connection()->async_file_opened(path, fd);
}

void LanguageClient::set_file_content(const String& path, const String& content)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->async_set_file_content(path, content);
}

void LanguageClient::insert_text(const String& path, const String& text, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    //    set_active_client();
    m_connection_wrapper.connection()->async_file_edit_insert_text(path, text, line, column);
}

void LanguageClient::remove_text(const String& path, size_t from_line, size_t from_column, size_t to_line, size_t to_column)
{
    if (!m_connection_wrapper.connection())
        return;
    m_connection_wrapper.connection()->async_file_edit_remove_text(path, from_line, from_column, to_line, to_column);
}

void LanguageClient::request_autocomplete(const String& path, size_t cursor_line, size_t cursor_column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->async_auto_complete_suggestions(GUI::AutocompleteProvider::ProjectLocation { path, cursor_line, cursor_column });
}

void LanguageClient::provide_autocomplete_suggestions(const Vector<GUI::AutocompleteProvider::Entry>& suggestions) const
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

HashMap<String, NonnullOwnPtr<ServerConnectionWrapper>> ServerConnectionInstances::s_instance_for_language;

void ServerConnection::declarations_in_document(const String& filename, const Vector<GUI::AutocompleteProvider::Declaration>& declarations)
{
    ProjectDeclarations::the().set_declared_symbols(filename, declarations);
}

void ServerConnection::todo_entries_in_document(String const& filename, Vector<Cpp::Parser::TodoEntry> const& todo_entries)
{
    ToDoEntries::the().set_entries(filename, move(todo_entries));
}

void LanguageClient::search_declaration(const String& path, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->async_find_declaration(GUI::AutocompleteProvider::ProjectLocation { path, line, column });
}

void LanguageClient::get_parameters_hint(const String& path, size_t line, size_t column)
{
    if (!m_connection_wrapper.connection())
        return;
    set_active_client();
    m_connection_wrapper.connection()->async_get_parameters_hint(GUI::AutocompleteProvider::ProjectLocation { path, line, column });
}

void LanguageClient::declaration_found(const String& file, size_t line, size_t column) const
{
    if (!on_declaration_found) {
        dbgln("on_declaration_found callback is not set");
        return;
    }
    on_declaration_found(file, line, column);
}

void LanguageClient::parameters_hint_result(Vector<String> const& params, size_t argument_index) const
{
    if (!on_function_parameters_hint_result) {
        dbgln("on_function_parameters_hint_result callback is not set");
        return;
    }
    on_function_parameters_hint_result(params, argument_index);
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

    static constexpr int max_crash_frequency_seconds = 10;
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
    notification->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/app-hack-studio.png").release_value_but_fixme_should_propagate_errors());
    notification->set_title("LanguageServer Crashes too much!");
    notification->set_text("LanguageServer aided features will not be available in this session");
    notification->show();
}
void ServerConnectionWrapper::show_crash_notification() const
{
    auto notification = GUI::Notification::construct();
    notification->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/app-hack-studio.png").release_value_but_fixme_should_propagate_errors());
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

    // After respawning the language-server, we have to send the content of open project files
    // so the server's FileDB will be up-to-date.
    for_each_open_file([this](const ProjectFile& file) {
        if (file.code_document().language() != m_language)
            return;
        m_connection->async_set_file_content(file.code_document().file_path(), file.document().text());
    });
}

}
