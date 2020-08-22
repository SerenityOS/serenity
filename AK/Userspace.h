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

#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

// HACK: This is just here to make syntax highlighting work in Qt Creator.
//       Once it supports C++20 concepts, we can remove this.
#if defined(__cpp_concepts) && !defined(__COVERITY__)
template<typename T>
concept PointerTypeName = IsPointer<T>::value;
template<PointerTypeName T>
#else
template<typename T, typename EnableIf<IsPointer<T>::value, int>::Type = 0>
#endif

class Userspace {
public:
    Userspace() { }

    operator bool() const { return m_ptr; }
    operator FlatPtr() const { return (FlatPtr)m_ptr; }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(const Userspace&) const = delete;
    bool operator<=(const Userspace&) const = delete;
    bool operator>=(const Userspace&) const = delete;
    bool operator<(const Userspace&) const = delete;
    bool operator>(const Userspace&) const = delete;

#ifdef KERNEL
    Userspace(FlatPtr ptr)
        : m_ptr(ptr)
    {
    }

    FlatPtr ptr() const { return m_ptr; }
    T unsafe_userspace_ptr() const { return (T)m_ptr; }
#else
    Userspace(T ptr)
        : m_ptr(ptr)
    {
    }

    T ptr() const { return m_ptr; }
#endif

private:
#ifdef KERNEL
    FlatPtr m_ptr { 0 };
#else
    T m_ptr { nullptr };
#endif
};

template<typename T, typename U>
inline Userspace<T> static_ptr_cast(const Userspace<U>& ptr)
{
#ifdef KERNEL
    auto casted_ptr = static_cast<T>(ptr.unsafe_userspace_ptr());
#else
    auto casted_ptr = static_cast<T>(ptr.ptr());
#endif
    return Userspace<T>((FlatPtr)casted_ptr);
}

}

using AK::static_ptr_cast;
using AK::Userspace;
