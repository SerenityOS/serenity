/*
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Because this interacts with ErrorOr and includes Error.h, this cannot go into TypedTransfer.h.

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/Platform.h>

namespace AK {

// TODO:
// - return decltype of clone() in FallibleCopyHelper
// - Allow passing template arguments to FallibleCopyHelper
// - Check that it works for clone-only types

template<typename T>
concept HasFallibleClone = requires(T&& t) {
                               t.clone().is_error();
                               t.clone().release_error();
                               t.clone().release_value();
                           };

template<typename Cloner, typename T>
concept IsCloner = VoidFunction<Cloner, T const&> || FallibleFunction<Cloner, T const&>;

template<typename T>
class CloneCaller;

template<typename T>
requires(!HasFallibleClone<T>)
class CloneCaller<T> {
public:
    void operator()(T const&)
    {
        // void as a return type indicates that this function shall not be called,
        // and instead the copy constructor shall be used.
        VERIFY_NOT_REACHED();
    }
};

template<typename T>
requires(HasFallibleClone<T>)
class CloneCaller<T> {
public:
    auto operator()(T const& source)
    {
        return source.clone();
    }
};

template<typename T, typename NewT, typename Cloner>
// Cannot use IsCloner here because C++ template specialization is not clever enough for that.
requires(VoidFunction<Cloner, T const&> || FallibleFunction<Cloner, T const&>)
class FallibleCopyHelper;

template<typename T, typename NewT, typename Cloner>
requires(VoidFunction<Cloner, T const&>)
class FallibleCopyHelper<T, NewT, Cloner> {
public:
    FallibleCopyHelper(Cloner)
    {
    }

    void operator()(NewT* destination, T const* source)
    {
        new (destination) NewT(*source);
    }
};

template<typename T, typename NewT, typename Cloner>
requires(FallibleFunction<Cloner, T const&>)
class FallibleCopyHelper<T, NewT, Cloner> {
public:
    FallibleCopyHelper(Cloner cloner)
        : m_cloner(move(cloner))
    {
    }

    ErrorOr<void> operator()(NewT* destination, T const* source)
    {
        new (destination) NewT(TRY(m_cloner(*source)));
        return {};
    }

private:
    Cloner m_cloner;
};

// ======

/*

template<typename OldType, typename NewType = OldType>
class InfallibleCopyConstruct {
public:
    void operator()(NewType* destination, OldType const* source)
    {
        new (destination) NewType(*source);
    }
};

template<typename OldType, typename NewType = OldType, typename... CloneArgs>
class FallibleClone {
public:
    ErrorOr<void> operator()(NewType* destination, OldType const* source, CloneArgs... clone_args)
    {
        new (destination) NewType(TRY(source->clone(clone_args...)));
        return {};
    }
};

// We would like to use CopyMethod::ImplicitInfallible() and CopyMethod::FallibleClone() as parameter,
// but might also want to expose these only when `USING_AK_GLOBALLY` is set.
// This can only be achieved with classes, not namespaces.
class CopyMethod {
public:
    template<typename OldType, typename NewType = OldType>
    using InfallibleCopyConstruct = InfallibleCopyConstruct<NewType, OldType>;
    template<typename OldType, typename NewType = OldType, typename... CloneArgs>
    using FallibleClone = FallibleClone<NewType, OldType, CloneArgs...>;

    // template<typename T, typename TraitsForT = Traits<T>, bool IsOrdered = false>
    // class HashTable;
    // template<typename K, typename V, typename KeyTraits = Traits<K>, typename ValueTraits = Traits<V>, bool IsOrdered = false>
    // class HashMap;
    // template<typename T, size_t inline_capacity = 0>
    // requires(!IsRvalueReference<T>) class Vector;
};

*/

}

#if USING_AK_GLOBALLY
// using AK::CopyMethod;
using AK::HasFallibleClone;
#endif
