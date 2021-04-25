/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/BitCast.h>
#include <AK/StdLibExtras.h>
#include <typeinfo>

namespace AK::Detail {

template<typename... Ts>
struct Variant;

template<typename F, typename... Ts>
struct Variant<F, Ts...> {
    static void delete_(const std::type_info& id, void* data)
    {
        if (id == typeid(F))
            bit_cast<F*>(data)->~F();
        else
            Variant<Ts...>::delete_(id, data);
    }

    static void move_(const std::type_info& old_id, void* old_data, void* new_data)
    {
        if (old_id == typeid(F))
            new (new_data) F(move(*bit_cast<F*>(old_data)));
        else
            Variant<Ts...>::move_(old_id, old_data, new_data);
    }

    static void copy_(const std::type_info& old_id, const void* old_data, void* new_data)
    {
        if (old_id == typeid(F))
            new (new_data) F(*bit_cast<F*>(old_data));
        else
            Variant<Ts...>::copy_(old_id, old_data, new_data);
    }

    template<typename Visitor>
    static void visit_(const std::type_info& id, void* data, Visitor&& visitor)
    {
        if (id == typeid(F))
            visitor(*bit_cast<F*>(data));
        else
            Variant<Ts...>::visit_(id, data, forward<Visitor>(visitor));
    }

    template<typename Visitor>
    static void visit_(const std::type_info& id, const void* data, Visitor&& visitor)
    {
        if (id == typeid(F))
            visitor(*bit_cast<const F*>(data));
        else
            Variant<Ts...>::visit_(id, data, forward<Visitor>(visitor));
    }
};

template<>
struct Variant<> {
    static void delete_(const std::type_info&, void*) { }
    static void move_(const std::type_info&, void*, void*) { }
    static void copy_(const std::type_info&, const void*, void*) { }
    template<typename Visitor>
    static void visit_(const std::type_info&, void*, Visitor&&) { }
    template<typename Visitor>
    static void visit_(const std::type_info&, const void*, Visitor&&) { }
};

struct VariantNoClearTag {
    explicit VariantNoClearTag() = default;
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

    template<typename... NewTs>
    friend struct Variant;

    Variant(const Variant& old)
        : Detail::VariantConstructors<Ts, Variant<Ts...>>()...
        , m_type_info(old.m_type_info)
    {
        Helper::copy_(*old.m_type_info, old.m_data, m_data);
    }

    // Note: A moved-from variant emulates the state of the object it contains
    //       so if a variant containing an int is moved from, it will still contain that int
    //       and if a variant with a nontrivial move ctor is moved from, it may or may not be valid
    //       but it will still contain the "moved-from" state of the object it previously contained.
    Variant(Variant&& old)
        : Detail::VariantConstructors<Ts, Variant<Ts...>>()...
        , m_type_info(old.m_type_info)
    {
        Helper::move_(*old.m_type_info, old.m_data, m_data);
    }

    ~Variant()
    {
        Helper::delete_(*m_type_info, m_data);
    }

    Variant& operator=(const Variant& other)
    {
        m_type_info = other.m_type_info;
        Helper::copy_(*other.m_type_info, other.m_data, m_data);
        return *this;
    }

    Variant& operator=(Variant&& other)
    {
        m_type_info = other.m_type_info;
        Helper::move_(*other.m_type_info, other.m_data, m_data);
        return *this;
    }

    using Detail::VariantConstructors<Ts, Variant<Ts...>>::VariantConstructors...;

    template<typename T>
    void set(T&& t)
    {
        Helper::delete_(*m_type_info, m_data);
        new (m_data) T(forward<T>(t));
        m_type_info = &typeid(T);
    }

    template<typename T>
    void set(T&& t, Detail::VariantNoClearTag)
    {
        new (m_data) T(forward<T>(t));
        m_type_info = &typeid(T);
    }

    template<typename T>
    T* get_pointer()
    {
        if (typeid(T) == *m_type_info)
            return reinterpret_cast<T*>(m_data);
        return nullptr;
    }

    template<typename T>
    T& get()
    {
        VERIFY(typeid(T) == *m_type_info);
        return *reinterpret_cast<T*>(m_data);
    }

    template<typename T>
    const T* get_pointer() const
    {
        if (typeid(T) == *m_type_info)
            return reinterpret_cast<const T*>(m_data);
        return nullptr;
    }

    template<typename T>
    const T& get() const
    {
        VERIFY(typeid(T) == *m_type_info);
        return *reinterpret_cast<const T*>(m_data);
    }

    template<typename T>
    [[nodiscard]] bool has() const
    {
        return typeid(T) == *m_type_info;
    }

    template<typename... Fs>
    void visit(Fs&&... functions)
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        Helper::visit_(*m_type_info, m_data, visitor);
    }

    template<typename... Fs>
    void visit(Fs&&... functions) const
    {
        Visitor<Fs...> visitor { forward<Fs>(functions)... };
        Helper::visit_(*m_type_info, m_data, visitor);
    }

    template<typename... NewTs>
    Variant<NewTs...> downcast() &&
    {
        VERIFY(covers<NewTs...>());
        Variant<NewTs...> instance { m_type_info };
        Helper::move_(*m_type_info, m_data, instance.m_data);
        return instance;
    }

    template<typename... NewTs>
    Variant<NewTs...> downcast() &
    {
        VERIFY(covers<NewTs...>());
        Variant<NewTs...> instance { m_type_info };
        Helper::copy_(*m_type_info, m_data, instance.m_data);
        return instance;
    }

private:
    static constexpr auto data_size = integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, sizeof(Ts)...>()).max();
    static constexpr auto data_alignment = integer_sequence_generate_array<size_t>(0, IntegerSequence<size_t, alignof(Ts)...>()).max();
    using Helper = Detail::Variant<Ts...>;

    template<typename... NewTs>
    bool covers() const
    {
        return ((typeid(NewTs) == *m_type_info) || ...);
    }

    explicit Variant(const std::type_info* type_info)
        : Detail::VariantConstructors<Ts, Variant<Ts...>>()...
        , m_type_info(type_info)
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
    const std::type_info* m_type_info;
};

}

using AK::Empty;
using AK::Variant;
