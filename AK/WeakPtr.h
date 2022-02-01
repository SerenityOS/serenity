/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef KERNEL
#    include <Kernel/Library/ThreadSafeWeakPtr.h>
#else

#    include <AK/Weakable.h>

namespace AK {

template<typename T>
class [[nodiscard]] WeakPtr {
    template<typename U>
    friend class Weakable;

public:
    WeakPtr() = default;

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr(const WeakPtr<U>& other)
        : m_link(other.m_link)
    {
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr(WeakPtr<U>&& other)
        : m_link(other.take_link())
    {
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr& operator=(WeakPtr<U>&& other)
    {
        m_link = other.take_link();
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr& operator=(const WeakPtr<U>& other)
    {
        if ((const void*)this != (const void*)&other)
            m_link = other.m_link;
        return *this;
    }

    WeakPtr& operator=(std::nullptr_t)
    {
        clear();
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr(const U& object)
        : m_link(object.template make_weak_ptr<U>().take_link())
    {
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr(const U* object)
    {
        if (object)
            m_link = object->template make_weak_ptr<U>().take_link();
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr(RefPtr<U> const& object)
    {
        if (object)
            m_link = object->template make_weak_ptr<U>().take_link();
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr(NonnullRefPtr<U> const& object)
    {
        m_link = object->template make_weak_ptr<U>().take_link();
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr& operator=(const U& object)
    {
        m_link = object.template make_weak_ptr<U>().take_link();
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr& operator=(const U* object)
    {
        if (object)
            m_link = object->template make_weak_ptr<U>().take_link();
        else
            m_link = nullptr;
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr& operator=(const RefPtr<U>& object)
    {
        if (object)
            m_link = object->template make_weak_ptr<U>().take_link();
        else
            m_link = nullptr;
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>>::Type* = nullptr>
    WeakPtr& operator=(const NonnullRefPtr<U>& object)
    {
        m_link = object->template make_weak_ptr<U>().take_link();
        return *this;
    }

    [[nodiscard]] RefPtr<T> strong_ref() const
    {
        return RefPtr<T> { ptr() };
    }

    T* ptr() const { return unsafe_ptr(); }
    T* operator->() { return unsafe_ptr(); }
    const T* operator->() const { return unsafe_ptr(); }
    operator const T*() const { return unsafe_ptr(); }
    operator T*() { return unsafe_ptr(); }

    [[nodiscard]] T* unsafe_ptr() const
    {
        if (m_link)
            return m_link->template unsafe_ptr<T>();
        return nullptr;
    }

    operator bool() const { return m_link ? !m_link->is_null() : false; }

    [[nodiscard]] bool is_null() const { return !m_link || m_link->is_null(); }
    void clear() { m_link = nullptr; }

    [[nodiscard]] RefPtr<WeakLink> take_link() { return move(m_link); }

private:
    WeakPtr(const RefPtr<WeakLink>& link)
        : m_link(link)
    {
    }

    RefPtr<WeakLink> m_link;
};

template<typename T>
template<typename U>
inline WeakPtr<U> Weakable<T>::make_weak_ptr() const
{
    if (!m_link)
        m_link = adopt_ref(*new WeakLink(const_cast<T&>(static_cast<T const&>(*this))));

    return WeakPtr<U>(m_link);
}

template<typename T>
struct Formatter<WeakPtr<T>> : Formatter<const T*> {
    ErrorOr<void> format(FormatBuilder& builder, WeakPtr<T> const& value)
    {
        return Formatter<const T*>::format(builder, value.ptr());
    }
};

template<typename T>
WeakPtr<T> try_make_weak_ptr(const T* ptr)
{
    if (ptr) {
        return ptr->template make_weak_ptr<T>();
    }
    return {};
}

}

using AK::WeakPtr;
#endif
