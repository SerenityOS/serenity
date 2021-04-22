/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/File.h>
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
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    assert(fd != -1);
    ftruncate(fd, 0);
    assert(fchmod(fd, 06755) != -1);

    char buffer[8];
    memset(&buffer, 0, sizeof(buffer));
    write(fd, buffer, sizeof(buffer));

    struct stat s;
    assert(fstat(fd, &s) != -1);
    close(fd);
    unlink(path);

    assert(!(s.st_mode & S_ISUID));
    assert(!(s.st_mode & S_ISGID));
}

static void test_change_file_ownership()
{
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    assert(fd != -1);
    ftruncate(fd, 0);
    assert(fchmod(fd, 06755) != -1);

    fchown(fd, getuid(), getgid());

    struct stat s;
    assert(fstat(fd, &s) != -1);
    close(fd);
    unlink(path);

    assert(!(s.st_mode & S_ISUID));
    assert(!(s.st_mode & S_ISGID));
}

static void test_change_file_permissions()
{
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    assert(fd != -1);
    ftruncate(fd, 0);
    assert(fchmod(fd, 06755) != -1);

    fchmod(fd, 0755);

    struct stat s;
    assert(fstat(fd, &s) != -1);
    close(fd);
    unlink(path);

    assert(!(s.st_mode & S_ISUID));
    assert(!(s.st_mode & S_ISGID));
}

static void test_change_file_location()
{
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    assert(fd != -1);
    ftruncate(fd, 0);
    assert(fchmod(fd, 06755) != -1);

    auto suid_path = Core::File::read_link(String::formatted("/proc/{}/fd/{}", getpid(), fd));
    assert(suid_path.characters());
    auto new_path = String::formatted("{}.renamed", suid_path);

    rename(suid_path.characters(), new_path.characters());

    struct stat s;
    assert(lstat(new_path.characters(), &s) != -1);
    close(fd);
    unlink(path);

    // renamed file should retain set-uid/set-gid permissions
    assert(s.st_mode & S_ISUID);
    assert(s.st_mode & S_ISGID);

    unlink(new_path.characters());
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
    RUNTEST(test_change_file_location);
    printf("PASS\n");

    return 0;
}
