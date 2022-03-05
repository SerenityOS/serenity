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

template<typename T>
class ValueAndShadowReference;

template<typename T>
class ValueWithShadow {
public:
    using ValueType = T;
    using ShadowType = Array<u8, sizeof(T)>;

    ValueWithShadow() = default;

    ValueWithShadow(T value, T shadow)
        : m_value(value)
    {
        ReadonlyBytes { &shadow, sizeof(shadow) }.copy_to(m_shadow);
    }

    ValueWithShadow(T value, ShadowType shadow)
        : m_value(value)
        , m_shadow(shadow)
    {
    }

    static ValueWithShadow create_initialized(T value)
    {
        ShadowType shadow;
        shadow.fill(0x01);
        return {
            value,
            shadow,
        };
    }

    ValueWithShadow(ValueAndShadowReference<T> const&);

    T value() const { return m_value; }
    ShadowType const& shadow() const { return m_shadow; }

    T shadow_as_value() const requires(IsTriviallyConstructible<T>)
    {
        return *bit_cast<T const*>(m_shadow.data());
    }

    template<auto member>
    auto reference_to() requires(IsClass<T> || IsUnion<T>)
    {
        using ResultType = ValueAndShadowReference<RemoveReference<decltype(declval<T>().*member)>>;
        return ResultType {
            m_value.*member,
            *bit_cast<typename ResultType::ShadowType*>(m_shadow.span().offset_pointer(bit_cast<u8*>(member) - bit_cast<u8*>(nullptr))),
        };
    }

    template<auto member>
    auto slice() const requires(IsClass<T> || IsUnion<T>)
    {
        using ResultType = ValueWithShadow<RemoveReference<decltype(declval<T>().*member)>>;
        return ResultType {
            m_value.*member,
            *bit_cast<typename ResultType::ShadowType*>(m_shadow.span().offset_pointer(bit_cast<u8*>(member) - bit_cast<u8*>(nullptr))),
        };
    }

    bool is_uninitialized() const
    {
        for (size_t i = 0; i < sizeof(ShadowType); ++i) {
            if ((m_shadow[i] & 0x01) != 0x01)
                return true;
        }
        return false;
    }

    void set_initialized()
    {
        m_shadow.fill(0x01);
    }

private:
    T m_value {};
    ShadowType m_shadow {};
};

template<typename T>
class ValueAndShadowReference {
public:
    using ValueType = T;
    using ShadowType = Array<u8, sizeof(T)>;

    ValueAndShadowReference(T& value, ShadowType& shadow)
        : m_value(value)
        , m_shadow(shadow)
    {
    }

    bool is_uninitialized() const
    {
        for (size_t i = 0; i < sizeof(ShadowType); ++i) {
            if ((m_shadow[i] & 0x01) != 0x01)
                return true;
        }
        return false;
    }

    ValueAndShadowReference<T>& operator=(const ValueWithShadow<T>&);

    T shadow_as_value() const requires(IsTriviallyConstructible<T>)
    {
        return *bit_cast<T const*>(m_shadow.data());
    }

    T& value() { return m_value; }
    ShadowType& shadow() { return m_shadow; }

    T const& value() const { return m_value; }
    ShadowType const& shadow() const { return m_shadow; }

private:
    T& m_value;
    ShadowType& m_shadow;
};

template<typename T>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_as_initialized(T value)
{
    return ValueWithShadow<T>::create_initialized(value);
}

template<typename T, typename U>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_with_taint_from(T value, U const& taint_a)
{
    if (taint_a.is_uninitialized())
        return { value, 0 };
    return shadow_wrap_as_initialized(value);
}

template<typename T, typename U, typename V>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_with_taint_from(T value, U const& taint_a, V const& taint_b)
{
    if (taint_a.is_uninitialized() || taint_b.is_uninitialized())
        return { value, 0 };
    return shadow_wrap_as_initialized(value);
}

template<typename T, typename U, typename V, typename X>
ALWAYS_INLINE ValueWithShadow<T> shadow_wrap_with_taint_from(T value, U const& taint_a, V const& taint_b, X const& taint_c)
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
