/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/BitCast.h>
#include <AK/StdLibExtraDetails.h>
#include <AK/StdLibExtras.h>
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

template<typename IndexType, IndexType CurrentIndex, typename... Ts>
struct VariantHelper;

template<typename IndexType, IndexType CurrentIndex, typename F, typename... Ts>
struct VariantHelper<IndexType, CurrentIndex, F, Ts...> {
    ALWAYS_INLINE static void delete_(IndexType id, void* data)
    {
        if (id == CurrentIndex)
            bit_cast<F*>(data)->~F();
        else
            VariantHelper<IndexType, CurrentIndex + 1, Ts...>::delete_(id, data);
    }

    ALWAYS_INLINE static void move_(IndexType old_id, void* old_data, void* new_data)
    {
        if (old_id == CurrentIndex)
            new (new_data) F(move(*bit_cast<F*>(old_data)));
        else
            VariantHelper<IndexType, CurrentIndex + 1, Ts...>::move_(old_id, old_data, new_data);
    }

    ALWAYS_INLINE static void copy_(IndexType old_id, void const* old_data, void* new_data)
    {
        if (old_id == CurrentIndex)
            new (new_data) F(*bit_cast<F const*>(old_data));
        else
            VariantHelper<IndexType, CurrentIndex + 1, Ts...>::copy_(old_id, old_data, new_data);
    }
};

template<typename IndexType, IndexType CurrentIndex>
struct VariantHelper<IndexType, CurrentIndex> {
    ALWAYS_INLINE static void delete_(IndexType, void*) { }
    ALWAYS_INLINE static void move_(IndexType, void*, void*) { }
    ALWAYS_INLINE static void copy_(IndexType, void const*, void*) { }
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

template<typename T>
struct Overload {
    // This Overload for <T> can be chosen, if the passed fully qualified type <U&&>, can be used to construct a <T>.
    // The compiler will choose the best overload from the overload set (AllOverloads<...>), in case of ambiguity.
    // NOTE: We always allow narrowing conversions here,
    //       the STL does not allow narrowing conversions for variant constructors.
    //       A point of allowing those is allowing instantiations with literal 0s.
    template<typename U, typename = T>
    requires(IsConstructible<T, U>)
    static auto operator()(T, U&&) -> T;
};

template<typename... Ts>
struct AllOverloads : public Overload<Ts>... {
    using Overload<Ts>::operator()...;
};

template<typename... Types>
using MakeOverloads = AllOverloads<Types...>;

template<typename T>
concept NotLvalueReference = !IsLvalueReference<T>;

template<NotLvalueReference... Ts>
struct Variant {
    // FIXME: Can we get this to return the index as well?
    using OverloadFinder = Detail::MakeOverloads<Ts...>;
    template<typename T>
    using BestMatch = InvokeResult<OverloadFinder, T, T>;

public:
    using IndexType = Conditional<(sizeof...(Ts) < 255), u8, size_t>; // Note: size+1 reserved for internal value checks
private:
    static constexpr IndexType invalid_index = sizeof...(Ts);

    template<typename T>
    static constexpr IndexType index_of() { return Detail::index_of<T, IndexType, Ts...>(); }

public:
    template<typename T>
    static constexpr bool can_contain() { return IsOneOf<T, Ts...>; }

    template<typename... NewTs>
    Variant(Variant<NewTs...>&& old)
    requires((can_contain<NewTs>() && ...))
        : Variant(move(old).template downcast<Ts...>())
    {
    }

    template<typename... NewTs>
    Variant(Variant<NewTs...> const& old)
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
    Variant(T&& t)
    {
        using BestOverload = BestMatch<T>;
        constexpr IndexType BestOverloadIndex = index_of<BestOverload>();

        new (m_data) BestOverload(forward<T>(t));
        m_index = BestOverloadIndex;
    }

    template<NotLvalueReference... NewTs>
    friend struct Variant;

    Variant()
    requires(!can_contain<Empty>())
    = delete;
    Variant()
    requires(can_contain<Empty>())
        : Variant(Empty())
    {
    }

    Variant(Variant const&)
    requires(!(IsCopyConstructible<Ts> && ...))
    = delete;
    Variant(Variant const&) = default;

    Variant(Variant&&)
    requires(!(IsMoveConstructible<Ts> && ...))
    = delete;
    Variant(Variant&&) = default;

    ~Variant()
    requires(!(IsDestructible<Ts> && ...))
    = delete;
    ~Variant() = default;

    Variant& operator=(Variant const&)
    requires(!(IsCopyConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...))
    = delete;
    Variant& operator=(Variant const&) = default;

    Variant& operator=(Variant&&)
    requires(!(IsMoveConstructible<Ts> && ...) || !(IsDestructible<Ts> && ...))
    = delete;
    Variant& operator=(Variant&&) = default;

    ALWAYS_INLINE Variant(Variant const& old)
    requires(!(IsTriviallyCopyConstructible<Ts> && ...))
        : m_data {}
        , m_index(old.m_index)
    {
        Helper::copy_(old.m_index, old.m_data, m_data);
    }

    // Note: A moved-from variant emulates the state of the object it contains
    //       so if a variant containing an int is moved from, it will still contain that int
    //       and if a variant with a nontrivial move ctor is moved from, it may or may not be valid
    //       but it will still contain the "moved-from" state of the object it previously contained.
    ALWAYS_INLINE Variant(Variant&& old)
    requires(!(IsTriviallyMoveConstructible<Ts> && ...))
        : m_index(old.m_index)
    {
        Helper::move_(old.m_index, old.m_data, m_data);
    }

    ALWAYS_INLINE ~Variant()
    requires(!(IsTriviallyDestructible<Ts> && ...))
    {
        Helper::delete_(m_index, m_data);
    }

    ALWAYS_INLINE Variant& operator=(Variant const& other)
    requires(!(IsTriviallyCopyConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
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
    requires(!(IsTriviallyMoveConstructible<Ts> && ...) || !(IsTriviallyDestructible<Ts> && ...))
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

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    void set(T&& t)
    requires(can_contain<StrippedT>() && requires { StrippedT(forward<T>(t)); })
    {
        constexpr auto new_index = index_of<StrippedT>();
        Helper::delete_(m_index, m_data);
        new (m_data) StrippedT(forward<T>(t));
        m_index = new_index;
    }

    template<typename T, typename StrippedT = RemoveCVReference<T>>
    void set(T&& t, Detail::VariantNoClearTag)
    requires(can_contain<StrippedT>() && requires { StrippedT(forward<T>(t)); })
    {
        constexpr auto new_index = index_of<StrippedT>();
        new (m_data) StrippedT(forward<T>(t));
        m_index = new_index;
    }

    template<typename T>
    T* get_pointer()
    requires(can_contain<T>())
    {
        if (index_of<T>() == m_index)
            return bit_cast<T*>(&m_data);
        return nullptr;
    }

    template<typename T>
    T& get()
    requires(can_contain<T>())
    {
        VERIFY(has<T>());
        return *bit_cast<T*>(&m_data);
    }

    template<typename T>
    T const* get_pointer() const
    requires(can_contain<T>())
    {
        if (index_of<T>() == m_index)
            return bit_cast<T const*>(&m_data);
        return nullptr;
    }

    template<typename T>
    T const& get() const
    requires(can_contain<T>())
    {
        VERIFY(has<T>());
        return *bit_cast<T const*>(&m_data);
    }

    template<typename T>
    [[nodiscard]] bool has() const
    requires(can_contain<T>())
    {
        return index_of<T>() == m_index;
    }

    bool operator==(Variant const& other) const
    {
        return this->visit([&]<typename T>(T const& self) {
            if (auto const* p = other.get_pointer<T>())
                return static_cast<T const&>(self) == static_cast<T const&>(*p);
            return false;
        });
    }

    template<typename... Fs>
    ALWAYS_INLINE decltype(auto) visit(Fs&&... functions)
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        return VisitHelper::visit(*this, move(visitor));
    }

    template<typename... Fs>
    ALWAYS_INLINE decltype(auto) visit(Fs&&... functions) const
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
    using Helper = Detail::VariantHelper<IndexType, 0, Ts...>;
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

    // Note: Make sure not to default-initialize!
    //       VariantConstructors::VariantConstructors(T) will set this to the correct value
    //       So default-constructing to anything will leave the first initialization with that value instead of the correct one.
    alignas(data_alignment) u8 m_data[data_size];
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
