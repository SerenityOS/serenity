/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

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
#elif defined(BOOTSTRAPPER)
#    include <Bootstrapper/Memory/malloc.h>
#else
#    include <stdlib.h>

#    define kcalloc calloc
#    define kmalloc malloc
#    define kfree free
#    define krealloc realloc

#    ifdef __serenity__

inline void* operator new(size_t size)
{
    return kmalloc(size);
}

inline void operator delete(void* ptr)
{
    return kfree(ptr);
}

inline void operator delete(void* ptr, size_t)
{
    return kfree(ptr);
}

inline void* operator new[](size_t size)
{
    return kmalloc(size);
}

inline void operator delete[](void* ptr)
{
    return kfree(ptr);
}

inline void operator delete[](void* ptr, size_t)
{
    return kfree(ptr);
}

inline void* operator new(size_t, void* ptr)
{
    return ptr;
}

inline void* operator new[](size_t, void* ptr)
{
    return ptr;
}
#    endif

#endif
