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
#include <LibWeb/WebDriver/Error.h>
#include <WebDriver/BrowserConnection.h>
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

    ErrorOr<Window*, Web::WebDriver::Error> current_window();
    ErrorOr<void, Web::WebDriver::Error> check_for_open_top_level_browsing_context_or_return_error();
    String const& current_window_handle() { return m_current_window_handle; }

    ErrorOr<void> start();
    ErrorOr<void> stop();
    JsonObject get_timeouts();
    ErrorOr<JsonValue, Web::WebDriver::Error> set_timeouts(JsonValue const& payload);
    ErrorOr<JsonValue, Web::WebDriver::Error> navigate_to(JsonValue const& url);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_current_url();
    ErrorOr<JsonValue, Web::WebDriver::Error> back();
    ErrorOr<JsonValue, Web::WebDriver::Error> forward();
    ErrorOr<JsonValue, Web::WebDriver::Error> refresh();
    ErrorOr<JsonValue, Web::WebDriver::Error> get_title();
    ErrorOr<JsonValue, Web::WebDriver::Error> get_window_handle();
    ErrorOr<void, Variant<Web::WebDriver::Error, Error>> close_window();
    ErrorOr<JsonValue, Web::WebDriver::Error> get_window_handles() const;
    ErrorOr<JsonValue, Web::WebDriver::Error> get_window_rect();
    ErrorOr<JsonValue, Web::WebDriver::Error> set_window_rect(JsonValue const& payload);
    ErrorOr<JsonValue, Web::WebDriver::Error> maximize_window();
    ErrorOr<JsonValue, Web::WebDriver::Error> minimize_window();
    ErrorOr<JsonValue, Web::WebDriver::Error> find_element(JsonValue const& payload);
    ErrorOr<JsonValue, Web::WebDriver::Error> find_elements(JsonValue const& payload);
    ErrorOr<JsonValue, Web::WebDriver::Error> find_element_from_element(JsonValue const& payload, StringView parameter_element_id);
    ErrorOr<JsonValue, Web::WebDriver::Error> find_elements_from_element(JsonValue const& payload, StringView parameter_element_id);
    ErrorOr<JsonValue, Web::WebDriver::Error> is_element_selected(StringView element_id);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_element_attribute(JsonValue const& payload, StringView element_id, StringView name);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_element_property(JsonValue const& payload, StringView element_id, StringView name);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_element_css_value(JsonValue const& payload, StringView element_id, StringView property_name);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_element_text(JsonValue const& payload, StringView element_id);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_element_tag_name(JsonValue const& payload, StringView element_id);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_element_rect(StringView element_id);
    ErrorOr<JsonValue, Web::WebDriver::Error> is_element_enabled(StringView element_id);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_source();
    ErrorOr<JsonValue, Web::WebDriver::Error> execute_script(JsonValue const& payload);
    ErrorOr<JsonValue, Web::WebDriver::Error> execute_async_script(JsonValue const& payload);
    ErrorOr<JsonValue, Web::WebDriver::Error> get_all_cookies();
    ErrorOr<JsonValue, Web::WebDriver::Error> get_named_cookie(String const& name);
    ErrorOr<JsonValue, Web::WebDriver::Error> add_cookie(JsonValue const& payload);
    ErrorOr<JsonValue, Web::WebDriver::Error> delete_cookie(StringView name);
    ErrorOr<JsonValue, Web::WebDriver::Error> delete_all_cookies();
    ErrorOr<JsonValue, Web::WebDriver::Error> take_screenshot();
    ErrorOr<JsonValue, Web::WebDriver::Error> take_element_screenshot(StringView element_id);

private:
    void delete_cookies(Optional<StringView> const& name = {});
    ErrorOr<JsonArray, Web::WebDriver::Error> find(LocalElement const& start_node, StringView location_strategy, StringView selector);

    using ElementLocationStrategyHandler = ErrorOr<Vector<LocalElement>, Web::WebDriver::Error> (Session::*)(LocalElement const&, StringView);
    struct LocatorStrategy {
        String name;
        ElementLocationStrategyHandler handler;
    };

    static Vector<LocatorStrategy> s_locator_strategies;

    ErrorOr<Vector<LocalElement>, Web::WebDriver::Error> locator_strategy_css_selectors(LocalElement const&, StringView);
    ErrorOr<Vector<LocalElement>, Web::WebDriver::Error> locator_strategy_link_text(LocalElement const&, StringView);
    ErrorOr<Vector<LocalElement>, Web::WebDriver::Error> locator_strategy_partial_link_text(LocalElement const&, StringView);
    ErrorOr<Vector<LocalElement>, Web::WebDriver::Error> locator_strategy_tag_name(LocalElement const&, StringView);
    ErrorOr<Vector<LocalElement>, Web::WebDriver::Error> locator_strategy_x_path(LocalElement const&, StringView);

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
