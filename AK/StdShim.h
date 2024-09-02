/*
 * Copyright (c) 2025, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Note:
// `construct_at` is required to come from the std namespace to be constexpr in C++20
// so we cannot put it in an AK::replaced_std and still have it work in constexpr contexts.
// `move` and `forward` are visible from the construct_at headers, so the similar applies to them.
// They are also often special cased in compilers and static analyzers,
// which makes it advantageous to use a `std::` sourced version
// So for non-Kernel code we will use the STL versions of these functions
// and for Kernel code we can provide our own versions.
// FIXME:
// Ideally we'd poison the std namespace after exporting what we need, but that would
// break any subsequent includes of STL headers, including <new> which is required by kmalloc.h

#ifdef KERNEL

#    include <AK/Assertions.h>
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
template<typename T, typename... Args, class = decltype(::new(declval<void*>()) T(declval<Args>()...))>
constexpr T* construct_at(T* location, Args&&... args) noexcept
{
    VERIFY(location);
    return ::new (static_cast<void*>(location)) T(forward<Args>(args)...);
}

template<size_t I, class T>
struct tuple_element;

template<size_t I, class T>
struct tuple_element<I, T volatile> {
    using type = typename tuple_element<I, T>::type volatile;
};

template<size_t I, class T>
struct tuple_element<I, T const volatile> {
    using type = typename tuple_element<I, T>::type const volatile;
};

template<size_t I, class T>
struct tuple_element<I, T const> {
    using type = typename tuple_element<I, T>::type const;
};

template<class T>
struct tuple_size;

template<class T, class...>
using tuple_size_implementation = T;

template<class T>
requires(!IsVolatile<T>)
struct tuple_size<tuple_size_implementation<T const, decltype(tuple_size<T>::value)>> : public AK::Detail::IntegralConstant<size_t, tuple_size<T>::value> { };

template<class T>
requires(!IsConst<T>)
struct tuple_size<tuple_size_implementation<T volatile, decltype(tuple_size<T>::value)>> : public AK::Detail::IntegralConstant<size_t, tuple_size<T>::value> { };

template<class T>
struct tuple_size<tuple_size_implementation<T const volatile, decltype(tuple_size<T>::value)>> : public AK::Detail::IntegralConstant<size_t, tuple_size<T>::value> { };

}

#else
// Note: Try to include as little STL as possible
#    if __has_include(<bits/move.h>) && __has_include(<bits/stl_construct.h>)
//       GLIBC LIBSTDC++
#        include <bits/move.h>
#        include <bits/stl_construct.h>
#        include <bits/utility.h> // tuple_{element,size}
#    elif __has_include(<__utility/forward.h>) && __has_include(<__memory/construct_at.h>)
//       LLVM LIBCXX
#        include <__memory/construct_at.h>
#        include <__tuple/tuple_element.h>
#        include <__tuple/tuple_size.h>
#        include <__utility/forward.h>
#        include <__utility/move.h>
#    else
// FIXME: Find a smaller header to include in these cases
#        warning "Using <utility>, <memory> and <tuple> for move, forward, construct_at and tuple_{element,size}"
#        include <memory>
#        include <tuple>
#        include <utility>
#    endif

#endif // KERNEL

namespace AK {
using std::construct_at;
using std::forward;
using std::move;
using std::tuple_element;
using std::tuple_size;
}
