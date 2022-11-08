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
#include <LibWeb/WebDriver/Response.h>
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
    Web::WebDriver::Response set_timeouts(JsonValue const& payload);
    Web::WebDriver::Response navigate_to(JsonValue const& url);
    Web::WebDriver::Response get_current_url();
    Web::WebDriver::Response back();
    Web::WebDriver::Response forward();
    Web::WebDriver::Response refresh();
    Web::WebDriver::Response get_title();
    Web::WebDriver::Response get_window_handle();
    ErrorOr<void, Variant<Web::WebDriver::Error, Error>> close_window();
    Web::WebDriver::Response get_window_handles() const;
    Web::WebDriver::Response get_window_rect();
    Web::WebDriver::Response set_window_rect(JsonValue const& payload);
    Web::WebDriver::Response maximize_window();
    Web::WebDriver::Response minimize_window();
    Web::WebDriver::Response find_element(JsonValue const& payload);
    Web::WebDriver::Response find_elements(JsonValue const& payload);
    Web::WebDriver::Response find_element_from_element(JsonValue const& payload, StringView parameter_element_id);
    Web::WebDriver::Response find_elements_from_element(JsonValue const& payload, StringView parameter_element_id);
    Web::WebDriver::Response is_element_selected(StringView element_id);
    Web::WebDriver::Response get_element_attribute(JsonValue const& payload, StringView element_id, StringView name);
    Web::WebDriver::Response get_element_property(JsonValue const& payload, StringView element_id, StringView name);
    Web::WebDriver::Response get_element_css_value(JsonValue const& payload, StringView element_id, StringView property_name);
    Web::WebDriver::Response get_element_text(JsonValue const& payload, StringView element_id);
    Web::WebDriver::Response get_element_tag_name(JsonValue const& payload, StringView element_id);
    Web::WebDriver::Response get_element_rect(StringView element_id);
    Web::WebDriver::Response is_element_enabled(StringView element_id);
    Web::WebDriver::Response get_source();
    Web::WebDriver::Response execute_script(JsonValue const& payload);
    Web::WebDriver::Response execute_async_script(JsonValue const& payload);
    Web::WebDriver::Response get_all_cookies();
    Web::WebDriver::Response get_named_cookie(String const& name);
    Web::WebDriver::Response add_cookie(JsonValue const& payload);
    Web::WebDriver::Response delete_cookie(StringView name);
    Web::WebDriver::Response delete_all_cookies();
    Web::WebDriver::Response take_screenshot();
    Web::WebDriver::Response take_element_screenshot(StringView element_id);

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
