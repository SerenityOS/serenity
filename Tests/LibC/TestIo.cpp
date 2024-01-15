/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <LibFileSystem/FileSystem.h>
#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <sys/wait.h>
#include <unistd.h>

#define EXPECT_ERROR_2(err, syscall, arg1, arg2)                                                                                   \
    do {                                                                                                                           \
        rc = syscall(arg1, arg2);                                                                                                  \
        EXPECT(rc < 0);                                                                                                            \
        EXPECT_EQ(errno, err);                                                                                                     \
        if (rc >= 0 || errno != err) {                                                                                             \
            warnln(__FILE__ ":{}: Expected " #err ": " #syscall "({}, {}), got rc={}, errno={}", __LINE__, arg1, arg2, rc, errno); \
        }                                                                                                                          \
    } while (0)

#define EXPECT_ERROR_3(err, syscall, arg1, arg2, arg3)                                                                                       \
    do {                                                                                                                                     \
        rc = syscall(arg1, arg2, arg3);                                                                                                      \
        EXPECT(rc < 0);                                                                                                                      \
        EXPECT_EQ(errno, err);                                                                                                               \
        if (rc >= 0 || errno != err) {                                                                                                       \
            warnln(__FILE__ ":{}: Expected " #err ": " #syscall "({}, {}, {}), got rc={}, errno={}", __LINE__, arg1, arg2, arg3, rc, errno); \
        }                                                                                                                                    \
    } while (0)

TEST_CASE(read_from_directory)
{
    char buffer[BUFSIZ];
    int fd = open("/", O_DIRECTORY | O_RDONLY);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EISDIR, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    VERIFY(rc == 0);
}

TEST_CASE(write_to_directory)
{
    char str[] = "oh frick";
    int fd = open("/", O_DIRECTORY | O_RDONLY);
    if (fd < 0)
        perror("open");
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, write, fd, str, sizeof(str));
    rc = close(fd);
    VERIFY(rc == 0);
}

TEST_CASE(read_from_writeonly)
{
    char buffer[BUFSIZ];
    int fd = open("/tmp/xxxx123", O_CREAT | O_WRONLY);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    VERIFY(rc == 0);
    rc = unlink("/tmp/xxxx123");
    VERIFY(rc == 0);
}

TEST_CASE(write_to_readonly)
{
    char str[] = "hello";
    int fd = open("/tmp/abcd123", O_CREAT | O_RDONLY);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, write, fd, str, sizeof(str));
    rc = close(fd);
    VERIFY(rc == 0);
    rc = unlink("/tmp/abcd123");
    VERIFY(rc == 0);
}

TEST_CASE(read_past_eof)
{
    char buffer[BUFSIZ];
    int fd = open("/home/anon/README.md", O_RDONLY);
    if (fd < 0)
        perror("open");
    VERIFY(fd >= 0);
    int rc;
    rc = lseek(fd, 99999, SEEK_SET);
    if (rc < 0)
        perror("lseek");
    rc = read(fd, buffer, sizeof(buffer));
    if (rc < 0)
        perror("read");
    if (rc > 0)
        warnln("read {} bytes past EOF", rc);
    rc = close(fd);
    VERIFY(rc == 0);
}

TEST_CASE(ftruncate_readonly)
{
    int fd = open("/tmp/trunctest", O_RDONLY | O_CREAT, 0666);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EBADF, ftruncate, fd, 0);
    rc = close(fd);
    VERIFY(rc == 0);
    rc = unlink("/tmp/trunctest");
    VERIFY(rc == 0);
}

TEST_CASE(ftruncate_negative)
{
    int fd = open("/tmp/trunctest", O_RDWR | O_CREAT, 0666);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EINVAL, ftruncate, fd, -1);
    rc = close(fd);
    VERIFY(rc == 0);
    rc = unlink("/tmp/trunctest");
    VERIFY(rc == 0);
}

TEST_CASE(mmap_directory)
{
    int fd = open("/tmp", O_RDONLY | O_DIRECTORY);
    VERIFY(fd >= 0);
    auto* ptr = mmap(nullptr, 4096, PROT_READ, MAP_SHARED, fd, 0);
    EXPECT_EQ(ptr, MAP_FAILED);
    if (ptr != MAP_FAILED) {
        warnln("Boo! mmap() of a directory succeeded!");
    }
    EXPECT_EQ(errno, ENODEV);
    if (errno != ENODEV) {
        warnln("Boo! mmap() of a directory gave errno={} instead of ENODEV!", errno);
        return;
    }
    close(fd);
}

TEST_CASE(tmpfs_read_past_end)
{
    int fd = open("/tmp/x", O_RDWR | O_CREAT | O_TRUNC, 0600);
    VERIFY(fd >= 0);

    int rc = ftruncate(fd, 1);
    VERIFY(rc == 0);

    rc = lseek(fd, 4096, SEEK_SET);
    VERIFY(rc == 4096);

    char buffer[16];
    int nread = read(fd, buffer, sizeof(buffer));
    if (nread != 0) {
        warnln("Expected 0-length read past end of file in /tmp");
    }
    rc = close(fd);
    VERIFY(rc == 0);
    rc = unlink("/tmp/x");
    VERIFY(rc == 0);
}

TEST_CASE(sysfs_read_past_uptime_end)
{
    int fd = open("/sys/kernel/uptime", O_RDONLY);
    VERIFY(fd >= 0);

    int rc = lseek(fd, 4096, SEEK_SET);
    VERIFY(rc == 4096);

    char buffer[16];
    int nread = read(fd, buffer, sizeof(buffer));
    if (nread != 0) {
        warnln("Expected 0-length read past end of file in /proc");
    }
    close(fd);
}

TEST_CASE(open_create_device)
{
    int fd = open("/tmp/fakedevice", (O_RDWR | O_CREAT), (S_IFCHR | 0600));
    VERIFY(fd >= 0);

    struct stat st;
    int rc = fstat(fd, &st);
    EXPECT(rc >= 0);
    if (rc < 0) {
        perror("stat");
    }

    EXPECT_EQ(st.st_mode, 0100600);
    if (st.st_mode != 0100600) {
        warnln("Expected mode 0100600 after attempt to create a device node with open(O_CREAT), mode={:o}", st.st_mode);
    }
    rc = unlink("/tmp/fakedevice");
    EXPECT_EQ(rc, 0);
    close(fd);
    EXPECT_EQ(rc, 0);
}

TEST_CASE(unlink_symlink)
{
    int rc = symlink("/proc/2/foo", "/tmp/linky");
    EXPECT(rc >= 0);
    if (rc < 0) {
        perror("symlink");
    }

    auto target = TRY_OR_FAIL(FileSystem::read_link("/tmp/linky"sv));
    EXPECT_EQ(target, "/proc/2/foo"sv);

    rc = unlink("/tmp/linky");
    EXPECT(rc >= 0);
    if (rc < 0) {
        perror("unlink");
        warnln("Expected unlink() of a symlink into an unreadable directory to succeed!");
    }
}

TEST_CASE(tmpfs_eoverflow)
{
    int fd = open("/tmp/x", O_RDWR | O_CREAT);
    EXPECT(fd >= 0);

    off_t rc = lseek(fd, INT64_MAX, SEEK_SET);
    EXPECT_EQ(rc, INT64_MAX);

    char buffer[16] {};
    char empty_buffer[16] {};

    rc = read(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EOVERFLOW);

    [[maybe_unused]] auto ignored = strlcpy(buffer, "abcdefghijklmno", sizeof(buffer) - 1);

    rc = write(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EOVERFLOW);
    if (rc >= 0 || errno != EOVERFLOW) {
        warnln("Expected EOVERFLOW when trying to write past INT64_MAX");
    }

    // ok now, write something to it, and try again
    rc = lseek(fd, 0, SEEK_SET);
    EXPECT_EQ(rc, 0);

    rc = write(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, 16);

    rc = lseek(fd, INT64_MAX, SEEK_SET);
    EXPECT_EQ(rc, INT64_MAX);

    memset(buffer, 0, sizeof(buffer));
    rc = read(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EOVERFLOW);
    if (rc >= 0 || errno != EOVERFLOW) {
        warnln("Expected EOVERFLOW when trying to read past INT64_MAX");
    }
    EXPECT_EQ(0, memcmp(buffer, empty_buffer, sizeof(buffer)));

    rc = close(fd);
    EXPECT_EQ(rc, 0);
    rc = unlink("/tmp/x");
    EXPECT_EQ(rc, 0);
}

TEST_CASE(tmpfs_massive_file)
{
    int fd = open("/tmp/x", O_RDWR | O_CREAT);
    EXPECT(fd >= 0);

    off_t rc = lseek(fd, INT32_MAX, SEEK_SET);
    EXPECT_EQ(rc, INT32_MAX);

    char buffer[16] {};
    rc = read(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, 0);

    [[maybe_unused]] auto ignored = strlcpy(buffer, "abcdefghijklmno", sizeof(buffer) - 1);

    rc = write(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, 16);

    // ok now, write something to it, and try again
    rc = lseek(fd, 0, SEEK_SET);
    EXPECT_EQ(rc, 0);

    rc = write(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, 16);

    rc = lseek(fd, INT32_MAX, SEEK_SET);
    EXPECT_EQ(rc, INT32_MAX);

    memset(buffer, 0, sizeof(buffer));
    rc = read(fd, buffer, sizeof(buffer));
    EXPECT_EQ(rc, 16);
    EXPECT(buffer != "abcdefghijklmno"sv);

    rc = close(fd);
    EXPECT_EQ(rc, 0);
    rc = unlink("/tmp/x");
    EXPECT_EQ(rc, 0);
}

TEST_CASE(rmdir_dot)
{
    int rc = mkdir("/home/anon/rmdir-test-1", 0700);
    EXPECT_EQ(rc, 0);

    rc = rmdir("/home/anon/rmdir-test-1/.");
    EXPECT_NE(rc, 0);
    EXPECT_EQ(errno, EINVAL);

    rc = chdir("/home/anon/rmdir-test-1");
    EXPECT_EQ(rc, 0);

    rc = rmdir(".");
    VERIFY(rc != 0);
    EXPECT_EQ(errno, EINVAL);

    rc = rmdir("/home/anon/rmdir-test-1");
    EXPECT_EQ(rc, 0);
}

TEST_CASE(rmdir_dot_dot)
{
    int rc = mkdir("/home/anon/rmdir-test-2", 0700);
    EXPECT_EQ(rc, 0);

    rc = mkdir("/home/anon/rmdir-test-2/foo", 0700);
    EXPECT_EQ(rc, 0);

    rc = rmdir("/home/anon/rmdir-test-2/foo/..");
    EXPECT_NE(rc, 0);
    EXPECT_EQ(errno, ENOTEMPTY);

    rc = rmdir("/home/anon/rmdir-test-2/foo");
    EXPECT_EQ(rc, 0);

    rc = rmdir("/home/anon/rmdir-test-2");
    EXPECT_EQ(rc, 0);
}

TEST_CASE(rmdir_someone_elses_directory_in_my_sticky_directory)
{
    // NOTE: This test only works when run as root, since it has to chown a directory to someone else.
    if (getuid() != 0)
        return;

    // Create /tmp/sticky-dir a sticky directory owned by 12345:12345
    // Then, create /tmp/sticky-dir/notmine, a normal directory owned by 23456:23456
    // Then, fork and seteuid to 12345, and try to rmdir the "notmine" directory. This should succeed.
    // In the parent, waitpid on the child, and finally rmdir /tmp/sticky-dir

    int rc = mkdir("/tmp/sticky-dir", 01777);
    EXPECT_EQ(rc, 0);

    rc = chown("/tmp/sticky-dir", 12345, 12345);
    EXPECT_EQ(rc, 0);

    rc = mkdir("/tmp/sticky-dir/notmine", 0700);
    EXPECT_EQ(rc, 0);

    rc = chown("/tmp/sticky-dir/notmine", 23456, 23456);
    EXPECT_EQ(rc, 0);

    int pid = fork();
    EXPECT(pid >= 0);

    if (pid == 0) {
        // We are in the child.
        rc = seteuid(12345);
        EXPECT_EQ(rc, 0);

        rc = rmdir("/tmp/sticky-dir/notmine");
        EXPECT_EQ(rc, 0);
        _exit(0);
    }

    int status = 0;
    waitpid(pid, &status, 0);

    rc = rmdir("/tmp/sticky-dir");
    EXPECT_EQ(rc, 0);
}

TEST_CASE(rmdir_while_inside_dir)
{
    int rc = mkdir("/home/anon/testdir", 0700);
    VERIFY(rc == 0);

    rc = chdir("/home/anon/testdir");
    VERIFY(rc == 0);

    rc = rmdir("/home/anon/testdir");
    VERIFY(rc == 0);

    int fd = open("x", O_CREAT | O_RDWR, 0600);
    EXPECT(fd < 0);
    EXPECT_EQ(errno, ENOENT);
    if (fd >= 0 || errno != ENOENT) {
        warnln("Expected ENOENT when trying to create a file inside a deleted directory. Got {} with errno={}", fd, errno);
    }

    rc = chdir("/home/anon");
    VERIFY(rc == 0);
}

TEST_CASE(writev)
{
    int pipefds[2];
    int rc = pipe(pipefds);
    EXPECT(rc == 0);

    iovec iov[2];
    iov[0].iov_base = const_cast<void*>((void const*)"Hello");
    iov[0].iov_len = 5;
    iov[1].iov_base = const_cast<void*>((void const*)"Friends");
    iov[1].iov_len = 7;
    int nwritten = writev(pipefds[1], iov, 2);
    EXPECT_EQ(nwritten, 12);
    if (nwritten < 0) {
        perror("writev");
    }
    if (nwritten != 12) {
        warnln("Didn't write 12 bytes to pipe with writev");
    }

    char buffer[32] {};
    int nread = read(pipefds[0], buffer, sizeof(buffer));
    EXPECT_EQ(nread, 12);
    EXPECT_EQ(buffer, "HelloFriends"sv);
    if (nread != 12 || memcmp(buffer, "HelloFriends", 12)) {
        warnln("Didn't read the expected data from pipe after writev");
        VERIFY_NOT_REACHED();
    }

    close(pipefds[0]);
    close(pipefds[1]);
}

TEST_CASE(rmdir_root)
{
    int rc = rmdir("/");
    EXPECT_EQ(rc, -1);
    EXPECT_EQ(errno, EBUSY);
    if (rc != -1 || errno != EBUSY) {
        warnln("rmdir(/) didn't fail with EBUSY");
    }
}

TEST_CASE(open_silly_things)
{
    int rc = -1;
    EXPECT_ERROR_2(ENOTDIR, open, "/dev/zero", (O_DIRECTORY | O_RDONLY));
    EXPECT_ERROR_2(EINVAL, open, "/dev/zero", (O_DIRECTORY | O_CREAT | O_RDWR));
    EXPECT_ERROR_2(EEXIST, open, "/dev/zero", (O_CREAT | O_EXCL | O_RDWR));
    EXPECT_ERROR_2(EINVAL, open, "/tmp/abcdef", (O_DIRECTORY | O_CREAT | O_RDWR));
    EXPECT_ERROR_2(EACCES, open, "/sys/kernel/processes", (O_RDWR));
    EXPECT_ERROR_2(ENOENT, open, "/boof/baaf/nonexistent", (O_CREAT | O_RDWR));
    EXPECT_ERROR_2(EISDIR, open, "/tmp", (O_DIRECTORY | O_RDWR));
    EXPECT_ERROR_2(EPERM, link, "/", "/home/anon/lolroot");
}
