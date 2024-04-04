/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <LibSQL/SQLClient.h>

#if !defined(AK_OS_SERENITY)
#    include <LibCore/Directory.h>
#    include <LibCore/Environment.h>
#    include <LibCore/SocketAddress.h>
#    include <LibCore/StandardPaths.h>
#    include <LibCore/System.h>
#    include <LibFileSystem/FileSystem.h>
#    include <signal.h>
#endif

namespace SQL {

#if !defined(AK_OS_SERENITY)

// This is heavily based on how SystemServer's Service creates its socket.
static ErrorOr<int> create_database_socket(ByteString const& socket_path)
{
    if (FileSystem::exists(socket_path))
        TRY(Core::System::unlink(socket_path));

#    ifdef SOCK_NONBLOCK
    auto socket_fd = TRY(Core::System::socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0));
#    else
    auto socket_fd = TRY(Core::System::socket(AF_LOCAL, SOCK_STREAM, 0));

    int option = 1;
    TRY(Core::System::ioctl(socket_fd, FIONBIO, &option));
    TRY(Core::System::fcntl(socket_fd, F_SETFD, FD_CLOEXEC));
#    endif

#    if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_GNU_HURD)
    TRY(Core::System::fchmod(socket_fd, 0600));
#    endif

    auto socket_address = Core::SocketAddress::local(socket_path);
    auto socket_address_un = socket_address.to_sockaddr_un().release_value();

    TRY(Core::System::bind(socket_fd, reinterpret_cast<sockaddr*>(&socket_address_un), sizeof(socket_address_un)));
    TRY(Core::System::listen(socket_fd, 16));

    return socket_fd;
}

static ErrorOr<void> launch_server(ByteString const& socket_path, ByteString const& pid_path, Vector<ByteString> candidate_server_paths)
{
    auto server_fd_or_error = create_database_socket(socket_path);
    if (server_fd_or_error.is_error()) {
        warnln("Failed to create a database socket at {}: {}", socket_path, server_fd_or_error.error());
        return server_fd_or_error.release_error();
    }
    auto server_fd = server_fd_or_error.value();
    sigset_t original_set;
    sigset_t setting_set;
    sigfillset(&setting_set);
    (void)pthread_sigmask(SIG_BLOCK, &setting_set, &original_set);
    auto server_pid = TRY(Core::System::fork());

    if (server_pid == 0) {
        (void)pthread_sigmask(SIG_SETMASK, &original_set, nullptr);
        TRY(Core::System::setsid());
        TRY(Core::System::signal(SIGCHLD, SIG_IGN));
        server_pid = TRY(Core::System::fork());

        if (server_pid != 0) {
            auto server_pid_file = TRY(Core::File::open(pid_path, Core::File::OpenMode::Write));
            TRY(server_pid_file->write_until_depleted(ByteString::number(server_pid)));

            TRY(Core::System::kill(getpid(), SIGTERM));
        }

        server_fd = TRY(Core::System::dup(server_fd));

        auto takeover_string = ByteString::formatted("SQLServer:{}", server_fd);
        TRY(Core::Environment::set("SOCKET_TAKEOVER"sv, takeover_string, Core::Environment::Overwrite::Yes));

        ErrorOr<void> result;
        for (auto const& server_path : candidate_server_paths) {
            auto arguments = Array {
                server_path.view(),
                "--pid-file"sv,
                pid_path,
            };
            result = Core::System::exec(arguments[0], arguments, Core::System::SearchInPath::Yes);
            if (!result.is_error())
                break;
        }
        if (result.is_error()) {
            warnln("Could not launch any of {}: {}", candidate_server_paths, result.error());
            TRY(Core::System::unlink(pid_path));
        }

        VERIFY_NOT_REACHED();
    }
    VERIFY(server_pid > 0);

    auto wait_err = Core::System::waitpid(server_pid);
    (void)pthread_sigmask(SIG_SETMASK, &original_set, nullptr);
    if (wait_err.is_error())
        return wait_err.release_error();
    return {};
}

static ErrorOr<bool> should_launch_server(ByteString const& pid_path)
{
    if (!FileSystem::exists(pid_path))
        return true;

    Optional<pid_t> pid;
    {
        auto server_pid_file = Core::File::open(pid_path, Core::File::OpenMode::Read);
        if (server_pid_file.is_error()) {
            warnln("Could not open SQLServer PID file '{}': {}", pid_path, server_pid_file.error());
            return server_pid_file.release_error();
        }

        auto contents = server_pid_file.value()->read_until_eof();
        if (contents.is_error()) {
            warnln("Could not read SQLServer PID file '{}': {}", pid_path, contents.error());
            return contents.release_error();
        }

        pid = StringView { contents.value() }.to_number<pid_t>();
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

ErrorOr<NonnullRefPtr<SQLClient>> SQLClient::launch_server_and_create_client(Vector<ByteString> candidate_server_paths)
{
    auto runtime_directory = TRY(Core::StandardPaths::runtime_directory());
    auto socket_path = ByteString::formatted("{}/SQLServer.socket", runtime_directory);
    auto pid_path = ByteString::formatted("{}/SQLServer.pid", runtime_directory);

    if (TRY(should_launch_server(pid_path)))
        TRY(launch_server(socket_path, pid_path, move(candidate_server_paths)));

    auto socket = TRY(Core::LocalSocket::connect(move(socket_path)));
    TRY(socket->set_blocking(true));

    return adopt_nonnull_ref_or_enomem(new (nothrow) SQLClient(move(socket)));
}

#endif

void SQLClient::execution_success(u64 statement_id, u64 execution_id, Vector<ByteString> const& column_names, bool has_results, size_t created, size_t updated, size_t deleted)
{
    if (!on_execution_success) {
        outln("{} row(s) created, {} updated, {} deleted", created, updated, deleted);
        return;
    }

    ExecutionSuccess success {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .column_names = move(const_cast<Vector<ByteString>&>(column_names)),
        .has_results = has_results,
        .rows_created = created,
        .rows_updated = updated,
        .rows_deleted = deleted,
    };

    on_execution_success(move(success));
}

void SQLClient::execution_error(u64 statement_id, u64 execution_id, SQLErrorCode const& code, ByteString const& message)
{
    if (!on_execution_error) {
        warnln("Execution error for statement_id {}: {} ({})", statement_id, message, to_underlying(code));
        return;
    }

    ExecutionError error {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .error_code = code,
        .error_message = move(const_cast<ByteString&>(message)),
    };

    on_execution_error(move(error));
}

void SQLClient::next_result(u64 statement_id, u64 execution_id, Vector<Value> const& row)
{
    ScopeGuard guard { [&]() { async_ready_for_next_result(statement_id, execution_id); } };

    if (!on_next_result) {
        StringBuilder builder;
        builder.join(", "sv, row, "\"{}\""sv);
        outln("{}", builder.string_view());
        return;
    }

    ExecutionResult result {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .values = move(const_cast<Vector<Value>&>(row)),
    };

    on_next_result(move(result));
}

void SQLClient::results_exhausted(u64 statement_id, u64 execution_id, size_t total_rows)
{
    if (!on_results_exhausted) {
        outln("{} total row(s)", total_rows);
        return;
    }

    ExecutionComplete success {
        .statement_id = statement_id,
        .execution_id = execution_id,
        .total_rows = total_rows,
    };

    on_results_exhausted(move(success));
}

}
