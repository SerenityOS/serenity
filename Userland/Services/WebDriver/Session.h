/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <LibCore/Promise.h>
#include <LibWeb/WebDriver/Capabilities.h>
#include <LibWeb/WebDriver/Error.h>
#include <LibWeb/WebDriver/Response.h>
#include <WebDriver/WebContentConnection.h>
#include <unistd.h>

namespace WebDriver {

struct LaunchBrowserCallbacks;

class Session : public RefCounted<Session> {
public:
    Session(unsigned session_id, NonnullRefPtr<Client> client, Web::WebDriver::LadybirdOptions options);
    ~Session();

    unsigned session_id() const { return m_id; }

    struct Window {
        String handle;
        NonnullRefPtr<WebContentConnection> web_content_connection;
    };

    WebContentConnection& web_content_connection() const
    {
        auto current_window = m_windows.get(m_current_window_handle);
        VERIFY(current_window.has_value());

        return current_window->web_content_connection;
    }

    String const& current_window_handle() const
    {
        return m_current_window_handle;
    }

    ErrorOr<void> start(LaunchBrowserCallbacks const&);
    Web::WebDriver::Response stop();
    Web::WebDriver::Response close_window();
    Web::WebDriver::Response switch_to_window(StringView);
    Web::WebDriver::Response get_window_handles() const;

private:
    using ServerPromise = Core::Promise<ErrorOr<void>>;
    ErrorOr<NonnullRefPtr<Core::LocalServer>> create_server(NonnullRefPtr<ServerPromise> promise);

    NonnullRefPtr<Client> m_client;
    Web::WebDriver::LadybirdOptions m_options;

    bool m_started { false };
    unsigned m_id { 0 };

    HashMap<String, Window> m_windows;
    String m_current_window_handle;

    Optional<DeprecatedString> m_web_content_socket_path;
    Optional<pid_t> m_browser_pid;

    RefPtr<Core::LocalServer> m_web_content_server;
};

}
