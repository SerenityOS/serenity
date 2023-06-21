/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>

#ifdef KERNEL
#    include <Kernel/Memory/VirtualAddress.h>
#endif

namespace AK {

template<typename T>
concept PointerTypeName = IsPointer<T>;

template<PointerTypeName T>
class Userspace {
public:
    Userspace() = default;

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

    explicit operator bool() const { return m_ptr != 0; }

    FlatPtr ptr() const { return m_ptr; }
    VirtualAddress vaddr() const { return VirtualAddress(m_ptr); }
    T unsafe_userspace_ptr() const { return reinterpret_cast<T>(m_ptr); }
#else
    Userspace(T ptr)
        : m_ptr(ptr)
    {
    }

    explicit operator bool() const { return m_ptr != nullptr; }

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
inline Userspace<T> static_ptr_cast(Userspace<U> const& ptr)
{
#ifdef KERNEL
    auto casted_ptr = static_cast<T>(ptr.unsafe_userspace_ptr());
#else
    auto casted_ptr = static_cast<T>(ptr.ptr());
#endif
    return Userspace<T>(reinterpret_cast<FlatPtr>(casted_ptr));
}

}

#if USING_AK_GLOBALLY
using AK::static_ptr_cast;
using AK::Userspace;
#endif
