/*
 * Copyright (c) 2020, the SerenityOS developers.
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

namespace AK {

template<typename... Types>
struct TypeList;

template<unsigned Index, typename List>
struct TypeListElement;

template<unsigned Index, typename Head, typename... Tail>
struct TypeListElement<Index, TypeList<Head, Tail...>>
    : TypeListElement<Index - 1, TypeList<Tail...>> {
};

template<typename Head, typename... Tail>
struct TypeListElement<0, TypeList<Head, Tail...>> {
    using Type = Head;
};

template<typename... Types>
struct TypeList {
    static constexpr unsigned size = sizeof...(Types);

    template<unsigned N>
    using Type = typename TypeListElement<N, TypeList<Types...>>::Type;
};

template<typename T>
struct TypeWrapper {
    using Type = T;
};

template<typename List, typename F, unsigned... Indexes>
constexpr void for_each_type_impl(F&& f, IndexSequence<Indexes...>)
{
    (forward<F>(f)(TypeWrapper<typename List::template Type<Indexes>> {}), ...);
}

template<typename List, typename F>
constexpr void for_each_type(F&& f)
{
    for_each_type_impl<List>(forward<F>(f), MakeIndexSequence<List::size> {});
}

template<typename ListA, typename ListB, typename F, unsigned... Indexes>
constexpr void for_each_type_zipped_impl(F&& f, IndexSequence<Indexes...>)
{
    (forward<F>(f)(TypeWrapper<typename ListA::template Type<Indexes>> {}, TypeWrapper<typename ListB::template Type<Indexes>> {}), ...);
}

template<typename ListA, typename ListB, typename F>
constexpr void for_each_type_zipped(F&& f)
{
    static_assert(ListA::size == ListB::size, "Can't zip TypeLists that aren't the same size!");
    for_each_type_zipped_impl<ListA, ListB>(forward<F>(f), MakeIndexSequence<ListA::size> {});
}

}

using AK::for_each_type;
using AK::for_each_type_zipped;
using AK::TypeList;
using AK::TypeListElement;
using AK::TypeWrapper;
