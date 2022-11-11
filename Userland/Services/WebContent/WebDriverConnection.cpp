/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Geometry/DOMRect.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/Platform/Timer.h>
#include <LibWeb/WebDriver/Screenshot.h>
#include <WebContent/ConnectionFromClient.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebDriverConnection.h>

namespace WebContent {

static JsonValue make_success_response(JsonValue value)
{
    JsonObject result;
    result.set("value", move(value));
    return result;
}

static JsonValue serialize_rect(Gfx::IntRect const& rect)
{
    JsonObject serialized_rect = {};
    serialized_rect.set("x", rect.x());
    serialized_rect.set("y", rect.y());
    serialized_rect.set("width", rect.width());
    serialized_rect.set("height", rect.height());

    return make_success_response(move(serialized_rect));
}

static Gfx::IntRect compute_window_rect(Web::Page const& page)
{
    return {
        page.window_position().x(),
        page.window_position().y(),
        page.window_size().width(),
        page.window_size().height()
    };
}

// https://w3c.github.io/webdriver/#dfn-calculate-the-absolute-position
static Gfx::IntPoint calculate_absolute_position_of_element(Web::Page const& page, JS::NonnullGCPtr<Web::Geometry::DOMRect> rect)
{
    // 1. Let rect be the value returned by calling getBoundingClientRect().

    // 2. Let window be the associated window of current top-level browsing context.
    auto const* window = page.top_level_browsing_context().active_window();

    // 3. Let x be (scrollX of window + rect’s x coordinate).
    auto x = (window ? static_cast<int>(window->scroll_x()) : 0) + static_cast<int>(rect->x());

    // 4. Let y be (scrollY of window + rect’s y coordinate).
    auto y = (window ? static_cast<int>(window->scroll_y()) : 0) + static_cast<int>(rect->y());

    // 5. Return a pair of (x, y).
    return { x, y };
}

static Gfx::IntRect calculate_absolute_rect_of_element(Web::Page const& page, Web::DOM::Element const& element)
{
    auto bounding_rect = element.get_bounding_client_rect();
    auto coordinates = calculate_absolute_position_of_element(page, bounding_rect);

    return {
        coordinates.x(),
        coordinates.y(),
        static_cast<int>(bounding_rect->width()),
        static_cast<int>(bounding_rect->height())
    };
}

// https://w3c.github.io/webdriver/#dfn-get-or-create-a-web-element-reference
static String get_or_create_a_web_element_reference(Web::DOM::Node const& element)
{
    // FIXME: 1. For each known element of the current browsing context’s list of known elements:
    // FIXME:     1. If known element equals element, return success with known element’s web element reference.
    // FIXME: 2. Add element to the list of known elements of the current browsing context.
    // FIXME: 3. Return success with the element’s web element reference.

    return String::number(element.id());
}

// https://w3c.github.io/webdriver/#dfn-web-element-reference-object
static JsonObject web_element_reference_object(Web::DOM::Node const& element)
{
    // https://w3c.github.io/webdriver/#dfn-web-element-identifier
    static String const web_element_identifier = "element-6066-11e4-a52e-4f735466cecf"sv;

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

// https://w3c.github.io/webdriver/#dfn-get-a-known-connected-element
static ErrorOr<Web::DOM::Element*, Web::WebDriver::Error> get_known_connected_element(StringView element_id)
{
    // NOTE: The whole concept of "connected elements" is not implemented yet. See get_or_create_a_web_element_reference().
    //       For now the element is only represented by its ID.
    auto element = element_id.to_int();
    if (!element.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Element ID is not an integer");

    auto* node = Web::DOM::Node::from_id(*element);

    if (!node || !node->is_element())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, String::formatted("Could not find element with ID: {}", element_id));

    return static_cast<Web::DOM::Element*>(node);
}

// https://w3c.github.io/webdriver/#dfn-scrolls-into-view
static void scroll_element_into_view(Web::DOM::Element& element)
{
    // 1. Let options be the following ScrollIntoViewOptions:
    Web::DOM::ScrollIntoViewOptions options {};
    // Logical scroll position "block"
    //     "end"
    options.block = Web::Bindings::ScrollLogicalPosition::End;
    // Logical scroll position "inline"
    //     "nearest"
    options.inline_ = Web::Bindings::ScrollLogicalPosition::Nearest;

    // 2. Run Function.[[Call]](scrollIntoView, options) with element as the this value.
    element.scroll_into_view(options);
}

static ErrorOr<String, Web::WebDriver::Error> get_property(JsonValue const& payload, StringView key)
{
    if (!payload.is_object())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const* property = payload.as_object().get_ptr(key);

    if (!property)
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("No property called '{}' present", key));
    if (!property->is_string())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Property '{}' is not a String", key));

    return property->as_string();
}

ErrorOr<NonnullRefPtr<WebDriverConnection>> WebDriverConnection::connect(ConnectionFromClient& web_content_client, PageHost& page_host, String const& webdriver_ipc_path)
{
    dbgln_if(WEBDRIVER_DEBUG, "Trying to connect to {}", webdriver_ipc_path);
    auto socket = TRY(Core::Stream::LocalSocket::connect(webdriver_ipc_path));

    dbgln_if(WEBDRIVER_DEBUG, "Connected to WebDriver");
    return adopt_nonnull_ref_or_enomem(new (nothrow) WebDriverConnection(move(socket), web_content_client, page_host));
}

WebDriverConnection::WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, ConnectionFromClient& web_content_client, PageHost& page_host)
    : IPC::ConnectionToServer<WebDriverClientEndpoint, WebDriverServerEndpoint>(*this, move(socket))
    , m_web_content_client(web_content_client)
    , m_page_host(page_host)
{
}

// https://w3c.github.io/webdriver/#dfn-close-the-session
void WebDriverConnection::close_session()
{
    // 1. Set the webdriver-active flag to false.
    set_is_webdriver_active(false);

    // 2. An endpoint node must close any top-level browsing contexts associated with the session, without prompting to unload.
    m_page_host.page().top_level_browsing_context().close();
}

void WebDriverConnection::set_is_webdriver_active(bool is_webdriver_active)
{
    m_page_host.set_is_webdriver_active(is_webdriver_active);
}

// 10.1 Navigate To, https://w3c.github.io/webdriver/#navigate-to
Messages::WebDriverClient::NavigateToResponse WebDriverConnection::navigate_to(JsonValue const& payload)
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection::navigate_to {}", payload);

    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // 2. Let url be the result of getting the property url from the parameters argument.
    if (!payload.is_object() || !payload.as_object().has_string("url"sv))
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload doesn't have a string `url`"sv);
    URL url(payload.as_object().get_ptr("url"sv)->as_string());

    // FIXME: 3. If url is not an absolute URL or is not an absolute URL with fragment or not a local scheme, return error with error code invalid argument.
    // FIXME: 4. Handle any user prompts and return its value if it is an error.
    // FIXME: 5. Let current URL be the current top-level browsing context’s active document’s URL.
    // FIXME: 6. If current URL and url do not have the same absolute URL:
    // FIXME:     a. If timer has not been started, start a timer. If this algorithm has not completed before timer reaches the session’s session page load timeout in milliseconds, return an error with error code timeout.

    // 7. Navigate the current top-level browsing context to url.
    m_page_host.page().load(url);

    // FIXME: 8. If url is special except for file and current URL and URL do not have the same absolute URL:
    // FIXME:     a. Try to wait for navigation to complete.
    // FIXME:     b. Try to run the post-navigation checks.
    // FIXME: 9. Set the current browsing context with the current top-level browsing context.
    // FIXME: 10. If the current top-level browsing context contains a refresh state pragma directive of time 1 second or less, wait until the refresh timeout has elapsed, a new navigate has begun, and return to the first step of this algorithm.

    // 11. Return success with data null.
    return make_success_response({});
}

// 10.2 Get Current URL, https://w3c.github.io/webdriver/#get-current-url
Messages::WebDriverClient::GetCurrentUrlResponse WebDriverConnection::get_current_url()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection::get_current_url");

    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let url be the serialization of the current top-level browsing context’s active document’s document URL.
    auto url = m_page_host.page().top_level_browsing_context().active_document()->url().to_string();

    // 4. Return success with data url.
    return make_success_response(url);
}

// 11.8.1 Get Window Rect, https://w3c.github.io/webdriver/#dfn-get-window-rect
Messages::WebDriverClient::GetWindowRectResponse WebDriverConnection::get_window_rect()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(compute_window_rect(m_page_host.page()));
}

// 11.8.2 Set Window Rect, https://w3c.github.io/webdriver/#dfn-set-window-rect
Messages::WebDriverClient::SetWindowRectResponse WebDriverConnection::set_window_rect(JsonValue const& payload)
{
    if (!payload.is_object())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload is not a JSON object");

    auto const& properties = payload.as_object();

    auto resolve_property = [](auto name, auto const* property, auto min, auto max) -> ErrorOr<Optional<i32>, Web::WebDriver::Error> {
        if (!property)
            return Optional<i32> {};
        if (!property->is_number())
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Property '{}' is not a Number", name));

        auto number = property->template to_number<i64>();

        if (number < min)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Property '{}' value {} exceeds the minimum allowed value {}", name, number, min));
        if (number > max)
            return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Property '{}' value {} exceeds the maximum allowed value {}", name, number, max));

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
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 9. Handle any user prompts and return its value if it is an error.
    // FIXME: 10. Fully exit fullscreen.

    // 11. Restore the window.
    restore_the_window();

    Gfx::IntRect window_rect;

    // 11. If width and height are not null:
    if (width.has_value() && height.has_value()) {
        // a. Set the width, in CSS pixels, of the operating system window containing the current top-level browsing context, including any browser chrome and externally drawn window decorations to a value that is as close as possible to width.
        // b. Set the height, in CSS pixels, of the operating system window containing the current top-level browsing context, including any browser chrome and externally drawn window decorations to a value that is as close as possible to height.
        auto size = m_web_content_client.did_request_resize_window({ *width, *height });
        window_rect.set_size(size);
    } else {
        window_rect.set_size(m_page_host.page().window_size());
    }

    // 12. If x and y are not null:
    if (x.has_value() && y.has_value()) {
        // a. Run the implementation-specific steps to set the position of the operating system level window containing the current top-level browsing context to the position given by the x and y coordinates.
        auto position = m_web_content_client.did_request_reposition_window({ *x, *y });
        window_rect.set_location(position);
    } else {
        window_rect.set_location(m_page_host.page().window_position());
    }

    // 14. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(window_rect);
}

// 11.8.3 Maximize Window, https://w3c.github.io/webdriver/#dfn-maximize-window
Messages::WebDriverClient::MaximizeWindowResponse WebDriverConnection::maximize_window()
{
    // 1. If the remote end does not support the Maximize Window command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 3. Handle any user prompts and return its value if it is an error.
    // FIXME: 4. Fully exit fullscreen.

    // 5. Restore the window.
    restore_the_window();

    // 6. Maximize the window of the current top-level browsing context.
    auto window_rect = maximize_the_window();

    // 7. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(window_rect);
}

// 11.8.4 Minimize Window, https://w3c.github.io/webdriver/#minimize-window
Messages::WebDriverClient::MinimizeWindowResponse WebDriverConnection::minimize_window()
{
    // 1. If the remote end does not support the Minimize Window command for the current top-level browsing context for any reason, return error with error code unsupported operation.

    // 2. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 3. Handle any user prompts and return its value if it is an error.
    // FIXME: 4. Fully exit fullscreen.

    // 5. Iconify the window.
    auto window_rect = iconify_the_window();

    // 6. Return success with data set to the WindowRect object for the current top-level browsing context.
    return serialize_rect(window_rect);
}

// 12.3.2 Find Element, https://w3c.github.io/webdriver/#dfn-find-element
Messages::WebDriverClient::FindElementResponse WebDriverConnection::find_element(JsonValue const& payload)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the current browsing context’s document element.
    auto* start_node = m_page_host.page().top_level_browsing_context().active_document();

    // 8. If start node is null, return error with error code no such element.
    if (!start_node)
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "document element does not exist"sv);

    // 9. Let result be the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(*start_node, *location_strategy, selector));

    // 10. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "The requested element does not exist"sv);

    return make_success_response(result.at(0));
}

// 12.3.3 Find Elements, https://w3c.github.io/webdriver/#dfn-find-elements
Messages::WebDriverClient::FindElementsResponse WebDriverConnection::find_elements(JsonValue const& payload)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the current browsing context’s document element.
    auto* start_node = m_page_host.page().top_level_browsing_context().active_document();

    // 8. If start node is null, return error with error code no such element.
    if (!start_node)
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "document element does not exist"sv);

    // 9. Return the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(*start_node, *location_strategy, selector));
    return make_success_response(move(result));
}

// 12.3.4 Find Element From Element, https://w3c.github.io/webdriver/#dfn-find-element-from-element
Messages::WebDriverClient::FindElementFromElementResponse WebDriverConnection::find_element_from_element(JsonValue const& payload, String const& element_id)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the result of trying to get a known connected element with url variable element id.
    auto* start_node = TRY(get_known_connected_element(element_id));

    // 8. Let result be the value of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(*start_node, *location_strategy, selector));

    // 9. If result is empty, return error with error code no such element. Otherwise, return the first element of result.
    if (result.is_empty())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchElement, "The requested element does not exist"sv);

    return make_success_response(result.at(0));
}

// 12.3.5 Find Elements From Element, https://w3c.github.io/webdriver/#dfn-find-elements-from-element
Messages::WebDriverClient::FindElementsFromElementResponse WebDriverConnection::find_elements_from_element(JsonValue const& payload, String const& element_id)
{
    // 1. Let location strategy be the result of getting a property called "using".
    auto location_strategy_string = TRY(get_property(payload, "using"sv));
    auto location_strategy = Web::WebDriver::location_strategy_from_string(location_strategy_string);

    // 2. If location strategy is not present as a keyword in the table of location strategies, return error with error code invalid argument.
    if (!location_strategy.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, String::formatted("Location strategy '{}' is invalid", location_strategy_string));

    // 3. Let selector be the result of getting a property called "value".
    // 4. If selector is undefined, return error with error code invalid argument.
    auto selector = TRY(get_property(payload, "value"sv));

    // 5. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 6. Handle any user prompts and return its value if it is an error.

    // 7. Let start node be the result of trying to get a known connected element with url variable element id.
    auto* start_node = TRY(get_known_connected_element(element_id));

    // 8. Return the result of trying to Find with start node, location strategy, and selector.
    auto result = TRY(find(*start_node, *location_strategy, selector));
    return make_success_response(move(result));
}

// 12.4.1 Is Element Selected, https://w3c.github.io/webdriver/#dfn-is-element-selected
Messages::WebDriverClient::IsElementSelectedResponse WebDriverConnection::is_element_selected(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Let selected be the value corresponding to the first matching statement:
    bool selected = false;

    // element is an input element with a type attribute in the Checkbox- or Radio Button state
    if (is<Web::HTML::HTMLInputElement>(*element)) {
        // -> The result of element’s checkedness.
        auto& input = static_cast<Web::HTML::HTMLInputElement&>(*element);
        using enum Web::HTML::HTMLInputElement::TypeAttributeState;

        if (input.type_state() == Checkbox || input.type_state() == RadioButton)
            selected = input.checked();
    }
    // element is an option element
    else if (is<Web::HTML::HTMLOptionElement>(*element)) {
        // -> The result of element’s selectedness.
        selected = static_cast<Web::HTML::HTMLOptionElement&>(*element).selected();
    }
    // Otherwise
    //   -> False.

    // 5. Return success with data selected.
    return make_success_response(selected);
}

// 12.4.2 Get Element Attribute, https://w3c.github.io/webdriver/#dfn-get-element-attribute
Messages::WebDriverClient::GetElementAttributeResponse WebDriverConnection::get_element_attribute(String const& element_id, String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Let result be the result of the first matching condition:
    Optional<String> result;

    // -> If name is a boolean attribute
    if (Web::HTML::is_boolean_attribute(name)) {
        // "true" (string) if the element has the attribute, otherwise null.
        if (element->has_attribute(name))
            result = "true"sv;
    }
    // -> Otherwise
    else {
        // The result of getting an attribute by name name.
        result = element->get_attribute(name);
    }

    // 5. Return success with data result.
    if (result.has_value())
        return make_success_response(result.release_value());
    return make_success_response({});
}

// 12.4.3 Get Element Property, https://w3c.github.io/webdriver/#dfn-get-element-property
Messages::WebDriverClient::GetElementPropertyResponse WebDriverConnection::get_element_property(String const& element_id, String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    Optional<String> result;

    // 4. Let property be the result of calling the Object.[[GetProperty]](name) on element.
    if (auto property_or_error = element->get(name); !property_or_error.is_throw_completion()) {
        auto property = property_or_error.release_value();

        // 5. Let result be the value of property if not undefined, or null.
        if (!property.is_undefined()) {
            if (auto string_or_error = property.to_string(element->vm()); !string_or_error.is_error())
                result = string_or_error.release_value();
        }
    }

    // 6. Return success with data result.
    if (result.has_value())
        return make_success_response(result.release_value());
    return make_success_response({});
}

// 12.4.4 Get Element CSS Value, https://w3c.github.io/webdriver/#dfn-get-element-css-value
Messages::WebDriverClient::GetElementCssValueResponse WebDriverConnection::get_element_css_value(String const& element_id, String const& name)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Let computed value be the result of the first matching condition:
    String computed_value;

    // -> current browsing context’s active document’s type is not "xml"
    if (!m_page_host.page().top_level_browsing_context().active_document()->is_xml_document()) {
        // computed value of parameter property name from element’s style declarations. property name is obtained from url variables.
        auto property = Web::CSS::property_id_from_string(name);

        if (auto* computed_values = element->computed_css_values())
            computed_value = computed_values->property(property)->to_string();
    }
    // -> Otherwise
    else {
        // "" (empty string)
        computed_value = String::empty();
    }

    // 5. Return success with data computed value.
    return make_success_response(move(computed_value));
}

// 12.4.5 Get Element Text, https://w3c.github.io/webdriver/#dfn-get-element-text
Messages::WebDriverClient::GetElementTextResponse WebDriverConnection::get_element_text(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Let rendered text be the result of performing implementation-specific steps whose result is exactly the same as the result of a Function.[[Call]](null, element) with bot.dom.getVisibleText as the this value.
    auto rendered_text = element->text_content();

    // 5. Return success with data rendered text.
    return make_success_response(move(rendered_text));
}

// 12.4.6 Get Element Tag Name, https://w3c.github.io/webdriver/#dfn-get-element-tag-name
Messages::WebDriverClient::GetElementTagNameResponse WebDriverConnection::get_element_tag_name(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Let qualified name be the result of getting element’s tagName IDL attribute.
    auto qualified_name = element->tag_name();

    // 5. Return success with data qualified name.
    return make_success_response(move(qualified_name));
}

// 12.4.7 Get Element Rect, https://w3c.github.io/webdriver/#dfn-get-element-rect
Messages::WebDriverClient::GetElementRectResponse WebDriverConnection::get_element_rect(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Calculate the absolute position of element and let it be coordinates.
    // 5. Let rect be element’s bounding rectangle.
    auto rect = calculate_absolute_rect_of_element(m_page_host.page(), *element);

    // 6. Let body be a new JSON Object initialized with:
    // "x"
    //     The first value of coordinates.
    // "y"
    //     The second value of coordinates.
    // "width"
    //     Value of rect’s width dimension.
    // "height"
    //     Value of rect’s height dimension.
    auto body = serialize_rect(rect);

    // 7. Return success with data body.
    return body;
}

// 12.4.8 Is Element Enabled, https://w3c.github.io/webdriver/#dfn-is-element-enabled
Messages::WebDriverClient::IsElementEnabledResponse WebDriverConnection::is_element_enabled(String const& element_id)
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Let enabled be a boolean initially set to true if the current browsing context’s active document’s type is not "xml".
    // 5. Otherwise, let enabled to false and jump to the last step of this algorithm.
    bool enabled = !m_page_host.page().top_level_browsing_context().active_document()->is_xml_document();

    // 6. Set enabled to false if a form control is disabled.
    if (enabled && is<Web::HTML::FormAssociatedElement>(*element)) {
        auto& form_associated_element = dynamic_cast<Web::HTML::FormAssociatedElement&>(*element);
        enabled = form_associated_element.enabled();
    }

    // 7. Return success with data enabled.
    return make_success_response(enabled);
}

// 13.1 Get Page Source, https://w3c.github.io/webdriver/#dfn-get-page-source
Messages::WebDriverClient::GetSourceResponse WebDriverConnection::get_source()
{
    // 1. If the current browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    auto* document = m_page_host.page().top_level_browsing_context().active_document();
    Optional<String> source;

    // 3. Let source be the result of invoking the fragment serializing algorithm on a fictional node whose only child is the document element providing true for the require well-formed flag. If this causes an exception to be thrown, let source be null.
    if (auto result = document->serialize_fragment(Web::DOMParsing::RequireWellFormed::Yes); !result.is_error())
        source = result.release_value();

    // 4. Let source be the result of serializing to string the current browsing context active document, if source is null.
    if (!source.has_value())
        source = MUST(document->serialize_fragment(Web::DOMParsing::RequireWellFormed::No));

    // 5. Return success with data source.
    return make_success_response(source.release_value());
}

// 17.1 Take Screenshot, https://w3c.github.io/webdriver/#take-screenshot
Messages::WebDriverClient::TakeScreenshotResponse WebDriverConnection::take_screenshot()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // 2. When the user agent is next to run the animation frame callbacks:
    //     a. Let root rect be the current top-level browsing context’s document element’s rectangle.
    //     b. Let screenshot result be the result of trying to call draw a bounding box from the framebuffer, given root rect as an argument.
    //     c. Let canvas be a canvas element of screenshot result’s data.
    //     d. Let encoding result be the result of trying encoding a canvas as Base64 canvas.
    //     e. Let encoded string be encoding result’s data.
    auto* document = m_page_host.page().top_level_browsing_context().active_document();
    auto root_rect = calculate_absolute_rect_of_element(m_page_host.page(), *document->document_element());

    auto encoded_string = TRY(Web::WebDriver::capture_element_screenshot(
        [&](auto const& rect, auto& bitmap) { m_page_host.paint(rect, bitmap); },
        m_page_host.page(),
        *document->document_element(),
        root_rect));

    // 3. Return success with data encoded string.
    return make_success_response(move(encoded_string));
}

// 17.2 Take Element Screenshot, https://w3c.github.io/webdriver/#dfn-take-element-screenshot
Messages::WebDriverClient::TakeElementScreenshotResponse WebDriverConnection::take_element_screenshot(String const& element_id)
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let element be the result of trying to get a known connected element with url variable element id.
    auto* element = TRY(get_known_connected_element(element_id));

    // 4. Scroll into view the element.
    scroll_element_into_view(*element);

    // 5. When the user agent is next to run the animation frame callbacks:
    //     a. Let element rect be element’s rectangle.
    //     b. Let screenshot result be the result of trying to call draw a bounding box from the framebuffer, given element rect as an argument.
    //     c. Let canvas be a canvas element of screenshot result’s data.
    //     d. Let encoding result be the result of trying encoding a canvas as Base64 canvas.
    //     e. Let encoded string be encoding result’s data.
    auto element_rect = calculate_absolute_rect_of_element(m_page_host.page(), *element);

    auto encoded_string = TRY(Web::WebDriver::capture_element_screenshot(
        [&](auto const& rect, auto& bitmap) { m_page_host.paint(rect, bitmap); },
        m_page_host.page(),
        *element,
        element_rect));

    // 6. Return success with data encoded string.
    return make_success_response(move(encoded_string));
}

// https://w3c.github.io/webdriver/#dfn-no-longer-open
ErrorOr<void, Web::WebDriver::Error> WebDriverConnection::ensure_open_top_level_browsing_context()
{
    // A browsing context is said to be no longer open if it has been discarded.
    if (m_page_host.page().top_level_browsing_context().has_been_discarded())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchWindow, "Window not found"sv);
    return {};
}

// https://w3c.github.io/webdriver/#dfn-restore-the-window
void WebDriverConnection::restore_the_window()
{
    // To restore the window, given an operating system level window with an associated top-level browsing context, run implementation-specific steps to restore or unhide the window to the visible screen.
    m_web_content_client.async_did_request_restore_window();

    // Do not return from this operation until the visibility state of the top-level browsing context’s active document has reached the visible state, or until the operation times out.
    // FIXME: Implement timeouts.
    Web::Platform::EventLoopPlugin::the().spin_until([this]() {
        auto state = m_page_host.page().top_level_browsing_context().system_visibility_state();
        return state == Web::HTML::VisibilityState::Visible;
    });
}

// https://w3c.github.io/webdriver/#dfn-maximize-the-window
Gfx::IntRect WebDriverConnection::maximize_the_window()
{
    // To maximize the window, given an operating system level window with an associated top-level browsing context, run the implementation-specific steps to transition the operating system level window into the maximized window state.
    auto rect = m_web_content_client.did_request_maximize_window();

    // Return when the window has completed the transition, or within an implementation-defined timeout.
    return rect;
}

// https://w3c.github.io/webdriver/#dfn-iconify-the-window
Gfx::IntRect WebDriverConnection::iconify_the_window()
{
    // To iconify the window, given an operating system level window with an associated top-level browsing context, run implementation-specific steps to iconify, minimize, or hide the window from the visible screen.
    auto rect = m_web_content_client.did_request_minimize_window();

    // Do not return from this operation until the visibility state of the top-level browsing context’s active document has reached the hidden state, or until the operation times out.
    // FIXME: Implement timeouts.
    Web::Platform::EventLoopPlugin::the().spin_until([this]() {
        auto state = m_page_host.page().top_level_browsing_context().system_visibility_state();
        return state == Web::HTML::VisibilityState::Hidden;
    });

    return rect;
}

// https://w3c.github.io/webdriver/#dfn-find
ErrorOr<JsonArray, Web::WebDriver::Error> WebDriverConnection::find(Web::DOM::ParentNode& start_node, Web::WebDriver::LocationStrategy using_, StringView value)
{
    // FIXME: 1. Let end time be the current time plus the session implicit wait timeout.

    // 2. Let location strategy be equal to using.
    auto location_strategy = using_;

    // 3. Let selector be equal to value.
    auto selector = value;

    // 4. Let elements returned be the result of trying to call the relevant element location strategy with arguments start node, and selector.
    auto elements = Web::WebDriver::invoke_location_strategy(location_strategy, start_node, selector);

    // 5. If a DOMException, SyntaxError, XPathException, or other error occurs during the execution of the element location strategy, return error invalid selector.
    if (elements.is_error())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidSelector, String::formatted("The location strategy could not finish: {}", elements.error().message));

    // FIXME: 6. If elements returned is empty and the current time is less than end time return to step 4. Otherwise, continue to the next step.

    // 7. Let result be an empty JSON List.
    JsonArray result;
    result.ensure_capacity(elements.value()->length());

    // 8. For each element in elements returned, append the web element reference object for element, to result.
    for (size_t i = 0; i < elements.value()->length(); ++i)
        result.append(web_element_reference_object(*elements.value()->item(i)));

    // 9. Return success with data result.
    return result;
}

}
