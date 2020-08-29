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

#include <AK/Assertions.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class alignas(T) [[nodiscard]] Optional
{
public:
    Optional() { }

    Optional(const T& value)
        : m_has_value(true)
    {
        new (&m_storage) T(value);
    }

    template<typename U>
    Optional(const U& value)
        : m_has_value(true)
    {
        new (&m_storage) T(value);
    }

    Optional(T && value)
        : m_has_value(true)
    {
        new (&m_storage) T(move(value));
    }

    Optional(Optional && other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value()) {
            new (&m_storage) T(other.release_value());
            other.m_has_value = false;
        }
    }

    Optional(const Optional& other)
        : m_has_value(other.m_has_value)
    {
        if (m_has_value) {
            new (&m_storage) T(other.value_without_consume_state());
        }
    }

    Optional& operator=(const Optional& other)
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

    Optional& operator=(Optional&& other)
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
    bool operator==(const Optional<O>& other) const
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
    ALWAYS_INLINE void emplace(Parameters && ... parameters)
    {
        clear();
        m_has_value = true;
        new (&m_storage) T(forward<Parameters>(parameters)...);
    }

    ALWAYS_INLINE bool has_value() const { return m_has_value; }

    ALWAYS_INLINE T& value()
    {
        ASSERT(m_has_value);
        return *reinterpret_cast<T*>(&m_storage);
    }

    ALWAYS_INLINE const T& value() const
    {
        return value_without_consume_state();
    }

    T release_value()
    {
        ASSERT(m_has_value);
        T released_value = move(value());
        value().~T();
        m_has_value = false;
        return released_value;
    }

    ALWAYS_INLINE T value_or(const T& fallback) const
    {
        if (m_has_value)
            return value();
        return fallback;
    }

private:
    // Call when we don't want to alter the consume state
    ALWAYS_INLINE const T& value_without_consume_state() const
    {
        ASSERT(m_has_value);
        return *reinterpret_cast<const T*>(&m_storage);
    }
    u8 m_storage[sizeof(T)] { 0 };
    bool m_has_value { false };
};

}

using AK::Optional;
