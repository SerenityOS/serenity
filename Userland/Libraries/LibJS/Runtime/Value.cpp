/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/Assertions.h>
#include <AK/CharacterTypes.h>
#include <AK/DeprecatedString.h>
#include <AK/FloatingPointStringConversions.h>
#include <AK/StringBuilder.h>
#include <AK/StringFloatingPointConversions.h>
#include <AK/Utf8View.h>
#include <LibCrypto/BigInt/SignedBigInteger.h>
#include <LibCrypto/NumberTheory/ModularFunctions.h>
#include <LibJS/Runtime/AbstractOperations.h>
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
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/SymbolObject.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>
#include <math.h>

namespace JS {

static inline bool same_type_for_equality(Value const& lhs, Value const& rhs)
{
    // If the top two bytes are identical then either:
    // both are NaN boxed Values with the same type
    // or they are doubles which happen to have the same top bytes.
    if ((lhs.encoded() & TAG_EXTRACTION) == (rhs.encoded() & TAG_EXTRACTION))
        return true;

    if (lhs.is_number() && rhs.is_number())
        return true;

    // One of the Values is not a number and they do not have the same tag
    return false;
}

static const Crypto::SignedBigInteger BIGINT_ZERO { 0 };

ALWAYS_INLINE bool both_number(Value const& lhs, Value const& rhs)
{
    return lhs.is_number() && rhs.is_number();
}

ALWAYS_INLINE bool both_bigint(Value const& lhs, Value const& rhs)
{
    return lhs.is_bigint() && rhs.is_bigint();
}

// 6.1.6.1.20 Number::toString ( x ), https://tc39.es/ecma262/#sec-numeric-types-number-tostring
// Implementation for radix = 10
DeprecatedString number_to_string(double d, NumberToStringMode mode)
{
    auto convert_to_decimal_digits_array = [](auto x, auto& digits, auto& length) {
        for (; x; x /= 10)
            digits[length++] = x % 10 | '0';
        for (i32 i = 0; 2 * i + 1 < length; ++i)
            swap(digits[i], digits[length - i - 1]);
    };

    // 1. If x is NaN, return "NaN".
    if (isnan(d))
        return "NaN";

    // 2. If x is +0𝔽 or -0𝔽, return "0".
    if (d == +0.0 || d == -0.0)
        return "0";

    // 4. If x is +∞𝔽, return "Infinity".
    if (isinf(d)) {
        if (d > 0)
            return "Infinity";
        else
            return "-Infinity";
    }

    StringBuilder builder;

    // 5. Let n, k, and s be integers such that k ≥ 1, radix ^ (k - 1) ≤ s < radix ^ k,
    // 𝔽(s × radix ^ (n - k)) is x, and k is as small as possible. Note that k is the number of
    // digits in the representation of s using radix radix, that s is not divisible by radix, and
    // that the least significant digit of s is not necessarily uniquely determined by these criteria.
    //
    // Note: guarantees provided by convert_floating_point_to_decimal_exponential_form satisfy
    //       requirements of NOTE 2.
    auto [sign, mantissa, exponent] = convert_floating_point_to_decimal_exponential_form(d);
    i32 k = 0;
    AK::Array<char, 20> mantissa_digits;
    convert_to_decimal_digits_array(mantissa, mantissa_digits, k);

    i32 n = exponent + k; // s = mantissa

    // 3. If x < -0𝔽, return the string-concatenation of "-" and Number::toString(-x, radix).
    if (sign)
        builder.append('-');

    // Non-standard: Intl needs number-to-string conversions for extremely large numbers without any
    // exponential formatting, as it will handle such formatting itself in a locale-aware way.
    bool force_no_exponent = mode == NumberToStringMode::WithoutExponent;

    // 6. If radix ≠ 10 or n is in the inclusive interval from -5 to 21, then
    if ((n >= -5 && n <= 21) || force_no_exponent) {
        // a. If n ≥ k, then
        if (n >= k) {
            // i. Return the string-concatenation of:
            // the code units of the k digits of the representation of s using radix radix
            builder.append(mantissa_digits.data(), k);
            // n - k occurrences of the code unit 0x0030 (DIGIT ZERO)
            builder.append_repeated('0', n - k);
            // b. Else if n > 0, then
        } else if (n > 0) {
            // i. Return the string-concatenation of:
            // the code units of the most significant n digits of the representation of s using radix radix
            builder.append(mantissa_digits.data(), n);
            // the code unit 0x002E (FULL STOP)
            builder.append('.');
            // the code units of the remaining k - n digits of the representation of s using radix radix
            builder.append(mantissa_digits.data() + n, k - n);
            // c. Else,
        } else {
            // i. Assert: n ≤ 0.
            VERIFY(n <= 0);
            // ii. Return the string-concatenation of:
            // the code unit 0x0030 (DIGIT ZERO)
            builder.append('0');
            // the code unit 0x002E (FULL STOP)
            builder.append('.');
            // -n occurrences of the code unit 0x0030 (DIGIT ZERO)
            builder.append_repeated('0', -n);
            // the code units of the k digits of the representation of s using radix radix
            builder.append(mantissa_digits.data(), k);
        }

        return builder.to_deprecated_string();
    }

    // 7. NOTE: In this case, the input will be represented using scientific E notation, such as 1.2e+3.

    // 9. If n < 0, then
    //     a. Let exponentSign be the code unit 0x002D (HYPHEN-MINUS).
    // 10. Else,
    //     a. Let exponentSign be the code unit 0x002B (PLUS SIGN).
    char exponent_sign = n < 0 ? '-' : '+';

    AK::Array<char, 5> exponent_digits;
    i32 exponent_length = 0;
    convert_to_decimal_digits_array(abs(n - 1), exponent_digits, exponent_length);

    // 11. If k is 1, then
    if (k == 1) {
        // a. Return the string-concatenation of:
        // the code unit of the single digit of s
        builder.append(mantissa_digits[0]);
        // the code unit 0x0065 (LATIN SMALL LETTER E)
        builder.append('e');
        // exponentSign
        builder.append(exponent_sign);
        // the code units of the decimal representation of abs(n - 1)
        builder.append(exponent_digits.data(), exponent_length);

        return builder.to_deprecated_string();
    }

    // 12. Return the string-concatenation of:
    // the code unit of the most significant digit of the decimal representation of s
    builder.append(mantissa_digits[0]);
    // the code unit 0x002E (FULL STOP)
    builder.append('.');
    // the code units of the remaining k - 1 digits of the decimal representation of s
    builder.append(mantissa_digits.data() + 1, k - 1);
    // the code unit 0x0065 (LATIN SMALL LETTER E)
    builder.append('e');
    // exponentSign
    builder.append(exponent_sign);
    // the code units of the decimal representation of abs(n - 1)
    builder.append(exponent_digits.data(), exponent_length);

    return builder.to_deprecated_string();
}

// 7.2.2 IsArray ( argument ), https://tc39.es/ecma262/#sec-isarray
ThrowCompletionOr<bool> Value::is_array(VM& vm) const
{
    // 1. If argument is not an Object, return false.
    if (!is_object())
        return false;

    auto const& object = as_object();

    // 2. If argument is an Array exotic object, return true.
    if (is<Array>(object))
        return true;

    // 3. If argument is a Proxy exotic object, then
    if (is<ProxyObject>(object)) {
        auto const& proxy = static_cast<ProxyObject const&>(object);

        // a. If argument.[[ProxyHandler]] is null, throw a TypeError exception.
        if (proxy.is_revoked())
            return vm.throw_completion<TypeError>(ErrorType::ProxyRevoked);

        // b. Let target be argument.[[ProxyTarget]].
        auto const& target = proxy.target();

        // c. Return ? IsArray(target).
        return Value(&target).is_array(vm);
    }

    // 4. Return false.
    return false;
}

Array& Value::as_array()
{
    VERIFY(is_object() && is<Array>(as_object()));
    return static_cast<Array&>(as_object());
}

// 7.2.3 IsCallable ( argument ), https://tc39.es/ecma262/#sec-iscallable
bool Value::is_function() const
{
    // 1. If argument is not an Object, return false.
    // 2. If argument has a [[Call]] internal method, return true.
    // 3. Return false.
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
ThrowCompletionOr<bool> Value::is_regexp(VM& vm) const
{
    if (!is_object())
        return false;

    auto matcher = TRY(as_object().get(*vm.well_known_symbol_match()));
    if (!matcher.is_undefined())
        return matcher.to_boolean();

    return is<RegExpObject>(as_object());
}

// 13.5.3 The typeof Operator, https://tc39.es/ecma262/#sec-typeof-operator
DeprecatedString Value::typeof() const
{
    if (is_number())
        return "number";

    switch (m_value.tag) {
    case UNDEFINED_TAG:
        return "undefined";
    case NULL_TAG:
        return "object";
    case STRING_TAG:
        return "string";
    case OBJECT_TAG:
        // B.3.7.3 Changes to the typeof Operator, https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot-typeof
        if (as_object().is_htmldda())
            return "undefined";
        if (is_function())
            return "function";
        return "object";
    case BOOLEAN_TAG:
        return "boolean";
    case SYMBOL_TAG:
        return "symbol";
    case BIGINT_TAG:
        return "bigint";
    default:
        VERIFY_NOT_REACHED();
    }
}

DeprecatedString Value::to_string_without_side_effects() const
{
    if (is_double())
        return number_to_string(m_value.as_double);

    switch (m_value.tag) {
    case UNDEFINED_TAG:
        return "undefined";
    case NULL_TAG:
        return "null";
    case BOOLEAN_TAG:
        return as_bool() ? "true" : "false";
    case INT32_TAG:
        return DeprecatedString::number(as_i32());
    case STRING_TAG:
        return as_string().deprecated_string();
    case SYMBOL_TAG:
        return as_symbol().to_deprecated_string();
    case BIGINT_TAG:
        return as_bigint().to_deprecated_string();
    case OBJECT_TAG:
        return DeprecatedString::formatted("[object {}]", as_object().class_name());
    case ACCESSOR_TAG:
        return "<accessor>";
    default:
        VERIFY_NOT_REACHED();
    }
}

ThrowCompletionOr<PrimitiveString*> Value::to_primitive_string(VM& vm)
{
    if (is_string())
        return &as_string();
    auto string = TRY(to_string(vm));
    return PrimitiveString::create(vm, string).ptr();
}

// 7.1.17 ToString ( argument ), https://tc39.es/ecma262/#sec-tostring
ThrowCompletionOr<DeprecatedString> Value::to_string(VM& vm) const
{
    if (is_double())
        return number_to_string(m_value.as_double);

    switch (m_value.tag) {
    case UNDEFINED_TAG:
        return "undefined"sv;
    case NULL_TAG:
        return "null"sv;
    case BOOLEAN_TAG:
        return as_bool() ? "true"sv : "false"sv;
    case INT32_TAG:
        return DeprecatedString::number(as_i32());
    case STRING_TAG:
        return as_string().deprecated_string();
    case SYMBOL_TAG:
        return vm.throw_completion<TypeError>(ErrorType::Convert, "symbol", "string");
    case BIGINT_TAG:
        return as_bigint().big_integer().to_base(10);
    case OBJECT_TAG: {
        auto primitive_value = TRY(to_primitive(vm, PreferredType::String));
        return primitive_value.to_string(vm);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

ThrowCompletionOr<Utf16String> Value::to_utf16_string(VM& vm) const
{
    if (is_string())
        return as_string().utf16_string();

    auto utf8_string = TRY(to_string(vm));
    return Utf16String(utf8_string);
}

// 7.1.2 ToBoolean ( argument ), https://tc39.es/ecma262/#sec-toboolean
bool Value::to_boolean() const
{
    if (is_double()) {
        if (is_nan())
            return false;
        return m_value.as_double != 0;
    }

    switch (m_value.tag) {
    case UNDEFINED_TAG:
    case NULL_TAG:
        return false;
    case BOOLEAN_TAG:
        return as_bool();
    case INT32_TAG:
        return as_i32() != 0;
    case STRING_TAG:
        return !as_string().is_empty();
    case SYMBOL_TAG:
        return true;
    case BIGINT_TAG:
        return as_bigint().big_integer() != BIGINT_ZERO;
    case OBJECT_TAG:
        // B.3.7.1 Changes to ToBoolean, https://tc39.es/ecma262/#sec-IsHTMLDDA-internal-slot-to-boolean
        if (as_object().is_htmldda())
            return false;
        return true;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 7.1.1 ToPrimitive ( input [ , preferredType ] ), https://tc39.es/ecma262/#sec-toprimitive
ThrowCompletionOr<Value> Value::to_primitive(VM& vm, PreferredType preferred_type) const
{
    auto get_hint_for_preferred_type = [&]() -> DeprecatedString {
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
        auto to_primitive_method = TRY(get_method(vm, *vm.well_known_symbol_to_primitive()));
        if (to_primitive_method) {
            auto hint = get_hint_for_preferred_type();
            auto result = TRY(call(vm, *to_primitive_method, *this, PrimitiveString::create(vm, hint)));
            if (!result.is_object())
                return result;
            return vm.throw_completion<TypeError>(ErrorType::ToPrimitiveReturnedObject, to_string_without_side_effects(), hint);
        }
        if (preferred_type == PreferredType::Default)
            preferred_type = PreferredType::Number;
        return as_object().ordinary_to_primitive(preferred_type);
    }
    return *this;
}

// 7.1.18 ToObject ( argument ), https://tc39.es/ecma262/#sec-toobject
ThrowCompletionOr<Object*> Value::to_object(VM& vm) const
{
    auto& realm = *vm.current_realm();
    VERIFY(!is_empty());
    if (is_number())
        return NumberObject::create(realm, as_double());

    switch (m_value.tag) {
    case UNDEFINED_TAG:
    case NULL_TAG:
        return vm.throw_completion<TypeError>(ErrorType::ToObjectNullOrUndefined);
    case BOOLEAN_TAG:
        return BooleanObject::create(realm, as_bool());
    case STRING_TAG:
        return StringObject::create(realm, const_cast<JS::PrimitiveString&>(as_string()), *realm.intrinsics().string_prototype());
    case SYMBOL_TAG:
        return SymbolObject::create(realm, const_cast<JS::Symbol&>(as_symbol()));
    case BIGINT_TAG:
        return BigIntObject::create(realm, const_cast<JS::BigInt&>(as_bigint()));
    case OBJECT_TAG:
        return &const_cast<Object&>(as_object());
    default:
        VERIFY_NOT_REACHED();
    }
}

// 7.1.3 ToNumeric ( value ), https://tc39.es/ecma262/#sec-tonumeric
FLATTEN ThrowCompletionOr<Value> Value::to_numeric(VM& vm) const
{
    auto primitive = TRY(to_primitive(vm, Value::PreferredType::Number));
    if (primitive.is_bigint())
        return primitive;
    return primitive.to_number(vm);
}

constexpr bool is_ascii_number(u32 code_point)
{
    return is_ascii_digit(code_point) || code_point == '.' || (code_point == 'e' || code_point == 'E') || code_point == '+' || code_point == '-';
}

struct NumberParseResult {
    StringView literal;
    u8 base;
};

static Optional<NumberParseResult> parse_number_text(StringView text)
{
    NumberParseResult result {};

    auto check_prefix = [&](auto lower_prefix, auto upper_prefix) {
        if (text.length() <= 2)
            return false;
        if (!text.starts_with(lower_prefix) && !text.starts_with(upper_prefix))
            return false;
        return true;
    };

    // https://tc39.es/ecma262/#sec-tonumber-applied-to-the-string-type
    if (check_prefix("0b"sv, "0B"sv)) {
        if (!all_of(text.substring_view(2), is_ascii_binary_digit))
            return {};

        result.literal = text.substring_view(2);
        result.base = 2;
    } else if (check_prefix("0o"sv, "0O"sv)) {
        if (!all_of(text.substring_view(2), is_ascii_octal_digit))
            return {};

        result.literal = text.substring_view(2);
        result.base = 8;
    } else if (check_prefix("0x"sv, "0X"sv)) {
        if (!all_of(text.substring_view(2), is_ascii_hex_digit))
            return {};

        result.literal = text.substring_view(2);
        result.base = 16;
    } else {
        if (!all_of(text, is_ascii_number))
            return {};

        result.literal = text;
        result.base = 10;
    }

    return result;
}

// 7.1.4.1.1 StringToNumber ( str ), https://tc39.es/ecma262/#sec-stringtonumber
Optional<Value> string_to_number(StringView string)
{
    // 1. Let text be StringToCodePoints(str).
    DeprecatedString text = Utf8View(string).trim(whitespace_characters, AK::TrimMode::Both).as_string();

    // 2. Let literal be ParseText(text, StringNumericLiteral).
    if (text.is_empty())
        return Value(0);
    if (text == "Infinity" || text == "+Infinity")
        return js_infinity();
    if (text == "-Infinity")
        return js_negative_infinity();

    auto result = parse_number_text(text);

    // 3. If literal is a List of errors, return NaN.
    if (!result.has_value())
        return js_nan();

    // 4. Return StringNumericValue of literal.
    if (result->base != 10) {
        auto bigint = Crypto::UnsignedBigInteger::from_base(result->base, result->literal);
        return Value(bigint.to_double());
    }

    auto maybe_double = text.to_double(AK::TrimWhitespace::No);
    if (!maybe_double.has_value())
        return js_nan();

    return Value(*maybe_double);
}

// 7.1.4 ToNumber ( argument ), https://tc39.es/ecma262/#sec-tonumber
ThrowCompletionOr<Value> Value::to_number(VM& vm) const
{
    VERIFY(!is_empty());
    if (is_number())
        return *this;

    switch (m_value.tag) {
    case UNDEFINED_TAG:
        return js_nan();
    case NULL_TAG:
        return Value(0);
    case BOOLEAN_TAG:
        return Value(as_bool() ? 1 : 0);
    case STRING_TAG:
        return string_to_number(as_string().deprecated_string().view());
    case SYMBOL_TAG:
        return vm.throw_completion<TypeError>(ErrorType::Convert, "symbol", "number");
    case BIGINT_TAG:
        return vm.throw_completion<TypeError>(ErrorType::Convert, "BigInt", "number");
    case OBJECT_TAG: {
        auto primitive = TRY(to_primitive(vm, PreferredType::Number));
        return primitive.to_number(vm);
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

static Optional<BigInt*> string_to_bigint(VM& vm, StringView string);

// 7.1.13 ToBigInt ( argument ), https://tc39.es/ecma262/#sec-tobigint
ThrowCompletionOr<BigInt*> Value::to_bigint(VM& vm) const
{
    auto primitive = TRY(to_primitive(vm, PreferredType::Number));

    VERIFY(!primitive.is_empty());
    if (primitive.is_number())
        return vm.throw_completion<TypeError>(ErrorType::Convert, "number", "BigInt");

    switch (primitive.m_value.tag) {
    case UNDEFINED_TAG:
        return vm.throw_completion<TypeError>(ErrorType::Convert, "undefined", "BigInt");
    case NULL_TAG:
        return vm.throw_completion<TypeError>(ErrorType::Convert, "null", "BigInt");
    case BOOLEAN_TAG: {
        auto value = primitive.as_bool() ? 1 : 0;
        return BigInt::create(vm, Crypto::SignedBigInteger { value }).ptr();
    }
    case BIGINT_TAG:
        return &primitive.as_bigint();
    case STRING_TAG: {
        // 1. Let n be ! StringToBigInt(prim).
        auto bigint = string_to_bigint(vm, primitive.as_string().deprecated_string());

        // 2. If n is undefined, throw a SyntaxError exception.
        if (!bigint.has_value())
            return vm.throw_completion<SyntaxError>(ErrorType::BigIntInvalidValue, primitive);

        // 3. Return n.
        return bigint.release_value();
    }
    case SYMBOL_TAG:
        return vm.throw_completion<TypeError>(ErrorType::Convert, "symbol", "BigInt");
    default:
        VERIFY_NOT_REACHED();
    }
}

struct BigIntParseResult {
    StringView literal;
    u8 base { 10 };
    bool is_negative { false };
};

static Optional<BigIntParseResult> parse_bigint_text(StringView text)
{
    BigIntParseResult result {};

    auto parse_for_prefixed_base = [&](auto lower_prefix, auto upper_prefix, auto validator) {
        if (text.length() <= 2)
            return false;
        if (!text.starts_with(lower_prefix) && !text.starts_with(upper_prefix))
            return false;
        return all_of(text.substring_view(2), validator);
    };

    if (parse_for_prefixed_base("0b"sv, "0B"sv, is_ascii_binary_digit)) {
        result.literal = text.substring_view(2);
        result.base = 2;
    } else if (parse_for_prefixed_base("0o"sv, "0O"sv, is_ascii_octal_digit)) {
        result.literal = text.substring_view(2);
        result.base = 8;
    } else if (parse_for_prefixed_base("0x"sv, "0X"sv, is_ascii_hex_digit)) {
        result.literal = text.substring_view(2);
        result.base = 16;
    } else {
        if (text.starts_with('-')) {
            text = text.substring_view(1);
            result.is_negative = true;
        } else if (text.starts_with('+')) {
            text = text.substring_view(1);
        }

        if (!all_of(text, is_ascii_digit))
            return {};

        result.literal = text;
        result.base = 10;
    }

    return result;
}

// 7.1.14 StringToBigInt ( str ), https://tc39.es/ecma262/#sec-stringtobigint
static Optional<BigInt*> string_to_bigint(VM& vm, StringView string)
{
    // 1. Let text be StringToCodePoints(str).
    auto text = Utf8View(string).trim(whitespace_characters, AK::TrimMode::Both).as_string();

    // 2. Let literal be ParseText(text, StringIntegerLiteral).
    auto result = parse_bigint_text(text);

    // 3. If literal is a List of errors, return undefined.
    if (!result.has_value())
        return {};

    // 4. Let mv be the MV of literal.
    // 5. Assert: mv is an integer.
    auto bigint = Crypto::SignedBigInteger::from_base(result->base, result->literal);
    if (result->is_negative && (bigint != BIGINT_ZERO))
        bigint.negate();

    // 6. Return ℤ(mv).
    return BigInt::create(vm, move(bigint));
}

// 7.1.15 ToBigInt64 ( argument ), https://tc39.es/ecma262/#sec-tobigint64
ThrowCompletionOr<i64> Value::to_bigint_int64(VM& vm) const
{
    auto* bigint = TRY(to_bigint(vm));
    return static_cast<i64>(bigint->big_integer().to_u64());
}

// 7.1.16 ToBigUint64 ( argument ), https://tc39.es/ecma262/#sec-tobiguint64
ThrowCompletionOr<u64> Value::to_bigint_uint64(VM& vm) const
{
    auto* bigint = TRY(to_bigint(vm));
    return bigint->big_integer().to_u64();
}

ThrowCompletionOr<double> Value::to_double(VM& vm) const
{
    return TRY(to_number(vm)).as_double();
}

// 7.1.19 ToPropertyKey ( argument ), https://tc39.es/ecma262/#sec-topropertykey
ThrowCompletionOr<PropertyKey> Value::to_property_key(VM& vm) const
{
    if (is_int32() && as_i32() >= 0)
        return PropertyKey { as_i32() };
    auto key = TRY(to_primitive(vm, PreferredType::String));
    if (key.is_symbol())
        return &key.as_symbol();
    return TRY(key.to_string(vm));
}

ThrowCompletionOr<i32> Value::to_i32_slow_case(VM& vm) const
{
    VERIFY(!is_int32());
    double value = TRY(to_number(vm)).as_double();
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

ThrowCompletionOr<i32> Value::to_i32(VM& vm) const
{
    if (is_int32())
        return as_i32();
    return to_i32_slow_case(vm);
}

// 7.1.7 ToUint32 ( argument ), https://tc39.es/ecma262/#sec-touint32
ThrowCompletionOr<u32> Value::to_u32(VM& vm) const
{
    double value = TRY(to_number(vm)).as_double();
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
ThrowCompletionOr<i16> Value::to_i16(VM& vm) const
{
    double value = TRY(to_number(vm)).as_double();
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
ThrowCompletionOr<u16> Value::to_u16(VM& vm) const
{
    double value = TRY(to_number(vm)).as_double();
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
ThrowCompletionOr<i8> Value::to_i8(VM& vm) const
{
    double value = TRY(to_number(vm)).as_double();
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
ThrowCompletionOr<u8> Value::to_u8(VM& vm) const
{
    double value = TRY(to_number(vm)).as_double();
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
ThrowCompletionOr<u8> Value::to_u8_clamp(VM& vm) const
{
    auto number = TRY(to_number(vm));
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
ThrowCompletionOr<size_t> Value::to_length(VM& vm) const
{
    auto len = TRY(to_integer_or_infinity(vm));
    if (len <= 0)
        return 0;
    // FIXME: The spec says that this function's output range is 0 - 2^53-1. But we don't want to overflow the size_t.
    constexpr double length_limit = sizeof(void*) == 4 ? NumericLimits<size_t>::max() : MAX_ARRAY_LIKE_INDEX;
    return min(len, length_limit);
}

// 7.1.22 ToIndex ( argument ), https://tc39.es/ecma262/#sec-toindex
ThrowCompletionOr<size_t> Value::to_index(VM& vm) const
{
    if (is_undefined())
        return 0;
    auto integer_index = TRY(to_integer_or_infinity(vm));
    if (integer_index < 0)
        return vm.throw_completion<RangeError>(ErrorType::InvalidIndex);
    auto index = MUST(Value(integer_index).to_length(vm));
    if (integer_index != index)
        return vm.throw_completion<RangeError>(ErrorType::InvalidIndex);
    return index;
}

// 7.1.5 ToIntegerOrInfinity ( argument ), https://tc39.es/ecma262/#sec-tointegerorinfinity
ThrowCompletionOr<double> Value::to_integer_or_infinity(VM& vm) const
{
    auto number = TRY(to_number(vm));
    if (number.is_nan() || number.as_double() == 0)
        return 0;
    if (number.is_infinity())
        return number.as_double();
    auto integer = floor(fabs(number.as_double()));
    if (number.as_double() < 0 && integer != 0)
        integer = -integer;
    return integer;
}

// Standalone variant using plain doubles for cases where we already got numbers and know the AO won't throw.
double to_integer_or_infinity(double number)
{
    // 1. Let number be ? ToNumber(argument).

    // 2. If number is NaN, +0𝔽, or -0𝔽, return 0.
    if (isnan(number) || number == 0)
        return 0;

    // 3. If number is +∞𝔽, return +∞.
    if (__builtin_isinf_sign(number) > 0)
        return static_cast<double>(INFINITY);

    // 4. If number is -∞𝔽, return -∞.
    if (__builtin_isinf_sign(number) < 0)
        return static_cast<double>(-INFINITY);

    // 5. Let integer be floor(abs(ℝ(number))).
    auto integer = floor(fabs(number));

    // 6. If number < -0𝔽, set integer to -integer.
    if (number < 0 && integer != 0)
        integer = -integer;

    // 7. Return integer.
    return integer;
}

// 7.3.3 GetV ( V, P ), https://tc39.es/ecma262/#sec-getv
ThrowCompletionOr<Value> Value::get(VM& vm, PropertyKey const& property_key) const
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. Let O be ? ToObject(V).
    auto* object = TRY(to_object(vm));

    // 3. Return ? O.[[Get]](P, V).
    return TRY(object->internal_get(property_key, *this));
}

// 7.3.11 GetMethod ( V, P ), https://tc39.es/ecma262/#sec-getmethod
ThrowCompletionOr<FunctionObject*> Value::get_method(VM& vm, PropertyKey const& property_key) const
{
    // 1. Assert: IsPropertyKey(P) is true.
    VERIFY(property_key.is_valid());

    // 2. Let func be ? GetV(V, P).
    auto function = TRY(get(vm, property_key));

    // 3. If func is either undefined or null, return undefined.
    if (function.is_nullish())
        return nullptr;

    // 4. If IsCallable(func) is false, throw a TypeError exception.
    if (!function.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, function.to_string_without_side_effects());

    // 5. Return func.
    return &function.as_function();
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> greater_than(VM& vm, Value lhs, Value rhs)
{
    if (lhs.is_int32() && rhs.is_int32())
        return lhs.as_i32() > rhs.as_i32();

    TriState relation = TRY(is_less_than(vm, lhs, rhs, false));
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> greater_than_equals(VM& vm, Value lhs, Value rhs)
{
    if (lhs.is_int32() && rhs.is_int32())
        return lhs.as_i32() >= rhs.as_i32();

    TriState relation = TRY(is_less_than(vm, lhs, rhs, true));
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> less_than(VM& vm, Value lhs, Value rhs)
{
    if (lhs.is_int32() && rhs.is_int32())
        return lhs.as_i32() < rhs.as_i32();

    TriState relation = TRY(is_less_than(vm, lhs, rhs, true));
    if (relation == TriState::Unknown)
        return Value(false);
    return Value(relation == TriState::True);
}

// 13.10 Relational Operators, https://tc39.es/ecma262/#sec-relational-operators
ThrowCompletionOr<Value> less_than_equals(VM& vm, Value lhs, Value rhs)
{
    if (lhs.is_int32() && rhs.is_int32())
        return lhs.as_i32() <= rhs.as_i32();

    TriState relation = TRY(is_less_than(vm, lhs, rhs, false));
    if (relation == TriState::Unknown || relation == TriState::True)
        return Value(false);
    return Value(true);
}

// 13.12 Binary Bitwise Operators, https://tc39.es/ecma262/#sec-binary-bitwise-operators
ThrowCompletionOr<Value> bitwise_and(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() || !rhs_numeric.is_finite_number())
            return Value(0);
        return Value(TRY(lhs_numeric.to_i32(vm)) & TRY(rhs_numeric.to_i32(vm)));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().bitwise_and(rhs_numeric.as_bigint().big_integer()));
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "bitwise AND");
}

// 13.12 Binary Bitwise Operators, https://tc39.es/ecma262/#sec-binary-bitwise-operators
ThrowCompletionOr<Value> bitwise_or(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value(TRY(lhs_numeric.to_i32(vm)) | TRY(rhs_numeric.to_i32(vm)));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().bitwise_or(rhs_numeric.as_bigint().big_integer()));
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "bitwise OR");
}

// 13.12 Binary Bitwise Operators, https://tc39.es/ecma262/#sec-binary-bitwise-operators
ThrowCompletionOr<Value> bitwise_xor(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number() && !rhs_numeric.is_finite_number())
            return Value(0);
        if (!lhs_numeric.is_finite_number())
            return rhs_numeric;
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        return Value(TRY(lhs_numeric.to_i32(vm)) ^ TRY(rhs_numeric.to_i32(vm)));
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().bitwise_xor(rhs_numeric.as_bigint().big_integer()));
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "bitwise XOR");
}

// 13.5.6 Bitwise NOT Operator ( ~ ), https://tc39.es/ecma262/#sec-bitwise-not-operator
ThrowCompletionOr<Value> bitwise_not(VM& vm, Value lhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    if (lhs_numeric.is_number())
        return Value(~TRY(lhs_numeric.to_i32(vm)));
    return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().bitwise_not());
}

// 13.5.4 Unary + Operator, https://tc39.es/ecma262/#sec-unary-plus-operator
ThrowCompletionOr<Value> unary_plus(VM& vm, Value lhs)
{
    return TRY(lhs.to_number(vm));
}

// 13.5.5 Unary - Operator, https://tc39.es/ecma262/#sec-unary-minus-operator
ThrowCompletionOr<Value> unary_minus(VM& vm, Value lhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    if (lhs_numeric.is_number()) {
        if (lhs_numeric.is_nan())
            return js_nan();
        return Value(-lhs_numeric.as_double());
    }
    if (lhs_numeric.as_bigint().big_integer() == BIGINT_ZERO)
        return BigInt::create(vm, BIGINT_ZERO);
    auto big_integer_negated = lhs_numeric.as_bigint().big_integer();
    big_integer_negated.negate();
    return BigInt::create(vm, big_integer_negated);
}

// 13.9.1 The Left Shift Operator ( << ), https://tc39.es/ecma262/#sec-left-shift-operator
ThrowCompletionOr<Value> left_shift(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        // Ok, so this performs toNumber() again but that "can't" throw
        auto lhs_i32 = MUST(lhs_numeric.to_i32(vm));
        auto rhs_u32 = MUST(rhs_numeric.to_u32(vm)) % 32;
        return Value(lhs_i32 << rhs_u32);
    }
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        // 6.1.6.2.9 BigInt::leftShift ( x, y ), https://tc39.es/ecma262/#sec-numeric-types-bigint-leftShift
        auto multiplier_divisor = Crypto::SignedBigInteger { Crypto::NumberTheory::Power(Crypto::UnsignedBigInteger(2), rhs_numeric.as_bigint().big_integer().unsigned_value()) };

        // 1. If y < 0ℤ, then
        if (rhs_numeric.as_bigint().big_integer().is_negative()) {
            // a. Return the BigInt value that represents ℝ(x) / 2^-y, rounding down to the nearest integer, including for negative numbers.
            // NOTE: Since y is negative we can just do ℝ(x) / 2^|y|
            auto const& big_integer = lhs_numeric.as_bigint().big_integer();
            auto division_result = big_integer.divided_by(multiplier_divisor);

            // For positive initial values and no remainder just return quotient
            if (division_result.remainder.is_zero() || !big_integer.is_negative())
                return BigInt::create(vm, division_result.quotient);
            // For negative round "down" to the next negative number
            return BigInt::create(vm, division_result.quotient.minus(Crypto::SignedBigInteger { 1 }));
        }
        // 2. Return the BigInt value that represents ℝ(x) × 2^y.
        return Value(BigInt::create(vm, lhs_numeric.as_bigint().big_integer().multiplied_by(multiplier_divisor)));
    }
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "left-shift");
}

// 13.9.2 The Signed Right Shift Operator ( >> ), https://tc39.es/ecma262/#sec-signed-right-shift-operator
ThrowCompletionOr<Value> right_shift(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        auto lhs_i32 = MUST(lhs_numeric.to_i32(vm));
        auto rhs_u32 = MUST(rhs_numeric.to_u32(vm)) % 32;
        return Value(lhs_i32 >> rhs_u32);
    }
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        auto rhs_negated = rhs_numeric.as_bigint().big_integer();
        rhs_negated.negate();
        return left_shift(vm, lhs, BigInt::create(vm, rhs_negated));
    }
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "right-shift");
}

// 13.9.3 The Unsigned Right Shift Operator ( >>> ), https://tc39.es/ecma262/#sec-unsigned-right-shift-operator
ThrowCompletionOr<Value> unsigned_right_shift(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        if (!lhs_numeric.is_finite_number())
            return Value(0);
        if (!rhs_numeric.is_finite_number())
            return lhs_numeric;
        // Ok, so this performs toNumber() again but that "can't" throw
        auto lhs_u32 = MUST(lhs_numeric.to_u32(vm));
        auto rhs_u32 = MUST(rhs_numeric.to_u32(vm)) % 32;
        return Value(lhs_u32 >> rhs_u32);
    }
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperator, "unsigned right-shift");
}

// 13.8.1 The Addition Operator ( + ), https://tc39.es/ecma262/#sec-addition-operator-plus
ThrowCompletionOr<Value> add(VM& vm, Value lhs, Value rhs)
{
    if (both_number(lhs, rhs)) {
        if (lhs.is_int32() && rhs.is_int32()) {
            Checked<i32> result;
            result = MUST(lhs.to_i32(vm));
            result += MUST(rhs.to_i32(vm));
            if (!result.has_overflow())
                return Value(result.value());
        }
        return Value(lhs.as_double() + rhs.as_double());
    }
    auto lhs_primitive = TRY(lhs.to_primitive(vm));
    auto rhs_primitive = TRY(rhs.to_primitive(vm));

    if (lhs_primitive.is_string() || rhs_primitive.is_string()) {
        auto lhs_string = TRY(lhs_primitive.to_primitive_string(vm));
        auto rhs_string = TRY(rhs_primitive.to_primitive_string(vm));
        return PrimitiveString::create(vm, *lhs_string, *rhs_string);
    }

    auto lhs_numeric = TRY(lhs_primitive.to_numeric(vm));
    auto rhs_numeric = TRY(rhs_primitive.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() + rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().plus(rhs_numeric.as_bigint().big_integer()));
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "addition");
}

// 13.8.2 The Subtraction Operator ( - ), https://tc39.es/ecma262/#sec-subtraction-operator-minus
ThrowCompletionOr<Value> sub(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        double lhsd = lhs_numeric.as_double();
        double rhsd = rhs_numeric.as_double();
        double interm = lhsd - rhsd;
        return Value(interm);
    }
    if (both_bigint(lhs_numeric, rhs_numeric))
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().minus(rhs_numeric.as_bigint().big_integer()));
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "subtraction");
}

// 13.7 Multiplicative Operators, https://tc39.es/ecma262/#sec-multiplicative-operators
ThrowCompletionOr<Value> mul(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() * rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric))
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().multiplied_by(rhs_numeric.as_bigint().big_integer()));
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "multiplication");
}

// 13.7 Multiplicative Operators, https://tc39.es/ecma262/#sec-multiplicative-operators
ThrowCompletionOr<Value> div(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric))
        return Value(lhs_numeric.as_double() / rhs_numeric.as_double());
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer() == BIGINT_ZERO)
            return vm.throw_completion<RangeError>(ErrorType::DivisionByZero);
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).quotient);
    }
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "division");
}

// 13.7 Multiplicative Operators, https://tc39.es/ecma262/#sec-multiplicative-operators
ThrowCompletionOr<Value> mod(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric)) {
        // 6.1.6.1.6 Number::remainder ( n, d ), https://tc39.es/ecma262/#sec-numeric-types-number-remainder
        // The ECMA specification is describing the mathematical definition of modulus
        // implemented by fmod.
        auto n = lhs_numeric.as_double();
        auto d = rhs_numeric.as_double();
        return Value(fmod(n, d));
    }
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer() == BIGINT_ZERO)
            return vm.throw_completion<RangeError>(ErrorType::DivisionByZero);
        return BigInt::create(vm, lhs_numeric.as_bigint().big_integer().divided_by(rhs_numeric.as_bigint().big_integer()).remainder);
    }
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "modulo");
}

static Value exp_double(Value base, Value exponent)
{
    VERIFY(both_number(base, exponent));
    if (exponent.is_nan())
        return js_nan();
    if (exponent.is_positive_zero() || exponent.is_negative_zero())
        return Value(1);
    if (base.is_nan())
        return js_nan();
    if (base.is_positive_infinity())
        return exponent.as_double() > 0 ? js_infinity() : Value(0);
    if (base.is_negative_infinity()) {
        auto is_odd_integral_number = exponent.is_integral_number() && (static_cast<i32>(exponent.as_double()) % 2 != 0);
        if (exponent.as_double() > 0)
            return is_odd_integral_number ? js_negative_infinity() : js_infinity();
        else
            return is_odd_integral_number ? Value(-0.0) : Value(0);
    }
    if (base.is_positive_zero())
        return exponent.as_double() > 0 ? Value(0) : js_infinity();
    if (base.is_negative_zero()) {
        auto is_odd_integral_number = exponent.is_integral_number() && (static_cast<i32>(exponent.as_double()) % 2 != 0);
        if (exponent.as_double() > 0)
            return is_odd_integral_number ? Value(-0.0) : Value(0);
        else
            return is_odd_integral_number ? js_negative_infinity() : js_infinity();
    }
    VERIFY(base.is_finite_number() && !base.is_positive_zero() && !base.is_negative_zero());
    if (exponent.is_positive_infinity()) {
        auto absolute_base = fabs(base.as_double());
        if (absolute_base > 1)
            return js_infinity();
        else if (absolute_base == 1)
            return js_nan();
        else if (absolute_base < 1)
            return Value(0);
    }
    if (exponent.is_negative_infinity()) {
        auto absolute_base = fabs(base.as_double());
        if (absolute_base > 1)
            return Value(0);
        else if (absolute_base == 1)
            return js_nan();
        else if (absolute_base < 1)
            return js_infinity();
    }
    VERIFY(exponent.is_finite_number() && !exponent.is_positive_zero() && !exponent.is_negative_zero());
    if (base.as_double() < 0 && !exponent.is_integral_number())
        return js_nan();
    return Value(::pow(base.as_double(), exponent.as_double()));
}

// 13.6 Exponentiation Operator, https://tc39.es/ecma262/#sec-exp-operator
ThrowCompletionOr<Value> exp(VM& vm, Value lhs, Value rhs)
{
    auto lhs_numeric = TRY(lhs.to_numeric(vm));
    auto rhs_numeric = TRY(rhs.to_numeric(vm));
    if (both_number(lhs_numeric, rhs_numeric))
        return exp_double(lhs_numeric, rhs_numeric);
    if (both_bigint(lhs_numeric, rhs_numeric)) {
        if (rhs_numeric.as_bigint().big_integer().is_negative())
            return vm.throw_completion<RangeError>(ErrorType::NegativeExponent);
        return BigInt::create(vm, Crypto::NumberTheory::Power(lhs_numeric.as_bigint().big_integer(), rhs_numeric.as_bigint().big_integer()));
    }
    return vm.throw_completion<TypeError>(ErrorType::BigIntBadOperatorOtherType, "exponentiation");
}

ThrowCompletionOr<Value> in(VM& vm, Value lhs, Value rhs)
{
    if (!rhs.is_object())
        return vm.throw_completion<TypeError>(ErrorType::InOperatorWithObject);
    auto lhs_property_key = TRY(lhs.to_property_key(vm));
    return Value(TRY(rhs.as_object().has_property(lhs_property_key)));
}

// 13.10.2 InstanceofOperator ( V, target ), https://tc39.es/ecma262/#sec-instanceofoperator
ThrowCompletionOr<Value> instance_of(VM& vm, Value lhs, Value rhs)
{
    if (!rhs.is_object())
        return vm.throw_completion<TypeError>(ErrorType::NotAnObject, rhs.to_string_without_side_effects());
    auto has_instance_method = TRY(rhs.get_method(vm, *vm.well_known_symbol_has_instance()));
    if (has_instance_method) {
        auto has_instance_result = TRY(call(vm, *has_instance_method, rhs, lhs));
        return Value(has_instance_result.to_boolean());
    }
    if (!rhs.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, rhs.to_string_without_side_effects());
    return TRY(ordinary_has_instance(vm, lhs, rhs));
}

// 7.3.22 OrdinaryHasInstance ( C, O ), https://tc39.es/ecma262/#sec-ordinaryhasinstance
ThrowCompletionOr<Value> ordinary_has_instance(VM& vm, Value lhs, Value rhs)
{
    if (!rhs.is_function())
        return Value(false);
    auto& rhs_function = rhs.as_function();

    if (is<BoundFunction>(rhs_function)) {
        auto& bound_target = static_cast<BoundFunction const&>(rhs_function);
        return instance_of(vm, lhs, Value(&bound_target.bound_target_function()));
    }

    if (!lhs.is_object())
        return Value(false);

    Object* lhs_object = &lhs.as_object();
    auto rhs_prototype = TRY(rhs_function.get(vm.names.prototype));
    if (!rhs_prototype.is_object())
        return vm.throw_completion<TypeError>(ErrorType::InstanceOfOperatorBadPrototype, rhs.to_string_without_side_effects());
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

    if (lhs.is_string())
        return lhs.as_string().deprecated_string() == rhs.as_string().deprecated_string();

    return lhs.m_value.encoded == rhs.m_value.encoded;
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
ThrowCompletionOr<bool> is_loosely_equal(VM& vm, Value lhs, Value rhs)
{
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
    // 4. Perform the following steps:
    // a. If Type(x) is Object and x has an [[IsHTMLDDA]] internal slot and y is either null or undefined, return true.
    if (lhs.is_object() && lhs.as_object().is_htmldda() && rhs.is_nullish())
        return true;

    // b. If x is either null or undefined and Type(y) is Object and y has an [[IsHTMLDDA]] internal slot, return true.
    if (lhs.is_nullish() && rhs.is_object() && rhs.as_object().is_htmldda())
        return true;

    // == End of B.3.6.2 ==

    // 5. If Type(x) is Number and Type(y) is String, return ! IsLooselyEqual(x, ! ToNumber(y)).
    if (lhs.is_number() && rhs.is_string())
        return is_loosely_equal(vm, lhs, MUST(rhs.to_number(vm)));

    // 6. If Type(x) is String and Type(y) is Number, return ! IsLooselyEqual(! ToNumber(x), y).
    if (lhs.is_string() && rhs.is_number())
        return is_loosely_equal(vm, MUST(lhs.to_number(vm)), rhs);

    // 7. If Type(x) is BigInt and Type(y) is String, then
    if (lhs.is_bigint() && rhs.is_string()) {
        // a. Let n be StringToBigInt(y).
        auto bigint = string_to_bigint(vm, rhs.as_string().deprecated_string());

        // b. If n is undefined, return false.
        if (!bigint.has_value())
            return false;

        // c. Return ! IsLooselyEqual(x, n).
        return is_loosely_equal(vm, lhs, *bigint);
    }

    // 8. If Type(x) is String and Type(y) is BigInt, return ! IsLooselyEqual(y, x).
    if (lhs.is_string() && rhs.is_bigint())
        return is_loosely_equal(vm, rhs, lhs);

    // 9. If Type(x) is Boolean, return ! IsLooselyEqual(! ToNumber(x), y).
    if (lhs.is_boolean())
        return is_loosely_equal(vm, MUST(lhs.to_number(vm)), rhs);

    // 10. If Type(y) is Boolean, return ! IsLooselyEqual(x, ! ToNumber(y)).
    if (rhs.is_boolean())
        return is_loosely_equal(vm, lhs, MUST(rhs.to_number(vm)));

    // 11. If Type(x) is either String, Number, BigInt, or Symbol and Type(y) is Object, return ! IsLooselyEqual(x, ? ToPrimitive(y)).
    if ((lhs.is_string() || lhs.is_number() || lhs.is_bigint() || lhs.is_symbol()) && rhs.is_object()) {
        auto rhs_primitive = TRY(rhs.to_primitive(vm));
        return is_loosely_equal(vm, lhs, rhs_primitive);
    }

    // 12. If Type(x) is Object and Type(y) is either String, Number, BigInt, or Symbol, return ! IsLooselyEqual(? ToPrimitive(x), y).
    if (lhs.is_object() && (rhs.is_string() || rhs.is_number() || rhs.is_bigint() || rhs.is_symbol())) {
        auto lhs_primitive = TRY(lhs.to_primitive(vm));
        return is_loosely_equal(vm, lhs_primitive, rhs);
    }

    // 13. If Type(x) is BigInt and Type(y) is Number, or if Type(x) is Number and Type(y) is BigInt, then
    if ((lhs.is_bigint() && rhs.is_number()) || (lhs.is_number() && rhs.is_bigint())) {
        // a. If x or y are any of NaN, +∞𝔽, or -∞𝔽, return false.
        if (lhs.is_nan() || lhs.is_infinity() || rhs.is_nan() || rhs.is_infinity())
            return false;

        // b. If ℝ(x) = ℝ(y), return true; otherwise return false.
        if ((lhs.is_number() && !lhs.is_integral_number()) || (rhs.is_number() && !rhs.is_integral_number()))
            return false;

        VERIFY(!lhs.is_nan() && !rhs.is_nan());

        auto& number_side = lhs.is_number() ? lhs : rhs;
        auto& bigint_side = lhs.is_number() ? rhs : lhs;

        return bigint_side.as_bigint().big_integer().compare_to_double(number_side.as_double()) == Crypto::UnsignedBigInteger::CompareResult::DoubleEqualsBigInt;
    }

    // 14. Return false.
    return false;
}

// 7.2.13 IsLessThan ( x, y, LeftFirst ), https://tc39.es/ecma262/#sec-islessthan
ThrowCompletionOr<TriState> is_less_than(VM& vm, Value lhs, Value rhs, bool left_first)
{
    Value x_primitive;
    Value y_primitive;

    if (left_first) {
        x_primitive = TRY(lhs.to_primitive(vm, Value::PreferredType::Number));
        y_primitive = TRY(rhs.to_primitive(vm, Value::PreferredType::Number));
    } else {
        y_primitive = TRY(lhs.to_primitive(vm, Value::PreferredType::Number));
        x_primitive = TRY(rhs.to_primitive(vm, Value::PreferredType::Number));
    }

    if (x_primitive.is_string() && y_primitive.is_string()) {
        auto x_string = x_primitive.as_string().deprecated_string();
        auto y_string = y_primitive.as_string().deprecated_string();

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

        return x_code_points.length() < y_code_points.length()
            ? TriState::True
            : TriState::False;
    }

    if (x_primitive.is_bigint() && y_primitive.is_string()) {
        auto y_bigint = string_to_bigint(vm, y_primitive.as_string().deprecated_string());
        if (!y_bigint.has_value())
            return TriState::Unknown;

        if (x_primitive.as_bigint().big_integer() < (*y_bigint)->big_integer())
            return TriState::True;
        return TriState::False;
    }

    if (x_primitive.is_string() && y_primitive.is_bigint()) {
        auto x_bigint = string_to_bigint(vm, x_primitive.as_string().deprecated_string());
        if (!x_bigint.has_value())
            return TriState::Unknown;

        if ((*x_bigint)->big_integer() < y_primitive.as_bigint().big_integer())
            return TriState::True;
        return TriState::False;
    }

    auto x_numeric = TRY(x_primitive.to_numeric(vm));
    auto y_numeric = TRY(y_primitive.to_numeric(vm));

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
    VERIFY(!x_numeric.is_nan() && !y_numeric.is_nan());
    if (x_numeric.is_number()) {
        x_lower_than_y = y_numeric.as_bigint().big_integer().compare_to_double(x_numeric.as_double())
            == Crypto::UnsignedBigInteger::CompareResult::DoubleLessThanBigInt;
    } else {
        x_lower_than_y = x_numeric.as_bigint().big_integer().compare_to_double(y_numeric.as_double())
            == Crypto::UnsignedBigInteger::CompareResult::DoubleGreaterThanBigInt;
    }
    if (x_lower_than_y)
        return TriState::True;
    else
        return TriState::False;
}

// 7.3.21 Invoke ( V, P [ , argumentsList ] ), https://tc39.es/ecma262/#sec-invoke
ThrowCompletionOr<Value> Value::invoke_internal(VM& vm, PropertyKey const& property_key, Optional<MarkedVector<Value>> arguments)
{
    auto property = TRY(get(vm, property_key));
    if (!property.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, property.to_string_without_side_effects());

    return call(vm, property.as_function(), *this, move(arguments));
}

}
