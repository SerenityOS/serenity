/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/Types.h>

namespace Kernel {

#define SLAB_ALLOC_SCRUB_BYTE 0xab
#define SLAB_DEALLOC_SCRUB_BYTE 0xbc

void* slab_alloc(size_t slab_size);
void slab_dealloc(void*, size_t slab_size);
void slab_alloc_init();
ErrorOr<void> slab_alloc_stats(Function<ErrorOr<void>(size_t slab_size, size_t allocated, size_t free)>);

#define MAKE_SLAB_ALLOCATED(type)                                            \
public:                                                                      \
    [[nodiscard]] void* operator new(size_t)                                 \
    {                                                                        \
        void* ptr = slab_alloc(sizeof(type));                                \
        VERIFY(ptr);                                                         \
        return ptr;                                                          \
    }                                                                        \
    [[nodiscard]] void* operator new(size_t, const std::nothrow_t&) noexcept \
    {                                                                        \
        return slab_alloc(sizeof(type));                                     \
    }                                                                        \
    void operator delete(void* ptr) noexcept                                 \
    {                                                                        \
        if (!ptr)                                                            \
            return;                                                          \
        slab_dealloc(ptr, sizeof(type));                                     \
    }                                                                        \
                                                                             \
private:

}
