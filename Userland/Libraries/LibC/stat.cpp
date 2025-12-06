/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
#include <bits/utimens.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <syscall.h>
#include <unistd.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/umask.html
mode_t umask(mode_t mask)
{
    return syscall(SC_umask, mask);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mkdir.html
int mkdir(char const* pathname, mode_t mode)
{
    return mkdirat(AT_FDCWD, pathname, mode);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mkdirat.html
int mkdirat(int dirfd, char const* pathname, mode_t mode)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_mkdir, dirfd, pathname, strlen(pathname), mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/chmod.html
int chmod(char const* pathname, mode_t mode)
{
    return fchmodat(AT_FDCWD, pathname, mode, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fchmodat.html
int fchmodat(int dirfd, char const* pathname, mode_t mode, int flags)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }

    if (flags & ~AT_SYMLINK_NOFOLLOW) {
        errno = EINVAL;
        return -1;
    }

    Syscall::SC_chmod_params params {
        dirfd,
        { pathname, strlen(pathname) },
        mode,
        !(flags & AT_SYMLINK_NOFOLLOW)
    };
    int rc = syscall(SC_chmod, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fchmod.html
int fchmod(int fd, mode_t mode)
{
    int rc = syscall(SC_fchmod, fd, mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mkfifo.html
int mkfifo(char const* pathname, mode_t mode)
{
    return mknod(pathname, mode | S_IFIFO, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mkfifoat.html
int mkfifoat(int dirfd, char const* pathname, mode_t mode)
{
    return mknodat(dirfd, pathname, mode | S_IFIFO, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mknod.html
int mknod(char const* pathname, mode_t mode, dev_t dev)
{
    return mknodat(AT_FDCWD, pathname, mode, dev);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/mknodat.html
int mknodat(int dirfd, char const* pathname, mode_t mode, dev_t dev)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_mknod_params params { { pathname, strlen(pathname) }, mode, dev, dirfd };
    int rc = syscall(SC_mknod, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

static int do_stat(int dirfd, char const* path, struct stat* statbuf, bool follow_symlinks)
{
    if (!path) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_stat_params params { { path, strlen(path) }, statbuf, dirfd, follow_symlinks };
    int rc = syscall(SC_stat, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/lstat.html
int lstat(char const* path, struct stat* statbuf)
{
    return do_stat(AT_FDCWD, path, statbuf, false);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/stat.html
int stat(char const* path, struct stat* statbuf)
{
    return do_stat(AT_FDCWD, path, statbuf, true);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fstat.html
int fstat(int fd, struct stat* statbuf)
{
    int rc = syscall(SC_fstat, fd, statbuf);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fstatat.html
int fstatat(int fd, char const* path, struct stat* statbuf, int flags)
{
    return do_stat(fd, path, statbuf, !(flags & AT_SYMLINK_NOFOLLOW));
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/futimens.html
int futimens(int fd, struct timespec const times[2])
{
    return __utimens(fd, nullptr, times, 0);
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
