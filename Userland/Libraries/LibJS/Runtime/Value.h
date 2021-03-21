/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibJS/Forward.h>
#include <math.h>

// 2 ** 53 - 1
static constexpr double MAX_ARRAY_LIKE_INDEX = 9007199254740991.0;
// 2 ** 32 - 1
static constexpr double MAX_U32 = 4294967295.0;

namespace JS {

class Value {
public:
    enum class Type {
        Empty,
        Undefined,
        Null,
        Int32,
        Double,
        String,
        Object,
        Boolean,
        Symbol,
        Accessor,
        BigInt,
        NativeProperty,
    };

    enum class PreferredType {
        Default,
        String,
        Number,
    };

    bool is_empty() const { return m_type == Type::Empty; }
    bool is_undefined() const { return m_type == Type::Undefined; }
    bool is_null() const { return m_type == Type::Null; }
    bool is_number() const { return m_type == Type::Int32 || m_type == Type::Double; }
    bool is_string() const { return m_type == Type::String; }
    bool is_object() const { return m_type == Type::Object; }
    bool is_boolean() const { return m_type == Type::Boolean; }
    bool is_symbol() const { return m_type == Type::Symbol; }
    bool is_accessor() const { return m_type == Type::Accessor; };
    bool is_bigint() const { return m_type == Type::BigInt; };
    bool is_native_property() const { return m_type == Type::NativeProperty; }
    bool is_nullish() const { return is_null() || is_undefined(); }
    bool is_cell() const { return is_string() || is_accessor() || is_object() || is_bigint() || is_symbol() || is_native_property(); }
    bool is_array() const;
    bool is_function() const;
    bool is_regexp(GlobalObject& global_object) const;

    bool is_nan() const { return is_number() && __builtin_isnan(as_double()); }
    bool is_infinity() const { return is_number() && __builtin_isinf(as_double()); }
    bool is_positive_infinity() const { return is_number() && __builtin_isinf_sign(as_double()) > 0; }
    bool is_negative_infinity() const { return is_number() && __builtin_isinf_sign(as_double()) < 0; }
    bool is_positive_zero() const { return is_number() && 1.0 / as_double() == INFINITY; }
    bool is_negative_zero() const { return is_number() && 1.0 / as_double() == -INFINITY; }
    bool is_integer() const { return is_finite_number() && (i32)as_double() == as_double(); }
    bool is_finite_number() const
    {
        if (!is_number())
            return false;
        auto number = as_double();
        return !__builtin_isnan(number) && !__builtin_isinf(number);
    }

    Value()
        : m_type(Type::Empty)
    {
    }

    explicit Value(bool value)
        : m_type(Type::Boolean)
    {
        m_value.as_bool = value;
    }

    explicit Value(double value)
    {
        if (value >= NumericLimits<i32>::min() && value <= NumericLimits<i32>::max() && trunc(value) == value && value != -0.0) {
            m_type = Type::Int32;
            m_value.as_i32 = static_cast<i32>(value);
        } else {
            m_type = Type::Double;
            m_value.as_double = value;
        }
    }

    explicit Value(unsigned value)
    {
        if (value > NumericLimits<i32>::max()) {
            m_value.as_double = static_cast<double>(value);
            m_type = Type::Double;
        } else {
            m_value.as_i32 = static_cast<i32>(value);
            m_type = Type::Int32;
        }
    }

    explicit Value(i32 value)
        : m_type(Type::Int32)
    {
        m_value.as_i32 = value;
    }

    Value(const Object* object)
        : m_type(object ? Type::Object : Type::Null)
    {
        m_value.as_object = const_cast<Object*>(object);
    }

    Value(const PrimitiveString* string)
        : m_type(Type::String)
    {
        m_value.as_string = const_cast<PrimitiveString*>(string);
    }

    Value(const Symbol* symbol)
        : m_type(Type::Symbol)
    {
        m_value.as_symbol = const_cast<Symbol*>(symbol);
    }

    Value(const Accessor* accessor)
        : m_type(Type::Accessor)
    {
        m_value.as_accessor = const_cast<Accessor*>(accessor);
    }

    Value(const BigInt* bigint)
        : m_type(Type::BigInt)
    {
        m_value.as_bigint = const_cast<BigInt*>(bigint);
    }

    Value(const NativeProperty* native_property)
        : m_type(Type::NativeProperty)
    {
        m_value.as_native_property = const_cast<NativeProperty*>(native_property);
    }

    explicit Value(Type type)
        : m_type(type)
    {
    }

    Type type() const { return m_type; }

    double as_double() const
    {
        VERIFY(is_number());
        if (m_type == Type::Int32)
            return m_value.as_i32;
        return m_value.as_double;
    }

    bool as_bool() const
    {
        VERIFY(type() == Type::Boolean);
        return m_value.as_bool;
    }

    Object& as_object()
    {
        VERIFY(type() == Type::Object);
        return *m_value.as_object;
    }

    const Object& as_object() const
    {
        VERIFY(type() == Type::Object);
        return *m_value.as_object;
    }

    PrimitiveString& as_string()
    {
        VERIFY(is_string());
        return *m_value.as_string;
    }

    const PrimitiveString& as_string() const
    {
        VERIFY(is_string());
        return *m_value.as_string;
    }

    Symbol& as_symbol()
    {
        VERIFY(is_symbol());
        return *m_value.as_symbol;
    }

    const Symbol& as_symbol() const
    {
        VERIFY(is_symbol());
        return *m_value.as_symbol;
    }

    Cell* as_cell()
    {
        VERIFY(is_cell());
        return m_value.as_cell;
    }

    Accessor& as_accessor()
    {
        VERIFY(is_accessor());
        return *m_value.as_accessor;
    }

    BigInt& as_bigint()
    {
        VERIFY(is_bigint());
        return *m_value.as_bigint;
    }

    NativeProperty& as_native_property()
    {
        VERIFY(is_native_property());
        return *m_value.as_native_property;
    }

    Array& as_array();
    Function& as_function();

    i32 as_i32() const;
    u32 as_u32() const;
    size_t as_size_t() const;

    String to_string(GlobalObject&, bool legacy_null_to_empty_string = false) const;
    PrimitiveString* to_primitive_string(GlobalObject&);
    Value to_primitive(GlobalObject&, PreferredType preferred_type = PreferredType::Default) const;
    Object* to_object(GlobalObject&) const;
    Value to_numeric(GlobalObject&) const;
    Value to_number(GlobalObject&) const;
    BigInt* to_bigint(GlobalObject&) const;
    double to_double(GlobalObject&) const;
    i32 to_i32(GlobalObject& global_object) const
    {
        if (m_type == Type::Int32)
            return m_value.as_i32;
        return to_i32_slow_case(global_object);
    }
    u32 to_u32(GlobalObject&) const;
    size_t to_length(GlobalObject&) const;
    size_t to_index(GlobalObject&) const;
    double to_integer_or_infinity(GlobalObject&) const;
    bool to_boolean() const;

    String to_string_without_side_effects() const;

    Value value_or(Value fallback) const
    {
        if (is_empty())
            return fallback;
        return *this;
    }

private:
    Type m_type { Type::Empty };

    i32 to_i32_slow_case(GlobalObject&) const;

    union {
        bool as_bool;
        i32 as_i32;
        double as_double;
        PrimitiveString* as_string;
        Symbol* as_symbol;
        Object* as_object;
        Cell* as_cell;
        Accessor* as_accessor;
        BigInt* as_bigint;
        NativeProperty* as_native_property;
    } m_value;
};

inline Value js_undefined()
{
    return Value(Value::Type::Undefined);
}

inline Value js_null()
{
    return Value(Value::Type::Null);
}

inline Value js_nan()
{
    return Value(NAN);
}

inline Value js_infinity()
{
    return Value(INFINITY);
}

inline Value js_negative_infinity()
{
    return Value(-INFINITY);
}

Value greater_than(GlobalObject&, Value lhs, Value rhs);
Value greater_than_equals(GlobalObject&, Value lhs, Value rhs);
Value less_than(GlobalObject&, Value lhs, Value rhs);
Value less_than_equals(GlobalObject&, Value lhs, Value rhs);
Value bitwise_and(GlobalObject&, Value lhs, Value rhs);
Value bitwise_or(GlobalObject&, Value lhs, Value rhs);
Value bitwise_xor(GlobalObject&, Value lhs, Value rhs);
Value bitwise_not(GlobalObject&, Value);
Value unary_plus(GlobalObject&, Value);
Value unary_minus(GlobalObject&, Value);
Value left_shift(GlobalObject&, Value lhs, Value rhs);
Value right_shift(GlobalObject&, Value lhs, Value rhs);
Value unsigned_right_shift(GlobalObject&, Value lhs, Value rhs);
Value add(GlobalObject&, Value lhs, Value rhs);
Value sub(GlobalObject&, Value lhs, Value rhs);
Value mul(GlobalObject&, Value lhs, Value rhs);
Value div(GlobalObject&, Value lhs, Value rhs);
Value mod(GlobalObject&, Value lhs, Value rhs);
Value exp(GlobalObject&, Value lhs, Value rhs);
Value in(GlobalObject&, Value lhs, Value rhs);
Value instance_of(GlobalObject&, Value lhs, Value rhs);
Value ordinary_has_instance(GlobalObject&, Value lhs, Value rhs);

bool abstract_eq(GlobalObject&, Value lhs, Value rhs);
bool strict_eq(Value lhs, Value rhs);
bool same_value(Value lhs, Value rhs);
bool same_value_zero(Value lhs, Value rhs);
bool same_value_non_numeric(Value lhs, Value rhs);
TriState abstract_relation(GlobalObject&, bool left_first, Value lhs, Value rhs);
Function* get_method(GlobalObject& global_object, Value, const PropertyName&);
size_t length_of_array_like(GlobalObject&, const Object&);

}

namespace AK {

template<>
struct Formatter<JS::Value> : Formatter<StringView> {
    void format(FormatBuilder& builder, const JS::Value& value)
    {
        Formatter<StringView>::format(builder, value.is_empty() ? "<empty>" : value.to_string_without_side_effects());
    }
};

}
