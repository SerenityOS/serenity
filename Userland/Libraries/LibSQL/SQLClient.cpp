/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibSQL/SQLClient.h>

#if !defined(AK_OS_SERENITY)
#    include <LibCore/Directory.h>
#    include <LibCore/File.h>
#    include <LibCore/SocketAddress.h>
#    include <LibCore/StandardPaths.h>
#    include <LibCore/Stream.h>
#    include <LibCore/System.h>
#endif

namespace SQL {

#if !defined(AK_OS_SERENITY)

// This is heavily based on how SystemServer's Service creates its socket.
static ErrorOr<int> create_database_socket(DeprecatedString const& socket_path)
{
    if (Core::File::exists(socket_path))
        TRY(Core::System::unlink(socket_path));

#    ifdef SOCK_NONBLOCK
    auto socket_fd = TRY(Core::System::socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0));
#    else
    auto socket_fd = TRY(Core::System::socket(AF_LOCAL, SOCK_STREAM, 0));

    int option = 1;
    TRY(Core::System::ioctl(socket_fd, FIONBIO, &option));
    TRY(Core::System::fcntl(socket_fd, F_SETFD, FD_CLOEXEC));
#    endif

    TRY(Core::System::fchmod(socket_fd, 0600));

    auto socket_address = Core::SocketAddress::local(socket_path);
    auto socket_address_un = socket_address.to_sockaddr_un().release_value();

    TRY(Core::System::bind(socket_fd, reinterpret_cast<sockaddr*>(&socket_address_un), sizeof(socket_address_un)));
    TRY(Core::System::listen(socket_fd, 16));

    return socket_fd;
}

static ErrorOr<void> launch_server(DeprecatedString const& socket_path, DeprecatedString const& pid_path, StringView server_path)
{
    auto server_fd = TRY(create_database_socket(socket_path));
    auto server_pid = TRY(Core::System::fork());

    if (server_pid == 0) {
        TRY(Core::System::setsid());
        TRY(Core::System::signal(SIGCHLD, SIG_IGN));
        server_pid = TRY(Core::System::fork());

        if (server_pid != 0) {
            auto server_pid_file = TRY(Core::Stream::File::open(pid_path, Core::Stream::OpenMode::Write));
            TRY(server_pid_file->write(DeprecatedString::number(server_pid).bytes()));

            exit(0);
        }

        server_fd = TRY(Core::System::dup(server_fd));

        auto takeover_string = DeprecatedString::formatted("{}:{}", socket_path, server_fd);
        TRY(Core::System::setenv("SOCKET_TAKEOVER"sv, takeover_string, true));

        auto arguments = Array {
            server_path,
            "--pid-file"sv,
            pid_path,
        };

        auto result = Core::System::exec(arguments[0], arguments, Core::System::SearchInPath::Yes);
        if (result.is_error()) {
            warnln("Could not launch {}: {}", server_path, result.error());
            TRY(Core::System::unlink(pid_path));
        }

        VERIFY_NOT_REACHED();
    }

    TRY(Core::System::waitpid(server_pid));
    return {};
}

static ErrorOr<bool> should_launch_server(DeprecatedString const& pid_path)
{
    if (!Core::File::exists(pid_path))
        return true;

    Optional<pid_t> pid;
    {
        auto server_pid_file = Core::Stream::File::open(pid_path, Core::Stream::OpenMode::Read);
        if (server_pid_file.is_error()) {
            warnln("Could not open SQLServer PID file '{}': {}", pid_path, server_pid_file.error());
            return server_pid_file.release_error();
        }

        auto contents = server_pid_file.value()->read_all();
        if (contents.is_error()) {
            warnln("Could not read SQLServer PID file '{}': {}", pid_path, contents.error());
            return contents.release_error();
        }

        pid = StringView { contents.value() }.to_int<pid_t>();
    }

    if (!pid.has_value()) {
        warnln("SQLServer PID file '{}' exists, but with an invalid PID", pid_path);
        TRY(Core::System::unlink(pid_path));
        return true;
    }
    if (kill(*pid, 0) < 0) {
        warnln("SQLServer PID file '{}' exists with PID {}, but process cannot be found", pid_path, *pid);
        TRY(Core::System::unlink(pid_path));
        return true;
    }

    return false;
}

ErrorOr<NonnullRefPtr<SQLClient>> SQLClient::launch_server_and_create_client(StringView server_path)
{
    auto runtime_directory = TRY(Core::StandardPaths::runtime_directory());
    auto socket_path = DeprecatedString::formatted("{}/SQLServer.socket", runtime_directory);
    auto pid_path = DeprecatedString::formatted("{}/SQLServer.pid", runtime_directory);

    if (TRY(should_launch_server(pid_path)))
        TRY(launch_server(socket_path, pid_path, server_path));

    auto socket = TRY(Core::Stream::LocalSocket::connect(move(socket_path)));
    TRY(socket->set_blocking(true));

    return adopt_nonnull_ref_or_enomem(new (nothrow) SQLClient(std::move(socket)));
}

#endif

void SQLClient::execution_error(u64 statement_id, u64 execution_id, SQLErrorCode const& code, DeprecatedString const& message)
{
    if (on_execution_error)
        on_execution_error(statement_id, execution_id, code, message);
    else
        warnln("Execution error for statement_id {}: {} ({})", statement_id, message, to_underlying(code));
}

void SQLClient::execution_success(u64 statement_id, u64 execution_id, bool has_results, size_t created, size_t updated, size_t deleted)
{
    if (on_execution_success)
        on_execution_success(statement_id, execution_id, has_results, created, updated, deleted);
    else
        outln("{} row(s) created, {} updated, {} deleted", created, updated, deleted);
}

void SQLClient::next_result(u64 statement_id, u64 execution_id, Vector<SQL::Value> const& row)
{
    if (on_next_result) {
        on_next_result(statement_id, execution_id, row);
        return;
    }

    bool first = true;
    for (auto& column : row) {
        if (!first)
            out(", ");
        out("\"{}\"", column);
        first = false;
    }
    outln();
}

void SQLClient::results_exhausted(u64 statement_id, u64 execution_id, size_t total_rows)
{
    if (on_results_exhausted)
        on_results_exhausted(statement_id, execution_id, total_rows);
    else
        outln("{} total row(s)", total_rows);
}

}
