/*
 * Copyright (c) 2022, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/malloc_integration.h>
#include <stdlib.h>
#include <sys/internals.h>

#ifdef _SHARED_LIBC
// These are filled in by the dynamic loader.
MallocFunction __malloc;
FreeFunction __free;
CallocFunction __calloc;
ReallocFunction __realloc;
PosixMemalignFunction __posix_memalign;
AlignedAllocFunction __aligned_alloc;
MallocSizeFunction __malloc_size;
MallocGoodSizeFunction __malloc_good_size;
SerenityDumpMallocStats __serenity_dump_malloc_stats;
HeapIsStableFunction ___heap_is_stable;
SetAllocationEnabledFunction ___set_allocation_enabled;
#endif

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/malloc.html
void* malloc(size_t size)
{
    auto ptr_or_error = __malloc(size);
    if (ptr_or_error.is_error()) {
        errno = ptr_or_error.error().code();
        return nullptr;
    }
    return ptr_or_error.value();
}

void free(void* ptr)
{
    __free(ptr);
}

void* calloc(size_t count, size_t size)
{
    auto ptr_or_error = __calloc(count, size);
    if (ptr_or_error.is_error()) {
        errno = ptr_or_error.error().code();
        return nullptr;
    }
    return ptr_or_error.value();
}

int posix_memalign(void** memptr, size_t alignment, size_t size)
{
    return __posix_memalign(memptr, alignment, size);
}

void* aligned_alloc(size_t alignment, size_t size)
{
    auto ptr_or_error = __aligned_alloc(alignment, size);
    if (ptr_or_error.is_error()) {
        errno = ptr_or_error.error().code();
        return nullptr;
    }
    return ptr_or_error.value();
}

size_t malloc_size(void const* ptr)
{
    return __malloc_size(ptr);
}

size_t malloc_good_size(size_t size)
{
    return __malloc_good_size(size);
}

void* realloc(void* ptr, size_t size)
{
    auto ptr_or_error = __realloc(ptr, size);
    if (ptr_or_error.is_error()) {
        errno = ptr_or_error.error().code();
        return nullptr;
    }
    return ptr_or_error.value();
}

void serenity_dump_malloc_stats()
{
    __serenity_dump_malloc_stats();
}

bool __heap_is_stable()
{
    return ___heap_is_stable();
}

bool __set_allocation_enabled(bool new_value)
{
    return ___set_allocation_enabled(new_value);
}
