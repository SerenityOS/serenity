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
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/Platform/Timer.h>
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

}
