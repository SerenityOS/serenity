/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Traits.h>

namespace AK {

template<typename T>
class TypedTransfer {
public:
    static size_t move(T* destination, T* source, size_t count)
    {
        if constexpr (Traits<T>::is_trivial()) {
            __builtin_memmove(destination, source, count * sizeof(T));
            return count;
        }

        for (size_t i = 0; i < count; ++i) {
            if (destination <= source)
                new (&destination[i]) T(AK::move(source[i]));
            else
                new (&destination[count - i - 1]) T(AK::move(source[count - i - 1]));
        }

        return count;
    }

    static size_t copy(T* destination, const T* source, size_t count)
    {
        if constexpr (Traits<T>::is_trivial()) {
            __builtin_memmove(destination, source, count * sizeof(T));
            return count;
        }

        for (size_t i = 0; i < count; ++i) {
            if (destination <= source)
                new (&destination[i]) T(source[i]);
            else
                new (&destination[count - i - 1]) T(source[count - i - 1]);
        }

        return count;
    }

    static bool compare(const T* a, const T* b, size_t count)
    {
        if constexpr (Traits<T>::is_trivial())
            return !__builtin_memcmp(a, b, count * sizeof(T));

        for (size_t i = 0; i < count; ++i) {
            if (a[i] != b[i])
                return false;
        }

        return true;
    }
};

}
