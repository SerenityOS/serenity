/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

#ifdef KERNEL
#    include <Kernel/Memory/VirtualAddress.h>
#endif

namespace AK {

template<typename T>
concept PointerTypeName = IsPointer<T>;

namespace Detail {
template<typename T>
void get_pointee_type_or_void();

template<typename T>
requires(!IsSameIgnoringCV<RemovePointer<T>, void>)
RemovePointer<T>& get_pointee_type_or_void();

template<typename T>
using PointeeTypeOrVoid = decltype(get_pointee_type_or_void<T>());
}

template<PointerTypeName T>
class Userspace {
    using ElementType = RemovePointer<T>;
    constexpr static bool allow_deref = !IsSameIgnoringCV<ElementType, void>;
    using Ref = Detail::PointeeTypeOrVoid<T>;
    using CRef = Detail::AddConstToReferencedType<Ref>;

public:
    Userspace() = default;
    Userspace(nullptr_t) { }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(Userspace const&) const = delete;
    bool operator<=(Userspace const&) const = delete;
    bool operator>=(Userspace const&) const = delete;
    bool operator<(Userspace const&) const = delete;
    bool operator>(Userspace const&) const = delete;

    bool operator==(nullptr_t) const { return m_ptr == 0; }

#ifdef KERNEL
    explicit Userspace(FlatPtr ptr)
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

    CRef operator[](size_t i) const
    requires(allow_deref)
    {
        return m_ptr[i];
    }
    Ref operator[](size_t i)
    requires(allow_deref)
    {
        return m_ptr[i];
    }
    CRef operator*() const
    requires(allow_deref)
    {
        return *m_ptr;
    }
    Ref operator*()
    requires(allow_deref)
    {
        return *m_ptr;
    }
    CRef operator->() const
    requires(allow_deref)
    {
        return *m_ptr;
    }
    Ref operator->()
    requires(allow_deref)
    {
        return *m_ptr;
    }

#endif

    operator Userspace<ElementType const*>() const
    {
        return Userspace<ElementType const*> { m_ptr };
    }

private:
#ifdef KERNEL
    FlatPtr m_ptr { 0 };
#else
    T m_ptr { nullptr };
#endif
};

template<typename T, typename U>
requires(requires { static_cast<T>(declval<U>()); })
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
