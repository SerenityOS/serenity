/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>
#include <LibHTTP/Forward.h>
#include <LibHTTP/HttpRequest.h>
#include <WebDriver/HttpError.h>
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
    ErrorOr<void> send_error_response(HttpError const& error, HTTP::HttpRequest const&);
    void die();
    void log_response(unsigned code, HTTP::HttpRequest const&);

    using RouteHandler = ErrorOr<JsonValue, HttpError> (Client::*)(Vector<StringView>, JsonValue const&);
    struct Route {
        HTTP::HttpRequest::Method method;
        Vector<String> path;
        RouteHandler handler;
    };

    struct RoutingResult {
        RouteHandler handler;
        Vector<StringView> parameters;
    };

    ErrorOr<RoutingResult, HttpError> match_route(HTTP::HttpRequest::Method method, String resource);
    ErrorOr<JsonValue, HttpError> handle_post_session(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_delete_session(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_get_status(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_post_url(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_get_url(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_get_title(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_delete_window(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_refresh(Vector<StringView>, JsonValue const& payload);
    ErrorOr<JsonValue, HttpError> handle_back(Vector<StringView>, JsonValue const& payload);

    ErrorOr<Session*, HttpError> find_session_with_id(StringView session_id);
    JsonValue make_json_value(JsonValue const&);

    template<typename T>
    static ErrorOr<T, HttpError> unwrap_result(ErrorOr<T, Variant<HttpError, Error>> result)
    {
        if (result.is_error()) {
            Variant<HttpError, Error> error = result.release_error();
            if (error.has<HttpError>())
                return error.get<HttpError>();
            return HttpError { 500, "unsupported operation", error.get<Error>().string_literal() };
        }

        return result.release_value();
    }
    static ErrorOr<void, HttpError> unwrap_result(ErrorOr<void, Variant<HttpError, Error>> result)
    {
        if (result.is_error()) {
            Variant<HttpError, Error> error = result.release_error();
            if (error.has<HttpError>())
                return error.get<HttpError>();
            return HttpError { 500, "unsupported operation", error.get<Error>().string_literal() };
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
