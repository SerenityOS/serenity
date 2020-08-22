/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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

#include <AK/Types.h>

namespace AK {

/**
 * This implements a "distinct" numeric type that is intentionally incompatible
 * to other incantations. The intention is that each "distinct" type that you
 * want simply gets different values for `fn_length` and `line`. The macros
 * `TYPEDEF_DISTINCT_NUMERIC_*()` at the bottom of `DistinctNumeric.h`.
 *
 * `Incr`, `Cmp`, `Bool`, `Flags`, `Shift`, and `Arith` simply split up the
 * space of operators into 6 simple categories:
 * - No matter the values of these, `DistinctNumeric` always implements `==` and `!=`.
 * - If `Incr` is true, then `++a`, `a++`, `--a`, and `a--` are implemented.
 * - If `Cmp` is true, then `a>b`, `a<b`, `a>=b`, and `a<=b` are implemented.
 * - If `Bool` is true, then `!a`, `a&&b`, and `a||b` are implemented (but not `operator bool()`, because of overzealous integer promotion rules).
 * - If `Flags` is true, then `~a`, `a&b`, `a|b`, `a^b`, `a&=b`, `a|=b`, and `a^=b` are implemented.
 * - If `Shift` is true, then `a<<b`, `a>>b`, `a<<=b`, `a>>=b` are implemented.
 * - If `Arith` is true, then `a+b`, `a-b`, `+a`, `-a`, `a*b`, `a/b`, `a%b`, and the respective `a_=b` versions are implemented.
 * The semantics are always those of the underlying basic type `T`.
 *
 * These can be combined arbitrarily. Want a numeric type that supports `++a`
 * and `a >> b` but not `a > b`? Sure thing, just set
 * `Incr=true, Cmp=false, Shift=true` and you're done!
 * Furthermore, some of these overloads make more sense with specific types, like `a&&b` which should be able to operate
 *
 * I intentionally decided against overloading `&a` because these shall remain
 * numeric types.
 *
 * The C++20 `operator<=>` would require, among other things `std::weak_equality`.
 * Since we do not have that, it cannot be implemented.
 *
 * The are many operators that do not work on `int`, so I left them out:
 * `a[b]`, `*a`, `a->b`, `a.b`, `a->*b`, `a.*b`.
 *
 * There are many more operators that do not make sense for numerical types,
 * or cannot be overloaded in the first place. Naturally, they are not implemented.
 */
template<typename T, bool Incr, bool Cmp, bool Bool, bool Flags, bool Shift, bool Arith, typename X>
class DistinctNumeric {
    typedef DistinctNumeric<T, Incr, Cmp, Bool, Flags, Shift, Arith, X> Self;

public:
    DistinctNumeric(T value)
        : m_value { value }
    {
    }

    const T& value() const { return m_value; }

    // Always implemented: identity.
    bool operator==(const Self& other) const
    {
        return this->m_value == other.m_value;
    }
    bool operator!=(const Self& other) const
    {
        return this->m_value != other.m_value;
    }

    // Only implemented when `Incr` is true:
    Self& operator++()
    {
        static_assert(Incr, "'++a' is only available for DistinctNumeric types with 'Incr'.");
        this->m_value += 1;
        return *this;
    }
    Self operator++(int)
    {
        static_assert(Incr, "'a++' is only available for DistinctNumeric types with 'Incr'.");
        Self ret = this->m_value;
        this->m_value += 1;
        return ret;
    }
    Self& operator--()
    {
        static_assert(Incr, "'--a' is only available for DistinctNumeric types with 'Incr'.");
        this->m_value -= 1;
        return *this;
    }
    Self operator--(int)
    {
        static_assert(Incr, "'a--' is only available for DistinctNumeric types with 'Incr'.");
        Self ret = this->m_value;
        this->m_value -= 1;
        return ret;
    }

    // Only implemented when `Cmp` is true:
    bool operator>(const Self& other) const
    {
        static_assert(Cmp, "'a>b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value > other.m_value;
    }
    bool operator<(const Self& other) const
    {
        static_assert(Cmp, "'a<b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value < other.m_value;
    }
    bool operator>=(const Self& other) const
    {
        static_assert(Cmp, "'a>=b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value >= other.m_value;
    }
    bool operator<=(const Self& other) const
    {
        static_assert(Cmp, "'a<=b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value <= other.m_value;
    }
    // 'operator<=>' cannot be implemented. See class comment.

    // Only implemented when `bool` is true:
    bool operator!() const
    {
        static_assert(Bool, "'!a' is only available for DistinctNumeric types with 'Bool'.");
        return !this->m_value;
    }
    bool operator&&(const Self& other) const
    {
        static_assert(Bool, "'a&&b' is only available for DistinctNumeric types with 'Bool'.");
        return this->m_value && other.m_value;
    }
    bool operator||(const Self& other) const
    {
        static_assert(Bool, "'a||b' is only available for DistinctNumeric types with 'Bool'.");
        return this->m_value || other.m_value;
    }
    // Intentionally don't define `operator bool() const` here. C++ is a bit
    // overzealos, and whenever there would be a type error, C++ instead tries
    // to convert to a common int-ish type first. `bool` is int-ish, so
    // `operator bool() const` would defy the entire point of this class.

    // Only implemented when `Flags` is true:
    Self operator~() const
    {
        static_assert(Flags, "'~a' is only available for DistinctNumeric types with 'Flags'.");
        return ~this->m_value;
    }
    Self operator&(const Self& other) const
    {
        static_assert(Flags, "'a&b' is only available for DistinctNumeric types with 'Flags'.");
        return this->m_value & other.m_value;
    }
    Self operator|(const Self& other) const
    {
        static_assert(Flags, "'a|b' is only available for DistinctNumeric types with 'Flags'.");
        return this->m_value | other.m_value;
    }
    Self operator^(const Self& other) const
    {
        static_assert(Flags, "'a^b' is only available for DistinctNumeric types with 'Flags'.");
        return this->m_value ^ other.m_value;
    }
    Self& operator&=(const Self& other)
    {
        static_assert(Flags, "'a&=b' is only available for DistinctNumeric types with 'Flags'.");
        this->m_value &= other.m_value;
        return *this;
    }
    Self& operator|=(const Self& other)
    {
        static_assert(Flags, "'a|=b' is only available for DistinctNumeric types with 'Flags'.");
        this->m_value |= other.m_value;
        return *this;
    }
    Self& operator^=(const Self& other)
    {
        static_assert(Flags, "'a^=b' is only available for DistinctNumeric types with 'Flags'.");
        this->m_value ^= other.m_value;
        return *this;
    }

    // Only implemented when `Shift` is true:
    // TODO: Should this take `int` instead?
    Self operator<<(const Self& other) const
    {
        static_assert(Shift, "'a<<b' is only available for DistinctNumeric types with 'Shift'.");
        return this->m_value << other.m_value;
    }
    Self operator>>(const Self& other) const
    {
        static_assert(Shift, "'a>>b' is only available for DistinctNumeric types with 'Shift'.");
        return this->m_value >> other.m_value;
    }
    Self& operator<<=(const Self& other)
    {
        static_assert(Shift, "'a<<=b' is only available for DistinctNumeric types with 'Shift'.");
        this->m_value <<= other.m_value;
        return *this;
    }
    Self& operator>>=(const Self& other)
    {
        static_assert(Shift, "'a>>=b' is only available for DistinctNumeric types with 'Shift'.");
        this->m_value >>= other.m_value;
        return *this;
    }

    // Only implemented when `Arith` is true:
    Self operator+(const Self& other) const
    {
        static_assert(Arith, "'a+b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value + other.m_value;
    }
    Self operator-(const Self& other) const
    {
        static_assert(Arith, "'a-b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value - other.m_value;
    }
    Self operator+() const
    {
        static_assert(Arith, "'+a' is only available for DistinctNumeric types with 'Arith'.");
        return +this->m_value;
    }
    Self operator-() const
    {
        static_assert(Arith, "'-a' is only available for DistinctNumeric types with 'Arith'.");
        return -this->m_value;
    }
    Self operator*(const Self& other) const
    {
        static_assert(Arith, "'a*b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value * other.m_value;
    }
    Self operator/(const Self& other) const
    {
        static_assert(Arith, "'a/b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value / other.m_value;
    }
    Self operator%(const Self& other) const
    {
        static_assert(Arith, "'a%b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value % other.m_value;
    }
    Self& operator+=(const Self& other)
    {
        static_assert(Arith, "'a+=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value += other.m_value;
        return *this;
    }
    Self& operator-=(const Self& other)
    {
        static_assert(Arith, "'a+=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value += other.m_value;
        return *this;
    }
    Self& operator*=(const Self& other)
    {
        static_assert(Arith, "'a*=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value *= other.m_value;
        return *this;
    }
    Self& operator/=(const Self& other)
    {
        static_assert(Arith, "'a/=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value /= other.m_value;
        return *this;
    }
    Self& operator%=(const Self& other)
    {
        static_assert(Arith, "'a%=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value %= other.m_value;
        return *this;
    }

private:
    T m_value;
};

// TODO: When 'consteval' sufficiently-well supported by host compilers, try to
// provide a more usable interface like this one:
// https://gist.github.com/alimpfard/a3b750e8c3a2f44fb3a2d32038968ddf

}

#define TYPEDEF_DISTINCT_NUMERIC_GENERAL(T, Incr, Cmp, Bool, Flags, Shift, Arith, NAME) \
    typedef DistinctNumeric<T, Incr, Cmp, Bool, Flags, Shift, Arith, struct __##NAME##_tag> NAME
#define TYPEDEF_DISTINCT_ORDERED_ID(T, NAME) TYPEDEF_DISTINCT_NUMERIC_GENERAL(T, false, true, true, false, false, false, NAME)
// TODO: Further typedef's?

using AK::DistinctNumeric;
