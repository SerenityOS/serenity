/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

template<typename T>
class alignas(T) [[nodiscard]] Optional {
public:
    ALWAYS_INLINE Optional() = default;

    ALWAYS_INLINE Optional(const T& value)
        : m_has_value(true)
    {
        new (&m_storage) T(value);
    }

    template<typename U>
    ALWAYS_INLINE Optional(const U& value)
        : m_has_value(true)
    {
        new (&m_storage) T(value);
    }

    ALWAYS_INLINE Optional(T&& value)
        : m_has_value(true)
    {
        new (&m_storage) T(move(value));
    }

    ALWAYS_INLINE Optional(Optional&& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value()) {
            new (&m_storage) T(other.release_value());
            other.m_has_value = false;
        }
    }

    ALWAYS_INLINE Optional(const Optional& other)
        : m_has_value(other.m_has_value)
    {
        if (m_has_value) {
            new (&m_storage) T(other.value());
        }
    }

    ALWAYS_INLINE Optional& operator=(const Optional& other)
    {
        if (this != &other) {
            clear();
            m_has_value = other.m_has_value;
            if (m_has_value) {
                new (&m_storage) T(other.value());
            }
        }
        return *this;
    }

    ALWAYS_INLINE Optional& operator=(Optional&& other)
    {
        if (this != &other) {
            clear();
            m_has_value = other.m_has_value;
            if (other.has_value())
                new (&m_storage) T(other.release_value());
        }
        return *this;
    }

    template<typename O>
    ALWAYS_INLINE bool operator==(const Optional<O>& other) const
    {
        return has_value() == other.has_value() && (!has_value() || value() == other.value());
    }

    ALWAYS_INLINE ~Optional()
    {
        clear();
    }

    ALWAYS_INLINE void clear()
    {
        if (m_has_value) {
            value().~T();
            m_has_value = false;
        }
    }

    template<typename... Parameters>
    ALWAYS_INLINE void emplace(Parameters&&... parameters)
    {
        clear();
        m_has_value = true;
        new (&m_storage) T(forward<Parameters>(parameters)...);
    }

    [[nodiscard]] ALWAYS_INLINE bool has_value() const { return m_has_value; }

    [[nodiscard]] ALWAYS_INLINE T& value()
    {
        VERIFY(m_has_value);
        return *reinterpret_cast<T*>(&m_storage);
    }

    [[nodiscard]] ALWAYS_INLINE const T& value() const
    {
        VERIFY(m_has_value);
        return *reinterpret_cast<const T*>(&m_storage);
    }

    [[nodiscard]] T release_value()
    {
        VERIFY(m_has_value);
        T released_value = move(value());
        value().~T();
        m_has_value = false;
        return released_value;
    }

    [[nodiscard]] ALWAYS_INLINE T value_or(const T& fallback) const
    {
        if (m_has_value)
            return value();
        return fallback;
    }

    ALWAYS_INLINE const T& operator*() const { return value(); }
    ALWAYS_INLINE T& operator*() { return value(); }

    ALWAYS_INLINE const T* operator->() const { return &value(); }
    ALWAYS_INLINE T* operator->() { return &value(); }

private:
    u8 m_storage[sizeof(T)] { 0 };
    bool m_has_value { false };
};

}

using AK::Optional;
