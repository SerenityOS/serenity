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

#include <AK/LogStream.h>
#include <AK/Weakable.h>

namespace AK {

template<typename T>
class WeakPtr {
    template<typename U>
    friend class Weakable;

public:
    WeakPtr() { }
    WeakPtr(std::nullptr_t) { }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr(const WeakPtr<U>& other)
        : m_link(other.m_link)
    {
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr(WeakPtr<U>&& other)
        : m_link(other.take_link())
    {
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr& operator=(WeakPtr<U>&& other)
    {
        m_link = other.take_link();
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
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

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr(const U& object)
        : m_link(object.template make_weak_ptr<U>().take_link())
    {
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr(const U* object)
    {
        if (object)
            m_link = object->template make_weak_ptr<U>().take_link();
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr(const RefPtr<U>& object)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                obj->template make_weak_ptr<U>().take_link();
        });
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr(const NonnullRefPtr<U>& object)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                obj->template make_weak_ptr<U>().take_link();
        });
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr& operator=(const U& object)
    {
        m_link = object.template make_weak_ptr<U>().take_link();
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr& operator=(const U* object)
    {
        if (object)
            m_link = object->template make_weak_ptr<U>().take_link();
        else
            m_link = nullptr;
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr& operator=(const RefPtr<U>& object)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template make_weak_ptr<U>().take_link();
            else
                m_link = nullptr;
        });
        return *this;
    }

    template<typename U, typename EnableIf<IsBaseOf<T, U>::value>::Type* = nullptr>
    WeakPtr& operator=(const NonnullRefPtr<U>& object)
    {
        object.do_while_locked([&](U* obj) {
            if (obj)
                m_link = obj->template make_weak_ptr<U>().take_link();
            else
                m_link = nullptr;
        });
        return *this;
    }

    RefPtr<T> strong_ref() const
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

#ifndef KERNEL
    // A lot of user mode code is single-threaded. But for kernel mode code
    // this is generally not true as everything is multi-threaded. So make
    // these shortcuts and aliases only available to non-kernel code.
    T* ptr() const { return unsafe_ptr(); }
    T* operator->() { return unsafe_ptr(); }
    const T* operator->() const { return unsafe_ptr(); }
    operator const T*() const { return unsafe_ptr(); }
    operator T*() { return unsafe_ptr(); }
#endif

    T* unsafe_ptr() const
    {
        T* ptr = nullptr;
        m_link.do_while_locked([&](WeakLink* link) {
            if (link)
                ptr = link->unsafe_ptr<T>();
        });
        return ptr;
    }

    operator bool() const { return m_link ? !m_link->is_null() : false; }

    bool is_null() const { return !m_link || m_link->is_null(); }
    void clear() { m_link = nullptr; }

    RefPtr<WeakLink> take_link() { return move(m_link); }

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
#ifdef DEBUG
    ASSERT(!m_being_destroyed);
#endif
    if (!m_link) {
        // There is a small chance that we create a new WeakLink and throw
        // it away because another thread beat us to it. But the window is
        // pretty small and the overhead isn't terrible.
        m_link.assign_if_null(adopt(*new WeakLink(const_cast<T&>(static_cast<const T&>(*this)))));
    }
    return WeakPtr<U>(m_link);
}

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const WeakPtr<T>& value)
{
#ifdef KERNEL
    auto ref = value.strong_ref();
    return stream << ref.ptr();
#else
    return stream << value.ptr();
#endif
}

template<typename T>
struct Formatter<WeakPtr<T>> : Formatter<const T*> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const WeakPtr<T>& value)
    {
#ifdef KERNEL
        auto ref = value.strong_ref();
        Formatter<const T*>::format(params, builder, ref.ptr());
#else
        Formatter<const T*>::format(params, builder, value.ptr());
#endif
    }
};

}

using AK::WeakPtr;
