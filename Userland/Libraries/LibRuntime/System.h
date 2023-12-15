/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/Types.h>
// FIXME: Serenity's LibC headers include random unrelated stuff which we usually don't want.
//        Instead of directly using Kernel/* here, prefix this random stuff just like glibc does.
#include <Kernel/API/POSIX/fcntl.h>
#include <Kernel/API/POSIX/sys/mman.h>
#include <Kernel/API/POSIX/sys/types.h>
#include <LibRuntime/StringArgument.h>

namespace Runtime {

AK_TYPEDEF_DISTINCT_ORDERED_ID(int, FileDescriptor);

struct StackBounds {
    uintptr_t user_stack_base;
    size_t user_stack_size;
};

enum class RegionAccess {
    Read = PROT_READ,
    Write = PROT_WRITE,
    Execute = PROT_EXEC,

    None = PROT_NONE,
    ReadWrite = Read | Write,
};

enum class MMap {
    Shared = MAP_SHARED,
    Private = MAP_PRIVATE,

    Fixed = MAP_FIXED,
    FixedNoReplace = MAP_FIXED_NOREPLACE,
    Anonymous = MAP_ANONYMOUS,
    Stack = MAP_STACK,
    NoReserve = MAP_NORESERVE,
    Randomized = MAP_RANDOMIZED,
    Purgeable = MAP_PURGEABLE,
};

AK_ENUM_BITWISE_OPERATORS(RegionAccess)
AK_ENUM_BITWISE_OPERATORS(MMap)

void dbgputstr(StringArgument const& string);
ErrorOr<void> get_process_name(StringBuilder& result);
StackBounds get_stack_bounds();
pid_t getpid();
pid_t gettid();
ErrorOr<void*> mmap(void* address, size_t size, RegionAccess access, MMap flags, StringView name, FileDescriptor fd = -1, off_t offset = 0, size_t alignment = 0);
ErrorOr<void> mprotect(void* address, size_t size, RegionAccess access);
ErrorOr<void> munmap(void* address, size_t size);
ErrorOr<void> set_mmap_name(void* address, size_t size, StringView name);

}
