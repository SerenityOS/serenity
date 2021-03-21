/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <mail@linusgroh.de>
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

#include <AK/AllOf.h>
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
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/Symbol.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/Value.h>
#include <ctype.h>
#include <math.h>

namespace JS {

// Used in various abstract operations to make it obvious when a non-optional return value must be discarded.
static const double INVALID { 0 };

static inline bool same_type_for_equality(const Value& lhs, const Value& rhs)
{
    if (lhs.type() == rhs.type())
        return true;
    if (lhs.is_number() && rhs.is_number())
        return true;
    return false;
}

static const Crypto::SignedBigInteger BIGINT_ZERO { 0 };

static bool is_valid_bigint_value(StringView string)
{
    string = string.trim_whitespace();
    if (string.length() > 1 && (string[0] == '-' || string[0] == '+'))
        string = string.substring_view(1, string.length() - 1);
    return all_of(string.begin(), string.end(), [](auto ch) { return isdigit(ch); });
}

ALWAYS_INLINE bool both_number(const Value& lhs, const Value& rhs)
{
    return lhs.is_number() && rhs.is_number();
}

ALWAYS_INLINE bool both_bigint(const Value& lhs, const Value& rhs)
{
    return lhs.is_bigint() && rhs.is_bigint();
}

static String double_to_string(double d)
{
    // https://tc39.es/ecma262/#sec-numeric-types-number-tostring
    if (isnan(d))
        return "NaN";
    if (d == +0.0 || d == -0.0)
        return "0";
    if (d < +0.0) {
        StringBuilder builder;
        builder.append('-');
        builder.append(double_to_string(-d));
        return builder.to_string();
    }
    if (d == INFINITY)
        return "Infinity";

    StringBuilder number_string_builder;

    size_t start_index = 0;
    size_t end_index = 0;
    size_t int_part_end = 0;

    // generate integer part (reversed)
    double int_part;
    double frac_part;
    frac_part = modf(d, &int_part);
    while (int_part > 0) {
        number_string_builder.append('0' + (int)fmod(int_part, 10));
        end_index++;
        int_part = floor(int_part / 10);
    }

    auto reversed_integer_part = number_string_builder.to_string().reverse();
    number_string_builder.clear();
    number_string_builder.append(reversed_integer_part);

    int_part_end = end_index;

    int exponent = 0;

    // generate fractional part
    while (frac_part > 0) {
        double old_frac_part = frac_part;
        frac_part *= 10;
        frac_part = modf(frac_part, &int_part);
        if (old_frac_part == frac_part)
            break;
        number_string_builder.append('0' + (int)int_part);
        end_index++;
        exponent--;
    }

    auto number_string = number_string_builder.to_string();

    // FIXME: Remove this hack.
    // FIXME: Instead find the shortest round-trippable representation.
    // Remove decimals after the 15th position
    if (end_index > int_part_end + 15) {
        exponent += end_index - int_part_end - 15;
        end_index = int_part_end + 15;
    }

    // remove leading zeroes
    while (start_index < end_index && number_string[start_index] == '0') {
        start_index++;
    }

    // remove trailing zeroes
    while (end_index > 0 && number_string[end_index - 1] == '0') {
        end_index--;
        exponent++;
    }

    if (end_index <= start_index)
        return "0";

    auto digits = number_string.substring_view(start_index, end_index - start_index);

    int number_of_digits = end_index - start_index;

    exponent += number_of_digits;

    StringBuilder builder;

    if (number_of_digits <= exponent && exponent <= 21) {
        builder.append(digits);
        builder.append(String::repeated('0', exponent - number_of_digits));
        return builder.to_string();
    }
    if (0 < exponent && exponent <= 21) {
        builder.append(digits.substring_view(0, exponent));
        builder.append('.');
        builder.append(digits.substring_view(exponent));
        return builder.to_string();
    }
    if (-6 < exponent && exponent <= 0) {
        builder.append("0.");
        builder.append(String::repeated('0', -exponent));
        builder.append(digits);
        return builder.to_string();
    }
    if (number_of_digits == 1) {
        builder.append(digits);
        builder.append('e');

        if (exponent - 1 > 0)
            builder.append('+');
        else
            builder.append('-');

        builder.append(String::number(fabs(exponent - 1)));
        return builder.to_string();
    }

    builder.append(digits[0]);
    builder.append('.');
    builder.append(digits.substring_view(1));
    builder.append('e');

    if (exponent - 1 > 0)
        builder.append('+');
    else
        builder.append('-');

    builder.append(String::number(fabs(exponent - 1)));
    return builder.to_string();
}

bool Value::is_array() const
{
    return is_object() && as_object().is_array();
}

Array& Value::as_array()
{
    VERIFY(is_array());
    return static_cast<Array&>(*m_value.as_object);
}

bool Value::is_function() const
{
    return is_object() && as_object().is_function();
}

Function& Value::as_function()
{
    VERIFY(is_function());
    return static_cast<Function&>(as_object());
}

bool Value::is_regexp(GlobalObject& global_object) const
{
    // 7.2.8 IsRegExp, https://tc39.es/ecma262/#sec-isregexp

    if (!is_object())
        return false;

    auto matcher = as_object().get(global_object.vm().well_known_symbol_match());
    if (global_object.vm().exception())
        return false;
    if (!matcher.is_empty() && !matcher.is_undefined())
        return matcher.to_boolean();

    return is<RegExpObject>(as_object());
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
    case Type::Int32:
        return String::number(m_value.as_i32);
    case Type::Double:
        return double_to_string(m_value.as_double);
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
        VERIFY_NOT_REACHED();
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

String Value::to_string(GlobalObject& global_object, bool legacy_null_to_empty_string) const
{
    switch (m_type) {
    case Type::Undefined:
        return "undefined";
    case Type::Null:
        return !legacy_null_to_empty_string ? "null" : String::empty();
    case Type::Boolean:
        return m_value.as_bool ? "true" : "false";
    case Type::Int32:
        return String::number(m_value.as_i32);
    case Type::Double:
        return double_to_string(m_value.as_double);
    case Type::String:
        return m_value.as_string->string();
    case Type::Symbol:
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::Convert, "symbol", "string");
        return {};
    case Type::BigInt:
        return m_value.as_bigint->big_integer().to_base10();
    case Type::Object: {
        auto primitive_value = to_primitive(global_object, PreferredType::String);
        if (global_object.vm().exception())
            return {};
        return primitive_value.to_string(global_object);
    }
    default:
        VERIFY_NOT_REACHED();
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
    case Type::Int32:
        return m_value.as_i32 != 0;
    case Type::Double:
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
        VERIFY_NOT_REACHED();
    }
}

Value Value::to_primitive(GlobalObject& global_object, PreferredType preferred_type) const
{
    auto get_hint_for_preferred_type = [&]() -> String {
        switch (preferred_type) {
        case PreferredType::Default:
            return "default";
        case PreferredType::String:
            return "string";
        case PreferredType::Number:
            return "number";
        default:
            VERIFY_NOT_REACHED();
        }
    };
    if (is_object()) {
        auto& vm = global_object.vm();
        auto to_primitive_method = get_method(global_object, *this, vm.well_known_symbol_to_primitive());
        if (vm.exception())
            return {};
        if (to_primitive_method) {
            auto hint = get_hint_for_preferred_type();
            auto result = vm.call(*to_primitive_method, *this, js_string(vm, hint));
            if (vm.exception())
                return {};
            if (!result.is_object())
                return result;
            vm.throw_exception<TypeError>(global_object, ErrorType::ToPrimitiveReturnedObject, to_string_without_side_effects(), hint);
            return {};
        }
        if (preferred_type == PreferredType::Default)
            preferred_type = PreferredType::Number;
        return as_object().ordinary_to_primitive(preferred_type);
    }
    return *this;
}

Object* Value::to_object(GlobalObject& global_object) const
{
    switch (m_type) {
    case Type::Undefined:
    case Type::Null:
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::ToObjectNullOrUndefined);
        return nullptr;
    case Type::Boolean:
        return BooleanObject::create(global_object, m_value.as_bool);
    case Type::Int32:
    case Type::Double:
        return NumberObject::create(global_object, as_double());
    case Type::String:
        return StringObject::create(global_object, *m_value.as_string);
    case Type::Symbol:
        return SymbolObject::create(global_object, *m_value.as_symbol);
    case Type::BigInt:
        return BigIntObject::create(global_object, *m_value.as_bigint);
    case Type::Object:
        return &const_cast<Object&>(as_object());
    default:
        dbgln("Dying because I can't to_object() on {}", *this);
        VERIFY_NOT_REACHED();
    }
}

Value Value::to_numeric(GlobalObject& global_object) const
{
    auto primitive = to_primitive(global_object, Value::PreferredType::Number);
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
    case Type::Int32:
    case Type::Double:
        return *this;
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
        auto primitive = to_primitive(global_object, PreferredType::Number);
        if (global_object.vm().exception())
            return {};
        return primitive.to_number(global_object);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

BigInt* Value::to_bigint(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();
    auto primitive = to_primitive(global_object, PreferredType::Number);
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
    case Type::Int32:
    case Type::Double:
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
        VERIFY_NOT_REACHED();
    }
}

// FIXME: These two conversions are wrong for JS, and seem likely to be footguns
i32 Value::as_i32() const
{
    return static_cast<i32>(as_double());
}

u32 Value::as_u32() const
{
    VERIFY(as_double() >= 0);
    return min((double)as_i32(), MAX_U32);
}

size_t Value::as_size_t() const
{
    VERIFY(as_double() >= 0);
    return min((double)as_i32(), MAX_ARRAY_LIKE_INDEX);
}

double Value::to_double(GlobalObject& global_object) const
{
    auto number = to_number(global_object);
    if (global_object.vm().exception())
        return INVALID;
    return number.as_double();
}

i32 Value::to_i32_slow_case(GlobalObject& global_object) const
{
    VERIFY(type() != Type::Int32);
    auto number = to_number(global_object);
    if (global_object.vm().exception())
        return INVALID;
    double value = number.as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto abs = fabs(value);
    auto int_val = floor(abs);
    if (signbit(value))
        int_val = -int_val;
    auto int32bit = fmod(int_val, 4294967296.0);
    if (int32bit >= 2147483648.0)
        int32bit -= 4294967296.0;
    return static_cast<i32>(int32bit);
}

u32 Value::to_u32(GlobalObject& global_object) const
{
    // 7.1.7 ToUint32, https://tc39.es/ecma262/#sec-touint32
    auto number = to_number(global_object);
    if (global_object.vm().exception())
        return INVALID;
    double value = number.as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto int_val = floor(fabs(value));
    if (signbit(value))
        int_val = -int_val;
    auto int32bit = fmod(int_val, NumericLimits<u32>::max() + 1.0);
    return static_cast<u32>(int32bit);
}

size_t Value::to_length(GlobalObject& global_object) const
{
    // 7.1.20 ToLength, https://tc39.es/ecma262/#sec-tolength

    auto& vm = global_object.vm();

    auto len = to_integer_or_infinity(global_object);
    if (vm.exception())
        return INVALID;
    if (len <= 0)
        return 0;
    return min(len, MAX_ARRAY_LIKE_INDEX);
}

size_t Value::to_index(GlobalObject& global_object) const
{
    // 7.1.22 ToIndex, https://tc39.es/ecma262/#sec-toindex

    auto& vm = global_object.vm();

    if (is_undefined())
        return 0;
    auto integer_index = to_integer_or_infinity(global_object);
    if (vm.exception())
        return INVALID;
    if (integer_index < 0) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidIndex);
        return INVALID;
    }
    auto index = Value(integer_index).to_length(global_object);
    VERIFY(!vm.exception());
    if (integer_index != index) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidIndex);
        return INVALID;
    }
    return index;
}

double Value::to_integer_or_infinity(GlobalObject& global_object) const
{
    // 7.1.5 ToIntegerOrInfinity, https://tc39.es/ecma262/#sec-tointegerorinfinity

    auto& vm = global_object.vm();

    auto number = to_number(global_object);
    if (vm.exception())
        return INVALID;
    if (number.is_nan() || number.as_double() == 0)
        return 0;
    if (number.is_infinity())
        return number.as_double();
    auto integer = floor(fabs(number.as_double()));
    if (number.as_double() < 0)
        integer = -integer;
    return integer;
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
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() || !rhs_numeric.is_finite_number())
            return Value(0);
        return Value(lhs_numeric.to_i32(global_object) & rhs_numeric.to_i32(global_object));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().bitwise_and(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "bitwise AND");
    return {};
}

Value bitwise_or(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value(lhs_numeric.to_i32(global_object) | rhs_numeric.to_i32(global_object));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().bitwise_or(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "bitwise OR");
    return {};
}

Value bitwise_xor(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value(lhs_numeric.to_i32(global_object) ^ rhs_numeric.to_i32(global_object));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().bitwise_xor(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "bitwise XOR");
    return {};
}

Value bitwise_not(GlobalObject& global_object, Value lhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (lhs_numeric.is_number())
        return Value(~lhs_numeric.to_i32(global_object));
    auto big_integer_bitwise_not = lhs_numeric.as_bigint().big_integer();
    big_integer_bitwise_not = big_integer_bitwise_not.plus(Crypto::SignedBigInteger { 1 });
    big_integer_bitwise_not.negate();
    return js_bigint(global_object.heap(), big_integer_bitwise_not);
}

Value unary_plus(GlobalObject& global_object, Value lhs)
{
    return lhs.to_number(global_object);
}

Value unary_minus(GlobalObject& global_object, Value lhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object);
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
    // 6.1.6.1.9 Number::leftShift
    // https://tc39.es/ecma262/#sec-numeric-types-number-leftShift
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        // Ok, so this performs toNumber() again but that "can't" throw
        auto lhs_i32 = lhs_numeric.to_i32(global_object);
        auto rhs_u32 = rhs_numeric.to_u32(global_object);
        return Value(lhs_i32 << rhs_u32);
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        TODO();
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "left-shift");
    return {};
}

Value right_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    // 6.1.6.1.11 Number::signedRightShift
    // https://tc39.es/ecma262/#sec-numeric-types-number-signedRightShift
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        // Ok, so this performs toNumber() again but that "can't" throw
        auto lhs_i32 = lhs_numeric.to_i32(global_object);
        auto rhs_u32 = rhs_numeric.to_u32(global_object);
        return Value(lhs_i32 >> rhs_u32);
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        TODO();
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "right-shift");
    return {};
}

Value unsigned_right_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    // 6.1.6.1.11 Number::unsignedRightShift
    // https://tc39.es/ecma262/#sec-numeric-types-number-unsignedRightShift
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        // Ok, so this performs toNumber() again but that "can't" throw
        auto lhs_u32 = lhs_numeric.to_u32(global_object);
        auto rhs_u32 = rhs_numeric.to_u32(global_object) % 32;
        return Value(lhs_u32 >> rhs_u32);
    }
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperator, "unsigned right-shift");
    return {};
}

Value add(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (both_number(lhs, rhs)) {
        if (lhs.type() == Value::Type::Int32 && rhs.type() == Value::Type::Int32) {
            Checked<i32> result;
            result = lhs.to_i32(global_object);
            result += rhs.to_i32(global_object);
            if (!result.has_overflow())
                return Value(result.value());
        }
        return Value(lhs.as_double() + rhs.as_double());
    }
    auto& vm = global_object.vm();
    auto lhs_primitive = lhs.to_primitive(global_object);
    if (vm.exception())
        return {};
    auto rhs_primitive = rhs.to_primitive(global_object);
    if (vm.exception())
        return {};

    if (lhs_primitive.is_string() || rhs_primitive.is_string()) {
        auto lhs_string = lhs_primitive.to_string(global_object);
        if (vm.exception())
            return {};
        auto rhs_string = rhs_primitive.to_string(global_object);
        if (vm.exception())
            return {};
        StringBuilder builder(lhs_string.length() + rhs_string.length());
        builder.append(lhs_string);
        builder.append(rhs_string);
        return js_string(vm.heap(), builder.to_string());
    }

    auto lhs_numeric = lhs_primitive.to_numeric(global_object);
    if (vm.exception())
        return {};
    auto rhs_numeric = rhs_primitive.to_numeric(global_object);
    if (vm.exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() + rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(vm.heap(), lhs_numeric.as_bigint().big_integer().plus(rhs_numeric.as_bigint().big_integer()));
    vm.throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "addition");
    return {};
}

Value sub(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() - rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().minus(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "subtraction");
    return {};
}

Value mul(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() * rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().multiplied_by(rhs_numeric.as_bigint().big_integer()));
    global_object.vm().throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "multiplication");
    return {};
}

Value div(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (vm.exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (vm.exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() / rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer() == BIGINT_ZERO) {
            vm.throw_exception<RangeError>(global_object, ErrorType::DivisionByZero);
            return {};
        }
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).quotient);
    }
    vm.throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "division");
    return {};
}

Value mod(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = lhs.to_numeric(global_object);
    if (vm.exception())
        return {};
    auto rhs_numeric = rhs.to_numeric(global_object);
    if (vm.exception())
        return {};
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (lhs_numeric.is_nan() || rhs_numeric.is_nan())
            return js_nan();
        auto index = lhs_numeric.as_double();
        auto period = rhs_numeric.as_double();
        auto trunc = (double)(i32)(index / period);
        return Value(index - trunc * period);
    }
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer() == BIGINT_ZERO) {
            vm.throw_exception<RangeError>(global_object, ErrorType::DivisionByZero);
            return {};
        }
        return js_bigint(global_object.heap(), lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).remainder);
    }
    vm.throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "modulo");
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
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer().is_negative()) {
            vm.throw_exception<RangeError>(global_object, ErrorType::NegativeExponent);
            return {};
        }
        return js_bigint(vm.heap(), Crypto::NumberTheory::Power(lhs_numeric.as_bigint().big_integer(), rhs_numeric.as_bigint().big_integer()));
    }
    vm.throw_exception<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "exponentiation");
    return {};
}

Value in(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (!rhs.is_object()) {
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::InOperatorWithObject);
        return {};
    }
    auto lhs_string = lhs.to_string(global_object);
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
    auto has_instance_method = get_method(global_object, Value(&rhs.as_object()), vm.well_known_symbol_has_instance());
    if (vm.exception())
        return {};
    if (has_instance_method) {
        auto has_instance_result = vm.call(*has_instance_method, rhs, lhs);
        if (vm.exception())
            return {};
        return Value(has_instance_result.to_boolean());
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

    if (is<BoundFunction>(rhs_function)) {
        auto& bound_target = static_cast<const BoundFunction&>(rhs_function);
        return instance_of(global_object, lhs, Value(&bound_target.target_function()));
    }

    if (!lhs.is_object())
        return Value(false);

    Object* lhs_object = &lhs.as_object();
    auto rhs_prototype = rhs_function.get(vm.names.prototype);
    if (vm.exception())
        return {};

    if (!rhs_prototype.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::InstanceOfOperatorBadPrototype, rhs.to_string_without_side_effects());
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

bool same_value(Value lhs, Value rhs)
{
    if (!same_type_for_equality(lhs, rhs))
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
    if (!same_type_for_equality(lhs, rhs))
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
    VERIFY(!lhs.is_number() && !lhs.is_bigint());
    VERIFY(same_type_for_equality(lhs, rhs));

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
        VERIFY_NOT_REACHED();
    }
}

bool strict_eq(Value lhs, Value rhs)
{
    if (!same_type_for_equality(lhs, rhs))
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
    if (same_type_for_equality(lhs, rhs))
        return strict_eq(lhs, rhs);

    if (lhs.is_nullish() && rhs.is_nullish())
        return true;

    if (lhs.is_number() && rhs.is_string())
        return abstract_eq(global_object, lhs, rhs.to_number(global_object));

    if (lhs.is_string() && rhs.is_number())
        return abstract_eq(global_object, lhs.to_number(global_object), rhs);

    if (lhs.is_bigint() && rhs.is_string()) {
        auto& rhs_string = rhs.as_string().string();
        if (!is_valid_bigint_value(rhs_string))
            return false;
        return abstract_eq(global_object, lhs, js_bigint(global_object.heap(), Crypto::SignedBigInteger::from_base10(rhs_string)));
    }

    if (lhs.is_string() && rhs.is_bigint())
        return abstract_eq(global_object, rhs, lhs);

    if (lhs.is_boolean())
        return abstract_eq(global_object, lhs.to_number(global_object), rhs);

    if (rhs.is_boolean())
        return abstract_eq(global_object, lhs, rhs.to_number(global_object));

    if ((lhs.is_string() || lhs.is_number() || lhs.is_bigint() || lhs.is_symbol()) && rhs.is_object()) {
        auto rhs_primitive = rhs.to_primitive(global_object);
        if (global_object.vm().exception())
            return false;
        return abstract_eq(global_object, lhs, rhs_primitive);
    }

    if (lhs.is_object() && (rhs.is_string() || rhs.is_number() || lhs.is_bigint() || rhs.is_symbol())) {
        auto lhs_primitive = lhs.to_primitive(global_object);
        if (global_object.vm().exception())
            return false;
        return abstract_eq(global_object, lhs_primitive, rhs);
    }

    if ((lhs.is_bigint() && rhs.is_number()) || (lhs.is_number() && rhs.is_bigint())) {
        if (lhs.is_nan() || lhs.is_infinity() || rhs.is_nan() || rhs.is_infinity())
            return false;
        if ((lhs.is_number() && !lhs.is_integer()) || (rhs.is_number() && !rhs.is_integer()))
            return false;
        if (lhs.is_number())
            return Crypto::SignedBigInteger { lhs.to_i32(global_object) } == rhs.as_bigint().big_integer();
        else
            return Crypto::SignedBigInteger { rhs.to_i32(global_object) } == lhs.as_bigint().big_integer();
    }

    return false;
}

TriState abstract_relation(GlobalObject& global_object, bool left_first, Value lhs, Value rhs)
{
    Value x_primitive;
    Value y_primitive;

    if (left_first) {
        x_primitive = lhs.to_primitive(global_object, Value::PreferredType::Number);
        if (global_object.vm().exception())
            return {};
        y_primitive = rhs.to_primitive(global_object, Value::PreferredType::Number);
        if (global_object.vm().exception())
            return {};
    } else {
        y_primitive = lhs.to_primitive(global_object, Value::PreferredType::Number);
        if (global_object.vm().exception())
            return {};
        x_primitive = rhs.to_primitive(global_object, Value::PreferredType::Number);
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
        VERIFY_NOT_REACHED();
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

    auto x_numeric = x_primitive.to_numeric(global_object);
    if (global_object.vm().exception())
        return {};
    auto y_numeric = y_primitive.to_numeric(global_object);
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

    VERIFY((x_numeric.is_number() && y_numeric.is_bigint()) || (x_numeric.is_bigint() && y_numeric.is_number()));

    bool x_lower_than_y;
    if (x_numeric.is_number()) {
        x_lower_than_y = x_numeric.is_integer()
            ? Crypto::SignedBigInteger { x_numeric.to_i32(global_object) } < y_numeric.as_bigint().big_integer()
            : (Crypto::SignedBigInteger { x_numeric.to_i32(global_object) } < y_numeric.as_bigint().big_integer() || Crypto::SignedBigInteger { x_numeric.to_i32(global_object) + 1 } < y_numeric.as_bigint().big_integer());
    } else {
        x_lower_than_y = y_numeric.is_integer()
            ? x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { y_numeric.to_i32(global_object) }
            : (x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { y_numeric.to_i32(global_object) } || x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { y_numeric.to_i32(global_object) + 1 });
    }
    if (x_lower_than_y)
        return TriState::True;
    else
        return TriState::False;
}

Function* get_method(GlobalObject& global_object, Value value, const PropertyName& property_name)
{
    // 7.3.10 GetMethod, https://tc39.es/ecma262/#sec-getmethod

    auto& vm = global_object.vm();
    auto* object = value.to_object(global_object);
    if (vm.exception())
        return nullptr;
    auto property_value = object->get(property_name);
    if (vm.exception())
        return nullptr;
    if (property_value.is_empty() || property_value.is_nullish())
        return nullptr;
    if (!property_value.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, property_value.to_string_without_side_effects());
        return nullptr;
    }
    return &property_value.as_function();
}

size_t length_of_array_like(GlobalObject& global_object, const Object& object)
{
    // 7.3.18 LengthOfArrayLike, https://tc39.es/ecma262/#sec-lengthofarraylike

    auto& vm = global_object.vm();
    auto result = object.get(vm.names.length).value_or(js_undefined());
    if (vm.exception())
        return INVALID;
    return result.to_length(global_object);
}

}
