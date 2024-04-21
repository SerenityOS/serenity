/*
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Directory.h>
#include <LibCore/Environment.h>
#include <LibCore/SingletonProcess.h>
#include <LibCore/SocketAddress.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <signal.h>

namespace Core::Detail {

static ErrorOr<Optional<pid_t>> get_process_pid(StringView process_name, ByteString const& pid_path)
{
    if (Core::System::stat(pid_path).is_error())
        return OptionalNone {};

    Optional<pid_t> pid;
    {
        auto pid_file = Core::File::open(pid_path, Core::File::OpenMode::Read);
        if (pid_file.is_error()) {
            warnln("Could not open {} PID file '{}': {}", process_name, pid_path, pid_file.error());
            return pid_file.release_error();
        }

        auto contents = pid_file.value()->read_until_eof();
        if (contents.is_error()) {
            warnln("Could not read {} PID file '{}': {}", process_name, pid_path, contents.error());
            return contents.release_error();
        }

        pid = StringView { contents.value() }.to_number<pid_t>();
    }

    if (!pid.has_value()) {
        warnln("{} PID file '{}' exists, but with an invalid PID", process_name, pid_path);
        TRY(Core::System::unlink(pid_path));
        return OptionalNone {};
    }
    if (kill(*pid, 0) < 0) {
        warnln("{} PID file '{}' exists with PID {}, but process cannot be found", process_name, pid_path, *pid);
        TRY(Core::System::unlink(pid_path));
        return OptionalNone {};
    }

    return pid;
}

// This is heavily based on how SystemServer's Service creates its socket.
static ErrorOr<int> create_ipc_socket(ByteString const& socket_path)
{
    if (!Core::System::stat(socket_path).is_error())
        TRY(Core::System::unlink(socket_path));

#ifdef SOCK_NONBLOCK
    auto socket_fd = TRY(Core::System::socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0));
#else
    auto socket_fd = TRY(Core::System::socket(AF_LOCAL, SOCK_STREAM, 0));

    int option = 1;
    TRY(Core::System::ioctl(socket_fd, FIONBIO, &option));
    TRY(Core::System::fcntl(socket_fd, F_SETFD, FD_CLOEXEC));
#endif

#if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_GNU_HURD)
    TRY(Core::System::fchmod(socket_fd, 0600));
#endif

    auto socket_address = Core::SocketAddress::local(socket_path);
    auto socket_address_un = socket_address.to_sockaddr_un().release_value();

    TRY(Core::System::bind(socket_fd, reinterpret_cast<sockaddr*>(&socket_address_un), sizeof(socket_address_un)));
    TRY(Core::System::listen(socket_fd, 16));

    return socket_fd;
}

static ErrorOr<pid_t> launch_process(StringView process_name, ByteString const& socket_path, ByteString const& pid_path, ReadonlySpan<ByteString> candidate_process_paths, ReadonlySpan<ByteString> command_line_arguments)
{
    auto ipc_fd_or_error = create_ipc_socket(socket_path);
    if (ipc_fd_or_error.is_error()) {
        warnln("Failed to create an IPC socket for {} at {}: {}", process_name, socket_path, ipc_fd_or_error.error());
        return ipc_fd_or_error.release_error();
    }

    sigset_t original_set;
    sigset_t setting_set;
    sigfillset(&setting_set);
    (void)pthread_sigmask(SIG_BLOCK, &setting_set, &original_set);

    auto ipc_fd = ipc_fd_or_error.value();
    auto pid = TRY(Core::System::fork());

    if (pid == 0) {
        (void)pthread_sigmask(SIG_SETMASK, &original_set, nullptr);
        TRY(Core::System::setsid());
        TRY(Core::System::signal(SIGCHLD, SIG_IGN));

        pid = TRY(Core::System::fork());

        if (pid != 0) {
            auto pid_file = TRY(Core::File::open(pid_path, Core::File::OpenMode::Write));
            TRY(pid_file->write_until_depleted(ByteString::number(pid)));

            TRY(Core::System::kill(getpid(), SIGTERM));
        }

        ipc_fd = TRY(Core::System::dup(ipc_fd));

        auto takeover_string = ByteString::formatted("{}:{}", process_name, ipc_fd);
        TRY(Core::Environment::set("SOCKET_TAKEOVER"sv, takeover_string, Core::Environment::Overwrite::Yes));

        ErrorOr<void> result;

        Vector<StringView> arguments {
            ""sv, // placeholder for the candidate path
            "--pid-file"sv,
            pid_path,
        };

        for (auto const& argument : command_line_arguments)
            arguments.append(argument);

        for (auto const& process_path : candidate_process_paths) {
            arguments[0] = process_path;

            result = Core::System::exec(arguments[0], arguments, Core::System::SearchInPath::Yes);
            if (!result.is_error())
                break;
        }

        if (result.is_error()) {
            warnln("Could not launch any of {}: {}", candidate_process_paths, result.error());
            TRY(Core::System::unlink(pid_path));
        }

        VERIFY_NOT_REACHED();
    }

    VERIFY(pid > 0);

    auto wait_err = Core::System::waitpid(pid);
    (void)pthread_sigmask(SIG_SETMASK, &original_set, nullptr);
    TRY(wait_err);

    auto process_pid = TRY(get_process_pid(process_name, pid_path));
    VERIFY(process_pid.has_value());

    return *process_pid;
}

struct ProcessPaths {
    ByteString socket_path;
    ByteString pid_path;
};
static ErrorOr<ProcessPaths> paths_for_process(StringView process_name)
{
    auto runtime_directory = TRY(Core::StandardPaths::runtime_directory());
    auto socket_path = ByteString::formatted("{}/{}.socket", runtime_directory, process_name);
    auto pid_path = ByteString::formatted("{}/{}.pid", runtime_directory, process_name);

    return ProcessPaths { move(socket_path), move(pid_path) };
}

ErrorOr<ProcessSocket> launch_and_connect_to_process(StringView process_name, ReadonlySpan<ByteString> candidate_process_paths, ReadonlySpan<ByteString> command_line_arguments)
{
    auto [socket_path, pid_path] = TRY(paths_for_process(process_name));
    pid_t pid = 0;

    if (auto existing_pid = TRY(Detail::get_process_pid(process_name, pid_path)); existing_pid.has_value())
        pid = *existing_pid;
    else
        pid = TRY(Detail::launch_process(process_name, socket_path, pid_path, candidate_process_paths, command_line_arguments));

    auto socket = TRY(Core::LocalSocket::connect(socket_path));
    TRY(socket->set_blocking(true));

    return ProcessSocket { move(socket), pid };
}

}
