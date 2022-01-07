/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/UFixedBigInt.h>
#include <string.h>

namespace UserspaceEmulator {

constexpr u64 _initialized_64 = 0x01010101'01010101LLU;
constexpr u128 _initialized_128 = u128(_initialized_64, _initialized_64);
constexpr u256 _initialized_256 = u256(_initialized_128, _initialized_128);

template<typename T>
class ValueAndShadowReference;

template<typename T>
class ValueWithShadow {
public:
    using ValueType = T;

    ValueWithShadow(T value, T shadow)
        : m_value(value)
        , m_shadow(shadow)
    {
    }

    ValueWithShadow(const ValueAndShadowReference<T>&);

    T value() const { return m_value; }
    T shadow() const { return m_shadow; }

    bool is_uninitialized() const
    {
        if constexpr (sizeof(T) == 32)
            return (m_shadow & _initialized_256) != _initialized_256;
        if constexpr (sizeof(T) == 16)
            return (m_shadow & _initialized_128) != _initialized_128;
        if constexpr (sizeof(T) == 8)
            return (m_shadow & _initialized_64) != _initialized_64;
        if constexpr (sizeof(T) == 4)
            return (m_shadow & 0x01010101) != 0x01010101;
        if constexpr (sizeof(T) == 2)
            return (m_shadow & 0x0101) != 0x0101;
        if constexpr (sizeof(T) == 1)
            return (m_shadow & 0x01) != 0x01;
    }

    void set_initialized()
    {
        if constexpr (sizeof(T) == 32)
            m_shadow = _initialized_256;
        if constexpr (sizeof(T) == 16)
            m_shadow = _initialized_128;
        if constexpr (sizeof(T) == 8)
            m_shadow = _initialized_64;
        if constexpr (sizeof(T) == 4)
            m_shadow = 0x01010101;
        if constexpr (sizeof(T) == 2)
            m_shadow = 0x0101;
        if constexpr (sizeof(T) == 1)
            m_shadow = 0x01;
    }

private:
    T m_value;
    T m_shadow;
};

template<typename T>
class ValueAndShadowReference {
public:
    using ValueType = T;

    ValueAndShadowReference(T& value, T& shadow)
        : m_value(value)
        , m_shadow(shadow)
    {
    }

    bool is_uninitialized() const
    {
        if constexpr (sizeof(T) == 32)
            return (m_shadow & _initialized_256) != _initialized_256;
        if constexpr (sizeof(T) == 16)
            return (m_shadow & _initialized_128) != _initialized_128;
        if constexpr (sizeof(T) == 8)
            return (m_shadow & _initialized_64) != _initialized_64;
        if constexpr (sizeof(T) == 4)
            return (m_shadow & 0x01010101) != 0x01010101;
        if constexpr (sizeof(T) == 2)
            return (m_shadow & 0x0101) != 0x0101;
        if constexpr (sizeof(T) == 1)
            return (m_shadow & 0x01) != 0x01;
    }

    ValueAndShadowReference<T>& operator=(const ValueWithShadow<T>&);

    T& value() { return m_value; }
    T& shadow() { return m_shadow; }

    const T& value() const { return m_value; }
    const T& shadow() const { return m_shadow; }

private:
    T& m_value;
    T& m_shadow;
};

template<typename T>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_as_initialized(T value)
{
    if constexpr (sizeof(T) == 32)
        return { value, _initialized_256 };
    if constexpr (sizeof(T) == 16)
        return { value, _initialized_128 };
    if constexpr (sizeof(T) == 8)
        return { value, _initialized_64 };
    if constexpr (sizeof(T) == 4)
        return { value, 0x01010101 };
    if constexpr (sizeof(T) == 2)
        return { value, 0x0101 };
    if constexpr (sizeof(T) == 1)
        return { value, 0x01 };
}

template<typename T, typename U>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_with_taint_from(T value, const U& taint_a)
{
    if (taint_a.is_uninitialized())
        return { value, 0 };
    return shadow_wrap_as_initialized(value);
}

template<typename T, typename U, typename V>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_with_taint_from(T value, const U& taint_a, const V& taint_b)
{
    if (taint_a.is_uninitialized() || taint_b.is_uninitialized())
        return { value, 0 };
    return shadow_wrap_as_initialized(value);
}

template<typename T, typename U, typename V, typename X>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_with_taint_from(T value, const U& taint_a, const V& taint_b, const X& taint_c)
{
    if (taint_a.is_uninitialized() || taint_b.is_uninitialized() || taint_c.is_uninitialized())
        return { value, 0 };
    return shadow_wrap_as_initialized(value);
}

template<typename T>
inline ValueWithShadow<T>::ValueWithShadow(const ValueAndShadowReference<T>& other)
    : m_value(other.value())
    , m_shadow(other.shadow())
{
}

template<typename T>
inline ValueAndShadowReference<T>& ValueAndShadowReference<T>::operator=(const ValueWithShadow<T>& other)
{
    m_value = other.value();
    m_shadow = other.shadow();
    return *this;
}

}

template<typename T>
struct AK::Formatter<UserspaceEmulator::ValueWithShadow<T>> : AK::Formatter<T> {
    ErrorOr<void> format(FormatBuilder& builder, UserspaceEmulator::ValueWithShadow<T> value)
    {
        return Formatter<T>::format(builder, value.value());
    }
};

#undef INITIALIZED_64
#undef INITIALIZED_128
#undef INITIALIZED_256
