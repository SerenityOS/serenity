/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef __serenity__
#    include <new>

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
                                                                          \
    private:
#else
#    define AK_MAKE_ETERNAL
#endif

#if defined(KERNEL)
#    include <Kernel/Heap/kmalloc.h>
#else
#    include <stdlib.h>

#    define kcalloc calloc
#    define kmalloc malloc
#    define kmalloc_good_size malloc_good_size
#    define kfree free
#    define krealloc realloc

#    ifdef __serenity__

#        include <AK/Assertions.h>
#        include <new>

inline void* operator new(size_t size)
{
    void* ptr = kmalloc(size);
    VERIFY(ptr);
    return ptr;
}

inline void* operator new(size_t size, const std::nothrow_t&) noexcept
{
    return kmalloc(size);
}

inline void operator delete(void* ptr) noexcept
{
    return kfree(ptr);
}

inline void operator delete(void* ptr, size_t) noexcept
{
    return kfree(ptr);
}

inline void* operator new[](size_t size)
{
    void* ptr = kmalloc(size);
    VERIFY(ptr);
    return ptr;
}

inline void* operator new[](size_t size, const std::nothrow_t&) noexcept
{
    return kmalloc(size);
}

inline void operator delete[](void* ptr) noexcept
{
    return kfree(ptr);
}

inline void operator delete[](void* ptr, size_t) noexcept
{
    return kfree(ptr);
}

#    endif

#endif

using std::nothrow;
