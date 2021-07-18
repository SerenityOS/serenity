/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if defined(KERNEL)
#    include <Kernel/Heap/kmalloc.h>
#else
#    include <new>
#    include <stdlib.h>

#    define kcalloc calloc
#    define kmalloc malloc
#    define kmalloc_good_size malloc_good_size
#    define kfree free

inline void kfree_sized(void* ptr, size_t)
{
    kfree(ptr);
}
#endif

#ifndef __serenity__
#    include <AK/Types.h>

#    ifndef AK_OS_MACOS
extern "C" {
inline size_t malloc_good_size(size_t size) { return size; }
}
#    else
#        include <malloc/malloc.h>
#    endif
#endif

#ifdef KERNEL
#    define AK_MAKE_ETERNAL                                                           \
    public:                                                                           \
        [[nodiscard]] void* operator new(size_t size)                                 \
        {                                                                             \
            void* ptr = kmalloc_eternal(size);                                        \
            VERIFY(ptr);                                                              \
            return ptr;                                                               \
        }                                                                             \
        [[nodiscard]] void* operator new(size_t size, const std::nothrow_t&) noexcept \
        {                                                                             \
            return kmalloc_eternal(size);                                             \
        }                                                                             \
                                                                                      \
    private:
#else
#    define AK_MAKE_ETERNAL
#endif

using std::nothrow;
