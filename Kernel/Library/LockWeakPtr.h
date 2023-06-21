/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Library/LockWeakable.h>

namespace AK {

template<typename T>
class [[nodiscard]] LockWeakPtr {
    template<typename U>
    friend class LockWeakable;

public:
    LockWeakPtr() = default;

    template<typename U>
    LockWeakPtr(WeakPtr<U> const& other)
    requires(IsBaseOf<T, U>)
        : m_link(other.m_link)
    {
    }

    template<typename U>
    LockWeakPtr(WeakPtr<U>&& other)
    requires(IsBaseOf<T, U>)
        : m_link(other.take_link())
    {
    }

    template<typename U>
    LockWeakPtr& operator=(WeakPtr<U>&& other)
    requires(IsBaseOf<T, U>)
    {
        m_link = other.take_link();
        return *this;
    }

    template<typename U>
    LockWeakPtr& operator=(WeakPtr<U> const& other)
    requires(IsBaseOf<T, U>)
    {
        if ((void const*)this != (void const*)&other)
            m_link = other.m_link;
        return *this;
    }

    LockWeakPtr& operator=(nullptr_t)
    {
        clear();
        return *this;
    }

    template<typename U>
    LockWeakPtr(U const& object)
    requires(IsBaseOf<T, U>)
        : m_link(object.template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link())
    {
    }

    template<typename U>
    LockWeakPtr(U const* object)
    requires(IsBaseOf<T, U>)
    {
        if (object)
            m_link = object->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
    }

    template<typename U>
    LockWeakPtr(LockRefPtr<U> const& object)
    requires(IsBaseOf<T, U>)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        });
    }

    template<typename U>
    LockWeakPtr(NonnullLockRefPtr<U> const& object)
    requires(IsBaseOf<T, U>)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        });
    }

    template<typename U>
    LockWeakPtr& operator=(U const& object)
    requires(IsBaseOf<T, U>)
    {
        m_link = object.template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        return *this;
    }

    template<typename U>
    LockWeakPtr& operator=(U const* object)
    requires(IsBaseOf<T, U>)
    {
        if (object)
            m_link = object->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        else
            m_link = nullptr;
        return *this;
    }

    template<typename U>
    LockWeakPtr& operator=(LockRefPtr<U> const& object)
    requires(IsBaseOf<T, U>)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
            else
                m_link = nullptr;
        });
        return *this;
    }

    template<typename U>
    LockWeakPtr& operator=(NonnullLockRefPtr<U> const& object)
    requires(IsBaseOf<T, U>)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
            else
                m_link = nullptr;
        });
        return *this;
    }

    [[nodiscard]] LockRefPtr<T> strong_ref() const
    {
        // This only works with RefCounted objects, but it is the only
        // safe way to get a strong reference from a LockWeakPtr. Any code
        // that uses objects not derived from RefCounted will have to
        // use unsafe_ptr(), but as the name suggests, it is not safe...
        LockRefPtr<T> ref;
        // Using do_while_locked protects against a race with clear()!
        m_link.do_while_locked([&](LockWeakLink* link) {
            if (link)
                ref = link->template strong_ref<T>();
        });
        return ref;
    }

    [[nodiscard]] T* unsafe_ptr() const
    {
        T* ptr = nullptr;
        m_link.do_while_locked([&](LockWeakLink* link) {
            if (link)
                ptr = link->unsafe_ptr<T>();
        });
        return ptr;
    }

    operator bool() const { return m_link ? !m_link->is_null() : false; }

    [[nodiscard]] bool is_null() const { return !m_link || m_link->is_null(); }
    void clear() { m_link = nullptr; }

    [[nodiscard]] LockRefPtr<LockWeakLink> take_link() { return move(m_link); }

private:
    LockWeakPtr(LockRefPtr<LockWeakLink> const& link)
        : m_link(link)
    {
    }

    LockRefPtr<LockWeakLink> m_link;
};

template<typename T>
template<typename U>
inline ErrorOr<LockWeakPtr<U>> LockWeakable<T>::try_make_weak_ptr() const
{
    if constexpr (IsBaseOf<AtomicRefCountedBase, T>) {
        // Checking m_being_destroyed isn't sufficient when dealing with
        // a RefCounted type.The reference count will drop to 0 before the
        // destructor is invoked and revoke_weak_ptrs is called. So, try
        // to add a ref (which should fail if the ref count is at 0) so
        // that we prevent the destructor and revoke_weak_ptrs from being
        // triggered until we're done.
        if (!static_cast<T const*>(this)->try_ref())
            return LockWeakPtr<U> {};
    } else {
        // For non-RefCounted types this means a weak reference can be
        // obtained until the ~LockWeakable destructor is invoked!
        if (m_being_destroyed.load(AK::MemoryOrder::memory_order_acquire))
            return LockWeakPtr<U> {};
    }
    if (!m_link) {
        // There is a small chance that we create a new LockWeakLink and throw
        // it away because another thread beat us to it. But the window is
        // pretty small and the overhead isn't terrible.
        m_link.assign_if_null(TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) LockWeakLink(const_cast<T&>(static_cast<T const&>(*this))))));
    }

    LockWeakPtr<U> weak_ptr(m_link);

    if constexpr (IsBaseOf<AtomicRefCountedBase, T>) {
        // Now drop the reference we temporarily added
        if (static_cast<T const*>(this)->unref()) {
            // We just dropped the last reference, which should have called
            // revoke_weak_ptrs, which should have invalidated our weak_ptr
            VERIFY(!weak_ptr.strong_ref());
            return LockWeakPtr<U> {};
        }
    }
    return weak_ptr;
}

template<typename T>
struct Formatter<LockWeakPtr<T>> : Formatter<T const*> {
    ErrorOr<void> format(FormatBuilder& builder, LockWeakPtr<T> const& value)
    {
        auto ref = value.strong_ref();
        return Formatter<T const*>::format(builder, ref.ptr());
    }
};

template<typename T>
ErrorOr<LockWeakPtr<T>> try_make_weak_ptr_if_nonnull(T const* ptr)
{
    if (ptr) {
        return ptr->template try_make_weak_ptr<T>();
    }
    return LockWeakPtr<T> {};
}

}

using AK::LockWeakPtr;
