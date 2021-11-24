/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibJS/Runtime/Accessor.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/BigInt.h>
#include <LibJS/Runtime/BigIntObject.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/BoundFunction.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NativeFunction.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/ProxyObject.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>
#include <math.h>

namespace JS {

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
    return all_of(string, [](auto ch) { return isdigit(ch); });
}

ALWAYS_INLINE bool both_number(const Value& lhs, const Value& rhs)
{
    return lhs.is_number() && rhs.is_number();
}

ALWAYS_INLINE bool both_bigint(const Value& lhs, const Value& rhs)
{
    return lhs.is_bigint() && rhs.is_bigint();
}

// 6.1.6.1.20 Number::toString ( x ), https://tc39.es/ecma262/#sec-numeric-types-number-tostring
static String double_to_string(double d)
{
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
    if (d == static_cast<double>(INFINITY))
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

        builder.append(String::number(AK::abs(exponent - 1)));
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

    builder.append(String::number(AK::abs(exponent - 1)));
    return builder.to_string();
}

// 7.2.2 IsArray ( argument ), https://tc39.es/ecma262/#sec-isarray
ThrowCompletionOr<bool> Value::is_array(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();

    if (!is_object())
        return false;
    auto& object = as_object();
    if (is<Array>(object))
        return true;
    if (is<ProxyObject>(object)) {
        auto& proxy = static_cast<ProxyObject const&>(object);
        if (proxy.is_revoked())
            return vm.throw_completion<TypeError>(global_object, ErrorType::ProxyRevoked);
        return Value(&proxy.target()).is_array(global_object);
    }
    return false;
}

Array& Value::as_array()
{
    VERIFY(is_object() && is<Array>(as_object()));
    return static_cast<Array&>(*m_value.as_object);
}

// 7.2.3 IsCallable ( argument ), https://tc39.es/ecma262/#sec-iscallable
bool Value::is_function() const
{
    return is_object() && as_object().is_function();
}

FunctionObject& Value::as_function()
{
    VERIFY(is_function());
    return static_cast<FunctionObject&>(as_object());
}

FunctionObject const& Value::as_function() const
{
    VERIFY(is_function());
    return static_cast<FunctionObject const&>(as_object());
}

// 7.2.4 IsConstructor ( argument ), https://tc39.es/ecma262/#sec-isconstructor
bool Value::is_constructor() const
{
    // 1. If Type(argument) is not Object, return false.
    if (!is_function())
        return false;

    // 2. If argument has a [[Construct]] internal method, return true.
    if (as_function().has_constructor())
        return true;

    // 3. Return false.
    return false;
}

// 7.2.8 IsRegExp ( argument ), https://tc39.es/ecma262/#sec-isregexp
ThrowCompletionOr<bool> Value::is_regexp(GlobalObject& global_object) const
{
    if (!is_object())
        return false;

    auto& vm = global_object.vm();
    auto matcher = TRY(as_object().get(*vm.well_known_symbol_match()));
    if (!matcher.is_undefined())
        return matcher.to_boolean();

    return is<RegExpObject>(as_object());
}

// 13.5.3 The typeof Operator, https://tc39.es/ecma262/#sec-typeof-operator
String Value::typeof() const
{
    switch (m_type) {
    case Value::Type::Undefined:
        return "undefined";
    case Value::Type::Null:
        return "object";
    case Value::Type::Int32:
    case Value::Type::Double:
        return "number";
    case Value::Type::String:
        return "string";
    case Value::Type::Object:
        // B.3.7.3 Changes to the typeof Operator, https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot-typeof
        if (as_object().is_htmldda())
            return "undefined";
        if (is_function())
            return "function";
        return "object";
    case Value::Type::Boolean:
        return "boolean";
    case Value::Type::Symbol:
        return "symbol";
    case Value::Type::BigInt:
        return "bigint";
    default:
        VERIFY_NOT_REACHED();
    }
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
    default:
        VERIFY_NOT_REACHED();
    }
}

ThrowCompletionOr<PrimitiveString*> Value::to_primitive_string(GlobalObject& global_object)
{
    if (is_string())
        return &as_string();
    auto string = TRY(to_string(global_object));
    return js_string(global_object.heap(), string);
}

// 7.1.17 ToString ( argument ), https://tc39.es/ecma262/#sec-tostring
ThrowCompletionOr<String> Value::to_string(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();
    switch (m_type) {
    case Type::Undefined:
        return "undefined"sv;
    case Type::Null:
        return "null"sv;
    case Type::Boolean:
        return m_value.as_bool ? "true"sv : "false"sv;
    case Type::Int32:
        return String::number(m_value.as_i32);
    case Type::Double:
        return double_to_string(m_value.as_double);
    case Type::String:
        return m_value.as_string->string();
    case Type::Symbol:
        return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "symbol", "string");
    case Type::BigInt:
        return m_value.as_bigint->big_integer().to_base(10);
    case Type::Object: {
        auto primitive_value = TRY(to_primitive(global_object, PreferredType::String));
        return primitive_value.to_string(global_object);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

ThrowCompletionOr<Utf16String> Value::to_utf16_string(GlobalObject& global_object) const
{
    if (m_type == Type::String)
        return m_value.as_string->utf16_string();

    auto utf8_string = TRY(to_string(global_object));
    return Utf16String(utf8_string);
}

// 7.1.2 ToBoolean ( argument ), https://tc39.es/ecma262/#sec-toboolean
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
        // B.3.7.1 Changes to ToBoolean, https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot-to-boolean
        if (m_value.as_object->is_htmldda())
            return false;
        return true;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 7.1.1 ToPrimitive ( input [ , preferredType ] ), https://tc39.es/ecma262/#sec-toprimitive
ThrowCompletionOr<Value> Value::to_primitive(GlobalObject& global_object, PreferredType preferred_type) const
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
        auto to_primitive_method = TRY(get_method(global_object, *vm.well_known_symbol_to_primitive()));
        if (to_primitive_method) {
            auto hint = get_hint_for_preferred_type();
            auto result = TRY(vm.call(*to_primitive_method, *this, js_string(vm, hint)));
            if (!result.is_object())
                return result;
            return vm.throw_completion<TypeError>(global_object, ErrorType::ToPrimitiveReturnedObject, to_string_without_side_effects(), hint);
        }
        if (preferred_type == PreferredType::Default)
            preferred_type = PreferredType::Number;
        return as_object().ordinary_to_primitive(preferred_type);
    }
    return *this;
}

// 7.1.18 ToObject ( argument ), https://tc39.es/ecma262/#sec-toobject
ThrowCompletionOr<Object*> Value::to_object(GlobalObject& global_object) const
{
    switch (m_type) {
    case Type::Undefined:
    case Type::Null:
        return global_object.vm().throw_completion<TypeError>(global_object, ErrorType::ToObjectNullOrUndefined);
    case Type::Boolean:
        return BooleanObject::create(global_object, m_value.as_bool);
    case Type::Int32:
    case Type::Double:
        return NumberObject::create(global_object, as_double());
    case Type::String:
        return StringObject::create(global_object, *m_value.as_string, *global_object.string_prototype());
    case Type::Symbol:
        return SymbolObject::create(global_object, *m_value.as_symbol);
    case Type::BigInt:
        return BigIntObject::create(global_object, *m_value.as_bigint);
    case Type::Object:
        return &const_cast<Object&>(as_object());
    default:
        VERIFY_NOT_REACHED();
    }
}

// 7.1.3 ToNumeric ( value ), https://tc39.es/ecma262/#sec-tonumeric
FLATTEN ThrowCompletionOr<Value> Value::to_numeric(GlobalObject& global_object) const
{
    auto primitive = TRY(to_primitive(global_object, Value::PreferredType::Number));
    if (primitive.is_bigint())
        return primitive;
    return primitive.to_number(global_object);
}

// 7.1.4 ToNumber ( argument ), https://tc39.es/ecma262/#sec-tonumber
ThrowCompletionOr<Value> Value::to_number(GlobalObject& global_object) const
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
        return global_object.vm().throw_completion<TypeError>(global_object, ErrorType::Convert, "symbol", "number");
    case Type::BigInt:
        return global_object.vm().throw_completion<TypeError>(global_object, ErrorType::Convert, "BigInt", "number");
    case Type::Object: {
        auto primitive = TRY(to_primitive(global_object, PreferredType::Number));
        return primitive.to_number(global_object);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

// 7.1.13 ToBigInt ( argument ), https://tc39.es/ecma262/#sec-tobigint
ThrowCompletionOr<BigInt*> Value::to_bigint(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();
    auto primitive = TRY(to_primitive(global_object, PreferredType::Number));
    switch (primitive.type()) {
    case Type::Undefined:
        return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "undefined", "BigInt");
    case Type::Null:
        return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "null", "BigInt");
    case Type::Boolean: {
        auto value = primitive.as_bool() ? 1 : 0;
        return js_bigint(vm, Crypto::SignedBigInteger { value });
    }
    case Type::BigInt:
        return &primitive.as_bigint();
    case Type::Int32:
    case Type::Double:
        return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "number", "BigInt");
    case Type::String: {
        auto& string = primitive.as_string().string();
        if (!is_valid_bigint_value(string))
            return vm.throw_completion<SyntaxError>(global_object, ErrorType::BigIntInvalidValue, string);
        return js_bigint(vm, Crypto::SignedBigInteger::from_base(10, string.trim_whitespace()));
    }
    case Type::Symbol:
        return vm.throw_completion<TypeError>(global_object, ErrorType::Convert, "symbol", "BigInt");
    default:
        VERIFY_NOT_REACHED();
    }
}

// 7.1.15 ToBigInt64 ( argument ), https://tc39.es/ecma262/#sec-tobigint64
ThrowCompletionOr<i64> Value::to_bigint_int64(GlobalObject& global_object) const
{
    auto* bigint = TRY(to_bigint(global_object));
    return static_cast<i64>(bigint->big_integer().to_u64());
}

// 7.1.16 ToBigUint64 ( argument ), https://tc39.es/ecma262/#sec-tobiguint64
ThrowCompletionOr<u64> Value::to_bigint_uint64(GlobalObject& global_object) const
{
    auto* bigint = TRY(to_bigint(global_object));
    return bigint->big_integer().to_u64();
}

ThrowCompletionOr<double> Value::to_double(GlobalObject& global_object) const
{
    return TRY(to_number(global_object)).as_double();
}

// 7.1.19 ToPropertyKey ( argument ), https://tc39.es/ecma262/#sec-topropertykey
ThrowCompletionOr<PropertyKey> Value::to_property_key(GlobalObject& global_object) const
{
    if (type() == Type::Int32 && as_i32() >= 0)
        return PropertyKey { as_i32() };
    auto key = TRY(to_primitive(global_object, PreferredType::String));
    if (key.is_symbol())
        return &key.as_symbol();
    return TRY(key.to_string(global_object));
}

ThrowCompletionOr<i32> Value::to_i32_slow_case(GlobalObject& global_object) const
{
    VERIFY(type() != Type::Int32);
    double value = TRY(to_number(global_object)).as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto abs = fabs(value);
    auto int_val = floor(abs);
    if (signbit(value))
        int_val = -int_val;
    auto remainder = fmod(int_val, 4294967296.0);
    auto int32bit = remainder >= 0.0 ? remainder : remainder + 4294967296.0; // The notation “x modulo y” computes a value k of the same sign as y
    if (int32bit >= 2147483648.0)
        int32bit -= 4294967296.0;
    return static_cast<i32>(int32bit);
}

ThrowCompletionOr<i32> Value::to_i32(GlobalObject& global_object) const
{
    if (m_type == Type::Int32)
        return m_value.as_i32;
    return to_i32_slow_case(global_object);
}

// 7.1.7 ToUint32 ( argument ), https://tc39.es/ecma262/#sec-touint32
ThrowCompletionOr<u32> Value::to_u32(GlobalObject& global_object) const
{
    double value = TRY(to_number(global_object)).as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto int_val = floor(fabs(value));
    if (signbit(value))
        int_val = -int_val;
    auto int32bit = fmod(int_val, NumericLimits<u32>::max() + 1.0);
    // Cast to i64 here to ensure that the double --> u32 cast doesn't invoke undefined behavior
    // Otherwise, negative numbers cause a UBSAN warning.
    return static_cast<u32>(static_cast<i64>(int32bit));
}

// 7.1.8 ToInt16 ( argument ), https://tc39.es/ecma262/#sec-toint16
ThrowCompletionOr<i16> Value::to_i16(GlobalObject& global_object) const
{
    double value = TRY(to_number(global_object)).as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto abs = fabs(value);
    auto int_val = floor(abs);
    if (signbit(value))
        int_val = -int_val;
    auto remainder = fmod(int_val, 65536.0);
    auto int16bit = remainder >= 0.0 ? remainder : remainder + 65536.0; // The notation “x modulo y” computes a value k of the same sign as y
    if (int16bit >= 32768.0)
        int16bit -= 65536.0;
    return static_cast<i16>(int16bit);
}

// 7.1.9 ToUint16 ( argument ), https://tc39.es/ecma262/#sec-touint16
ThrowCompletionOr<u16> Value::to_u16(GlobalObject& global_object) const
{
    double value = TRY(to_number(global_object)).as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto int_val = floor(fabs(value));
    if (signbit(value))
        int_val = -int_val;
    auto int16bit = fmod(int_val, NumericLimits<u16>::max() + 1.0);
    if (int16bit < 0)
        int16bit += NumericLimits<u16>::max() + 1.0;
    return static_cast<u16>(int16bit);
}

// 7.1.10 ToInt8 ( argument ), https://tc39.es/ecma262/#sec-toint8
ThrowCompletionOr<i8> Value::to_i8(GlobalObject& global_object) const
{
    double value = TRY(to_number(global_object)).as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto abs = fabs(value);
    auto int_val = floor(abs);
    if (signbit(value))
        int_val = -int_val;
    auto remainder = fmod(int_val, 256.0);
    auto int8bit = remainder >= 0.0 ? remainder : remainder + 256.0; // The notation “x modulo y” computes a value k of the same sign as y
    if (int8bit >= 128.0)
        int8bit -= 256.0;
    return static_cast<i8>(int8bit);
}

// 7.1.11 ToUint8 ( argument ), https://tc39.es/ecma262/#sec-touint8
ThrowCompletionOr<u8> Value::to_u8(GlobalObject& global_object) const
{
    double value = TRY(to_number(global_object)).as_double();
    if (!isfinite(value) || value == 0)
        return 0;
    auto int_val = floor(fabs(value));
    if (signbit(value))
        int_val = -int_val;
    auto int8bit = fmod(int_val, NumericLimits<u8>::max() + 1.0);
    if (int8bit < 0)
        int8bit += NumericLimits<u8>::max() + 1.0;
    return static_cast<u8>(int8bit);
}

// 7.1.12 ToUint8Clamp ( argument ), https://tc39.es/ecma262/#sec-touint8clamp
ThrowCompletionOr<u8> Value::to_u8_clamp(GlobalObject& global_object) const
{
    auto number = TRY(to_number(global_object));
    if (number.is_nan())
        return 0;
    double value = number.as_double();
    if (value <= 0.0)
        return 0;
    if (value >= 255.0)
        return 255;
    auto int_val = floor(value);
    if (int_val + 0.5 < value)
        return static_cast<u8>(int_val + 1.0);
    if (value < int_val + 0.5)
        return static_cast<u8>(int_val);
    if (fmod(int_val, 2.0) == 1.0)
        return static_cast<u8>(int_val + 1.0);
    return static_cast<u8>(int_val);
}

// 7.1.20 ToLength ( argument ), https://tc39.es/ecma262/#sec-tolength
ThrowCompletionOr<size_t> Value::to_length(GlobalObject& global_object) const
{
    auto len = TRY(to_integer_or_infinity(global_object));
    if (len <= 0)
        return 0;
    // FIXME: The spec says that this function's output range is 0 - 2^53-1. But we don't want to overflow the size_t.
    constexpr double length_limit = sizeof(void*) == 4 ? NumericLimits<size_t>::max() : MAX_ARRAY_LIKE_INDEX;
    return min(len, length_limit);
}

// 7.1.22 ToIndex ( argument ), https://tc39.es/ecma262/#sec-toindex
ThrowCompletionOr<size_t> Value::to_index(GlobalObject& global_object) const
{
    auto& vm = global_object.vm();

    if (is_undefined())
        return 0;
    auto integer_index = TRY(to_integer_or_infinity(global_object));
    if (integer_index < 0)
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidIndex);
    auto index = MUST(Value(integer_index).to_length(global_object));
    if (integer_index != index)
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidIndex);
    return index;
}

// 7.1.5 ToIntegerOrInfinity ( argument ), https://tc39.es/ecma262/#sec-tointegerorinfinity
ThrowCompletionOr<double> Value::to_integer_or_infinity(GlobalObject& global_object) const
{
    auto number = TRY(to_number(global_object));
    if (number.is_nan() || number.as_double() == 0)
        return 0;
    if (number.is_infinity())
        return number.as_double();
    auto integer = floor(fabs(number.as_double()));
    if (number.as_double() < 0)
        integer = -integer;
    return integer;
}

// 7.3.3 GetV ( V, P ), https://tc39.es/ecma262/#sec-getv
ThrowCompletionOr<Value> Value::get(GlobalObject& global_object, PropertyKey const& property_name) const
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let O be ? ToObject(V).
    auto* object = TRY(to_object(global_object));

    // 3. Return ? O.[[Get]](P, V).
    return TRY(object->internal_get(property_name, *this));
}

// 7.3.10 GetMethod ( V, P ), https://tc39.es/ecma262/#sec-getmethod
ThrowCompletionOr<FunctionObject*> Value::get_method(GlobalObject& global_object, PropertyKey const& property_name) const
{
    auto& vm = global_object.vm();

    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_name.is_valid());

    // 2. Let func be ? GetV(V, P).
    auto function = TRY(get(global_object, property_name));

    // 3. If func is either undefined or null, return undefined.
    if (function.is_nullish())
        return nullptr;

    // 4. If IsCallable(func) is false, throw a TypeError exception.
    if (!function.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, function.to_string_without_side_effects());

    // 5. Return func.
    return &function.as_function();
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> greater_than(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (lhs.type() == Value::Type::Int32 && rhs.type() == Value::Type::Int32)
        return lhs.as_i32() > rhs.as_i32();

    TriState relation = TRY(is_less_than(global_object, false, lhs, rhs));
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> greater_than_equals(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (lhs.type() == Value::Type::Int32 && rhs.type() == Value::Type::Int32)
        return lhs.as_i32() >= rhs.as_i32();

    TriState relation = TRY(is_less_than(global_object, true, lhs, rhs));
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> less_than(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (lhs.type() == Value::Type::Int32 && rhs.type() == Value::Type::Int32)
        return lhs.as_i32() < rhs.as_i32();

    TriState relation = TRY(is_less_than(global_object, true, lhs, rhs));
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> less_than_equals(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (lhs.type() == Value::Type::Int32 && rhs.type() == Value::Type::Int32)
        return lhs.as_i32() <= rhs.as_i32();

    TriState relation = TRY(is_less_than(global_object, false, lhs, rhs));
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

// 13.12 Binary Bitwise Operators, https://tc39.es/ecma262/#sec-binary-bitwise-operators
ThrowCompletionOr<Value> bitwise_and(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() || !rhs_numeric.is_finite_number())
            return Value(0);
        return Value(TRY(lhs_numeric.to_i32(global_object)) & TRY(rhs_numeric.to_i32(global_object)));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().bitwise_and(rhs_numeric.as_bigint().big_integer())));
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "bitwise AND");
}

// 13.12 Binary Bitwise Operators, https://tc39.es/ecma262/#sec-binary-bitwise-operators
ThrowCompletionOr<Value> bitwise_or(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value(TRY(lhs_numeric.to_i32(global_object)) | TRY(rhs_numeric.to_i32(global_object)));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().bitwise_or(rhs_numeric.as_bigint().big_integer())));
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "bitwise OR");
}

// 13.12 Binary Bitwise Operators, https://tc39.es/ecma262/#sec-binary-bitwise-operators
ThrowCompletionOr<Value> bitwise_xor(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value(TRY(lhs_numeric.to_i32(global_object)) ^ TRY(rhs_numeric.to_i32(global_object)));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().bitwise_xor(rhs_numeric.as_bigint().big_integer())));
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "bitwise XOR");
}

// 13.5.6 Bitwise NOT Operator ( ~ ), https://tc39.es/ecma262/#sec-bitwise-not-operator
ThrowCompletionOr<Value> bitwise_not(GlobalObject& global_object, Value lhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    if (lhs_numeric.is_number())
        return Value(~TRY(lhs_numeric.to_i32(global_object)));
    auto big_integer_bitwise_not = lhs_numeric.as_bigint().big_integer();
    big_integer_bitwise_not = big_integer_bitwise_not.plus(Crypto::SignedBigInteger { 1 });
    big_integer_bitwise_not.negate();
    return Value(js_bigint(vm, big_integer_bitwise_not));
}

// 13.5.4 Unary + Operator, https://tc39.es/ecma262/#sec-unary-plus-operator
ThrowCompletionOr<Value> unary_plus(GlobalObject& global_object, Value lhs)
{
    return TRY(lhs.to_number(global_object));
}

// 13.5.5 Unary - Operator, https://tc39.es/ecma262/#sec-unary-minus-operator
ThrowCompletionOr<Value> unary_minus(GlobalObject& global_object, Value lhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    if (lhs_numeric.is_number()) {
        if (lhs_numeric.is_nan())
            return js_nan();
        return Value(-lhs_numeric.as_double());
    }
    if (lhs_numeric.as_bigint().big_integer() == BIGINT_ZERO)
        return Value(js_bigint(vm, BIGINT_ZERO));
    auto big_integer_negated = lhs_numeric.as_bigint().big_integer();
    big_integer_negated.negate();
    return Value(js_bigint(vm, big_integer_negated));
}

// 13.9.1 The Left Shift Operator ( << ), https://tc39.es/ecma262/#sec-left-shift-operator
ThrowCompletionOr<Value> left_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        // Ok, so this performs toNumber() again but that "can't" throw
        auto lhs_i32 = MUST(lhs_numeric.to_i32(global_object));
        auto rhs_u32 = MUST(rhs_numeric.to_u32(global_object)) % 32;
        return Value(lhs_i32 << rhs_u32);
    }
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        auto multiplier_divisor = Crypto::SignedBigInteger { Crypto::NumberTheory::Power(Crypto::UnsignedBigInteger(2), rhs_numeric.as_bigint().big_integer().unsigned_value()) };
        if (rhs_numeric.as_bigint().big_integer().is_negative())
            return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().divided_by(multiplier_divisor).quotient));
        else
            return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().multiplied_by(multiplier_divisor)));
    }
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "left-shift");
}

// 13.9.2 The Signed Right Shift Operator ( >> ), https://tc39.es/ecma262/#sec-signed-right-shift-operator
ThrowCompletionOr<Value> right_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        auto lhs_i32 = MUST(lhs_numeric.to_i32(global_object));
        auto rhs_u32 = MUST(rhs_numeric.to_u32(global_object)) % 32;
        return Value(lhs_i32 >> rhs_u32);
    }
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        auto rhs_negated = rhs_numeric.as_bigint().big_integer();
        rhs_negated.negate();
        return left_shift(global_object, lhs, js_bigint(vm, rhs_negated));
    }
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "right-shift");
}

// 13.9.3 The Unsigned Right Shift Operator ( >>> ), https://tc39.es/ecma262/#sec-unsigned-right-shift-operator
ThrowCompletionOr<Value> unsigned_right_shift(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        // Ok, so this performs toNumber() again but that "can't" throw
        auto lhs_u32 = MUST(lhs_numeric.to_u32(global_object));
        auto rhs_u32 = MUST(rhs_numeric.to_u32(global_object)) % 32;
        return Value(lhs_u32 >> rhs_u32);
    }
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperator, "unsigned right-shift");
}

// 13.8.1 The Addition Operator ( + ), https://tc39.es/ecma262/#sec-addition-operator-plus
ThrowCompletionOr<Value> add(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (both_number(lhs, rhs)) {
        if (lhs.type() == Value::Type::Int32 && rhs.type() == Value::Type::Int32) {
            Checked<i32> result;
            result = MUST(lhs.to_i32(global_object));
            result += MUST(rhs.to_i32(global_object));
            if (!result.has_overflow())
                return Value(result.value());
        }
        return Value(lhs.as_double() + rhs.as_double());
    }
    auto& vm = global_object.vm();
    auto lhs_primitive = TRY(lhs.to_primitive(global_object));
    auto rhs_primitive = TRY(rhs.to_primitive(global_object));

    if (lhs_primitive.is_string() && rhs_primitive.is_string()) {
        auto const& lhs_string = lhs_primitive.as_string();
        auto const& rhs_string = rhs_primitive.as_string();

        if (lhs_string.has_utf16_string() && rhs_string.has_utf16_string()) {
            auto const& lhs_utf16_string = lhs_string.utf16_string();
            auto const& rhs_utf16_string = rhs_string.utf16_string();

            Vector<u16, 1> combined;
            combined.ensure_capacity(lhs_utf16_string.length_in_code_units() + rhs_utf16_string.length_in_code_units());
            combined.extend(lhs_utf16_string.string());
            combined.extend(rhs_utf16_string.string());
            return Value(js_string(vm.heap(), Utf16String(move(combined))));
        }
    }
    if (lhs_primitive.is_string() || rhs_primitive.is_string()) {
        auto lhs_string = TRY(lhs_primitive.to_string(global_object));
        auto rhs_string = TRY(rhs_primitive.to_string(global_object));
        StringBuilder builder(lhs_string.length() + rhs_string.length());
        builder.append(lhs_string);
        builder.append(rhs_string);
        return Value(js_string(vm, builder.to_string()));
    }

    auto lhs_numeric = TRY(lhs_primitive.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs_primitive.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() + rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().plus(rhs_numeric.as_bigint().big_integer())));
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "addition");
}

// 13.8.2 The Subtraction Operator ( - ), https://tc39.es/ecma262/#sec-subtraction-operator-minus
ThrowCompletionOr<Value> sub(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() - rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().minus(rhs_numeric.as_bigint().big_integer())));
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "subtraction");
}

// 13.7 Multiplicative Operators, https://tc39.es/ecma262/#sec-multiplicative-operators
ThrowCompletionOr<Value> mul(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() * rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().multiplied_by(rhs_numeric.as_bigint().big_integer())));
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "multiplication");
}

// 13.7 Multiplicative Operators, https://tc39.es/ecma262/#sec-multiplicative-operators
ThrowCompletionOr<Value> div(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() / rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer() == BIGINT_ZERO)
            return vm.throw_completion<RangeError>(global_object, ErrorType::DivisionByZero);
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).quotient));
    }
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "division");
}

// 13.7 Multiplicative Operators, https://tc39.es/ecma262/#sec-multiplicative-operators
ThrowCompletionOr<Value> mod(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric)) {
        // 6.1.6.1.6 Number::remainder ( n, d ), https://tc39.es/ecma262/#sec-numeric-types-number-remainder

        // 1. If n is NaN or d is NaN, return NaN.
        if (lhs_numeric.is_nan() || rhs_numeric.is_nan())
            return js_nan();

        // 2. If n is +∞𝔽 or n is -∞𝔽, return NaN.
        if (lhs_numeric.is_positive_infinity() || lhs_numeric.is_negative_infinity())
            return js_nan();

        // 3. If d is +∞𝔽 or d is -∞𝔽, return n.
        if (rhs_numeric.is_positive_infinity() || rhs_numeric.is_negative_infinity())
            return lhs_numeric;

        // 4. If d is +0𝔽 or d is -0𝔽, return NaN.
        if (rhs_numeric.is_positive_zero() || rhs_numeric.is_negative_zero())
            return js_nan();

        // 5. If n is +0𝔽 or n is -0𝔽, return n.
        if (lhs_numeric.is_positive_zero() || lhs_numeric.is_negative_zero())
            return lhs_numeric;

        // 6. Assert: n and d are finite and non-zero.

        auto index = lhs_numeric.as_double();
        auto period = rhs_numeric.as_double();
        auto trunc = (double)(i32)(index / period);

        // 7. Let r be ℝ(n) - (ℝ(d) × q) where q is an integer that is negative if and only if n and d have opposite sign, and whose magnitude is as large as possible without exceeding the magnitude of ℝ(n) / ℝ(d).
        // 8. Return 𝔽(r).
        return Value(index - trunc * period);
    }
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer() == BIGINT_ZERO)
            return vm.throw_completion<RangeError>(global_object, ErrorType::DivisionByZero);
        return Value(js_bigint(vm, lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).remainder));
    }
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "modulo");
}

// 13.6 Exponentiation Operator, https://tc39.es/ecma262/#sec-exp-operator
ThrowCompletionOr<Value> exp(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    auto lhs_numeric = TRY(lhs.to_numeric(global_object));
    auto rhs_numeric = TRY(rhs.to_numeric(global_object));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(pow(lhs_numeric.as_double(), rhs_numeric.as_double()));
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer().is_negative())
            return vm.throw_completion<RangeError>(global_object, ErrorType::NegativeExponent);
        return Value(js_bigint(vm, Crypto::NumberTheory::Power(lhs_numeric.as_bigint().big_integer(), rhs_numeric.as_bigint().big_integer())));
    }
    return vm.throw_completion<TypeError>(global_object, ErrorType::BigIntBadOperatorOtherType, "exponentiation");
}

ThrowCompletionOr<Value> in(GlobalObject& global_object, Value lhs, Value rhs)
{
    if (!rhs.is_object())
        return global_object.vm().throw_completion<TypeError>(global_object, ErrorType::InOperatorWithObject);
    auto lhs_property_key = TRY(lhs.to_property_key(global_object));
    return Value(TRY(rhs.as_object().has_property(lhs_property_key)));
}

// 13.10.2 InstanceofOperator ( V, target ), https://tc39.es/ecma262/#sec-instanceofoperator
ThrowCompletionOr<Value> instance_of(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    if (!rhs.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, rhs.to_string_without_side_effects());
    auto has_instance_method = TRY(rhs.get_method(global_object, *vm.well_known_symbol_has_instance()));
    if (has_instance_method) {
        auto has_instance_result = TRY(vm.call(*has_instance_method, rhs, lhs));
        return Value(has_instance_result.to_boolean());
    }
    if (!rhs.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, rhs.to_string_without_side_effects());
    return TRY(ordinary_has_instance(global_object, lhs, rhs));
}

// 7.3.21 OrdinaryHasInstance ( C, O ), https://tc39.es/ecma262/#sec-ordinaryhasinstance
ThrowCompletionOr<Value> ordinary_has_instance(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();
    if (!rhs.is_function())
        return Value(false);
    auto& rhs_function = rhs.as_function();

    if (is<BoundFunction>(rhs_function)) {
        auto& bound_target = static_cast<const BoundFunction&>(rhs_function);
        return instance_of(global_object, lhs, Value(&bound_target.bound_target_function()));
    }

    if (!lhs.is_object())
        return Value(false);

    Object* lhs_object = &lhs.as_object();
    auto rhs_prototype = TRY(rhs_function.get(vm.names.prototype));
    if (!rhs_prototype.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::InstanceOfOperatorBadPrototype, rhs.to_string_without_side_effects());
    while (true) {
        lhs_object = TRY(lhs_object->internal_get_prototype_of());
        if (!lhs_object)
            return Value(false);
        if (same_value(rhs_prototype, lhs_object))
            return Value(true);
    }
}

// 7.2.10 SameValue ( x, y ), https://tc39.es/ecma262/#sec-samevalue
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

// 7.2.11 SameValueZero ( x, y ), https://tc39.es/ecma262/#sec-samevaluezero
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

// 7.2.12 SameValueNonNumeric ( x, y ), https://tc39.es/ecma262/#sec-samevaluenonnumeric
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

// 7.2.15 IsStrictlyEqual ( x, y ), https://tc39.es/ecma262/#sec-isstrictlyequal
bool is_strictly_equal(Value lhs, Value rhs)
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

// 7.2.14 IsLooselyEqual ( x, y ), https://tc39.es/ecma262/#sec-islooselyequal
ThrowCompletionOr<bool> is_loosely_equal(GlobalObject& global_object, Value lhs, Value rhs)
{
    auto& vm = global_object.vm();

    // 1. If Type(x) is the same as Type(y), then
    if (same_type_for_equality(lhs, rhs)) {
        // a. Return IsStrictlyEqual(x, y).
        return is_strictly_equal(lhs, rhs);
    }

    // 2. If x is null and y is undefined, return true.
    // 3. If x is undefined and y is null, return true.
    if (lhs.is_nullish() && rhs.is_nullish())
        return true;

    // 4. NOTE: This step is replaced in section B.3.6.2.
    // B.3.6.2 Changes to IsLooselyEqual, https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot-aec
    // 1. If Type(x) is Object and x has an [[IsHTMLDDA]] internal slot and y is either null or undefined, return true.
    if (lhs.is_object() && lhs.as_object().is_htmldda() && rhs.is_nullish())
        return true;

    // 2. If x is either null or undefined and Type(y) is Object and y has an [[IsHTMLDDA]] internal slot, return true.
    if (lhs.is_nullish() && rhs.is_object() && rhs.as_object().is_htmldda())
        return true;

    // == End of B.3.6.2 ==

    // 5. If Type(x) is Number and Type(y) is String, return IsLooselyEqual(x, ! ToNumber(y)).
    if (lhs.is_number() && rhs.is_string())
        return is_loosely_equal(global_object, lhs, MUST(rhs.to_number(global_object)));

    // 6. If Type(x) is String and Type(y) is Number, return IsLooselyEqual(! ToNumber(x), y).
    if (lhs.is_string() && rhs.is_number())
        return is_loosely_equal(global_object, MUST(lhs.to_number(global_object)), rhs);

    // 7. If Type(x) is BigInt and Type(y) is String, then
    if (lhs.is_bigint() && rhs.is_string()) {
        auto& rhs_string = rhs.as_string().string();
        // a. Let n be ! StringToBigInt(y).
        // b. If n is NaN, return false.
        if (!is_valid_bigint_value(rhs_string))
            return false;
        // c. Return IsLooselyEqual(x, n).
        return is_loosely_equal(global_object, lhs, js_bigint(vm, Crypto::SignedBigInteger::from_base(10, rhs_string)));
    }

    // 8. If Type(x) is String and Type(y) is BigInt, return IsLooselyEqual(y, x).
    if (lhs.is_string() && rhs.is_bigint())
        return is_loosely_equal(global_object, rhs, lhs);

    // 9. If Type(x) is Boolean, return IsLooselyEqual(! ToNumber(x), y).
    if (lhs.is_boolean())
        return is_loosely_equal(global_object, MUST(lhs.to_number(global_object)), rhs);

    // 10. If Type(y) is Boolean, return IsLooselyEqual(x, ! ToNumber(y)).
    if (rhs.is_boolean())
        return is_loosely_equal(global_object, lhs, MUST(rhs.to_number(global_object)));

    // 11. If Type(x) is either String, Number, BigInt, or Symbol and Type(y) is Object, return IsLooselyEqual(x, ? ToPrimitive(y)).
    if ((lhs.is_string() || lhs.is_number() || lhs.is_bigint() || lhs.is_symbol()) && rhs.is_object()) {
        auto rhs_primitive = TRY(rhs.to_primitive(global_object));
        return is_loosely_equal(global_object, lhs, rhs_primitive);
    }

    // 12. If Type(x) is Object and Type(y) is either String, Number, BigInt, or Symbol, return IsLooselyEqual(? ToPrimitive(x), y).
    if (lhs.is_object() && (rhs.is_string() || rhs.is_number() || rhs.is_bigint() || rhs.is_symbol())) {
        auto lhs_primitive = TRY(lhs.to_primitive(global_object));
        return is_loosely_equal(global_object, lhs_primitive, rhs);
    }

    // 13. If Type(x) is BigInt and Type(y) is Number, or if Type(x) is Number and Type(y) is BigInt, then
    if ((lhs.is_bigint() && rhs.is_number()) || (lhs.is_number() && rhs.is_bigint())) {
        // a. If x or y are any of NaN, +∞𝔽, or -∞𝔽, return false.
        if (lhs.is_nan() || lhs.is_infinity() || rhs.is_nan() || rhs.is_infinity())
            return false;

        // b. If ℝ(x) = ℝ(y), return true; otherwise return false.
        if ((lhs.is_number() && !lhs.is_integral_number()) || (rhs.is_number() && !rhs.is_integral_number()))
            return false;
        if (lhs.is_number())
            return Crypto::SignedBigInteger { MUST(lhs.to_i32(global_object)) } == rhs.as_bigint().big_integer();
        else
            return Crypto::SignedBigInteger { MUST(rhs.to_i32(global_object)) } == lhs.as_bigint().big_integer();
    }

    // 14. Return false.
    return false;
}

// 7.2.13 IsLessThan ( x, y, LeftFirst ), https://tc39.es/ecma262/#sec-islessthan
ThrowCompletionOr<TriState> is_less_than(GlobalObject& global_object, bool left_first, Value lhs, Value rhs)
{
    Value x_primitive;
    Value y_primitive;

    if (left_first) {
        x_primitive = TRY(lhs.to_primitive(global_object, Value::PreferredType::Number));
        y_primitive = TRY(rhs.to_primitive(global_object, Value::PreferredType::Number));
    } else {
        y_primitive = TRY(lhs.to_primitive(global_object, Value::PreferredType::Number));
        x_primitive = TRY(rhs.to_primitive(global_object, Value::PreferredType::Number));
    }

    if (x_primitive.is_string() && y_primitive.is_string()) {
        auto x_string = x_primitive.as_string().string();
        auto y_string = y_primitive.as_string().string();

        Utf8View x_code_points { x_string };
        Utf8View y_code_points { y_string };

        if (x_code_points.starts_with(y_code_points))
            return TriState::False;
        if (y_code_points.starts_with(x_code_points))
            return TriState::True;

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
        if (x_primitive.as_bigint().big_integer() < Crypto::SignedBigInteger::from_base(10, y_string))
            return TriState::True;
        else
            return TriState::False;
    }

    if (x_primitive.is_string() && y_primitive.is_bigint()) {
        auto& x_string = x_primitive.as_string().string();
        if (!is_valid_bigint_value(x_string))
            return TriState::Unknown;
        if (Crypto::SignedBigInteger::from_base(10, x_string) < y_primitive.as_bigint().big_integer())
            return TriState::True;
        else
            return TriState::False;
    }

    auto x_numeric = TRY(x_primitive.to_numeric(global_object));
    auto y_numeric = TRY(y_primitive.to_numeric(global_object));

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
        x_lower_than_y = x_numeric.is_integral_number()
            ? Crypto::SignedBigInteger { MUST(x_numeric.to_i32(global_object)) } < y_numeric.as_bigint().big_integer()
            : (Crypto::SignedBigInteger { MUST(x_numeric.to_i32(global_object)) } < y_numeric.as_bigint().big_integer() || Crypto::SignedBigInteger { MUST(x_numeric.to_i32(global_object)) + 1 } < y_numeric.as_bigint().big_integer());
    } else {
        x_lower_than_y = y_numeric.is_integral_number()
            ? x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { MUST(y_numeric.to_i32(global_object)) }
            : (x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { MUST(y_numeric.to_i32(global_object)) } || x_numeric.as_bigint().big_integer() < Crypto::SignedBigInteger { MUST(y_numeric.to_i32(global_object)) + 1 });
    }
    if (x_lower_than_y)
        return TriState::True;
    else
        return TriState::False;
}

// 7.3.20 Invoke ( V, P [ , argumentsList ] ), https://tc39.es/ecma262/#sec-invoke
ThrowCompletionOr<Value> Value::invoke_internal(GlobalObject& global_object, JS::PropertyKey const& property_name, Optional<MarkedValueList> arguments)
{
    auto& vm = global_object.vm();
    auto property = TRY(get(global_object, property_name));
    if (!property.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, property.to_string_without_side_effects());

    return vm.call(property.as_function(), *this, move(arguments));
}

}
