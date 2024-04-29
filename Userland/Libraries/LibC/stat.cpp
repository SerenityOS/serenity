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
}
