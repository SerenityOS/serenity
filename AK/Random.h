/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <stdlib.h>

#if defined(__unix__)
#    include <unistd.h>
#endif

#if defined(AK_OS_MACOS)
#    include <sys/random.h>
#endif

namespace AK {

inline void fill_with_random([[maybe_unused]] void* buffer, [[maybe_unused]] size_t length)
{
#if defined(AK_OS_SERENITY) || defined(AK_OS_ANDROID)
    arc4random_buf(buffer, length);
#elif defined(OSS_FUZZ)
#else
    auto fill_with_random_fallback = [&]() {
        char* char_buffer = static_cast<char*>(buffer);
        for (size_t i = 0; i < length; i++)
            char_buffer[i] = rand();
    };

#    if defined(__unix__) or defined(AK_OS_MACOS)
    // The maximum permitted value for the getentropy length argument.
    static constexpr size_t getentropy_length_limit = 256;

    auto iterations = length / getentropy_length_limit;
    auto remainder = length % getentropy_length_limit;
    auto address = reinterpret_cast<FlatPtr>(buffer);

    for (size_t i = 0; i < iterations; ++i) {
        if (getentropy(reinterpret_cast<void*>(address), getentropy_length_limit) != 0) {
            fill_with_random_fallback();
            return;
        }

        address += getentropy_length_limit;
    }

    if (remainder == 0 || getentropy(reinterpret_cast<void*>(address), remainder) == 0)
        return;
#    endif

    fill_with_random_fallback();
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

template<typename Collection>
inline void shuffle(Collection& collection)
{
    // Fisher-Yates shuffle
    for (size_t i = collection.size() - 1; i >= 1; --i) {
        size_t j = get_random_uniform(i + 1);
        AK::swap(collection[i], collection[j]);
    }
}

}

#if USING_AK_GLOBALLY
using AK::fill_with_random;
using AK::get_random;
using AK::get_random_uniform;
using AK::shuffle;
#endif
