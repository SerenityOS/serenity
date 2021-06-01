/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    using ValueType = T;

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

    ALWAYS_INLINE Optional(Optional& other)
        : m_has_value(other.m_has_value)
    {
        if (m_has_value) {
            new (&m_storage) T(other.value());
        }
    }

    template<typename U>
    ALWAYS_INLINE Optional(U& value)
        : m_has_value(true)
    {
        new (&m_storage) T(value);
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

    template<typename O>
    ALWAYS_INLINE bool operator==(O const& other) const
    {
        return has_value() && value() == other;
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
