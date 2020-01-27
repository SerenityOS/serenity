/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>

inline constexpr unsigned encoded_device(unsigned major, unsigned minor)
{
    return (minor & 0xff) | (major << 8) | ((minor & ~0xff) << 12);
}

static int usage()
{
    printf("usage: mknod <name> <c|b|p> <major> <minor>\n");
    return 0;
}

int main(int argc, char** argv)
{
    if (pledge("stdio dpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // FIXME: When invoked with type "p", no need for major/minor numbers.
    // FIXME: Add some kind of option for specifying the file permissions.
    if (argc != 5)
        return usage();

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

    int major = atoi(argv[3]);
    int minor = atoi(argv[4]);

    int rc = mknod(name, mode, encoded_device(major, minor));
    if (rc < 0) {
        perror("mknod");
        return 1;
    }
    return 0;
}
