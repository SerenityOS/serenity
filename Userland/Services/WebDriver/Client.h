/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>
#include <LibHTTP/Forward.h>
#include <LibHTTP/HttpRequest.h>
#include <LibWeb/WebDriver/Error.h>
#include <LibWeb/WebDriver/Response.h>
#include <WebDriver/Session.h>

namespace WebDriver {

class Client final : public Core::Object {
    C_OBJECT(Client);

public:
    void start();
    void close_session(unsigned session_id);

private:
    Client(NonnullOwnPtr<Core::Stream::BufferedTCPSocket>, Core::Object* parent);

    ErrorOr<JsonValue> read_body_as_json(HTTP::HttpRequest const&);
    ErrorOr<bool> handle_request(HTTP::HttpRequest const&, JsonValue const& body);
    ErrorOr<void> send_response(StringView content, HTTP::HttpRequest const&);
    ErrorOr<void> send_error_response(Web::WebDriver::Error const& error, HTTP::HttpRequest const&);
    void die();
    void log_response(unsigned code, HTTP::HttpRequest const&);

    using RouteHandler = Web::WebDriver::Response (Client::*)(Vector<StringView> const&, JsonValue const&);
    struct Route {
        HTTP::HttpRequest::Method method;
        Vector<String> path;
        RouteHandler handler;
    };

    struct RoutingResult {
        RouteHandler handler;
        Vector<StringView> parameters;
    };

    ErrorOr<RoutingResult, Web::WebDriver::Error> match_route(HTTP::HttpRequest::Method method, String const& resource);
    Web::WebDriver::Response handle_new_session(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_delete_session(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_status(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_timeouts(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_set_timeouts(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_navigate_to(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_current_url(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_back(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_forward(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_refresh(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_title(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_window_handle(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_close_window(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_window_handles(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_window_rect(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_set_window_rect(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_maximize_window(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_minimize_window(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_fullscreen_window(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_find_element(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_find_elements(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_find_element_from_element(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_find_elements_from_element(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_is_element_selected(Vector<StringView> const& parameters, JsonValue const& payload);
    Web::WebDriver::Response handle_get_element_attribute(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_element_property(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_element_css_value(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_element_text(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_element_tag_name(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_element_rect(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_is_element_enabled(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_source(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_execute_script(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_execute_async_script(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_all_cookies(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_get_named_cookie(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_add_cookie(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_delete_cookie(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_delete_all_cookies(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_take_screenshot(Vector<StringView> const&, JsonValue const& payload);
    Web::WebDriver::Response handle_take_element_screenshot(Vector<StringView> const&, JsonValue const& payload);

    ErrorOr<Session*, Web::WebDriver::Error> find_session_with_id(StringView session_id);
    ErrorOr<NonnullOwnPtr<Session>, Web::WebDriver::Error> take_session_with_id(StringView session_id);
    JsonValue make_json_value(JsonValue const&);

    template<typename T>
    static ErrorOr<T, Web::WebDriver::Error> unwrap_result(ErrorOr<T, Variant<Web::WebDriver::Error, Error>> result)
    {
        if (result.is_error()) {
            Variant<Web::WebDriver::Error, Error> error = result.release_error();
            if (error.has<Web::WebDriver::Error>())
                return error.get<Web::WebDriver::Error>();
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnsupportedOperation, error.get<Error>().string_literal());
        }

        return result.release_value();
    }
    static ErrorOr<void, Web::WebDriver::Error> unwrap_result(ErrorOr<void, Variant<Web::WebDriver::Error, Error>> result)
    {
        if (result.is_error()) {
            Variant<Web::WebDriver::Error, Error> error = result.release_error();
            if (error.has<Web::WebDriver::Error>())
                return error.get<Web::WebDriver::Error>();
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::UnsupportedOperation, error.get<Error>().string_literal());
        }
        return {};
    }

    NonnullOwnPtr<Core::Stream::BufferedTCPSocket> m_socket;
    static Vector<Route> s_routes;
    String m_prefix = "/";

    static NonnullOwnPtrVector<Session> s_sessions;
    static Atomic<unsigned> s_next_session_id;
};

}
