/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/pthread_cancel.h>
#include <bits/utimens.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdarg.h>
#include <string.h>
#include <syscall.h>
#include <time.h>

extern "C" {

int fcntl(int fd, int cmd, ...)
{
    __pthread_maybe_cancel();

    va_list ap;
    va_start(ap, cmd);
    uintptr_t extra_arg = va_arg(ap, uintptr_t);
    int rc = syscall(SC_fcntl, fd, cmd, extra_arg);
    va_end(ap);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int create_inode_watcher(unsigned flags)
{
    int rc = syscall(SC_create_inode_watcher, flags);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int inode_watcher_add_watch(int fd, char const* path, size_t path_length, unsigned event_mask)
{
    Syscall::SC_inode_watcher_add_watch_params params { { path, path_length }, fd, event_mask };
    int rc = syscall(SC_inode_watcher_add_watch, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int inode_watcher_remove_watch(int fd, int wd)
{
    int rc = syscall(SC_inode_watcher_remove_watch, fd, wd);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int creat(char const* path, mode_t mode)
{
    __pthread_maybe_cancel();

    return open(path, O_CREAT | O_WRONLY | O_TRUNC, mode);
}

int open(char const* path, int options, ...)
{
    __pthread_maybe_cancel();

    if (!path) {
        errno = EFAULT;
        return -1;
    }
    auto path_length = strlen(path);
    if (path_length > INT32_MAX) {
        errno = EINVAL;
        return -1;
    }
    va_list ap;
    va_start(ap, options);
    auto mode = (mode_t)va_arg(ap, unsigned);
    va_end(ap);
    Syscall::SC_open_params params { AT_FDCWD, { path, path_length }, options, mode };
    int rc = syscall(SC_open, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

int openat(int dirfd, char const* path, int options, ...)
{
    __pthread_maybe_cancel();

    if (!path) {
        errno = EFAULT;
        return -1;
    }
    auto path_length = strlen(path);
    if (path_length > INT32_MAX) {
        errno = EINVAL;
        return -1;
    }
    va_list ap;
    va_start(ap, options);
    auto mode = (mode_t)va_arg(ap, unsigned);
    va_end(ap);
    Syscall::SC_open_params params { dirfd, { path, path_length }, options, mode };
    int rc = syscall(SC_open, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_fadvise.html
int posix_fadvise(int fd, off_t offset, off_t len, int advice)
{
    // Per POSIX:
    // "The posix_fadvise() function shall have no effect on the semantics of other operations on the specified data,
    // although it may affect the performance of other operations."

    // For now, we simply ignore posix_fadvise() requests. In the future we may use them to optimize performance.
    (void)fd;
    (void)offset;
    (void)len;
    (void)advice;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/posix_fallocate.html
int posix_fallocate(int fd, off_t offset, off_t len)
{
    // posix_fallocate does not set errno.
    return -static_cast<int>(syscall(SC_posix_fallocate, fd, offset, len));
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/utimensat.html
int utimensat(int dirfd, char const* path, struct timespec const times[2], int flag)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    return __utimens(dirfd, path, times, flag);
}

int __utimens(int fd, char const* path, struct timespec const times[2], int flag)
{
    size_t path_length = 0;
    if (path) {
        path_length = strlen(path);
        if (path_length > INT32_MAX) {
            errno = EINVAL;
            return -1;
        }
    }

    // POSIX allows AT_SYMLINK_NOFOLLOW flag or no flags.
    if (flag & ~AT_SYMLINK_NOFOLLOW) {
        errno = EINVAL;
        return -1;
    }

    // Return early without error since both changes are to be omitted.
    if (times && times[0].tv_nsec == UTIME_OMIT && times[1].tv_nsec == UTIME_OMIT)
        return 0;

    // According to POSIX, when times is a nullptr, it's equivalent to setting
    // both last access time and last modification time to the current time.
    // Setting the times argument to nullptr if it matches this case prevents
    // the need to copy it in the kernel.
    if (times && times[0].tv_nsec == UTIME_NOW && times[1].tv_nsec == UTIME_NOW)
        times = nullptr;

    if (times) {
        for (int i = 0; i < 2; ++i) {
            if ((times[i].tv_nsec != UTIME_NOW && times[i].tv_nsec != UTIME_OMIT)
                && (times[i].tv_nsec < 0 || times[i].tv_nsec >= 1'000'000'000L)) {
                errno = EINVAL;
                return -1;
            }
        }
    }

    int rc = 0;
    if (path) {
        // NOTE: fd is treated as dirfd for this syscall.
        Syscall::SC_utimensat_params params { fd, { path, path_length }, times, flag };
        rc = syscall(SC_utimensat, &params);
    } else {
        Syscall::SC_futimens_params params { fd, times };
        rc = syscall(SC_futimens, &params);
    }

    __RETURN_WITH_ERRNO(rc, rc, -1);
}
}
