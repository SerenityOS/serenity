#include <Kernel/Syscall.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <string.h>

extern "C" {

int fcntl(int fd, int cmd, ...)
{
    va_list ap;
    va_start(ap, cmd);
    u32 extra_arg = va_arg(ap, u32);
    int rc = syscall(SC_fcntl, fd, cmd, extra_arg);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int watch_file(const char* path, size_t path_length)
{
    int rc = syscall(SC_watch_file, path, path_length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int creat(const char* path, mode_t mode)
{
    return open(path, O_CREAT | O_WRONLY | O_TRUNC, mode);
}

int creat_with_path_length(const char* path, size_t path_length, mode_t mode)
{
    return open_with_path_length(path, path_length, O_CREAT | O_WRONLY | O_TRUNC, mode);
}

int open_with_path_length(const char* path, size_t path_length, int options, mode_t mode)
{
    return openat_with_path_length(AT_FDCWD, path, path_length, options, mode);
}

int openat_with_path_length(int dirfd, const char* path, size_t path_length, int options, mode_t mode)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    if (path_length > INT32_MAX) {
        errno = EINVAL;
        return -1;
    }
    Syscall::SC_open_params params { dirfd, { path, path_length }, options, mode };
    int rc = syscall(SC_open, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int open(const char* path, int options, ...)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    va_list ap;
    va_start(ap, options);
    auto mode = (mode_t)va_arg(ap, unsigned);
    va_end(ap);
    return open_with_path_length(path, strlen(path), options, mode);
}

int openat(int dirfd, const char* path, int options, ...)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    va_list ap;
    va_start(ap, options);
    auto mode = (mode_t)va_arg(ap, unsigned);
    va_end(ap);
    return openat_with_path_length(dirfd, path, strlen(path), options, mode);
}
}
