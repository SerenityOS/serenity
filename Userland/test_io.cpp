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

#include <AK/Assertions.h>
#include <AK/LogStream.h>
#include <AK/Types.h>
#include <LibCore/File.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/uio.h>
#include <unistd.h>

#define EXPECT_ERROR_2(err, syscall, arg1, arg2)                                                                                                                          \
    do {                                                                                                                                                                  \
        rc = syscall(arg1, arg2);                                                                                                                                         \
        if (rc >= 0 || errno != err) {                                                                                                                                    \
            fprintf(stderr, __FILE__ ":%d: Expected " #err ": " #syscall "(%p, %p), got rc=%d, errno=%d\n", __LINE__, (const void*)(arg1), (const void*)arg2, rc, errno); \
        }                                                                                                                                                                 \
    } while (0)

#define EXPECT_ERROR_3(err, syscall, arg1, arg2, arg3)                                                                                                                                               \
    do {                                                                                                                                                                                             \
        rc = syscall(arg1, arg2, arg3);                                                                                                                                                              \
        if (rc >= 0 || errno != err) {                                                                                                                                                               \
            fprintf(stderr, __FILE__ ":%d: Expected " #err ": " #syscall "(%p, %p, %p), got rc=%d, errno=%d\n", __LINE__, (const void*)(arg1), (const void*)(arg2), (const void*)(arg3), rc, errno); \
        }                                                                                                                                                                                            \
    } while (0)

static void test_read_from_directory()
{
    char buffer[BUFSIZ];
    int fd = open("/", O_DIRECTORY | O_RDONLY);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EISDIR, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    ASSERT(rc == 0);
}

static void test_write_to_directory()
{
    char str[] = "oh frick";
    int fd = open("/", O_DIRECTORY | O_RDONLY);
    if (fd < 0)
        perror("open");
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, write, fd, str, sizeof(str));
    rc = close(fd);
    ASSERT(rc == 0);
}

static void test_read_from_writeonly()
{
    char buffer[BUFSIZ];
    int fd = open("/tmp/xxxx123", O_CREAT | O_WRONLY);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    ASSERT(rc == 0);
}

static void test_write_to_readonly()
{
    char str[] = "hello";
    int fd = open("/tmp/abcd123", O_CREAT | O_RDONLY);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, write, fd, str, sizeof(str));
    rc = close(fd);
    ASSERT(rc == 0);
}

static void test_read_past_eof()
{
    char buffer[BUFSIZ];
    int fd = open("/home/anon/myfile.txt", O_RDONLY);
    if (fd < 0)
        perror("open");
    ASSERT(fd >= 0);
    int rc;
    rc = lseek(fd, 9999, SEEK_SET);
    if (rc < 0)
        perror("lseek");
    rc = read(fd, buffer, sizeof(buffer));
    if (rc < 0)
        perror("read");
    if (rc > 0)
        fprintf(stderr, "read %d bytes past EOF\n", rc);
    rc = close(fd);
    ASSERT(rc == 0);
}

static void test_ftruncate_readonly()
{
    int fd = open("/tmp/trunctest", O_RDONLY | O_CREAT, 0666);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EBADF, ftruncate, fd, 0);
    close(fd);
}

static void test_ftruncate_negative()
{
    int fd = open("/tmp/trunctest", O_RDWR | O_CREAT, 0666);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EINVAL, ftruncate, fd, -1);
    close(fd);
}

static void test_mmap_directory()
{
    int fd = open("/tmp", O_RDONLY | O_DIRECTORY);
    ASSERT(fd >= 0);
    auto* ptr = mmap(nullptr, 4096, PROT_READ, MAP_FILE | MAP_SHARED, fd, 0);
    if (ptr != MAP_FAILED) {
        fprintf(stderr, "Boo! mmap() of a directory succeeded!\n");
        return;
    }
    if (errno != ENODEV) {
        fprintf(stderr, "Boo! mmap() of a directory gave errno=%d instead of ENODEV!\n", errno);
        return;
    }
    close(fd);
}

static void test_tmpfs_read_past_end()
{
    int fd = open("/tmp/x", O_RDWR | O_CREAT | O_TRUNC, 0600);
    ASSERT(fd >= 0);

    int rc = ftruncate(fd, 1);
    ASSERT(rc == 0);

    rc = lseek(fd, 4096, SEEK_SET);
    ASSERT(rc == 4096);

    char buffer[16];
    int nread = read(fd, buffer, sizeof(buffer));
    if (nread != 0) {
        fprintf(stderr, "Expected 0-length read past end of file in /tmp\n");
    }
    close(fd);
}

static void test_procfs_read_past_end()
{
    int fd = open("/proc/uptime", O_RDONLY);
    ASSERT(fd >= 0);

    int rc = lseek(fd, 4096, SEEK_SET);
    ASSERT(rc == 4096);

    char buffer[16];
    int nread = read(fd, buffer, sizeof(buffer));
    if (nread != 0) {
        fprintf(stderr, "Expected 0-length read past end of file in /proc\n");
    }
    close(fd);
}

static void test_open_create_device()
{
    int fd = open("/tmp/fakedevice", (O_RDWR | O_CREAT), (S_IFCHR | 0600));
    ASSERT(fd >= 0);

    struct stat st;
    if (fstat(fd, &st) < 0) {
        perror("stat");
        ASSERT_NOT_REACHED();
    }

    if (st.st_mode != 0100600) {
        fprintf(stderr, "Expected mode 0100600 after attempt to create a device node with open(O_CREAT), mode=%o\n", st.st_mode);
    }
    unlink("/tmp/fakedevice");
    close(fd);
}

static void test_unlink_symlink()
{
    int rc = symlink("/proc/2/foo", "/tmp/linky");
    if (rc < 0) {
        perror("symlink");
        ASSERT_NOT_REACHED();
    }

    auto target = Core::File::read_link("/tmp/linky");
    ASSERT(target == "/proc/2/foo");

    rc = unlink("/tmp/linky");
    if (rc < 0) {
        perror("unlink");
        fprintf(stderr, "Expected unlink() of a symlink into an unreadable directory to succeed!\n");
    }
}

static void test_eoverflow()
{
    int fd = open("/tmp/x", O_RDWR);
    ASSERT(fd >= 0);

    int rc = lseek(fd, INT32_MAX, SEEK_SET);
    ASSERT(rc == INT32_MAX);

    char buffer[16];
    rc = read(fd, buffer, sizeof(buffer));
    if (rc >= 0 || errno != EOVERFLOW) {
        fprintf(stderr, "Expected EOVERFLOW when trying to read past INT32_MAX\n");
    }
    rc = write(fd, buffer, sizeof(buffer));
    if (rc >= 0 || errno != EOVERFLOW) {
        fprintf(stderr, "Expected EOVERFLOW when trying to write past INT32_MAX\n");
    }
    close(fd);
}

static void test_rmdir_while_inside_dir()
{
    int rc = mkdir("/home/anon/testdir", 0700);
    ASSERT(rc == 0);

    rc = chdir("/home/anon/testdir");
    ASSERT(rc == 0);

    rc = rmdir("/home/anon/testdir");
    ASSERT(rc == 0);

    int fd = open("x", O_CREAT | O_RDWR, 0600);
    if (fd >= 0 || errno != ENOENT) {
        fprintf(stderr, "Expected ENOENT when trying to create a file inside a deleted directory. Got %d with errno=%d\n", fd, errno);
    }

    rc = chdir("/home/anon");
    ASSERT(rc == 0);
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
        ASSERT_NOT_REACHED();
    }
    if (nwritten != 12) {
        fprintf(stderr, "Didn't write 12 bytes to pipe with writev\n");
        ASSERT_NOT_REACHED();
    }

    char buffer[32];
    int nread = read(pipefds[0], buffer, sizeof(buffer));
    if (nread != 12 || memcmp(buffer, "HelloFriends", 12)) {
        fprintf(stderr, "Didn't read the expected data from pipe after writev\n");
        ASSERT_NOT_REACHED();
    }

    close(pipefds[0]);
    close(pipefds[1]);
}

static void test_rmdir_root()
{
    int rc = rmdir("/");
    if (rc != -1 || errno != EBUSY) {
        warn() << "rmdir(/) didn't fail with EBUSY";
        ASSERT_NOT_REACHED();
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
