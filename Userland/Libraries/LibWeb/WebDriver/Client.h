/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>
#include <LibHTTP/Forward.h>
#include <LibHTTP/HttpRequest.h>
#include <LibWeb/WebDriver/Error.h>
#include <LibWeb/WebDriver/Response.h>

namespace Web::WebDriver {

using Parameters = Span<StringView const>;

class Client : public Core::Object {
    C_OBJECT_ABSTRACT(Client);

public:
    virtual ~Client();

    // 8. Sessions, https://w3c.github.io/webdriver/#sessions
    virtual Response new_session(Parameters parameters, JsonValue payload) = 0;
    virtual Response delete_session(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_status(Parameters parameters, JsonValue payload) = 0;

    // 9. Timeouts, https://w3c.github.io/webdriver/#timeouts
    virtual Response get_timeouts(Parameters parameters, JsonValue payload) = 0;
    virtual Response set_timeouts(Parameters parameters, JsonValue payload) = 0;

    // 10. Navigation, https://w3c.github.io/webdriver/#navigation
    virtual Response navigate_to(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_current_url(Parameters parameters, JsonValue payload) = 0;
    virtual Response back(Parameters parameters, JsonValue payload) = 0;
    virtual Response forward(Parameters parameters, JsonValue payload) = 0;
    virtual Response refresh(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_title(Parameters parameters, JsonValue payload) = 0;

    // 11. Contexts, https://w3c.github.io/webdriver/#contexts
    virtual Response get_window_handle(Parameters parameters, JsonValue payload) = 0;
    virtual Response close_window(Parameters parameters, JsonValue payload) = 0;
    virtual Response switch_to_window(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_window_handles(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_window_rect(Parameters parameters, JsonValue payload) = 0;
    virtual Response set_window_rect(Parameters parameters, JsonValue payload) = 0;
    virtual Response maximize_window(Parameters parameters, JsonValue payload) = 0;
    virtual Response minimize_window(Parameters parameters, JsonValue payload) = 0;
    virtual Response fullscreen_window(Parameters parameters, JsonValue payload) = 0;

    // 12. Elements, https://w3c.github.io/webdriver/#elements
    virtual Response find_element(Parameters parameters, JsonValue payload) = 0;
    virtual Response find_elements(Parameters parameters, JsonValue payload) = 0;
    virtual Response find_element_from_element(Parameters parameters, JsonValue payload) = 0;
    virtual Response find_elements_from_element(Parameters parameters, JsonValue payload) = 0;
    virtual Response find_element_from_shadow_root(Parameters parameters, JsonValue payload) = 0;
    virtual Response find_elements_from_shadow_root(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_active_element(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_element_shadow_root(Parameters parameters, JsonValue payload) = 0;
    virtual Response is_element_selected(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_element_attribute(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_element_property(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_element_css_value(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_element_text(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_element_tag_name(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_element_rect(Parameters parameters, JsonValue payload) = 0;
    virtual Response is_element_enabled(Parameters parameters, JsonValue payload) = 0;

    // 13. Document, https://w3c.github.io/webdriver/#document
    virtual Response get_source(Parameters parameters, JsonValue payload) = 0;
    virtual Response execute_script(Parameters parameters, JsonValue payload) = 0;
    virtual Response execute_async_script(Parameters parameters, JsonValue payload) = 0;

    // 14. Cookies, https://w3c.github.io/webdriver/#cookies
    virtual Response get_all_cookies(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_named_cookie(Parameters parameters, JsonValue payload) = 0;
    virtual Response add_cookie(Parameters parameters, JsonValue payload) = 0;
    virtual Response delete_cookie(Parameters parameters, JsonValue payload) = 0;
    virtual Response delete_all_cookies(Parameters parameters, JsonValue payload) = 0;

    // 16. User prompts, https://w3c.github.io/webdriver/#user-prompts
    virtual Response dismiss_alert(Parameters parameters, JsonValue payload) = 0;
    virtual Response accept_alert(Parameters parameters, JsonValue payload) = 0;
    virtual Response get_alert_text(Parameters parameters, JsonValue payload) = 0;
    virtual Response send_alert_text(Parameters parameters, JsonValue payload) = 0;

    // 17. Screen capture, https://w3c.github.io/webdriver/#screen-capture
    virtual Response take_screenshot(Parameters parameters, JsonValue payload) = 0;
    virtual Response take_element_screenshot(Parameters parameters, JsonValue payload) = 0;

    // 18. Print, https://w3c.github.io/webdriver/#print
    virtual Response print_page(Parameters parameters, JsonValue payload) = 0;

protected:
    Client(NonnullOwnPtr<Core::Stream::BufferedTCPSocket>, Core::Object* parent);

private:
    using WrappedError = Variant<AK::Error, WebDriver::Error>;

    void die();
    ErrorOr<void, WrappedError> on_ready_to_read();
    ErrorOr<JsonValue, WrappedError> read_body_as_json();
    ErrorOr<void, WrappedError> handle_request(JsonValue body);
    ErrorOr<void, WrappedError> send_success_response(JsonValue result);
    ErrorOr<void, WrappedError> send_error_response(Error const& error);
    void log_response(unsigned code);

    NonnullOwnPtr<Core::Stream::BufferedTCPSocket> m_socket;
    Optional<HTTP::HttpRequest> m_request;
};

}
