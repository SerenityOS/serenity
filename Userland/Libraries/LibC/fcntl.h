/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/fcntl.h>

__BEGIN_DECLS

#define POSIX_FADV_DONTNEED 1
#define POSIX_FADV_NOREUSE 2
#define POSIX_FADV_NORMAL 3
#define POSIX_FADV_RANDOM 4
#define POSIX_FADV_SEQUENTIAL 5
#define POSIX_FADV_WILLNEED 6

int creat(char const* path, mode_t);
int open(char const* path, int options, ...);
int openat(int dirfd, char const* path, int options, ...);

int fcntl(int fd, int cmd, ...);
int create_inode_watcher(unsigned flags);
int inode_watcher_add_watch(int fd, char const* path, size_t path_length, unsigned event_mask);
int inode_watcher_remove_watch(int fd, int wd);

int posix_fadvise(int fd, off_t offset, off_t len, int advice);

__END_DECLS
