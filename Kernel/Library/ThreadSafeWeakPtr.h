/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Weakable.h>

namespace AK {

template<typename T>
class [[nodiscard]] WeakPtr {
    template<typename U>
    friend class Weakable;

public:
    WeakPtr() = default;

    template<typename U>
    WeakPtr(WeakPtr<U> const& other) requires(IsBaseOf<T, U>)
        : m_link(other.m_link)
    {
    }

    template<typename U>
    WeakPtr(WeakPtr<U>&& other) requires(IsBaseOf<T, U>)
        : m_link(other.take_link())
    {
    }

    template<typename U>
    WeakPtr& operator=(WeakPtr<U>&& other) requires(IsBaseOf<T, U>)
    {
        m_link = other.take_link();
        return *this;
    }

    template<typename U>
    WeakPtr& operator=(WeakPtr<U> const& other) requires(IsBaseOf<T, U>)
    {
        if ((void const*)this != (void const*)&other)
            m_link = other.m_link;
        return *this;
    }

    WeakPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    template<typename U>
    WeakPtr(const U& object) requires(IsBaseOf<T, U>)
        : m_link(object.template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link())
    {
    }

    template<typename U>
    WeakPtr(const U* object) requires(IsBaseOf<T, U>)
    {
        if (object)
            m_link = object->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
    }

    template<typename U>
    WeakPtr(RefPtr<U> const& object) requires(IsBaseOf<T, U>)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        });
    }

    template<typename U>
    WeakPtr(NonnullRefPtr<U> const& object) requires(IsBaseOf<T, U>)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        });
    }

    template<typename U>
    WeakPtr& operator=(const U& object) requires(IsBaseOf<T, U>)
    {
        m_link = object.template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        return *this;
    }

    template<typename U>
    WeakPtr& operator=(const U* object) requires(IsBaseOf<T, U>)
    {
        if (object)
            m_link = object->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
        else
            m_link = nullptr;
        return *this;
    }

    template<typename U>
    WeakPtr& operator=(RefPtr<U> const& object) requires(IsBaseOf<T, U>)
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
    WeakPtr& operator=(NonnullRefPtr<U> const& object) requires(IsBaseOf<T, U>)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template try_make_weak_ptr<U>().release_value_but_fixme_should_propagate_errors().take_link();
            else
                m_link = nullptr;
        });
        return *this;
    }

    [[nodiscard]] RefPtr<T> strong_ref() const
    {
        // This only works with RefCounted objects, but it is the only
        // safe way to get a strong reference from a WeakPtr. Any code
        // that uses objects not derived from RefCounted will have to
        // use unsafe_ptr(), but as the name suggests, it is not safe...
        RefPtr<T> ref;
        // Using do_while_locked protects against a race with clear()!
        m_link.do_while_locked([&](WeakLink* link) {
            if (link)
                ref = link->template strong_ref<T>();
        });
        return ref;
    }

    [[nodiscard]] T* unsafe_ptr() const
    {
        T* ptr = nullptr;
        m_link.do_while_locked([&](WeakLink* link) {
            if (link)
                ptr = link->unsafe_ptr<T>();
        });
        return ptr;
    }

    operator bool() const { return m_link ? !m_link->is_null() : false; }

    [[nodiscard]] bool is_null() const { return !m_link || m_link->is_null(); }
    void clear() { m_link = nullptr; }

    [[nodiscard]] RefPtr<WeakLink> take_link() { return move(m_link); }

private:
    WeakPtr(RefPtr<WeakLink> const& link)
        : m_link(link)
    {
    }

    RefPtr<WeakLink> m_link;
};

template<typename T>
template<typename U>
inline ErrorOr<WeakPtr<U>> Weakable<T>::try_make_weak_ptr() const
{
    if constexpr (IsBaseOf<RefCountedBase, T>) {
        // Checking m_being_destroyed isn't sufficient when dealing with
        // a RefCounted type.The reference count will drop to 0 before the
        // destructor is invoked and revoke_weak_ptrs is called. So, try
        // to add a ref (which should fail if the ref count is at 0) so
        // that we prevent the destructor and revoke_weak_ptrs from being
        // triggered until we're done.
        if (!static_cast<const T*>(this)->try_ref())
            return WeakPtr<U> {};
    } else {
        // For non-RefCounted types this means a weak reference can be
        // obtained until the ~Weakable destructor is invoked!
        if (m_being_destroyed.load(AK::MemoryOrder::memory_order_acquire))
            return WeakPtr<U> {};
    }
    if (!m_link) {
        // There is a small chance that we create a new WeakLink and throw
        // it away because another thread beat us to it. But the window is
        // pretty small and the overhead isn't terrible.
        m_link.assign_if_null(TRY(adopt_nonnull_ref_or_enomem(new (nothrow) WeakLink(const_cast<T&>(static_cast<const T&>(*this))))));
    }

    WeakPtr<U> weak_ptr(m_link);

    if constexpr (IsBaseOf<RefCountedBase, T>) {
        // Now drop the reference we temporarily added
        if (static_cast<const T*>(this)->unref()) {
            // We just dropped the last reference, which should have called
            // revoke_weak_ptrs, which should have invalidated our weak_ptr
            VERIFY(!weak_ptr.strong_ref());
            return WeakPtr<U> {};
        }
    }
    return weak_ptr;
}

template<typename T>
struct Formatter<WeakPtr<T>> : Formatter<const T*> {
    ErrorOr<void> format(FormatBuilder& builder, WeakPtr<T> const& value)
    {
        auto ref = value.strong_ref();
        return Formatter<const T*>::format(builder, ref.ptr());
    }
};

template<typename T>
ErrorOr<WeakPtr<T>> try_make_weak_ptr_if_nonnull(T const* ptr)
{
    if (ptr) {
        return ptr->template try_make_weak_ptr<T>();
    }
    return WeakPtr<T> {};
}

}

using AK::WeakPtr;
