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
#include <AK/Base64.h>
#include <AK/NumericLimits.h>
#include <AK/Time.h>
#include <AK/URL.h>
#include <LibCore/LocalServer.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibGfx/PNGWriter.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/WebDriver/ExecuteScript.h>
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

ErrorOr<Session::Window*, WebDriverError> Session::current_window()
{
    auto window = m_windows.get(m_current_window_handle);
    if (!window.has_value())
        return WebDriverError::from_code(ErrorCode::NoSuchWindow, "Window not found");
    return window.release_value();
}

ErrorOr<void, WebDriverError> Session::check_for_open_top_level_browsing_context_or_return_error()
{
    (void)TRY(current_window());
    return {};
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
ErrorOr<JsonValue, WebDriverError> Session::set_timeouts(JsonValue const& payload)
{
    // 1. Let timeouts be the result of trying to JSON deserialize as a timeouts configuration the request’s parameters.
    auto timeouts = TRY(json_deserialize_as_a_timeouts_configuration(payload));

    // 2. Make the session timeouts the new timeouts.
    m_timeouts_configuration = move(timeouts);

    // 3. Return success with data null.
    return JsonValue {};
}

// 10.1 Navigate To, https://w3c.github.io/webdriver/#dfn-navigate-to
ErrorOr<JsonValue, WebDriverError> Session::navigate_to(JsonValue const& payload)
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME 2. Handle any user prompts and return its value if it is an error.

    // 3. If the url property is missing from the parameters argument or it is not a string, return error with error code invalid argument.
    if (!payload.is_object() || !payload.as_object().has_string("url"sv)) {
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload doesn't have a string url");
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
ErrorOr<JsonValue, WebDriverError> Session::get_current_url()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let url be the serialization of the current top-level browsing context’s active document’s document URL.
    auto url = m_browser_connection->get_url().to_string();

    // 4. Return success with data url.
    return JsonValue(url);
}

// 10.3 Back, https://w3c.github.io/webdriver/#dfn-back
ErrorOr<JsonValue, WebDriverError> Session::back()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

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
ErrorOr<JsonValue, WebDriverError> Session::forward()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

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
ErrorOr<JsonValue, WebDriverError> Session::refresh()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

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
ErrorOr<JsonValue, WebDriverError> Session::get_title()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let title be the initial value of the title IDL attribute of the current top-level browsing context's active document.
    // 4. Return success with data title.
    return JsonValue(m_browser_connection->get_title());
}

// 11.1 Get Window Handle, https://w3c.github.io/webdriver/#get-window-handle
ErrorOr<JsonValue, WebDriverError> Session::get_window_handle()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // 2. Return success with data being the window handle associated with the current top-level browsing context.
    return m_current_window_handle;
}

// 11.2 Close Window, https://w3c.github.io/webdriver/#dfn-close-window
ErrorOr<void, Variant<WebDriverError, Error>> Session::close_window()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // 2. Close the current top-level browsing context.
    m_windows.remove(m_current_window_handle);

    // 3. If there are no more open top-level browsing contexts, then close the session.
    if (m_windows.is_empty()) {
        auto result = stop();
        if (result.is_error()) {
            return Variant<WebDriverError, Error>(result.release_error());
        }
    }

    return {};
}

// 11.4 Get Window Handles, https://w3c.github.io/webdriver/#dfn-get-window-handles
ErrorOr<JsonValue, WebDriverError> Session::get_window_handles() const
{
    // 1. Let handles be a JSON List.
    auto handles = JsonArray {};

    // 2. For each top-level browsing context in the remote end, push the associated window handle onto handles.
    for (auto const& window_handle : m_windows.keys())
        handles.append(window_handle);

    // 3. Return success with data handles.
    return handles;
}

static JsonObject serialize_window_rect(Gfx::IntRect const& rect)
{
    JsonObject serialized_rect = {};
    serialized_rect.set("x", rect.x());
    serialized_rect.set("y", rect.y());
    serialized_rect.set("width", rect.width());
    serialized_rect.set("height", rect.height());

    return serialized_rect;
}

// 11.8.1 Get Window Rect, https://w3c.github.io/webdriver/#dfn-get-window-rect
ErrorOr<JsonValue, WebDriverError> Session::get_window_rect()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_window_rect(m_browser_connection->get_window_rect());
}

// 11.8.2 Set Window Rect, https://w3c.github.io/webdriver/#dfn-set-window-rect
ErrorOr<JsonValue, WebDriverError> Session::set_window_rect(JsonValue const& payload)
{
    if (!payload.is_object())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();

    auto resolve_property = [](auto name, auto const* property, auto min, auto max) -> ErrorOr<Optional<i32>, WebDriverError> {
        if (!property)
            return Optional<i32> {};
        if (!property->is_number())
            return WebDriverError::from_code(ErrorCode::InvalidArgument, String::formatted("Property '{}' is not a Number", name));

        auto number = property->template to_number<i64>();

        if (number < min)
            return WebDriverError::from_code(ErrorCode::InvalidArgument, String::formatted("Property '{}' value {} exceeds the minimum allowed value {}", name, number, min));
        if (number > max)
            return WebDriverError::from_code(ErrorCode::InvalidArgument, String::formatted("Property '{}' value {} exceeds the maximum allowed value {}", name, number, max));

        return static_cast<i32>(number);
    };

    // 1. Let width be the result of getting a property named width from the parameters argument, else let it be null.
    auto const* width_property = properties.get_ptr("width"sv);

    // 2. Let height be the result of getting a property named height from the parameters argument, else let it be null.
    auto const* height_property = properties.get_ptr("height"sv);

    // 3. Let x be the result of getting a property named x from the parameters argument, else let it be null.
    auto const* x_property = properties.get_ptr("x"sv);

    // 4. Let y be the result of getting a property named y from the parameters argument, else let it be null.
    auto const* y_property = properties.get_ptr("y"sv);

    // 5. If width or height is neither null nor a Number from 0 to 2^31 − 1, return error with error code invalid argument.
    auto width = TRY(resolve_property("width"sv, width_property, 0, NumericLimits<i32>::max()));
    auto height = TRY(resolve_property("height"sv, height_property, 0, NumericLimits<i32>::max()));

    // 6. If x or y is neither null nor a Number from −(2^31) to 2^31 − 1, return error with error code invalid argument.
    auto x = TRY(resolve_property("x"sv, x_property, NumericLimits<i32>::min(), NumericLimits<i32>::max()));
    auto y = TRY(resolve_property("y"sv, y_property, NumericLimits<i32>::min(), NumericLimits<i32>::max()));

    // 7. If the remote end does not support the Set Window Rect command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 8. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 9. Handle any user prompts and return its value if it is an error.
    // FIXME: 10. Fully exit fullscreen.

    // 11. Restore the window.
    m_browser_connection->async_restore_window();

    // 11. If width and height are not null:
    if (width.has_value() && height.has_value()) {
        // a. Set the width, in CSS pixels, of the operating system window containing the current top-level browsing context, including any browser chrome and externally drawn window decorations to a value that is as close as possible to width.
        // b. Set the height, in CSS pixels, of the operating system window containing the current top-level browsing context, including any browser chrome and externally drawn window decorations to a value that is as close as possible to height.
        m_browser_connection->async_set_window_size(Gfx::IntSize { *width, *height });
    }

    // 12. If x and y are not null:
    if (x.has_value() && y.has_value()) {
        // a. Run the implementation-specific steps to set the position of the operating system level window containing the current top-level browsing context to the position given by the x and y coordinates.
        m_browser_connection->async_set_window_position(Gfx::IntPoint { *x, *y });
    }

    // 14. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_window_rect(m_browser_connection->get_window_rect());
}

// 11.8.3 Maximize Window, https://w3c.github.io/webdriver/#dfn-maximize-window
ErrorOr<JsonValue, WebDriverError> Session::maximize_window()
{
    // 1. If the remote end does not support the Maximize Window command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 3. Handle any user prompts and return its value if it is an error.
    // FIXME: 4. Fully exit fullscreen.

    // 5. Restore the window.
    m_browser_connection->async_restore_window();

    // 6. Maximize the window of the current top-level browsing context.
    m_browser_connection->async_maximize_window();

    // 7. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_window_rect(m_browser_connection->get_window_rect());
}

// 11.8.4 Minimize Window, https://w3c.github.io/webdriver/#minimize-window
ErrorOr<JsonValue, WebDriverError> Session::minimize_window()
{
    // 1. If the remote end does not support the Minimize Window command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 3. Handle any user prompts and return its value if it is an error.
    // FIXME: 4. Fully exit fullscreen.

    // 5. Iconify the window.
    m_browser_connection->async_minimize_window();

    // 6. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_window_rect(m_browser_connection->get_window_rect());
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
ErrorOr<JsonArray, WebDriverError> Session::find(Session::LocalElement const& start_node, StringView const& using_, StringView const& value)
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
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No valid location strategy");

    auto elements_or_error = (this->*location_strategy_handler.value().handler)(start_node, selector);

    // 5. If a DOMException, SyntaxError, XPathException, or other error occurs during the execution of the element location strategy, return error invalid selector.
    if (elements_or_error.is_error())
        return WebDriverError::from_code(ErrorCode::InvalidSelector, String::formatted("The location strategy could not finish: {}", elements_or_error.release_error().message));

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
ErrorOr<Vector<Session::LocalElement>, WebDriverError> Session::locator_strategy_css_selectors(Session::LocalElement const& start_node, StringView const& selector)
{
    // 1. Let elements be the result of calling querySelectorAll() with start node as this and selector as the argument.
    //    If this causes an exception to be thrown, return error with error code invalid selector.
    auto elements_ids = m_browser_connection->query_selector_all(start_node.id, selector);

    if (!elements_ids.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidSelector, "query_selector_all returned failed!");

    Vector<Session::LocalElement> elements;
    for (auto id : elements_ids.release_value()) {
        elements.append({ id });
    }

    // 2.Return success with data elements.
    return elements;
}

// https://w3c.github.io/webdriver/#link-text
ErrorOr<Vector<Session::LocalElement>, WebDriverError> Session::locator_strategy_link_text(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return WebDriverError::from_code(ErrorCode::UnsupportedOperation, "Not implemented: locator strategy link text");
}

// https://w3c.github.io/webdriver/#partial-link-text
ErrorOr<Vector<Session::LocalElement>, WebDriverError> Session::locator_strategy_partial_link_text(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return WebDriverError::from_code(ErrorCode::UnsupportedOperation, "Not implemented: locator strategy partial link text");
}

// https://w3c.github.io/webdriver/#tag-name
ErrorOr<Vector<Session::LocalElement>, WebDriverError> Session::locator_strategy_tag_name(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return WebDriverError::from_code(ErrorCode::UnsupportedOperation, "Not implemented: locator strategy tag name");
}

// https://w3c.github.io/webdriver/#xpath
ErrorOr<Vector<Session::LocalElement>, WebDriverError> Session::locator_strategy_x_path(Session::LocalElement const&, StringView const&)
{
    // FIXME: Implement
    return WebDriverError::from_code(ErrorCode::UnsupportedOperation, "Not implemented: locator strategy XPath");
}

// 12.3.2 Find Element, https://w3c.github.io/webdriver/#dfn-find-element
ErrorOr<JsonValue, WebDriverError> Session::find_element(JsonValue const& payload)
{
    if (!payload.is_object())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'using' present");
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'using' is not a String");

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No valid location strategy");

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'value' present");
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'value' is not a String");

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the current browsing context’s document element.
    auto maybe_start_node_id = m_browser_connection->get_document_element();

    // 8. If start node is null, return error with error code no such element.
    if (!maybe_start_node_id.has_value())
        return WebDriverError::from_code(ErrorCode::NoSuchElement, "document element does not exist");

    auto start_node_id = maybe_start_node_id.release_value();
    LocalElement start_node = { start_node_id };

    // 9. Let result be the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));

    // 10. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return WebDriverError::from_code(ErrorCode::NoSuchElement, "The requested element does not exist");

    return JsonValue(result.at(0));
}

// 12.3.3 Find Elements, https://w3c.github.io/webdriver/#dfn-find-elements
ErrorOr<JsonValue, WebDriverError> Session::find_elements(JsonValue const& payload)
{
    if (!payload.is_object())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'using' present");
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'using' is not a String");

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No valid location strategy");

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'value' present");
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'value' is not a String");

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the current browsing context’s document element.
    auto maybe_start_node_id = m_browser_connection->get_document_element();

    // 8. If start node is null, return error with error code no such element.
    if (!maybe_start_node_id.has_value())
        return WebDriverError::from_code(ErrorCode::NoSuchElement, "document element does not exist");

    auto start_node_id = maybe_start_node_id.release_value();
    LocalElement start_node = { start_node_id };

    // 9. Return the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));
    return JsonValue(result);
}

// 12.3.4 Find Element From Element, https://w3c.github.io/webdriver/#dfn-find-element-from-element
ErrorOr<JsonValue, WebDriverError> Session::find_element_from_element(JsonValue const& payload, StringView parameter_element_id)
{
    if (!payload.is_object())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'using' present");
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'using' is not a String");

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No valid location strategy");

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'value' present");
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'value' is not a String");

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // FIXME: 7. Let start node be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID

    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Element ID is not an i32");

    auto element_id = maybe_element_id.release_value();
    LocalElement start_node = { element_id };

    // 8. Let result be the value of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));

    // 9. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return WebDriverError::from_code(ErrorCode::NoSuchElement, "The requested element does not exist");

    return JsonValue(result.at(0));
}

// 12.3.5 Find Elements From Element, https://w3c.github.io/webdriver/#dfn-find-elements-from-element
ErrorOr<JsonValue, WebDriverError> Session::find_elements_from_element(JsonValue const& payload, StringView parameter_element_id)
{
    if (!payload.is_object())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();
    // 1. Let location strategy be the result of getting a property called "using".
    if (!properties.has("using"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'using' present");
    auto const& maybe_location_strategy = properties.get("using"sv);
    if (!maybe_location_strategy.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'using' is not a String");

    auto location_strategy = maybe_location_strategy.to_string();

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!s_locator_strategies.first_matching([&](LocatorStrategy const& match) { return match.name == location_strategy; }).has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No valid location strategy");

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    if (!properties.has("value"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "No property called 'value' present");
    auto const& maybe_selector = properties.get("value"sv);
    if (!maybe_selector.is_string())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Property 'value' is not a String");

    auto selector = maybe_selector.to_string();

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // FIXME: 7. Let start node be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID

    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Element ID is not an i32");

    auto element_id = maybe_element_id.release_value();
    LocalElement start_node = { element_id };

    // 8. Return the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(start_node, location_strategy, selector));
    return JsonValue(result);
}

// 12.4.2 Get Element Attribute, https://w3c.github.io/webdriver/#dfn-get-element-attribute
ErrorOr<JsonValue, WebDriverError> Session::get_element_attribute(JsonValue const&, StringView parameter_element_id, StringView name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // FIXME: 3. Let element be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID
    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Element ID is not an i32");

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
ErrorOr<JsonValue, WebDriverError> Session::get_element_property(JsonValue const&, StringView parameter_element_id, StringView name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // FIXME: 3. Let element be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID
    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Element ID is not an i32");

    auto element_id = maybe_element_id.release_value();

    // 4. Let property be the result of calling the Object.[[GetProperty]](name) on element.
    auto property = m_browser_connection->get_element_property(element_id, name);

    // 5. Let result be the value of property if not undefined, or null.
    if (!property.has_value())
        return JsonValue();

    // 6. Return success with data result.
    return JsonValue(property.release_value());
}

// 12.4.4 Get Element CSS Value, https://w3c.github.io/webdriver/#dfn-get-element-css-value
ErrorOr<JsonValue, WebDriverError> Session::get_element_css_value(JsonValue const&, StringView parameter_element_id, StringView property_name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // FIXME: 3. Let element be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID
    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Element ID is not an i32");

    auto element_id = maybe_element_id.release_value();

    // 4. Let computed value be the result of the first matching condition:
    // -> current browsing context’s active document’s type is not "xml"
    //    computed value of parameter property name from element’s style declarations. property name is obtained from url variables.
    // -> Otherwise
    //    "" (empty string)
    auto active_documents_type = m_browser_connection->get_active_documents_type();
    if (active_documents_type == "xml")
        return JsonValue("");

    auto computed_value = m_browser_connection->get_computed_value_for_element(element_id, property_name);

    // 5. Return success with data computed value.
    return JsonValue(computed_value);
}

// 12.4.5 Get Element Text, https://w3c.github.io/webdriver/#dfn-get-element-text
ErrorOr<JsonValue, WebDriverError> Session::get_element_text(JsonValue const&, StringView parameter_element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // FIXME: 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Element ID is not an i32");

    auto element_id = maybe_element_id.release_value();

    // 4. Let rendered text be the result of performing implementation-specific steps whose result is exactly the
    //    same as the result of a Function.[[Call]](null, element) with bot.dom.getVisibleText as the this value.
    auto rendered_text = m_browser_connection->get_element_text(element_id);

    // 5. Return success with data rendered text.
    return JsonValue(rendered_text);
}

// 12.4.6 Get Element Tag Name, https://w3c.github.io/webdriver/#dfn-get-element-tag-name
ErrorOr<JsonValue, WebDriverError> Session::get_element_tag_name(JsonValue const&, StringView parameter_element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // FIXME: 3. Let element be the result of trying to get a known connected element with url variable element id.
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference()
    //       For now the element is only represented by its ID
    auto maybe_element_id = parameter_element_id.to_int();
    if (!maybe_element_id.has_value())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Element ID is not an i32");

    auto element_id = maybe_element_id.release_value();

    // 4. Let qualified name be the result of getting element’s tagName IDL attribute.
    auto qualified_name = m_browser_connection->get_element_tag_name(element_id);

    // 5. Return success with data qualified name.
    return JsonValue(qualified_name);
}

struct ScriptArguments {
    String script;
    JsonArray const& arguments;
};

// https://w3c.github.io/webdriver/#dfn-extract-the-script-arguments-from-a-request
static ErrorOr<ScriptArguments, WebDriverError> extract_the_script_arguments_from_a_request(JsonValue const& payload)
{
    if (!payload.is_object())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();

    // 1. Let script be the result of getting a property named script from the parameters.
    // 2. If script is not a String, return error with error code invalid argument.
    if (!properties.has_string("script"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload doesn't have a 'script' string property");
    auto script = properties.get("script"sv).as_string();

    // 3. Let args be the result of getting a property named args from the parameters.
    // 4. If args is not an Array return error with error code invalid argument.
    if (!properties.has_array("args"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload doesn't have an 'args' string property");
    auto const& args = properties.get("args"sv).as_array();

    // 5. Let arguments be the result of calling the JSON deserialize algorithm with arguments args.
    // NOTE: We forward the JSON array to the Browser and then WebContent process over IPC, so this is not necessary.

    // 6. Return success with data script and arguments.
    return ScriptArguments { script, args };
}

// 13.2.1 Execute Script, https://w3c.github.io/webdriver/#dfn-execute-script
ErrorOr<JsonValue, WebDriverError> Session::execute_script(JsonValue const& payload)
{
    // 1. Let body and arguments be the result of trying to extract the script arguments from a request with argument parameters.
    auto const& [body, arguments] = TRY(extract_the_script_arguments_from_a_request(payload));

    // 2. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 3. Handle any user prompts, and return its value if it is an error.

    // 4., 5.1-5.3.
    Vector<String> json_arguments;
    arguments.for_each([&](JsonValue const& json_value) {
        // NOTE: serialized() instead of to_string() ensures proper quoting.
        json_arguments.append(json_value.serialized<StringBuilder>());
    });

    dbgln("Executing script with 'args': [{}] / 'body':\n{}", String::join(", "sv, json_arguments), body);
    auto execute_script_response = m_browser_connection->execute_script(body, json_arguments, m_timeouts_configuration.script_timeout, false);
    dbgln("Executing script returned: {}", execute_script_response.json_result());

    // NOTE: This is assumed to be a valid JSON value.
    auto result = MUST(JsonValue::from_string(execute_script_response.json_result()));

    switch (execute_script_response.result_type()) {
    // 6. If promise is still pending and the session script timeout is reached, return error with error code script timeout.
    case Web::WebDriver::ExecuteScriptResultType::Timeout:
        return WebDriverError::from_code(ErrorCode::ScriptTimeoutError, "Script timed out");
    // 7. Upon fulfillment of promise with value v, let result be a JSON clone of v, and return success with data result.
    case Web::WebDriver::ExecuteScriptResultType::PromiseResolved:
        return result;
    // 8. Upon rejection of promise with reason r, let result be a JSON clone of r, and return error with error code javascript error and data result.
    case Web::WebDriver::ExecuteScriptResultType::PromiseRejected:
    case Web::WebDriver::ExecuteScriptResultType::JavaScriptError:
        return WebDriverError::from_code(ErrorCode::JavascriptError, "Script returned an error", move(result));
    default:
        VERIFY_NOT_REACHED();
    }
}

// 13.2.2 Execute Async Script, https://w3c.github.io/webdriver/#dfn-execute-async-script
ErrorOr<JsonValue, WebDriverError> Session::execute_async_script(JsonValue const& parameters)
{
    // 1. Let body and arguments by the result of trying to extract the script arguments from a request with argument parameters.
    auto [body, arguments] = TRY(extract_the_script_arguments_from_a_request(parameters));

    // 2. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 3. Handle any user prompts, and return its value if it is an error.

    // 4., 5.1-5.11.
    Vector<String> json_arguments;
    arguments.for_each([&](JsonValue const& json_value) {
        // NOTE: serialized() instead of to_string() ensures proper quoting.
        json_arguments.append(json_value.serialized<StringBuilder>());
    });

    dbgln("Executing async script with 'args': [{}] / 'body':\n{}", String::join(", "sv, json_arguments), body);
    auto execute_script_response = m_browser_connection->execute_script(body, json_arguments, m_timeouts_configuration.script_timeout, true);
    dbgln("Executing async script returned: {}", execute_script_response.json_result());

    // NOTE: This is assumed to be a valid JSON value.
    auto result = MUST(JsonValue::from_string(execute_script_response.json_result()));

    switch (execute_script_response.result_type()) {
    // 6. If promise is still pending and the session script timeout is reached, return error with error code script timeout.
    case Web::WebDriver::ExecuteScriptResultType::Timeout:
        return WebDriverError::from_code(ErrorCode::ScriptTimeoutError, "Script timed out");
    // 7. Upon fulfillment of promise with value v, let result be a JSON clone of v, and return success with data result.
    case Web::WebDriver::ExecuteScriptResultType::PromiseResolved:
        return result;
    // 8. Upon rejection of promise with reason r, let result be a JSON clone of r, and return error with error code javascript error and data result.
    case Web::WebDriver::ExecuteScriptResultType::PromiseRejected:
        return WebDriverError::from_code(ErrorCode::JavascriptError, "Script returned an error", move(result));
    default:
        VERIFY_NOT_REACHED();
    }
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
ErrorOr<JsonValue, WebDriverError> Session::get_all_cookies()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

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
ErrorOr<JsonValue, WebDriverError> Session::get_named_cookie(String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

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
    return WebDriverError::from_code(ErrorCode::NoSuchCookie, "Cookie not found");
}

// 14.3 Add Cookie, https://w3c.github.io/webdriver/#dfn-adding-a-cookie
ErrorOr<JsonValue, WebDriverError> Session::add_cookie(JsonValue const& payload)
{
    // 1. Let data be the result of getting a property named cookie from the parameters argument.
    if (!payload.is_object() || !payload.as_object().has_object("cookie"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Payload doesn't have a cookie object");

    auto const& maybe_data = payload.as_object().get("cookie"sv);

    // 2. If data is not a JSON Object with all the required (non-optional) JSON keys listed in the table for cookie conversion,
    //    return error with error code invalid argument.
    // NOTE: Table is here: https://w3c.github.io/webdriver/#dfn-table-for-cookie-conversion
    if (!maybe_data.is_object())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Value \"cookie\' is not an object");

    auto const& data = maybe_data.as_object();

    if (!data.has("name"sv) || !data.has("value"sv))
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Cookie-Object doesn't contain all required keys");

    // 3. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 4. Handle any user prompts, and return its value if it is an error.

    // FIXME: 5. If the current browsing context’s document element is a cookie-averse Document object,
    //           return error with error code invalid cookie domain.

    // 6. If cookie name or cookie value is null,
    //    FIXME: cookie domain is not equal to the current browsing context’s active document’s domain,
    //    cookie secure only or cookie HTTP only are not boolean types,
    //    or cookie expiry time is not an integer type, or it less than 0 or greater than the maximum safe integer,
    //    return error with error code invalid argument.
    if (data.get("name"sv).is_null() || data.get("value"sv).is_null())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Cookie-Object is malformed: name or value are null");
    if (data.has("secure"sv) && !data.get("secure"sv).is_bool())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Cookie-Object is malformed: secure is not bool");
    if (data.has("httpOnly"sv) && !data.get("httpOnly"sv).is_bool())
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Cookie-Object is malformed: httpOnly is not bool");
    Optional<Core::DateTime> expiry_time;
    if (data.has("expiry"sv)) {
        auto expiry_argument = data.get("expiry"sv);
        if (!expiry_argument.is_u32()) {
            // NOTE: less than 0 or greater than safe integer are handled by the JSON parser
            return WebDriverError::from_code(ErrorCode::InvalidArgument, "Cookie-Object is malformed: expiry is not u32");
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
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Expect name attribute to be string");

    if (auto value_attribute = data.get("value"sv); value_attribute.is_string())
        cookie.value = value_attribute.as_string();
    else
        return WebDriverError::from_code(ErrorCode::InvalidArgument, "Expect value attribute to be string");

    // Cookie path
    //     The value if the entry exists, otherwise "/".
    if (data.has("path"sv)) {
        if (auto path_attribute = data.get("path"sv); path_attribute.is_string())
            cookie.path = path_attribute.as_string();
        else
            return WebDriverError::from_code(ErrorCode::InvalidArgument, "Expect path attribute to be string");
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
            return WebDriverError::from_code(ErrorCode::InvalidArgument, "Expect domain attribute to be string");
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
ErrorOr<JsonValue, WebDriverError> Session::delete_cookie(StringView const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts, and return its value if it is an error.

    // 3. Delete cookies using the url variable name parameter as the filter argument.
    delete_cookies(name);

    // 4. Return success with data null.
    return JsonValue();
}

// 14.5 Delete All Cookies, https://w3c.github.io/webdriver/#dfn-delete-all-cookies
ErrorOr<JsonValue, WebDriverError> Session::delete_all_cookies()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // FIXME: 2. Handle any user prompts, and return its value if it is an error.

    // 3. Delete cookies, giving no filtering argument.
    delete_cookies();

    // 4. Return success with data null.
    return JsonValue();
}

// https://w3c.github.io/webdriver/#dfn-encoding-a-canvas-as-base64
static ErrorOr<String, WebDriverError> encode_bitmap_as_canvas_element(Gfx::Bitmap const& bitmap)
{
    // FIXME: 1. If the canvas element’s bitmap’s origin-clean flag is set to false, return error with error code unable to capture screen.

    // 2. If the canvas element’s bitmap has no pixels (i.e. either its horizontal dimension or vertical dimension is zero) then return error with error code unable to capture screen.
    if (bitmap.width() == 0 || bitmap.height() == 0)
        return WebDriverError::from_code(ErrorCode::UnableToCaptureScreen, "Captured screenshot is empty"sv);

    // 3. Let file be a serialization of the canvas element’s bitmap as a file, using "image/png" as an argument.
    auto file = Gfx::PNGWriter::encode(bitmap);

    // 4. Let data url be a data: URL representing file. [RFC2397]
    auto data_url = AK::URL::create_with_data("image/png"sv, encode_base64(file), true).to_string();

    // 5. Let index be the index of "," in data url.
    auto index = data_url.find(',');
    VERIFY(index.has_value());

    // 6. Let encoded string be a substring of data url using (index + 1) as the start argument.
    auto encoded_string = data_url.substring(*index + 1);

    // 7. Return success with data encoded string.
    return encoded_string;
}

// 17.1 Take Screenshot, https://w3c.github.io/webdriver/#take-screenshot
ErrorOr<JsonValue, WebDriverError> Session::take_screenshot()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // 2. When the user agent is next to run the animation frame callbacks:
    //     a. Let root rect be the current top-level browsing context’s document element’s rectangle.
    //     b. Let screenshot result be the result of trying to call draw a bounding box from the framebuffer, given root rect as an argument.
    auto screenshot = m_browser_connection->take_screenshot();
    if (!screenshot.is_valid())
        return WebDriverError::from_code(ErrorCode::UnableToCaptureScreen, "Unable to capture screenshot"sv);

    //     c. Let canvas be a canvas element of screenshot result’s data.
    //     d. Let encoding result be the result of trying encoding a canvas as Base64 canvas.
    //     e. Let encoded string be encoding result’s data.
    auto encoded_string = TRY(encode_bitmap_as_canvas_element(*screenshot.bitmap()));

    // 3. Return success with data encoded string.
    return encoded_string;
}

}
