/*
 * Copyright (c) 2020, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Traits.h>
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
template<typename T, typename X, bool Incr, bool Cmp, bool Bool, bool Flags, bool Shift, bool Arith>
class DistinctNumeric {
    using Self = DistinctNumeric<T, X, Incr, Cmp, Bool, Flags, Shift, Arith>;

public:
    constexpr DistinctNumeric() = default;

    constexpr DistinctNumeric(T value)
        : m_value { value }
    {
    }

    constexpr const T& value() const { return m_value; }

    // Always implemented: identity.
    constexpr bool operator==(const Self& other) const
    {
        return this->m_value == other.m_value;
    }
    constexpr bool operator!=(const Self& other) const
    {
        return this->m_value != other.m_value;
    }

    // Only implemented when `Incr` is true:
    constexpr Self& operator++()
    {
        static_assert(Incr, "'++a' is only available for DistinctNumeric types with 'Incr'.");
        this->m_value += 1;
        return *this;
    }
    constexpr Self operator++(int)
    {
        static_assert(Incr, "'a++' is only available for DistinctNumeric types with 'Incr'.");
        Self ret = this->m_value;
        this->m_value += 1;
        return ret;
    }
    constexpr Self& operator--()
    {
        static_assert(Incr, "'--a' is only available for DistinctNumeric types with 'Incr'.");
        this->m_value -= 1;
        return *this;
    }
    constexpr Self operator--(int)
    {
        static_assert(Incr, "'a--' is only available for DistinctNumeric types with 'Incr'.");
        Self ret = this->m_value;
        this->m_value -= 1;
        return ret;
    }

    // Only implemented when `Cmp` is true:
    constexpr bool operator>(const Self& other) const
    {
        static_assert(Cmp, "'a>b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value > other.m_value;
    }
    constexpr bool operator<(const Self& other) const
    {
        static_assert(Cmp, "'a<b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value < other.m_value;
    }
    constexpr bool operator>=(const Self& other) const
    {
        static_assert(Cmp, "'a>=b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value >= other.m_value;
    }
    constexpr bool operator<=(const Self& other) const
    {
        static_assert(Cmp, "'a<=b' is only available for DistinctNumeric types with 'Cmp'.");
        return this->m_value <= other.m_value;
    }
    // 'operator<=>' cannot be implemented. See class comment.

    // Only implemented when `bool` is true:
    constexpr bool operator!() const
    {
        static_assert(Bool, "'!a' is only available for DistinctNumeric types with 'Bool'.");
        return !this->m_value;
    }
    // Intentionally don't define `operator bool() const` here. C++ is a bit
    // overzealous, and whenever there would be a type error, C++ instead tries
    // to convert to a common int-ish type first. `bool` is int-ish, so
    // `operator bool() const` would defy the entire point of this class.

    // Only implemented when `Flags` is true:
    constexpr Self operator~() const
    {
        static_assert(Flags, "'~a' is only available for DistinctNumeric types with 'Flags'.");
        return ~this->m_value;
    }
    constexpr Self operator&(const Self& other) const
    {
        static_assert(Flags, "'a&b' is only available for DistinctNumeric types with 'Flags'.");
        return this->m_value & other.m_value;
    }
    constexpr Self operator|(const Self& other) const
    {
        static_assert(Flags, "'a|b' is only available for DistinctNumeric types with 'Flags'.");
        return this->m_value | other.m_value;
    }
    constexpr Self operator^(const Self& other) const
    {
        static_assert(Flags, "'a^b' is only available for DistinctNumeric types with 'Flags'.");
        return this->m_value ^ other.m_value;
    }
    constexpr Self& operator&=(const Self& other)
    {
        static_assert(Flags, "'a&=b' is only available for DistinctNumeric types with 'Flags'.");
        this->m_value &= other.m_value;
        return *this;
    }
    constexpr Self& operator|=(const Self& other)
    {
        static_assert(Flags, "'a|=b' is only available for DistinctNumeric types with 'Flags'.");
        this->m_value |= other.m_value;
        return *this;
    }
    constexpr Self& operator^=(const Self& other)
    {
        static_assert(Flags, "'a^=b' is only available for DistinctNumeric types with 'Flags'.");
        this->m_value ^= other.m_value;
        return *this;
    }

    // Only implemented when `Shift` is true:
    // TODO: Should this take `int` instead?
    constexpr Self operator<<(const Self& other) const
    {
        static_assert(Shift, "'a<<b' is only available for DistinctNumeric types with 'Shift'.");
        return this->m_value << other.m_value;
    }
    constexpr Self operator>>(const Self& other) const
    {
        static_assert(Shift, "'a>>b' is only available for DistinctNumeric types with 'Shift'.");
        return this->m_value >> other.m_value;
    }
    constexpr Self& operator<<=(const Self& other)
    {
        static_assert(Shift, "'a<<=b' is only available for DistinctNumeric types with 'Shift'.");
        this->m_value <<= other.m_value;
        return *this;
    }
    constexpr Self& operator>>=(const Self& other)
    {
        static_assert(Shift, "'a>>=b' is only available for DistinctNumeric types with 'Shift'.");
        this->m_value >>= other.m_value;
        return *this;
    }

    // Only implemented when `Arith` is true:
    constexpr Self operator+(const Self& other) const
    {
        static_assert(Arith, "'a+b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value + other.m_value;
    }
    constexpr Self operator-(const Self& other) const
    {
        static_assert(Arith, "'a-b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value - other.m_value;
    }
    constexpr Self operator+() const
    {
        static_assert(Arith, "'+a' is only available for DistinctNumeric types with 'Arith'.");
        return +this->m_value;
    }
    constexpr Self operator-() const
    {
        static_assert(Arith, "'-a' is only available for DistinctNumeric types with 'Arith'.");
        return -this->m_value;
    }
    constexpr Self operator*(const Self& other) const
    {
        static_assert(Arith, "'a*b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value * other.m_value;
    }
    constexpr Self operator/(const Self& other) const
    {
        static_assert(Arith, "'a/b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value / other.m_value;
    }
    constexpr Self operator%(const Self& other) const
    {
        static_assert(Arith, "'a%b' is only available for DistinctNumeric types with 'Arith'.");
        return this->m_value % other.m_value;
    }
    constexpr Self& operator+=(const Self& other)
    {
        static_assert(Arith, "'a+=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value += other.m_value;
        return *this;
    }
    constexpr Self& operator-=(const Self& other)
    {
        static_assert(Arith, "'a+=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value += other.m_value;
        return *this;
    }
    constexpr Self& operator*=(const Self& other)
    {
        static_assert(Arith, "'a*=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value *= other.m_value;
        return *this;
    }
    constexpr Self& operator/=(const Self& other)
    {
        static_assert(Arith, "'a/=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value /= other.m_value;
        return *this;
    }
    constexpr Self& operator%=(const Self& other)
    {
        static_assert(Arith, "'a%=b' is only available for DistinctNumeric types with 'Arith'.");
        this->m_value %= other.m_value;
        return *this;
    }

private:
    T m_value {};
};

template<typename T, typename X, bool Incr, bool Cmp, bool Bool, bool Flags, bool Shift, bool Arith>
struct Formatter<DistinctNumeric<T, X, Incr, Cmp, Bool, Flags, Shift, Arith>> : Formatter<T> {
    ErrorOr<void> format(FormatBuilder& builder, DistinctNumeric<T, X, Incr, Cmp, Bool, Flags, Shift, Arith> value)
    {
        return Formatter<T>::format(builder, value.value());
    }
};

// TODO: When 'consteval' sufficiently-well supported by host compilers, try to
// provide a more usable interface like this one:
// https://gist.github.com/alimpfard/a3b750e8c3a2f44fb3a2d32038968ddf

}

#define TYPEDEF_DISTINCT_NUMERIC_GENERAL(T, Incr, Cmp, Bool, Flags, Shift, Arith, NAME) \
    using NAME = DistinctNumeric<T, struct __##NAME##_tag, Incr, Cmp, Bool, Flags, Shift, Arith>;
#define TYPEDEF_DISTINCT_ORDERED_ID(T, NAME) TYPEDEF_DISTINCT_NUMERIC_GENERAL(T, false, true, true, false, false, false, NAME)
// TODO: Further type aliases?

template<typename T, typename X, auto... Args>
struct Traits<AK::DistinctNumeric<T, X, Args...>> : public GenericTraits<AK::DistinctNumeric<T, X, Args...>> {
    static constexpr bool is_trivial() { return true; }
    static constexpr auto hash(const DistinctNumeric<T, X, Args...>& d) { return Traits<T>::hash(d.value()); }
};

using AK::DistinctNumeric;
