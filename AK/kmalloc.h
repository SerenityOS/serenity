/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Checked.h>

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
#    define AK_MAKE_ETERNAL                                               \
    public:                                                               \
        void* operator new(size_t size) { return kmalloc_eternal(size); } \
        void operator delete(void*, size_t) { VERIFY_NOT_REACHED(); }     \
                                                                          \
    private:
#else
#    define AK_MAKE_ETERNAL
#endif

using std::nothrow;

inline void* kmalloc_array(Checked<size_t> a, Checked<size_t> b)
{
    auto size = a * b;
    VERIFY(!size.has_overflow());
    return kmalloc(size.value());
}

inline void* kmalloc_array(Checked<size_t> a, Checked<size_t> b, Checked<size_t> c)
{
    auto size = a * b * c;
    VERIFY(!size.has_overflow());
    return kmalloc(size.value());
}
