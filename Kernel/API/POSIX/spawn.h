/*
 * Copyright (c) 2026, Fırat Kızılboğa <firatkizilboga11@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
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

struct SpawnFileActionHeader {
    SpawnFileActionType type;
    u16 record_length;
};

struct SpawnFileActionDup2 {
    SpawnFileActionHeader header;
    i32 old_fd;
    i32 new_fd;
};

struct SpawnFileActionOpen {
    SpawnFileActionHeader header;
    i32 fd;
    i32 flags;
    mode_t mode;
    u16 path_length;
    char path[];
};

struct SpawnFileActionClose {
    SpawnFileActionHeader header;
    i32 fd;
};

struct SpawnFileActionChdir {
    SpawnFileActionHeader header;
    u16 path_length;
    char path[];
};

struct SpawnFileActionFchdir {
    SpawnFileActionHeader header;
    i32 fd;
};

// Maximum alignment across all SpawnFileAction structs.
// Used to ensure proper alignment when serializing variable-length records.
inline constexpr size_t SpawnFileActionAlignment = max(
    max(alignof(SpawnFileActionHeader), alignof(SpawnFileActionDup2)),
    max(max(alignof(SpawnFileActionOpen), alignof(SpawnFileActionClose)),
        max(alignof(SpawnFileActionChdir), alignof(SpawnFileActionFchdir))));

}
