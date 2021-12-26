/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/Value.h>
#include <math.h>

namespace JS {

bool Value::is_array() const
{
    return is_object() && as_object().is_array();
}

bool Value::is_function() const
{
    return is_object() && as_object().is_function();
}

Function& Value::as_function()
{
    ASSERT(is_function());
    return static_cast<Function&>(as_object());
}

String Value::to_string_without_side_effects() const
{
    switch (m_type) {
    case Type::Undefined:
        return "undefined";
    case Type::Null:
        return "null";
    case Type::Boolean:
        return m_value.as_bool ? "true" : "false";
    case Type::Number:
        if (is_nan())
            return "NaN";
        if (is_infinity())
            return is_negative_infinity() ? "-Infinity" : "Infinity";
        if (is_integer())
            return String::number(as_i32());
        return String::format("%.4f", m_value.as_double);
    case Type::String:
        return m_value.as_string->string();
    case Type::Symbol:
        return m_value.as_symbol->to_string();
    case Type::Object:
        return String::format("[object %s]", as_object().class_name());
    case Type::Accessor:
        return "<accessor>";
    default:
        ASSERT_NOT_REACHED();
    }
}

PrimitiveString* Value::to_primitive_string(Interpreter& interpreter)
{
    if (is_string())
        return &as_string();
    auto string = to_string(interpreter);
    if (interpreter.exception())
        return nullptr;
    return js_string(interpreter, string);
}

String Value::to_string(Interpreter& interpreter) const
{
    switch (m_type) {
    case Type::Undefined:
        return "undefined";
    case Type::Null:
        return "null";
    case Type::Boolean:
        return m_value.as_bool ? "true" : "false";
    case Type::Number:
        if (is_nan())
            return "NaN";
        if (is_infinity())
            return is_negative_infinity() ? "-Infinity" : "Infinity";
        if (is_integer())
            return String::number(as_i32());
        return String::format("%.4f", m_value.as_double);
    case Type::String:
        return m_value.as_string->string();
    case Type::Symbol:
        interpreter.throw_exception<TypeError>("Can't convert symbol to string");
        return {};
    case Type::Object: {
        auto primitive_value = as_object().to_primitive(PreferredType::String);
        if (interpreter.exception())
            return {};
        return primitive_value.to_string(interpreter);
    }
    default:
        ASSERT_NOT_REACHED();
    }
}

bool Value::to_boolean() const
{
    switch (m_type) {
    case Type::Undefined:
    case Type::Null:
        return false;
    case Type::Boolean:
        return m_value.as_bool;
    case Type::Number:
        if (is_nan())
            return false;
        return m_value.as_double != 0;
    case Type::String:
        return !m_value.as_string->string().is_empty();
    case Type::Symbol:
    case Type::Object:
        return true;
    default:
        ASSERT_NOT_REACHED();
    }
}

Value Value::to_primitive(Interpreter&, PreferredType preferred_type) const
{
    if (is_object())
        return as_object().to_primitive(preferred_type);
    return *this;
}

Object* Value::to_object(Interpreter& interpreter) const
{
    switch (m_type) {
    case Type::Undefined:
    case Type::Null:
        interpreter.throw_exception<TypeError>("ToObject on null or undefined.");
        return nullptr;
    case Type::Boolean:
        return BooleanObject::create(interpreter.global_object(), m_value.as_bool);
    case Type::Number:
        return NumberObject::create(interpreter.global_object(), m_value.as_double);
    case Type::String:
        return StringObject::create(interpreter.global_object(), *m_value.as_string);
    case Type::Symbol:
        return SymbolObject::create(interpreter.global_object(), *m_value.as_symbol);
    case Type::Object:
        return &const_cast<Object&>(as_object());
    default:
        dbg() << "Dying because I can't to_object() on " << *this;
        ASSERT_NOT_REACHED();
    }
}

Value Value::to_number(Interpreter& interpreter) const
{
    switch (m_type) {
    case Type::Undefined:
        return js_nan();
    case Type::Null:
        return Value(0);
    case Type::Boolean:
        return Value(m_value.as_bool ? 1 : 0);
    case Type::Number:
        return Value(m_value.as_double);
    case Type::String: {
        auto string = as_string().string().trim_whitespace();
        if (string.is_empty())
            return Value(0);
        if (string == "Infinity" || string == "+Infinity")
            return js_infinity();
        if (string == "-Infinity")
            return js_negative_infinity();
        char* endptr;
        auto parsed_double = strtod(string.characters(), &endptr);
        if (*endptr)
            return js_nan();
        return Value(parsed_double);
    }
    case Type::Symbol:
        interpreter.throw_exception<TypeError>("Can't convert symbol to number");
        return {};
    case Type::Object: {
        auto primitive = m_value.as_object->to_primitive(PreferredType::Number);
        if (interpreter.exception())
            return {};
        return primitive.to_number(interpreter);
    }
    default:
        ASSERT_NOT_REACHED();
    }
}

i32 Value::as_i32() const
{
    return static_cast<i32>(as_double());
}

size_t Value::as_size_t() const
{
    ASSERT(as_double() >= 0);
    return min((double)as_i32(), MAX_ARRAY_LIKE_INDEX);
}

double Value::to_double(Interpreter& interpreter) const
{
    auto number = to_number(interpreter);
    if (interpreter.exception())
        return 0;
    return number.as_double();
}

i32 Value::to_i32(Interpreter& interpreter) const
{
    auto number = to_number(interpreter);
    if (interpreter.exception())
        return 0;
    if (number.is_nan())
        return 0;
    // FIXME: What about infinity though - that's UB...
    // Maybe NumericLimits<i32>::max() for +Infinity and NumericLimits<i32>::min() for -Infinity?
    return number.as_i32();
}

size_t Value::to_size_t(Interpreter& interpreter) const
{
    if (is_empty())
        return 0;
    auto number = to_number(interpreter);
    if (interpreter.exception())
        return 0;
    if (number.is_nan())
        return 0;
    if (number.as_double() <= 0)
        return 0;
    return number.as_size_t();
}

Value greater_than(Interpreter& interpreter, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(interpreter, false, lhs, rhs);
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

Value greater_than_equals(Interpreter& interpreter, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(interpreter, true, lhs, rhs);
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

Value less_than(Interpreter& interpreter, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(interpreter, true, lhs, rhs);
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

Value less_than_equals(Interpreter& interpreter, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(interpreter, false, lhs, rhs);
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

Value bitwise_and(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value((i32)lhs_number.as_double() & (i32)rhs_number.as_double());
}

Value bitwise_or(Interpreter& interpreter, Value lhs, Value rhs)
{
    bool lhs_invalid = lhs.is_undefined() || lhs.is_null() || lhs.is_nan() || lhs.is_infinity();
    bool rhs_invalid = rhs.is_undefined() || rhs.is_null() || rhs.is_nan() || rhs.is_infinity();

    if (lhs_invalid && rhs_invalid)
        return Value(0);

    if (lhs_invalid || rhs_invalid)
        return lhs_invalid ? rhs.to_number(interpreter) : lhs.to_number(interpreter);

    if (!rhs.is_number() && !lhs.is_number())
        return Value(0);

    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value((i32)lhs_number.as_double() | (i32)rhs_number.as_double());
}

Value bitwise_xor(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value((i32)lhs_number.as_double() ^ (i32)rhs_number.as_double());
}

Value bitwise_not(Interpreter& interpreter, Value lhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value(~(i32)lhs_number.as_double());
}

Value unary_plus(Interpreter& interpreter, Value lhs)
{
    return lhs.to_number(interpreter);
}

Value unary_minus(Interpreter& interpreter, Value lhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (lhs_number.is_nan())
        return js_nan();
    return Value(-lhs_number.as_double());
}

Value left_shift(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (!lhs_number.is_finite_number())
        return Value(0);
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (!rhs_number.is_finite_number())
        return lhs_number;
    return Value((i32)lhs_number.as_double() << (i32)rhs_number.as_double());
}

Value right_shift(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (!lhs_number.is_finite_number())
        return Value(0);
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (!rhs_number.is_finite_number())
        return lhs_number;
    return Value((i32)lhs_number.as_double() >> (i32)rhs_number.as_double());
}

Value unsigned_right_shift(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (!lhs_number.is_finite_number())
        return Value(0);
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (!rhs_number.is_finite_number())
        return lhs_number;
    return Value((unsigned)lhs_number.as_double() >> (i32)rhs_number.as_double());
}

Value add(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_primitive = lhs.to_primitive(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_primitive = rhs.to_primitive(interpreter);
    if (interpreter.exception())
        return {};

    if (lhs_primitive.is_string() || rhs_primitive.is_string()) {
        auto lhs_string = lhs_primitive.to_string(interpreter);
        if (interpreter.exception())
            return {};
        auto rhs_string = rhs_primitive.to_string(interpreter);
        if (interpreter.exception())
            return {};
        StringBuilder builder(lhs_string.length() + rhs_string.length());
        builder.append(lhs_string);
        builder.append(rhs_string);
        return js_string(interpreter, builder.to_string());
    }

    auto lhs_number = lhs_primitive.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs_primitive.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value(lhs_number.as_double() + rhs_number.as_double());
}

Value sub(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value(lhs_number.as_double() - rhs_number.as_double());
}

Value mul(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value(lhs_number.as_double() * rhs_number.as_double());
}

Value div(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value(lhs_number.as_double() / rhs_number.as_double());
}

Value mod(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (lhs_number.is_nan() || rhs_number.is_nan())
        return js_nan();
    auto index = lhs_number.as_double();
    auto period = rhs_number.as_double();
    auto trunc = (double)(i32)(index / period);
    return Value(index - trunc * period);
}

Value exp(Interpreter& interpreter, Value lhs, Value rhs)
{
    auto lhs_number = lhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto rhs_number = rhs.to_number(interpreter);
    if (interpreter.exception())
        return {};
    return Value(pow(lhs_number.as_double(), rhs_number.as_double()));
}

Value in(Interpreter& interpreter, Value lhs, Value rhs)
{
    if (!rhs.is_object())
        return interpreter.throw_exception<TypeError>("'in' operator must be used on object");
    auto lhs_string = lhs.to_string(interpreter);
    if (interpreter.exception())
        return {};
    return Value(rhs.as_object().has_property(lhs_string));
}

Value instance_of(Interpreter&, Value lhs, Value rhs)
{
    if (!lhs.is_object() || !rhs.is_object())
        return Value(false);
    auto constructor_prototype_property = rhs.as_object().get("prototype");
    if (!constructor_prototype_property.is_object())
        return Value(false);
    return Value(lhs.as_object().has_prototype(&constructor_prototype_property.as_object()));
}

const LogStream& operator<<(const LogStream& stream, const Value& value)
{
    return stream << (value.is_empty() ? "<empty>" : value.to_string_without_side_effects());
}

bool same_value(Interpreter& interpreter, Value lhs, Value rhs)
{
    if (lhs.type() != rhs.type())
        return false;

    if (lhs.is_number()) {
        if (lhs.is_nan() && rhs.is_nan())
            return true;
        if (lhs.is_positive_zero() && rhs.is_negative_zero())
            return false;
        if (lhs.is_negative_zero() && rhs.is_positive_zero())
            return false;
        return lhs.as_double() == rhs.as_double();
    }

    return same_value_non_numeric(interpreter, lhs, rhs);
}

bool same_value_zero(Interpreter& interpreter, Value lhs, Value rhs)
{
    if (lhs.type() != rhs.type())
        return false;

    if (lhs.is_number()) {
        if (lhs.is_nan() && rhs.is_nan())
            return true;
        if ((lhs.is_positive_zero() || lhs.is_negative_zero()) && (rhs.is_positive_zero() || rhs.is_negative_zero()))
            return true;
        return lhs.as_double() == rhs.as_double();
    }

    return same_value_non_numeric(interpreter, lhs, rhs);
}

bool same_value_non_numeric(Interpreter&, Value lhs, Value rhs)
{
    ASSERT(!lhs.is_number());
    ASSERT(lhs.type() == rhs.type());

    switch (lhs.type()) {
    case Value::Type::Undefined:
    case Value::Type::Null:
        return true;
    case Value::Type::String:
        return lhs.as_string().string() == rhs.as_string().string();
    case Value::Type::Symbol:
        return &lhs.as_symbol() == &rhs.as_symbol();
    case Value::Type::Boolean:
        return lhs.as_bool() == rhs.as_bool();
    case Value::Type::Object:
        return &lhs.as_object() == &rhs.as_object();
    default:
        ASSERT_NOT_REACHED();
    }
}

bool strict_eq(Interpreter& interpreter, Value lhs, Value rhs)
{
    if (lhs.type() != rhs.type())
        return false;

    if (lhs.is_number()) {
        if (lhs.is_nan() || rhs.is_nan())
            return false;
        if (lhs.as_double() == rhs.as_double())
            return true;
        if ((lhs.is_positive_zero() || lhs.is_negative_zero()) && (rhs.is_positive_zero() || rhs.is_negative_zero()))
            return true;
        return false;
    }

    return same_value_non_numeric(interpreter, lhs, rhs);
}

bool abstract_eq(Interpreter& interpreter, Value lhs, Value rhs)
{
    if (lhs.type() == rhs.type())
        return strict_eq(interpreter, lhs, rhs);

    if ((lhs.is_undefined() || lhs.is_null()) && (rhs.is_undefined() || rhs.is_null()))
        return true;

    if (lhs.is_number() && rhs.is_string())
        return abstract_eq(interpreter, lhs, rhs.to_number(interpreter));

    if (lhs.is_string() && rhs.is_number())
        return abstract_eq(interpreter, lhs.to_number(interpreter), rhs);

    if (lhs.is_boolean())
        return abstract_eq(interpreter, lhs.to_number(interpreter), rhs);

    if (rhs.is_boolean())
        return abstract_eq(interpreter, lhs, rhs.to_number(interpreter));

    if ((lhs.is_string() || lhs.is_number() || lhs.is_symbol()) && rhs.is_object())
        return abstract_eq(interpreter, lhs, rhs.to_primitive(interpreter));

    if (lhs.is_object() && (rhs.is_string() || rhs.is_number() || rhs.is_symbol()))
        return abstract_eq(interpreter, lhs.to_primitive(interpreter), rhs);

    return false;
}

TriState abstract_relation(Interpreter& interpreter, bool left_first, Value lhs, Value rhs)
{
    Value x_primitive = {};
    Value y_primitive = {};

    if (left_first) {
        x_primitive = lhs.to_primitive(interpreter, Value::PreferredType::Number);
        if (interpreter.exception())
            return {};
        y_primitive = rhs.to_primitive(interpreter, Value::PreferredType::Number);
        if (interpreter.exception())
            return {};
    } else {
        y_primitive = lhs.to_primitive(interpreter, Value::PreferredType::Number);
        if (interpreter.exception())
            return {};
        x_primitive = rhs.to_primitive(interpreter, Value::PreferredType::Number);
        if (interpreter.exception())
            return {};
    }

    if (x_primitive.is_string() && y_primitive.is_string()) {
        auto x_string = x_primitive.as_string().string();
        auto y_string = y_primitive.as_string().string();

        if (x_string.starts_with(y_string))
            return TriState::False;
        if (y_string.starts_with(x_string))
            return TriState::True;

        Utf8View x_codepoints { x_string };
        Utf8View y_codepoints { y_string };
        for (auto k = x_codepoints.begin(), l = y_codepoints.begin();
             k != x_codepoints.end() && l != y_codepoints.end();
             ++k, ++l) {
            if (*k != *l) {
                if (*k < *l) {
                    return TriState::True;
                } else {
                    return TriState::False;
                }
            }
        }
        ASSERT_NOT_REACHED();
    }

    // FIXME add BigInt cases here once we have BigInt

    auto x_numeric = x_primitive.to_number(interpreter);
    if (interpreter.exception())
        return {};
    auto y_numeric = y_primitive.to_number(interpreter);
    if (interpreter.exception())
        return {};

    if (x_numeric.is_nan() || y_numeric.is_nan())
        return TriState::Unknown;

    if (x_numeric.is_positive_infinity() || y_numeric.is_negative_infinity())
        return TriState::False;

    if (x_numeric.is_negative_infinity() || y_numeric.is_positive_infinity())
        return TriState::True;

    auto x_value = x_numeric.as_double();
    auto y_value = y_numeric.as_double();

    if (x_value < y_value)
        return TriState::True;
    else
        return TriState::False;
}

}
