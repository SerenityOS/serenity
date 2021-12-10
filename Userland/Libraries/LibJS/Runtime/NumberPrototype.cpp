/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/NumberObject.h>
#include <LibJS/Runtime/NumberPrototype.h>

namespace JS {

static const u8 max_precision_for_radix[37] = {
    // clang-format off
    0,  0,  52, 32, 26, 22, 20, 18, 17, 16,
    15, 15, 14, 14, 13, 13, 13, 12, 12, 12,
    12, 11, 11, 11, 11, 11, 11, 10, 10, 10,
    10, 10, 10, 10, 10, 10, 10,
    // clang-format on
};

static char digits[] = "0123456789abcdefghijklmnopqrstuvwxyz";

NumberPrototype::NumberPrototype(GlobalObject& global_object)
    : NumberObject(0, *global_object.object_prototype())
{
}

void NumberPrototype::initialize(GlobalObject& object)
{
    auto& vm = this->vm();
    Object::initialize(object);
    u8 attr = Attribute::Configurable | Attribute::Writable;
    define_native_function(vm.names.toFixed, to_fixed, 1, attr);
    define_native_function(vm.names.toLocaleString, to_locale_string, 0, attr);
    define_native_function(vm.names.toString, to_string, 1, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

NumberPrototype::~NumberPrototype()
{
}

// thisNumberValue ( value ), https://tc39.es/ecma262/#thisnumbervalue
static ThrowCompletionOr<Value> this_number_value(GlobalObject& global_object, Value value)
{
    if (value.is_number())
        return value;
    if (value.is_object() && is<NumberObject>(value.as_object()))
        return Value(static_cast<NumberObject&>(value.as_object()).number());
    auto& vm = global_object.vm();
    return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "Number");
}

// 21.1.3.3 Number.prototype.toFixed ( fractionDigits ), https://tc39.es/ecma262/#sec-number.prototype.tofixed
JS_DEFINE_NATIVE_FUNCTION(NumberPrototype::to_fixed)
{
    auto number_value = TRY(this_number_value(global_object, vm.this_value(global_object)));
    auto fraction_digits = TRY(vm.argument(0).to_integer_or_infinity(global_object));
    if (!vm.argument(0).is_finite_number())
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidFractionDigits);

    if (fraction_digits < 0 || fraction_digits > 100)
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidFractionDigits);

    if (!number_value.is_finite_number())
        return js_string(vm, TRY(number_value.to_string(global_object)));

    auto number = number_value.as_double();
    if (fabs(number) >= 1e+21)
        return js_string(vm, MUST(number_value.to_string(global_object)));

    return js_string(vm, String::formatted("{:0.{1}}", number, static_cast<size_t>(fraction_digits)));
}

// 18.2.1 Number.prototype.toLocaleString ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sup-number.prototype.tolocalestring
JS_DEFINE_NATIVE_FUNCTION(NumberPrototype::to_locale_string)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let x be ? thisNumberValue(this value).
    auto number_value = TRY(this_number_value(global_object, vm.this_value(global_object)));

    MarkedValueList arguments { vm.heap() };
    arguments.append(locales);
    arguments.append(options);

    // 2. Let numberFormat be ? Construct(%NumberFormat%, « locales, options »).
    auto* number_format = static_cast<Intl::NumberFormat*>(TRY(construct(global_object, *global_object.intl_number_format_constructor(), move(arguments))));

    // 3. Return ? FormatNumeric(numberFormat, x).
    // Note: Our implementation of FormatNumeric does not throw.
    auto formatted = Intl::format_numeric(*number_format, number_value.as_double());

    return js_string(vm, move(formatted));
}

// 21.1.3.6 Number.prototype.toString ( [ radix ] ), https://tc39.es/ecma262/#sec-number.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(NumberPrototype::to_string)
{
    auto number_value = TRY(this_number_value(global_object, vm.this_value(global_object)));
    double radix_argument = 10;
    auto argument = vm.argument(0);
    if (!vm.argument(0).is_undefined())
        radix_argument = TRY(argument.to_integer_or_infinity(global_object));
    int radix = (int)radix_argument;

    if (radix < 2 || radix > 36)
        return vm.throw_completion<RangeError>(global_object, ErrorType::InvalidRadix);

    if (number_value.is_positive_infinity())
        return js_string(vm, "Infinity");
    if (number_value.is_negative_infinity())
        return js_string(vm, "-Infinity");
    if (number_value.is_nan())
        return js_string(vm, "NaN");
    if (number_value.is_positive_zero() || number_value.is_negative_zero())
        return js_string(vm, "0");

    double number = number_value.as_double();
    bool negative = number < 0;
    if (negative)
        number *= -1;

    u64 int_part = floor(number);
    double decimal_part = number - int_part;

    Vector<char> backwards_characters;

    if (int_part == 0) {
        backwards_characters.append('0');
    } else {
        while (int_part > 0) {
            backwards_characters.append(digits[int_part % radix]);
            int_part /= radix;
        }
    }

    Vector<char> characters;
    if (negative)
        characters.append('-');

    // Reverse characters;
    for (ssize_t i = backwards_characters.size() - 1; i >= 0; --i) {
        characters.append(backwards_characters[i]);
    }

    // decimal part
    if (decimal_part != 0.0) {
        characters.append('.');

        u8 precision = max_precision_for_radix[radix];

        for (u8 i = 0; i < precision; ++i) {
            decimal_part *= radix;
            u64 integral = floor(decimal_part);
            characters.append(digits[integral]);
            decimal_part -= integral;
        }

        while (characters.last() == '0')
            characters.take_last();
    }

    return js_string(vm, String(characters.data(), characters.size()));
}

// 21.1.3.7 Number.prototype.valueOf ( ), https://tc39.es/ecma262/#sec-number.prototype.valueof
JS_DEFINE_NATIVE_FUNCTION(NumberPrototype::value_of)
{
    return this_number_value(global_object, vm.this_value(global_object));
}

}
