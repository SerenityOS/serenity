/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/BitCast.h>
#include <AK/StdLibExtras.h>

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
    static void delete_(IndexType id, void* data)
    {
        if (id == current_index)
            bit_cast<F*>(data)->~F();
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::delete_(id, data);
    }

    static void move_(IndexType old_id, void* old_data, void* new_data)
    {
        if (old_id == current_index)
            new (new_data) F(move(*bit_cast<F*>(old_data)));
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::move_(old_id, old_data, new_data);
    }

    static void copy_(IndexType old_id, const void* old_data, void* new_data)
    {
        if (old_id == current_index)
            new (new_data) F(*bit_cast<F*>(old_data));
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::copy_(old_id, old_data, new_data);
    }

    template<typename Visitor>
    static void visit_(IndexType id, void* data, Visitor&& visitor)
    {
        if (id == current_index)
            visitor(*bit_cast<F*>(data));
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::visit_(id, data, forward<Visitor>(visitor));
    }

    template<typename Visitor>
    static void visit_(IndexType id, const void* data, Visitor&& visitor)
    {
        if (id == current_index)
            visitor(*bit_cast<const F*>(data));
        else
            Variant<IndexType, InitialIndex + 1, Ts...>::visit_(id, data, forward<Visitor>(visitor));
    }
};

template<typename IndexType, IndexType InitialIndex>
struct Variant<IndexType, InitialIndex> {
    static void delete_(IndexType, void*) { }
    static void move_(IndexType, void*, void*) { }
    static void copy_(IndexType, const void*, void*) { }
    template<typename Visitor>
    static void visit_(IndexType, void*, Visitor&&) { }
    template<typename Visitor>
    static void visit_(IndexType, const void*, Visitor&&) { }
};

struct VariantNoClearTag {
    explicit VariantNoClearTag() = default;
};
struct VariantConstructTag {
    explicit VariantConstructTag() = default;
};

template<typename T, typename Base>
struct VariantConstructors {
    VariantConstructors(T&& t)
    {
        internal_cast().template set<T>(forward<T>(t), VariantNoClearTag {});
    }

    VariantConstructors() { }

    Base& operator=(const T& value)
    {
        Base variant { value };
        internal_cast() = move(variant);
        return internal_cast();
    }

    Base& operator=(T&& value)
    {
        Base variant { move(value) };
        internal_cast() = move(variant);
        return internal_cast();
    }

private:
    [[nodiscard]] Base& internal_cast()
    {
        // Warning: Internal type shenanigans - VariantsConstrutors<T, Base> <- Base
        //          Not the other way around, so be _really_ careful not to cause issues.
        return *reinterpret_cast<Base*>(this);
    }
};

}

namespace AK {

struct Empty {
};

template<typename... Ts>
struct Variant
    : public Detail::VariantConstructors<Ts, Variant<Ts...>>... {
private:
    using IndexType = Conditional<sizeof...(Ts) < 255, u8, size_t>; // Note: size+1 reserved for internal value checks
    static constexpr IndexType invalid_index = sizeof...(Ts);

    template<typename T>
    static constexpr IndexType index_of() { return Detail::index_of<T, IndexType, Ts...>(); }

public:
    template<typename... NewTs>
    friend struct Variant;

    Variant(const Variant& old)
        : Detail::VariantConstructors<Ts, Variant<Ts...>>()...
        , m_index(old.m_index)
    {
        Helper::copy_(old.m_index, old.m_data, m_data);
    }

    // Note: A moved-from variant emulates the state of the object it contains
    //       so if a variant containing an int is moved from, it will still contain that int
    //       and if a variant with a nontrivial move ctor is moved from, it may or may not be valid
    //       but it will still contain the "moved-from" state of the object it previously contained.
    Variant(Variant&& old)
        : Detail::VariantConstructors<Ts, Variant<Ts...>>()...
        , m_index(old.m_index)
    {
        Helper::move_(old.m_index, old.m_data, m_data);
    }

    ~Variant()
    {
        Helper::delete_(m_index, m_data);
    }

    Variant& operator=(const Variant& other)
    {
        m_index = other.m_index;
        Helper::copy_(other.m_index, other.m_data, m_data);
        return *this;
    }

    Variant& operator=(Variant&& other)
    {
        m_index = other.m_index;
        Helper::move_(other.m_index, other.m_data, m_data);
        return *this;
    }

    using Detail::VariantConstructors<Ts, Variant<Ts...>>::VariantConstructors...;

    template<typename T>
    void set(T&& t) requires(index_of<T>() != invalid_index)
    {
        using StrippedT = RemoveCV<RemoveReference<T>>;
        constexpr auto new_index = index_of<StrippedT>();
        Helper::delete_(m_index, m_data);
        new (m_data) StrippedT(forward<T>(t));
        m_index = new_index;
    }

    template<typename T>
    void set(T&& t, Detail::VariantNoClearTag)
    {
        using StrippedT = RemoveCV<RemoveReference<T>>;
        constexpr auto new_index = index_of<StrippedT>();
        new (m_data) StrippedT(forward<T>(t));
        m_index = new_index;
    }

    template<typename T>
    T* get_pointer()
    {
        if (index_of<T>() == m_index)
            return bit_cast<T*>(&m_data);
        return nullptr;
    }

    template<typename T>
    [[gnu::noinline]] T& get()
    {
        VERIFY(has<T>());
        return *bit_cast<T*>(&m_data);
    }

    template<typename T>
    [[gnu::noinline]] const T* get_pointer() const
    {
        if (index_of<T>() == m_index)
            return bit_cast<const T*>(&m_data);
        return nullptr;
    }

    template<typename T>
    [[gnu::noinline]] const T& get() const
    {
        VERIFY(has<T>());
        return *bit_cast<const T*>(&m_data);
    }

    template<typename T>
    [[nodiscard]] bool has() const
    {
        return index_of<T>() == m_index;
    }

    template<typename... Fs>
    void visit(Fs&&... functions)
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        Helper::visit_(m_index, m_data, visitor);
    }

    template<typename... Fs>
    void visit(Fs&&... functions) const
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        Helper::visit_(m_index, m_data, visitor);
    }

    template<typename... NewTs>
    Variant<NewTs...> downcast() &&
    {
        Variant<NewTs...> instance { Variant<NewTs...>::invalid_index, Detail::VariantConstructTag {} };
        visit([&](auto& value) {
            if constexpr (Variant<NewTs...>::template can_contain<RemoveCV<RemoveReference<decltype(value)>>>())
                instance.set(move(value), Detail::VariantNoClearTag {});
        });
        VERIFY(instance.m_index != instance.invalid_index);
        return instance;
    }

    template<typename... NewTs>
    Variant<NewTs...> downcast() &
    {
        Variant<NewTs...> instance { Variant<NewTs...>::invalid_index, Detail::VariantConstructTag {} };
        visit([&](const auto& value) {
            if constexpr (Variant<NewTs...>::template can_contain<RemoveCV<RemoveReference<decltype(value)>>>())
                instance.set(value, Detail::VariantNoClearTag {});
        });
        VERIFY(instance.m_index != instance.invalid_index);
        return instance;
    }

    template<typename T>
    static constexpr bool can_contain()
    {
        return index_of<T>() != invalid_index;
    }

private:
    static constexpr auto data_size = integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, sizeof(Ts)...>()).max();
    static constexpr auto data_alignment = integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, alignof(Ts)...>()).max();
    using Helper = Detail::Variant<IndexType, 0, Ts...>;

    explicit Variant(IndexType index, Detail::VariantConstructTag)
        : Detail::MergeAndDeduplicatePacks<Detail::VariantConstructors<Ts, Variant<Ts...>>...>()
        , m_index(index)
    {
    }

    template<typename... Fs>
    struct Visitor : Fs... {
        Visitor(Fs&&... args)
            : Fs(args)...
        {
        }

        using Fs::operator()...;
    };

    alignas(data_alignment) u8 m_data[data_size];
    // Note: Make sure not to default-initialize!
    //       VariantConstructors::VariantConstructors(T) will set this to the correct value
    //       So default-constructing to anything will leave the first initialization with that value instead of the correct one.
    IndexType m_index;
};

}

using AK::Empty;
using AK::Variant;
