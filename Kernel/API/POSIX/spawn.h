/*
 * Copyright (c) 2026, Fırat Kızılboğa <firatkizilboga11@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <sys/types.h>

namespace Kernel::API::POSIX {

enum class SpawnFileActionType : u8 {
    Dup2 = 0,
    Open = 1,
    Close = 2,
    Chdir = 3,
    Fchdir = 4,
};

struct [[gnu::packed]] SpawnFileActionHeader {
    SpawnFileActionType type;
    u16 record_length;
};

struct [[gnu::packed]] SpawnFileActionDup2 {
    SpawnFileActionHeader header;
    i32 old_fd;
    i32 new_fd;
};

struct [[gnu::packed]] SpawnFileActionOpen {
    SpawnFileActionHeader header;
    i32 fd;
    i32 flags;
    mode_t mode;
    u16 path_length;
    char path[];
};

struct [[gnu::packed]] SpawnFileActionClose {
    SpawnFileActionHeader header;
    i32 fd;
};

struct [[gnu::packed]] SpawnFileActionChdir {
    SpawnFileActionHeader header;
    u16 path_length;
    char path[];
};

struct [[gnu::packed]] SpawnFileActionFchdir {
    SpawnFileActionHeader header;
    i32 fd;
};

}
