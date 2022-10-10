/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"
#include "BrowserConnection.h"
#include "Client.h"
#include <LibCore/LocalServer.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
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

// DELETE /session/{session id}/window https://w3c.github.io/webdriver/#dfn-close-window
ErrorOr<void, Variant<HttpError, Error>> Session::delete_window()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = get_window_object();
    if (!current_window.has_value())
        return Variant<HttpError, Error>(HttpError { 400, "no such window", "Window not found" });

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

// POST /session/{session id}/url https://w3c.github.io/webdriver/#dfn-navigate-to
ErrorOr<JsonValue, HttpError> Session::post_url(JsonValue const& payload)
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = get_window_object();
    if (!current_window.has_value())
        return HttpError { 400, "no such window", "Window not found" };

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

// GET /session/{session id}/url https://w3c.github.io/webdriver/#dfn-get-current-url
ErrorOr<JsonValue, HttpError> Session::get_url()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = get_window_object();
    if (!current_window.has_value())
        return HttpError { 400, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let url be the serialization of the current top-level browsing context’s active document’s document URL.
    auto url = m_browser_connection->get_url().to_string();

    // 4. Return success with data url.
    return JsonValue(url);
}

// GET /session/{session id}/title https://w3c.github.io/webdriver/#dfn-get-title
ErrorOr<JsonValue, HttpError> Session::get_title()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = get_window_object();
    if (!current_window.has_value())
        return HttpError { 400, "no such window", "Window not found" };

    // FIXME: 2. Handle any user prompts and return its value if it is an error.

    // 3. Let title be the initial value of the title IDL attribute of the current top-level browsing context's active document.
    // 4. Return success with data title.
    return JsonValue(m_browser_connection->get_title());
}

}
