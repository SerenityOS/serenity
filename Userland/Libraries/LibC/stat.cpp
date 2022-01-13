/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <assert.h>
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
int mkdir(const char* pathname, mode_t mode)
{
    if (!pathname) {
        errno = EFAULT;
        return -1;
    }
    int rc = syscall(SC_mkdir, pathname, strlen(pathname), mode);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/chmod.html
int chmod(const char* pathname, mode_t mode)
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
int mkfifo(const char* pathname, mode_t mode)
{
    return mknod(pathname, mode | S_IFIFO, 0);
}

static int do_stat(int dirfd, const char* path, struct stat* statbuf, bool follow_symlinks)
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
int lstat(const char* path, struct stat* statbuf)
{
    return do_stat(AT_FDCWD, path, statbuf, false);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/stat.html
int stat(const char* path, struct stat* statbuf)
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
int fstatat(int fd, const char* path, struct stat* statbuf, int flags)
{
    return do_stat(fd, path, statbuf, !(flags & AT_SYMLINK_NOFOLLOW));
}
}
