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
#include <AK/Format.h>
#include <AK/Types.h>
#ifdef KERNEL
#    include <Kernel/Arch/x86/CPU.h>
#endif

namespace AK {

template<typename T>
class OwnPtr;
template<typename T>
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
    template<typename U>
    friend class RefPtr;
    template<typename U>
    friend class NonnullRefPtr;
    template<typename U>
    friend class WeakPtr;

public:
    using ElementType = T;

    enum AdoptTag { Adopt };

    ALWAYS_INLINE NonnullRefPtr(const T& object)
        : m_ptr(&const_cast<T&>(object))
    {
        VERIFY(m_ptr);
        const_cast<T&>(object).ref();
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(const U& object)
        : m_ptr(static_cast<T*>(const_cast<U*>(&object)))
    {
        VERIFY(m_ptr);
        const_cast<T&>(static_cast<const T&>(object)).ref();
    }
    ALWAYS_INLINE NonnullRefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
        VERIFY(m_ptr);
    }
    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr&& other)
        : m_ptr(&other.leak_ref())
    {
        VERIFY(m_ptr);
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr<U>&& other)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
        VERIFY(m_ptr);
    }
    ALWAYS_INLINE NonnullRefPtr(const NonnullRefPtr& other)
        : m_ptr(other.add_ref())
    {
        VERIFY(m_ptr);
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(const NonnullRefPtr<U>& other)
        : m_ptr(static_cast<T*>(other.add_ref()))
    {
        VERIFY(m_ptr);
    }
    ALWAYS_INLINE ~NonnullRefPtr()
    {
        assign(nullptr);
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr.store((T*)0xb0b0b0b0b0b0b0b0);
        else
            m_ptr.store((T*)0xb0b0b0b0);
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
        assign(static_cast<T*>(other.add_ref()));
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
        assign(static_cast<T*>(&other.leak_ref()));
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
        T* ptr = m_ptr.exchange(nullptr);
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
        T* other_ptr = other.m_ptr.exchange(nullptr);
        T* ptr = m_ptr.exchange(other_ptr);
        other.m_ptr.exchange(ptr);
    }

    template<typename U>
    void swap(NonnullRefPtr<U>& other)
    {
        // NOTE: swap is not atomic!
        U* other_ptr = other.m_ptr.exchange(nullptr);
        T* ptr = m_ptr.exchange(other_ptr);
        other.m_ptr.exchange(ptr);
    }

private:
    NonnullRefPtr() = delete;

    ALWAYS_INLINE T* as_ptr() const
    {
        return m_ptr;
    }

    ALWAYS_INLINE T* as_nonnull_ptr() const
    {
        T* ptr = m_ptr;
        VERIFY(ptr);
        return ptr;
    }

    ALWAYS_INLINE void assign(T* new_ptr)
    {
        T* prev_ptr = m_ptr.exchange(new_ptr);
        unref_if_not_null(prev_ptr);
    }

    T* add_ref() const
    {
        T* ptr = m_ptr;
        ref_if_not_null(ptr);
        return ptr;
    }

    Atomic<T*, MemoryOrder::memory_order_relaxed> m_ptr { nullptr };
};

template<typename T>
inline NonnullRefPtr<T> adopt(T& object)
{
    return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, object);
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
