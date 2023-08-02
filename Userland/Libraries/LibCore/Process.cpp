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
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <errno.h>
#include <spawn.h>
#include <unistd.h>

#ifdef AK_OS_SERENITY
#    include <serenity.h>
#    include <syscall.h>
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
    return "???"_short_string;
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

}
