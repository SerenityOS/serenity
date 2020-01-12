#pragma once

#include <AK/Types.h>

namespace AK {

enum MemoryOrder {
    memory_order_relaxed = __ATOMIC_RELAXED,
    memory_order_consume = __ATOMIC_CONSUME,
    memory_order_acquire = __ATOMIC_ACQUIRE,
    memory_order_release = __ATOMIC_RELEASE,
    memory_order_acq_rel = __ATOMIC_ACQ_REL,
    memory_order_seq_cst = __ATOMIC_SEQ_CST
};

template<typename T>
class Atomic {
    T m_value { 0 };

public:
    Atomic() noexcept = default;
    Atomic(const Atomic&) = delete;
    Atomic& operator=(const Atomic&) volatile = delete;

    Atomic(T val) noexcept
        : m_value(val)
    {
    }

    T exchange(T desired, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_exchange_n(&m_value, desired, order);
    }

    bool compare_exchange_strong(T& expected, T desired, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        if (order == memory_order_acq_rel || order == memory_order_release)
            return __atomic_compare_exchange_n(&m_value, &expected, desired, false, memory_order_release, memory_order_acquire);
        else
            return __atomic_compare_exchange_n(&m_value, &expected, desired, false, order, order);
    }

    T operator++() volatile noexcept
    {
        return fetch_add(1) + 1;
    }

    T operator++(int) volatile noexcept
    {
        return fetch_add(1);
    }

    T operator+=(T val) volatile noexcept
    {
        return fetch_add(val) + val;
    }

    T fetch_add(T val, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_fetch_add(&m_value, val, order);
    }

    T operator--() volatile noexcept
    {
        return fetch_sub(1) - 1;
    }

    T operator--(int) volatile noexcept
    {
        return fetch_sub(1);
    }

    T operator-=(T val) volatile noexcept
    {
        return fetch_sub(val) - val;
    }

    T fetch_sub(T val, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_fetch_sub(&m_value, val, order);
    }

    T operator&=(T val) volatile noexcept
    {
        return fetch_and(val) & val;
    }

    T fetch_and(T val, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_fetch_and(&m_value, val, order);
    }

    T operator|=(T val) volatile noexcept
    {
        return fetch_or(val) | val;
    }

    T fetch_or(T val, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_fetch_or(&m_value, val, order);
    }

    T operator^=(T val) volatile noexcept
    {
        return fetch_xor(val) ^ val;
    }

    T fetch_xor(T val, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_fetch_xor(&m_value, val, order);
    }

    operator T() const volatile noexcept
    {
        return load();
    }

    T load(MemoryOrder order = memory_order_seq_cst) const volatile noexcept
    {
        return __atomic_load_n(&m_value, order);
    }

    T operator=(T desired) volatile noexcept
    {
        store(desired);
        return desired;
    }

    void store(T desired, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        __atomic_store_n(&m_value, desired, order);
    }

    bool is_lock_free() const volatile noexcept
    {
        return __atomic_is_lock_free(sizeof(m_value), &m_value);
    }
};

template<typename T>
class Atomic<T*> {
    T* m_value { nullptr };

public:
    Atomic() noexcept = default;
    Atomic(const Atomic&) = delete;
    Atomic& operator=(const Atomic&) volatile = delete;

    Atomic(T* val) noexcept
        : m_value(val)
    {
    }

    T* exchange(T* desired, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_exchange_n(&m_value, desired, order);
    }

    bool compare_exchange_strong(T*& expected, T* desired, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        if (order == memory_order_acq_rel || order == memory_order_release)
            return __atomic_compare_exchange_n(&m_value, &expected, desired, false, memory_order_release, memory_order_acquire);
        else
            return __atomic_compare_exchange_n(&m_value, &expected, desired, false, order, order);
    }

    T* operator++() volatile noexcept
    {
        return fetch_add(1) + 1;
    }

    T* operator++(int) volatile noexcept
    {
        return fetch_add(1);
    }

    T* operator+=(ptrdiff_t val) volatile noexcept
    {
        return fetch_add(val) + val;
    }

    T* fetch_add(ptrdiff_t val, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_fetch_add(&m_value, val * sizeof(*m_value), order);
    }

    T* operator--() volatile noexcept
    {
        return fetch_sub(1) - 1;
    }

    T* operator--(int) volatile noexcept
    {
        return fetch_sub(1);
    }

    T* operator-=(ptrdiff_t val) volatile noexcept
    {
        return fetch_sub(val) - val;
    }

    T* fetch_sub(ptrdiff_t val, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        return __atomic_fetch_sub(&m_value, val * sizeof(*m_value), order);
    }

    operator T*() const volatile noexcept
    {
        return load();
    }

    T* load(MemoryOrder order = memory_order_seq_cst) const volatile noexcept
    {
        return __atomic_load_n(&m_value, order);
    }

    T* operator=(T* desired) volatile noexcept
    {
        store(desired);
        return desired;
    }

    void store(T* desired, MemoryOrder order = memory_order_seq_cst) volatile noexcept
    {
        __atomic_store_n(&m_value, desired, order);
    }

    bool is_lock_free() const volatile noexcept
    {
        return __atomic_is_lock_free(sizeof(m_value), &m_value);
    }
};

}

using AK::Atomic;
