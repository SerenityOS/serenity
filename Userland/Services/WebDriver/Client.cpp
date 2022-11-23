/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibWeb/WebDriver/Capabilities.h>
#include <LibWeb/WebDriver/TimeoutsConfiguration.h>
#include <WebDriver/Client.h>

namespace WebDriver {

Atomic<unsigned> Client::s_next_session_id;
NonnullOwnPtrVector<Session> Client::s_sessions;

ErrorOr<NonnullRefPtr<Client>> Client::try_create(NonnullOwnPtr<Core::Stream::BufferedTCPSocket> socket, Core::Object* parent)
{
    TRY(socket->set_blocking(true));
    return adopt_nonnull_ref_or_enomem(new (nothrow) Client(move(socket), parent));
}

Client::Client(NonnullOwnPtr<Core::Stream::BufferedTCPSocket> socket, Core::Object* parent)
    : Web::WebDriver::Client(move(socket), parent)
{
}

Client::~Client() = default;

ErrorOr<Session*, Web::WebDriver::Error> Client::find_session_with_id(StringView session_id)
{
    auto session_id_or_error = session_id.to_uint<>();
    if (!session_id_or_error.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidSessionId, "Invalid session id");

    for (auto& session : Client::s_sessions) {
        if (session.session_id() == session_id_or_error.value())
            return &session;
    }
    return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidSessionId, "Invalid session id");
}

ErrorOr<NonnullOwnPtr<Session>, Web::WebDriver::Error> Client::take_session_with_id(StringView session_id)
{
    auto session_id_or_error = session_id.to_uint<>();
    if (!session_id_or_error.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidSessionId, "Invalid session id");

    for (size_t i = 0; i < Client::s_sessions.size(); ++i) {
        if (Client::s_sessions[i].session_id() == session_id_or_error.value()) {
            return Client::s_sessions.take(i);
        }
    }

    return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidSessionId, "Invalid session id");
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

// Step 12 of https://w3c.github.io/webdriver/#dfn-new-sessions
static void initialize_session_from_capabilities(WebContentConnection& web_content_connection, JsonObject& capabilities)
{
    // 1. Let strategy be the result of getting property "pageLoadStrategy" from capabilities.
    auto const* strategy = capabilities.get_ptr("pageLoadStrategy"sv);

    // 2. If strategy is a string, set the current session’s page loading strategy to strategy. Otherwise, set the page loading strategy to normal and set a property of capabilities with name "pageLoadStrategy" and value "normal".
    if (strategy && strategy->is_string())
        web_content_connection.async_set_page_load_strategy(Web::WebDriver::page_load_strategy_from_string(strategy->as_string()));
    else
        capabilities.set("pageLoadStrategy"sv, "normal"sv);

    // 3. Let strictFileInteractability be the result of getting property "strictFileInteractability" from capabilities.
    auto const* strict_file_interactiblity = capabilities.get_ptr("strictFileInteractability"sv);

    // 4. If strictFileInteractability is a boolean, set the current session’s strict file interactability to strictFileInteractability. Otherwise set the current session’s strict file interactability to false.
    if (strict_file_interactiblity && strict_file_interactiblity->is_bool())
        web_content_connection.async_set_strict_file_interactability(strict_file_interactiblity->as_bool());
    else
        capabilities.set("strictFileInteractability"sv, false);

    // FIXME: 5. Let proxy be the result of getting property "proxy" from capabilities and run the substeps of the first matching statement:
    // FIXME:     proxy is a proxy configuration object
    // FIXME:         Take implementation-defined steps to set the user agent proxy using the extracted proxy configuration. If the defined proxy cannot be configured return error with error code session not created.
    // FIXME:     Otherwise
    // FIXME:         Set a property of capabilities with name "proxy" and a value that is a new JSON Object.

    // 6. If capabilities has a property with the key "timeouts":
    if (auto const* timeouts = capabilities.get_ptr("timeouts"sv); timeouts && timeouts->is_object()) {
        // a. Let timeouts be the result of trying to JSON deserialize as a timeouts configuration the value of the "timeouts" property.
        // NOTE: This happens on the remote end.

        // b. Make the session timeouts the new timeouts.
        MUST(web_content_connection.set_timeouts(*timeouts));
    } else {
        // 7. Set a property on capabilities with name "timeouts" and value that of the JSON deserialization of the session timeouts.
        capabilities.set("timeouts"sv, Web::WebDriver::timeouts_object({}));
    }

    // 8. Apply changes to the user agent for any implementation-defined capabilities selected during the capabilities processing step.
    auto const* behavior = capabilities.get_ptr("unhandledPromptBehavior"sv);
    if (behavior && behavior->is_string())
        web_content_connection.async_set_unhandled_prompt_behavior(Web::WebDriver::unhandled_prompt_behavior_from_string(behavior->as_string()));
    else
        capabilities.set("unhandledPromptBehavior"sv, "dismiss and notify"sv);
}

// 8.1 New Session, https://w3c.github.io/webdriver/#dfn-new-sessions
// POST /session
Web::WebDriver::Response Client::new_session(Web::WebDriver::Parameters, JsonValue payload)
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

    // 4. Let capabilities be the result of trying to process capabilities with parameters as an argument.
    auto capabilities = TRY(Web::WebDriver::process_capabilities(payload));

    // 5. If capabilities’s is null, return error with error code session not created.
    if (capabilities.is_null())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::SessionNotCreated, "Could not match capabilities"sv);

    // 6. Let session id be the result of generating a UUID.
    // FIXME: Actually create a UUID.
    auto session_id = Client::s_next_session_id++;

    // 7. Let session be a new session with the session ID of session id.
    Web::WebDriver::LadybirdOptions options { capabilities.as_object() };
    auto session = make<Session>(session_id, *this, move(options));

    if (auto start_result = session->start(); start_result.is_error())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::SessionNotCreated, String::formatted("Failed to start session: {}", start_result.error().string_literal()));

    auto& web_content_connection = session->web_content_connection();

    // FIXME: 8. Set the current session to session.

    // FIXME: 9. Run any WebDriver new session algorithm defined in external specifications,
    //           with arguments session and capabilities.

    // 10. Append session to active sessions.
    Client::s_sessions.append(move(session));

    // NOTE: We do step 12 before 11 because step 12 mutates the capabilities we set in step 11.

    // 12. Initialize the following from capabilities:
    initialize_session_from_capabilities(web_content_connection, capabilities.as_object());

    // 11. Let body be a JSON Object initialized with:
    JsonObject body;
    // "sessionId"
    //     session id
    body.set("sessionId", String::number(session_id));
    // "capabilities"
    //     capabilities
    body.set("capabilities", move(capabilities));

    // 13. Set the webdriver-active flag to true.
    web_content_connection.async_set_is_webdriver_active(true);

    // FIXME: 14. Set the current top-level browsing context for session with the top-level browsing context
    //            of the UA’s current browsing context.

    // FIXME: 15. Set the request queue to a new queue.

    // 16. Return success with data body.
    return JsonValue { move(body) };
}

// 8.2 Delete Session, https://w3c.github.io/webdriver/#dfn-delete-session
// DELETE /session/{session id}
Web::WebDriver::Response Client::delete_session(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>");

    // 1. If the current session is an active session, try to close the session.
    auto session = TRY(take_session_with_id(parameters[0]));
    TRY(session->stop());

    // 2. Return success with data null.
    return JsonValue {};
}

// 8.3 Status, https://w3c.github.io/webdriver/#dfn-status
// GET /status
Web::WebDriver::Response Client::get_status(Web::WebDriver::Parameters, JsonValue)
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
    return JsonValue { body };
}

// 9.1 Get Timeouts, https://w3c.github.io/webdriver/#dfn-get-timeouts
// GET /session/{session id}/timeouts
Web::WebDriver::Response Client::get_timeouts(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session id>/timeouts");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_timeouts();
}

// 9.2 Set Timeouts, https://w3c.github.io/webdriver/#dfn-set-timeouts
// POST /session/{session id}/timeouts
Web::WebDriver::Response Client::set_timeouts(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session id>/timeouts");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().set_timeouts(payload);
}

// 10.1 Navigate To, https://w3c.github.io/webdriver/#dfn-navigate-to
// POST /session/{session id}/url
Web::WebDriver::Response Client::navigate_to(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/url");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().navigate_to(payload);
}

// 10.2 Get Current URL, https://w3c.github.io/webdriver/#dfn-get-current-url
// GET /session/{session id}/url
Web::WebDriver::Response Client::get_current_url(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/url");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_current_url();
}

// 10.3 Back, https://w3c.github.io/webdriver/#dfn-back
// POST /session/{session id}/back
Web::WebDriver::Response Client::back(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/back");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().back();
}

// 10.4 Forward, https://w3c.github.io/webdriver/#dfn-forward
// POST /session/{session id}/forward
Web::WebDriver::Response Client::forward(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/forward");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().forward();
}

// 10.5 Refresh, https://w3c.github.io/webdriver/#dfn-refresh
// POST /session/{session id}/refresh
Web::WebDriver::Response Client::refresh(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/refresh");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().refresh();
}

// 10.6 Get Title, https://w3c.github.io/webdriver/#dfn-get-title
// GET /session/{session id}/title
Web::WebDriver::Response Client::get_title(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/title");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_title();
}

// 11.1 Get Window Handle, https://w3c.github.io/webdriver/#get-window-handle
// GET /session/{session id}/window
Web::WebDriver::Response Client::get_window_handle(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/window");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_window_handle();
}

// 11.2 Close Window, https://w3c.github.io/webdriver/#dfn-close-window
// DELETE /session/{session id}/window
Web::WebDriver::Response Client::close_window(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>/window");
    auto* session = TRY(find_session_with_id(parameters[0]));

    auto open_windows = TRY(session->web_content_connection().close_window());
    if (open_windows.is_array() && open_windows.as_array().is_empty())
        TRY(session->stop());

    return open_windows;
}

// 11.4 Get Window Handles, https://w3c.github.io/webdriver/#dfn-get-window-handles
// GET /session/{session id}/window/handles
Web::WebDriver::Response Client::get_window_handles(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/window/handles");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_window_handles();
}

// 11.8.1 Get Window Rect, https://w3c.github.io/webdriver/#dfn-get-window-rect
// GET /session/{session id}/window/rect
Web::WebDriver::Response Client::get_window_rect(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/window/rect");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_window_rect();
}

// 11.8.2 Set Window Rect, https://w3c.github.io/webdriver/#dfn-set-window-rect
// POST /session/{session id}/window/rect
Web::WebDriver::Response Client::set_window_rect(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/window/rect");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().set_window_rect(payload);
}

// 11.8.3 Maximize Window, https://w3c.github.io/webdriver/#dfn-maximize-window
// POST /session/{session id}/window/maximize
Web::WebDriver::Response Client::maximize_window(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/window/maximize");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().maximize_window();
}

// 11.8.4 Minimize Window, https://w3c.github.io/webdriver/#minimize-window
// POST /session/{session id}/window/minimize
Web::WebDriver::Response Client::minimize_window(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/window/minimize");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().minimize_window();
}

// 11.8.5 Fullscreen Window, https://w3c.github.io/webdriver/#dfn-fullscreen-window
// POST /session/{session id}/window/fullscreen
Web::WebDriver::Response Client::fullscreen_window(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/window/fullscreen");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().fullscreen_window();
}

// 12.3.2 Find Element, https://w3c.github.io/webdriver/#dfn-find-element
// POST /session/{session id}/element
Web::WebDriver::Response Client::find_element(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/element");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().find_element(payload);
}

// 12.3.3 Find Elements, https://w3c.github.io/webdriver/#dfn-find-elements
// POST /session/{session id}/elements
Web::WebDriver::Response Client::find_elements(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/elements");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().find_elements(payload);
}

// 12.3.4 Find Element From Element, https://w3c.github.io/webdriver/#dfn-find-element-from-element
// POST /session/{session id}/element/{element id}/element
Web::WebDriver::Response Client::find_element_from_element(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/element/<element_id>/element");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().find_element_from_element(payload, parameters[1]);
}

// 12.3.5 Find Elements From Element, https://w3c.github.io/webdriver/#dfn-find-elements-from-element
// POST /session/{session id}/element/{element id}/elements
Web::WebDriver::Response Client::find_elements_from_element(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/element/<element_id>/elements");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().find_elements_from_element(payload, parameters[1]);
}

// 12.3.6 Find Element From Shadow Root, https://w3c.github.io/webdriver/#find-element-from-shadow-root
// POST /session/{session id}/shadow/{shadow id}/element
Web::WebDriver::Response Client::find_element_from_shadow_root(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/shadow/<shadow_id>/element");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().find_element_from_shadow_root(payload, parameters[1]);
}

// 12.3.7 Find Elements From Shadow Root, https://w3c.github.io/webdriver/#find-elements-from-shadow-root
// POST /session/{session id}/shadow/{shadow id}/elements
Web::WebDriver::Response Client::find_elements_from_shadow_root(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/shadow/<shadow_id>/elements");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().find_elements_from_shadow_root(payload, parameters[1]);
}

// 12.3.8 Get Active Element, https://w3c.github.io/webdriver/#get-active-element
// GET /session/{session id}/element/active
Web::WebDriver::Response Client::get_active_element(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/active");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_active_element();
}

// 12.3.9 Get Element Shadow Root, https://w3c.github.io/webdriver/#get-element-shadow-root
// GET /session/{session id}/element/{element id}/shadow
Web::WebDriver::Response Client::get_element_shadow_root(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/shadow");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_element_shadow_root(parameters[1]);
}

// 12.4.1 Is Element Selected, https://w3c.github.io/webdriver/#dfn-is-element-selected
// GET /session/{session id}/element/{element id}/selected
Web::WebDriver::Response Client::is_element_selected(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/selected");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().is_element_selected(parameters[1]);
}

// 12.4.2 Get Element Attribute, https://w3c.github.io/webdriver/#dfn-get-element-attribute
// GET /session/{session id}/element/{element id}/attribute/{name}
Web::WebDriver::Response Client::get_element_attribute(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/attribute/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_element_attribute(parameters[1], parameters[2]);
}

// 12.4.3 Get Element Property, https://w3c.github.io/webdriver/#dfn-get-element-property
// GET /session/{session id}/element/{element id}/property/{name}
Web::WebDriver::Response Client::get_element_property(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/property/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_element_property(parameters[1], parameters[2]);
}

// 12.4.4 Get Element CSS Value, https://w3c.github.io/webdriver/#dfn-get-element-css-value
// GET /session/{session id}/element/{element id}/css/{property name}
Web::WebDriver::Response Client::get_element_css_value(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/css/<property_name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_element_css_value(parameters[1], parameters[2]);
}

// 12.4.5 Get Element Text, https://w3c.github.io/webdriver/#dfn-get-element-text
// GET /session/{session id}/element/{element id}/text
Web::WebDriver::Response Client::get_element_text(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/text");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_element_text(parameters[1]);
}

// 12.4.6 Get Element Tag Name, https://w3c.github.io/webdriver/#dfn-get-element-tag-name
// GET /session/{session id}/element/{element id}/name
Web::WebDriver::Response Client::get_element_tag_name(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/name");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_element_tag_name(parameters[1]);
}

// 12.4.7 Get Element Rect, https://w3c.github.io/webdriver/#dfn-get-element-rect
// GET /session/{session id}/element/{element id}/rect
Web::WebDriver::Response Client::get_element_rect(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/rect");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_element_rect(parameters[1]);
}

// 12.4.8 Is Element Enabled, https://w3c.github.io/webdriver/#dfn-is-element-enabled
// GET /session/{session id}/element/{element id}/enabled
Web::WebDriver::Response Client::is_element_enabled(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/enabled");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().is_element_enabled(parameters[1]);
}

// 13.1 Get Page Source, https://w3c.github.io/webdriver/#dfn-get-page-source
// GET /session/{session id}/source
Web::WebDriver::Response Client::get_source(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/source");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_source();
}

// 13.2.1 Execute Script, https://w3c.github.io/webdriver/#dfn-execute-script
// POST /session/{session id}/execute/sync
Web::WebDriver::Response Client::execute_script(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/execute/sync");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().execute_script(payload);
}

// 13.2.2 Execute Async Script, https://w3c.github.io/webdriver/#dfn-execute-async-script
// POST /session/{session id}/execute/async
Web::WebDriver::Response Client::execute_async_script(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/execute/async");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().execute_async_script(payload);
}

// 14.1 Get All Cookies, https://w3c.github.io/webdriver/#dfn-get-all-cookies
// GET /session/{session id}/cookie
Web::WebDriver::Response Client::get_all_cookies(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/cookie");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_all_cookies();
}

// 14.2 Get Named Cookie, https://w3c.github.io/webdriver/#dfn-get-named-cookie
// GET /session/{session id}/cookie/{name}
Web::WebDriver::Response Client::get_named_cookie(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/cookie/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_named_cookie(parameters[1]);
}

// 14.3 Add Cookie, https://w3c.github.io/webdriver/#dfn-adding-a-cookie
// POST /session/{session id}/cookie
Web::WebDriver::Response Client::add_cookie(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/cookie");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().add_cookie(payload);
}

// 14.4 Delete Cookie, https://w3c.github.io/webdriver/#dfn-delete-cookie
// DELETE /session/{session id}/cookie/{name}
Web::WebDriver::Response Client::delete_cookie(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>/cookie/<name>");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().delete_cookie(parameters[1]);
}

// 14.5 Delete All Cookies, https://w3c.github.io/webdriver/#dfn-delete-all-cookies
// DELETE /session/{session id}/cookie
Web::WebDriver::Response Client::delete_all_cookies(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling DELETE /session/<session_id>/cookie");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().delete_all_cookies();
}

// 16.1 Dismiss Alert, https://w3c.github.io/webdriver/#dismiss-alert
// POST /session/{session id}/alert/dismiss
Web::WebDriver::Response Client::dismiss_alert(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/alert/dismiss");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().dismiss_alert();
}

// 16.2 Accept Alert, https://w3c.github.io/webdriver/#accept-alert
// POST /session/{session id}/alert/accept
Web::WebDriver::Response Client::accept_alert(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/alert/accept");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().accept_alert();
}

// 16.3 Get Alert Text, https://w3c.github.io/webdriver/#get-alert-text
// GET /session/{session id}/alert/text
Web::WebDriver::Response Client::get_alert_text(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/alert/text");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().get_alert_text();
}

// 16.4 Send Alert Text, https://w3c.github.io/webdriver/#send-alert-text
// POST /session/{session id}/alert/text
Web::WebDriver::Response Client::send_alert_text(Web::WebDriver::Parameters parameters, JsonValue payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling POST /session/<session_id>/alert/text");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().send_alert_text(payload);
}

// 17.1 Take Screenshot, https://w3c.github.io/webdriver/#take-screenshot
// GET /session/{session id}/screenshot
Web::WebDriver::Response Client::take_screenshot(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/screenshot");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().take_screenshot();
}

// 17.2 Take Element Screenshot, https://w3c.github.io/webdriver/#dfn-take-element-screenshot
// GET /session/{session id}/element/{element id}/screenshot
Web::WebDriver::Response Client::take_element_screenshot(Web::WebDriver::Parameters parameters, JsonValue)
{
    dbgln_if(WEBDRIVER_DEBUG, "Handling GET /session/<session_id>/element/<element_id>/screenshot");
    auto* session = TRY(find_session_with_id(parameters[0]));
    return session->web_content_connection().take_element_screenshot(parameters[1]);
}

}
