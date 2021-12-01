/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define NONNULLREFPTR_SCRUB_BYTE 0xe1

#ifdef KERNEL
#    include <Kernel/Library/ThreadSafeNonnullRefPtr.h>
#else
#    include <AK/Assertions.h>
#    include <AK/Atomic.h>
#    include <AK/Format.h>
#    include <AK/Traits.h>
#    include <AK/Types.h>

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
class [[nodiscard]] NonnullRefPtr {
    template<typename U, typename P>
    friend class RefPtr;
    template<typename U>
    friend class NonnullRefPtr;
    template<typename U>
    friend class WeakPtr;

public:
    using ElementType = T;

    enum AdoptTag { Adopt };

    ALWAYS_INLINE NonnullRefPtr(T const& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->ref();
    }

    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(U const& object) requires(IsConvertible<U*, T*>)
        : m_ptr(const_cast<T*>(static_cast<T const*>(&object)))
    {
        m_ptr->ref();
    }

    ALWAYS_INLINE NonnullRefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }

    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr&& other)
        : m_ptr(&other.leak_ref())
    {
    }

    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr<U>&& other) requires(IsConvertible<U*, T*>)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
    }

    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr const& other)
        : m_ptr(const_cast<T*>(other.ptr()))
    {
        m_ptr->ref();
    }

    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr<U> const& other) requires(IsConvertible<U*, T*>)
        : m_ptr(const_cast<T*>(static_cast<T const*>(other.ptr())))
    {
        m_ptr->ref();
    }

    ALWAYS_INLINE ~NonnullRefPtr()
    {
        unref_if_not_null(m_ptr);
        m_ptr = nullptr;
#    ifdef SANITIZE_PTRS
        m_ptr = reinterpret_cast<T*>(explode_byte(NONNULLREFPTR_SCRUB_BYTE));
#    endif
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

    NonnullRefPtr& operator=(NonnullRefPtr const& other)
    {
        NonnullRefPtr tmp { other };
        swap(tmp);
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(NonnullRefPtr<U> const& other) requires(IsConvertible<U*, T*>)
    {
        NonnullRefPtr tmp { other };
        swap(tmp);
        return *this;
    }

    ALWAYS_INLINE NonnullRefPtr& operator=(NonnullRefPtr&& other)
    {
        NonnullRefPtr tmp { move(other) };
        swap(tmp);
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(NonnullRefPtr<U>&& other) requires(IsConvertible<U*, T*>)
    {
        NonnullRefPtr tmp { move(other) };
        swap(tmp);
        return *this;
    }

    NonnullRefPtr& operator=(T const& object)
    {
        NonnullRefPtr tmp { object };
        swap(tmp);
        return *this;
    }

    [[nodiscard]] ALWAYS_INLINE T& leak_ref()
    {
        T* ptr = exchange(m_ptr, nullptr);
        VERIFY(ptr);
        return *ptr;
    }

    ALWAYS_INLINE RETURNS_NONNULL T* ptr()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE RETURNS_NONNULL const T* ptr() const
    {
        return as_nonnull_ptr();
    }

    ALWAYS_INLINE RETURNS_NONNULL T* operator->()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE RETURNS_NONNULL const T* operator->() const
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

    ALWAYS_INLINE RETURNS_NONNULL operator T*()
    {
        return as_nonnull_ptr();
    }
    ALWAYS_INLINE RETURNS_NONNULL operator const T*() const
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
        AK::swap(m_ptr, other.m_ptr);
    }

    template<typename U>
    void swap(NonnullRefPtr<U>& other) requires(IsConvertible<U*, T*>)
    {
        AK::swap(m_ptr, other.m_ptr);
    }

    // clang-format off
private:
    NonnullRefPtr() = delete;
    // clang-format on

    ALWAYS_INLINE RETURNS_NONNULL T* as_nonnull_ptr() const
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    T* m_ptr { nullptr };
};

template<typename T>
inline NonnullRefPtr<T> adopt_ref(T& object)
{
    return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, object);
}

template<typename T>
struct Formatter<NonnullRefPtr<T>> : Formatter<const T*> {
    ErrorOr<void> format(FormatBuilder& builder, NonnullRefPtr<T> const& value)
    {
        return Formatter<const T*>::format(builder, value.ptr());
    }
};

template<typename T, typename U>
inline void swap(NonnullRefPtr<T>& a, NonnullRefPtr<U>& b) requires(IsConvertible<U*, T*>)
{
    a.swap(b);
}

template<typename T, class... Args>
requires(IsConstructible<T, Args...>) inline NonnullRefPtr<T> make_ref_counted(Args&&... args)
{
    return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, *new T(forward<Args>(args)...));
}

// FIXME: Remove once P0960R3 is available in Clang.
template<typename T, class... Args>
inline NonnullRefPtr<T> make_ref_counted(Args&&... args)
{
    return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, *new T { forward<Args>(args)... });
}
}

template<typename T>
struct Traits<NonnullRefPtr<T>> : public GenericTraits<NonnullRefPtr<T>> {
    using PeekType = T*;
    using ConstPeekType = const T*;
    static unsigned hash(const NonnullRefPtr<T>& p) { return ptr_hash(p.ptr()); }
    static bool equals(const NonnullRefPtr<T>& a, const NonnullRefPtr<T>& b) { return a.ptr() == b.ptr(); }
};

using AK::adopt_ref;
using AK::make_ref_counted;
using AK::NonnullRefPtr;

#endif
