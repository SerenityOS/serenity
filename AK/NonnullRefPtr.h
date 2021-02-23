/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/Atomic.h>
#include <AK/LogStream.h>
#include <AK/Types.h>
#ifdef KERNEL
#    include <Kernel/Arch/i386/CPU.h>
#endif

namespace AK {

template<typename T>
class OwnPtr;
template<typename T, typename PtrTraits>
class RefPtr;

template<typename T>
ALWAYS_INLINE void ref_if_not_null(T* ptr)
{
    if (ptr)
        ptr->ref();
}

template<typename T>
ALWAYS_INLINE void unref_if_not_null(T* ptr)
{
    if (ptr)
        ptr->unref();
}

template<typename T>
class NonnullRefPtr {
    template<typename U, typename P>
    friend class RefPtr;
    template<typename U>
    friend class NonnullRefPtr;
    template<typename U>
    friend class WeakPtr;

public:
    using ElementType = T;

    enum AdoptTag { Adopt };

    ALWAYS_INLINE NonnullRefPtr(const T& object)
        : m_bits((FlatPtr)&object)
    {
        VERIFY(!(m_bits & 1));
        const_cast<T&>(object).ref();
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(const U& object)
        : m_bits((FlatPtr) static_cast<const T*>(&object))
    {
        VERIFY(!(m_bits & 1));
        const_cast<T&>(static_cast<const T&>(object)).ref();
    }
    ALWAYS_INLINE NonnullRefPtr(AdoptTag, T& object)
        : m_bits((FlatPtr)&object)
    {
        VERIFY(!(m_bits & 1));
    }
    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr&& other)
        : m_bits((FlatPtr)&other.leak_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr<U>&& other)
        : m_bits((FlatPtr)&other.leak_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    ALWAYS_INLINE NonnullRefPtr(const NonnullRefPtr& other)
        : m_bits((FlatPtr)other.add_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(const NonnullRefPtr<U>& other)
        : m_bits((FlatPtr)other.add_ref())
    {
        VERIFY(!(m_bits & 1));
    }
    ALWAYS_INLINE ~NonnullRefPtr()
    {
        assign(nullptr);
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_bits.store(0xb0b0b0b0b0b0b0b0, AK::MemoryOrder::memory_order_relaxed);
        else
            m_bits.store(0xb0b0b0b0, AK::MemoryOrder::memory_order_relaxed);
#endif
    }

    template<typename U>
    NonnullRefPtr(const OwnPtr<U>&) = delete;
    template<typename U>
    NonnullRefPtr& operator=(const OwnPtr<U>&) = delete;

    template<typename U>
    NonnullRefPtr(const RefPtr<U>&) = delete;
    template<typename U>
    NonnullRefPtr& operator=(const RefPtr<U>&) = delete;
    NonnullRefPtr(const RefPtr<T>&) = delete;
    NonnullRefPtr& operator=(const RefPtr<T>&) = delete;

    NonnullRefPtr& operator=(const NonnullRefPtr& other)
    {
        if (this != &other)
            assign(other.add_ref());
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(const NonnullRefPtr<U>& other)
    {
        assign(other.add_ref());
        return *this;
    }

    ALWAYS_INLINE NonnullRefPtr& operator=(NonnullRefPtr&& other)
    {
        if (this != &other)
            assign(&other.leak_ref());
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(NonnullRefPtr<U>&& other)
    {
        assign(&other.leak_ref());
        return *this;
    }

    NonnullRefPtr& operator=(const T& object)
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

    ALWAYS_INLINE T* ptr()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE const T* ptr() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE T* operator->()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE const T* operator->() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE T& operator*()
    {
        return *as_nonnull_ptr();
    }
    ALWAYS_INLINE const T& operator*() const
    {
        return *as_nonnull_ptr();
    }

    ALWAYS_INLINE operator T*()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE operator const T*() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE operator T&()
    {
        return *as_nonnull_ptr();
    }
    ALWAYS_INLINE operator const T&() const
    {
        return *as_nonnull_ptr();
    }

    operator bool() const = delete;
    bool operator!() const = delete;

    void swap(NonnullRefPtr& other)
    {
        if (this == &other)
            return;

        // NOTE: swap is not atomic!
        T* other_ptr = other.exchange(nullptr);
        T* ptr = exchange(other_ptr);
        other.exchange(ptr);
    }

    template<typename U>
    void swap(NonnullRefPtr<U>& other)
    {
        // NOTE: swap is not atomic!
        U* other_ptr = other.exchange(nullptr);
        T* ptr = exchange(other_ptr);
        other.exchange(ptr);
    }

private:
    NonnullRefPtr() = delete;

    ALWAYS_INLINE T* as_ptr() const
    {
        return (T*)(m_bits.load(AK::MemoryOrder::memory_order_relaxed) & ~(FlatPtr)1);
    }

    ALWAYS_INLINE T* as_nonnull_ptr() const
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
inline NonnullRefPtr<T> adopt(T& object)
{
    return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, object);
}

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const NonnullRefPtr<T>& value)
{
    return stream << value.ptr();
}

template<typename T>
struct Formatter<NonnullRefPtr<T>> : Formatter<const T*> {
    void format(FormatBuilder& builder, const NonnullRefPtr<T>& value)
    {
        Formatter<const T*>::format(builder, value.ptr());
    }
};

template<typename T, typename U>
inline void swap(NonnullRefPtr<T>& a, NonnullRefPtr<U>& b)
{
    a.swap(b);
}

}

using AK::adopt;
using AK::NonnullRefPtr;
