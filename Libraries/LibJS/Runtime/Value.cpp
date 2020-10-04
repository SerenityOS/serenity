/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/Value.h>
#include <ctype.h>
#include <math.h>

namespace JS {

static const Crypto::SignedBigInteger BIGINT_ZERO { 0 };

static bool is_valid_bigint_value(String string)
{
    string = string.trim_whitespace();
    if (string.length() > 1 && (string[0] == '-' || string[0] == '+'))
        string = string.substring_view(1, string.length() - 1);
    for (auto& ch : string) {
        if (!isdigit(ch))
            return false;
    }
    return true;
}

ALWAYS_INLINE bool both_number(const Value& lhs, const Value& rhs)
{
    return lhs.is_number() && rhs.is_number();
}

ALWAYS_INLINE bool both_bigint(const Value& lhs, const Value& rhs)
{
    return lhs.is_bigint() && rhs.is_bigint();
}

bool Value::is_array() const
{
    return is_object() && as_object().is_array();
}

Array& Value::as_array()
{
    ASSERT(is_array());
    return static_cast<Array&>(*m_value.as_object);
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
    case Type::BigInt:
        return m_value.as_bigint->to_string();
    case Type::Object:
        return String::formatted("[object {}]", as_object().class_name());
    case Type::Accessor:
        return "<accessor>";
    case Type::NativeProperty:
        return "<native-property>";
    default:
        ASSERT_NOT_REACHED();
    }
}

PrimitiveString* Value::to_primitive_string(GlobalObject& global_object)
{
    if (is_string())
        return &as_string();
    auto string = to_string(global_object);
    if (global_object.vm().exception())
        return nullptr;
    return js_string(global_object.heap(), string);
}

String Value::to_string(GlobalObject& global_object) const
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
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::Convert, "symbol", "string");
        return {};
    case Type::BigInt:
        return m_value.as_bigint->big_integer().to_base10();
    case Type::Object: {
        auto primitive_value = as_object().to_primitive(PreferredType::String);
        if (global_object.vm().exception())
            return {};
        return primitive_value.to_string(global_object);
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
        return true;
    case Type::BigInt:
        return m_value.as_bigint->big_integer() != BIGINT_ZERO;
    case Type::Object:
        return true;
    default:
        ASSERT_NOT_REACHED();
    }
}

Value Value::to_primitive(PreferredType preferred_type) const
{
    if (is_object())
        return as_object().to_primitive(preferred_type);
    return *this;
}

Object* Value::to_object(GlobalObject& global_object) const
{
    switch (m_type) {
    case Type::Undefined:
    case Type::Null:
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::ToObjectNullOrUndef);
        return nullptr;
    case Type::Boolean:
        return BooleanObject::create(global_object, m_value.as_bool);
    case Type::Number:
        return NumberObject::create(global_object, m_value.as_double);
    case Type::String:
        return StringObject::create(global_object, *m_value.as_string);
    case Type::Symbol:
        return SymbolObject::create(global_object, *m_value.as_symbol);
    case Type::BigInt:
        return BigIntObject::create(global_object, *m_value.as_bigint);
    case Type::Object:
        return &const_cast<Object&>(as_object());
    default:
        dbg() << "Dying because I can't to_object() on " << *this;
        ASSERT_NOT_REACHED();
    }
}

Value Value::to_numeric(GlobalObject& global_object) const
{
    auto primitive = to_primitive(Value::PreferredType::Number);
    if (global_object.vm().exception())
        return {};
    if (primitive.is_bigint())
        return primitive;
    return primitive.to_number(global_object);
}

Value Value::to_number(GlobalObject& global_object) const
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
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::Convert, "symbol", "number");
        return {};
    case Type::BigInt:
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::Convert, "BigInt", "number");
        return {};
    case Type::Object: {
        auto primitive = m_value.as_object->to_primitive(PreferredType::Number);
        if (global_object.vm().exception())
            return {};
        return primitive.to_number(global_object);
    }
    default:
        ASSERT_NOT_REACHED();
    }
}

BigInt* Value::to_bigint(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();
    auto primitive = to_primitive(PreferredType::Number);
    if (vm.exception())
        return nullptr;
    switch (primitive.type()) {
    case Type::Undefined:
        vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "undefined", "BigInt");
        return nullptr;
    case Type::Null:
        vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "null", "BigInt");
        return nullptr;
    case Type::Boolean: {
        auto value = primitive.as_bool() ? 1 : 0;
        return js_bigint(vm.heap(), Crypto::SignedBigInteger { value });
    }
    case Type::BigInt:
        return &primitive.as_bigint();
    case Type::Number:
        vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "number", "BigInt");
        return {};
    case Type::String: {
        auto& string = primitive.as_string().string();
        if (!is_valid_bigint_value(string)) {
            vm.throw_exception<SyntaxError>(global_object, ErrorType::BigIntInvalidValue, string);
            return {};
        }
        return js_bigint(vm.heap(), Crypto::SignedBigInteger::from_base10(string.trim_whitespace()));
    }
    case Type::Symbol:
        vm.throw_exception<TypeError>(global_object, ErrorType::Convert, "symbol", "BigInt");
        return {};
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

double Value::to_double(GlobalObject& global_object) const
{
    auto number = to_number(global_object);
    if (global_object.vm().exception())
        return 0;
    return number.as_double();
}

i32 Value::to_i32(GlobalObject& global_object) const
{
    auto number = to_number(global_object);
    if (global_object.vm().exception())
        return 0;
    if (number.is_nan())
        return 0;
    // FIXME: What about infinity though - that's UB...
    // Maybe NumericLimits<i32>::max() for +Infinity and NumericLimits<i32>::min() for -Infinity?
    return number.as_i32();
}

size_t Value::to_size_t(GlobalObject& global_object) const
{
    if (is_empty())
        return 0;
    auto number = to_number(global_object);
    if (global_object.vm().exception())
        return 0;
    if (number.is_nan())
        return 0;
    if (number.as_double() <= 0)
        return 0;
    return number.as_size_t();
}

Value greater_than(GlobalObject& global_object, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(global_object, false, lhs, rhs);
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

Value greater_than_equals(GlobalObject& global_object, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(global_object, true, lhs, rhs);
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

Value less_than(GlobalObject& global_object, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(global_object, true, lhs, rhs);
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

Value less_than_equals(GlobalObject& global_object, Value lhs, Value rhs)
{
    TriState relation = abstract_relation(global_object, false, lhs, rhs);
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

Value bitwise_and(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() || !rhs_numeric.is_finite_number())
            return Value(0);
        return Value((i32)lhs_numeric.as_double() & (i32)rhs_numeric.as_double());
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().bitwise_and(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "bitwise AND");
    return {};
}

Value bitwise_or(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value((i32)lhs_numeric.as_double() | (i32)rhs_numeric.as_double());
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().bitwise_or(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "bitwise OR");
    return {};
}

Value bitwise_xor(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value((i32)lhs_numeric.as_double() ^ (i32)rhs_numeric.as_double());
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().bitwise_xor(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "bitwise XOR");
    return {};
}

Value bitwise_not(GlobalObject& global_object, Value lhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (lhs_numeric.is_number())
        return Value(~(i32)lhs_numeric.as_double());
    auto big_integer_bitwise_not = lhs_numeric.as_bigint().big_integer();
    big_integer_bitwise_not = big_integer_bitwise_not.plus(Crypto::SignedBigInteger { 1 });
    big_integer_bitwise_not.negate();
    return js_bigint(global_object.heap(), big_integer_bitwise_not);
}

Value unary_plus(GlobalObject& global_object, Value lhs)
{
    return lhs.to_number(global_object.global_object());
}

Value unary_minus(GlobalObject& global_object, Value lhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (lhs_numeric.is_number()) {
        if (lhs_numeric.is_nan())
            return js_nan();
        return Value(-lhs_numeric.as_double());
    }
    if (lhs_numeric.as_bigint().big_integer() == BIGINT_ZERO)
        return js_bigint(global_object.heap(), BIGINT_ZERO);
    auto big_integer_negated = lhs_numeric.as_bigint().big_integer();
    big_integer_negated.negate();
    return js_bigint(global_object.heap(), big_integer_negated);
}

Value left_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value((i32)lhs_numeric.as_double() << (i32)rhs_numeric.as_double());
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        TODO();
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "left-shift");
    return {};
}

Value right_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value((i32)lhs_numeric.as_double() >> (i32)rhs_numeric.as_double());
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        TODO();
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "right-shift");
    return {};
}

Value unsigned_right_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value((unsigned)lhs_numeric.as_double() >> (i32)rhs_numeric.as_double());
    }
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperator, "unsigned right-shift");
    return {};
}

Value add(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_primitive = lhs.to_primitive();
    if (global_object.vm().exception())
        return {};
    auto rhs_primitive = rhs.to_primitive();
    if (global_object.vm().exception())
        return {};

    if (lhs_primitive.is_string() || rhs_primitive.is_string()) {
        auto lhs_string = lhs_primitive.to_string(global_object.global_object());
        if (global_object.vm().exception())
            return {};
        auto rhs_string = rhs_primitive.to_string(global_object.global_object());
        if (global_object.vm().exception())
            return {};
        StringBuilder builder(lhs_string.length() + rhs_string.length());
        builder.append(lhs_string);
        builder.append(rhs_string);
        return js_string(global_object.heap(), builder.to_string());
    }

    auto lhs_numeric = lhs_primitive.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs_primitive.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() + rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().plus(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "addition");
    return {};
}

Value sub(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() - rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().minus(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "subtraction");
    return {};
}

Value mul(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() * rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().multiplied_by(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "multiplication");
    return {};
}

Value div(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() / rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).quotient);
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "division");
    return {};
}

Value mod(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (lhs_numeric.is_nan() || rhs_numeric.is_nan())
            return js_nan();
        auto index = lhs_numeric.as_double();
        auto period = rhs_numeric.as_double();
        auto trunc = (double)(i32)(index / period);
        return Value(index - trunc * period);
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).remainder);
    global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::BigIntBadOperatorOtherType, "modulo");
    return {};
}

Value exp(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (vm.exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (vm.exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(pow(lhs_numeric.as_double(), rhs_numeric.as_double()));
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(vm.heap(), Crypto::NumberTheory::Power(lhs_numeric.as_bigint().big_integer(), rhs_numeric.as_bigint().big_integer()));
    vm.throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "exponentiation");
    return {};
}

Value in(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (!rhs.is_object()) {
        global_object.vm().throw_exception<TypeError>(global_object.global_object(), ErrorType::InOperatorWithObject);
        return {};
    }
    auto lhs_string = lhs.to_string(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    return Value(rhs.as_object().has_property(lhs_string));
}

Value instance_of(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    if (!rhs.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, rhs.to_string_without_side_effects());
        return {};
    }
    auto has_instance_method = rhs.as_object().get(vm.well_known_symbol_has_instance());
    if (!has_instance_method.is_empty()) {
        if (!has_instance_method.is_function()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, has_instance_method.to_string_without_side_effects());
            return {};
        }

        return Value(vm.call(has_instance_method.as_function(), rhs, lhs).to_boolean());
    }

    if (!rhs.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, rhs.to_string_without_side_effects());
        return {};
    }
    return ordinary_has_instance(global_object, lhs, rhs);
}

Value ordinary_has_instance(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    if (!rhs.is_function())
        return Value(false);
    auto& rhs_function = rhs.as_function();

    if (rhs_function.is_bound_function()) {
        auto& bound_target = static_cast<BoundFunction&>(rhs_function);
        return instance_of(global_object, lhs, Value(&bound_target.target_function()));
    }

    if (!lhs.is_object())
        return Value(false);

    Object* lhs_object = &lhs.as_object();
    auto rhs_prototype = rhs_function.get("prototype");
    if (vm.exception())
        return {};

    if (!rhs_prototype.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::InstanceOfOperatorBadPrototype, rhs_prototype.to_string_without_side_effects());
        return {};
    }
    while (true) {
        lhs_object = lhs_object->prototype();
        if (vm.exception())
            return {};
        if (!lhs_object)
            return Value(false);
        if (same_value(rhs_prototype, lhs_object))
            return Value(true);
    }
}

const LogStream& operator<<(const LogStream& stream, const Value& value)
{
    return stream << (value.is_empty() ? "<empty>" : value.to_string_without_side_effects());
}

bool same_value(Value lhs, Value rhs)
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

    if (lhs.is_bigint()) {
        auto lhs_big_integer = lhs.as_bigint().big_integer();
        auto rhs_big_integer = rhs.as_bigint().big_integer();
        if (lhs_big_integer == BIGINT_ZERO && rhs_big_integer == BIGINT_ZERO && lhs_big_integer.is_negative() != rhs_big_integer.is_negative())
            return false;
        return lhs_big_integer == rhs_big_integer;
    }

    return same_value_non_numeric(lhs, rhs);
}

bool same_value_zero(Value lhs, Value rhs)
{
    if (lhs.type() != rhs.type())
        return false;

    if (lhs.is_number()) {
        if (lhs.is_nan() && rhs.is_nan())
            return true;
        return lhs.as_double() == rhs.as_double();
    }

    if (lhs.is_bigint())
        return lhs.as_bigint().big_integer() == rhs.as_bigint().big_integer();

    return same_value_non_numeric(lhs, rhs);
}

bool same_value_non_numeric(Value lhs, Value rhs)
{
    ASSERT(!lhs.is_number() && !lhs.is_bigint());
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

bool strict_eq(Value lhs, Value rhs)
{
    if (lhs.type() != rhs.type())
        return false;

    if (lhs.is_number()) {
        if (lhs.is_nan() || rhs.is_nan())
            return false;
        if (lhs.as_double() == rhs.as_double())
            return true;
        return false;
    }

    if (lhs.is_bigint())
        return lhs.as_bigint().big_integer() == rhs.as_bigint().big_integer();

    return same_value_non_numeric(lhs, rhs);
}

bool abstract_eq(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (lhs.type() == rhs.type())
        return strict_eq(lhs, rhs);

    if (lhs.is_nullish() && rhs.is_nullish())
        return true;

    if (lhs.is_number() && rhs.is_string())
        return abstract_eq(global_object, lhs, rhs.to_number(global_object.global_object()));

    if (lhs.is_string() && rhs.is_number())
        return abstract_eq(global_object, lhs.to_number(global_object.global_object()), rhs);

    if (lhs.is_bigint() && rhs.is_string()) {
        auto& rhs_string = rhs.as_string().string();
        if (!is_valid_bigint_value(rhs_string))
            return false;
        return abstract_eq(global_object, lhs, js_bigint(global_object.heap(), Crypto::SignedBigInteger::from_base10(rhs_string)));
    }

    if (lhs.is_string() && rhs.is_bigint())
        return abstract_eq(global_object, rhs, lhs);

    if (lhs.is_boolean())
        return abstract_eq(global_object, lhs.to_number(global_object.global_object()), rhs);

    if (rhs.is_boolean())
        return abstract_eq(global_object, lhs, rhs.to_number(global_object.global_object()));

    if ((lhs.is_string() || lhs.is_number() || lhs.is_bigint() || lhs.is_symbol()) && rhs.is_object())
        return abstract_eq(global_object, lhs, rhs.to_primitive());

    if (lhs.is_object() && (rhs.is_string() || rhs.is_number() || lhs.is_bigint() || rhs.is_symbol()))
        return abstract_eq(global_object, lhs.to_primitive(), rhs);

    if ((lhs.is_bigint() && rhs.is_number()) || (lhs.is_number() && rhs.is_bigint())) {
        if (lhs.is_nan() || lhs.is_infinity() || rhs.is_nan() || rhs.is_infinity())
            return false;
        if ((lhs.is_number() && !lhs.is_integer()) || (rhs.is_number() && !rhs.is_integer()))
            return false;
        if (lhs.is_number())
            return Crypto::SignedBigInteger { lhs.as_i32() } == rhs.as_bigint().big_integer();
        else
            return Crypto::SignedBigInteger { rhs.as_i32() } == lhs.as_bigint().big_integer();
    }

    return false;
}

TriState abstract_relation(GlobalObject& global_object, bool left_first, Value lhs, Value rhs)
{
    Value x_primitive;
    Value y_primitive;

    if (left_first) {
        x_primitive = lhs.to_primitive(Value::PreferredType::Number);
        if (global_object.vm().exception())
            return {};
        y_primitive = rhs.to_primitive(Value::PreferredType::Number);
        if (global_object.vm().exception())
            return {};
    } else {
        y_primitive = lhs.to_primitive(Value::PreferredType::Number);
        if (global_object.vm().exception())
            return {};
        x_primitive = rhs.to_primitive(Value::PreferredType::Number);
        if (global_object.vm().exception())
            return {};
    }

    if (x_primitive.is_string() && y_primitive.is_string()) {
        auto x_string = x_primitive.as_string().string();
        auto y_string = y_primitive.as_string().string();

        if (x_string.starts_with(y_string))
            return TriState::False;
        if (y_string.starts_with(x_string))
            return TriState::True;

        Utf8View x_code_points { x_string };
        Utf8View y_code_points { y_string };
        for (auto k = x_code_points.begin(), l = y_code_points.begin();
             k != x_code_points.end() && l != y_code_points.end();
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

    if (x_primitive.is_bigint() && y_primitive.is_string()) {
        auto& y_string = y_primitive.as_string().string();
        if (!is_valid_bigint_value(y_string))
            return TriState::Unknown;
        if (x_primitive.as_bigint().big_integer() < Crypto::SignedBigInteger::from_base10(y_string))
            return TriState::True;
        else
            return TriState::False;
    }

    if (x_primitive.is_string() && y_primitive.is_bigint()) {
        auto& x_string = x_primitive.as_string().string();
        if (!is_valid_bigint_value(x_string))
            return TriState::Unknown;
        if (Crypto::SignedBigInteger::from_base10(x_string) < y_primitive.as_bigint().big_integer())
            return TriState::True;
        else
            return TriState::False;
    }

    auto x_numeric = x_primitive.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};
    auto y_numeric = y_primitive.to_numeric(global_object.global_object());
    if (global_object.vm().exception())
        return {};

    if (x_numeric.is_nan() || y_numeric.is_nan())
        return TriState::Unknown;

    if (x_numeric.is_positive_infinity() || y_numeric.is_negative_infinity())
        return TriState::False;

    if (x_numeric.is_negative_infinity() || y_numeric.is_positive_infinity())
        return TriState::True;

    if (x_numeric.is_number() && y_numeric.is_number()) {
        if (x_numeric.as_double() < y_numeric.as_double())
            return TriState::True;
        else
            return TriState::False;
    }

    if (x_numeric.is_bigint() && y_numeric.is_bigint()) {
        if (x_numeric.as_bigint().big_integer() < y_numeric.as_bigint().big_integer())
            return TriState::True;
        else
            return TriState::False;
    }

    ASSERT((x_numeric.is_number() && y_numeric.is_bigint()) || (x_numeric.is_bigint() && y_numeric.is_number()));

    bool x_lower_than_y;
    if (x_numeric.is_number()) {
        x_lower_than_y = x_numeric.is_integer()
            ? Crypto::SignedBigInteger { x_numeric.as_i32() } < y_numeric.as_bigint().big_integer()
            : (Crypto::SignedBigInteger { x_numeric.as_i32() } < y_numeric.as_bigint().big_integer() || Crypto::SignedBigInteger { x_numeric.as_i32() + 1 } < y_numeric.as_bigint().big_integer());
    } else {
        x_lower_than_y = y_numeric.is_integer()
            ? x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { y_numeric.as_i32() }
            : (x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { y_numeric.as_i32() } || x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { y_numeric.as_i32() + 1 });
    }
    if (x_lower_than_y)
        return TriState::True;
    else
        return TriState::False;
}

size_t length_of_array_like(GlobalObject& global_object, Value value)
{
    ASSERT(value.is_object());
    auto result = value.as_object().get("length");
    if (global_object.vm().exception())
        return 0;
    return result.to_size_t(global_object);
}

}
