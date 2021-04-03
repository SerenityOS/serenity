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

#include <AK/Atomic.h>
#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#ifdef KERNEL
#    include <Kernel/Arch/x86/CPU.h>
#endif

namespace AK {

template<typename T>
class OwnPtr;

template<typename T>
class RefPtr {
    template<typename U>
    friend class RefPtr;
    template<typename U>
    friend class WeakPtr;

public:
    enum AdoptTag {
        Adopt
    };

    RefPtr() = default;
    RefPtr(const T* ptr)
        : m_ptr(const_cast<T*>(ptr))
    {
        ref_if_not_null(const_cast<T*>(ptr));
    }
    RefPtr(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        T* ptr = m_ptr;
        VERIFY(ptr);
        VERIFY(!is_null());
        ptr->ref();
    }
    RefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
        VERIFY(!is_null());
    }
    RefPtr(RefPtr&& other)
        : m_ptr(other.leak_ref())
    {
    }
    ALWAYS_INLINE RefPtr(const NonnullRefPtr<T>& other)
        : m_ptr(const_cast<T*>(other.add_ref()))
    {
    }
    template<typename U>
    ALWAYS_INLINE RefPtr(const NonnullRefPtr<U>& other)
        : m_ptr(const_cast<U*>(other.add_ref()))
    {
    }
    template<typename U>
    ALWAYS_INLINE RefPtr(NonnullRefPtr<U>&& other)
        : m_ptr(&other.leak_ref())
    {
        VERIFY(!is_null());
    }
    template<typename U>
    RefPtr(RefPtr<U>&& other)
        : m_ptr(other.leak_ref())
    {
    }
    RefPtr(const RefPtr& other)
        : m_ptr(other.do_add_ref())
    {
    }
    template<typename U>
    RefPtr(const RefPtr<U>& other)
        : m_ptr(other.do_add_ref())
    {
    }
    ALWAYS_INLINE ~RefPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)0xe0e0e0e0e0e0e0e0;
        else
            m_ptr = (T*)0xe0e0e0e0;
#endif
    }

    template<typename U>
    RefPtr(const OwnPtr<U>&) = delete;
    template<typename U>
    RefPtr& operator=(const OwnPtr<U>&) = delete;

    void swap(RefPtr& other)
    {
        if (this == &other)
            return;

        T* other_ptr = other.m_ptr.exchange(nullptr);
        T* ptr = m_ptr.exchange(other_ptr);
        other.m_ptr.exchange(ptr);
    }

    template<typename U>
    void swap(RefPtr<U>& other)
    {
        U* other_ptr = other.m_ptr.exchange(nullptr);
        T* ptr = m_ptr.exchange(static_cast<T*>(other_ptr));
        other.m_ptr.exchange(static_cast<T*>(other_ptr));
    }

    ALWAYS_INLINE RefPtr& operator=(RefPtr&& other)
    {
        if (this != &other)
            assign(other.leak_ref());
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(RefPtr<U>&& other)
    {
        assign(other.leak_ref());
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(NonnullRefPtr<U>&& other)
    {
        assign(&other.leak_ref());
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const NonnullRefPtr<T>& other)
    {
        assign(other.add_ref());
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(const NonnullRefPtr<U>& other)
    {
        assign(other.add_ref());
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const RefPtr& other)
    {
        if (this != &other)
            assign(other.do_add_ref());
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(const RefPtr<U>& other)
    {
        assign(other.do_add_ref());
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const T* ptr)
    {
        ref_if_not_null(const_cast<T*>(ptr));
        assign(const_cast<T*>(ptr));
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(const T& object)
    {
        const_cast<T&>(object).ref();
        assign(const_cast<T*>(&object));
        return *this;
    }

    RefPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    ALWAYS_INLINE bool assign_if_null(RefPtr&& other)
    {
        if (this == &other)
            return is_null();
        T* other_ptr = other.leak_ref();
        T* expected = nullptr;
        if (m_ptr.compare_exchange_strong(expected, other_ptr))
            return true;
        unref_if_not_null(other_ptr);
        return false;
    }

    template<typename U>
    ALWAYS_INLINE bool assign_if_null(RefPtr<U>&& other)
    {
        if (this == &other)
            return is_null();
        T* other_ptr = static_cast<T*>(other.leak_ref());
        T* expected = nullptr;
        if (m_ptr.compare_exchange_strong(expected, other_ptr))
            return true;
        unref_if_not_null(other_ptr);
        return false;
    }

    ALWAYS_INLINE void clear()
    {
        assign(nullptr);
    }

    bool operator!() const { return !m_ptr; }

    [[nodiscard]] T* leak_ref()
    {
        return m_ptr.exchange(nullptr);
    }

    NonnullRefPtr<T> release_nonnull()
    {
        T* ptr = leak_ref();
        VERIFY(ptr);
        return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, *ptr);
    }

    ALWAYS_INLINE T* ptr() { return m_ptr; }
    ALWAYS_INLINE const T* ptr() const { return m_ptr; }

    ALWAYS_INLINE T* operator->()
    {
        return nonnull_ptr();
    }

    ALWAYS_INLINE const T* operator->() const
    {
        return nonnull_ptr();
    }

    ALWAYS_INLINE T& operator*()
    {
        return *nonnull_ptr();
    }

    ALWAYS_INLINE const T& operator*() const
    {
        return *nonnull_ptr();
    }

    ALWAYS_INLINE operator const T*() const { return ptr(); }
    ALWAYS_INLINE operator T*() { return ptr(); }

    ALWAYS_INLINE operator bool() { return !is_null(); }

    bool operator==(std::nullptr_t) const { return is_null(); }
    bool operator!=(std::nullptr_t) const { return !is_null(); }

    bool operator==(const RefPtr& other) const { return ptr() == other.ptr(); }
    bool operator!=(const RefPtr& other) const { return ptr() != other.ptr(); }

    bool operator==(RefPtr& other) { return ptr() == other.ptr(); }
    bool operator!=(RefPtr& other) { return ptr() != other.ptr(); }

    bool operator==(const T* other) const { return ptr() == other; }
    bool operator!=(const T* other) const { return ptr() != other; }

    bool operator==(T* other) { return ptr() == other; }
    bool operator!=(T* other) { return ptr() != other; }

    ALWAYS_INLINE bool is_null() const { return !m_ptr; }

private:
    [[nodiscard]] ALWAYS_INLINE T* do_add_ref() const
    {
        T* ptr = m_ptr;
        if (ptr)
            ptr->ref();
        return ptr;
    }

    ALWAYS_INLINE void assign(T* ptr)
    {
        T* prev_ptr = m_ptr.exchange(ptr);
        if (prev_ptr != ptr)
            unref_if_not_null(prev_ptr);
    }

    ALWAYS_INLINE T* nonnull_ptr() const
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    // We use relaxed because we don't need to synchronize on the
    // object unless the last reference is dropped (which is done
    // in RefCounted)
    Atomic<T*, MemoryOrder::memory_order_relaxed> m_ptr { nullptr };
};

template<typename T>
struct Formatter<RefPtr<T>> : Formatter<const T*> {
    void format(FormatBuilder& builder, const RefPtr<T>& value)
    {
        Formatter<const T*>::format(builder, value.ptr());
    }
};

template<typename T>
struct Traits<RefPtr<T>> : public GenericTraits<RefPtr<T>> {
    using PeekType = const T*;
    static unsigned hash(const RefPtr<T>& p) { return ptr_hash(p.ptr()); }
    static bool equals(const RefPtr<T>& a, const RefPtr<T>& b) { return a.ptr() == b.ptr(); }
};

template<typename T, typename U>
inline NonnullRefPtr<T> static_ptr_cast(const NonnullRefPtr<U>& ptr)
{
    return NonnullRefPtr<T>(static_cast<const T&>(*ptr));
}

template<typename T, typename U>
inline RefPtr<T> static_ptr_cast(const RefPtr<U>& ptr)
{
    return RefPtr<T>(static_cast<const T*>(ptr.ptr()));
}

template<typename T, typename U>
inline void swap(RefPtr<T>& a, RefPtr<U>& b)
{
    a.swap(b);
}

}

using AK::RefPtr;
using AK::static_ptr_cast;
