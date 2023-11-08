/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#ifdef KERNEL
#    include <Kernel/Arch/Processor.h>
#    include <Kernel/Library/ScopedCritical.h>
#endif

#define NONNULLLOCKREFPTR_SCRUB_BYTE 0xa1

namespace AK {

template<typename T, typename PtrTraits>
class LockRefPtr;

template<typename T>
class [[nodiscard]] NonnullLockRefPtr {
    template<typename U, typename P>
    friend class LockRefPtr;
    template<typename U>
    friend class NonnullLockRefPtr;
    template<typename U>
    friend class LockWeakPtr;

public:
    using ElementType = T;

    enum AdoptTag { Adopt };

    ALWAYS_INLINE NonnullLockRefPtr(T const& object)
        : m_bits((FlatPtr)&object)
    {
        VERIFY(!(m_bits & 1));
        const_cast<T&>(object).ref();
    }
    template<typename U>
    ALWAYS_INLINE NonnullLockRefPtr(U const& object)
    requires(IsConvertible<U*, T*>)
        : m_bits((FlatPtr) static_cast<T const*>(&object))
    {
        VERIFY(!(m_bits & 1));
        const_cast<T&>(static_cast<T const&>(object)).ref();
    }
    ALWAYS_INLINE NonnullLockRefPtr(AdoptTag, T& object)
        : m_bits((FlatPtr)&object)
    {
        VERIFY(!(m_bits & 1));
    }
    ALWAYS_INLINE NonnullLockRefPtr(NonnullLockRefPtr&& other)
        : m_bits((FlatPtr)&other.leak_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    template<typename U>
    ALWAYS_INLINE NonnullLockRefPtr(NonnullLockRefPtr<U>&& other)
    requires(IsConvertible<U*, T*>)
        : m_bits((FlatPtr)&other.leak_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    ALWAYS_INLINE NonnullLockRefPtr(NonnullLockRefPtr const& other)
        : m_bits((FlatPtr)other.add_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    template<typename U>
    ALWAYS_INLINE NonnullLockRefPtr(NonnullLockRefPtr<U> const& other)
    requires(IsConvertible<U*, T*>)
        : m_bits((FlatPtr)other.add_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    ALWAYS_INLINE ~NonnullLockRefPtr()
    {
        assign(nullptr);
#ifdef SANITIZE_PTRS
        m_bits.store(explode_byte(NONNULLLOCKREFPTR_SCRUB_BYTE), AK::MemoryOrder::memory_order_relaxed);
#endif
    }

    template<typename U>
    NonnullLockRefPtr(OwnPtr<U> const&) = delete;
    template<typename U>
    NonnullLockRefPtr& operator=(OwnPtr<U> const&) = delete;

    template<typename U>
    NonnullLockRefPtr(LockRefPtr<U> const&) = delete;
    template<typename U>
    NonnullLockRefPtr& operator=(LockRefPtr<U> const&) = delete;
    NonnullLockRefPtr(LockRefPtr<T> const&) = delete;
    NonnullLockRefPtr& operator=(LockRefPtr<T> const&) = delete;

    NonnullLockRefPtr& operator=(NonnullLockRefPtr const& other)
    {
        if (this != &other)
            assign(other.add_ref());
        return *this;
    }

    template<typename U>
    NonnullLockRefPtr& operator=(NonnullLockRefPtr<U> const& other)
    requires(IsConvertible<U*, T*>)
    {
        assign(other.add_ref());
        return *this;
    }

    ALWAYS_INLINE NonnullLockRefPtr& operator=(NonnullLockRefPtr&& other)
    {
        if (this != &other)
            assign(&other.leak_ref());
        return *this;
    }

    template<typename U>
    NonnullLockRefPtr& operator=(NonnullLockRefPtr<U>&& other)
    requires(IsConvertible<U*, T*>)
    {
        assign(&other.leak_ref());
        return *this;
    }

    NonnullLockRefPtr& operator=(T const& object)
    {
        const_cast<T&>(object).ref();
        assign(const_cast<T*>(&object));
        return *this;
    }

    [[nodiscard]] ALWAYS_INLINE T& leak_ref()
    {
        T* ptr = exchange(nullptr);
        VERIFY(ptr);
        return *ptr;
    }

    ALWAYS_INLINE RETURNS_NONNULL T* ptr()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE RETURNS_NONNULL T const* ptr() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE RETURNS_NONNULL T* operator->()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE RETURNS_NONNULL T const* operator->() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE T& operator*()
    {
        return *as_nonnull_ptr();
    }
    ALWAYS_INLINE T const& operator*() const
    {
        return *as_nonnull_ptr();
    }

    ALWAYS_INLINE RETURNS_NONNULL operator T*()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE RETURNS_NONNULL operator T const*() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE operator T&()
    {
        return *as_nonnull_ptr();
    }
    ALWAYS_INLINE operator T const&() const
    {
        return *as_nonnull_ptr();
    }

    operator bool() const = delete;
    bool operator!() const = delete;

    void swap(NonnullLockRefPtr& other)
    {
        if (this == &other)
            return;

        // NOTE: swap is not atomic!
        T* other_ptr = other.exchange(nullptr);
        T* ptr = exchange(other_ptr);
        other.exchange(ptr);
    }

    template<typename U>
    void swap(NonnullLockRefPtr<U>& other)
    requires(IsConvertible<U*, T*>)
    {
        // NOTE: swap is not atomic!
        U* other_ptr = other.exchange(nullptr);
        T* ptr = exchange(other_ptr);
        other.exchange(ptr);
    }

private:
    NonnullLockRefPtr() = delete;

    ALWAYS_INLINE T* as_ptr() const
    {
        return (T*)(m_bits.load(AK::MemoryOrder::memory_order_relaxed) & ~(FlatPtr)1);
    }

    ALWAYS_INLINE RETURNS_NONNULL T* as_nonnull_ptr() const
    {
        T* ptr = (T*)(m_bits.load(AK::MemoryOrder::memory_order_relaxed) & ~(FlatPtr)1);
        VERIFY(ptr);
        return ptr;
    }

    template<typename F>
    void do_while_locked(F f) const
    {
#ifdef KERNEL
        // We don't want to be pre-empted while we have the lock bit set
        Kernel::ScopedCritical critical;
#endif
        FlatPtr bits;
        for (;;) {
            bits = m_bits.fetch_or(1, AK::MemoryOrder::memory_order_acq_rel);
            if (!(bits & 1))
                break;
#ifdef KERNEL
            Kernel::Processor::wait_check();
#endif
        }
        VERIFY(!(bits & 1));
        f((T*)bits);
        m_bits.store(bits, AK::MemoryOrder::memory_order_release);
    }

    ALWAYS_INLINE void assign(T* new_ptr)
    {
        T* prev_ptr = exchange(new_ptr);
        unref_if_not_null(prev_ptr);
    }

    ALWAYS_INLINE T* exchange(T* new_ptr)
    {
        VERIFY(!((FlatPtr)new_ptr & 1));
#ifdef KERNEL
        // We don't want to be pre-empted while we have the lock bit set
        Kernel::ScopedCritical critical;
#endif
        // Only exchange while not locked
        FlatPtr expected = m_bits.load(AK::MemoryOrder::memory_order_relaxed);
        for (;;) {
            expected &= ~(FlatPtr)1; // only if lock bit is not set
            if (m_bits.compare_exchange_strong(expected, (FlatPtr)new_ptr, AK::MemoryOrder::memory_order_acq_rel))
                break;
#ifdef KERNEL
            Kernel::Processor::wait_check();
#endif
        }
        VERIFY(!(expected & 1));
        return (T*)expected;
    }

    T* add_ref() const
    {
#ifdef KERNEL
        // We don't want to be pre-empted while we have the lock bit set
        Kernel::ScopedCritical critical;
#endif
        // Lock the pointer
        FlatPtr expected = m_bits.load(AK::MemoryOrder::memory_order_relaxed);
        for (;;) {
            expected &= ~(FlatPtr)1; // only if lock bit is not set
            if (m_bits.compare_exchange_strong(expected, expected | 1, AK::MemoryOrder::memory_order_acq_rel))
                break;
#ifdef KERNEL
            Kernel::Processor::wait_check();
#endif
        }

        // Add a reference now that we locked the pointer
        ref_if_not_null((T*)expected);

        // Unlock the pointer again
        m_bits.store(expected, AK::MemoryOrder::memory_order_release);
        return (T*)expected;
    }

    mutable Atomic<FlatPtr> m_bits { 0 };
};

template<typename T>
inline NonnullLockRefPtr<T> adopt_lock_ref(T& object)
{
    return NonnullLockRefPtr<T>(NonnullLockRefPtr<T>::Adopt, object);
}

template<typename T>
struct Formatter<NonnullLockRefPtr<T>> : Formatter<T const*> {
    ErrorOr<void> format(FormatBuilder& builder, NonnullLockRefPtr<T> const& value)
    {
        return Formatter<T const*>::format(builder, value.ptr());
    }
};

template<typename T, typename U>
inline void swap(NonnullLockRefPtr<T>& a, NonnullLockRefPtr<U>& b)
requires(IsConvertible<U*, T*>)
{
    a.swap(b);
}

}

template<typename T>
struct Traits<NonnullLockRefPtr<T>> : public DefaultTraits<NonnullLockRefPtr<T>> {
    using PeekType = T*;
    using ConstPeekType = T const*;
    static unsigned hash(NonnullLockRefPtr<T> const& p) { return ptr_hash(p.ptr()); }
    static bool equals(NonnullLockRefPtr<T> const& a, NonnullLockRefPtr<T> const& b) { return a.ptr() == b.ptr(); }
};

using AK::adopt_lock_ref;
using AK::NonnullLockRefPtr;
