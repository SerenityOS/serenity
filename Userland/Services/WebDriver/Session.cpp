/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Format.h>
#include <LibCore/LocalServer.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <WebDriver/ConnectionFromClient.h>
#include <WebDriver/Session.h>
#include <unistd.h>

namespace WebDriver {

WebDriverSession::WebDriverSession(unsigned session_id)
    : m_id(session_id)
{
}

WebDriverSession::~WebDriverSession()
{
    if (m_started) {
        auto error = stop();
        if (error.is_error()) {
            warnln("Failed to stop session {}: {}", m_id, error.error());
        }
    }
}

ErrorOr<void> WebDriverSession::start()
{ /*
     m_local_server = Core::LocalServer::construct();
     auto socket_path = String::formatted("/tmp/browser_webdriver_{}_{}", getpid(), m_id);
     m_local_server->listen(socket_path);
     m_local_server->on_accept = [&](NonnullOwnPtr<Core::Stream::LocalSocket> socket) {
         dbgln("Accepted a connection!");
         if (!socket->set_blocking(true).is_error()) {
             m_browser_connection = ConnectionFromClient(move(socket));
         }
     };*/

    auto socket_path = String::formatted("/tmp/browser_webdriver_{}_{}", getpid(), m_id);
    dbgln("Listening for WebDriver connection on {}", socket_path);

    // FIXME: Use Core::LocalServer
    struct sockaddr_un addr;
    int listen_socket = TRY(Core::System::socket(AF_UNIX, SOCK_STREAM, 0));
    ::memset(&addr, 0, sizeof(struct sockaddr_un));
    addr.sun_family = AF_UNIX;
    ::strncpy(addr.sun_path, socket_path.characters(), sizeof(addr.sun_path) - 1);

    TRY(Core::System::bind(listen_socket, (const struct sockaddr*)&addr,
        sizeof(struct sockaddr_un)));
    TRY(Core::System::listen(listen_socket, 1));

    char const* argv[] = { "/bin/Browser", "--webdriver", socket_path.characters(), nullptr };
    m_browser_pid = TRY(Core::System::posix_spawn("/bin/Browser", nullptr, nullptr, const_cast<char**>(argv), environ));

    int data_socket = TRY(Core::System::accept(listen_socket, nullptr, nullptr));
    auto socket = TRY(Core::Stream::LocalSocket::adopt_fd(data_socket));
    TRY(socket->set_blocking(true));
    m_browser_connection = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ConnectionFromClient(move(socket))));

    dbgln("Browser is connected");

    m_started = true;
    m_windows.set("main", make<WebDriverSession::Window>("main", true));
    m_current_window_handle = "main";

    ::sleep(3);

    return {};
}

ErrorOr<void> WebDriverSession::stop()
{
    m_browser_connection->async_quit();
    return {};
}

ErrorOr<void, Variant<HttpError, Error>> WebDriverSession::delete_window()
{
    // https://w3c.github.io/webdriver/webdriver-spec.html#close-window
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = get_window_object();
    if (!current_window.has_value())
        return Variant<HttpError, Error>(HttpError { 400, "no such window", "Window not found" });

    // 2. Close the current top-level browsing context.
    // FIXME: Actually remove the window
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

ErrorOr<JsonValue, HttpError> WebDriverSession::post_url(JsonValue payload)
{
    // https://w3c.github.io/webdriver/webdriver-spec.html#go
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = get_window_object();
    if (!current_window.has_value())
        return HttpError { 400, "no such window", "Window not found" };

    // 2. Handle any user prompts and return its value if it is an error.
    // FIXME

    // 3. If the url property is missing from the parameters argument or it is not a string, return error with error code invalid argument.
    if (!payload.is_object() || !payload.as_object().has_string("url"sv)) {
        return HttpError { 400, "invalid argument", "Payload doesn't have a string url" };
    }

    // 4. Let url be the result of getting a property named url from the parameters argument.
    URL url(payload.as_object().get_ptr("url"sv)->as_string());

    // 5. If url is not an absolute URL or an absolute URL with fragment, return error with error code invalid argument. [URL]
    // FIXME

    // 6. Let url be the result of getting a property named url from the parameters argument.
    // Duplicate step?

    // 7. Navigate the current top-level browsing context to url.
    m_browser_connection->async_set_url(url);

    // 8. Run the post-navigation checks and return its value if it is an error.
    // FIXME

    // 9. Wait for navigation to complete and return its value if it is an error.
    // FIXME
    ::sleep(5);

    // 10. Set the current browsing context to the current top-level browsing context.
    // FIXME

    // 11. Return success with data null.
    return JsonValue();
}

ErrorOr<JsonValue, HttpError> WebDriverSession::get_title()
{
    // https://w3c.github.io/webdriver/webdriver-spec.html#get-title
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    auto current_window = get_window_object();
    if (!current_window.has_value())
        return HttpError { 400, "no such window", "Window not found" };

    // 2. Handle any user prompts and return its value if it is an error.
    // FIXME

    // 3. Let title be the initial value of the title IDL attribute of the current top-level browsing context's active document.
    // 4.Return success with data title.
    return JsonValue(m_browser_connection->get_title());
}

}
