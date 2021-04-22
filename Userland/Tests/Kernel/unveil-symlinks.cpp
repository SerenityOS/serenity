/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

int main()
{
    rmdir("/tmp/foo/1");
    rmdir("/tmp/foo");
    unlink("/tmp/bar");

    if (mkdir("/tmp/foo", 0755) < 0) {
        perror("mkdir");
        return 1;
    }

    if (mkdir("/tmp/foo/1", 0755) < 0) {
        perror("mkdir");
        return 1;
    }

    if (symlink("/tmp/foo", "/tmp/bar")) {
        perror("symlink");
        return 1;
    }

    if (unveil("/tmp/foo", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    int fd = open("/tmp/foo/1", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }
    close(fd);

    fd = open("/tmp/bar/1", O_RDONLY);
    if (fd >= 0) {
        fprintf(stderr, "FAIL, symlink was not unveiled\n");
        return 1;
    }

    if (chdir("/tmp")) {
        perror("chdir");
        return 1;
    }

    fd = open("./foo/1", O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }
    close(fd);

    fd = open("./bar/1", O_RDONLY);
    if (fd >= 0) {
        fprintf(stderr, "FAIL, symlink was not unveiled\n");
        return 1;
    }

    printf("PASS\n");
    return 0;
}
