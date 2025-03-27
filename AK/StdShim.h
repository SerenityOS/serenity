/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Note: std::forward and move are often special cased in compilers and static analyzers
//       so we should always, if possible, use the std:: versions
//       and not introduce a fake version in a look-alike namespace, aka AK::replaced_std

#ifdef KERNEL

#    include <AK/StdLibExtraDetails.h>

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

}

#else
// Note: Try to include as little STL as possible
#    if __has_include(<bits/move.h>)
//       GLIBC LIBSTDC++
#        include <bits/move.h>
#    elif __has_include(<__utility/forward.h>)
//       LLVM LIBCXX
#        include <__utility/forward.h>
#        include <__utility/move.h>
#    else
// FIXME: Find a smaller header to include in these cases
#        include <utility>
#    endif
#endif

namespace AK {
using std::forward;
using std::move;
}
