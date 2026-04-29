/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/Concepts.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/StdLibExtras.h>
#include <AK/StdShim.h>
#include <AK/TypeList.h>

namespace AK::Detail {

template<typename T, typename IndexType, typename... Ts>
consteval IndexType index_of()
{
    bool matches[] = { IsSame<T, Ts>... };
    for (size_t i = 0; i < sizeof...(Ts); ++i) {
        if (matches[i])
            return static_cast<IndexType>(i);
    }
    return static_cast<IndexType>(sizeof...(Ts));
}

// How this works in constexpr:
// For construction we use a sentinel value (`VariantIndex`)
// to pass along the desired depth/Type into the constructor chain
// This is done as we cant easily start the lifetime of the alternatives
// after the union was instantiated
// Future c++ features should fix this
// (see trivial unions (c++26) and std::start_lifetime (c++29))
// For move and copy assignment we destroy the active member,
// and then replace the whole storage
template<size_t Index>
struct VariantIndex {
    static constexpr size_t Value = Index;
};

template<size_t CurrentIndex, typename... Ts>
union VariantStorage;

template<size_t CurrentIndex, typename F, typename... Ts>
union VariantStorage<CurrentIndex, F, Ts...> {
    using ElementType = F;
    static constexpr size_t Index = CurrentIndex;
    using __NEXT = VariantStorage<CurrentIndex + 1, Ts...>;

    constexpr VariantStorage() { }

    template<size_t Idx, typename... Us>
    requires(Idx == CurrentIndex)
    constexpr VariantStorage(VariantIndex<Idx>, Us&&... args)
        : value(forward<Us>(args)...)
    {
    }

    template<size_t Idx, typename... Us>
    requires(Idx > CurrentIndex)
    constexpr VariantStorage(VariantIndex<Idx>, Us&&... args)
        : rest(VariantIndex<Idx> {}, forward<Us>(args)...)
    {
    }

    constexpr VariantStorage(VariantStorage const&) = default;
    constexpr VariantStorage(VariantStorage const&)
    requires(!IsTriviallyCopyConstructible<F> || (!IsTriviallyCopyConstructible<Ts> || ...))
    {
    }

    constexpr VariantStorage(VariantStorage&&) = default;
    constexpr VariantStorage(VariantStorage&&)
    requires(!IsTriviallyMoveConstructible<F> || (!IsTriviallyMoveConstructible<Ts> || ...))
    {
    }
    constexpr VariantStorage& operator=(VariantStorage const&) = default;
    constexpr VariantStorage& operator=(VariantStorage const&)
    requires(!IsTriviallyCopyAssignable<F> || (!IsTriviallyCopyAssignable<Ts> || ...))
    {
    }

    constexpr VariantStorage& operator=(VariantStorage&&) = default;
    constexpr VariantStorage& operator=(VariantStorage&&)
    requires(!IsTriviallyMoveAssignable<F> || (!IsTriviallyMoveAssignable<Ts> || ...))
    {
    }

    constexpr ~VariantStorage() = default;
    constexpr ~VariantStorage()
    requires(!IsTriviallyDestructible<F> || (!IsTriviallyDestructible<Ts> || ...))
    {
    }

    template<size_t I, typename Self>
    constexpr auto&& get(this Self&& self)
    {
        if constexpr (I == CurrentIndex) {
            return forward<Self>(self).value;
        } else {
            return forward<Self>(self).rest.template get<I>();
        }
    }

    char __dummy;
    // Note: STL impls seem to wrap this into its own struct, which just forwards the constructor
    //       not sure why though
    F value;
    __NEXT rest;
};

template<size_t CurrentIndex, typename F>
union VariantStorage<CurrentIndex, F> {
    using ElementType = F;
    static constexpr size_t Index = CurrentIndex;

    constexpr VariantStorage() { }

    template<size_t Idx, typename... Us>
    requires(Idx == CurrentIndex)
    constexpr VariantStorage(VariantIndex<Idx>, Us&&... args)
        : value(forward<Us>(args)...)
    {
    }
    // Note: Non default/trivial versions of the copy/move
    //       constructors/assignment operators should never be called,
    //       as those are handled by the Variant propper
    constexpr VariantStorage(VariantStorage const&) = default;
    constexpr VariantStorage(VariantStorage const&)
    requires(!IsTriviallyCopyConstructible<F>)
    {
        VERIFY_NOT_REACHED();
    }

    constexpr VariantStorage(VariantStorage&&) = default;
    constexpr VariantStorage(VariantStorage&&)
    requires(!IsTriviallyMoveConstructible<F>)
    {
        VERIFY_NOT_REACHED();
    }
    constexpr VariantStorage& operator=(VariantStorage const&) = default;
    constexpr VariantStorage& operator=(VariantStorage const&)
    requires(!IsTriviallyCopyAssignable<F>)
    {
        VERIFY_NOT_REACHED();
    }

    constexpr VariantStorage& operator=(VariantStorage&&) = default;
    constexpr VariantStorage& operator=(VariantStorage&&)
    requires(!IsTriviallyMoveAssignable<F>)
    {
        VERIFY_NOT_REACHED();
    }

    constexpr ~VariantStorage() = default;
    constexpr ~VariantStorage()
    requires(!IsTriviallyDestructible<F>)
    {
    }

    template<size_t I, typename Self>
    constexpr auto&& get(this Self&& self)
    {
        static_assert(I == CurrentIndex);
        return forward<Self>(self).value;
    }

    char __dummy;
    F value;
};

struct VariantHelper {
    template<size_t TargetIndex, typename Variant, typename... Us>
    static constexpr void construct(Variant& variant, Us&&... args)
    {
        new (&variant) Variant(VariantIndex<TargetIndex> {}, forward<Us>(args)...);
    }

    template<typename Variant>
    static constexpr void delete_(Variant& variant, size_t id)
    {
        if (id == Variant::Index) {
            using F = Variant::ElementType;
            variant.value.~F();
        } else if constexpr (requires { variant.rest; }) {
            delete_(variant.rest, id);
        } else {
            __builtin_unreachable();
        }
    }

    template<typename From, typename To>
    static constexpr void move_to(From& from, size_t id, To& to)
    {
        if (id == From::Index) {
            construct<From::Index>(to, move(from.value));
        } else if constexpr (requires { from.rest; }) {
            move_to(from.rest, id, to);
        } else {
            __builtin_unreachable();
        }
    }

    template<typename From, typename To>
    static constexpr void copy_to(From const& from, size_t id, To& to)
    {
        if (id == From::Index) {
            construct<From::Index>(to, from.value);
        } else if constexpr (requires { from.rest; }) {
            copy_to(from.rest, id, to);
        } else {
            __builtin_unreachable();
        }
    }
};

template<typename IndexType, typename... Ts>
struct VisitImpl {
    template<typename RT, typename T, size_t I, typename Fn>
    static constexpr bool has_explicitly_named_overload()
    {
        // If we're not allowed to make a member function pointer and call it directly (without explicitly resolving it),
        // we have a templated function on our hands (or a function overload set).
        // in such cases, we don't have an explicitly named overload, and we would have to select it.
        return requires { (declval<Fn>().*(&Fn::operator()))(declval<T>()); };
    }

    template<typename ReturnType, typename T, typename Visitor, auto... Is>
    static constexpr bool should_invoke_const_overload(IndexSequence<Is...>)
    {
        // Scan over all the different visitor functions, if none of them are suitable for calling with `T const&`, avoid calling that first.
        return ((has_explicitly_named_overload<ReturnType, T, Is, typename Visitor::Types::template Type<Is>>()) || ...);
    }

    template<typename Self, typename Visitor, IndexType CurrentIndex = 0>
    ALWAYS_INLINE static constexpr decltype(auto) visit(Self& self, Visitor&& visitor)
    requires(CurrentIndex < sizeof...(Ts))
    {
        using T = typename TypeList<Ts...>::template Type<CurrentIndex>;

        if (self.index() == CurrentIndex) {
            // Check if Visitor::operator() is an explicitly typed function (as opposed to a templated function)
            // if so, try to call that with `T const&` first before copying the Variant's const-ness.
            // This emulates normal C++ call semantics where templated functions are considered last, after all non-templated overloads
            // are checked and found to be unusable.
            using ReturnType = decltype(visitor(declval<T&>()));
            if constexpr (should_invoke_const_overload<ReturnType, T, Visitor>(MakeIndexSequence<Visitor::Types::size>()))
                return visitor(AddConstToReferencedType<Self&>(self).template get<T>());

            return visitor(self.template get<T>());
        }

        if constexpr ((CurrentIndex + 1) < sizeof...(Ts))
            return visit<Self, Visitor, CurrentIndex + 1>(self, forward<Visitor>(visitor));
        else
            VERIFY_NOT_REACHED();
    }
};

struct VariantNoClearTag {
    explicit VariantNoClearTag() = default;
};
struct VariantConstructTag {
    explicit VariantConstructTag() = default;
};

// Type list deduplication
// Since this is a big template mess, each template is commented with how and why it works.
struct ParameterPackTag {
};

// Pack<Ts...> is just a way to pass around the type parameter pack Ts
template<typename... Ts>
struct ParameterPack : ParameterPackTag {
};

// Blank<T> is a unique replacement for T, if T is a duplicate type.
template<typename T>
struct Blank {
    void operator()() const;
};

template<typename A, typename P>
inline constexpr bool IsTypeInPack = false;

// IsTypeInPack<T, Pack<Ts...>> will just return whether 'T' exists in 'Ts'.
template<typename T, typename... Ts>
inline constexpr bool IsTypeInPack<T, ParameterPack<Ts...>> = (IsSame<T, Ts> || ...);

// Replaces T with Blank<T> if it exists in Qs.
template<typename T, typename... Qs>
using BlankIfDuplicate = Conditional<(IsTypeInPack<T, Qs> || ...), Blank<T>, T>;

template<size_t I, typename...>
struct InheritFromUniqueEntries;

// InheritFromUniqueEntries will inherit from both Qs and Ts, but only scan entries going *forwards*
// that is to say, if it's scanning from index I in Qs, it won't scan for duplicates for entries before I
// as that has already been checked before.
// This makes sure that the search is linear in time (like the 'merge' step of merge sort).
template<size_t I, typename... Ts, size_t... Js, typename... Qs>
struct InheritFromUniqueEntries<I, ParameterPack<Ts...>, IndexSequence<Js...>, Qs...>
    : public BlankIfDuplicate<Ts, Conditional<Js <= I, ParameterPack<>, Qs>...>... {

    using BlankIfDuplicate<Ts, Conditional<Js <= I, ParameterPack<>, Qs>...>::BlankIfDuplicate...;
    using BlankIfDuplicate<Ts, Conditional<Js <= I, ParameterPack<>, Qs>...>::operator()...;
};

template<typename...>
struct InheritFromPacks;

// InheritFromPacks will attempt to 'merge' the pack 'Ps' with *itself*, but skip the duplicate entries
// (via InheritFromUniqueEntries).
template<size_t... Is, typename... Ps>
struct InheritFromPacks<IndexSequence<Is...>, Ps...>
    : public InheritFromUniqueEntries<Is, Ps, IndexSequence<Is...>, Ps...>... {

    using InheritFromUniqueEntries<Is, Ps, IndexSequence<Is...>, Ps...>::InheritFromUniqueEntries...;
    using InheritFromUniqueEntries<Is, Ps, IndexSequence<Is...>, Ps...>::operator()...;
};

// Just a nice wrapper around InheritFromPacks, which will wrap any parameter packs in ParameterPack (unless it already is one).
template<typename... Ps>
using MergeAndDeduplicatePacks = InheritFromPacks<MakeIndexSequence<sizeof...(Ps)>, Conditional<IsBaseOf<ParameterPackTag, Ps>, Ps, ParameterPack<Ps>>...>;

// NOTE: This always allows allows narrowing,
//       The stl version does not allow narrowing conversions
//       main points where we need it are instantiations with literal 0s,
//       which we could possibly check for with a some more template magic and is_constant_p.
template<typename T>
struct Overload {
    // This Overload for <T> can be chosen,
    // if the passed type <U>, in its fully qualified form*, can construct a <T>
    // The compiler will then choose the "ideal" overload, if it is unambiguous
    // *: This is the reason for the forwarding reference in the arguments
    template<typename U, typename = T>
    requires(IsConstructible<T, U>)
    auto operator()(T, U&&) const -> __IdentityType<T>;
};

template<typename... Bases>
struct AllOverloads : MergeAndDeduplicatePacks<ParameterPack<Bases>...> {
    void operator()() const;
    using MergeAndDeduplicatePacks<ParameterPack<Bases>...>::operator();
};

template<typename IndexSequence>
struct MakeOverloadsImpl;

template<size_t... Indices>
struct MakeOverloadsImpl<IndexSequence<Indices...>> {
    template<typename... Types>
    using Apply = AllOverloads<Overload<Types>...>;
};

template<typename... Types>
using MakeOverloads = typename MakeOverloadsImpl<MakeIndexSequence<sizeof...(Types)>>::template Apply<Types...>;
}

namespace AK {

template<typename T>
concept NotLvalueReference = !IsLvalueReference<T>;

template<NotLvalueReference...>
struct Variant;

template<NotLvalueReference... Ts>
struct Variant {
    // FIXME: Can we get this to return the index as well?
    using OverloadFinder = Detail::MakeOverloads<Ts...>;
    template<typename T>
    using BestMatch = InvokeResult<OverloadFinder, T, T>::Type;

public:
    using IndexType = Conditional<(sizeof...(Ts) < 255), u8, size_t>; // Note: size+1 reserved for internal value checks
private:
    static constexpr IndexType invalid_index = sizeof...(Ts);

    template<typename T>
    static constexpr IndexType index_of() { return Detail::index_of<T, IndexType, Ts...>(); }

    using Storage = Detail::VariantStorage<0, Ts...>;

public:
    template<typename T>
    static constexpr bool can_contain() { return IsOneOf<T, Ts...>; }

    template<typename... NewTs>
    constexpr Variant(Variant<NewTs...>&& old)
    requires((can_contain<NewTs>() && ...))
        : Variant(move(old).template downcast<Ts...>())
    {
    }

    template<typename... NewTs>
    constexpr Variant(Variant<NewTs...> const& old)
    requires((can_contain<NewTs>() && ...))
        : Variant(old.template downcast<Ts...>())
    {
    }

    // FIXME: Not sure why we need the `!IsSame` constraint here to avoid recursion,
    //        The variant should not be able to contain it self, so a constructibility check should
    //        be enough?
    template<typename T>
    requires(!IsSame<RemoveCVReference<T>, Variant>
        && (IsConstructible<Ts, T> || ...))
    constexpr Variant(T&& t)
    {
        using BestOverload = BestMatch<T>;
        // FIXME: Can we get the index directly from the resolution?
        constexpr IndexType BestOverloadIndex = index_of<BestOverload>();
        // FIXME: Is this replacement over the trivial empty union/dummy initialized union
        //        free, and should we try to do it directly
        Helper::template construct<BestOverloadIndex>(m_data, forward<T>(t));
        m_index = BestOverloadIndex;
    }

    template<NotLvalueReference... NewTs>
    friend struct Variant;

    Variant()
    requires(!can_contain<Empty>())
    = delete;
    constexpr Variant()
    requires(can_contain<Empty>())
        : Variant(Empty())
    {
    }

    Variant(Variant const&)
    requires(!(IsCopyConstructible<Ts> && ...))
    = delete;
    constexpr Variant(Variant const&) = default;

    Variant(Variant&&)
    requires(!(IsMoveConstructible<Ts> && ...))
    = delete;
    constexpr Variant(Variant&&) = default;

    ~Variant()
    requires(!(IsDestructible<Ts> && ...))
    = delete;
    constexpr ~Variant() = default;

    Variant& operator=(Variant const&)
    requires(!(IsCopyConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...))
    = delete;
    constexpr Variant& operator=(Variant const&) = default;

    Variant& operator=(Variant&&)
    requires(!(IsMoveConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...))
    = delete;
    constexpr Variant& operator=(Variant&&) = default;

    constexpr Variant(Variant const& old)
    requires(!(IsTriviallyCopyConstructible<Ts> && ...))
        : m_data {}
        , m_index(old.m_index)
    {
        Helper::copy_to(old.m_data, old.m_index, m_data);
    }

    // Note: A moved-from variant emulates the state of the object it contains
    //       so if a variant containing an int is moved from, it will still contain that int
    //       and if a variant with a nontrivial move ctor is moved from, it may or may not be valid
    //       but it will still contain the "moved-from" state of the object it previously contained.
    constexpr Variant(Variant&& old)
    requires(!(IsTriviallyMoveConstructible<Ts> && ...))
        : m_index(old.m_index)
    {
        Helper::move_to(old.m_data, old.m_index, m_data);
    }

    constexpr ~Variant()
    requires(!(IsTriviallyDestructible<Ts> && ...))
    {
        Helper::delete_(m_data, m_index);
    }

    constexpr Variant& operator=(Variant const& other)
    requires(!(IsTriviallyCopyConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
    {
        if (this != &other) {
            if constexpr (!(IsTriviallyDestructible<Ts> && ...)) {
                Helper::delete_(m_data, m_index);
            }
            m_index = other.m_index;
            Helper::copy_to(other.m_data, other.m_index, m_data);
        }
        return *this;
    }

    constexpr Variant& operator=(Variant&& other)
    requires(!(IsTriviallyMoveConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
    {
        if (this != &other) {
            if constexpr (!(IsTriviallyDestructible<Ts> && ...)) {
                Helper::delete_(m_data, m_index);
            }
            m_index = other.m_index;
            Helper::move_to(other.m_data, other.m_index, m_data);
        }
        return *this;
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    constexpr void set(T&& t)
    requires(can_contain<StrippedT>() && requires { StrippedT(forward<T>(t)); })
    {
        constexpr auto new_index = index_of<StrippedT>();
        Helper::delete_(m_data, m_index);
        Helper::template construct<new_index>(m_data, forward<T>(t));
        m_index = new_index;
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    constexpr void set(T&& t, Detail::VariantNoClearTag)
    requires(can_contain<StrippedT>() && requires { StrippedT(forward<T>(t)); })
    {
        constexpr auto new_index = index_of<StrippedT>();
        Helper::template construct<new_index>(m_data, forward<T>(t));
        m_index = new_index;
    }

    template<typename T>
    constexpr T* get_pointer()
    requires(can_contain<T>())
    {
        constexpr IndexType I = index_of<T>();
        if (I == m_index)
            return &m_data.template get<I>();
        return nullptr;
    }

    template<typename T>
    constexpr T& get()
    requires(can_contain<T>())
    {
        VERIFY(has<T>());
        constexpr IndexType I = index_of<T>();
        return m_data.template get<I>();
    }

    template<typename T>
    constexpr T const* get_pointer() const
    requires(can_contain<T>())
    {
        constexpr IndexType I = index_of<T>();
        if (I == m_index)
            return &m_data.template get<I>();
        return nullptr;
    }

    template<typename T>
    constexpr T const& get() const
    requires(can_contain<T>())
    {
        VERIFY(has<T>());
        constexpr IndexType I = index_of<T>();
        return m_data.template get<I>();
    }

    template<typename T>
    [[nodiscard]] constexpr bool has() const
    requires(can_contain<T>())
    {
        return index_of<T>() == m_index;
    }

    constexpr bool operator==(Variant const& other) const
    {
        return this->visit([&]<typename T>(T const& self) {
            if (auto const* p = other.get_pointer<T>())
                return static_cast<T const&>(self) == static_cast<T const&>(*p);
            return false;
        });
    }

    template<typename... Fs>
    constexpr decltype(auto) visit(Fs&&... functions)
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(*this, move(visitor));
    }

    template<typename... Fs>
    constexpr decltype(auto) visit(Fs&&... functions) const
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(*this, move(visitor));
    }

    template<typename... NewTs>
    decltype(auto) downcast() &&
    {
        if constexpr (sizeof...(NewTs) == 1 && (IsSpecializationOf<NewTs, Variant> && ...)) {
            return move(*this).template downcast_variant<NewTs...>();
        } else {
            Variant<NewTs...> instance { Variant<NewTs...>::invalid_index, Detail::VariantConstructTag {} };
            visit([&](auto& value) {
                if constexpr (Variant<NewTs...>::template can_contain<RemoveCVReference<decltype(value)>>())
                    instance.set(move(value), Detail::VariantNoClearTag {});
            });
            VERIFY(instance.m_index != instance.invalid_index);
            return instance;
        }
    }

    template<typename... NewTs>
    decltype(auto) downcast() const&
    {
        if constexpr (sizeof...(NewTs) == 1 && (IsSpecializationOf<NewTs, Variant> && ...)) {
            return (*this).downcast_variant(TypeWrapper<NewTs...> {});
        } else {
            Variant<NewTs...> instance { Variant<NewTs...>::invalid_index, Detail::VariantConstructTag {} };
            visit([&](auto const& value) {
                if constexpr (Variant<NewTs...>::template can_contain<RemoveCVReference<decltype(value)>>())
                    instance.set(value, Detail::VariantNoClearTag {});
            });
            VERIFY(instance.m_index != instance.invalid_index);
            return instance;
        }
    }

    auto index() const { return m_index; }

private:
    template<typename... NewTs>
    Variant<NewTs...> downcast_variant(TypeWrapper<Variant<NewTs...>>) &&
    {
        return move(*this).template downcast<NewTs...>();
    }

    template<typename... NewTs>
    Variant<NewTs...> downcast_variant(TypeWrapper<Variant<NewTs...>>) const&
    {
        return (*this).template downcast<NewTs...>();
    }

    static constexpr auto data_size = Detail::integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, sizeof(Ts)...>()).max();
    static constexpr auto data_alignment = Detail::integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, alignof(Ts)...>()).max();
    using Helper = Detail::VariantHelper;
    using VisitHelper = Detail::VisitImpl<IndexType, Ts...>;

    explicit Variant(IndexType index, Detail::VariantConstructTag)
        : m_index(index)
    {
    }

    ALWAYS_INLINE void clear_without_destruction()
    {
        __builtin_memset(m_data, 0, data_size);
        m_index = invalid_index;
    }

    template<typename... Fs>
    struct Visitor : Fs... {
        using Types = TypeList<Fs...>;

        Visitor(Fs&&... args)
            : Fs(forward<Fs>(args))...
        {
        }

        using Fs::operator()...;
    };

    Storage m_data;
    IndexType m_index;
};

template<typename... Ts>
struct TypeList<Variant<Ts...>> : TypeList<Ts...> {};

}

#if USING_AK_GLOBALLY
using AK::Variant;
#endif
