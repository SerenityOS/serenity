/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <sys/wait.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (arguments.strings.size() < 3) {
        warnln("usage: flock <path> <command...>");
        return 1;
    }

    pid_t child_pid = TRY(Core::System::posix_spawnp(arguments.strings[2], nullptr, nullptr, &arguments.argv[2], environ));
    auto [_, status] = TRY(Core::System::waitpid(child_pid));

    return WEXITSTATUS(status);
}
