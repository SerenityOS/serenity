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

    return 0;
}
