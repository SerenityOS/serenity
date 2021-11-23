/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <signal.h>
#include <sys/stat.h>

namespace Core::System {

#ifdef __serenity__
ErrorOr<void> pledge(StringView promises, StringView execpromises);
ErrorOr<void> unveil(StringView path, StringView permissions);
#endif

ErrorOr<void> sigaction(int signal, struct sigaction const* action, struct sigaction* old_action);
ErrorOr<struct stat> fstat(int fd);
ErrorOr<int> fcntl(int fd, int command, ...);
ErrorOr<void*> mmap(void* address, size_t, int protection, int flags, int fd, off_t, size_t alignment = 0, StringView name = {});

}
