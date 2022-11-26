/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Types.h>

#if defined(AK_OS_SERENITY) || defined(AK_OS_ANDROID)
#    include <stdlib.h>
#endif

#if defined(__unix__)
#    include <unistd.h>
#endif

#if defined(AK_OS_MACOS)
#    include <sys/random.h>
#endif

#if defined(AK_OS_WINDOWS)
#    include <random>
#    include <unistd.h>
#endif

namespace AK {

inline void fill_with_random([[maybe_unused]] void* buffer, [[maybe_unused]] size_t length)
{
#if defined(AK_OS_SERENITY) || defined(AK_OS_ANDROID)
    arc4random_buf(buffer, length);
#elif defined(OSS_FUZZ)
#elif defined(__unix__) or defined(AK_OS_MACOS)
    [[maybe_unused]] int rc = getentropy(buffer, length);
#else
    char* char_buffer = static_cast<char*>(buffer);
    for (size_t i = 0; i < length; i++) {
        char_buffer[i] = std::rand();
    }
#endif
}

template<typename T>
inline T get_random()
{
    T t;
    fill_with_random(&t, sizeof(T));
    return t;
}

u32 get_random_uniform(u32 max_bounds);

}

#if USING_AK_GLOBALLY
using AK::fill_with_random;
using AK::get_random;
using AK::get_random_uniform;
#endif
