/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"
#include "BrowserConnection.h"
#include "Client.h"
#include <AK/Time.h>
#include <LibCore/LocalServer.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <unistd.h>

namespace WebDriver {

Session::Session(unsigned session_id, NonnullRefPtr<Client> client)
    : m_client(move(client))
    , m_id(session_id)
{
}

Session::~Session()
{
    if (m_started) {
        auto error = stop();
        if (error.is_error()) {
            warnln("Failed to stop session {}: {}", m_id, error.error());
        }
    }
}

ErrorOr<void> Session::start()
{
    auto socket_path = String::formatted("/tmp/browser_webdriver_{}_{}", getpid(), m_id);
    dbgln("Listening for WebDriver connection on {}", socket_path);

    // FIXME: Use Core::LocalServer
    struct sockaddr_un addr;
    int listen_socket = TRY(Core::System::socket(AF_UNIX, SOCK_STREAM, 0));
    ::memset(&addr, 0, sizeof(struct sockaddr_un));
    addr.sun_family = AF_UNIX;
    ::strncpy(addr.sun_path, socket_path.characters(), sizeof(addr.sun_path) - 1);

    TRY(Core::System::bind(listen_socket, (const struct sockaddr*)&addr, sizeof(struct sockaddr_un)));
    TRY(Core::System::listen(listen_socket, 1));

    char const* argv[] = { "/bin/Browser", "--webdriver", socket_path.characters(), nullptr };
    TRY(Core::System::posix_spawn("/bin/Browser"sv, nullptr, nullptr, const_cast<char**>(argv), environ));

    int data_socket = TRY(Core::System::accept(listen_socket, nullptr, nullptr));
    auto socket = TRY(Core::Stream::LocalSocket::adopt_fd(data_socket));
    TRY(socket->set_blocking(true));
    m_browser_connection = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) BrowserConnection(move(socket), m_client, session_id())));
    dbgln("Browser is connected");

    m_started = true;
    m_windows.set("main", make<Session::Window>("main", true));
    m_current_window_handle = "main";

    return {};
}

ErrorOr<void> Session::stop()
{
    m_browser_connection->async_quit();
    return {};
}

// 9.1 Get Timeouts, https://w3c.github.io/webdriver/#dfn-get-timeouts
JsonObject Session::get_timeouts()
{
    // 1. Let timeouts be the timeouts object for session’s timeouts configuration
    auto timeouts = timeouts_object(m_timeouts_configuration);

    // 2. Return success with data timeouts.
    return timeouts;
}

// 9.2 Set Timeouts, https://w3c.github.io/webdriver/#dfn-set-timeouts
ErrorOr<JsonValue, HttpError> Session::set_timeouts(JsonValue const& payload)
{
    // 1. Let timeouts be the result of trying to JSON deserialize as a timeouts configuration the request’s parameters.
    auto timeouts = TRY(json_deserialize_as_a_timeouts_configuration(payload));

    // 2. Make the session timeouts the new timeouts.
    m_timeouts_configuration = move(timeouts);

    // 3. Return success with data null.
    return JsonValue {};
}

// 10.1 Navigate To, https://w3c.github.io/webdriver/#dfn-navigate-to
ErrorOr<JsonValue, HttpError> Session::navigate_to(JsonValue const& payload)
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME 2. Handle any user prompts and return its value if it is an error.

    // 3. If the url property is missing from the parameters argument or it is not a string, return error with error code invalid argument.
    if (!payload.is_object() || !payload.as_object().has_string("url"sv)) {
        return HttpError { 400, "invalid argument", "Payload doesn't have a string url" };
    }

    // 4. Let url be the result of getting a property named url from the parameters argument.
    URL url(payload.as_object().get_ptr("url"sv)->as_string());

    // FIXME: 5. If url is not an absolute URL or an absolute URL with fragment, return error with error code invalid argument. [URL]

    // 6. Let url be the result of getting a property named url from the parameters argument.
    // Duplicate step?

    // 7. Navigate the current top-level browsing context to url.
    m_browser_connection->async_set_url(url);

    // FIXME: 8. Run the post-navigation checks and return its value if it is an error.

    // FIXME: 9. Wait for navigation to complete and return its value if it is an error.

    // FIXME: 10. Set the current browsing context to the current top-level browsing context.

    // 11. Return success with data null.
    return JsonValue();
}

// 10.2 Get Current URL, https://w3c.github.io/webdriver/#dfn-get-current-url
ErrorOr<JsonValue, HttpError> Session::get_current_url()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let url be the serialization of the current top-level browsing context’s active document’s document URL.
    auto url = m_browser_connection->get_url().to_string();

    // 4. Return success with data url.
    return JsonValue(url);
}

// 10.3 Back, https://w3c.github.io/webdriver/#dfn-back
ErrorOr<JsonValue, HttpError> Session::back()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Traverse the history by a delta –1 for the current browsing context.
    m_browser_connection->async_back();

    // FIXME: 4. If the previous step completed results in a pageHide event firing, wait until pageShow event
    //           fires or for the session page load timeout milliseconds to pass, whichever occurs sooner.

    // FIXME: 5. If the previous step completed by the session page load timeout being reached, and user
    //           prompts have been handled, return error with error code timeout.

    // 6. Return success with data null.
    return JsonValue();
}

// 10.4 Forward, https://w3c.github.io/webdriver/#dfn-forward
ErrorOr<JsonValue, HttpError> Session::forward()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Traverse the history by a delta 1 for the current browsing context.
    m_browser_connection->async_forward();

    // FIXME: 4. If the previous step completed results in a pageHide event firing, wait until pageShow event
    //           fires or for the session page load timeout milliseconds to pass, whichever occurs sooner.

    // FIXME: 5. If the previous step completed by the session page load timeout being reached, and user
    //           prompts have been handled, return error with error code timeout.

    // 6. Return success with data null.
    return JsonValue();
}

// 10.5 Refresh, https://w3c.github.io/webdriver/#dfn-refresh
ErrorOr<JsonValue, HttpError> Session::refresh()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Initiate an overridden reload of the current top-level browsing context’s active document.
    m_browser_connection->async_refresh();

    // FIXME: 4. If url is special except for file:

    // FIXME:     1. Try to wait for navigation to complete.

    // FIXME:     2. Try to run the post-navigation checks.

    // FIXME: 5. Set the current browsing context with current top-level browsing context.

    // 6. Return success with data null.
    return JsonValue();
}

// 10.6 Get Title, https://w3c.github.io/webdriver/#dfn-get-title
ErrorOr<JsonValue, HttpError> Session::get_title()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let title be the initial value of the title IDL attribute of the current top-level browsing context's active document.
    // 4. Return success with data title.
    return JsonValue(m_browser_connection->get_title());
}

// 11.1 Get Window Handle, https://w3c.github.io/webdriver/#get-window-handle
ErrorOr<JsonValue, HttpError> Session::get_window_handle()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // 2. Return success with data being the window handle associated with the current top-level browsing context.
    return m_current_window_handle;
}

// 11.2 Close Window, https://w3c.github.io/webdriver/#dfn-close-window
ErrorOr<void, Variant<HttpError, Error>> Session::close_window()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return Variant<HttpError, Error>(HttpError { 404, "no such window", "Window not found" });

    // 2. Close the current top-level browsing context.
    m_windows.remove(m_current_window_handle);

    // 3. If there are no more open top-level browsing contexts, then close the session.
    if (m_windows.is_empty()) {
        auto result = stop();
        if (result.is_error()) {
            return Variant<HttpError, Error>(result.release_error());
        }
    }

    return {};
}

// 11.4 Get Window Handles, https://w3c.github.io/webdriver/#dfn-get-window-handles
ErrorOr<JsonValue, HttpError> Session::get_window_handles() const
{
    // 1. Let handles be a JSON List.
    auto handles = JsonArray {};

    // 2. For each top-level browsing context in the remote end, push the associated window handle onto handles.
    for (auto const& window_handle : m_windows.keys())
        handles.append(window_handle);

    // 3. Return success with data handles.
    return handles;
}

// https://w3c.github.io/webdriver/#dfn-get-or-create-a-web-element-reference
static String get_or_create_a_web_element_reference(Session::LocalElement const& element)
{
    // FIXME: 1. For each known element of the current browsing context’s list of known elements:
    // FIXME:     1. If known element equals element, return success with known element’s web element reference.
    // FIXME: 2. Add element to the list of known elements of the current browsing context.
    // FIXME: 3. Return success with the element’s web element reference.

    return String::formatted("{}", element.id);
}

// https://w3c.github.io/webdriver/#dfn-web-element-identifier
static const String web_element_identifier = "element-6066-11e4-a52e-4f735466cecf";

// https://w3c.github.io/webdriver/#dfn-web-element-reference-object
static JsonObject web_element_reference_object(Session::LocalElement const& element)
{
    // 1. Let identifier be the web element identifier.
    auto identifier = web_element_identifier;
    // 2. Let reference be the result of get or create a web element reference given element.
    auto reference = get_or_create_a_web_element_reference(element);
    // 3. Return a JSON Object initialized with a property with name identifier and value reference.
    JsonObject object;
    object.set("name"sv, identifier);
    object.set("value"sv, reference);
    return object;
}

// https://w3c.github.io/webdriver/#dfn-find
ErrorOr<JsonArray, HttpError> Session::find(Session::LocalElement const& start_node, StringView const& using_, StringView const& value)
{
    // 1. Let end time be the current time plus the session implicit wait timeout.
    auto end_time = Time::now_monotonic() + Time::from_milliseconds(static_cast<i64>(m_timeouts_configuration.implicit_wait_timeout));

    // 2. Let location strategy be equal to using.
    auto location_strategy = using_;

    // 3. Let selector be equal to value.
    auto selector = value;

    // 4. Let elements returned be the result of trying to call the relevant element location strategy with arguments start node, and selector.
    auto location_strategy_handler = s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; });
    if (!location_strategy_handler.has_value())
        return HttpError { 400, "invalid argument", "No valid location strategy" };

    auto elements_or_error = (this->*location_strategy_handler.value().handler)(start_node, selector);

    // 5. If a DOMException, SyntaxError, XPathException, or other error occurs during the execution of the element location strategy, return error invalid selector.
    if (elements_or_error.is_error())
        return HttpError { 400, "invalid selector", String::formatted("The location strategy could not finish: {}", elements_or_error.release_error().message) };

    auto elements = elements_or_error.release_value();

    // FIXME: 6. If elements returned is empty and the current time is less than end time return to step 4. Otherwise, continue to the next step.
    (void)end_time;

    // 7. Let result be an empty JSON List.
    auto result = JsonArray();

    // 8. For each element in elements returned, append the web element reference object for element, to result.
    for (auto const& element : elements) {
        result.append(JsonValue(web_element_reference_object(element)));
    }

    // 9. Return success with data result.
    return result;
}

// https://w3c.github.io/webdriver/#dfn-table-of-location-strategies
Vector<Session::LocatorStrategy> Session::s_locator_strategies = {
    { "css selector", &Session::locator_strategy_css_selectors },
    { "link text", &Session::locator_strategy_link_text },
    { "partial link text", &Session::locator_strategy_partial_link_text },
    { "tag name", &Session::locator_strategy_tag_name },
    { "xpath", &Session::locator_strategy_x_path },
};

// https://w3c.github.io/webdriver/#css-selectors
ErrorOr<Vector<Session::LocalElement>, HttpError> Session::locator_strategy_css_selectors(Session::LocalElement const& start_node, StringView const& selector)
{
    // 1. Let elements be the result of calling querySelectorAll() with start node as this and selector as the argument.
    //    If this causes an exception to be thrown, return error with error code invalid selector.
    auto elements_ids = m_browser_connection->query_selector_all(start_node.id, selector);

    if (!elements_ids.has_value())
        return HttpError { 400, "invalid selector", "query_selector_all returned failed!" };

    Vector<Session::LocalElement> elements;
    for (auto id : elements_ids.release_value()) {
        elements.append({ id });
    }

    // 2.Return success with data elements.
    return elements;
}

// https://w3c.github.io/webdriver/#link-text
ErrorOr<Vector<Session::LocalElement>, HttpError> Session::locator_strategy_link_text(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return HttpError { 501, "not implemented", "locator strategy link text" };
}

// https://w3c.github.io/webdriver/#partial-link-text
ErrorOr<Vector<Session::LocalElement>, HttpError> Session::locator_strategy_partial_link_text(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return HttpError { 501, "not implemented", "locator strategy partial link text" };
}

// https://w3c.github.io/webdriver/#tag-name
ErrorOr<Vector<Session::LocalElement>, HttpError> Session::locator_strategy_tag_name(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return HttpError { 501, "not implemented", "locator strategy tag name" };
}

// https://w3c.github.io/webdriver/#xpath
ErrorOr<Vector<Session::LocalElement>, HttpError> Session::locator_strategy_x_path(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return HttpError { 501, "not implemented", "locator strategy XPath" };
}

// 12.3.2 Find Element, https://w3c.github.io/webdriver/#dfn-find-element
ErrorOr<JsonValue, HttpError> Session::find_element(JsonValue const& payload)
{
    if (!payload.is_object())
        return HttpError { 400, "invalid argument", "Payload is not a JSON object" };

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return HttpError { 400, "invalid argument", "No property called 'using' present" };
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return HttpError { 400, "invalid argument", "Property 'using' is not a String" };

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return HttpError { 400, "invalid argument", "No valid location strategy" };

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return HttpError { 400, "invalid argument", "No property called 'value' present" };
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return HttpError { 400, "invalid argument", "Property 'value' is not a String" };

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the current browsing context’s document element.
    auto maybe_start_node_id = m_browser_connection->get_document_element();

    // 8. If start node is null, return error with error code no such element.
    if (!maybe_start_node_id.has_value())
        return HttpError { 404, "no such element", "document element does not exist" };

    auto start_node_id = maybe_start_node_id.release_value();
    LocalElement start_node = { start_node_id };

    // 9. Let result be the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));

    // 10. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return HttpError { 404, "no such element", "the requested element does not exist" };

    return JsonValue(result.at(0));
}

// 12.3.3 Find Elements, https://w3c.github.io/webdriver/#dfn-find-elements
ErrorOr<JsonValue, HttpError> Session::find_elements(JsonValue const& payload)
{
    if (!payload.is_object())
        return HttpError { 400, "invalid argument", "Payload is not a JSON object" };

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return HttpError { 400, "invalid argument", "No property called 'using' present" };
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return HttpError { 400, "invalid argument", "Property 'using' is not a String" };

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return HttpError { 400, "invalid argument", "No valid location strategy" };

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return HttpError { 400, "invalid argument", "No property called 'value' present" };
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return HttpError { 400, "invalid argument", "Property 'value' is not a String" };

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the current browsing context’s document element.
    auto maybe_start_node_id = m_browser_connection->get_document_element();

    // 8. If start node is null, return error with error code no such element.
    if (!maybe_start_node_id.has_value())
        return HttpError { 404, "no such element", "document element does not exist" };

    auto start_node_id = maybe_start_node_id.release_value();
    LocalElement start_node = { start_node_id };

    // 9. Return the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));
    return JsonValue(result);
}

// 12.3.4 Find Element From Element, https://w3c.github.io/webdriver/#dfn-find-element-from-element
ErrorOr<JsonValue, HttpError> Session::find_element_from_element(JsonValue const& payload, StringView parameter_element_id)
{
    if (!payload.is_object())
        return HttpError { 400, "invalid argument", "Payload is not a JSON object" };

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return HttpError { 400, "invalid argument", "No property called 'using' present" };
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return HttpError { 400, "invalid argument", "Property 'using' is not a String" };

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return HttpError { 400, "invalid argument", "No valid location strategy" };

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return HttpError { 400, "invalid argument", "No property called 'value' present" };
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return HttpError { 400, "invalid argument", "Property 'value' is not a String" };

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // FIXME: 7. Let start node be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID

    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return HttpError { 400, "invalid argument", "Element ID is not an i32" };

    auto element_id = maybe_element_id.release_value();
    LocalElement start_node = { element_id };

    // 8. Let result be the value of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));

    // 9. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return HttpError { 404, "no such element", "the requested element does not exist" };

    return JsonValue(result.at(0));
}

// 12.3.5 Find Elements From Element, https://w3c.github.io/webdriver/#dfn-find-elements-from-element
ErrorOr<JsonValue, HttpError> Session::find_elements_from_element(JsonValue const& payload, StringView parameter_element_id)
{
    if (!payload.is_object())
        return HttpError { 400, "invalid argument", "Payload is not a JSON object" };

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return HttpError { 400, "invalid argument", "No property called 'using' present" };
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return HttpError { 400, "invalid argument", "Property 'using' is not a String" };

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return HttpError { 400, "invalid argument", "No valid location strategy" };

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return HttpError { 400, "invalid argument", "No property called 'value' present" };
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return HttpError { 400, "invalid argument", "Property 'value' is not a String" };

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // FIXME: 7. Let start node be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID

    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return HttpError { 400, "invalid argument", "Element ID is not an i32" };

    auto element_id = maybe_element_id.release_value();
    LocalElement start_node = { element_id };

    // 8. Return the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));
    return JsonValue(result);
}

// 12.4.2 Get Element Attribute, https://w3c.github.io/webdriver/#dfn-get-element-attribute
ErrorOr<JsonValue, HttpError> Session::get_element_attribute(JsonValue const&, StringView parameter_element_id, StringView name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // FIXME: 3. Let element be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID
    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return HttpError { 400, "invalid argument", "Element ID is not an i32" };

    auto element_id = maybe_element_id.release_value();

    // FIXME: The case that the element does not exist is not handled at all and null is returned in that case.

    // 4. Let result be the result of the first matching condition:
    // -> FIXME: If name is a boolean attribute
    //    NOTE: LibWeb doesn't know about boolean attributes directly
    //    "true" (string) if the element has the attribute, otherwise null.
    // -> Otherwise
    //    The result of getting an attribute by name name.
    auto result = m_browser_connection->get_element_attribute(element_id, name);

    if (!result.has_value())
        return JsonValue(AK::JsonValue::Type::Null);

    // 5. Return success with data result.
    return JsonValue(result.release_value());
}

// 12.4.3 Get Element Property, https://w3c.github.io/webdriver/#dfn-get-element-property
ErrorOr<JsonValue, HttpError> Session::get_element_property(JsonValue const&, StringView parameter_element_id, StringView name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // FIXME: 3. Let element be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID
    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return HttpError { 400, "invalid argument", "Element ID is not an i32" };

    auto element_id = maybe_element_id.release_value();

    // 4. Let property be the result of calling the Object.[[GetProperty]](name) on element.
    auto property = m_browser_connection->get_element_property(element_id, name);

    // 5. Let result be the value of property if not undefined, or null.
    if (!property.has_value())
        return JsonValue();

    // 6. Return success with data result.
    return JsonValue(property.release_value());
}

// https://w3c.github.io/webdriver/#dfn-serialized-cookie
static JsonObject serialize_cookie(Web::Cookie::Cookie const& cookie)
{
    JsonObject serialized_cookie = {};
    serialized_cookie.set("name", cookie.name);
    serialized_cookie.set("value", cookie.value);
    serialized_cookie.set("path", cookie.path);
    serialized_cookie.set("domain", cookie.domain);
    serialized_cookie.set("secure", cookie.secure);
    serialized_cookie.set("httpOnly", cookie.http_only);
    serialized_cookie.set("expiry", cookie.expiry_time.timestamp());
    // FIXME: Add sameSite to Cookie and serialize it here too.

    return serialized_cookie;
}

// 14.1 Get All Cookies, https://w3c.github.io/webdriver/#dfn-get-all-cookies
ErrorOr<JsonValue, HttpError> Session::get_all_cookies()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts, and return its value if it is an error.

    // 3. Let cookies be a new JSON List.
    JsonArray cookies = {};

    // 4. For each cookie in all associated cookies of the current browsing context’s active document:
    for (auto const& cookie : m_browser_connection->get_all_cookies()) {
        // 1. Let serialized cookie be the result of serializing cookie.
        auto serialized_cookie = serialize_cookie(cookie);

        // 2. Append serialized cookie to cookies
        cookies.append(serialized_cookie);
    }

    // 5. Return success with data cookies.
    return JsonValue(cookies);
}

// 14.2 Get Named Cookie, https://w3c.github.io/webdriver/#dfn-get-named-cookie
ErrorOr<JsonValue, HttpError> Session::get_named_cookie(String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts, and return its value if it is an error.

    // 3. If the url variable name is equal to a cookie’s cookie name amongst all associated cookies of the
    //    current browsing context’s active document, return success with the serialized cookie as data.
    auto maybe_cookie = m_browser_connection->get_named_cookie(name);
    if (maybe_cookie.has_value()) {
        auto cookie = maybe_cookie.release_value();
        auto serialized_cookie = serialize_cookie(cookie);
        return JsonValue(serialized_cookie);
    }

    // 4. Otherwise, return error with error code no such cookie.
    return HttpError { 404, "no such cookie", "Cookie not found" };
}

// 14.3 Add Cookie, https://w3c.github.io/webdriver/#dfn-adding-a-cookie
ErrorOr<JsonValue, HttpError> Session::add_cookie(JsonValue const& payload)
{
    // 1. Let data be the result of getting a property named cookie from the parameters argument.
    if (!payload.is_object() || !payload.as_object().has_object("cookie"sv))
        return HttpError { 400, "invalid argument", "Payload doesn't have a cookie object" };

    auto const& maybe_data = payload.as_object().get("cookie"sv);

    // 2. If data is not a JSON Object with all the required (non-optional) JSON keys listed in the table for cookie conversion,
    //    return error with error code invalid argument.
    // NOTE: Table is here: https://w3c.github.io/webdriver/#dfn-table-for-cookie-conversion
    if (!maybe_data.is_object())
        return HttpError { 400, "invalid argument", "Value \"cookie\' is not an object" };

    auto const& data = maybe_data.as_object();

    if (!data.has("name"sv) || !data.has("value"sv))
        return HttpError { 400, "invalid argument", "Cookie-Object doesn't contain all required keys" };

    // 3. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 4. Handle any user prompts, and return its value if it is an error.

    // FIXME: 5. If the current browsing context’s document element is a cookie-averse Document object,
    //           return error with error code invalid cookie domain.

    // 6. If cookie name or cookie value is null,
    //    FIXME: cookie domain is not equal to the current browsing context’s active document’s domain,
    //    cookie secure only or cookie HTTP only are not boolean types,
    //    or cookie expiry time is not an integer type, or it less than 0 or greater than the maximum safe integer,
    //    return error with error code invalid argument.
    if (data.get("name"sv).is_null() || data.get("value"sv).is_null())
        return HttpError { 400, "invalid argument", "Cookie-Object is malformed: name or value are null" };
    if (data.has("secure"sv) && !data.get("secure"sv).is_bool())
        return HttpError { 400, "invalid argument", "Cookie-Object is malformed: secure is not bool" };
    if (data.has("httpOnly"sv) && !data.get("httpOnly"sv).is_bool())
        return HttpError { 400, "invalid argument", "Cookie-Object is malformed: httpOnly is not bool" };
    Optional<Core::DateTime> expiry_time;
    if (data.has("expiry"sv)) {
        auto expiry_argument = data.get("expiry"sv);
        if (!expiry_argument.is_u32()) {
            // NOTE: less than 0 or greater than safe integer are handled by the JSON parser
            return HttpError { 400, "invalid argument", "Cookie-Object is malformed: expiry is not u32" };
        }
        expiry_time = Core::DateTime::from_timestamp(expiry_argument.as_u32());
    }

    // 7. Create a cookie in the cookie store associated with the active document’s address using
    //    cookie name name, cookie value value, and an attribute-value list of the following cookie concepts
    //    listed in the table for cookie conversion from data:
    Web::Cookie::ParsedCookie cookie;
    if (auto name_attribute = data.get("name"sv); name_attribute.is_string())
        cookie.name = name_attribute.as_string();
    else
        return HttpError { 400, "invalid argument", "Expect name attribute to be string" };

    if (auto value_attribute = data.get("value"sv); value_attribute.is_string())
        cookie.value = value_attribute.as_string();
    else
        return HttpError { 400, "invalid argument", "Expect value attribute to be string" };

    // Cookie path
    //     The value if the entry exists, otherwise "/".
    if (data.has("path"sv)) {
        if (auto path_attribute = data.get("path"sv); path_attribute.is_string())
            cookie.path = path_attribute.as_string();
        else
            return HttpError { 400, "invalid argument", "Expect path attribute to be string" };
    } else {
        cookie.path = "/";
    }

    // Cookie domain
    //     The value if the entry exists, otherwise the current browsing context’s active document’s URL domain.
    // NOTE: The otherwise case is handled by the CookieJar
    if (data.has("domain"sv)) {
        if (auto domain_attribute = data.get("domain"sv); domain_attribute.is_string())
            cookie.domain = domain_attribute.as_string();
        else
            return HttpError { 400, "invalid argument", "Expect domain attribute to be string" };
    }

    // Cookie secure only
    //     The value if the entry exists, otherwise false.
    if (data.has("secure"sv)) {
        cookie.secure_attribute_present = data.get("secure"sv).as_bool();
    } else {
        cookie.secure_attribute_present = false;
    }

    // Cookie HTTP only
    //     The value if the entry exists, otherwise false.
    if (data.has("httpOnly"sv)) {
        cookie.http_only_attribute_present = data.get("httpOnly"sv).as_bool();
    } else {
        cookie.http_only_attribute_present = false;
    }

    // Cookie expiry time
    //     The value if the entry exists, otherwise leave unset to indicate that this is a session cookie.
    cookie.expiry_time_from_expires_attribute = expiry_time;

    // FIXME: Cookie same site
    //            The value if the entry exists, otherwise leave unset to indicate that no same site policy is defined.

    m_browser_connection->async_add_cookie(move(cookie));

    // If there is an error during this step, return error with error code unable to set cookie.
    // NOTE: This probably should only apply to the actual setting of the cookie in the Browser,
    //       which cannot fail in our case.
    //       Thus, the error-codes used above are 400 "invalid argument".

    // 8. Return success with data null.
    return JsonValue();
}

// https://w3c.github.io/webdriver/#dfn-delete-cookies
void Session::delete_cookies(Optional<StringView> const& name)
{
    // For each cookie among all associated cookies of the current browsing context’s active document,
    // run the substeps of the first matching condition:
    for (auto& cookie : m_browser_connection->get_all_cookies()) {
        // -> name is undefined
        // -> name is equal to cookie name
        if (!name.has_value() || name.value() == cookie.name) {
            // Set the cookie expiry time to a Unix timestamp in the past.
            cookie.expiry_time = Core::DateTime::from_timestamp(0);
            m_browser_connection->async_update_cookie(cookie);
        }
        // -> Otherwise
        //    Do nothing.
    }
}

// 14.4 Delete Cookie, https://w3c.github.io/webdriver/#dfn-delete-cookie
ErrorOr<JsonValue, HttpError> Session::delete_cookie(StringView const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts, and return its value if it is an error.

    // 3. Delete cookies using the url variable name parameter as the filter argument.
    delete_cookies(name);

    // 4. Return success with data null.
    return JsonValue();
}

// 14.5 Delete All Cookies, https://w3c.github.io/webdriver/#dfn-delete-all-cookies
ErrorOr<JsonValue, HttpError> Session::delete_all_cookies()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    auto current_window = this->current_window();
    if (!current_window.has_value())
        return HttpError { 404, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts, and return its value if it is an error.

    // 3. Delete cookies, giving no filtering argument.
    delete_cookies();

    // 4. Return success with data null.
    return JsonValue();
}

}
