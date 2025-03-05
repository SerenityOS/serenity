/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Diagnostics.h>
#include <AK/StdLibExtraDetails.h>

// Note:
// These functions we might need to implement to get some stuff working in constexpr contexts,
// Or because they'd are visible in the standard library and we need to provide them,
// So we need to include the official implementations and poison the rest of std in lagom builds
// for serenity builds we are the standard lib so we can implement them ourselves
// ALSO NOTE for move/forward: These have to be in the "std" namespace since some compilers and static analyzers rely on it.

// Note: Named in a way that will sort this after other headers that might need to include stl headers

#ifdef AK_OS_SERENITY

#    ifdef KERNEL
#        include <Kernel/Heap/kmalloc.h>
#    else
#        include <new>
#    endif

namespace std { // NOLINT(cert-dcl58-cpp) We are the standard library

template<typename T>
constexpr T&& forward(AK::Detail::RemoveReference<T>& param)
{
    return static_cast<T&&>(param);
}

template<typename T>
constexpr T&& forward(AK::Detail::RemoveReference<T>&& param) noexcept
{
    static_assert(!AK::Detail::IsLvalueReference<T>, "Can't forward an rvalue as an lvalue.");
    return static_cast<T&&>(param);
}

template<typename T>
constexpr T&& move(T& arg)
{
    return static_cast<T&&>(arg);
}

// `new` will be constexpr in C++26, until then we need this
template<typename T, typename... Args>
constexpr T* construct_at(T* location, Args&&... args) noexcept
{
    // FIXME: GCC may complain here about casting away const....
    AK_IGNORE_DIAGNOSTIC("-Wcast-qual", return ::new ((void*)location) T(forward<Args>(args)...));
}

}

#else
// Note: Try to include as little STL as possible
#    if __has_include(<bits/stl_construct.h>)
#        include <bits/stl_construct.h>
#    else
#        include <memory>
#    endif

#endif

namespace AK {
using std::construct_at;
using std::forward;
using std::move;
}

// FIXME: Poison STD
//        kmalloc.h includes a few system headers as well, which would break when poisoning std
