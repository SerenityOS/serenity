/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/JsonValue.h>
#include <AK/RefPtr.h>
#include <LibCore/Promise.h>
#include <LibWeb/WebDriver/Error.h>
#include <LibWeb/WebDriver/Response.h>
#include <WebDriver/BrowserConnection.h>
#include <WebDriver/TimeoutsConfiguration.h>
#include <WebDriver/WebContentConnection.h>
#include <unistd.h>

namespace WebDriver {

class Session {
public:
    Session(unsigned session_id, NonnullRefPtr<Client> client);
    ~Session();

    unsigned session_id() const { return m_id; }

    struct Window {
        String handle;
        bool is_open;
    };

    struct LocalElement {
        i32 id;
    };

    ErrorOr<Window*, Web::WebDriver::Error> current_window();
    ErrorOr<void, Web::WebDriver::Error> check_for_open_top_level_browsing_context_or_return_error();
    String const& current_window_handle() { return m_current_window_handle; }

    WebContentConnection& web_content_connection()
    {
        VERIFY(m_web_content_connection);
        return *m_web_content_connection;
    }

    ErrorOr<void> start();
    Web::WebDriver::Response stop();
    JsonObject get_timeouts();
    Web::WebDriver::Response set_timeouts(JsonValue const& payload);
    Web::WebDriver::Response back();
    Web::WebDriver::Response forward();
    Web::WebDriver::Response refresh();
    Web::WebDriver::Response get_title();
    Web::WebDriver::Response get_window_handle();
    ErrorOr<void, Variant<Web::WebDriver::Error, Error>> close_window();
    Web::WebDriver::Response get_window_handles() const;
    Web::WebDriver::Response take_element_screenshot(StringView element_id);

private:
    enum class ServerType {
        Browser,
        WebContent,
    };
    using ServerPromise = Core::Promise<ErrorOr<void>>;
    ErrorOr<NonnullRefPtr<Core::LocalServer>> create_server(String const& socket_path, ServerType type, NonnullRefPtr<ServerPromise> promise);

    NonnullRefPtr<Client> m_client;
    bool m_started { false };
    unsigned m_id { 0 };
    HashMap<String, NonnullOwnPtr<Window>> m_windows;
    String m_current_window_handle;
    RefPtr<BrowserConnection> m_browser_connection;
    RefPtr<WebContentConnection> m_web_content_connection;

    // https://w3c.github.io/webdriver/#dfn-session-script-timeout
    TimeoutsConfiguration m_timeouts_configuration;
};

}
