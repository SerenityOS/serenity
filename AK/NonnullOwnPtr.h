/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/RefCounted.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
#include <AK/Types.h>

#define NONNULLOWNPTR_SCRUB_BYTE 0xf1

namespace AK {

template<typename T, typename PtrTraits>
class RefPtr;
template<typename T>
class NonnullRefPtr;
template<typename T>
class WeakPtr;

template<typename T>
class [[nodiscard]] NonnullOwnPtr {
public:
    using ElementType = T;

    enum AdoptTag { Adopt };

    NonnullOwnPtr(AdoptTag, T& ptr)
        : m_ptr(&ptr)
    {
        static_assert(
            requires { requires typename T::AllowOwnPtr()(); } || !requires { requires !typename T::AllowOwnPtr()(); declval<T>().ref(); declval<T>().unref(); },
            "Use NonnullRefPtr<> for RefCounted types");
    }
    NonnullOwnPtr(NonnullOwnPtr&& other)
        : m_ptr(other.leak_ptr())
    {
        VERIFY(m_ptr);
    }
    template<typename U>
    NonnullOwnPtr(NonnullOwnPtr<U>&& other)
        : m_ptr(other.leak_ptr())
    {
        VERIFY(m_ptr);
    }
    ~NonnullOwnPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        m_ptr = (T*)(explode_byte(NONNULLOWNPTR_SCRUB_BYTE));
#endif
    }

    NonnullOwnPtr(const NonnullOwnPtr&) = delete;
    template<typename U>
    NonnullOwnPtr(const NonnullOwnPtr<U>&) = delete;
    NonnullOwnPtr& operator=(const NonnullOwnPtr&) = delete;
    template<typename U>
    NonnullOwnPtr& operator=(const NonnullOwnPtr<U>&) = delete;

    template<typename U, typename PtrTraits = RefPtrTraits<U>>
    NonnullOwnPtr(const RefPtr<U, PtrTraits>&) = delete;
    template<typename U>
    NonnullOwnPtr(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    NonnullOwnPtr(const WeakPtr<U>&) = delete;
    template<typename U, typename PtrTraits = RefPtrTraits<U>>
    NonnullOwnPtr& operator=(const RefPtr<U, PtrTraits>&) = delete;
    template<typename U>
    NonnullOwnPtr& operator=(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    NonnullOwnPtr& operator=(const WeakPtr<U>&) = delete;

    NonnullOwnPtr& operator=(NonnullOwnPtr&& other)
    {
        NonnullOwnPtr ptr(move(other));
        swap(ptr);
        return *this;
    }

    template<typename U>
    NonnullOwnPtr& operator=(NonnullOwnPtr<U>&& other)
    {
        NonnullOwnPtr ptr(move(other));
        swap(ptr);
        return *this;
    }

    [[nodiscard]] T* leak_ptr()
    {
        return exchange(m_ptr, nullptr);
    }

    ALWAYS_INLINE RETURNS_NONNULL T* ptr()
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    ALWAYS_INLINE RETURNS_NONNULL const T* ptr() const
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    ALWAYS_INLINE RETURNS_NONNULL T* operator->() { return ptr(); }
    ALWAYS_INLINE RETURNS_NONNULL const T* operator->() const { return ptr(); }

    ALWAYS_INLINE T& operator*() { return *ptr(); }
    ALWAYS_INLINE const T& operator*() const { return *ptr(); }

    ALWAYS_INLINE RETURNS_NONNULL operator const T*() const { return ptr(); }
    ALWAYS_INLINE RETURNS_NONNULL operator T*() { return ptr(); }

    operator bool() const = delete;
    bool operator!() const = delete;

    void swap(NonnullOwnPtr& other)
    {
        ::swap(m_ptr, other.m_ptr);
    }

    template<typename U>
    void swap(NonnullOwnPtr<U>& other)
    {
        ::swap(m_ptr, other.m_ptr);
    }

    template<typename U>
    NonnullOwnPtr<U> release_nonnull()
    {
        VERIFY(m_ptr);
        return NonnullOwnPtr<U>(NonnullOwnPtr<U>::Adopt, static_cast<U&>(*leak_ptr()));
    }

private:
    void clear()
    {
        if (!m_ptr)
            return;
        delete m_ptr;
        m_ptr = nullptr;
    }

    T* m_ptr = nullptr;
};

#if !defined(KERNEL)

template<typename T>
inline NonnullOwnPtr<T> adopt_own(T& object)
{
    return NonnullOwnPtr<T>(NonnullOwnPtr<T>::Adopt, object);
}

template<class T, class... Args>
requires(IsConstructible<T, Args...>) inline NonnullOwnPtr<T> make(Args&&... args)
{
    return NonnullOwnPtr<T>(NonnullOwnPtr<T>::Adopt, *new T(forward<Args>(args)...));
}

// FIXME: Remove once P0960R3 is available in Clang.
template<class T, class... Args>
inline NonnullOwnPtr<T> make(Args&&... args)
{
    return NonnullOwnPtr<T>(NonnullOwnPtr<T>::Adopt, *new T { forward<Args>(args)... });
}

#endif

template<typename T>
struct Traits<NonnullOwnPtr<T>> : public GenericTraits<NonnullOwnPtr<T>> {
    using PeekType = T*;
    using ConstPeekType = const T*;
    static unsigned hash(const NonnullOwnPtr<T>& p) { return ptr_hash((FlatPtr)p.ptr()); }
    static bool equals(const NonnullOwnPtr<T>& a, const NonnullOwnPtr<T>& b) { return a.ptr() == b.ptr(); }
};

template<typename T, typename U>
inline void swap(NonnullOwnPtr<T>& a, NonnullOwnPtr<U>& b)
{
    a.swap(b);
}

template<typename T>
struct Formatter<NonnullOwnPtr<T>> : Formatter<const T*> {
    ErrorOr<void> format(FormatBuilder& builder, NonnullOwnPtr<T> const& value)
    {
        return Formatter<const T*>::format(builder, value.ptr());
    }
};

}

#if !defined(KERNEL)
using AK::adopt_own;
using AK::make;
#endif
using AK::NonnullOwnPtr;
