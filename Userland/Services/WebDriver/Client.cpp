/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/MemoryStream.h>
#include <LibHTTP/HttpRequest.h>
#include <LibHTTP/HttpResponse.h>
#include <WebDriver/Client.h>
#include <WebDriver/Session.h>
#include <WebDriver/TimeoutsConfiguration.h>

namespace WebDriver {

Atomic<unsigned> Client::s_next_session_id;
NonnullOwnPtrVector<Session> Client::s_sessions;
Vector<Client::Route> Client::s_routes = {
    { HTTP::HttpRequest::Method::POST, { "session" }, &Client::handle_new_session },
    { HTTP::HttpRequest::Method::DELETE, { "session", ":session_id" }, &Client::handle_delete_session },
    { HTTP::HttpRequest::Method::GET, { "status" }, &Client::handle_get_status },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "timeouts" }, &Client::handle_get_timeouts },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "timeouts" }, &Client::handle_set_timeouts },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "url" }, &Client::handle_navigate_to },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "url" }, &Client::handle_get_current_url },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "back" }, &Client::handle_back },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "forward" }, &Client::handle_forward },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "refresh" }, &Client::handle_refresh },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "title" }, &Client::handle_get_title },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "window" }, &Client::handle_get_window_handle },
    { HTTP::HttpRequest::Method::DELETE, { "session", ":session_id", "window" }, &Client::handle_close_window },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "window", "handles" }, &Client::handle_get_window_handles },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "element" }, &Client::handle_find_element },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "elements" }, &Client::handle_find_elements },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "element", ":element_id", "element" }, &Client::handle_find_element_from_element },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "element", ":element_id", "elements" }, &Client::handle_find_elements_from_element },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "element", ":element_id", "attribute", ":name" }, &Client::handle_get_element_attribute },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "element", ":element_id", "property", ":name" }, &Client::handle_get_element_property },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "cookie" }, &Client::handle_get_all_cookies },
    { HTTP::HttpRequest::Method::GET, { "session", ":session_id", "cookie", ":name" }, &Client::handle_get_named_cookie },
    { HTTP::HttpRequest::Method::POST, { "session", ":session_id", "cookie" }, &Client::handle_add_cookie },
    { HTTP::HttpRequest::Method::DELETE, { "session", ":session_id", "cookie", ":name" }, &Client::handle_delete_cookie },
    { HTTP::HttpRequest::Method::DELETE, { "session", ":session_id", "cookie" }, &Client::handle_delete_all_cookies },
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

            auto maybe_data = m_socket->read(buffer);
            if (maybe_data.is_error()) {
                warnln("Failed to read data from the request: {}", maybe_data.error());
                die();
                return;
            }

            if (m_socket->is_eof()) {
                die();
                break;
            }

            builder.append(StringView(maybe_data.value()));
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
    // If we received a multipart body here, this would fail badly.
    unsigned content_length = 0;
    for (auto const& header : request.headers()) {
        if (header.name.equals_ignoring_case("Content-Length"sv)) {
            content_length = header.value.to_int(TrimWhitespace::Yes).value_or(0);
            break;
        }
    }

    if (!content_length)
        return JsonValue();

    // FIXME: Check the Content-Type is actually application/json
    JsonParser json_parser(request.body());
    return json_parser.parse();
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
        dbgln_if(WEBDRIVER_DEBUG, "Failed to match route: {}", error);
        TRY(send_error_response(error, request));
        return false;
    }

    auto routing_result = routing_result_match.release_value();
    auto result = (this->*routing_result.handler)(routing_result.parameters, body);
    if (result.is_error()) {
        dbgln_if(WEBDRIVER_DEBUG, "Error in calling route handler: {}", result.error());
        TRY(send_error_response(result.release_error(), request));
        return false;
    }

    auto object = result.release_value();
    TRY(send_response(object.to_string(), request));

    return true;
}

// https://w3c.github.io/webdriver/#dfn-send-a-response
ErrorOr<void> Client::send_response(StringView content, HTTP::HttpRequest const& request)
{
    // FIXME: Implement to spec.

    StringBuilder builder;
    builder.append("HTTP/1.0 200 OK\r\n"sv);
    builder.append("Server: WebDriver (SerenityOS)\r\n"sv);
    builder.append("X-Frame-Options: SAMEORIGIN\r\n"sv);
    builder.append("X-Content-Type-Options: nosniff\r\n"sv);
    builder.append("Pragma: no-cache\r\n"sv);
    builder.append("Content-Type: application/json; charset=utf-8\r\n"sv);
    builder.appendff("Content-Length: {}\r\n", content.length());
    builder.append("\r\n"sv);

    auto builder_contents = builder.to_byte_buffer();
    TRY(m_socket->write(builder_contents));
    TRY(m_socket->write(content.bytes()));
    log_response(200, request);

    auto keep_alive = false;
    if (auto it = request.headers().find_if([](auto& header) { return header.name.equals_ignoring_case("Connection"sv); }); !it.is_end()) {
        if (it->value.trim_whitespace().equals_ignoring_case("keep-alive"sv))
            keep_alive = true;
    }
    if (!keep_alive)
        m_socket->close();

    return {};
}

// https://w3c.github.io/webdriver/#dfn-send-an-error
ErrorOr<void> Client::send_error_response(HttpError const& error, HTTP::HttpRequest const& request)
{
    // FIXME: Implement to spec.

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
    header_builder.append("\r\n"sv);
    header_builder.append("Content-Type: application/json; charset=UTF-8\r\n"sv);
    header_builder.appendff("Content-Length: {}\r\n", content_builder.length());
    header_builder.append("\r\n"sv);
    TRY(m_socket->write(header_builder.to_byte_buffer()));
    TRY(m_socket->write(content_builder.to_byte_buffer()));

    log_response(error.http_status, request);
    return {};
}

void Client::log_response(unsigned code, HTTP::HttpRequest const& request)
{
    outln("{} :: {:03d} :: {} {}", Core::DateTime::now().to_string(), code, request.method_name(), request.resource());
}

// https://w3c.github.io/webdriver/#dfn-match-a-request
ErrorOr<Client::RoutingResult, HttpError> Client::match_route(HTTP::HttpRequest::Method method, String const& resource)
{
    // FIXME: Implement to spec.

    dbgln_if(WEBDRIVER_DEBUG, "match_route({}, {})", HTTP::to_string(method), resource);

    // https://w3c.github.io/webdriver/webdriver-spec.html#routing-requests
    if (!resource.starts_with(m_prefix))
        return HttpError { 404, "unknown command", "The resource doesn't start with the prefix." };

    Vector<StringView> resource_split = resource.substring_view(m_prefix.length()).split_view('/', true);
    Vector<StringView> parameters;

    bool matched_path = false;

    for (auto const& route : Client::s_routes) {
        dbgln_if(WEBDRIVER_DEBUG, "- Checking {} {}", HTTP::to_string(route.method), String::join("/"sv, route.path));
        if (resource_split.size() != route.path.size()) {
            dbgln_if(WEBDRIVER_DEBUG, "-> Discarding: Wrong length");
            continue;
        }

        bool match = true;
        for (size_t i = 0; i < route.path.size(); ++i) {
            if (route.path[i].starts_with(':')) {
                parameters.append(resource_split[i]);
                continue;
            }

            if (route.path[i] != resource_split[i]) {
                match = false;
                parameters.clear();
                dbgln_if(WEBDRIVER_DEBUG, "-> Discarding: Part `{}` does not match `{}`", route.path[i], resource_split[i]);
                break;
            }
        }

        if (match && route.method == method) {
            dbgln_if(WEBDRIVER_DEBUG, "-> Matched! :^)");
            return RoutingResult { route.handler, parameters };
        }
        matched_path = true;
    }

    // Matched a path, but didn't match a known method
    if (matched_path) {
        dbgln_if(WEBDRIVER_DEBUG, "- A path matched, but method didn't. :^(");
        return HttpError { 405, "unknown method", "The command matched a known URL but did not match a method for that URL." };
    }

    // Didn't have any match
    dbgln_if(WEBDRIVER_DEBUG, "- No matches. :^(");
    return HttpError { 404, "unknown command", "The command was not recognized." };
}

ErrorOr<Session*, HttpError> Client::find_session_with_id(StringView session_id)
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

void Client::close_session(unsigned session_id)
{
    bool found = Client::s_sessions.remove_first_matching([&](auto const& it) {
        return it->session_id() == session_id;
    });

    if (found)
        dbgln_if(WEBDRIVER_DEBUG, "Shut down session {}", session_id);
    else
        dbgln_if(WEBDRIVER_DEBUG, "Unable to shut down session {}: Not found", session_id);
}

JsonValue Client::make_json_value(JsonValue const& value)
{
    JsonObject result;
    result.set("value", value);
    return result;
}

// 8.1 New Session, https://w3c.github.io/webdriver/#dfn-new-sessions
// POST /session
ErrorOr<JsonValue, HttpError> Client::handle_new_session(Vector<StringView> const&, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session");

    // FIXME: 1. If the maximum active sessions is equal to the length of the list of active sessions,
    //           return error with error code session not created.

    // FIXME: 2. If the remote end is an intermediary node, take implementation-defined steps that either
    //           result in returning an error with error code session not created, or in returning a
    //           success with data that is isomorphic to that returned by remote ends according to the
    //           rest of this algorithm. If an error is not returned, the intermediary node must retain a
    //           reference to the session created on the upstream node as the associated session such
    //           that commands may be forwarded to this associated session on subsequent commands.

    // FIXME: 3. If the maximum active sessions is equal to the length of the list of active sessions,
    //           return error with error code session not created.

    // FIXME: 4. Let capabilities be the result of trying to process capabilities with parameters as an argument.
    auto capabilities = JsonObject {};

    // FIXME: 5. If capabilities’s is null, return error with error code session not created.

    // 6. Let session id be the result of generating a UUID.
    // FIXME: Actually create a UUID.
    auto session_id = Client::s_next_session_id++;

    // 7. Let session be a new session with the session ID of session id.
    NonnullOwnPtr<Session> session = make<Session>(session_id, *this);
    auto start_result = session->start();
    if (start_result.is_error()) {
        return HttpError { 500, "Failed to start session", start_result.error().string_literal() };
    }

    // FIXME: 8. Set the current session to session.

    // FIXME: 9. Run any WebDriver new session algorithm defined in external specifications,
    //           with arguments session and capabilities.

    // 10. Append session to active sessions.
    Client::s_sessions.append(move(session));

    // 11. Let body be a JSON Object initialized with:
    JsonObject body;
    // "sessionId"
    //     session id
    body.set("sessionId", String::number(session_id));
    // "capabilities"
    //     capabilities
    body.set("capabilities", move(capabilities));

    // FIXME: 12. Initialize the following from capabilities:
    //            NOTE: See spec for steps

    // FIXME: 13. Set the webdriver-active flag to true.

    // FIXME: 14. Set the current top-level browsing context for session with the top-level browsing context
    //            of the UA’s current browsing context.

    // FIXME: 15. Set the request queue to a new queue.

    // 16. Return success with data body.
    return make_json_value(body);
}

// 8.2 Delete Session, https://w3c.github.io/webdriver/#dfn-delete-session
// DELETE /session/{session id}
ErrorOr<JsonValue, HttpError> Client::handle_delete_session(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>");

    // 1. If the current session is an active session, try to close the session.
    auto* session = TRY(find_session_with_id(parameters[0]));

    auto stop_result = session->stop();
    if (stop_result.is_error()) {
        return HttpError { 500, "unsupported operation", stop_result.error().string_literal() };
    }

    // 2. Return success with data null.
    return make_json_value(JsonValue());
}

// 8.3 Status, https://w3c.github.io/webdriver/#dfn-status
// GET /status
ErrorOr<JsonValue, HttpError> Client::handle_get_status(Vector<StringView> const&, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /status");

    // 1. Let body be a new JSON Object with the following properties:
    //    "ready"
    //        The remote end’s readiness state.
    //    "message"
    //        An implementation-defined string explaining the remote end’s readiness state.
    // FIXME: Report if we are somehow not ready.
    JsonObject body;
    body.set("ready", true);
    body.set("message", "Ready to start some sessions!");

    // 2. Return success with data body.
    return body;
}

// 9.1 Get Timeouts, https://w3c.github.io/webdriver/#dfn-get-timeouts
// GET /session/{session id}/timeouts
ErrorOr<JsonValue, HttpError> Client::handle_get_timeouts(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session id>/timeouts");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = session->get_timeouts();
    return make_json_value(result);
}

// 9.2 Set Timeouts, https://w3c.github.io/webdriver/#dfn-set-timeouts
// POST /session/{session id}/timeouts
ErrorOr<JsonValue, HttpError> Client::handle_set_timeouts(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session id>/timeouts");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->set_timeouts(payload));
    return make_json_value(result);
}

// 10.1 Navigate To, https://w3c.github.io/webdriver/#dfn-navigate-to
// POST /session/{session id}/url
ErrorOr<JsonValue, HttpError> Client::handle_navigate_to(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/url");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->navigate_to(payload));
    return make_json_value(result);
}

// 10.2 Get Current URL, https://w3c.github.io/webdriver/#dfn-get-current-url
// GET /session/{session id}/url
ErrorOr<JsonValue, HttpError> Client::handle_get_current_url(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/url");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->get_current_url());
    return make_json_value(result);
}

// 10.3 Back, https://w3c.github.io/webdriver/#dfn-back
// POST /session/{session id}/back
ErrorOr<JsonValue, HttpError> Client::handle_back(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/back");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->back());
    return make_json_value(result);
}

// 10.4 Forward, https://w3c.github.io/webdriver/#dfn-forward
// POST /session/{session id}/forward
ErrorOr<JsonValue, HttpError> Client::handle_forward(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/forward");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->forward());
    return make_json_value(result);
}

// 10.5 Refresh, https://w3c.github.io/webdriver/#dfn-refresh
// POST /session/{session id}/refresh
ErrorOr<JsonValue, HttpError> Client::handle_refresh(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/refresh");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->refresh());
    return make_json_value(result);
}

// 10.6 Get Title, https://w3c.github.io/webdriver/#dfn-get-title
// GET /session/{session id}/title
ErrorOr<JsonValue, HttpError> Client::handle_get_title(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/title");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->get_title());
    return make_json_value(result);
}

// 11.1 Get Window Handle, https://w3c.github.io/webdriver/#get-window-handle
// GET /session/{session id}/window
ErrorOr<JsonValue, HttpError> Client::handle_get_window_handle(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/window");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->get_window_handle());
    return make_json_value(result);
}

// 11.2 Close Window, https://w3c.github.io/webdriver/#dfn-close-window
// DELETE /session/{session id}/window
ErrorOr<JsonValue, HttpError> Client::handle_close_window(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>/window");
    auto* session = TRY(find_session_with_id(parameters[0]));
    TRY(unwrap_result(session->close_window()));
    return make_json_value(JsonValue());
}

// 11.4 Get Window Handles, https://w3c.github.io/webdriver/#dfn-get-window-handles
// GET /session/{session id}/window/handles
ErrorOr<JsonValue, HttpError> Client::handle_get_window_handles(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/window/handles");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->get_window_handles());
    return make_json_value(result);
}

// 12.3.2 Find Element, https://w3c.github.io/webdriver/#dfn-find-element
// POST /session/{session id}/element
ErrorOr<JsonValue, HttpError> Client::handle_find_element(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/element");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->find_element(payload));
    return make_json_value(result);
}

// 12.3.3 Find Elements, https://w3c.github.io/webdriver/#dfn-find-elements
// POST /session/{session id}/elements
ErrorOr<JsonValue, HttpError> Client::handle_find_elements(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/elements");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->find_elements(payload));
    return make_json_value(result);
}

// 12.3.4 Find Element From Element, https://w3c.github.io/webdriver/#dfn-find-element-from-element
// POST	/session/{session id}/element/{element id}/element
ErrorOr<JsonValue, HttpError> Client::handle_find_element_from_element(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/element/<element_id>/element");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->find_element_from_element(payload, parameters[1]));
    return make_json_value(result);
}

// 12.3.5 Find Elements From Element, https://w3c.github.io/webdriver/#dfn-find-elements-from-element
// POST /session/{session id}/element/{element id}/elements
ErrorOr<JsonValue, HttpError> Client::handle_find_elements_from_element(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/element/<element_id>/elements");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->find_elements_from_element(payload, parameters[1]));
    return make_json_value(result);
}

// 12.4.2 Get Element Attribute, https://w3c.github.io/webdriver/#dfn-get-element-attribute
// GET /session/{session id}/element/{element id}/attribute/{name}
ErrorOr<JsonValue, HttpError> Client::handle_get_element_attribute(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/attribute/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->get_element_attribute(payload, parameters[1], parameters[2]));
    return make_json_value(result);
}

// 12.4.3 Get Element Property, https://w3c.github.io/webdriver/#dfn-get-element-property
// GET 	/session/{session id}/element/{element id}/property/{name}
ErrorOr<JsonValue, HttpError> Client::handle_get_element_property(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/property/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->get_element_property(payload, parameters[1], parameters[2]));
    return make_json_value(result);
}

// 14.1 Get All Cookies, https://w3c.github.io/webdriver/#dfn-get-all-cookies
// GET /session/{session id}/cookie
ErrorOr<JsonValue, HttpError> Client::handle_get_all_cookies(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/cookie");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto cookies = TRY(session->get_all_cookies());
    return make_json_value(cookies);
}

// 14.2 Get Named Cookie, https://w3c.github.io/webdriver/#dfn-get-named-cookie
// GET /session/{session id}/cookie/{name}
ErrorOr<JsonValue, HttpError> Client::handle_get_named_cookie(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/cookie/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto cookies = TRY(session->get_named_cookie(parameters[1]));
    return make_json_value(cookies);
}

// 14.3 Add Cookie, https://w3c.github.io/webdriver/#dfn-adding-a-cookie
// POST /session/{session id}/cookie
ErrorOr<JsonValue, HttpError> Client::handle_add_cookie(Vector<StringView> const& parameters, JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/cookie");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->add_cookie(payload));
    return make_json_value(result);
}

// 14.4 Delete Cookie, https://w3c.github.io/webdriver/#dfn-delete-cookie
// DELETE /session/{session id}/cookie/{name}
ErrorOr<JsonValue, HttpError> Client::handle_delete_cookie(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>/cookie/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->delete_cookie(parameters[1]));
    return make_json_value(result);
}

// 14.5 Delete All Cookies, https://w3c.github.io/webdriver/#dfn-delete-all-cookies
// DELETE /session/{session id}/cookie
ErrorOr<JsonValue, HttpError> Client::handle_delete_all_cookies(Vector<StringView> const& parameters, JsonValue const&)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>/cookie");
    auto* session = TRY(find_session_with_id(parameters[0]));
    auto result = TRY(session->delete_all_cookies());
    return make_json_value(result);
}

}
