/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, César Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Platform.h>
#include <AK/Types.h>

namespace AK {

static ALWAYS_INLINE void atomic_signal_fence(MemoryOrder order) noexcept
{
    return __atomic_signal_fence(order);
}

static ALWAYS_INLINE void atomic_thread_fence(MemoryOrder order) noexcept
{
    return __atomic_thread_fence(order);
}

static ALWAYS_INLINE void full_memory_barrier() noexcept
{
    atomic_signal_fence(AK::MemoryOrder::memory_order_acq_rel);
    atomic_thread_fence(AK::MemoryOrder::memory_order_acq_rel);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_exchange(T volatile* var, T desired, MemoryOrder order = memory_order_seq_cst) noexcept
{
    // FIXME: Remove once clang allows implements atomic enums
#ifdef __clang__
    if constexpr (Enum<T>)
        return (T)__atomic_exchange_n(reinterpret_cast<UnderlyingType<T>*>(const_cast<T*>(var)), static_cast<UnderlyingType<T>>(desired), order);
    else
#endif
        return __atomic_exchange_n(var, desired, order);
}

template<IntegralLike T, typename V = RemoveVolatile<T>>
[[nodiscard]] static ALWAYS_INLINE bool atomic_compare_exchange_strong(T volatile* var, V& expected, V desired, MemoryOrder order = memory_order_seq_cst) noexcept
{
    // FIXME: Remove once clang allows implements atomic enums
#ifdef __clang__
    if constexpr (Enum<T>) {
        if (order == memory_order_acq_rel || order == memory_order_release)
            return __atomic_compare_exchange_n(const_cast<UnderlyingType<V>*>((UnderlyingType<V> const volatile*)var), reinterpret_cast<UnderlyingType<V>*>(&expected), static_cast<UnderlyingType<T>>(desired), false, memory_order_release, memory_order_acquire);
        return __atomic_compare_exchange_n(const_cast<UnderlyingType<V>*>((UnderlyingType<V> const volatile*)var), reinterpret_cast<UnderlyingType<V>*>(&expected), static_cast<UnderlyingType<T>>(desired), false, order, order);
    } else
#endif
    {
        if (order == memory_order_acq_rel || order == memory_order_release)
            return __atomic_compare_exchange_n(const_cast<V*>(var), &expected, desired, false, memory_order_release, memory_order_acquire);
        return __atomic_compare_exchange_n(const_cast<V*>(var), &expected, desired, false, order, order);
    }
}

template<IntegralLike T, typename V = RemoveVolatile<T>>
[[nodiscard]] static ALWAYS_INLINE bool atomic_compare_exchange_strong(T volatile* var, V& expected, std::nullptr_t, MemoryOrder order = memory_order_seq_cst) noexcept
{
    // FIXME: Remove once clang allows implements atomic enums
#ifdef __clang__
    if constexpr (Enum<T>) {
        if (order == memory_order_acq_rel || order == memory_order_release)
            return __atomic_compare_exchange_n(reinterpret_cast<UnderlyingType<V> const*>(const_cast<V*>(var)), reinterpret_cast<UnderlyingType<V>*>(&expected), nullptr, false, memory_order_release, memory_order_acquire);
        return __atomic_compare_exchange_n(reinterpret_cast<UnderlyingType<V> const*>(const_cast<V*>(var)), reinterpret_cast<UnderlyingType<V>*>(&expected), nullptr, false, order, order);
    } else
#endif
    {
        if (order == memory_order_acq_rel || order == memory_order_release)
            return __atomic_compare_exchange_n(const_cast<V*>(var), &expected, nullptr, false, memory_order_release, memory_order_acquire);
        return __atomic_compare_exchange_n(const_cast<V*>(var), &expected, nullptr, false, order, order);
    }
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_fetch_add(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_fetch_add(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_fetch_sub(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_fetch_sub(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_fetch_and(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_fetch_and(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_fetch_or(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_fetch_or(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_fetch_xor(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_fetch_xor(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_add_fetch(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_add_fetch(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_sub_fetch(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_sub_fetch(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_and_fetch(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_and_fetch(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_or_fetch(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_or_fetch(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_xor_fetch(T volatile* var, T val, MemoryOrder order = memory_order_seq_cst) noexcept
{
    return __atomic_xor_fetch(var, val, order);
}

template<IntegralLike T>
static ALWAYS_INLINE T atomic_load(T volatile* var, MemoryOrder order = memory_order_seq_cst) noexcept
{
    // FIXME: Remove once clang allows implements atomic enums
#ifdef __clang__
    if constexpr (Enum<T>)
        return (T)__atomic_load_n(reinterpret_cast<UnderlyingType<T> const*>(const_cast<T*>(var)), order);
    else
#endif
        return __atomic_load_n(const_cast<T*>(var), order);
}

template<IntegralLike T, typename V = RemoveVolatile<T>>
static ALWAYS_INLINE void atomic_store(T volatile* var, V desired, MemoryOrder order = memory_order_seq_cst) noexcept
{
    // FIXME: Remove once clang allows implements atomic enums
#ifdef __clang__
    if constexpr (Enum<T>)
        __atomic_store_n(reinterpret_cast<UnderlyingType<V> const*>(const_cast<V*>(var)), desired, order);
    else
#endif
        __atomic_store_n(const_cast<V*>(var), desired, order);
}

template<IntegralLike T>
static ALWAYS_INLINE bool atomic_is_lock_free(T volatile* ptr = nullptr) noexcept
{
    return __atomic_is_lock_free(sizeof(T), ptr);
}

template<IntegralLike T, AK::MemoryOrder DefaultMemoryOrder = memory_order_seq_cst>
class Atomic {
    static_assert(DependentFalse<Atomic>, "Do not use the general definition of AK::Atomic<>.");
};

template<Enum T, MemoryOrder DefaultMemoryOrder>
class Atomic<T, DefaultMemoryOrder> {
    T m_value { 0 };

public:
    Atomic() noexcept = default;
    Atomic& operator=(Atomic const&) volatile = delete;
    Atomic& operator=(Atomic&&) volatile = delete;
    Atomic(Atomic const&) = delete;
    Atomic(Atomic&&) = delete;

    constexpr Atomic(T val) noexcept
        : m_value(val)
    {
    }

    ALWAYS_INLINE T volatile* ptr() noexcept
    {
        return &m_value;
    }

    ALWAYS_INLINE T exchange(T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_exchange(&m_value, desired, order);
    }

    [[nodiscard]] ALWAYS_INLINE bool compare_exchange_strong(T& expected, T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_compare_exchange_strong(&m_value, expected, desired, order);
    }

    ALWAYS_INLINE operator T() const volatile noexcept
    {
        return load();
    }

    ALWAYS_INLINE T load(MemoryOrder order = DefaultMemoryOrder) const volatile noexcept
    {
        return atomic_load(&m_value, order);
    }

    // NOLINTNEXTLINE(misc-unconventional-assign-operator) We want operator= to exchange the value, so returning an object of type Atomic& here does not make sense
    ALWAYS_INLINE T operator=(T desired) volatile noexcept
    {
        store(desired);
        return desired;
    }

    ALWAYS_INLINE void store(T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        __atomic_store(&m_value, &desired, order);
    }

    ALWAYS_INLINE bool is_lock_free() const volatile noexcept
    {
        return __atomic_is_lock_free(sizeof(m_value), &m_value);
    }
};

template<Integral T, MemoryOrder DefaultMemoryOrder>
class Atomic<T, DefaultMemoryOrder> {
    T m_value { 0 };

public:
    Atomic() noexcept = default;
    Atomic& operator=(Atomic const&) volatile = delete;
    Atomic& operator=(Atomic&&) volatile = delete;
    Atomic(Atomic const&) = delete;
    Atomic(Atomic&&) = delete;

    constexpr Atomic(T val) noexcept
        : m_value(val)
    {
    }

    ALWAYS_INLINE T volatile* ptr() noexcept
    {
        return &m_value;
    }

    ALWAYS_INLINE T exchange(T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_exchange(&m_value, desired, order);
    }

    [[nodiscard]] ALWAYS_INLINE bool compare_exchange_strong(T& expected, T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_compare_exchange_strong(&m_value, expected, desired, order);
    }

    ALWAYS_INLINE T operator++() volatile noexcept
    {
        return add_fetch(1);
    }

    ALWAYS_INLINE T operator++(int) volatile noexcept
    {
        return fetch_add(1);
    }

    ALWAYS_INLINE T operator+=(T val) volatile noexcept
    {
        return add_fetch(val);
    }

    ALWAYS_INLINE T fetch_add(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_fetch_add(&m_value, val, order);
    }

    ALWAYS_INLINE T operator--() volatile noexcept
    {
        return sub_fetch(1);
    }

    ALWAYS_INLINE T operator--(int) volatile noexcept
    {
        return fetch_sub(1);
    }

    ALWAYS_INLINE T operator-=(T val) volatile noexcept
    {
        return sub_fetch(val);
    }

    ALWAYS_INLINE T fetch_sub(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_fetch_sub(&m_value, val, order);
    }

    ALWAYS_INLINE T operator&=(T val) volatile noexcept
    {
        return and_fetch(val);
    }

    ALWAYS_INLINE T fetch_and(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_fetch_and(&m_value, val, order);
    }

    ALWAYS_INLINE T operator|=(T val) volatile noexcept
    {
        return or_fetch(val);
    }

    ALWAYS_INLINE T fetch_or(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_fetch_or(&m_value, val, order);
    }

    ALWAYS_INLINE T operator^=(T val) volatile noexcept
    {
        return xor_fetch(val);
    }

    ALWAYS_INLINE T fetch_xor(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_fetch_xor(&m_value, val, order);
    }

    ALWAYS_INLINE T add_fetch(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_add_fetch(&m_value, val, order);
    }

    ALWAYS_INLINE T sub_fetch(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_sub_fetch(&m_value, val, order);
    }

    ALWAYS_INLINE T and_fetch(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_and_fetch(&m_value, val, order);
    }

    ALWAYS_INLINE T or_fetch(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_or_fetch(&m_value, val, order);
    }

    ALWAYS_INLINE T xor_fetch(T val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_xor_fetch(&m_value, val, order);
    }

    ALWAYS_INLINE operator T() const volatile noexcept
    {
        return load();
    }

    ALWAYS_INLINE T load(MemoryOrder order = DefaultMemoryOrder) const volatile noexcept
    {
        return atomic_load(&m_value, order);
    }

    // NOLINTNEXTLINE(misc-unconventional-assign-operator) We want operator= to exchange the value, so returning an object of type Atomic& here does not make sense
    ALWAYS_INLINE T operator=(T desired) volatile noexcept
    {
        store(desired);
        return desired;
    }

    ALWAYS_INLINE void store(T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        atomic_store(&m_value, desired, order);
    }

    ALWAYS_INLINE bool is_lock_free() const volatile noexcept
    {
        return atomic_is_lock_free(sizeof(m_value), &m_value);
    }
};

template<Pointer T, MemoryOrder DefaultMemoryOrder>
class Atomic<T, DefaultMemoryOrder> {
    T m_value { nullptr };

public:
    Atomic() noexcept = default;
    Atomic& operator=(Atomic const&) volatile = delete;
    Atomic& operator=(Atomic&&) volatile = delete;
    Atomic(Atomic const&) = delete;
    Atomic(Atomic&&) = delete;

    constexpr Atomic(T val) noexcept
        : m_value(val)
    {
    }

    ALWAYS_INLINE T ptr() noexcept
    {
        return &m_value;
    }

    ALWAYS_INLINE T exchange(T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_exchange(&m_value, desired, order);
    }

    [[nodiscard]] ALWAYS_INLINE bool compare_exchange_strong(T& expected, T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return atomic_compare_exchange_strong(&m_value, expected, desired, order);
    }

    ALWAYS_INLINE T operator++() volatile noexcept
    {
        return add_fetch(1);
    }

    ALWAYS_INLINE T operator++(int) volatile noexcept
    {
        return fetch_add(1);
    }

    ALWAYS_INLINE T operator+=(ptrdiff_t val) volatile noexcept
    {
        return add_fetch(val);
    }

    ALWAYS_INLINE T fetch_add(ptrdiff_t val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return __atomic_fetch_add(&m_value, val * sizeof(*m_value), order);
    }

    ALWAYS_INLINE T operator--() volatile noexcept
    {
        return sub_fetch(1);
    }

    ALWAYS_INLINE T operator--(int) volatile noexcept
    {
        return fetch_sub(1);
    }

    ALWAYS_INLINE T operator-=(ptrdiff_t val) volatile noexcept
    {
        return sub_fetch(val);
    }

    ALWAYS_INLINE T fetch_sub(ptrdiff_t val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return __atomic_fetch_sub(&m_value, val * sizeof(*m_value), order);
    }

    ALWAYS_INLINE T add_fetch(ptrdiff_t val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return __atomic_add_fetch(&m_value, val * sizeof(*m_value), order);
    }

    ALWAYS_INLINE T sub_fetch(ptrdiff_t val, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        return __atomic_sub_fetch(&m_value, val * sizeof(*m_value), order);
    }

    ALWAYS_INLINE operator T() const volatile noexcept
    {
        return load();
    }

    ALWAYS_INLINE T load(MemoryOrder order = DefaultMemoryOrder) const volatile noexcept
    {
        return atomic_load(&m_value, order);
    }

    // NOLINTNEXTLINE(misc-unconventional-assign-operator) We want operator= to exchange the value, so returning an object of type Atomic& here does not make sense
    ALWAYS_INLINE T operator=(T desired) volatile noexcept
    {
        store(desired);
        return desired;
    }

    ALWAYS_INLINE void store(T desired, MemoryOrder order = DefaultMemoryOrder) volatile noexcept
    {
        atomic_store(&m_value, desired, order);
    }

    ALWAYS_INLINE bool is_lock_free() const volatile noexcept
    {
        return atomic_is_lock_free(sizeof(m_value), &m_value);
    }
};
}

using AK::Atomic;
using AK::full_memory_barrier;
