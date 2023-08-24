/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <errno.h>
#include <spawn.h>
#include <unistd.h>

#if defined(AK_OS_SERENITY)
#    include <serenity.h>
#    include <syscall.h>
#elif defined(AK_OS_BSD_GENERIC) && !defined(AK_OS_SOLARIS)
#    include <sys/sysctl.h>
#endif

namespace Core {

struct ArgvList {
    DeprecatedString m_path;
    DeprecatedString m_working_directory;
    Vector<char const*, 10> m_argv;

    ArgvList(DeprecatedString path, size_t size)
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

    void set_working_directory(DeprecatedString const& working_directory)
    {
        m_working_directory = working_directory;
    }

    ErrorOr<pid_t> spawn(Process::KeepAsChild keep_as_child)
    {
#ifdef AK_OS_SERENITY
        posix_spawn_file_actions_t spawn_actions;
        posix_spawn_file_actions_init(&spawn_actions);
        ScopeGuard cleanup_spawn_actions = [&] {
            posix_spawn_file_actions_destroy(&spawn_actions);
        };
        if (!m_working_directory.is_empty())
            posix_spawn_file_actions_addchdir(&spawn_actions, m_working_directory.characters());

        auto pid = TRY(System::posix_spawn(m_path.view(), &spawn_actions, nullptr, const_cast<char**>(get().data()), System::environment()));
        if (keep_as_child == Process::KeepAsChild::No)
            TRY(System::disown(pid));
#else
        auto pid = TRY(System::posix_spawn(m_path.view(), nullptr, nullptr, const_cast<char**>(get().data()), System::environment()));
        // FIXME: Support keep_as_child outside Serenity.
        (void)keep_as_child;
#endif
        return pid;
    }
};

ErrorOr<pid_t> Process::spawn(StringView path, ReadonlySpan<DeprecatedString> arguments, DeprecatedString working_directory, KeepAsChild keep_as_child)
{
    ArgvList argv { path, arguments.size() };
    for (auto const& arg : arguments)
        argv.append(arg.characters());
    argv.set_working_directory(working_directory);
    return argv.spawn(keep_as_child);
}

ErrorOr<pid_t> Process::spawn(StringView path, ReadonlySpan<StringView> arguments, DeprecatedString working_directory, KeepAsChild keep_as_child)
{
    Vector<DeprecatedString> backing_strings;
    backing_strings.ensure_capacity(arguments.size());
    ArgvList argv { path, arguments.size() };
    for (auto const& arg : arguments) {
        backing_strings.append(arg);
        argv.append(backing_strings.last().characters());
    }
    argv.set_working_directory(working_directory);
    return argv.spawn(keep_as_child);
}

ErrorOr<pid_t> Process::spawn(StringView path, ReadonlySpan<char const*> arguments, DeprecatedString working_directory, KeepAsChild keep_as_child)
{
    ArgvList argv { path, arguments.size() };
    for (auto arg : arguments)
        argv.append(arg);
    argv.set_working_directory(working_directory);
    return argv.spawn(keep_as_child);
}

ErrorOr<String> Process::get_name()
{
#if defined(AK_OS_SERENITY)
    char buffer[BUFSIZ];
    int rc = get_process_name(buffer, BUFSIZ);
    if (rc != 0)
        return Error::from_syscall("get_process_name"sv, -rc);
    return String::from_utf8(StringView { buffer, strlen(buffer) });
#elif defined(AK_OS_LINUX)
    return String::from_utf8(StringView { program_invocation_name, strlen(program_invocation_name) });
#elif defined(AK_OS_BSD_GENERIC)
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

    rc = syscall(SC_set_thread_name, gettid(), name.characters_without_null_termination(), name.length());
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
        auto tracer_pid = parts[1].to_uint<u32>();
        return (tracer_pid != 0UL);
    }
    return false;
#elif defined(AK_OS_MACOS)
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
    return ((info.kp_proc.p_flag & P_TRACED) != 0);
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

}
