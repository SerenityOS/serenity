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
int chmod(const char* pathname, mode_t);
int fchmod(int fd, mode_t);
int mkdir(const char* pathname, mode_t);
int mkfifo(const char* pathname, mode_t);
int fstat(int fd, struct stat* statbuf);
int lstat(const char* path, struct stat* statbuf);
int stat(const char* path, struct stat* statbuf);
int fstatat(int fd, const char* path, struct stat* statbuf, int flags);

__END_DECLS
