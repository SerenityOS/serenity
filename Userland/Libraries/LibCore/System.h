/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <signal.h>
#include <sys/stat.h>
#include <termios.h>

namespace Core::System {

#ifdef __serenity__
ErrorOr<void> pledge(StringView promises, StringView execpromises = {});
ErrorOr<void> unveil(StringView path, StringView permissions);
ErrorOr<Array<int, 2>> pipe2(int flags);
ErrorOr<void> sendfd(int sockfd, int fd);
ErrorOr<int> recvfd(int sockfd, int options);
#endif

ErrorOr<void> sigaction(int signal, struct sigaction const* action, struct sigaction* old_action);
ErrorOr<struct stat> fstat(int fd);
ErrorOr<int> fcntl(int fd, int command, ...);
ErrorOr<void*> mmap(void* address, size_t, int protection, int flags, int fd, off_t, size_t alignment = 0, StringView name = {});
ErrorOr<void> munmap(void* address, size_t);
ErrorOr<int> open(StringView path, int options, ...);
ErrorOr<void> close(int fd);
ErrorOr<void> ftruncate(int fd, off_t length);
ErrorOr<struct stat> stat(StringView path);
ErrorOr<struct stat> lstat(StringView path);
ErrorOr<ssize_t> read(int fd, Bytes buffer);
ErrorOr<ssize_t> write(int fd, ReadonlyBytes buffer);
ErrorOr<void> kill(pid_t, int signal);
ErrorOr<int> dup(int source_fd);
ErrorOr<int> dup2(int source_fd, int destination_fd);
ErrorOr<String> ptsname(int fd);
ErrorOr<String> gethostname();
ErrorOr<void> ioctl(int fd, unsigned request, ...);
ErrorOr<struct termios> tcgetattr(int fd);
ErrorOr<void> tcsetattr(int fd, int optional_actions, struct termios const&);
ErrorOr<void> chmod(StringView pathname, mode_t mode);

}
