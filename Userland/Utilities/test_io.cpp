/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <LibCore/File.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <unistd.h>

#define EXPECT_ERROR_2(err, syscall, arg1, arg2)                                                                                                                   \
    do {                                                                                                                                                           \
        rc = syscall(arg1, arg2);                                                                                                                                  \
        if (rc >= 0 || errno != err) {                                                                                                                             \
            warnln(__FILE__ ":{}: Expected " #err ": " #syscall "({:p}, {:p}), got rc={}, errno={}", __LINE__, (const void*)(arg1), (const void*)arg2, rc, errno); \
        }                                                                                                                                                          \
    } while (0)

#define EXPECT_ERROR_3(err, syscall, arg1, arg2, arg3)                                                                                                                                          \
    do {                                                                                                                                                                                        \
        rc = syscall(arg1, arg2, arg3);                                                                                                                                                         \
        if (rc >= 0 || errno != err) {                                                                                                                                                          \
            warnln(__FILE__ ":{}: Expected " #err ": " #syscall "({:p}, {:p}, {:p}), got rc={}, errno={}", __LINE__, (const void*)(arg1), (const void*)(arg2), (const void*)(arg3), rc, errno); \
        }                                                                                                                                                                                       \
    } while (0)

static void test_read_from_directory()
{
    char buffer[BUFSIZ];
    int fd = open("/", O_DIRECTORY | O_RDONLY);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EISDIR, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    VERIFY(rc == 0);
}

static void test_write_to_directory()
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

static void test_read_from_writeonly()
{
    char buffer[BUFSIZ];
    int fd = open("/tmp/xxxx123", O_CREAT | O_WRONLY);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    VERIFY(rc == 0);
}

static void test_write_to_readonly()
{
    char str[] = "hello";
    int fd = open("/tmp/abcd123", O_CREAT | O_RDONLY);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, write, fd, str, sizeof(str));
    rc = close(fd);
    VERIFY(rc == 0);
}

static void test_read_past_eof()
{
    char buffer[BUFSIZ];
    int fd = open("/home/anon/myfile.txt", O_RDONLY);
    if (fd < 0)
        perror("open");
    VERIFY(fd >= 0);
    int rc;
    rc = lseek(fd, 9999, SEEK_SET);
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

static void test_ftruncate_readonly()
{
    int fd = open("/tmp/trunctest", O_RDONLY | O_CREAT, 0666);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EBADF, ftruncate, fd, 0);
    close(fd);
}

static void test_ftruncate_negative()
{
    int fd = open("/tmp/trunctest", O_RDWR | O_CREAT, 0666);
    VERIFY(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EINVAL, ftruncate, fd, -1);
    close(fd);
}

static void test_mmap_directory()
{
    int fd = open("/tmp", O_RDONLY | O_DIRECTORY);
    VERIFY(fd >= 0);
    auto* ptr = mmap(nullptr, 4096, PROT_READ, MAP_FILE | MAP_SHARED, fd, 0);
    if (ptr != MAP_FAILED) {
        warnln("Boo! mmap() of a directory succeeded!");
        return;
    }
    if (errno != ENODEV) {
        warnln("Boo! mmap() of a directory gave errno={} instead of ENODEV!", errno);
        return;
    }
    close(fd);
}

static void test_tmpfs_read_past_end()
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
    close(fd);
}

static void test_procfs_read_past_end()
{
    int fd = open("/proc/uptime", O_RDONLY);
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

static void test_open_create_device()
{
    int fd = open("/tmp/fakedevice", (O_RDWR | O_CREAT), (S_IFCHR | 0600));
    VERIFY(fd >= 0);

    struct stat st;
    if (fstat(fd, &st) < 0) {
        perror("stat");
        VERIFY_NOT_REACHED();
    }

    if (st.st_mode != 0100600) {
        warnln("Expected mode 0100600 after attempt to create a device node with open(O_CREAT), mode={:o}", st.st_mode);
    }
    unlink("/tmp/fakedevice");
    close(fd);
}

static void test_unlink_symlink()
{
    int rc = symlink("/proc/2/foo", "/tmp/linky");
    if (rc < 0) {
        perror("symlink");
        VERIFY_NOT_REACHED();
    }

    auto target = Core::File::read_link("/tmp/linky");
    VERIFY(target == "/proc/2/foo");

    rc = unlink("/tmp/linky");
    if (rc < 0) {
        perror("unlink");
        warnln("Expected unlink() of a symlink into an unreadable directory to succeed!");
    }
}

static void test_eoverflow()
{
    int fd = open("/tmp/x", O_RDWR);
    VERIFY(fd >= 0);

    int rc = lseek(fd, INT32_MAX, SEEK_SET);
    VERIFY(rc == INT32_MAX);

    char buffer[16];
    rc = read(fd, buffer, sizeof(buffer));
    if (rc >= 0 || errno != EOVERFLOW) {
        warnln("Expected EOVERFLOW when trying to read past INT32_MAX");
    }
    rc = write(fd, buffer, sizeof(buffer));
    if (rc >= 0 || errno != EOVERFLOW) {
        warnln("Expected EOVERFLOW when trying to write past INT32_MAX");
    }
    close(fd);
}

static void test_rmdir_while_inside_dir()
{
    int rc = mkdir("/home/anon/testdir", 0700);
    VERIFY(rc == 0);

    rc = chdir("/home/anon/testdir");
    VERIFY(rc == 0);

    rc = rmdir("/home/anon/testdir");
    VERIFY(rc == 0);

    int fd = open("x", O_CREAT | O_RDWR, 0600);
    if (fd >= 0 || errno != ENOENT) {
        warnln("Expected ENOENT when trying to create a file inside a deleted directory. Got {} with errno={}", fd, errno);
    }

    rc = chdir("/home/anon");
    VERIFY(rc == 0);
}

static void test_writev()
{
    int pipefds[2];
    pipe(pipefds);

    iovec iov[2];
    iov[0].iov_base = const_cast<void*>((const void*)"Hello");
    iov[0].iov_len = 5;
    iov[1].iov_base = const_cast<void*>((const void*)"Friends");
    iov[1].iov_len = 7;
    int nwritten = writev(pipefds[1], iov, 2);
    if (nwritten < 0) {
        perror("writev");
        VERIFY_NOT_REACHED();
    }
    if (nwritten != 12) {
        warnln("Didn't write 12 bytes to pipe with writev");
        VERIFY_NOT_REACHED();
    }

    char buffer[32];
    int nread = read(pipefds[0], buffer, sizeof(buffer));
    if (nread != 12 || memcmp(buffer, "HelloFriends", 12)) {
        warnln("Didn't read the expected data from pipe after writev");
        VERIFY_NOT_REACHED();
    }

    close(pipefds[0]);
    close(pipefds[1]);
}

static void test_rmdir_root()
{
    int rc = rmdir("/");
    if (rc != -1 || errno != EBUSY) {
        warnln("rmdir(/) didn't fail with EBUSY");
        VERIFY_NOT_REACHED();
    }
}

int main()
{
    int rc;
    EXPECT_ERROR_2(ENOTDIR, open, "/dev/zero", (O_DIRECTORY | O_RDONLY));
    EXPECT_ERROR_2(EINVAL, open, "/dev/zero", (O_DIRECTORY | O_CREAT | O_RDWR));
    EXPECT_ERROR_2(EEXIST, open, "/dev/zero", (O_CREAT | O_EXCL | O_RDWR));
    EXPECT_ERROR_2(EINVAL, open, "/tmp/abcdef", (O_DIRECTORY | O_CREAT | O_RDWR));
    EXPECT_ERROR_2(EACCES, open, "/proc/all", (O_RDWR));
    EXPECT_ERROR_2(ENOENT, open, "/boof/baaf/nonexistent", (O_CREAT | O_RDWR));
    EXPECT_ERROR_2(EISDIR, open, "/tmp", (O_DIRECTORY | O_RDWR));

    test_read_from_directory();
    test_write_to_directory();
    test_read_from_writeonly();
    test_write_to_readonly();
    test_read_past_eof();
    test_ftruncate_readonly();
    test_ftruncate_negative();
    test_mmap_directory();
    test_tmpfs_read_past_end();
    test_procfs_read_past_end();
    test_open_create_device();
    test_unlink_symlink();
    test_eoverflow();
    test_rmdir_while_inside_dir();
    test_writev();
    test_rmdir_root();

    EXPECT_ERROR_2(EPERM, link, "/", "/home/anon/lolroot");

    return 0;
}
