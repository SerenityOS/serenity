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
#include <LibWeb/Platform/Timer.h>
#include <WebContent/PageHost.h>
#include <WebContent/WebDriverConnection.h>

namespace WebContent {

#define DRIVER_TRY(expression)                            \
    ({                                                    \
        auto _temporary_result = (expression);            \
        if (_temporary_result.is_error()) [[unlikely]]    \
            return { _temporary_result.release_error() }; \
        _temporary_result.release_value();                \
    })

static JsonValue make_success_response(JsonValue value)
{
    JsonObject result;
    result.set("value", move(value));
    return result;
}

ErrorOr<NonnullRefPtr<WebDriverConnection>> WebDriverConnection::connect(PageHost& page_host, String const& webdriver_ipc_path)
{
    dbgln_if(WEBDRIVER_DEBUG, "Trying to connect to {}", webdriver_ipc_path);
    auto socket = TRY(Core::Stream::LocalSocket::connect(webdriver_ipc_path));

    dbgln_if(WEBDRIVER_DEBUG, "Connected to WebDriver");
    return adopt_nonnull_ref_or_enomem(new (nothrow) WebDriverConnection(move(socket), page_host));
}

WebDriverConnection::WebDriverConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket, PageHost& page_host)
    : IPC::ConnectionToServer<WebDriverClientEndpoint, WebDriverServerEndpoint>(*this, move(socket))
    , m_page_host(page_host)
{
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
    DRIVER_TRY(ensure_open_top_level_browsing_context());

    // 2. Let url be the result of getting the property url from the parameters argument.
    if (!payload.is_object() || !payload.as_object().has_string("url"sv))
        return { Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::InvalidArgument, "Payload doesn't have a string `url`"sv) };
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
    return { make_success_response({}) };
}

// 10.2 Get Current URL, https://w3c.github.io/webdriver/#get-current-url
Messages::WebDriverClient::GetCurrentUrlResponse WebDriverConnection::get_current_url()
{
    dbgln_if(WEBDRIVER_DEBUG, "WebDriverConnection::get_current_url");

    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    DRIVER_TRY(ensure_open_top_level_browsing_context());

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let url be the serialization of the current top-level browsing context’s active document’s document URL.
    auto url = m_page_host.page().top_level_browsing_context().active_document()->url().to_string();

    // 4. Return success with data url.
    return { make_success_response(url) };
}

// https://w3c.github.io/webdriver/#dfn-no-longer-open
ErrorOr<void, Web::WebDriver::Error> WebDriverConnection::ensure_open_top_level_browsing_context()
{
    // A browsing context is said to be no longer open if it has been discarded.
    if (m_page_host.page().top_level_browsing_context().has_been_discarded())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchWindow, "Window not found"sv);
    return {};
}

}
