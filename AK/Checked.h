/*
 * Copyright (C) 2011-2019 Apple Inc. All rights reserved.
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/NumericLimits.h>
#include <AK/StdLibExtras.h>

namespace AK {

template<typename Destination, typename Source, bool destination_is_wider = (sizeof(Destination) >= sizeof(Source)), bool destination_is_signed = NumericLimits<Destination>::is_signed(), bool source_is_signed = NumericLimits<Source>::is_signed()>
struct TypeBoundsChecker;

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, false, false, false> {
    static constexpr bool is_within_range(Source value)
    {
        return value <= NumericLimits<Destination>::max();
    }
};

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, false, true, true> {
    static constexpr bool is_within_range(Source value)
    {
        return value <= NumericLimits<Destination>::max()
            && NumericLimits<Destination>::min() <= value;
    }
};

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, false, false, true> {
    static constexpr bool is_within_range(Source value)
    {
        return static_cast<MakeUnsigned<Source>>(value) <= NumericLimits<Destination>::max();
    }
};

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, false, true, false> {
    static constexpr bool is_within_range(Source value)
    {
        return value <= static_cast<Source>(NumericLimits<Destination>::max());
    }
};

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, true, false, false> {
    static constexpr bool is_within_range(Source)
    {
        return true;
    }
};

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, true, true, true> {
    static constexpr bool is_within_range(Source)
    {
        return true;
    }
};

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, true, false, true> {
    static constexpr bool is_within_range(Source value)
    {
        return value >= 0;
    }
};

template<typename Destination, typename Source>
struct TypeBoundsChecker<Destination, Source, true, true, false> {
    static constexpr bool is_within_range(Source value)
    {
        if (sizeof(Destination) > sizeof(Source))
            return true;
        return value <= static_cast<Source>(NumericLimits<Destination>::max());
    }
};

template<typename Destination, typename Source>
inline constexpr bool is_within_range(Source value)
{
    return TypeBoundsChecker<Destination, Source>::is_within_range(value);
}

template<typename T>
class Checked {
public:
    Checked()
        : m_value(0)
    {
    }

    Checked(T value)
        : m_value(value)
    {
    }

    template<typename U>
    Checked(U value)
    {
        m_overflow = !is_within_range<T>(value);
        m_value = value;
    }

    Checked(const Checked& other)
        : m_value(other.m_value)
        , m_overflow(other.m_overflow)
    {
    }

    Checked(Checked&& other)
        : m_value(exchange(other.m_value, 0))
        , m_overflow(exchange(other.m_overflow, false))
    {
    }

    template<typename U>
    Checked& operator=(U value)
    {
        return *this = Checked(value);
    }

    Checked& operator=(const Checked& other)
    {
        m_value = other.value();
        m_overflow = other.m_overflow;
        return *this;
    }

    Checked& operator=(Checked&& other)
    {
        m_value = exchange(other.m_value, 0);
        m_overflow = exchange(other.m_overflow, false);
        return *this;
    }

    bool has_overflow() const
    {
        return m_overflow;
    }

    ALWAYS_INLINE bool operator!() const
    {
        ASSERT(!m_overflow);
        return !m_value;
    }

    ALWAYS_INLINE T value() const
    {
        ASSERT(!m_overflow);
        return m_value;
    }

    void add(T other)
    {
        m_overflow |= __builtin_add_overflow(m_value, other, &m_value);
    }

    void sub(T other)
    {
        m_overflow |= __builtin_sub_overflow(m_value, other, &m_value);
    }

    void mul(T other)
    {
        m_overflow |= __builtin_mul_overflow(m_value, other, &m_value);
    }

    void div(T other)
    {
        m_value /= other;
    }

    Checked& operator+=(T other)
    {
        add(other);
        return *this;
    }

    Checked& operator-=(T other)
    {
        sub(other);
        return *this;
    }

    Checked& operator*=(T other)
    {
        mul(other);
        return *this;
    }

    Checked& operator/=(T other)
    {
        div(other);
        return *this;
    }

    Checked& operator++()
    {
        add(1);
        return *this;
    }

    Checked operator++(int)
    {
        Checked old { *this };
        add(1);
        return old;
    }

    template<typename U, typename V>
    static bool addition_would_overflow(U u, V v)
    {
#ifdef __clang__
        Checked checked;
        checked = u;
        checked += v;
        return checked.has_overflow();
#else
        return __builtin_add_overflow_p(u, v, (T)0);
#endif
    }

    template<typename U, typename V>
    static bool multiplication_would_overflow(U u, V v)
    {
#ifdef __clang__
        Checked checked;
        checked = u;
        checked *= v;
        return checked.has_overflow();
#else
        return __builtin_mul_overflow_p(u, v, (T)0);
#endif
    }

    template<typename U, typename V, typename X>
    static bool multiplication_would_overflow(U u, V v, X x)
    {
        Checked checked;
        checked = u;
        checked *= v;
        checked *= x;
        return checked.has_overflow();
    }

private:
    T m_value;
    bool m_overflow { false };
};

template<typename T>
inline Checked<T> operator+(const Checked<T>& a, const Checked<T>& b)
{
    Checked<T> c { a };
    c.add(b.value());
    return c;
}

template<typename T>
inline Checked<T> operator-(const Checked<T>& a, const Checked<T>& b)
{
    Checked<T> c { a };
    c.sub(b.value());
    return c;
}

template<typename T>
inline Checked<T> operator*(const Checked<T>& a, const Checked<T>& b)
{
    Checked<T> c { a };
    c.mul(b.value());
    return c;
}

template<typename T>
inline Checked<T> operator/(const Checked<T>& a, const Checked<T>& b)
{
    Checked<T> c { a };
    c.div(b.value());
    return c;
}

template<typename T>
inline bool operator<(const Checked<T>& a, T b)
{
    return a.value() < b;
}

template<typename T>
inline bool operator>(const Checked<T>& a, T b)
{
    return a.value() > b;
}

template<typename T>
inline bool operator>=(const Checked<T>& a, T b)
{
    return a.value() >= b;
}

template<typename T>
inline bool operator<=(const Checked<T>& a, T b)
{
    return a.value() <= b;
}

template<typename T>
inline bool operator==(const Checked<T>& a, T b)
{
    return a.value() == b;
}

template<typename T>
inline bool operator!=(const Checked<T>& a, T b)
{
    return a.value() != b;
}

template<typename T>
inline bool operator<(T a, const Checked<T>& b)
{
    return a < b.value();
}

template<typename T>
inline bool operator>(T a, const Checked<T>& b)
{
    return a > b.value();
}

template<typename T>
inline bool operator>=(T a, const Checked<T>& b)
{
    return a >= b.value();
}

template<typename T>
inline bool operator<=(T a, const Checked<T>& b)
{
    return a <= b.value();
}

template<typename T>
inline bool operator==(T a, const Checked<T>& b)
{
    return a == b.value();
}

template<typename T>
inline bool operator!=(T a, const Checked<T>& b)
{
    return a != b.value();
}

template<typename T>
inline Checked<T> make_checked(T value)
{
    return Checked<T>(value);
}

}

using AK::Checked;
using AK::make_checked;
