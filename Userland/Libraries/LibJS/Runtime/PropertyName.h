/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <LibJS/Runtime/StringOrSymbol.h>

namespace JS {

class PropertyName {
public:
    enum class Type : u8 {
        Invalid,
        Number,
        String,
        Symbol,
    };

    enum class StringMayBeNumber {
        Yes,
        No,
    };

    static PropertyName from_value(GlobalObject& global_object, Value value)
    {
        if (value.is_empty())
            return {};
        if (value.is_symbol())
            return value.as_symbol();
        if (value.is_integral_number() && value.as_double() >= 0 && value.as_double() < NumericLimits<u32>::max())
            return value.as_u32();
        auto string = value.to_string(global_object);
        if (string.is_null())
            return {};
        return string;
    }

    PropertyName() { }

    template<Integral T>
    PropertyName(T index)
    {
        // FIXME: Replace this with requires(IsUnsigned<T>)?
        //        Needs changes in various places using `int` (but not actually being in the negative range)
        VERIFY(index >= 0);
        if constexpr (NumericLimits<T>::max() >= NumericLimits<u32>::max()) {
            if (index >= NumericLimits<u32>::max()) {
                m_string = String::number(index);
                m_type = Type::String;
                m_string_may_be_number = false;
                return;
            }
        }

        m_type = Type::Number;
        m_number = index;
    }

    PropertyName(char const* chars)
        : m_type(Type::String)
        , m_string(FlyString(chars))
    {
    }

    PropertyName(String const& string)
        : m_type(Type::String)
        , m_string(FlyString(string))
    {
        VERIFY(!m_string.is_null());
    }

    PropertyName(FlyString string, StringMayBeNumber string_may_be_number = StringMayBeNumber::Yes)
        : m_type(Type::String)
        , m_string_may_be_number(string_may_be_number == StringMayBeNumber::Yes)
        , m_string(move(string))
    {
        VERIFY(!m_string.is_null());
    }

    PropertyName(Symbol& symbol)
        : m_type(Type::Symbol)
        , m_symbol(&symbol)
    {
    }

    PropertyName(StringOrSymbol const& string_or_symbol)
    {
        if (string_or_symbol.is_string()) {
            m_string = string_or_symbol.as_string();
            m_type = Type::String;
        } else if (string_or_symbol.is_symbol()) {
            m_symbol = const_cast<Symbol*>(string_or_symbol.as_symbol());
            m_type = Type::Symbol;
        }
    }

    ALWAYS_INLINE Type type() const { return m_type; }

    bool is_valid() const { return m_type != Type::Invalid; }
    bool is_number() const
    {
        if (m_type == Type::Number)
            return true;
        if (m_type != Type::String || !m_string_may_be_number)
            return false;

        return const_cast<PropertyName*>(this)->try_coerce_into_number();
    }
    bool is_string() const
    {
        if (m_type != Type::String)
            return false;
        if (!m_string_may_be_number)
            return true;

        return !const_cast<PropertyName*>(this)->try_coerce_into_number();
    }
    bool is_symbol() const { return m_type == Type::Symbol; }

    bool try_coerce_into_number()
    {
        VERIFY(m_string_may_be_number);
        if (m_string.is_empty()) {
            m_string_may_be_number = false;
            return false;
        }

        if (char first = m_string.characters()[0]; first < '0' || first > '9') {
            m_string_may_be_number = false;
            return false;
        } else if (m_string.length() > 1 && first == '0') {
            m_string_may_be_number = false;
            return false;
        }

        auto property_index = m_string.to_uint(TrimWhitespace::No);
        if (!property_index.has_value() || property_index.value() == NumericLimits<u32>::max()) {
            m_string_may_be_number = false;
            return false;
        }
        m_type = Type::Number;
        m_number = *property_index;
        return true;
    }

    u32 as_number() const
    {
        VERIFY(is_number());
        return m_number;
    }

    FlyString const& as_string() const
    {
        VERIFY(is_string());
        return m_string;
    }

    Symbol const* as_symbol() const
    {
        VERIFY(is_symbol());
        return m_symbol;
    }

    String to_string() const
    {
        VERIFY(is_valid());
        VERIFY(!is_symbol());
        if (is_string())
            return as_string();
        return String::number(as_number());
    }

    StringOrSymbol to_string_or_symbol() const
    {
        VERIFY(is_valid());
        VERIFY(!is_number());
        if (is_string())
            return StringOrSymbol(as_string());
        return StringOrSymbol(as_symbol());
    }

private:
    Type m_type { Type::Invalid };
    bool m_string_may_be_number { true };
    FlyString m_string;
    Symbol* m_symbol { nullptr };
    u32 m_number { 0 };
};

struct PropertyNameTraits : public Traits<PropertyName> {
    static unsigned hash(PropertyName const& name)
    {
        VERIFY(name.is_valid());
        if (name.is_string())
            return name.as_string().hash();
        if (name.is_number())
            return int_hash(name.as_number());
        return ptr_hash(name.as_symbol());
    }

    static bool equals(PropertyName const& a, PropertyName const& b)
    {
        if (a.type() != b.type())
            return false;

        switch (a.type()) {
        case PropertyName::Type::Number:
            return a.as_number() == b.as_number();
        case PropertyName::Type::String:
            return a.as_string() == b.as_string();
        case PropertyName::Type::Symbol:
            return a.as_symbol() == b.as_symbol();
        default:
            VERIFY_NOT_REACHED();
        }
    }
};

}

namespace AK {

template<>
struct Formatter<JS::PropertyName> : Formatter<StringView> {
    void format(FormatBuilder& builder, JS::PropertyName const& property_name)
    {
        if (!property_name.is_valid())
            Formatter<StringView>::format(builder, "<invalid PropertyName>");
        else if (property_name.is_number())
            Formatter<StringView>::format(builder, String::number(property_name.as_number()));
        else
            Formatter<StringView>::format(builder, property_name.to_string_or_symbol().to_display_string());
    }
};

}
