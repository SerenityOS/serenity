/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/DistinctNumeric.h>

namespace AK {

template<typename T>
struct ArbitrarySizedEnum : public T {
    using T::T;

    consteval ArbitrarySizedEnum(T v)
        : T(v)
    {
    }

    constexpr ArbitrarySizedEnum(T v, Badge<ArbitrarySizedEnum<T>>)
        : T(v)
    {
    }

    template<Integral X>
    [[nodiscard]] consteval ArbitrarySizedEnum<T> operator<<(X other) const
    {
        return T(this->value() << other);
    }

    template<Integral X>
    constexpr ArbitrarySizedEnum<T>& operator<<=(X other)
    {
        this->value() <<= other;
        return *this;
    }

    template<Integral X>
    [[nodiscard]] consteval ArbitrarySizedEnum<T> operator>>(X other) const
    {
        return T(this->value() >> other);
    }

    template<Integral X>
    constexpr ArbitrarySizedEnum<T>& operator>>=(X other)
    {
        this->value() >>= other;
        return *this;
    }

    template<Integral X>
    [[nodiscard]] constexpr bool operator==(X other) const
    {
        return this->value() == T(other);
    }

    [[nodiscard]] constexpr bool operator==(ArbitrarySizedEnum<T> const& other) const
    {
        return this->value() == other.value();
    }

    // NOTE: The following operators mirror AK_ENUM_BITWISE_OPERATORS.

    [[nodiscard]] constexpr ArbitrarySizedEnum<T> operator|(ArbitrarySizedEnum<T> const& other) const
    {
        return { T(this->value() | other.value()), Badge<ArbitrarySizedEnum<T>> {} };
    }

    [[nodiscard]] constexpr ArbitrarySizedEnum<T> operator&(ArbitrarySizedEnum<T> const& other) const
    {
        return { T(this->value() & other.value()), Badge<ArbitrarySizedEnum<T>> {} };
    }

    [[nodiscard]] constexpr ArbitrarySizedEnum<T> operator^(ArbitrarySizedEnum<T> const& other) const
    {
        return { T(this->value() ^ other.value()), Badge<ArbitrarySizedEnum<T>> {} };
    }

    [[nodiscard]] constexpr ArbitrarySizedEnum<T> operator~() const
    {
        return { T(~this->value()), Badge<ArbitrarySizedEnum<T>> {} };
    }

    constexpr ArbitrarySizedEnum<T>& operator|=(ArbitrarySizedEnum<T> const& other)
    {
        this->value() |= other.value();
        return *this;
    }

    constexpr ArbitrarySizedEnum<T>& operator&=(ArbitrarySizedEnum<T> const& other)
    {
        this->value() &= other.value();
        return *this;
    }

    constexpr ArbitrarySizedEnum<T>& operator^=(ArbitrarySizedEnum<T> const& other)
    {
        this->value() ^= other.value();
        return *this;
    }

    [[nodiscard]] constexpr bool has_flag(ArbitrarySizedEnum<T> const& mask) const
    {
        return (*this & mask) == mask;
    }

    [[nodiscard]] constexpr bool has_any_flag(ArbitrarySizedEnum<T> const& mask) const
    {
        return (*this & mask) != 0u;
    }
};

#define AK_MAKE_ARBITRARY_SIZED_ENUM(EnumName, T, ...)                                                                         \
    namespace EnumName {                                                                                                       \
    using EnumName = ArbitrarySizedEnum<DistinctNumeric<T, struct __##EnumName##Tag, AK::DistinctNumericFeature::Comparison>>; \
    using Type = EnumName;                                                                                                     \
    using UnderlyingType = T;                                                                                                  \
    inline constexpr static EnumName __VA_ARGS__;                                                                              \
    }

}

#if USING_AK_GLOBALLY
using AK::ArbitrarySizedEnum;
#endif
