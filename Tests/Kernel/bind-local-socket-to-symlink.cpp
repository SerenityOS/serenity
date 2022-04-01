/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

int main(int, char**)
{
    constexpr char const* path = "/tmp/foo";
    int rc = symlink("bar", path);
    if (rc < 0) {
        perror("symlink");
        return 1;
    }

    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        perror("socket");
        return 1;
    }

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    VERIFY(strlcpy(addr.sun_path, path, sizeof(addr.sun_path)) < sizeof(addr.sun_path));

    rc = bind(fd, (struct sockaddr*)(&addr), sizeof(addr));
    if (rc < 0 && errno == EADDRINUSE) {
        printf("PASS\n");
        return 0;
    }

    return 1;
}
