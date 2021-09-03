/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Assertions.h>
#include <YAK/StdLibExtras.h>
#include <YAK/Types.h>
#include <YAK/kmalloc.h>

namespace YAK {

template<typename T>
class [[nodiscard]] Optional {
public:
    using ValueType = T;

    ALWAYS_INLINE Optional() = default;

#ifdef YAK_HAS_CONDITIONALLY_TRIVIAL
    Optional(const Optional& other) requires(!IsCopyConstructible<T>) = delete;
    Optional(const Optional& other) = default;

    Optional(Optional&& other) requires(!IsMoveConstructible<T>) = delete;

    Optional& operator=(const Optional&) requires(!IsCopyConstructible<T> || !IsDestructible<T>) = delete;
    Optional& operator=(const Optional&) = default;

    Optional& operator=(Optional&& other) requires(!IsMoveConstructible<T> || !IsDestructible<T>) = delete;

    ~Optional() requires(!IsDestructible<T>) = delete;
    ~Optional() = default;
#endif

    ALWAYS_INLINE Optional(const Optional& other)
#ifdef YAK_HAS_CONDITIONALLY_TRIVIAL
        requires(!IsTriviallyCopyConstructible<T>)
#endif
        : m_has_value(other.m_has_value)
    {
        if (other.has_value()) {
            new (&m_storage) T(other.value());
        }
    }

    ALWAYS_INLINE Optional(Optional&& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value()) {
            new (&m_storage) T(other.release_value());
        }
    }

    template<typename U = T>
    ALWAYS_INLINE explicit(!IsConvertible<U&&, T>) Optional(U&& value) requires(!IsSame<RemoveCVReference<U>, Optional<T>> && IsConstructible<T, U&&>)
        : m_has_value(true)
    {
        new (&m_storage) T(forward<U>(value));
    }

    ALWAYS_INLINE Optional& operator=(const Optional& other)
#ifdef YAK_HAS_CONDITIONALLY_TRIVIAL
        requires(!IsTriviallyCopyConstructible<T> || !IsTriviallyDestructible<T>)
#endif
    {
        if (this != &other) {
            clear();
            m_has_value = other.m_has_value;
            if (other.has_value()) {
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
            if (other.has_value()) {
                new (&m_storage) T(other.release_value());
            }
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
#ifdef YAK_HAS_CONDITIONALLY_TRIVIAL
        requires(!IsTriviallyDestructible<T>)
#endif
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
        return *__builtin_launder(reinterpret_cast<T*>(&m_storage));
    }

    [[nodiscard]] ALWAYS_INLINE const T& value() const
    {
        VERIFY(m_has_value);
        return *__builtin_launder(reinterpret_cast<const T*>(&m_storage));
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
    alignas(T) u8 m_storage[sizeof(T)];
    bool m_has_value { false };
};
}

using YAK::Optional;
