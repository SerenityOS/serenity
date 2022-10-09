/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/Process.h>
#include <LibCore/System.h>
#include <errno.h>
#include <spawn.h>

#ifdef AK_OS_SERENITY
#    include <serenity.h>
#endif

extern char** environ;

namespace Core {

struct ArgvList {
    String m_path;
    Vector<char const*, 10> m_argv;

    ArgvList(String path, size_t size)
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

    ErrorOr<pid_t> spawn()
    {
        auto pid = TRY(System::posix_spawn(m_path.view(), nullptr, nullptr, const_cast<char**>(get().data()), environ));
#ifdef AK_OS_SERENITY
        TRY(System::disown(pid));
#endif
        return pid;
    }
};

ErrorOr<pid_t> Process::spawn(StringView path, Span<String const> arguments)
{
    ArgvList argv { path, arguments.size() };
    for (auto const& arg : arguments)
        argv.append(arg.characters());
    return argv.spawn();
}

ErrorOr<pid_t> Process::spawn(StringView path, Span<StringView const> arguments)
{
    Vector<String> backing_strings;
    backing_strings.ensure_capacity(arguments.size());
    ArgvList argv { path, arguments.size() };
    for (auto const& arg : arguments) {
        backing_strings.append(arg);
        argv.append(backing_strings.last().characters());
    }
    return argv.spawn();
}

ErrorOr<pid_t> Process::spawn(StringView path, Span<char const* const> arguments)
{
    ArgvList argv { path, arguments.size() };
    for (auto arg : arguments)
        argv.append(arg);
    return argv.spawn();
}

}
