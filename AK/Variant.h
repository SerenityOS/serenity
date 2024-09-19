/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/StdLibExtras.h>
#include <AK/TypeList.h>

namespace AK::Detail {

template<typename T, typename IndexType, IndexType InitialIndex, typename... InTypes>
struct VariantIndexOf {
    static_assert(DependentFalse<T, IndexType, InTypes...>, "Invalid VariantIndex instantiated");
};

template<typename T, typename IndexType, IndexType InitialIndex, typename InType, typename... RestOfInTypes>
struct VariantIndexOf<T, IndexType, InitialIndex, InType, RestOfInTypes...> {
    consteval IndexType operator()()
    {
        if constexpr (IsSame<T, InType>)
            return InitialIndex;
        else
            return VariantIndexOf<T, IndexType, InitialIndex + 1, RestOfInTypes...> {}();
    }
};

template<typename T, typename IndexType, IndexType InitialIndex>
struct VariantIndexOf<T, IndexType, InitialIndex> {
    consteval IndexType operator()() { return InitialIndex; }
};

template<typename T, typename IndexType, typename... Ts>
consteval IndexType index_of()
{
    return VariantIndexOf<T, IndexType, 0, Ts...> {}();
}

template<typename... T>
union VariantStorage;

template<typename T, typename... Rest>
union VariantStorage<T, Rest...> {
    // Note: This has to be a union, as type laundering via reinterpret_cast is
    //       not allowed in constexpr contexts, but changing the active member
    //       of a union is.
    // Note: On construction of this class:
    //       initially no member is active
    //       to set a member as active you should use `construct_at`
    //       or once c++26 is available, placement new,
    //       to abstract this away VariantHelper should be used
    // Note: The default copy/move constructors and assignment operators
    //       are to allow Variant to leverage them itself, iff the types allow that trivially
    //       otherwise they are deleted
    // Note: Proper destruction is handled by the Variant itself
    //       But all possible destructors need to be declared to make the compiler happy

    constexpr VariantStorage() = default;
    constexpr VariantStorage()
    requires(!IsTriviallyConstructible<T> || (!IsTriviallyConstructible<Rest> || ...)) {};

    constexpr VariantStorage(VariantStorage&&) = default;
    constexpr VariantStorage(VariantStorage&&)
    requires(!IsTriviallyMoveConstructible<T> || (!IsTriviallyMoveConstructible<Rest> || ...))
    = delete;
    constexpr VariantStorage(VariantStorage const&) = default;
    constexpr VariantStorage(VariantStorage const&)
    requires(!IsTriviallyCopyConstructible<T> || (!IsTriviallyCopyConstructible<Rest> || ...))
    = delete;

    constexpr VariantStorage& operator=(VariantStorage&&) = default;
    constexpr VariantStorage& operator=(VariantStorage&&)
    requires(!IsTriviallyMoveAssignable<T> || (!IsTriviallyMoveAssignable<Rest> || ...))
    = delete;
    constexpr VariantStorage& operator=(VariantStorage const&) = default;
    constexpr VariantStorage& operator=(VariantStorage const&)
    requires(!IsTriviallyCopyAssignable<T> || (!IsTriviallyCopyAssignable<Rest> || ...))
    = delete;

    // Note: Actual destruction is handled by the Variant itself
    constexpr ~VariantStorage() = default;
    constexpr ~VariantStorage()
    requires(!IsTriviallyDestructible<T> || (!IsTriviallyDestructible<Rest> || ...)) {};
    ~VariantStorage()
    requires(!IsDestructible<T> || (!IsDestructible<Rest> || ...))
    = delete;

    VariantStorage<Rest...> rest;
    T value;
};

template<>
union VariantStorage<> {
};

template<typename IndexType, IndexType InitialIndex, typename... Ts>
struct VariantHelper;

template<typename IndexType, IndexType InitialIndex, typename F, typename... Ts>
struct VariantHelper<IndexType, InitialIndex, F, Ts...> {
    static constexpr auto current_index = VariantIndexOf<F, IndexType, InitialIndex, F, Ts...> {}();

    ALWAYS_INLINE constexpr static void reset(VariantStorage<F, Ts...>& storage)
    {
        storage.~VariantStorage<F, Ts...>();
        construct_at(&storage);
    }

    ALWAYS_INLINE constexpr static void delete_(IndexType state, VariantStorage<F, Ts...>& storage)
    {
        if constexpr (sizeof...(Ts) == 0) {
            // Note: This is the last type in the variant
            VERIFY(state == current_index);
            storage.value.~F();
        } else {
            if (state == current_index)
                storage.value.~F();
            else
                VariantHelper<IndexType, InitialIndex + 1, Ts...>::delete_(state, storage.rest);
        }
    }

    ALWAYS_INLINE constexpr static void move_(IndexType old_state, VariantStorage<F, Ts...>& old_storage, VariantStorage<F, Ts...>& new_storage)
    {
        if constexpr (sizeof...(Ts) == 0) {
            // Note: This is the last type in the variant
            VERIFY(old_state == current_index);
            construct_at<F>(&new_storage.value, move(old_storage.value));
        } else {
            if (old_state == current_index)
                construct_at<F>(&new_storage.value, move(old_storage.value));
            else
                VariantHelper<IndexType, InitialIndex + 1, Ts...>::move_(old_state, old_storage.rest, new_storage.rest);
        }
    }

    ALWAYS_INLINE constexpr static void copy(IndexType old_state, VariantStorage<F, Ts...> const& old_storage, VariantStorage<F, Ts...>& new_storage)
    {
        if constexpr (sizeof...(Ts) == 0) {
            // Note: This is the last type in the variant
            VERIFY(old_state == current_index);
            construct_at<F>(&new_storage.value, old_storage.value);
        } else {
            if (old_state == current_index)
                construct_at<F>(&new_storage.value, old_storage.value);
            else
                VariantHelper<IndexType, InitialIndex + 1, Ts...>::copy(old_state, old_storage.rest, new_storage.rest);
        }
    }

    template<typename T>
    ALWAYS_INLINE constexpr static T& get(VariantStorage<F, Ts...>& storage, IndexType state)
    {
        if constexpr (IsSame<T, F>) {
            VERIFY(state == current_index);
            return storage.value;
        }
        if constexpr (sizeof...(Ts) == 0) {
            VERIFY_NOT_REACHED();
        } else {
            return VariantHelper<IndexType, InitialIndex + 1, Ts...>::template get<T>(storage.rest, state);
        }
    }

    template<typename T>
    ALWAYS_INLINE constexpr static T const& get(VariantStorage<F, Ts...> const& storage, IndexType state)
    {
        if constexpr (IsSame<T, F>) {
            VERIFY(state == current_index);
            return storage.value;
        }
        if constexpr (sizeof...(Ts) == 0) {
            VERIFY_NOT_REACHED();
        } else {
            return VariantHelper<IndexType, InitialIndex + 1, Ts...>::template get<T>(storage.rest, state);
        }
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    requires(IsSame<StrippedT, F> || (IsSame<StrippedT, Ts> || ...))
    ALWAYS_INLINE constexpr static void set(VariantStorage<F, Ts...>& storage, T&& value, IndexType state)
    {
        if constexpr (IsSame<StrippedT, F>) {
            VERIFY(state == current_index);
            construct_at(&storage.value, forward<T>(value));
        } else {
            if constexpr (sizeof...(Ts) == 0) {
                VERIFY_NOT_REACHED();
            } else {
                VariantHelper<IndexType, InitialIndex + 1, Ts...>::set(storage.rest, forward<T>(value), state);
            }
        }
    }
};

template<typename IndexType, typename... Ts>
struct VisitImpl {
    template<typename T, size_t I, typename Fn>
    static consteval bool has_explicitly_named_overload()
    {
        // If we're not allowed to make a member function pointer and call it directly (without explicitly resolving it),
        // we have a templated function on our hands (or a function overload set).
        // in such cases, we don't have an explicitly named overload, and we would have to select it.
        return requires { (declval<Fn>().*(&Fn::operator()))(declval<T>()); };
    }

    template<typename T, typename Visitor, auto... Is>
    static consteval bool should_invoke_const_overload(IndexSequence<Is...>)
    {
        // Scan over all the different visitor functions, if none of them are suitable for calling with `T const&`, avoid calling that first.
        return ((has_explicitly_named_overload<T, Is, typename Visitor::Types::template Type<Is>>()) || ...);
    }

    template<typename Self, typename Storage, typename Visitor, IndexType CurrentIndex = 0>
    ALWAYS_INLINE static constexpr decltype(auto) visit(Self& self, IndexType id, Storage& data, Visitor&& visitor)
    {
        static_assert(CurrentIndex < sizeof...(Ts));

        if (id == CurrentIndex) {
            using T = typename TypeList<Ts...>::template Type<CurrentIndex>;
            // Check if Visitor::operator() is an explicitly typed function (as opposed to a templated function)
            // if so, try to call that with `T const&` first before copying the Variant's const-ness.
            // This emulates normal C++ call semantics where templated functions are considered last, after all non-templated overloads
            // are checked and found to be unusable.
            constexpr bool should_invoke_const = should_invoke_const_overload<T, Visitor>(MakeIndexSequence<Visitor::Types::size>());
            // FIXME: We could make this a bit more efficient on the compiler, by slicing the VariantStorage
            //        and only passing the relevant part to the `get` method.
            return visitor(VariantHelper<IndexType, 0, Ts...>::template get<T>(
                static_cast<Conditional<should_invoke_const, AddConst<Storage>&, Storage&>>(data), id));
        }

        if constexpr ((CurrentIndex + 1) < sizeof...(Ts))
            return visit<Self, Storage, Visitor, CurrentIndex + 1>(self, id, data, forward<Visitor>(visitor));
        else
            VERIFY_NOT_REACHED();
    }
};

struct VariantNoClearTag {
    explicit constexpr VariantNoClearTag() = default;
};
struct VariantConstructTag {
    explicit constexpr VariantConstructTag() = default;
};

template<typename T, typename Base>
struct VariantConstructors {
    // This class is mainly used to get proper Value decay for calling the constructors

    // The pointless `typename Base` constraints are a workaround for https://gcc.gnu.org/bugzilla/show_bug.cgi?id=109683
    ALWAYS_INLINE constexpr VariantConstructors(Base& self, T&& t)
    requires(requires { T(move(t)); typename Base; })
    {
        self.set(move(t), VariantNoClearTag {});
    }

    ALWAYS_INLINE constexpr VariantConstructors(Base& self, T const& t)
    requires(requires { T(t); typename Base; })
    {
        self.set(t, VariantNoClearTag {});
    }

    ALWAYS_INLINE constexpr VariantConstructors() = default;
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
};

template<typename...>
struct InheritFromPacks;

// InheritFromPacks will attempt to 'merge' the pack 'Ps' with *itself*, but skip the duplicate entries
// (via InheritFromUniqueEntries).
template<size_t... Is, typename... Ps>
struct InheritFromPacks<IndexSequence<Is...>, Ps...>
    : public InheritFromUniqueEntries<Is, Ps, IndexSequence<Is...>, Ps...>... {

    using InheritFromUniqueEntries<Is, Ps, IndexSequence<Is...>, Ps...>::InheritFromUniqueEntries...;
};

// Just a nice wrapper around InheritFromPacks, which will wrap any parameter packs in ParameterPack (unless it already is one).
template<typename... Ps>
using MergeAndDeduplicatePacks = InheritFromPacks<MakeIndexSequence<sizeof...(Ps)>, Conditional<IsBaseOf<ParameterPackTag, Ps>, Ps, ParameterPack<Ps>>...>;

}

namespace AK {

struct Empty {
    constexpr bool operator==(Empty const&) const = default;
};

template<typename T>
concept NotLvalueReference = !IsLvalueReference<T>;

template<NotLvalueReference... Ts>
struct Variant {
public:
    using IndexType = Conditional<(sizeof...(Ts) < 255), u8, size_t>; // Note: size+1 reserved for internal value checks
private:
    static constexpr IndexType invalid_index = sizeof...(Ts);

    template<typename T>
    static constexpr IndexType index_of() { return Detail::index_of<T, IndexType, Ts...>(); }

public:
    template<typename T>
    static constexpr bool can_contain()
    {
        return index_of<T>() != invalid_index;
    }

    template<typename T>
    requires(!IsSame<T, Variant<Ts...>>
                && (requires(Variant v, T&& a) { Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...>(v, forward<T>(a)); }))
    constexpr Variant(T&& value)
        : m_storage()
        , m_index(invalid_index)
    {
        // This is technically abusing the side effects of the constructor.
        // Getting the compiler to pick the right constructor seems a lot simpler than doing the same
        // with a free-/member-function
        Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...>(*this, forward<T>(value));
    }

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
    ~Variant() = default;

    Variant& operator=(Variant const&)
    requires(!(IsCopyConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...))
    = delete;
    constexpr Variant& operator=(Variant const&) = default;

    Variant& operator=(Variant&&)
    requires(!(IsMoveConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...))
    = delete;
    constexpr Variant& operator=(Variant&&) = default;

    ALWAYS_INLINE constexpr Variant(Variant const& old)
    requires(!(IsTriviallyCopyConstructible<Ts> && ...))
        : m_storage {}
        , m_index(old.m_index)
    {
        Helper::copy(old.m_index, old.m_storage, m_storage);
    }

    // Note: A moved-from variant emulates the state of the object it contains
    //       so if a variant containing an int is moved from, it will still contain that int
    //       and if a variant with a nontrivial move ctor is moved from, it may or may not be valid
    //       but it will still contain the "moved-from" state of the object it previously contained.
    ALWAYS_INLINE constexpr Variant(Variant&& old)
    requires(!(IsTriviallyMoveConstructible<Ts> && ...))
        : m_index(old.m_index)
    {
        Helper::move_(old.m_index, old.m_storage, m_storage);
    }

    ALWAYS_INLINE constexpr ~Variant()
    requires(!(IsTriviallyDestructible<Ts> && ...))
    {
        Helper::delete_(m_index, m_storage);
    }

    ALWAYS_INLINE constexpr Variant& operator=(Variant const& other)
    requires(!(IsTriviallyCopyConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
    {
        if (this != &other) {
            if constexpr (!(IsTriviallyDestructible<Ts> && ...)) {
                Helper::delete_(m_index, m_storage);
                Helper::reset(m_storage);
            }
            m_index = other.m_index;
            Helper::copy(other.m_index, other.m_storage, m_storage);
        }
        return *this;
    }

    ALWAYS_INLINE constexpr Variant& operator=(Variant&& other)
    requires(!(IsTriviallyMoveConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
    {
        if (this != &other) {
            if constexpr (!(IsTriviallyDestructible<Ts> && ...)) {
                Helper::delete_(m_index, m_storage);
                Helper::reset(m_storage);
            }
            m_index = other.m_index;
            Helper::move_(other.m_index, other.m_storage, m_storage);
        }
        return *this;
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    constexpr void set(T&& t)
    requires(can_contain<StrippedT>() && requires { StrippedT(forward<T>(t)); })
    {
        constexpr auto new_index = index_of<StrippedT>();
        Helper::delete_(m_index, m_storage);
        Helper::reset(m_storage);
        Helper::set(m_storage, forward<T>(t), new_index);
        m_index = new_index;
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    constexpr void set(T&& t, Detail::VariantNoClearTag)
    requires(can_contain<StrippedT>() && requires { StrippedT(forward<T>(t)); })
    {
        constexpr auto new_index = index_of<StrippedT>();
        Helper::set(m_storage, forward<T>(t), new_index);
        m_index = new_index;
    }

    template<typename T>
    constexpr T* get_pointer()
    requires(can_contain<T>())
    {
        if (index_of<T>() == m_index)
            return &Helper::template get<T>(m_storage, m_index);
        return nullptr;
    }

    template<typename T>
    constexpr T& get()
    requires(can_contain<T>())
    {
        VERIFY(has<T>());
        return Helper::template get<T>(m_storage, m_index);
    }

    template<typename T>
    constexpr T const* get_pointer() const
    requires(can_contain<T>())
    {
        if (index_of<T>() == m_index)
            return &Helper::template get<T>(m_storage, m_index);
        return nullptr;
    }

    template<typename T>
    constexpr T const& get() const
    requires(can_contain<T>())
    {
        VERIFY(has<T>());
        return Helper::template get<T>(m_storage, m_index);
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
                return self == *p;
            return false;
        });
    }

    template<typename... Fs>
    ALWAYS_INLINE constexpr decltype(auto) visit(Fs&&... functions)
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(*this, m_index, m_storage, move(visitor));
    }

    template<typename... Fs>
    ALWAYS_INLINE constexpr decltype(auto) visit(Fs&&... functions) const
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(*this, m_index, m_storage, move(visitor));
    }

    template<typename... NewTs>
    constexpr decltype(auto) downcast() &&
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
    constexpr decltype(auto) downcast() const&
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

    constexpr auto index() const { return m_index; }

private:
    template<typename... NewTs>
    constexpr Variant<NewTs...> downcast_variant(TypeWrapper<Variant<NewTs...>>) &&
    {
        return move(*this).template downcast<NewTs...>();
    }

    template<typename... NewTs>
    constexpr Variant<NewTs...> downcast_variant(TypeWrapper<Variant<NewTs...>>) const&
    {
        return (*this).template downcast<NewTs...>();
    }

    static constexpr auto data_size = sizeof(Detail::VariantStorage<Ts...>);
    static constexpr auto data_alignment = alignof(Detail::VariantStorage<Ts...>);
    using Helper = Detail::VariantHelper<IndexType, 0, Ts...>;
    using VisitHelper = Detail::VisitImpl<IndexType, Ts...>;

    template<typename T_, typename U_>
    friend struct Detail::VariantConstructors;

    explicit constexpr Variant(IndexType index, Detail::VariantConstructTag)
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

    // Note: Make sure not to default-initialize!
    //       VariantConstructors::VariantConstructors(T) will set this to the correct value
    //       So default-constructing to anything will leave the first initialization with that value instead of the correct one.
    Detail::VariantStorage<Ts...> m_storage;
    IndexType m_index;
};

template<typename... Ts>
struct TypeList<Variant<Ts...>> : TypeList<Ts...> { };

}

#if USING_AK_GLOBALLY
using AK::Empty;
using AK::Variant;
#endif
