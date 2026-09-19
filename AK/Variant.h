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
// to pass along the desired depth/type into the constructor chain.
// This is done as we can't easily start the lifetime of the alternatives
// after the union was instantiated.
// Upcoming c++26 features should fix this
// (see trivial unions (P3074R7) and std::start_lifetime (P3726R2))
// For move and copy assignment we destroy the active member,
// and then replace the whole storage,
// trivial unions may also make this nicer
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
    using Rest = VariantStorage<CurrentIndex + 1, Ts...>;

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
        : rest(VariantIndex<Idx> { }, forward<Us>(args)...)
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
            return forward_like<Self>(self.value);
        } else {
            return forward_like<Self>(self.rest).template get<I>();
        }
    }

    // Note: STL impls seem to wrap this into its own struct, which just forwards the constructor
    //       not sure why though
    F value;
    Rest rest;
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
        return forward_like<Self>(self.value);
    }

    F value;
};

struct VariantHelper {
    template<size_t TargetIndex, typename Variant, typename... Us>
    static constexpr void construct(Variant& variant, Us&&... args)
    {
        construct_at<Variant>(&variant, VariantIndex<TargetIndex> { }, forward<Us>(args)...);
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
    template<typename T, size_t I, typename Fn>
    static constexpr bool has_explicitly_named_overload()
    {
        // If we're not allowed to make a member function pointer and call it directly (without explicitly resolving it),
        // we have a templated function on our hands (or a function overload set).
        // in such cases, we don't have an explicitly named overload, and we would have to select it.
        return requires { (declval<Fn>().*(&Fn::operator()))(declval<T>()); };
    }

    template<typename T, typename Visitor, auto... Is>
    static constexpr bool should_invoke_const_overload(IndexSequence<Is...>)
    {
        // Scan over all the different visitor functions, if none of them are suitable for calling with `T const&`, avoid calling that first.
        return ((has_explicitly_named_overload<T, Is, typename Visitor::Types::template Type<Is>>()) || ...);
    }

    template<typename Self, typename Visitor, IndexType CurrentIndex = 0>
    ALWAYS_INLINE static constexpr decltype(auto) visit(Self&& self, Visitor&& visitor)
    requires(CurrentIndex < sizeof...(Ts))
    {
        using T = typename TypeList<Ts...>::template Type<CurrentIndex>;

        if (self.index() == CurrentIndex) {
            // Check if any Visitor::operator() is an explicitly typed function (as opposed to a templated function)
            if constexpr (IsRvalueReference<Self&&> && should_invoke_const_overload<T&&, Visitor>(MakeIndexSequence<Visitor::Types::size>()))
                return visitor(forward_like<Self>(self.m_data).template get<CurrentIndex>());
            // if so, try to call that with `T const&` first before copying the Variant's const-ness.
            // This emulates normal C++ call semantics where templated functions are considered last, after all non-templated overloads
            // are checked and found to be unusable.
            // FIXME: This now prefers the const& overload, even if a non const one is present
            //        just calling `get` without any specifiers is close, but prefers `auto&` to `T const&`
            else if constexpr (should_invoke_const_overload<T, Visitor>(MakeIndexSequence<Visitor::Types::size>()))
                return visitor(static_cast<T const&>(forward_like<Self>(self.m_data).template get<CurrentIndex>()));
            else
                return visitor(forward_like<Self>(self.m_data).template get<CurrentIndex>());
        }

        if constexpr ((CurrentIndex + 1) < sizeof...(Ts))
            return visit<Self, Visitor, CurrentIndex + 1>(forward<Self>(self), forward<Visitor>(visitor));
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

template<size_t I, typename T>
struct Overload {
    using Type = T;
    static constexpr size_t Index = I;
    // This Overload for <T> can be chosen, if the passed fully qualified type <U&&>, can be used to construct a <T>.
    // The compiler will choose the best overload from the overload set (AllOverloads<...>), in case of ambiguity.
    // NOTE: We always allow narrowing conversions here,
    //       the STL does not allow narrowing conversions for variant constructors.
    //       A point of allowing those is allowing instantiations with literal 0s.
    template<typename U, typename = T>
    requires(IsConstructible<T, U>)
    static auto operator()(T, U&&) -> Overload;
};

template<typename Is, typename... Ts>
struct AllOverloads;

template<size_t... Indices, typename... Ts>
struct AllOverloads<IndexSequence<Indices...>, Ts...> : public Overload<Indices, Ts>... {
    using Overload<Indices, Ts>::operator()...;
};

template<typename... Types>
using MakeOverloads = AllOverloads<MakeIndexSequence<sizeof...(Types)>, Types...>;

template<typename T>
concept NotLvalueReference = !IsLvalueReference<T>;

template<NotLvalueReference... Ts>
struct Variant {
    using OverloadFinder = Detail::MakeOverloads<Ts...>;
    template<typename T>
    using BestMatch = InvokeResult<OverloadFinder, T, T>;

public:
    using IndexType = Conditional<(sizeof...(Ts) < 255), u8, size_t>; // Note: size+1 reserved for internal value checks
private:
    static constexpr IndexType invalid_index = sizeof...(Ts);

    template<typename T>
    static constexpr IndexType index_of() { return Detail::index_of<T, IndexType, Ts...>(); }

    using Storage = Detail::VariantStorage<0, Ts...>;

public:
    template<typename T>
    static consteval bool can_contain() { return IsOneOf<T, Ts...>; }

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

    // While a Variant should not be able to contain itself,
    // this overload may still be chosen in place of copy or move construction,
    // if an element has a templated catch-all constructor.
    // So we need to exclude Variant itself to prevent recursion.
    template<typename T>
    requires(!IsSame<RemoveCVReference<T>, Variant>
        && (IsConstructible<Ts, T> || ...))
    constexpr Variant(T&& t)
    {
        using BestOverload = BestMatch<T>;

        constexpr IndexType BestOverloadIndex = BestOverload::Index;
        m_index = BestOverloadIndex;
        // FIXME: Is this replacement over the trivial empty union/dummy initialized union
        //        free, and should we try to do it directly
        Helper::template construct<BestOverloadIndex>(m_data, forward<T>(t));
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
        : m_data { }
        , m_index(old.m_index)
    {
        Helper::copy_to(old.m_data, old.m_index, m_data);
    }

    // Note: A moved-from variant emulates the state of the object it contains
    //       so if a variant containing an int is moved from, it will still contain that int
    //       and if a variant with a nontrivial move ctor is moved from, it may or may not be valid
    //       but it will still contain the "moved-from" state of the object it previously contained.
    ALWAYS_INLINE constexpr Variant(Variant&& old)
    requires(!(IsTriviallyMoveConstructible<Ts> && ...))
        : m_index(old.m_index)
    {
        Helper::move_to(old.m_data, old.m_index, m_data);
    }

    ALWAYS_INLINE constexpr ~Variant()
    requires(!(IsTriviallyDestructible<Ts> && ...))
    {
        Helper::delete_(m_data, m_index);
    }

    ALWAYS_INLINE constexpr Variant& operator=(Variant const& other)
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

    template<typename T>
    requires(!IsSame<RemoveCVReference<T>, Variant>
        && (IsConstructible<Ts, T> || ...))
    ALWAYS_INLINE constexpr Variant& operator=(T&& value)
    {
        // FIXME: Use move/copy assign if appropriate
        if constexpr (!(IsTriviallyDestructible<Ts> && ...)) {
            Helper::delete_(m_data, m_index);
        }
        using BestOverload = BestMatch<T>;

        constexpr IndexType BestOverloadIndex = BestOverload::Index;
        m_index = BestOverloadIndex;

        Helper::construct<BestOverloadIndex>(m_data, forward<T>(value));

        return *this;
    }

    ALWAYS_INLINE constexpr Variant& operator=(Variant&& other)
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
        m_index = new_index;
        Helper::template construct<new_index>(m_data, forward<T>(t));
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    constexpr void set(T&& t, Detail::VariantNoClearTag)
    requires(can_contain<StrippedT>() && requires { StrippedT(forward<T>(t)); })
    {
        constexpr auto new_index = index_of<StrippedT>();
        m_index = new_index;
        Helper::template construct<new_index>(m_data, forward<T>(t));
    }

    template<typename T, typename Self>
    constexpr ApplyConstFrom<Self, T>* get_pointer(this Self& self)
    requires(can_contain<T>())
    {
        constexpr IndexType I = index_of<T>();
        if (I == self.m_index)
            return &self.m_data.template get<I>();
        return nullptr;
    }

    template<typename T, typename Self>
    constexpr LikeT<Self, T> get(this Self&& self)
    requires(can_contain<T>())
    {
        VERIFY(self.template has<T>());
        constexpr IndexType I = index_of<T>();
        return forward_like<Self>(self.m_data).template get<I>();
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

    template<typename... Fs, typename Self>
    ALWAYS_INLINE constexpr decltype(auto) visit(this Self&& self, Fs&&... functions)
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(forward<Self>(self), move(visitor));
    }

    template<typename... NewTs, typename Self>
    ALWAYS_INLINE constexpr decltype(auto) downcast(this Self&& self)
    {
        if constexpr (sizeof...(NewTs) == 1 && (IsSpecializationOf<NewTs, Variant> && ...)) {
            return forward<Self>(self).downcast_variant(TypeWrapper<NewTs...> { });
        } else {
            return forward<Self>(self).visit([](auto&& value) -> Variant<NewTs...> {
                if constexpr (Variant<NewTs...>::template can_contain<RemoveCVReference<decltype(value)>>())
                    return Variant<NewTs...>(forward_like<Self>(value));
                else
                    VERIFY_NOT_REACHED();
            });
        }
    }

    constexpr auto index() const { return m_index; }

private:
    friend struct Detail::VisitImpl<IndexType, Ts...>;

    template<typename... NewTs, typename Self>
    constexpr Variant<NewTs...> downcast_variant(this Self&& self, TypeWrapper<Variant<NewTs...>>)
    {
        return forward<Self>(self).template downcast<NewTs...>();
    }

    using Helper = Detail::VariantHelper;
    using VisitHelper = Detail::VisitImpl<IndexType, Ts...>;

    explicit Variant(IndexType index, Detail::VariantConstructTag)
        : m_index(index)
    {
    }

    template<typename... Fs>
    struct Visitor : Fs... {
        using Types = TypeList<Fs...>;

        constexpr Visitor(Fs&&... args)
            : Fs(forward<Fs>(args))...
        {
        }

        using Fs::operator()...;
    };

    Storage m_data;
    IndexType m_index;
};

}

namespace AK {

template<typename... Ts>
using Variant = DedupAndApply<Detail::Variant, Ts...>::Type;

template<typename... Ts>
struct TypeList<Detail::Variant<Ts...>> : TypeList<Ts...> { };

}

#if USING_AK_GLOBALLY
using AK::Variant;
#endif
