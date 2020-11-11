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

#include <assert.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

static void test_change_file_contents()
{
    const char* path = "suid";

    int fd = open(path, O_CREAT | O_RDWR, 06755);
    assert(fd != -1);
    ftruncate(fd, 0);

    char buffer[8];
    memset(&buffer, 0, sizeof(buffer));
    write(fd, buffer, sizeof(buffer));

    struct stat s;
    assert(fstat(fd, &s) != -1);
    close(fd);

    assert(s.st_mode & ~S_ISUID);
    assert(s.st_mode & ~S_ISGID);

    unlink(path);
}

static void test_change_file_ownership()
{
    const char* path = "suid";

    int fd = open(path, O_CREAT | O_RDWR, 06755);
    assert(fd != -1);
    ftruncate(fd, 0);

    fchown(fd, getuid(), getgid());

    struct stat s;
    assert(fstat(fd, &s) != -1);
    close(fd);

    assert(s.st_mode & ~S_ISUID);
    assert(s.st_mode & ~S_ISGID);

    unlink(path);
}

static void test_change_file_permissions()
{
    const char* path = "suid";

    int fd = open(path, O_CREAT | O_RDWR, 06755);
    assert(fd != -1);
    ftruncate(fd, 0);

    fchmod(fd, 0755);

    struct stat s;
    assert(fstat(fd, &s) != -1);
    close(fd);

    assert(s.st_mode & ~S_ISUID);
    assert(s.st_mode & ~S_ISGID);

    unlink(path);
}

int main()
{
#define RUNTEST(x)                      \
    {                                   \
        printf("Running " #x " ...\n"); \
        x();                            \
        printf("Success!\n");           \
    }
    RUNTEST(test_change_file_contents);
    RUNTEST(test_change_file_ownership);
    RUNTEST(test_change_file_permissions);
    printf("PASS\n");

    return 0;
}
