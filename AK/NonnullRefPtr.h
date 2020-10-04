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
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class OwnPtr;
template<typename T>
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
class NonnullRefPtr {
public:
    typedef T ElementType;

    enum AdoptTag { Adopt };

    ALWAYS_INLINE NonnullRefPtr(const T& object)
        : m_ptr(const_cast<T*>(&object))
    {
        m_ptr->ref();
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(const U& object)
        : m_ptr(&const_cast<U&>(object))
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
    ALWAYS_INLINE NonnullRefPtr(NonnullRefPtr<U>&& other)
        : m_ptr(&other.leak_ref())
    {
    }
    ALWAYS_INLINE NonnullRefPtr(const NonnullRefPtr& other)
        : m_ptr(const_cast<T*>(other.ptr()))
    {
        m_ptr->ref();
    }
    template<typename U>
    ALWAYS_INLINE NonnullRefPtr(const NonnullRefPtr<U>& other)
        : m_ptr(const_cast<U*>(other.ptr()))
    {
        m_ptr->ref();
    }
    ALWAYS_INLINE ~NonnullRefPtr()
    {
        unref_if_not_null(m_ptr);
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
        NonnullRefPtr ptr(other);
        swap(ptr);
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(const NonnullRefPtr<U>& other)
    {
        NonnullRefPtr ptr(other);
        swap(ptr);
        return *this;
    }

    ALWAYS_INLINE NonnullRefPtr& operator=(NonnullRefPtr&& other)
    {
        NonnullRefPtr ptr(move(other));
        swap(ptr);
        return *this;
    }

    template<typename U>
    NonnullRefPtr& operator=(NonnullRefPtr<U>&& other)
    {
        NonnullRefPtr ptr(move(other));
        swap(ptr);
        return *this;
    }

    NonnullRefPtr& operator=(const T& object)
    {
        NonnullRefPtr ptr(object);
        swap(ptr);
        return *this;
    }

    [[nodiscard]] ALWAYS_INLINE T& leak_ref()
    {
        ASSERT(m_ptr);
        return *exchange(m_ptr, nullptr);
    }

    ALWAYS_INLINE T* ptr()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    ALWAYS_INLINE const T* ptr() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    ALWAYS_INLINE T* operator->()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    ALWAYS_INLINE const T* operator->() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    ALWAYS_INLINE T& operator*()
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }
    ALWAYS_INLINE const T& operator*() const
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

    ALWAYS_INLINE operator T*()
    {
        ASSERT(m_ptr);
        return m_ptr;
    }
    ALWAYS_INLINE operator const T*() const
    {
        ASSERT(m_ptr);
        return m_ptr;
    }

    ALWAYS_INLINE operator T&()
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }
    ALWAYS_INLINE operator const T&() const
    {
        ASSERT(m_ptr);
        return *m_ptr;
    }

    operator bool() const = delete;
    bool operator!() const = delete;

    void swap(NonnullRefPtr& other)
    {
        ::swap(m_ptr, other.m_ptr);
    }

    template<typename U>
    void swap(NonnullRefPtr<U>& other)
    {
        ::swap(m_ptr, other.m_ptr);
    }

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

template<typename T>
struct Formatter<NonnullRefPtr<T>> : Formatter<const T*> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const NonnullRefPtr<T>& value)
    {
        Formatter<const T*>::format(params, builder, value.ptr());
    }
};

template<typename T, typename U>
inline void swap(NonnullRefPtr<T>& a, NonnullRefPtr<U>& b)
{
    a.swap(b);
}

}

using AK::adopt;
using AK::NonnullRefPtr;
