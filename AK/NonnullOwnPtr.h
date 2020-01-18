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
#include <AK/Traits.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class RefPtr;
template<typename T>
class NonnullRefPtr;
template<typename T>
class WeakPtr;

template<typename T>
class CONSUMABLE(unconsumed) NonnullOwnPtr {
public:
    typedef T ElementType;

    enum AdoptTag { Adopt };

    RETURN_TYPESTATE(unconsumed)
    NonnullOwnPtr(AdoptTag, T& ptr)
        : m_ptr(&ptr)
    {
    }
    RETURN_TYPESTATE(unconsumed)
    NonnullOwnPtr(NonnullOwnPtr&& other)
        : m_ptr(other.leak_ptr())
    {
        ASSERT(m_ptr);
    }
    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullOwnPtr(NonnullOwnPtr<U>&& other)
        : m_ptr(static_cast<T*>(other.leak_ptr()))
    {
        ASSERT(m_ptr);
    }
    ~NonnullOwnPtr()
    {
        clear();
#ifdef SANITIZE_PTRS
        if constexpr (sizeof(T*) == 8)
            m_ptr = (T*)(0xe3e3e3e3e3e3e3e3);
        else
            m_ptr = (T*)(0xe3e3e3e3);
#endif
    }

    NonnullOwnPtr(const NonnullOwnPtr&) = delete;
    template<typename U>
    NonnullOwnPtr(const NonnullOwnPtr<U>&) = delete;
    NonnullOwnPtr& operator=(const NonnullOwnPtr&) = delete;
    template<typename U>
    NonnullOwnPtr& operator=(const NonnullOwnPtr<U>&) = delete;

    template<typename U>
    NonnullOwnPtr(const RefPtr<U>&) = delete;
    template<typename U>
    NonnullOwnPtr(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    NonnullOwnPtr(const WeakPtr<U>&) = delete;
    template<typename U>
    NonnullOwnPtr& operator=(const RefPtr<U>&) = delete;
    template<typename U>
    NonnullOwnPtr& operator=(const NonnullRefPtr<U>&) = delete;
    template<typename U>
    NonnullOwnPtr& operator=(const WeakPtr<U>&) = delete;

    RETURN_TYPESTATE(unconsumed)
    NonnullOwnPtr& operator=(NonnullOwnPtr&& other)
    {
        if (this != &other) {
            delete m_ptr;
            m_ptr = other.leak_ptr();
            ASSERT(m_ptr);
        }
        return *this;
    }

    template<typename U>
    RETURN_TYPESTATE(unconsumed)
    NonnullOwnPtr& operator=(NonnullOwnPtr<U>&& other)
    {
        if (this != static_cast<void*>(&other)) {
            delete m_ptr;
            m_ptr = other.leak_ptr();
            ASSERT(m_ptr);
        }
        return *this;
    }

    CALLABLE_WHEN(unconsumed)
    SET_TYPESTATE(consumed)
    T* leak_ptr()
    {
        return exchange(m_ptr, nullptr);
    }

    CALLABLE_WHEN(unconsumed)
    T* ptr() { return m_ptr; }
    CALLABLE_WHEN(unconsumed)
    const T* ptr() const { return m_ptr; }

    CALLABLE_WHEN(unconsumed)
    T* operator->() { return m_ptr; }
    CALLABLE_WHEN(unconsumed)
    const T* operator->() const { return m_ptr; }

    CALLABLE_WHEN(unconsumed)
    T& operator*() { return *m_ptr; }
    CALLABLE_WHEN(unconsumed)
    const T& operator*() const { return *m_ptr; }

    CALLABLE_WHEN(unconsumed)
    operator const T*() const { return m_ptr; }
    CALLABLE_WHEN(unconsumed)
    operator T*() { return m_ptr; }

    operator bool() const = delete;
    bool operator!() const = delete;

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

template<class T, class... Args>
inline NonnullOwnPtr<T>
make(Args&&... args)
{
    return NonnullOwnPtr<T>(NonnullOwnPtr<T>::Adopt, *new T(forward<Args>(args)...));
}

template<typename T>
struct Traits<NonnullOwnPtr<T>> : public GenericTraits<NonnullOwnPtr<T>> {
    using PeekType = const T*;
    static unsigned hash(const NonnullOwnPtr<T>& p) { return int_hash((u32)p.ptr()); }
    static void dump(const NonnullOwnPtr<T>& p) { kprintf("%p", p.ptr()); }
    static bool equals(const NonnullOwnPtr<T>& a, const NonnullOwnPtr<T>& b) { return a.ptr() == b.ptr(); }
};

template<typename T>
inline const LogStream& operator<<(const LogStream& stream, const NonnullOwnPtr<T>& value)
{
    return stream << value.ptr();
}

}

using AK::make;
using AK::NonnullOwnPtr;
