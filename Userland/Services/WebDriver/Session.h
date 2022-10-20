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
#include <WebDriver/TimeoutsConfiguration.h>
#include <WebDriver/WebDriverError.h>
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

    ErrorOr<Window*, WebDriverError> current_window();
    ErrorOr<void, WebDriverError> check_for_open_top_level_browsing_context_or_return_error();
    String const& current_window_handle() { return m_current_window_handle; }

    ErrorOr<void> start();
    ErrorOr<void> stop();
    JsonObject get_timeouts();
    ErrorOr<JsonValue, WebDriverError> set_timeouts(JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> navigate_to(JsonValue const& url);
    ErrorOr<JsonValue, WebDriverError> get_current_url();
    ErrorOr<JsonValue, WebDriverError> back();
    ErrorOr<JsonValue, WebDriverError> forward();
    ErrorOr<JsonValue, WebDriverError> refresh();
    ErrorOr<JsonValue, WebDriverError> get_title();
    ErrorOr<JsonValue, WebDriverError> get_window_handle();
    ErrorOr<void, Variant<WebDriverError, Error>> close_window();
    ErrorOr<JsonValue, WebDriverError> get_window_handles() const;
    ErrorOr<JsonValue, WebDriverError> find_element(JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> find_elements(JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> find_element_from_element(JsonValue const& payload, StringView parameter_element_id);
    ErrorOr<JsonValue, WebDriverError> find_elements_from_element(JsonValue const& payload, StringView parameter_element_id);
    ErrorOr<JsonValue, WebDriverError> get_element_attribute(JsonValue const& payload, StringView element_id, StringView name);
    ErrorOr<JsonValue, WebDriverError> get_element_property(JsonValue const& payload, StringView element_id, StringView name);
    ErrorOr<JsonValue, WebDriverError> get_element_css_value(JsonValue const& payload, StringView element_id, StringView property_name);
    ErrorOr<JsonValue, WebDriverError> get_all_cookies();
    ErrorOr<JsonValue, WebDriverError> get_named_cookie(String const& name);
    ErrorOr<JsonValue, WebDriverError> add_cookie(JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> delete_cookie(StringView const& name);
    ErrorOr<JsonValue, WebDriverError> delete_all_cookies();

private:
    void delete_cookies(Optional<StringView> const& name = {});
    ErrorOr<JsonArray, WebDriverError> find(LocalElement const& start_node, StringView const& location_strategy, StringView const& selector);

    using ElementLocationStrategyHandler = ErrorOr<Vector<LocalElement>, WebDriverError> (Session::*)(LocalElement const&, StringView const&);
    struct LocatorStrategy {
        String name;
        ElementLocationStrategyHandler handler;
    };

    static Vector<LocatorStrategy> s_locator_strategies;

    ErrorOr<Vector<LocalElement>, WebDriverError> locator_strategy_css_selectors(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, WebDriverError> locator_strategy_link_text(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, WebDriverError> locator_strategy_partial_link_text(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, WebDriverError> locator_strategy_tag_name(LocalElement const&, StringView const&);
    ErrorOr<Vector<LocalElement>, WebDriverError> locator_strategy_x_path(LocalElement const&, StringView const&);

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
