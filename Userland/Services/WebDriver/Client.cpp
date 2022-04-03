/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/URL.h>
#include <LibCore/DateTime.h>
#include <LibCore/MemoryStream.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <WebDriver/Client.h>
#include <WebDriver/Session.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

namespace WebDriver {

Atomic<unsigned> Client::s_next_session_id;
NonnullOwnPtrVector<WebDriverSession> Client::s_sessions;
Vector<Client::Route> Client::s_routes = {
    { HTTP::HttpRequest::Method::POST, { "session" }, &Client::handle_post_session },
    { HTTP::HttpRequest::Method::DELETE, { "session", ":session_id" }, &Client::handle_delete_session },
    { HTTP::HttpRequest::Method::GET, { "status" }, &Client::handle_get_status },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "url" }, &Client::handle_post_url },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "url" }, &Client::handle_get_url },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "title" }, &Client::handle_get_title },
    { HTTP::HttpRequest::Method::DELETE, { "session", ":session_id", "window" }, &Client::handle_delete_window },
};

Client::Client(NonnullOwnPtr<Core::Stream::BufferedTCPSocket> socket, Core::Object* parent)
    : Core::Object(parent)
    , m_socket(move(socket))
{
}

void Client::die()
{
    m_socket->close();
    deferred_invoke([this] { remove_from_parent(); });
}

void Client::start()
{
    m_socket->on_ready_to_read = [this] {
        StringBuilder builder;

        // FIXME: All this should be moved to LibHTTP and be made spec compliant
        auto maybe_buffer = ByteBuffer::create_uninitialized(m_socket->buffer_size());
        if (maybe_buffer.is_error()) {
            warnln("Could not create buffer for client: {}", maybe_buffer.error());
            die();
            return;
        }

        auto buffer = maybe_buffer.release_value();
        for (;;) {
            auto maybe_can_read = m_socket->can_read_without_blocking();
            if (maybe_can_read.is_error()) {
                warnln("Failed to get the blocking status for the socket: {}", maybe_can_read.error());
                die();
                return;
            }

            if (!maybe_can_read.value())
                break;

            auto maybe_nread = m_socket->read_line(buffer);
            if (maybe_nread.is_error()) {
                warnln("Failed to read a line from the request: {}", maybe_nread.error());
                die();
                return;
            }

            if (m_socket->is_eof()) {
                die();
                break;
            }

            // Normalize \r at the end of the line
            size_t nread = maybe_nread.release_value();
            while (nread && buffer.data()[nread - 1] == '\r')
                nread--;

            builder.append(StringView { buffer.data(), nread });
            builder.append("\r\n");

            // End of headers
            if (!nread) {
                break;
            }
        }

        auto request = builder.to_byte_buffer();
        auto http_request_or_error = HTTP::HttpRequest::from_raw_request(request);
        if (!http_request_or_error.has_value())
            return;

        auto http_request = http_request_or_error.release_value();

        auto body_or_error = read_body_as_json(http_request);
        if (body_or_error.is_error()) {
            warnln("Failed to read the request body: {}", body_or_error.error());
            die();
            return;
        }

        auto maybe_did_handle = handle_request(http_request, body_or_error.value());
        if (maybe_did_handle.is_error()) {
            warnln("Failed to handle the request: {}", maybe_did_handle.error());
        }

        die();
    };
}

ErrorOr<JsonValue> Client::read_body_as_json(HTTP::HttpRequest const& request)
{
    // FIXME: Teach LibHTTP to read a body.
    // If we received a multipart body here, this would fail badly.
    unsigned content_length = 0;
    for (auto const& header : request.headers()) {
        if (header.name.equals_ignoring_case("Content-Length")) {
            content_length = header.value.to_int(TrimWhitespace::Yes).value_or(0);
            break;
        }
    }

    if (!content_length)
        return JsonValue();

    ByteBuffer buffer = TRY(ByteBuffer::create_uninitialized(content_length));
    TRY(m_socket->read(buffer));

    // FIXME: Check the Content-Type is actually application/json
    JsonParser json_parser(StringView(buffer.data(), buffer.size()));
    auto body_or_error = json_parser.parse();
    if (body_or_error.is_error())
        return JsonValue();

    return body_or_error.value();
}

ErrorOr<bool> Client::handle_request(HTTP::HttpRequest const& request, JsonValue const& body)
{
    if constexpr (WEBDRIVER_DEBUG) {
        dbgln("Got HTTP request: {} {}", request.method_name(), request.resource());
        if (!body.is_null())
            dbgln("Body: {}", body.to_string());
    }

    auto routing_result_match = match_route(request.method(), request.resource());
    if (routing_result_match.is_error()) {
        auto error = routing_result_match.release_error();
        TRY(send_error_response(error, request));
        return false;
    }

    auto routing_result = routing_result_match.release_value();
    auto result = (this->*routing_result.handler)(routing_result.parameters, body);
    if (result.is_error()) {
        TRY(send_error_response(result.release_error(), request));
        return false;
    }

    StringBuilder builder;
    JsonValue object = result.release_value();
    object.serialize(builder);

    TRY(send_response(builder.string_view(), request));

    return true;
}

ErrorOr<void> Client::send_response(StringView content, HTTP::HttpRequest const& request)
{
    StringBuilder builder;
    builder.append("HTTP/1.0 200 OK\r\n");
    builder.append("Server: WebDriver (SerenityOS)\r\n");
    builder.append("X-Frame-Options: SAMEORIGIN\r\n");
    builder.append("X-Content-Type-Options: nosniff\r\n");
    builder.append("Pragma: no-cache\r\n");
    builder.append("Content-Type: application/json; charset=utf-8\r\n");
    builder.appendff("Content-Length: {}\r\n", content.length());
    builder.append("\r\n");

    auto builder_contents = builder.to_byte_buffer();
    TRY(m_socket->write(builder_contents));
    TRY(m_socket->write(content.bytes()));
    log_response(200, request);

    auto keep_alive = false;
    if (auto it = request.headers().find_if([](auto& header) { return header.name.equals_ignoring_case("Connection"); }); !it.is_end()) {
        if (it->value.trim_whitespace().equals_ignoring_case("keep-alive"))
            keep_alive = true;
    }
    if (!keep_alive)
        m_socket->close();

    return {};
}

ErrorOr<void> Client::send_error_response(HttpError const& error, HTTP::HttpRequest const& request)
{
    dbgln("send_error_response: {} {}: {}", error.http_status, error.error, error.message);
    auto reason_phrase = HTTP::HttpResponse::reason_phrase_for_code(error.http_status);

    auto result = JsonObject();
    result.set("error", error.error);
    result.set("message", error.message);
    result.set("stacktrace", "");

    StringBuilder content_builder;
    result.serialize(content_builder);

    StringBuilder header_builder;
    header_builder.appendff("HTTP/1.0 {} ", error.http_status);
    header_builder.append(reason_phrase);
    header_builder.append("\r\n");
    header_builder.append("Content-Type: application/json; charset=UTF-8\r\n");
    header_builder.appendff("Content-Length: {}\r\n", content_builder.length());
    header_builder.append("\r\n");
    TRY(m_socket->write(header_builder.to_byte_buffer()));
    TRY(m_socket->write(content_builder.to_byte_buffer()));

    log_response(error.http_status, request);
    return {};
}

void Client::log_response(unsigned code, HTTP::HttpRequest const& request)
{
    outln("{} :: {:03d} :: {} {}", Core::DateTime::now().to_string(), code, request.method_name(), request.resource());
}

ErrorOr<Client::RoutingResult, HttpError> Client::match_route(HTTP::HttpRequest::Method method, String resource)
{
    // https://w3c.github.io/webdriver/webdriver-spec.html#routing-requests
    if (!resource.starts_with(m_prefix))
        return HttpError { 404, "unknown command", "The resource doesn't start with the prefix." };

    Vector<StringView> resource_split = resource.substring_view(m_prefix.length()).split_view('/', true);
    Vector<StringView> parameters;

    bool matched_path = false;

    for (auto const& route : Client::s_routes) {
        if (resource_split.size() != route.path.size()) {
            continue;
        }

        bool match = true;
        for (size_t i = 0; i < route.path.size(); ++i) {
            if (route.path[i].starts_with(":")) {
                parameters.append(resource_split[i]);
                continue;
            }

            if (route.path[i] != resource_split[i]) {
                match = false;
                parameters.clear();
                break;
            }
        }

        if (match && route.method == method) {
            return RoutingResult { route.handler, parameters };
        }
        matched_path = true;
    }

    // Matched a path, but didn't match a known method
    if (matched_path)
        return HttpError { 405, "unknown method", "The command matched a known URL but did not match a method for that URL." };

    // Didn't have any match
    return HttpError { 404, "unknown command", "The command was not recognized." };
}

ErrorOr<WebDriverSession*, HttpError> Client::find_session_with_id(StringView session_id)
{
    auto session_id_or_error = session_id.to_uint<>();
    if (!session_id_or_error.has_value())
        return HttpError { 404, "invalid session id", "Invalid session id" };

    for (auto& session : Client::s_sessions) {
        if (session.session_id() == session_id_or_error.value())
            return &session;
    }
    return HttpError { 404, "invalid session id", "Invalid session id" };
}

JsonValue Client::make_json_value(JsonValue value)
{
    JsonObject result;
    result.set("value", value);
    return result;
}

ErrorOr<JsonValue, HttpError> Client::handle_post_session(Vector<StringView>, JsonValue const&)
{
    NonnullOwnPtr<WebDriverSession> session = make<WebDriverSession>(Client::s_next_session_id++);
    auto start_result = session->start();
    if (start_result.is_error()) {
        return HttpError { 500, "Failed to start session", start_result.error().string_literal() };
    }

    Client::s_sessions.append(move(session));
    JsonObject value;
    value.set("sessionId", String::formatted("{}", Client::s_sessions.last().session_id()));

    return make_json_value(value);
}

ErrorOr<JsonValue, HttpError> Client::handle_delete_session(Vector<StringView> parameters, JsonValue const&)
{
    WebDriverSession* session = TRY(find_session_with_id(parameters[0]));

    auto stop_result = session->stop();
    if (stop_result.is_error()) {
        return HttpError { 500, "unsupported operation", stop_result.error().string_literal() };
    }

    return make_json_value(JsonValue());
}

ErrorOr<JsonValue, HttpError> Client::handle_get_status(Vector<StringView>, JsonValue const&)
{
    return HttpError { 400, "", "" };
}

ErrorOr<JsonValue, HttpError> Client::handle_post_url(Vector<StringView> parameters, JsonValue const& payload)
{
    WebDriverSession* session = TRY(find_session_with_id(parameters[0]));

    auto result = TRY(session->post_url(payload));
    return make_json_value(result);
}

ErrorOr<JsonValue, HttpError> Client::handle_get_url(Vector<StringView>, JsonValue const&)
{
    return HttpError { 400, "", "" };
}

ErrorOr<JsonValue, HttpError> Client::handle_get_title(Vector<StringView> parameters, JsonValue const&)
{
    WebDriverSession* session = TRY(find_session_with_id(parameters[0]));

    auto result = TRY(session->get_title());

    return make_json_value(result);
}

ErrorOr<JsonValue, HttpError> Client::handle_delete_window(Vector<StringView> parameters, JsonValue const&)
{
    WebDriverSession* session = TRY(find_session_with_id(parameters[0]));

    TRY(unwrap_result(session->delete_window()));

    return make_json_value(JsonValue());
}
}
