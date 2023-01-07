/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

#ifdef KERNEL
#    include <Kernel/API/POSIX/sys/limits.h>
#else
#    include <limits.h>
#endif

struct [[gnu::packed]] InodeWatcherEvent {
    enum class Type : u32 {
        Invalid = 0,
        MetadataModified = 1 << 0,
        ContentModified = 1 << 1,
        Deleted = 1 << 2,
        ChildCreated = 1 << 3,
        ChildDeleted = 1 << 4,
    };

    int watch_descriptor { 0 };
    Type type { Type::Invalid };
    size_t name_length { 0 };
    // This is a VLA which is written during the read() from the descriptor.
    char const name[];
};

AK_ENUM_BITWISE_OPERATORS(InodeWatcherEvent::Type);

constexpr unsigned MAXIMUM_EVENT_SIZE = sizeof(InodeWatcherEvent) + NAME_MAX + 1;
