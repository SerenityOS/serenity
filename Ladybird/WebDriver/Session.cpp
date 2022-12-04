/*
 * Copyright (c) 2022, Florent Castelli <florent.castelli@gmail.com>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "Session.h"
#include "../Utilities.h"
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <WebDriver/Client.h>
#include <unistd.h>

namespace WebDriver {

Session::Session(unsigned session_id, NonnullRefPtr<Client> client, Web::WebDriver::LadybirdOptions options)
    : m_client(move(client))
    , m_options(move(options))
    , m_id(session_id)
{
}

Session::~Session()
{
    if (auto error = stop(); error.is_error())
        warnln("Failed to stop session {}: {}", m_id, error.error());
}

ErrorOr<void> Session::start()
{
    int socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));
    auto [webdriver_fd, webcontent_fd] = socket_fds;

    int fd_passing_socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fd_passing_socket_fds));
    auto [webdriver_fd_passing_fd, webcontent_fd_passing_fd] = fd_passing_socket_fds;

    m_browser_pid = TRY(Core::System::fork());

    if (m_browser_pid == 0) {
        TRY(Core::System::close(webdriver_fd_passing_fd));
        TRY(Core::System::close(webdriver_fd));

        auto takeover_string = DeprecatedString::formatted("WebDriver:{}", webcontent_fd);
        TRY(Core::System::setenv("SOCKET_TAKEOVER"sv, takeover_string, true));

        auto fd_passing_socket_string = DeprecatedString::number(webcontent_fd_passing_fd);

        if (m_options.headless) {
            auto resouces = DeprecatedString::formatted("{}/res", s_serenity_resource_root);
            auto error_page = DeprecatedString::formatted("{}/res/html/error.html", s_serenity_resource_root);
            auto certs = DeprecatedString::formatted("{}/etc/ca_certs.ini", s_serenity_resource_root);

            char const* argv[] = {
                "headless-browser",
                "--resources",
                resouces.characters(),
                "--error-page",
                error_page.characters(),
                "--certs",
                certs.characters(),
                "--webdriver-fd-passing-socket",
                fd_passing_socket_string.characters(),
                "about:blank",
                nullptr,
            };

            if (execvp("./_deps/lagom-build/headless-browser", const_cast<char**>(argv)) < 0)
                perror("execvp");
        } else {
            char const* argv[] = {
                "ladybird",
                "--webdriver-fd-passing-socket",
                fd_passing_socket_string.characters(),
                nullptr,
            };

            if (execvp("./ladybird", const_cast<char**>(argv)) < 0)
                perror("execvp");
        }

        VERIFY_NOT_REACHED();
    }

    TRY(Core::System::close(webcontent_fd_passing_fd));
    TRY(Core::System::close(webcontent_fd));

    auto socket = TRY(Core::Stream::LocalSocket::adopt_fd(webdriver_fd));
    TRY(socket->set_blocking(true));

    m_web_content_connection = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WebContentConnection(move(socket), m_client, session_id())));
    m_web_content_connection->set_fd_passing_socket(TRY(Core::Stream::LocalSocket::adopt_fd(webdriver_fd_passing_fd)));

    m_started = true;
    return {};
}

// https://w3c.github.io/webdriver/#dfn-close-the-session
Web::WebDriver::Response Session::stop()
{
    if (!m_started)
        return JsonValue {};

    // 1. Perform the following substeps based on the remote end’s type:
    // NOTE: We perform the "Remote end is an endpoint node" steps in the WebContent process.
    m_web_content_connection->close_session();

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

}
