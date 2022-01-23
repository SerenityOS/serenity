/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

// NOTE: If you're here because of an internal compiler error in GCC 10.3.0+,
//       it's because of the following bug:
//
//       https://gcc.gnu.org/bugzilla/show_bug.cgi?id=96745
//
//       Make sure you didn't accidentally make your destructor private before
//       you start bug hunting. :^)

template<typename T>
class [[nodiscard]] Optional {
    template<typename U>
    friend class Optional;

public:
    using ValueType = T;

    ALWAYS_INLINE Optional() = default;

#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
    Optional(Optional const& other) requires(!IsCopyConstructible<T>) = delete;
    Optional(Optional const& other) = default;

    Optional(Optional&& other) requires(!IsMoveConstructible<T>) = delete;

    Optional& operator=(Optional const&) requires(!IsCopyConstructible<T> || !IsDestructible<T>) = delete;
    Optional& operator=(Optional const&) = default;

    Optional& operator=(Optional&& other) requires(!IsMoveConstructible<T> || !IsDestructible<T>) = delete;

    ~Optional() requires(!IsDestructible<T>) = delete;
    ~Optional() = default;
#endif

    ALWAYS_INLINE Optional(Optional const& other)
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
        requires(!IsTriviallyCopyConstructible<T>)
#endif
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.value());
    }

    ALWAYS_INLINE Optional(Optional&& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.release_value());
    }

    template<typename U>
    requires(IsConstructible<T, U const&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional>) ALWAYS_INLINE explicit Optional(Optional<U> const& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.value());
    }

    template<typename U>
    requires(IsConstructible<T, U&&> && !IsSpecializationOf<T, Optional> && !IsSpecializationOf<U, Optional>) ALWAYS_INLINE explicit Optional(Optional<U>&& other)
        : m_has_value(other.m_has_value)
    {
        if (other.has_value())
            new (&m_storage) T(other.release_value());
    }

    template<typename U = T>
    ALWAYS_INLINE explicit(!IsConvertible<U&&, T>) Optional(U&& value) requires(!IsSame<RemoveCVReference<U>, Optional<T>> && IsConstructible<T, U&&>)
        : m_has_value(true)
    {
        new (&m_storage) T(forward<U>(value));
    }

    ALWAYS_INLINE Optional& operator=(Optional const& other)
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
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
    ALWAYS_INLINE bool operator==(Optional<O> const& other) const
    {
        return has_value() == other.has_value() && (!has_value() || value() == other.value());
    }

    template<typename O>
    ALWAYS_INLINE bool operator==(O const& other) const
    {
        return has_value() && value() == other;
    }

    ALWAYS_INLINE ~Optional()
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
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

    [[nodiscard]] ALWAYS_INLINE T& value() &
    {
        VERIFY(m_has_value);
        return *__builtin_launder(reinterpret_cast<T*>(&m_storage));
    }

    [[nodiscard]] ALWAYS_INLINE T const& value() const&
    {
        VERIFY(m_has_value);
        return *__builtin_launder(reinterpret_cast<T const*>(&m_storage));
    }

    [[nodiscard]] ALWAYS_INLINE T value() &&
    {
        return release_value();
    }

    [[nodiscard]] ALWAYS_INLINE T release_value()
    {
        VERIFY(m_has_value);
        T released_value = move(value());
        value().~T();
        m_has_value = false;
        return released_value;
    }

    [[nodiscard]] ALWAYS_INLINE T value_or(T const& fallback) const&
    {
        if (m_has_value)
            return value();
        return fallback;
    }

    [[nodiscard]] ALWAYS_INLINE T value_or(T&& fallback) &&
    {
        if (m_has_value)
            return move(value());
        return move(fallback);
    }

    ALWAYS_INLINE T const& operator*() const { return value(); }
    ALWAYS_INLINE T& operator*() { return value(); }

    ALWAYS_INLINE T const* operator->() const { return &value(); }
    ALWAYS_INLINE T* operator->() { return &value(); }

private:
    alignas(T) u8 m_storage[sizeof(T)];
    bool m_has_value { false };
};
}

using AK::Optional;
