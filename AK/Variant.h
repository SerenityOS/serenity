/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/BitCast.h>
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

template<typename IndexType, IndexType InitialIndex, typename... Ts>
struct Variant;

template<typename IndexType, IndexType InitialIndex, typename F, typename... Ts>
struct Variant<IndexType, InitialIndex, F, Ts...> {
    static constexpr auto current_index = VariantIndexOf<F, IndexType, InitialIndex, F, Ts...> {}();
    ALWAYS_INLINE static void delete_(IndexType id, void* data)
    {
        if (id == current_index)
            bit_cast<F*>(data)->~F();
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::delete_(id, data);
    }

    ALWAYS_INLINE static void move_(IndexType old_id, void* old_data, void* new_data)
    {
        if (old_id == current_index)
            new (new_data) F(move(*bit_cast<F*>(old_data)));
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::move_(old_id, old_data, new_data);
    }

    ALWAYS_INLINE static void copy_(IndexType old_id, const void* old_data, void* new_data)
    {
        if (old_id == current_index)
            new (new_data) F(*bit_cast<F const*>(old_data));
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::copy_(old_id, old_data, new_data);
    }
};

template<typename IndexType, IndexType InitialIndex>
struct Variant<IndexType, InitialIndex> {
    ALWAYS_INLINE static void delete_(IndexType, void*) { }
    ALWAYS_INLINE static void move_(IndexType, void*, void*) { }
    ALWAYS_INLINE static void copy_(IndexType, const void*, void*) { }
};

template<typename IndexType, typename... Ts>
struct VisitImpl {
    template<typename Visitor, IndexType CurrentIndex = 0>
    ALWAYS_INLINE static constexpr decltype(auto) visit(IndexType id, const void* data, Visitor&& visitor) requires(CurrentIndex < sizeof...(Ts))
    {
        using T = typename TypeList<Ts...>::template Type<CurrentIndex>;

        if (id == CurrentIndex)
            return visitor(*bit_cast<T*>(data));

        if constexpr ((CurrentIndex + 1) < sizeof...(Ts))
            return visit<Visitor, CurrentIndex + 1>(id, data, forward<Visitor>(visitor));
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

template<typename T, typename Base>
struct VariantConstructors {
    ALWAYS_INLINE VariantConstructors(T&& t)
    {
        internal_cast().clear_without_destruction();
        internal_cast().set(move(t), VariantNoClearTag {});
    }

    ALWAYS_INLINE VariantConstructors(const T& t)
    {
        internal_cast().clear_without_destruction();
        internal_cast().set(t, VariantNoClearTag {});
    }

    ALWAYS_INLINE VariantConstructors() = default;

private:
    [[nodiscard]] ALWAYS_INLINE Base& internal_cast()
    {
        // Warning: Internal type shenanigans - VariantsConstrutors<T, Base> <- Base
        //          Not the other way around, so be _really_ careful not to cause issues.
        return *reinterpret_cast<Base*>(this);
    }
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

template<unsigned I, typename...>
struct InheritFromUniqueEntries;

// InheritFromUniqueEntries will inherit from both Qs and Ts, but only scan entries going *forwards*
// that is to say, if it's scanning from index I in Qs, it won't scan for duplicates for entries before I
// as that has already been checked before.
// This makes sure that the search is linear in time (like the 'merge' step of merge sort).
template<unsigned I, typename... Ts, unsigned... Js, typename... Qs>
struct InheritFromUniqueEntries<I, ParameterPack<Ts...>, IndexSequence<Js...>, Qs...>
    : public BlankIfDuplicate<Ts, Conditional<Js <= I, ParameterPack<>, Qs>...>... {

    using BlankIfDuplicate<Ts, Conditional<Js <= I, ParameterPack<>, Qs>...>::BlankIfDuplicate...;
};

template<typename...>
struct InheritFromPacks;

// InheritFromPacks will attempt to 'merge' the pack 'Ps' with *itself*, but skip the duplicate entries
// (via InheritFromUniqueEntries).
template<unsigned... Is, typename... Ps>
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
};

template<typename... Ts>
struct Variant
    : public Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...> {
private:
    using IndexType = Conditional<sizeof...(Ts) < 255, u8, size_t>; // Note: size+1 reserved for internal value checks
    static constexpr IndexType invalid_index = sizeof...(Ts);

    template<typename T>
    static constexpr IndexType index_of() { return Detail::index_of<T, IndexType, Ts...>(); }

public:
    template<typename T>
    static constexpr bool can_contain()
    {
        return index_of<T>() != invalid_index;
    }

    template<typename... NewTs>
    friend struct Variant;

    Variant() requires(!can_contain<Empty>()) = delete;
    Variant() requires(can_contain<Empty>())
        : Variant(Empty())
    {
    }

#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
    Variant(const Variant&) requires(!(IsCopyConstructible<Ts> && ...)) = delete;
    Variant(const Variant&) = default;

    Variant(Variant&&) requires(!(IsMoveConstructible<Ts> && ...)) = delete;
    Variant(Variant&&) = default;

    ~Variant() requires(!(IsDestructible<Ts> && ...)) = delete;
    ~Variant() = default;

    Variant& operator=(const Variant&) requires(!(IsCopyConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...)) = delete;
    Variant& operator=(const Variant&) = default;

    Variant& operator=(Variant&&) requires(!(IsMoveConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...)) = delete;
    Variant& operator=(Variant&&) = default;
#endif

    ALWAYS_INLINE Variant(const Variant& old)
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
        requires(!(IsTriviallyCopyConstructible<Ts> && ...))
#endif
        : Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...>()
        , m_data {}
        , m_index(old.m_index)
    {
        Helper::copy_(old.m_index, old.m_data, m_data);
    }

    // Note: A moved-from variant emulates the state of the object it contains
    //       so if a variant containing an int is moved from, it will still contain that int
    //       and if a variant with a nontrivial move ctor is moved from, it may or may not be valid
    //       but it will still contain the "moved-from" state of the object it previously contained.
    ALWAYS_INLINE Variant(Variant&& old)
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
        requires(!(IsTriviallyMoveConstructible<Ts> && ...))
#endif
        : Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...>()
        , m_index(old.m_index)
    {
        Helper::move_(old.m_index, old.m_data, m_data);
    }

    ALWAYS_INLINE ~Variant()
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
        requires(!(IsTriviallyDestructible<Ts> && ...))
#endif
    {
        Helper::delete_(m_index, m_data);
    }

    ALWAYS_INLINE Variant& operator=(const Variant& other)
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
        requires(!(IsTriviallyCopyConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
#endif
    {
        if (this != &other) {
            if constexpr (!(IsTriviallyDestructible<Ts> && ...)) {
                Helper::delete_(m_index, m_data);
            }
            m_index = other.m_index;
            Helper::copy_(other.m_index, other.m_data, m_data);
        }
        return *this;
    }

    ALWAYS_INLINE Variant& operator=(Variant&& other)
#ifdef AK_HAS_CONDITIONALLY_TRIVIAL
        requires(!(IsTriviallyMoveConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
#endif
    {
        if (this != &other) {
            if constexpr (!(IsTriviallyDestructible<Ts> && ...)) {
                Helper::delete_(m_index, m_data);
            }
            m_index = other.m_index;
            Helper::move_(other.m_index, other.m_data, m_data);
        }
        return *this;
    }

    using Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...>::MergeAndDeduplicatePacks;

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    void set(T&& t) requires(can_contain<StrippedT>())
    {
        constexpr auto new_index = index_of<StrippedT>();
        Helper::delete_(m_index, m_data);
        new (m_data) StrippedT(forward<T>(t));
        m_index = new_index;
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    void set(T&& t, Detail::VariantNoClearTag) requires(can_contain<StrippedT>())
    {
        constexpr auto new_index = index_of<StrippedT>();
        new (m_data) StrippedT(forward<T>(t));
        m_index = new_index;
    }

    template<typename T>
    T* get_pointer() requires(can_contain<T>())
    {
        if (index_of<T>() == m_index)
            return bit_cast<T*>(&m_data);
        return nullptr;
    }

    template<typename T>
    T& get() requires(can_contain<T>())
    {
        VERIFY(has<T>());
        return *bit_cast<T*>(&m_data);
    }

    template<typename T>
    const T* get_pointer() const requires(can_contain<T>())
    {
        if (index_of<T>() == m_index)
            return bit_cast<const T*>(&m_data);
        return nullptr;
    }

    template<typename T>
    const T& get() const requires(can_contain<T>())
    {
        VERIFY(has<T>());
        return *bit_cast<const T*>(&m_data);
    }

    template<typename T>
    [[nodiscard]] bool has() const requires(can_contain<T>())
    {
        return index_of<T>() == m_index;
    }

    template<typename... Fs>
    ALWAYS_INLINE decltype(auto) visit(Fs&&... functions)
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(m_index, m_data, move(visitor));
    }

    template<typename... Fs>
    ALWAYS_INLINE decltype(auto) visit(Fs&&... functions) const
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(m_index, m_data, move(visitor));
    }

    template<typename... NewTs>
    Variant<NewTs...> downcast() &&
    {
        Variant<NewTs...> instance { Variant<NewTs...>::invalid_index, Detail::VariantConstructTag {} };
        visit([&](auto& value) {
            if constexpr (Variant<NewTs...>::template can_contain<RemoveCVReference<decltype(value)>>())
                instance.set(move(value), Detail::VariantNoClearTag {});
        });
        VERIFY(instance.m_index != instance.invalid_index);
        return instance;
    }

    template<typename... NewTs>
    Variant<NewTs...> downcast() const&
    {
        Variant<NewTs...> instance { Variant<NewTs...>::invalid_index, Detail::VariantConstructTag {} };
        visit([&](const auto& value) {
            if constexpr (Variant<NewTs...>::template can_contain<RemoveCVReference<decltype(value)>>())
                instance.set(value, Detail::VariantNoClearTag {});
        });
        VERIFY(instance.m_index != instance.invalid_index);
        return instance;
    }

    template<typename... NewTs>
    explicit operator Variant<NewTs...>() &&
    {
        return downcast<NewTs...>();
    }

    template<typename... NewTs>
    explicit operator Variant<NewTs...>() const&
    {
        return downcast<NewTs...>();
    }

private:
    static constexpr auto data_size = Detail::integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, sizeof(Ts)...>()).max();
    static constexpr auto data_alignment = Detail::integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, alignof(Ts)...>()).max();
    using Helper = Detail::Variant<IndexType, 0, Ts...>;
    using VisitHelper = Detail::VisitImpl<IndexType, Ts...>;

    template<typename T_, typename U_>
    friend struct Detail::VariantConstructors;

    explicit Variant(IndexType index, Detail::VariantConstructTag)
        : Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...>()
        , m_index(index)
    {
    }

    ALWAYS_INLINE void clear_without_destruction()
    {
        __builtin_memset(m_data, 0, data_size);
        m_index = invalid_index;
    }

    template<typename... Fs>
    struct Visitor : Fs... {
        Visitor(Fs&&... args)
            : Fs(forward<Fs>(args))...
        {
        }

        using Fs::operator()...;
    };

    // Note: Make sure not to default-initialize!
    //       VariantConstructors::VariantConstructors(T) will set this to the correct value
    //       So default-constructing to anything will leave the first initialization with that value instead of the correct one.
    alignas(data_alignment) u8 m_data[data_size];
    IndexType m_index;
};

}

using AK::Empty;
using AK::Variant;
