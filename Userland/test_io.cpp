#include <AK/Assertions.h>
#include <AK/Types.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/mman.h>
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

void test_read_from_directory()
{
    char buffer[BUFSIZ];
    int fd = open("/", O_DIRECTORY | O_RDONLY);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EISDIR, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    ASSERT(rc == 0);
}

void test_write_to_directory()
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

void test_read_from_writeonly()
{
    char buffer[BUFSIZ];
    int fd = open("/tmp/xxxx123", O_CREAT | O_WRONLY);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, read, fd, buffer, sizeof(buffer));
    rc = close(fd);
    ASSERT(rc == 0);
}

void test_write_to_readonly()
{
    char str[] = "hello";
    int fd = open("/tmp/abcd123", O_CREAT | O_RDONLY);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_3(EBADF, write, fd, str, sizeof(str));
    rc = close(fd);
    ASSERT(rc == 0);
}

void test_read_past_eof()
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

void test_ftruncate_readonly()
{
    int fd = open("/tmp/trunctest", O_RDONLY | O_CREAT, 0666);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EBADF, ftruncate, fd, 0);
    close(fd);
}

void test_ftruncate_negative()
{
    int fd = open("/tmp/trunctest", O_RDWR | O_CREAT, 0666);
    ASSERT(fd >= 0);
    int rc;
    EXPECT_ERROR_2(EINVAL, ftruncate, fd, -1);
    close(fd);
}

void test_mmap_directory()
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

void test_tmpfs_read_past_end()
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

void test_procfs_read_past_end()
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

void test_open_create_device()
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

void test_unlink_symlink()
{
    int rc = symlink("/proc/2/foo", "/tmp/linky");
    if (rc < 0) {
        perror("symlink");
        ASSERT_NOT_REACHED();
    }
    rc = unlink("/tmp/linky");
    if (rc < 0) {
        perror("unlink");
        fprintf(stderr, "Expected unlink() of a symlink into an unreadable directory to succeed!\n");
    }
}

void test_eoverflow()
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

int main(int, char**)
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

    return 0;
}
