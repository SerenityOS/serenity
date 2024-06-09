/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2023-2024, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/Environment.h>
#include <LibCore/File.h>
#include <LibCore/Process.h>
#include <LibCore/Socket.h>
#include <LibCore/SocketAddress.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <errno.h>
#include <signal.h>
#include <spawn.h>
#include <unistd.h>

#if defined(AK_OS_SERENITY)
#    include <serenity.h>
#    include <sys/prctl.h>
#    include <syscall.h>
#elif defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_SOLARIS)
#    include <sys/sysctl.h>
#elif defined(AK_OS_GNU_HURD)
extern "C" {
#    include <hurd.h>
}
#endif
#if defined(AK_OS_FREEBSD)
#    include <sys/user.h>
#endif

namespace Core {

struct ArgvList {
    ByteString m_path;
    Vector<char const*, 10> m_argv;

    ArgvList(ByteString path, size_t size)
        : m_path { path }
    {
        m_argv.ensure_capacity(size + 2);
        m_argv.append(m_path.characters());
    }

    void append(char const* arg)
    {
        m_argv.append(arg);
    }

    Span<char const*> get()
    {
        if (m_argv.is_empty() || m_argv.last() != nullptr)
            m_argv.append(nullptr);
        return m_argv;
    }
};

ErrorOr<Process> Process::spawn(ProcessSpawnOptions const& options)
{
#define CHECK(invocation)                  \
    if (int returned_errno = (invocation)) \
        return Error::from_errno(returned_errno);

    posix_spawn_file_actions_t spawn_actions;
    CHECK(posix_spawn_file_actions_init(&spawn_actions));
    ScopeGuard cleanup_spawn_actions = [&] {
        posix_spawn_file_actions_destroy(&spawn_actions);
    };

    if (options.working_directory.has_value()) {
#ifdef AK_OS_SERENITY
        CHECK(posix_spawn_file_actions_addchdir(&spawn_actions, options.working_directory->characters()));
#else
        // FIXME: Support ProcessSpawnOptions::working_directory n platforms that support it.
        TODO();
#endif
    }

    for (auto const& file_action : options.file_actions) {
        TRY(file_action.visit(
            [&](FileAction::OpenFile const& action) -> ErrorOr<void> {
                CHECK(posix_spawn_file_actions_addopen(
                    &spawn_actions,
                    action.fd,
                    action.path.characters(),
                    File::open_mode_to_options(action.mode | Core::File::OpenMode::KeepOnExec),
                    action.permissions));
                return {};
            },
            [&](FileAction::CloseFile const& action) -> ErrorOr<void> {
                CHECK(posix_spawn_file_actions_addclose(&spawn_actions, action.fd));
                return {};
            }));
    }

#undef CHECK

    ArgvList argv_list(options.executable, options.arguments.size());
    for (auto const& argument : options.arguments)
        argv_list.append(argument.characters());

    pid_t pid;
    if (options.search_for_executable_in_path) {
        pid = TRY(System::posix_spawnp(options.executable.view(), &spawn_actions, nullptr, const_cast<char**>(argv_list.get().data()), Core::Environment::raw_environ()));
    } else {
        pid = TRY(System::posix_spawn(options.executable.view(), &spawn_actions, nullptr, const_cast<char**>(argv_list.get().data()), Core::Environment::raw_environ()));
    }
    return Process { pid };
}

ErrorOr<pid_t> Process::spawn(StringView path, ReadonlySpan<ByteString> arguments, ByteString working_directory, KeepAsChild keep_as_child)
{
    auto process = TRY(spawn({
        .executable = path,
        .arguments = Vector<ByteString> { arguments },
        .working_directory = working_directory.is_empty() ? Optional<ByteString> {} : Optional<ByteString> { working_directory },
    }));

    if (keep_as_child == KeepAsChild::No)
        TRY(process.disown());
    else {
        // FIXME: This won't be needed if return value is changed to Process.
        process.m_should_disown = false;
    }
    return process.pid();
}

ErrorOr<pid_t> Process::spawn(StringView path, ReadonlySpan<StringView> arguments, ByteString working_directory, KeepAsChild keep_as_child)
{
    Vector<ByteString> backing_strings;
    backing_strings.ensure_capacity(arguments.size());
    for (auto const& argument : arguments)
        backing_strings.append(argument);

    auto process = TRY(spawn({
        .executable = path,
        .arguments = backing_strings,
        .working_directory = working_directory.is_empty() ? Optional<ByteString> {} : Optional<ByteString> { working_directory },
    }));

    if (keep_as_child == KeepAsChild::No)
        TRY(process.disown());
    else
        process.m_should_disown = false;
    return process.pid();
}

ErrorOr<pid_t> Process::spawn(StringView path, ReadonlySpan<char const*> arguments, ByteString working_directory, KeepAsChild keep_as_child)
{
    Vector<ByteString> backing_strings;
    backing_strings.ensure_capacity(arguments.size());
    for (auto const& argument : arguments)
        backing_strings.append(argument);

    auto process = TRY(spawn({
        .executable = path,
        .arguments = backing_strings,
        .working_directory = working_directory.is_empty() ? Optional<ByteString> {} : Optional<ByteString> { working_directory },
    }));

    if (keep_as_child == KeepAsChild::No)
        TRY(process.disown());
    else
        process.m_should_disown = false;
    return process.pid();
}

ErrorOr<String> Process::get_name()
{
#if defined(AK_OS_SERENITY)
    char buffer[BUFSIZ];
    int rc = get_process_name(buffer, BUFSIZ);
    if (rc != 0)
        return Error::from_syscall("get_process_name"sv, -rc);
    return String::from_utf8(StringView { buffer, strlen(buffer) });
#elif defined(AK_LIBC_GLIBC) || defined(AK_OS_LINUX)
    return String::from_utf8(StringView { program_invocation_name, strlen(program_invocation_name) });
#elif defined(AK_OS_BSD_GENERIC) || defined(AK_OS_HAIKU)
    auto const* progname = getprogname();
    return String::from_utf8(StringView { progname, strlen(progname) });
#else
    // FIXME: Implement Process::get_name() for other platforms.
    return "???"_string;
#endif
}

ErrorOr<void> Process::set_name([[maybe_unused]] StringView name, [[maybe_unused]] SetThreadName set_thread_name)
{
#if defined(AK_OS_SERENITY)
    int rc = set_process_name(name.characters_without_null_termination(), name.length());
    if (rc != 0)
        return Error::from_syscall("set_process_name"sv, -rc);
    if (set_thread_name == SetThreadName::No)
        return {};

    rc = prctl(PR_SET_THREAD_NAME, gettid(), name.characters_without_null_termination(), name.length());
    if (rc != 0)
        return Error::from_syscall("set_thread_name"sv, -rc);
    return {};
#else
    // FIXME: Implement Process::set_name() for other platforms.
    return {};
#endif
}

ErrorOr<bool> Process::is_being_debugged()
{
#if defined(AK_OS_LINUX)
    auto unbuffered_status_file = TRY(Core::File::open("/proc/self/status"sv, Core::File::OpenMode::Read));
    auto status_file = TRY(Core::InputBufferedFile::create(move(unbuffered_status_file)));
    auto buffer = TRY(ByteBuffer::create_uninitialized(4096));
    while (TRY(status_file->can_read_line())) {
        auto line = TRY(status_file->read_line(buffer));
        auto const parts = line.split_view(':');
        if (parts.size() < 2 || parts[0] != "TracerPid"sv)
            continue;
        auto tracer_pid = parts[1].to_number<u32>();
        return (tracer_pid != 0UL);
    }
    return false;
#elif defined(AK_OS_GNU_HURD)
    process_t proc = getproc();
    if (!MACH_PORT_VALID(proc))
        return Error::from_syscall("getproc"sv, -errno);

    int flags = PI_FETCH_TASKINFO;
    // We're going to ask the proc server for the info about our process,
    // and it is going to reply, placing the info into a buffer. It can
    // either fill in (overwrite) the buffer we provide to it (called pi_buffer
    // below), or allocate (as if with mmap or vm_allocate) a new buffer.
    // The buffer is really of type struct procinfo[], but it's transferred
    // over IPC as int[]. We pass in a double pointer (int** pi_array) that
    // initially points to our pi_buffer, but the call will update it to
    // point to the newly allocated buffer if it ends up making one.
    struct procinfo pi_buffer = {};
    int* pi_array = reinterpret_cast<int*>(&pi_buffer);
    mach_msg_type_number_t pi_array_len = sizeof(pi_buffer) / sizeof(int);
    data_t waits = nullptr;
    mach_msg_type_number_t waits_len = 0;

    kern_return_t err = proc_getprocinfo(proc, getpid(), &flags, &pi_array, &pi_array_len, &waits, &waits_len);
    mach_port_deallocate(mach_task_self(), proc);
    if (err) {
        __hurd_fail(static_cast<error_t>(err));
        return Error::from_syscall("proc_getprocinfo"sv, -errno);
    }

    // Now cast the returned buffer pointer back to struct procinfo, and
    // read the info we're interested in (the PI_TRACED flag) from there.
    VERIFY(pi_array_len >= sizeof(struct procinfo));
    struct procinfo* procinfo = reinterpret_cast<struct procinfo*>(pi_array);
    bool traced = procinfo->state & PI_TRACED;

    // If the returned buffer is not the one we allocated on the stack,
    // we should unmap it.
    if (procinfo != &pi_buffer)
        (void)System::munmap(pi_array, pi_array_len * sizeof(int));
    if (waits)
        (void)System::munmap(waits, waits_len);

    return traced;
#elif defined(AK_OS_MACOS) || defined(AK_OS_FREEBSD)
    // https://developer.apple.com/library/archive/qa/qa1361/_index.html
    int mib[4] = {};
    struct kinfo_proc info = {};
    size_t size = sizeof(info);

    // Initialize mib, which tells sysctl the info we want, in this case
    // we're looking for information about a specific process ID.
    mib[0] = CTL_KERN;
    mib[1] = KERN_PROC;
    mib[2] = KERN_PROC_PID;
    mib[3] = getpid();

    if (sysctl(mib, sizeof(mib) / sizeof(*mib), &info, &size, NULL, 0) < 0)
        return Error::from_syscall("sysctl"sv, -errno);

        // We're being debugged if the P_TRACED flag is set.
#    if defined(AK_OS_MACOS)
    return ((info.kp_proc.p_flag & P_TRACED) != 0);
#    elif defined(AK_OS_FREEBSD)
    return ((info.ki_flag & P_TRACED) != 0);
#    endif
#endif
    // FIXME: Implement this for more platforms.
    return Error::from_string_view("Platform does not support checking for debugger"sv);
}

// Forces the process to sleep until a debugger is attached, then breaks.
void Process::wait_for_debugger_and_break()
{
    bool should_print_process_info { true };
    for (;;) {
        auto check = Process::is_being_debugged();
        if (check.is_error()) {
            dbgln("Cannot wait for debugger: {}. Continuing.", check.release_error());
            return;
        }
        if (check.value()) {
            kill(getpid(), SIGTRAP);
            return;
        }
        if (should_print_process_info) {
            dbgln("Process {} with pid {} is sleeping, waiting for debugger.", Process::get_name(), getpid());
            should_print_process_info = false;
        }
        ::usleep(100 * 1000);
    }
}

ErrorOr<void> Process::disown()
{
    if (m_pid != 0 && m_should_disown) {
#ifdef AK_OS_SERENITY
        TRY(System::disown(m_pid));
#else
        // FIXME: Support disown outside Serenity.
#endif
        m_should_disown = false;
        return {};
    } else {
        return Error::from_errno(EINVAL);
    }
}

ErrorOr<bool> Process::wait_for_termination()
{
    VERIFY(m_pid > 0);

    bool exited_with_code_0 = true;
    int status;
    if (waitpid(m_pid, &status, 0) == -1)
        return Error::from_syscall("waitpid"sv, errno);

    if (WIFEXITED(status)) {
        exited_with_code_0 &= WEXITSTATUS(status) == 0;
    } else if (WIFSIGNALED(status)) {
        exited_with_code_0 = false;
    } else if (WIFSTOPPED(status)) {
        // This is only possible if the child process is being traced by us.
        VERIFY_NOT_REACHED();
    } else {
        VERIFY_NOT_REACHED();
    }

    m_should_disown = false;
    return exited_with_code_0;
}

ErrorOr<IPCProcess::ProcessAndIPCSocket> IPCProcess::spawn_and_connect_to_process(ProcessSpawnOptions const& options)
{
    int socket_fds[2] {};
    TRY(System::socketpair(AF_LOCAL, SOCK_STREAM, 0, socket_fds));

    ArmedScopeGuard guard_fd_0 { [&] { MUST(System::close(socket_fds[0])); } };
    ArmedScopeGuard guard_fd_1 { [&] { MUST(System::close(socket_fds[1])); } };

    auto& file_actions = const_cast<ProcessSpawnOptions&>(options).file_actions;
    file_actions.append(FileAction::CloseFile { socket_fds[0] });

    auto takeover_string = MUST(String::formatted("{}:{}", options.name, socket_fds[1]));
    TRY(Environment::set("SOCKET_TAKEOVER"sv, takeover_string, Environment::Overwrite::Yes));

    auto process = TRY(Process::spawn(options));

    auto ipc_socket = TRY(LocalSocket::adopt_fd(socket_fds[0]));
    guard_fd_0.disarm();
    TRY(ipc_socket->set_blocking(true));

    return ProcessAndIPCSocket { move(process), move(ipc_socket) };
}

ErrorOr<Optional<pid_t>> IPCProcess::get_process_pid(StringView process_name, StringView pid_path)
{
    if (System::stat(pid_path).is_error())
        return OptionalNone {};

    Optional<pid_t> pid;
    {
        auto pid_file = File::open(pid_path, File::OpenMode::Read);
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
        TRY(System::unlink(pid_path));
        return OptionalNone {};
    }
    if (kill(*pid, 0) < 0) {
        warnln("{} PID file '{}' exists with PID {}, but process cannot be found", process_name, pid_path, *pid);
        TRY(System::unlink(pid_path));
        return OptionalNone {};
    }

    return pid;
}

// This is heavily based on how SystemServer's Service creates its socket.
ErrorOr<int> IPCProcess::create_ipc_socket(ByteString const& socket_path)
{
    if (!System::stat(socket_path).is_error())
        TRY(System::unlink(socket_path));

#ifdef SOCK_NONBLOCK
    auto socket_fd = TRY(System::socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0));
#else
    auto socket_fd = TRY(System::socket(AF_LOCAL, SOCK_STREAM, 0));

    int option = 1;
    TRY(System::ioctl(socket_fd, FIONBIO, &option));
    TRY(System::fcntl(socket_fd, F_SETFD, FD_CLOEXEC));
#endif

#if !defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_GNU_HURD)
    TRY(System::fchmod(socket_fd, 0600));
#endif

    auto socket_address = SocketAddress::local(socket_path);
    auto socket_address_un = socket_address.to_sockaddr_un().release_value();

    TRY(System::bind(socket_fd, reinterpret_cast<sockaddr*>(&socket_address_un), sizeof(socket_address_un)));
    TRY(System::listen(socket_fd, 16));

    return socket_fd;
}

ErrorOr<IPCProcess::ProcessPaths> IPCProcess::paths_for_process(StringView process_name)
{
    auto runtime_directory = TRY(StandardPaths::runtime_directory());
    auto socket_path = ByteString::formatted("{}/{}.socket", runtime_directory, process_name);
    auto pid_path = ByteString::formatted("{}/{}.pid", runtime_directory, process_name);

    return ProcessPaths { move(socket_path), move(pid_path) };
}

ErrorOr<IPCProcess::ProcessAndIPCSocket> IPCProcess::spawn_singleton_and_connect_to_process(ProcessSpawnOptions const& options)
{
    auto [socket_path, pid_path] = TRY(paths_for_process(options.name));
    Process process { -1 };

    if (auto existing_pid = TRY(get_process_pid(options.name, pid_path)); existing_pid.has_value()) {
        process = Process { *existing_pid };
    } else {
        auto ipc_fd = TRY(create_ipc_socket(socket_path));

        sigset_t original_set;
        sigset_t setting_set;
        sigfillset(&setting_set);
        (void)pthread_sigmask(SIG_BLOCK, &setting_set, &original_set);

        // FIXME: Roll this daemon implementation into `Process::disown`.
        if (auto pid = TRY(System::fork()); pid == 0) {
            (void)pthread_sigmask(SIG_SETMASK, &original_set, nullptr);
            TRY(System::setsid());
            TRY(System::signal(SIGCHLD, SIG_IGN));

            auto& arguments = const_cast<Vector<ByteString>&>(options.arguments);
            arguments.append("--pid-file"sv);
            arguments.append(pid_path);

            auto takeover_string = ByteString::formatted("{}:{}", options.name, TRY(System::dup(ipc_fd)));
            TRY(Environment::set("SOCKET_TAKEOVER"sv, takeover_string, Environment::Overwrite::Yes));

            auto process = TRY(Process::spawn(options));
            {
                auto pid_file = TRY(File::open(pid_path, File::OpenMode::Write));
                TRY(pid_file->write_until_depleted(ByteString::number(process.pid())));
            }

            TRY(System::kill(getpid(), SIGTERM));
        } else {
            auto wait_err = System::waitpid(pid);
            (void)pthread_sigmask(SIG_SETMASK, &original_set, nullptr);
            TRY(wait_err);
        }

        auto pid = TRY(get_process_pid(options.name, pid_path));
        VERIFY(pid.has_value());

        process = Process { *pid };
    }

    auto ipc_socket = TRY(LocalSocket::connect(socket_path));
    TRY(ipc_socket->set_blocking(true));

    return ProcessAndIPCSocket { move(process), move(ipc_socket) };
}

}
