/*
 * Copyright (c) 2020, the SerenityOS developers.
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
