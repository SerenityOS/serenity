/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/sys/stat.h>
#include <sys/cdefs.h>
#include <sys/types.h>
#include <time.h>

__BEGIN_DECLS

mode_t umask(mode_t);
int chmod(char const* pathname, mode_t);
int fchmodat(int fd, char const* path, mode_t mode, int flag);
int fchmod(int fd, mode_t);
int mkdir(char const* pathname, mode_t);
int mkdirat(int dirfd, char const* pathname, mode_t);
int mkfifo(char const* pathname, mode_t);
int mkfifoat(int dirfd, char const* pathname, mode_t);
int fstat(int fd, struct stat* statbuf);
int lstat(char const* path, struct stat* statbuf);
int stat(char const* path, struct stat* statbuf);
int fstatat(int fd, char const* path, struct stat* statbuf, int flags);
int futimens(int fd, struct timespec const times[2]);

__END_DECLS
