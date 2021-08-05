/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/Process.h>
#include <errno.h>
#include <spawn.h>

#ifdef __serenity__
#    include <serenity.h>
#endif

extern char** environ;

namespace Core {

pid_t Process::spawn(StringView path)
{
    String path_string = path;

    pid_t pid;
    char const* argv[] = { path_string.characters(), nullptr };
    if ((errno = posix_spawn(&pid, path_string.characters(), nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("Process::spawn posix_spawn");
    } else {
#ifdef __serenity__
        if (disown(pid) < 0)
            perror("Process::spawn disown");
#endif
    }
    return pid;
}

}
