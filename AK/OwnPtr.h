/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#ifdef KERNEL
#    include <Kernel/KResult.h>
#endif

namespace AK {

template<typename T>
class OwnPtr {
public:
    OwnPtr() = default;

    OwnPtr(decltype(nullptr))
        : m_ptr(nullptr)
    {
    }

    OwnPtr(OwnPtr&& other)
        : m_ptr(other.leak_ptr())
    {
    }

    template<typename U>
    OwnPtr(NonnullOwnPtr<U>&& other)
        : m_ptr(other.leak_ptr())
    {
    }
    template<typename U>
    OwnPtr(OwnPtr<U>&& other)
        : m_ptr(other.leak_ptr())
    {
    }
    ~OwnPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        m_ptr = (T*)(explode_byte(0xe1));
#endif
    }

    OwnPtr(OwnPtr const&) = delete;
    template<typename U>
    OwnPtr(const OwnPtr<U>&) = delete;
    OwnPtr& operator=(OwnPtr const&) = delete;
    template<typename U>
    OwnPtr& operator=(const OwnPtr<U>&) = delete;

    template<typename U>
    OwnPtr(const NonnullOwnPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const NonnullOwnPtr<U>&) = delete;
    template<typename U>
    OwnPtr(const RefPtr<U>&) = delete;
    template<typename U>
    OwnPtr(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    OwnPtr(const WeakPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const RefPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    OwnPtr& operator=(const WeakPtr<U>&) = delete;

    OwnPtr& operator=(OwnPtr&& other)
    {
        OwnPtr ptr(move(other));
        swap(ptr);
        return *this;
    }

    template<typename U>
    OwnPtr& operator=(OwnPtr<U>&& other)
    {
        OwnPtr ptr(move(other));
        swap(ptr);
        return *this;
    }

    template<typename U>
    OwnPtr& operator=(NonnullOwnPtr<U>&& other)
    {
        OwnPtr ptr(move(other));
        swap(ptr);
        VERIFY(m_ptr);
        return *this;
    }

    OwnPtr& operator=(T* ptr) = delete;

    OwnPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    void clear()
    {
        delete m_ptr;
        m_ptr = nullptr;
    }

    bool operator!() const { return !m_ptr; }

    [[nodiscard]] T* leak_ptr()
    {
        T* leaked_ptr = m_ptr;
        m_ptr = nullptr;
        return leaked_ptr;
    }

    NonnullOwnPtr<T> release_nonnull()
    {
        VERIFY(m_ptr);
        return NonnullOwnPtr<T>(NonnullOwnPtr<T>::Adopt, *leak_ptr());
    }

    template<typename U>
    NonnullOwnPtr<U> release_nonnull()
    {
        VERIFY(m_ptr);
        return NonnullOwnPtr<U>(NonnullOwnPtr<U>::Adopt, static_cast<U&>(*leak_ptr()));
    }

    T* ptr() { return m_ptr; }
    T const* ptr() const { return m_ptr; }

    T* operator->()
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    T const* operator->() const
    {
        VERIFY(m_ptr);
        return m_ptr;
    }

    T& operator*()
    {
        VERIFY(m_ptr);
        return *m_ptr;
    }

    T const& operator*() const
    {
        VERIFY(m_ptr);
        return *m_ptr;
    }

    operator T const*() const { return m_ptr; }
    operator T*() { return m_ptr; }

    operator bool() { return !!m_ptr; }

    void swap(OwnPtr& other)
    {
        ::swap(m_ptr, other.m_ptr);
    }

    template<typename U>
    void swap(OwnPtr<U>& other)
    {
        ::swap(m_ptr, other.m_ptr);
    }

    static OwnPtr lift(T* ptr)
    {
        return OwnPtr { ptr };
    }

protected:
    explicit OwnPtr(T* ptr)
        : m_ptr(ptr)
    {
        static_assert(
            requires { requires typename T::AllowOwnPtr()(); } || !requires { requires !typename T::AllowOwnPtr()(); declval<T>().ref(); declval<T>().unref(); }, "Use RefPtr<> for RefCounted types");
    }

private:
    T* m_ptr = nullptr;
};

template<typename T, typename U>
inline void swap(OwnPtr<T>& a, OwnPtr<U>& b)
{
    a.swap(b);
}

template<typename T>
inline OwnPtr<T> adopt_own_if_nonnull(T* object)
{
    if (object)
        return OwnPtr<T>::lift(object);
    return {};
}

#ifdef KERNEL
template<typename T>
inline Kernel::KResultOr<NonnullOwnPtr<T>> adopt_nonnull_own_or_enomem(T* object)
{
    auto result = adopt_own_if_nonnull(object);
    if (!result)
        return ENOMEM;
    return result.release_nonnull();
}
#endif

template<typename T, class... Args>
requires(IsConstructible<T, Args...>) inline OwnPtr<T> try_make(Args&&... args)
{
    return adopt_own_if_nonnull(new (nothrow) T(forward<Args>(args)...));
}

// FIXME: Remove once P0960R3 is available in Clang.
template<typename T, class... Args>
inline OwnPtr<T> try_make(Args&&... args)

{
    return adopt_own_if_nonnull(new (nothrow) T { forward<Args>(args)... });
}

template<typename T>
struct Traits<OwnPtr<T>> : public GenericTraits<OwnPtr<T>> {
    using PeekType = T*;
    using ConstPeekType = T const*;
    static unsigned hash(const OwnPtr<T>& p) { return ptr_hash(p.ptr()); }
    static bool equals(const OwnPtr<T>& a, const OwnPtr<T>& b) { return a.ptr() == b.ptr(); }
};

}

using AK::adopt_own_if_nonnull;
using AK::OwnPtr;
using AK::try_make;

#ifdef KERNEL
using AK::adopt_nonnull_own_or_enomem;
#endif
