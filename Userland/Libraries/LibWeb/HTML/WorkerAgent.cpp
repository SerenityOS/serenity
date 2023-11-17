/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/Socket.h>
#include <LibCore/System.h>
#include <LibWeb/HTML/WorkerAgent.h>
#include <LibWeb/Worker/WebWorkerClient.h>

// FIXME: Deduplicate this code with ladybird!!

#ifndef AK_OS_SERENITY
namespace {

ErrorOr<String> application_directory()
{
    auto current_executable_path = TRY(Core::System::current_executable_path());
    auto dirname = LexicalPath::dirname(current_executable_path.to_deprecated_string());
    return String::from_deprecated_string(dirname);
}

ErrorOr<Vector<String>> get_paths_for_helper_process(StringView process_name)
{
    auto application_path = TRY(application_directory());
    Vector<String> paths;

    TRY(paths.try_append(TRY(String::formatted("{}/{}", application_path, process_name))));
    TRY(paths.try_append(TRY(String::formatted("./{}", process_name))));
    // NOTE: Add platform-specific paths here
    return paths;
}

ErrorOr<NonnullRefPtr<Web::HTML::WebWorkerClient>> launch_web_worker_process(ReadonlySpan<String> candidate_web_content_paths)
{
    int socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    int ui_fd = socket_fds[0];
    int wc_fd = socket_fds[1];

    int fd_passing_socket_fds[2] {};
    TRY(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fd_passing_socket_fds));

    int ui_fd_passing_fd = fd_passing_socket_fds[0];
    int wc_fd_passing_fd = fd_passing_socket_fds[1];

    if (auto child_pid = TRY(Core::System::fork()); child_pid == 0) {
        TRY(Core::System::close(ui_fd_passing_fd));
        TRY(Core::System::close(ui_fd));

        auto takeover_string = TRY(String::formatted("WebWorker:{}", wc_fd));
        TRY(Core::System::setenv("SOCKET_TAKEOVER"sv, takeover_string, true));

        auto webcontent_fd_passing_socket_string = TRY(String::number(wc_fd_passing_fd));

        ErrorOr<void> result;
        for (auto const& path : candidate_web_content_paths) {
            if (Core::System::access(path, X_OK).is_error())
                continue;

            auto arguments = Vector {
                path.bytes_as_string_view(),
                "--fd-passing-socket"sv,
                webcontent_fd_passing_socket_string
            };

            result = Core::System::exec(arguments[0], arguments.span(), Core::System::SearchInPath::Yes);
            if (!result.is_error())
                break;
        }

        if (result.is_error())
            warnln("Could not launch any of {}: {}", candidate_web_content_paths, result.error());
        VERIFY_NOT_REACHED();
    }

    TRY(Core::System::close(wc_fd_passing_fd));
    TRY(Core::System::close(wc_fd));

    auto socket = TRY(Core::LocalSocket::adopt_fd(ui_fd));
    TRY(socket->set_blocking(true));

    auto new_client = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Web::HTML::WebWorkerClient(move(socket))));
    new_client->set_fd_passing_socket(TRY(Core::LocalSocket::adopt_fd(ui_fd_passing_fd)));

    return new_client;
}
}
#endif

namespace Web::HTML {

WorkerAgent::WorkerAgent(AK::URL url, WorkerOptions const& options)
    : m_worker_options(options)
    , m_url(move(url))
{
#ifndef AK_OS_SERENITY
    // FIXME: Add factory function
    auto paths = MUST(get_paths_for_helper_process("WebWorker"sv));
    m_worker_ipc = MUST(launch_web_worker_process(paths));
#else
    m_worker_ipc = MUST(Web::HTML::WebWorkerClient::try_create());
#endif

    int fds[2] = {};
    MUST(Core::System::socketpair(AF_LOCAL, SOCK_STREAM, 0, fds));
    m_message_port_fd = fds[0];

    m_worker_ipc->async_start_dedicated_worker(m_url, options.type, options.credentials, options.name, fds[1]);
}

}
