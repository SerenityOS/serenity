/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AutoCompleteResponse.h"
#include <AK/Forward.h>
#include <AK/LexicalPath.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCpp/Preprocessor.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibSyntax/Language.h>

#include <DevTools/HackStudio/LanguageServers/LanguageClientEndpoint.h>
#include <DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>

namespace HackStudio {

class LanguageClient;
class ConnectionToServerWrapper;

class ConnectionToServer
    : public IPC::ConnectionToServer<LanguageClientEndpoint, LanguageServerEndpoint>
    , public LanguageClientEndpoint {
    friend class ConnectionToServerWrapper;

public:
    ConnectionToServer(NonnullOwnPtr<Core::LocalSocket> socket, ByteString const& project_path)
        : IPC::ConnectionToServer<LanguageClientEndpoint, LanguageServerEndpoint>(*this, move(socket))
    {
        m_project_path = project_path;
        async_greet(m_project_path);
    }

    WeakPtr<LanguageClient> language_client() { return m_current_language_client; }
    ByteString const& project_path() const { return m_project_path; }

    virtual void die() override;

    LanguageClient const* active_client() const { return !m_current_language_client ? nullptr : m_current_language_client.ptr(); }

protected:
    virtual void auto_complete_suggestions(Vector<CodeComprehension::AutocompleteResultEntry> const&) override;
    virtual void declaration_location(CodeComprehension::ProjectLocation const&) override;
    virtual void declarations_in_document(ByteString const&, Vector<CodeComprehension::Declaration> const&) override;
    virtual void todo_entries_in_document(ByteString const&, Vector<CodeComprehension::TodoEntry> const&) override;
    virtual void parameters_hint_result(Vector<ByteString> const&, int index) override;
    virtual void tokens_info_result(Vector<CodeComprehension::TokenInfo> const&) override;
    void set_wrapper(ConnectionToServerWrapper& wrapper) { m_wrapper = &wrapper; }

    ByteString m_project_path;
    WeakPtr<LanguageClient> m_current_language_client;
    ConnectionToServerWrapper* m_wrapper { nullptr };
};

class ConnectionToServerWrapper {
    AK_MAKE_NONCOPYABLE(ConnectionToServerWrapper);

public:
    explicit ConnectionToServerWrapper(ByteString const& language_name, Function<NonnullRefPtr<ConnectionToServer>()> connection_creator);
    ~ConnectionToServerWrapper() = default;

    template<typename LanguageServerType>
    static ConnectionToServerWrapper& get_or_create(ByteString const& project_path);

    Syntax::Language language() const { return m_language; }
    ConnectionToServer* connection();
    void on_crash();
    void try_respawn_connection();

    void attach(LanguageClient& client);
    void detach();
    void set_active_client(LanguageClient& client);

private:
    void create_connection();
    void show_crash_notification() const;
    void show_frequent_crashes_notification() const;

    Syntax::Language m_language;
    Function<NonnullRefPtr<ConnectionToServer>()> m_connection_creator;
    RefPtr<ConnectionToServer> m_connection;

    Core::ElapsedTimer m_last_crash_timer;
    bool m_respawn_allowed { true };
};

class ConnectionToServerInstances {
public:
    static void set_instance_for_language(ByteString const& language_name, NonnullOwnPtr<ConnectionToServerWrapper>&& connection_wrapper);
    static void remove_instance_for_language(ByteString const& language_name);

    static ConnectionToServerWrapper* get_instance_wrapper(ByteString const& language_name);

private:
    static HashMap<ByteString, NonnullOwnPtr<ConnectionToServerWrapper>> s_instance_for_language;
};

class LanguageClient : public Weakable<LanguageClient> {
public:
    explicit LanguageClient(ConnectionToServerWrapper& connection_wrapper)
        : m_connection_wrapper(connection_wrapper)
    {
        if (m_connection_wrapper.connection()) {
            m_previous_client = m_connection_wrapper.connection()->language_client();
            VERIFY(m_previous_client.ptr() != this);
            m_connection_wrapper.attach(*this);
        }
    }

    virtual ~LanguageClient()
    {
        // m_connection_wrapper is nullified if the server crashes
        if (m_connection_wrapper.connection())
            m_connection_wrapper.detach();

        VERIFY(m_previous_client.ptr() != this);
        if (m_previous_client && m_connection_wrapper.connection())
            m_connection_wrapper.set_active_client(*m_previous_client);
    }

    Syntax::Language language() const { return m_connection_wrapper.language(); }
    void set_active_client();
    bool is_active_client() const;
    virtual void open_file(ByteString const& path, int fd);
    virtual void set_file_content(ByteString const& path, ByteString const& content);
    virtual void insert_text(ByteString const& path, ByteString const& text, size_t line, size_t column);
    virtual void remove_text(ByteString const& path, size_t from_line, size_t from_column, size_t to_line, size_t to_column);
    virtual void request_autocomplete(ByteString const& path, size_t cursor_line, size_t cursor_column);
    virtual void search_declaration(ByteString const& path, size_t line, size_t column);
    virtual void get_parameters_hint(ByteString const& path, size_t line, size_t column);
    virtual void get_tokens_info(ByteString const& filename);

    void provide_autocomplete_suggestions(Vector<CodeComprehension::AutocompleteResultEntry> const&) const;
    void declaration_found(ByteString const& file, size_t line, size_t column) const;
    void parameters_hint_result(Vector<ByteString> const& params, size_t argument_index) const;

    // Callbacks that get called when the result of a language server query is ready
    Function<void(Vector<CodeComprehension::AutocompleteResultEntry>)> on_autocomplete_suggestions;
    Function<void(ByteString const&, size_t, size_t)> on_declaration_found;
    Function<void(Vector<ByteString> const&, size_t)> on_function_parameters_hint_result;
    Function<void(Vector<CodeComprehension::TokenInfo> const&)> on_tokens_info_result;

private:
    ConnectionToServerWrapper& m_connection_wrapper;
    WeakPtr<LanguageClient> m_previous_client;
};

template<typename ConnectionToServerT>
static inline NonnullOwnPtr<LanguageClient> get_language_client(ByteString const& project_path)
{
    return make<LanguageClient>(ConnectionToServerWrapper::get_or_create<ConnectionToServerT>(project_path));
}

template<typename LanguageServerType>
ConnectionToServerWrapper& ConnectionToServerWrapper::get_or_create(ByteString const& project_path)
{
    auto* wrapper = ConnectionToServerInstances::get_instance_wrapper(LanguageServerType::language_name());
    if (wrapper)
        return *wrapper;

    auto connection_wrapper_ptr = make<ConnectionToServerWrapper>(LanguageServerType::language_name(), [project_path]() { return LanguageServerType::try_create(project_path).release_value_but_fixme_should_propagate_errors(); });
    auto& connection_wrapper = *connection_wrapper_ptr;
    ConnectionToServerInstances::set_instance_for_language(LanguageServerType::language_name(), move(connection_wrapper_ptr));
    return connection_wrapper;
}

}
