/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Note:
// These functions we might need to implement to get some stuff working in constexpr contexts,
// Or because they'd are visible in the standard library and we need to provide them,
// So we need to include the official implementations and poison the rest of std in lagom builds
// for serenity builds we are the standard lib so we can implement them ourselves
// ALSO NOTE for move/forward: These have to be in the "std" namespace since some compilers and static analyzers rely on it.

// Note: Named in a way that will sort this after other headers that might need to include stl headers

#ifdef KERNEL
#    include <AK/StdLibExtraDetails.h>
#    include <Kernel/Heap/kmalloc.h>

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
    return ::new ((void*)location) T(forward<Args>(args)...);
}

}

#else
// Note: Try to include as little STL as possible
#    if __has_include(<bits/stl_construct.h>)
//       GLIBC LIBSTDC++
#        include <bits/stl_construct.h>
#    elif __has_include(<__memory/construct_at.h>)
//       LLVM LIBCXX
#        include <__memory/construct_at.h>
#    else
// FIXME: Find a smaller header to include in these cases
#        include <memory>
#    endif

#endif

namespace AK {
using std::construct_at;
using std::forward;
using std::move;
}

// FIXME: Poison STD
//        kmalloc.h includes a few system headers as well, which would break when poisoning std here
