/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

constexpr unsigned encoded_device(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

static int usage()
{
    warnln("usage: mknod <name> <c|b|p> [<major> <minor>]");
    return 1;
}

int main(int argc, char** argv)
{
    if (pledge("stdio dpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // FIXME: Add some kind of option for specifying the file permissions.
    if (argc < 3)
        return usage();

    if (argv[2][0] == 'p') {
        if (argc != 3)
            return usage();
    } else if (argc != 5) {
        return usage();
    }

    const char* name = argv[1];
    mode_t mode = 0666;
    switch (argv[2][0]) {
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
    if (argc == 5) {
        major = atoi(argv[3]);
        minor = atoi(argv[4]);
    }

    int rc = mknod(name, mode, encoded_device(major, minor));
    if (rc < 0) {
        perror("mknod");
        return 1;
    }
    return 0;
}
