/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <LibFileSystem/FileSystem.h>
#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

TEST_CASE(test_change_file_contents)
{
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT(fd != -1);
    ftruncate(fd, 0);
    EXPECT(fchmod(fd, 06755) != -1);

    char buffer[8] {};
    write(fd, buffer, sizeof(buffer));

    struct stat s;
    EXPECT(fstat(fd, &s) != -1);
    close(fd);
    unlink(path);

    EXPECT(!(s.st_mode & S_ISUID));
    EXPECT(!(s.st_mode & S_ISGID));
}

TEST_CASE(test_change_file_ownership)
{
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT(fd != -1);
    ftruncate(fd, 0);
    EXPECT(fchmod(fd, 06755) != -1);

    fchown(fd, getuid(), getgid());

    struct stat s;
    EXPECT(fstat(fd, &s) != -1);
    close(fd);
    unlink(path);

    EXPECT(!(s.st_mode & S_ISUID));
    EXPECT(!(s.st_mode & S_ISGID));
}

TEST_CASE(test_change_file_permissions)
{
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT(fd != -1);
    ftruncate(fd, 0);
    EXPECT(fchmod(fd, 06755) != -1);

    fchmod(fd, 0755);

    struct stat s;
    EXPECT(fstat(fd, &s) != -1);
    close(fd);
    unlink(path);

    EXPECT(!(s.st_mode & S_ISUID));
    EXPECT(!(s.st_mode & S_ISGID));
}

TEST_CASE(test_change_file_location)
{
    char path[] = "/tmp/suid.XXXXXX";
    auto fd = mkstemp(path);
    EXPECT(fd != -1);
    ftruncate(fd, 0);
    EXPECT(fchmod(fd, 06755) != -1);

    auto suid_path = TRY_OR_FAIL(FileSystem::read_link(ByteString::formatted("/proc/{}/fd/{}", getpid(), fd)));
    EXPECT(suid_path.characters());
    auto new_path = ByteString::formatted("{}.renamed", suid_path);

    rename(suid_path.characters(), new_path.characters());

    struct stat s;
    EXPECT(lstat(new_path.characters(), &s) != -1);
    close(fd);
    unlink(path);

    // Renamed file should retain set-uid/set-gid permissions
    EXPECT(s.st_mode & S_ISUID);
    EXPECT(s.st_mode & S_ISGID);

    unlink(new_path.characters());
}
