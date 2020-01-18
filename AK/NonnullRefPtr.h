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
#include <AK/LogStream.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class OwnPtr;
template<typename T>
class RefPtr;

template<typename T>
inline void ref_if_not_null(T* ptr)
{
    if (ptr)
        ptr->ref();
}

template<typename T>
inline void deref_if_not_null(T* ptr)
{
    if (ptr)
        ptr->deref();
}

template<typename T>
class CONSUMABLE(unconsumed) NonnullRefPtr {
public:
    typedef T ElementType;

    enum AdoptTag { Adopt };

    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->ref();
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const U& object)
        : m_ptr(&const_cast<T&>(static_cast<const T&>(object)))
    {
        m_ptr->ref();
    }
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(AdoptTag, T& object)
        : m_ptr(&object)
    {
    }
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(NonnullRefPtr&& other)
        : m_ptr(&other.leak_ref())
    {
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(NonnullRefPtr<U>&& other)
        : m_ptr(static_cast<T*>(&other.leak_ref()))
    {
    }
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const NonnullRefPtr& other)
        : m_ptr(const_cast<T*>(other.ptr()))
    {
        m_ptr->ref();
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullRefPtr(const NonnullRefPtr<U>& other)
        : m_ptr(const_cast<T*>(static_cast<const T*>((other.ptr()))))
    {
        m_ptr->ref();
    }
    ~NonnullRefPtr()
    {
        deref_if_not_null(m_ptr);
        m_ptr = nullptr;
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)(0xb0b0b0b0b0b0b0b0);
        else
            m_ptr = (T*)(0xb0b0b0b0);
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
        if (m_ptr != other.m_ptr) {
            deref_if_not_null(m_ptr);
            m_ptr = const_cast<T*>(other.ptr());
            m_ptr->ref();
        }
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(const NonnullRefPtr<U>& other)
    {
        if (m_ptr != other.ptr()) {
            deref_if_not_null(m_ptr);
            m_ptr = const_cast<T*>(static_cast<const T*>(other.ptr()));
            m_ptr->ref();
        }
        return *this;
    }

    NonnullRefPtr& operator=(NonnullRefPtr&& other)
    {
        if (this != &other) {
            deref_if_not_null(m_ptr);
            m_ptr = &other.leak_ref();
        }
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(NonnullRefPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            deref_if_not_null(m_ptr);
            m_ptr = static_cast<T*>(&other.leak_ref());
        }
        return *this;
    }

    NonnullRefPtr& operator=(const T& object)
    {
        if (m_ptr != &object) {
            deref_if_not_null(m_ptr);
            m_ptr = const_cast<T*>(&object);
            m_ptr->ref();
        }
        return *this;
    }

    [[nodiscard]] CALLABLE_WHEN(unconsumed)
    SET_TYPESTATE(consumed)
    T& leak_ref()
    {
        ASSERT(m_ptr);
        return *exchange(m_ptr, nullptr);
    }

    CALLABLE_WHEN("unconsumed","unknown")
    T* ptr()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    CALLABLE_WHEN("unconsumed","unknown")
    const T* ptr() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    T* operator->()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    const T* operator->() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    T& operator*()
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    const T& operator*() const
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    operator T*()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    operator const T*() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    CALLABLE_WHEN(unconsumed)
    operator T&()
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }
    CALLABLE_WHEN(unconsumed)
    operator const T&() const
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

    operator bool() const = delete;
    bool operator!() const = delete;

private:
    NonnullRefPtr() = delete;

    T* m_ptr { nullptr };
};

template<typename T>
inline NonnullRefPtr<T> adopt(T& object)
{
    return NonnullRefPtr<T>(NonnullRefPtr<T>::Adopt, object);
}

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const NonnullRefPtr<T>& value)
{
    return stream << value.ptr();
}

}

using AK::adopt;
using AK::NonnullRefPtr;
