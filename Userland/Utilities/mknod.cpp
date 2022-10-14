/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>

static int usage()
{
    warnln("usage: mknod <name> <c|b|p> [<major> <minor>]");
    return 1;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio dpath"));

    // FIXME: Add some kind of option for specifying the file permissions.
    if (arguments.strings.size() < 3)
        return usage();

    if (arguments.strings[2].starts_with('p')) {
        if (arguments.strings.size() != 3)
            return usage();
    } else if (arguments.strings.size() != 5) {
        return usage();
    }

    auto name = arguments.strings[1];
    mode_t mode = 0666;
    switch (arguments.strings[2][0]) {
    case 'c':
    case 'u':
        mode |= S_IFCHR;
        break;
    case 'b':
        mode |= S_IFBLK;
        break;
    case 'p':
        mode |= S_IFIFO;
        break;
    default:
        return usage();
    }

    int major = 0;
    int minor = 0;
    if (arguments.strings.size() == 5) {
        major = atoi(arguments.argv[3]);
        minor = atoi(arguments.argv[4]);
    }

    TRY(Core::System::mknod(name, mode, makedev(major, minor)));

    return 0;
}
