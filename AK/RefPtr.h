/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define REFPTR_SCRUB_BYTE 0xe0

#ifdef KERNEL
#    include <Kernel/Library/ThreadSafeRefPtr.h>
#else

#    include <AK/Assertions.h>
#    include <AK/Atomic.h>
#    include <AK/Error.h>
#    include <AK/Format.h>
#    include <AK/NonnullRefPtr.h>
#    include <AK/StdLibExtras.h>
#    include <AK/Traits.h>
#    include <AK/Types.h>

namespace AK {

template<typename T>
class OwnPtr;

template<typename T, typename PtrTraits>
class [[nodiscard]] RefPtr {
    template<typename U, typename P>
    friend class RefPtr;
    template<typename U>
    friend class WeakPtr;

public:
    enum AdoptTag {
        Adopt
    };

    RefPtr() = default;
    RefPtr(T const* ptr)
        : m_ptr(const_cast<T*>(ptr))
    {
        ref_if_not_null(m_ptr);
    }

    RefPtr(T const& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->ref();
    }

    RefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }

    RefPtr(RefPtr&& other)
        : m_ptr(other.leak_ref())
    {
    }

    ALWAYS_INLINE RefPtr(NonnullRefPtr<T> const& other)
        : m_ptr(const_cast<T*>(other.ptr()))
    {
        m_ptr->ref();
    }

    template<typename U>
    ALWAYS_INLINE RefPtr(NonnullRefPtr<U> const& other) requires(IsConvertible<U*, T*>)
        : m_ptr(const_cast<T*>(static_cast<T const*>(other.ptr())))
    {
        m_ptr->ref();
    }

    template<typename U>
    ALWAYS_INLINE RefPtr(NonnullRefPtr<U>&& other) requires(IsConvertible<U*, T*>)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
    }

    template<typename U, typename P = RefPtrTraits<U>>
    RefPtr(RefPtr<U, P>&& other) requires(IsConvertible<U*, T*>)
        : m_ptr(static_cast<T*>(other.leak_ref()))
    {
    }

    RefPtr(RefPtr const& other)
        : m_ptr(other.m_ptr)
    {
        ref_if_not_null(m_ptr);
    }

    template<typename U, typename P = RefPtrTraits<U>>
    RefPtr(RefPtr<U, P> const& other) requires(IsConvertible<U*, T*>)
        : m_ptr(const_cast<T*>(static_cast<T const*>(other.ptr())))
    {
        ref_if_not_null(m_ptr);
    }

    ALWAYS_INLINE ~RefPtr()
    {
        clear();
#    ifdef SANITIZE_PTRS
        m_ptr = reinterpret_cast<T*>(explode_byte(REFPTR_SCRUB_BYTE));
#    endif
    }

    template<typename U>
    RefPtr(OwnPtr<U> const&) = delete;
    template<typename U>
    RefPtr& operator=(OwnPtr<U> const&) = delete;

    void swap(RefPtr& other)
    {
        AK::swap(m_ptr, other.m_ptr);
    }

    template<typename U, typename P = RefPtrTraits<U>>
    void swap(RefPtr<U, P>& other) requires(IsConvertible<U*, T*>)
    {
        AK::swap(m_ptr, other.m_ptr);
    }

    ALWAYS_INLINE RefPtr& operator=(RefPtr&& other)
    {
        RefPtr tmp { move(other) };
        swap(tmp);
        return *this;
    }

    template<typename U, typename P = RefPtrTraits<U>>
    ALWAYS_INLINE RefPtr& operator=(RefPtr<U, P>&& other) requires(IsConvertible<U*, T*>)
    {
        RefPtr tmp { move(other) };
        swap(tmp);
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(NonnullRefPtr<U>&& other) requires(IsConvertible<U*, T*>)
    {
        RefPtr tmp { move(other) };
        swap(tmp);
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(NonnullRefPtr<T> const& other)
    {
        RefPtr tmp { other };
        swap(tmp);
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(NonnullRefPtr<U> const& other) requires(IsConvertible<U*, T*>)
    {
        RefPtr tmp { other };
        swap(tmp);
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(RefPtr const& other)
    {
        RefPtr tmp { other };
        swap(tmp);
        return *this;
    }

    template<typename U>
    ALWAYS_INLINE RefPtr& operator=(RefPtr<U> const& other) requires(IsConvertible<U*, T*>)
    {
        RefPtr tmp { other };
        swap(tmp);
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(T const* ptr)
    {
        RefPtr tmp { ptr };
        swap(tmp);
        return *this;
    }

    ALWAYS_INLINE RefPtr& operator=(T const& object)
    {
        RefPtr tmp { object };
        swap(tmp);
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
        *this = move(other);
        return true;
    }

    template<typename U, typename P = RefPtrTraits<U>>
    ALWAYS_INLINE bool assign_if_null(RefPtr<U, P>&& other)
    {
        if (this == &other)
            return is_null();
        *this = move(other);
        return true;
    }

    ALWAYS_INLINE void clear()
    {
        unref_if_not_null(m_ptr);
        m_ptr = nullptr;
    }

    bool operator!() const { return !m_ptr; }

    [[nodiscard]] T* leak_ref()
    {
        return exchange(m_ptr, nullptr);
    }

    NonnullRefPtr<T> release_nonnull()
    {
        auto* ptr = leak_ref();
        VERIFY(ptr);
        return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, *ptr);
    }

    ALWAYS_INLINE T* ptr() { return as_ptr(); }
    ALWAYS_INLINE const T* ptr() const { return as_ptr(); }

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

    ALWAYS_INLINE operator const T*() const { return as_ptr(); }
    ALWAYS_INLINE operator T*() { return as_ptr(); }

    ALWAYS_INLINE operator bool() { return !is_null(); }

    bool operator==(std::nullptr_t) const { return is_null(); }
    bool operator!=(std::nullptr_t) const { return !is_null(); }

    bool operator==(const RefPtr& other) const { return as_ptr() == other.as_ptr(); }
    bool operator!=(const RefPtr& other) const { return as_ptr() != other.as_ptr(); }

    bool operator==(RefPtr& other) { return as_ptr() == other.as_ptr(); }
    bool operator!=(RefPtr& other) { return as_ptr() != other.as_ptr(); }

    bool operator==(const T* other) const { return as_ptr() == other; }
    bool operator!=(const T* other) const { return as_ptr() != other; }

    bool operator==(T* other) { return as_ptr() == other; }
    bool operator!=(T* other) { return as_ptr() != other; }

    ALWAYS_INLINE bool is_null() const { return !m_ptr; }

private:
    ALWAYS_INLINE T* as_ptr() const
    {
        return m_ptr;
    }

    ALWAYS_INLINE T* as_nonnull_ptr() const
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    T* m_ptr { nullptr };
};

template<typename T>
struct Formatter<RefPtr<T>> : Formatter<const T*> {
    ErrorOr<void> format(FormatBuilder& builder, RefPtr<T> const& value)
    {
        return Formatter<const T*>::format(builder, value.ptr());
    }
};

template<typename T>
struct Traits<RefPtr<T>> : public GenericTraits<RefPtr<T>> {
    using PeekType = T*;
    using ConstPeekType = const T*;
    static unsigned hash(const RefPtr<T>& p) { return ptr_hash(p.ptr()); }
    static bool equals(const RefPtr<T>& a, const RefPtr<T>& b) { return a.ptr() == b.ptr(); }
};

template<typename T, typename U>
inline NonnullRefPtr<T> static_ptr_cast(const NonnullRefPtr<U>& ptr)
{
    return NonnullRefPtr<T>(static_cast<const T&>(*ptr));
}

template<typename T, typename U, typename PtrTraits = RefPtrTraits<T>>
inline RefPtr<T> static_ptr_cast(const RefPtr<U>& ptr)
{
    return RefPtr<T, PtrTraits>(static_cast<const T*>(ptr.ptr()));
}

template<typename T, typename PtrTraitsT, typename U, typename PtrTraitsU>
inline void swap(RefPtr<T, PtrTraitsT>& a, RefPtr<U, PtrTraitsU>& b) requires(IsConvertible<U*, T*>)
{
    a.swap(b);
}

template<typename T>
inline RefPtr<T> adopt_ref_if_nonnull(T* object)
{
    if (object)
        return RefPtr<T>(RefPtr<T>::Adopt, *object);
    return {};
}

template<typename T, class... Args>
requires(IsConstructible<T, Args...>) inline ErrorOr<NonnullRefPtr<T>> try_make_ref_counted(Args&&... args)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) T(forward<Args>(args)...));
}

// FIXME: Remove once P0960R3 is available in Clang.
template<typename T, class... Args>
inline ErrorOr<NonnullRefPtr<T>> try_make_ref_counted(Args&&... args)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) T { forward<Args>(args)... });
}

template<typename T>
inline ErrorOr<NonnullRefPtr<T>> adopt_nonnull_ref_or_enomem(T* object)
{
    auto result = adopt_ref_if_nonnull(object);
    if (!result)
        return Error::from_errno(ENOMEM);
    return result.release_nonnull();
}

}

using AK::adopt_ref_if_nonnull;
using AK::RefPtr;
using AK::static_ptr_cast;
using AK::try_make_ref_counted;

#endif
