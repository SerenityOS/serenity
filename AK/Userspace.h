/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
concept PointerTypeName = IsPointer<T>;

template<PointerTypeName T>
class Userspace {
public:
    Userspace() = default;

    operator bool() const { return m_ptr; }
    operator FlatPtr() const { return (FlatPtr)m_ptr; }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(Userspace const&) const = delete;
    bool operator<=(Userspace const&) const = delete;
    bool operator>=(Userspace const&) const = delete;
    bool operator<(Userspace const&) const = delete;
    bool operator>(Userspace const&) const = delete;

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
