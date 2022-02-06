/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/BitCast.h>
#include <AK/Concepts.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/Result.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/Utf16String.h>
#include <math.h>

// 2 ** 53 - 1
static constexpr double MAX_ARRAY_LIKE_INDEX = 9007199254740991.0;
// Unique bit representation of negative zero (only sign bit set)
static constexpr u64 NEGATIVE_ZERO_BITS = ((u64)1 << 63);

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
    bool is_nullish() const { return is_null() || is_undefined(); }
    bool is_cell() const { return is_string() || is_accessor() || is_object() || is_bigint() || is_symbol(); }
    ThrowCompletionOr<bool> is_array(GlobalObject&) const;
    bool is_function() const;
    bool is_constructor() const;
    ThrowCompletionOr<bool> is_regexp(GlobalObject&) const;

    bool is_nan() const
    {
        if (type() == Type::Int32)
            return false;
        return is_number() && __builtin_isnan(as_double());
    }

    bool is_infinity() const
    {
        if (type() == Type::Int32)
            return false;
        return is_number() && __builtin_isinf(as_double());
    }

    bool is_positive_infinity() const
    {
        if (type() == Type::Int32)
            return false;
        return is_number() && __builtin_isinf_sign(as_double()) > 0;
    }

    bool is_negative_infinity() const
    {
        if (type() == Type::Int32)
            return false;
        return is_number() && __builtin_isinf_sign(as_double()) < 0;
    }

    bool is_positive_zero() const
    {
        if (type() == Type::Int32)
            return as_i32() == 0;
        return is_number() && bit_cast<u64>(as_double()) == 0;
    }

    bool is_negative_zero() const
    {
        if (type() == Type::Int32)
            return false;
        return is_number() && bit_cast<u64>(as_double()) == NEGATIVE_ZERO_BITS;
    }

    bool is_integral_number() const
    {
        if (type() == Type::Int32)
            return true;
        return is_finite_number() && trunc(as_double()) == as_double();
    }

    bool is_finite_number() const
    {
        if (type() == Type::Int32)
            return true;
        if (!is_number())
            return false;
        auto number = as_double();
        return !__builtin_isnan(number) && !__builtin_isinf(number);
    }

    Value()
        : m_type(Type::Empty)
    {
    }

    template<typename T>
    requires(SameAs<RemoveCVReference<T>, bool>) explicit Value(T value)
        : m_type(Type::Boolean)
    {
        m_value.as_bool = value;
    }

    explicit Value(double value)
    {
        bool is_negative_zero = bit_cast<u64>(value) == NEGATIVE_ZERO_BITS;
        if (value >= NumericLimits<i32>::min() && value <= NumericLimits<i32>::max() && trunc(value) == value && !is_negative_zero) {
            m_type = Type::Int32;
            m_value.as_i32 = static_cast<i32>(value);
        } else {
            m_type = Type::Double;
            m_value.as_double = value;
        }
    }

    explicit Value(unsigned long value)
    {
        if (value > NumericLimits<i32>::max()) {
            m_value.as_double = static_cast<double>(value);
            m_type = Type::Double;
        } else {
            m_value.as_i32 = static_cast<i32>(value);
            m_type = Type::Int32;
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

    Cell& as_cell()
    {
        VERIFY(is_cell());
        return *m_value.as_cell;
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

    Array& as_array();
    FunctionObject& as_function();
    FunctionObject const& as_function() const;

    // FIXME: These two conversions are wrong for JS, and seem likely to be footguns
    i32 as_i32() const
    {
        if (m_type == Type::Int32)
            return m_value.as_i32;
        return static_cast<i32>(as_double());
    }
    u32 as_u32() const
    {
        if (m_type == Type::Int32 && m_value.as_i32 >= 0)
            return m_value.as_i32;
        VERIFY(as_double() >= 0);
        return (u32)min(as_double(), (double)NumericLimits<u32>::max());
    }

    u64 encoded() const { return m_value.encoded; }

    ThrowCompletionOr<String> to_string(GlobalObject&) const;
    ThrowCompletionOr<Utf16String> to_utf16_string(GlobalObject&) const;
    ThrowCompletionOr<PrimitiveString*> to_primitive_string(GlobalObject&);
    ThrowCompletionOr<Value> to_primitive(GlobalObject&, PreferredType preferred_type = PreferredType::Default) const;
    ThrowCompletionOr<Object*> to_object(GlobalObject&) const;
    ThrowCompletionOr<Value> to_numeric(GlobalObject&) const;
    ThrowCompletionOr<Value> to_number(GlobalObject&) const;
    ThrowCompletionOr<BigInt*> to_bigint(GlobalObject&) const;
    ThrowCompletionOr<i64> to_bigint_int64(GlobalObject&) const;
    ThrowCompletionOr<u64> to_bigint_uint64(GlobalObject&) const;
    ThrowCompletionOr<double> to_double(GlobalObject&) const;
    ThrowCompletionOr<PropertyKey> to_property_key(GlobalObject&) const;
    ThrowCompletionOr<i32> to_i32(GlobalObject& global_object) const;
    ThrowCompletionOr<u32> to_u32(GlobalObject&) const;
    ThrowCompletionOr<i16> to_i16(GlobalObject&) const;
    ThrowCompletionOr<u16> to_u16(GlobalObject&) const;
    ThrowCompletionOr<i8> to_i8(GlobalObject&) const;
    ThrowCompletionOr<u8> to_u8(GlobalObject&) const;
    ThrowCompletionOr<u8> to_u8_clamp(GlobalObject&) const;
    ThrowCompletionOr<size_t> to_length(GlobalObject&) const;
    ThrowCompletionOr<size_t> to_index(GlobalObject&) const;
    ThrowCompletionOr<double> to_integer_or_infinity(GlobalObject&) const;
    bool to_boolean() const;

    ThrowCompletionOr<Value> get(GlobalObject&, PropertyKey const&) const;
    ThrowCompletionOr<FunctionObject*> get_method(GlobalObject&, PropertyKey const&) const;

    String to_string_without_side_effects() const;
    Optional<BigInt*> string_to_bigint(GlobalObject& global_object) const;

    Value value_or(Value fallback) const
    {
        if (is_empty())
            return fallback;
        return *this;
    }

    String typeof() const;

    bool operator==(Value const&) const;

    template<typename... Args>
    [[nodiscard]] ALWAYS_INLINE ThrowCompletionOr<Value> invoke(GlobalObject& global_object, PropertyKey const& property_key, Args... args);

private:
    Type m_type { Type::Empty };

    [[nodiscard]] ThrowCompletionOr<Value> invoke_internal(GlobalObject& global_object, PropertyKey const&, Optional<MarkedValueList> arguments);

    ThrowCompletionOr<i32> to_i32_slow_case(GlobalObject&) const;

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

        u64 encoded;
    } m_value { .encoded = 0 };
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

inline void Cell::Visitor::visit(Value value)
{
    if (value.is_cell())
        visit_impl(value.as_cell());
}

ThrowCompletionOr<Value> greater_than(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> greater_than_equals(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> less_than(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> less_than_equals(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> bitwise_and(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> bitwise_or(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> bitwise_xor(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> bitwise_not(GlobalObject&, Value);
ThrowCompletionOr<Value> unary_plus(GlobalObject&, Value);
ThrowCompletionOr<Value> unary_minus(GlobalObject&, Value);
ThrowCompletionOr<Value> left_shift(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> right_shift(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> unsigned_right_shift(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> add(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> sub(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> mul(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> div(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> mod(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> exp(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> in(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> instance_of(GlobalObject&, Value lhs, Value rhs);
ThrowCompletionOr<Value> ordinary_has_instance(GlobalObject&, Value lhs, Value rhs);

ThrowCompletionOr<bool> is_loosely_equal(GlobalObject&, Value lhs, Value rhs);
bool is_strictly_equal(Value lhs, Value rhs);
bool same_value(Value lhs, Value rhs);
bool same_value_zero(Value lhs, Value rhs);
bool same_value_non_numeric(Value lhs, Value rhs);
ThrowCompletionOr<TriState> is_less_than(GlobalObject&, bool left_first, Value lhs, Value rhs);

inline bool Value::operator==(Value const& value) const { return same_value(*this, value); }

struct ValueTraits : public Traits<Value> {
    static unsigned hash(Value value)
    {
        VERIFY(!value.is_empty());
        if (value.is_string())
            return value.as_string().string().hash();

        if (value.is_bigint())
            return value.as_bigint().big_integer().hash();

        if (value.is_negative_zero())
            value = Value(0);

        return u64_hash(value.encoded()); // FIXME: Is this the best way to hash pointers, doubles & ints?
    }
    static bool equals(const Value a, const Value b)
    {
        return same_value_zero(a, b);
    }
};

}

namespace AK {

template<>
struct Formatter<JS::Value> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, JS::Value value)
    {
        return Formatter<StringView>::format(builder, value.is_empty() ? "<empty>" : value.to_string_without_side_effects());
    }
};

}
