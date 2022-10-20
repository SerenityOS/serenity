/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>
#include <LibHTTP/Forward.h>
#include <LibHTTP/HttpRequest.h>
#include <WebDriver/Session.h>
#include <WebDriver/WebDriverError.h>

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
    ErrorOr<void> send_error_response(WebDriverError const& error, HTTP::HttpRequest const&);
    void die();
    void log_response(unsigned code, HTTP::HttpRequest const&);

    using RouteHandler = ErrorOr<JsonValue, WebDriverError> (Client::*)(Vector<StringView> const&, JsonValue const&);
    struct Route {
        HTTP::HttpRequest::Method method;
        Vector<String> path;
        RouteHandler handler;
    };

    struct RoutingResult {
        RouteHandler handler;
        Vector<StringView> parameters;
    };

    ErrorOr<RoutingResult, WebDriverError> match_route(HTTP::HttpRequest::Method method, String const& resource);
    ErrorOr<JsonValue, WebDriverError> handle_new_session(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_delete_session(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_status(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_timeouts(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_set_timeouts(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_navigate_to(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_current_url(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_back(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_forward(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_refresh(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_title(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_window_handle(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_close_window(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_window_handles(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_find_element(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_find_elements(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_find_element_from_element(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_find_elements_from_element(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_element_attribute(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_element_property(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_element_css_value(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_element_tag_name(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_all_cookies(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_get_named_cookie(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_add_cookie(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_delete_cookie(Vector<StringView> const&, JsonValue const& payload);
    ErrorOr<JsonValue, WebDriverError> handle_delete_all_cookies(Vector<StringView> const&, JsonValue const& payload);

    ErrorOr<Session*, WebDriverError> find_session_with_id(StringView session_id);
    JsonValue make_json_value(JsonValue const&);

    template<typename T>
    static ErrorOr<T, WebDriverError> unwrap_result(ErrorOr<T, Variant<WebDriverError, Error>> result)
    {
        if (result.is_error()) {
            Variant<WebDriverError, Error> error = result.release_error();
            if (error.has<WebDriverError>())
                return error.get<WebDriverError>();
            return WebDriverError::from_code(ErrorCode::UnsupportedOperation, error.get<Error>().string_literal());
        }

        return result.release_value();
    }
    static ErrorOr<void, WebDriverError> unwrap_result(ErrorOr<void, Variant<WebDriverError, Error>> result)
    {
        if (result.is_error()) {
            Variant<WebDriverError, Error> error = result.release_error();
            if (error.has<WebDriverError>())
                return error.get<WebDriverError>();
            return WebDriverError::from_code(ErrorCode::UnsupportedOperation, error.get<Error>().string_literal());
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
