/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/JsonValue.h>
#include <AK/RefPtr.h>
#include <WebDriver/BrowserConnection.h>
#include <WebDriver/HttpError.h>
#include <WebDriver/TimeoutsConfiguration.h>
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

    Optional<Window*> current_window() { return m_windows.get(m_current_window_handle); }
    String const& current_window_handle() { return m_current_window_handle; }

    ErrorOr<void> start();
    ErrorOr<void> stop();
    JsonObject get_timeouts();
    ErrorOr<JsonValue, HttpError> set_timeouts(JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> navigate_to(JsonValue const& url);
    ErrorOr<JsonValue, HttpError> get_current_url();
    ErrorOr<JsonValue, HttpError> back();
    ErrorOr<JsonValue, HttpError> forward();
    ErrorOr<JsonValue, HttpError> refresh();
    ErrorOr<JsonValue, HttpError> get_title();
    ErrorOr<JsonValue, HttpError> get_window_handle();
    ErrorOr<void, Variant<HttpError, Error>> close_window();
    ErrorOr<JsonValue, HttpError> get_window_handles() const;
    ErrorOr<JsonValue, HttpError> find_element(JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> find_elements(JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> find_element_from_element(JsonValue const& payload, StringView parameter_element_id);
    ErrorOr<JsonValue, HttpError> find_elements_from_element(JsonValue const& payload, StringView parameter_element_id);
    ErrorOr<JsonValue, HttpError> get_element_attribute(JsonValue const& payload, StringView element_id, StringView name);
    ErrorOr<JsonValue, HttpError> get_element_property(JsonValue const& payload, StringView element_id, StringView name);
    ErrorOr<JsonValue, HttpError> get_all_cookies();
    ErrorOr<JsonValue, HttpError> get_named_cookie(String const& name);
    ErrorOr<JsonValue, HttpError> add_cookie(JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> delete_cookie(StringView const& name);
    ErrorOr<JsonValue, HttpError> delete_all_cookies();

private:
    void delete_cookies(Optional<StringView> const& name = {});
    ErrorOr<JsonArray, HttpError> find(LocalElement const& start_node, StringView const& location_strategy, StringView const& selector);

    using ElementLocationStrategyHandler = ErrorOr<Vector<LocalElement>, HttpError> (Session::*)(LocalElement const&, StringView const&);
    struct LocatorStrategy {
        String name;
        ElementLocationStrategyHandler handler;
    };

    static Vector<LocatorStrategy> s_locator_strategies;

    ErrorOr<Vector<LocalElement>, HttpError> locator_strategy_css_selectors(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, HttpError> locator_strategy_link_text(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, HttpError> locator_strategy_partial_link_text(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, HttpError> locator_strategy_tag_name(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, HttpError> locator_strategy_x_path(LocalElement const&, StringView const&);

    NonnullRefPtr<Client> m_client;
    bool m_started { false };
    unsigned m_id { 0 };
    HashMap<String, NonnullOwnPtr<Window>> m_windows;
    String m_current_window_handle;
    RefPtr<Core::LocalServer> m_local_server;
    RefPtr<BrowserConnection> m_browser_connection;

    // https://w3c.github.io/webdriver/#dfn-session-script-timeout
    TimeoutsConfiguration m_timeouts_configuration;
};

}
