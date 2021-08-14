/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/fcntl.h>

__BEGIN_DECLS

int creat(const char* path, mode_t);
int open(const char* path, int options, ...);
int openat(int dirfd, const char* path, int options, ...);

int fcntl(int fd, int cmd, ...);
int create_inode_watcher(unsigned flags);
int inode_watcher_add_watch(int fd, const char* path, size_t path_length, unsigned event_mask);
int inode_watcher_remove_watch(int fd, int wd);

__END_DECLS
