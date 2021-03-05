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

#pragma once

#include "AutoCompleteResponse.h"
#include "Language.h"
#include <AK/Forward.h>
#include <AK/LexicalPath.h>
#include <AK/Types.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>
#include <LibCore/ElapsedTimer.h>
#include <LibIPC/ServerConnection.h>

#include <DevTools/HackStudio/LanguageServers/LanguageClientEndpoint.h>
#include <DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>

namespace HackStudio {

class LanguageClient;
class ServerConnectionWrapper;

class ServerConnection
    : public IPC::ServerConnection<LanguageClientEndpoint, LanguageServerEndpoint>
    , public LanguageClientEndpoint {
    friend class ServerConnectionWrapper;

public:
    ServerConnection(const StringView& socket, const String& project_path)
        : IPC::ServerConnection<LanguageClientEndpoint, LanguageServerEndpoint>(*this, socket)
    {
        m_project_path = project_path;
    }

    virtual void handshake() override
    {
        send_sync<Messages::LanguageServer::Greet>(m_project_path);
    }

    WeakPtr<LanguageClient> language_client() { return m_current_language_client; }
    const String& project_path() const { return m_project_path; }

    virtual void die() override;

protected:
    virtual void handle(const Messages::LanguageClient::AutoCompleteSuggestions&) override;
    virtual void handle(const Messages::LanguageClient::DeclarationLocation&) override;
    virtual void handle(const Messages::LanguageClient::DeclarationsInDocument&) override;
    void set_wrapper(ServerConnectionWrapper& wrapper) { m_wrapper = &wrapper; }

    String m_project_path;
    WeakPtr<LanguageClient> m_current_language_client;
    ServerConnectionWrapper* m_wrapper { nullptr };
};

class ServerConnectionWrapper {
    AK_MAKE_NONCOPYABLE(ServerConnectionWrapper);

public:
    explicit ServerConnectionWrapper(const String& language_name, Function<NonnullRefPtr<ServerConnection>()> connection_creator);
    ~ServerConnectionWrapper() = default;

    template<typename LanguageServerType>
    static ServerConnectionWrapper& get_or_create(const String& project_path);

    ServerConnection* connection();
    void on_crash();
    void try_respawn_connection();

    void attach(LanguageClient& client);
    void detach();
    void set_active_client(LanguageClient& client);

private:
    void create_connection();
    void show_crash_notification() const;
    void show_frequenct_crashes_notification() const;

    Language m_language;
    Function<NonnullRefPtr<ServerConnection>()> m_connection_creator;
    RefPtr<ServerConnection> m_connection;

    Core::ElapsedTimer m_last_crash_timer;
    bool m_respawn_allowed { true };
};

class ServerConnectionInstances {
public:
    static void set_instance_for_language(const String& language_name, NonnullOwnPtr<ServerConnectionWrapper>&& connection_wrapper);
    static void remove_instance_for_language(const String& language_name);

    static ServerConnectionWrapper* get_instance_wrapper(const String& language_name);

private:
    static HashMap<String, NonnullOwnPtr<ServerConnectionWrapper>> s_instance_for_language;
};

class LanguageClient : public Weakable<LanguageClient> {
public:
    explicit LanguageClient(ServerConnectionWrapper& connection_wrapper)
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

    void set_active_client();
    virtual void open_file(const String& path, int fd);
    virtual void set_file_content(const String& path, const String& content);
    virtual void insert_text(const String& path, const String& text, size_t line, size_t column);
    virtual void remove_text(const String& path, size_t from_line, size_t from_column, size_t to_line, size_t to_column);
    virtual void request_autocomplete(const String& path, size_t cursor_line, size_t cursor_column);
    virtual void set_autocomplete_mode(const String& mode);
    virtual void search_declaration(const String& path, size_t line, size_t column);

    void provide_autocomplete_suggestions(const Vector<GUI::AutocompleteProvider::Entry>&) const;
    void declaration_found(const String& file, size_t line, size_t column) const;

    Function<void(Vector<GUI::AutocompleteProvider::Entry>)> on_autocomplete_suggestions;
    Function<void(const String&, size_t, size_t)> on_declaration_found;

private:
    ServerConnectionWrapper& m_connection_wrapper;
    WeakPtr<LanguageClient> m_previous_client;
};

template<typename ServerConnectionT>
static inline NonnullOwnPtr<LanguageClient> get_language_client(const String& project_path)
{
    return make<LanguageClient>(ServerConnectionWrapper::get_or_create<ServerConnectionT>(project_path));
}

template<typename LanguageServerType>
ServerConnectionWrapper& ServerConnectionWrapper::get_or_create(const String& project_path)
{
    auto* wrapper = ServerConnectionInstances::get_instance_wrapper(LanguageServerType::language_name());
    if (wrapper)
        return *wrapper;

    auto connection_wrapper_ptr = make<ServerConnectionWrapper>(LanguageServerType::language_name(), [project_path]() { return LanguageServerType::construct(project_path); });
    auto& connection_wrapper = *connection_wrapper_ptr;
    ServerConnectionInstances::set_instance_for_language(LanguageServerType::language_name(), move(connection_wrapper_ptr));
    return connection_wrapper;
}

}
