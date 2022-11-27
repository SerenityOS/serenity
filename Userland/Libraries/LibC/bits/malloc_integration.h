/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>

#ifdef _SHARED_LIBC
typedef ErrorOr<void*> (*MallocFunction)(size_t);
typedef void (*FreeFunction)(void*);
typedef ErrorOr<void*> (*CallocFunction)(size_t, size_t);
typedef ErrorOr<void*> (*ReallocFunction)(void*, size_t);
typedef int (*PosixMemalignFunction)(void**, size_t, size_t);
typedef ErrorOr<void*> (*AlignedAllocFunction)(size_t, size_t);
typedef size_t (*MallocSizeFunction)(void const*);
typedef size_t (*MallocGoodSizeFunction)(size_t);
typedef void (*SerenityDumpMallocStats)();
typedef bool (*HeapIsStableFunction)();
typedef bool (*SetAllocationEnabledFunction)(bool);

extern "C" {
extern MallocFunction __malloc;
extern FreeFunction __free;
extern CallocFunction __calloc;
extern ReallocFunction __realloc;
extern PosixMemalignFunction __posix_memalign;
extern AlignedAllocFunction __aligned_alloc;
extern MallocSizeFunction __malloc_size;
extern MallocGoodSizeFunction __malloc_good_size;
extern SerenityDumpMallocStats __serenity_dump_malloc_stats;
extern HeapIsStableFunction ___heap_is_stable;
extern SetAllocationEnabledFunction ___set_allocation_enabled;
}
#else
ErrorOr<void*> __malloc(size_t);
void __free(void*);
ErrorOr<void*> __calloc(size_t, size_t);
ErrorOr<void*> __realloc(void*, size_t);
int __posix_memalign(void**, size_t, size_t);
ErrorOr<void*> __aligned_alloc(size_t, size_t);
size_t __malloc_size(void const*);
size_t __malloc_good_size(size_t);
void __serenity_dump_malloc_stats();
bool ___heap_is_stable();
bool ___set_allocation_enabled(bool);
#endif
