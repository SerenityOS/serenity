/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Session.h"
#include "Client.h"
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/NumericLimits.h>
#include <AK/Time.h>
#include <LibCore/LocalServer.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
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

ErrorOr<Session::Window*, Web::WebDriver::Error> Session::current_window()
{
    auto window = m_windows.get(m_current_window_handle);
    if (!window.has_value())
        return Web::WebDriver::Error::from_code(Web::WebDriver::ErrorCode::NoSuchWindow, "Window not found");
    return window.release_value();
}

ErrorOr<void, Web::WebDriver::Error> Session::check_for_open_top_level_browsing_context_or_return_error()
{
    (void)TRY(current_window());
    return {};
}

ErrorOr<NonnullRefPtr<Core::LocalServer>> Session::create_server(String const& socket_path, NonnullRefPtr<ServerPromise> promise)
{
    dbgln("Listening for WebDriver connection on {}", socket_path);

    auto server = TRY(Core::LocalServer::try_create());
    server->listen(socket_path);

    server->on_accept = [this, promise](auto client_socket) mutable {
        auto maybe_connection = adopt_nonnull_ref_or_enomem(new (nothrow) WebContentConnection(move(client_socket), m_client, session_id()));
        if (maybe_connection.is_error()) {
            promise->resolve(maybe_connection.release_error());
            return;
        }

        dbgln("WebDriver is connected to WebContent socket");
        m_web_content_connection = maybe_connection.release_value();

        promise->resolve({});
    };

    server->on_accept_error = [promise](auto error) mutable {
        promise->resolve(move(error));
    };

    return server;
}

ErrorOr<void> Session::start()
{
    auto promise = TRY(ServerPromise::try_create());

    auto web_content_socket_path = String::formatted("/tmp/webdriver/session_{}_{}", getpid(), m_id);
    auto web_content_server = TRY(create_server(web_content_socket_path, promise));

    char const* argv[] = {
        "/bin/Browser",
        "--webdriver-content-path",
        web_content_socket_path.characters(),
        nullptr,
    };

    m_browser_pid = TRY(Core::System::posix_spawn("/bin/Browser"sv, nullptr, nullptr, const_cast<char**>(argv), environ));

    // FIXME: Allow this to be more asynchronous. For now, this at least allows us to propagate
    //        errors received while accepting the Browser and WebContent sockets.
    TRY(promise->await());

    m_started = true;
    m_windows.set("main", make<Session::Window>("main", true));
    m_current_window_handle = "main";

    return {};
}

// https://w3c.github.io/webdriver/#dfn-close-the-session
Web::WebDriver::Response Session::stop()
{
    // 1. Perform the following substeps based on the remote end’s type:
    // NOTE: We perform the "Remote end is an endpoint node" steps in the WebContent process.
    m_web_content_connection->close_session();
    m_web_content_connection = nullptr;

    // 2. Remove the current session from active sessions.
    // NOTE: Handled by WebDriver::Client.

    // 3. Perform any implementation-specific cleanup steps.
    if (m_browser_pid.has_value()) {
        MUST(Core::System::kill(*m_browser_pid, SIGTERM));
        m_browser_pid = {};
    }

    m_started = false;

    // 4. If an error has occurred in any of the steps above, return the error, otherwise return success with data null.
    return JsonValue {};
}

// 9.1 Get Timeouts, https://w3c.github.io/webdriver/#dfn-get-timeouts
JsonObject Session::get_timeouts()
{
    // 1. Let timeouts be the timeouts object for session’s timeouts configuration
    auto timeouts = Web::WebDriver::timeouts_object(m_timeouts_configuration);

    // 2. Return success with data timeouts.
    return timeouts;
}

// 9.2 Set Timeouts, https://w3c.github.io/webdriver/#dfn-set-timeouts
Web::WebDriver::Response Session::set_timeouts(JsonValue const& payload)
{
    // 1. Let timeouts be the result of trying to JSON deserialize as a timeouts configuration the request’s parameters.
    auto timeouts = TRY(Web::WebDriver::json_deserialize_as_a_timeouts_configuration(payload));

    // 2. Make the session timeouts the new timeouts.
    m_timeouts_configuration = move(timeouts);

    // 3. Return success with data null.
    return JsonValue {};
}

// 11.1 Get Window Handle, https://w3c.github.io/webdriver/#get-window-handle
Web::WebDriver::Response Session::get_window_handle()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // 2. Return success with data being the window handle associated with the current top-level browsing context.
    return JsonValue { m_current_window_handle };
}

// 11.2 Close Window, https://w3c.github.io/webdriver/#dfn-close-window
ErrorOr<void, Variant<Web::WebDriver::Error, Error>> Session::close_window()
{
    // 1. If the current top-level browsing context is no longer open, return error with error code no such window.
    TRY(check_for_open_top_level_browsing_context_or_return_error());

    // 2. Close the current top-level browsing context.
    m_windows.remove(m_current_window_handle);

    // 3. If there are no more open top-level browsing contexts, then close the session.
    if (m_windows.is_empty()) {
        auto result = stop();
        if (result.is_error()) {
            return Variant<Web::WebDriver::Error, Error>(result.release_error());
        }
    }

    return {};
}

// 11.4 Get Window Handles, https://w3c.github.io/webdriver/#dfn-get-window-handles
Web::WebDriver::Response Session::get_window_handles() const
{
    // 1. Let handles be a JSON List.
    auto handles = JsonArray {};

    // 2. For each top-level browsing context in the remote end, push the associated window handle onto handles.
    for (auto const& window_handle : m_windows.keys())
        handles.append(window_handle);

    // 3. Return success with data handles.
    return JsonValue { handles };
}

}
