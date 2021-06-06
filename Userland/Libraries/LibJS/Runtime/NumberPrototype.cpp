/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
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
    define_native_function(vm.names.toString, to_string, 1, attr);
    define_native_function(vm.names.valueOf, value_of, 0, attr);
}

NumberPrototype::~NumberPrototype()
{
}

// https://tc39.es/ecma262/#thisnumbervalue
static Value this_number_value(GlobalObject& global_object, StringView const& name)
{
    auto& vm = global_object.vm();
    auto this_value = vm.this_value(global_object);
    if (this_value.is_number())
        return this_value;
    if (this_value.is_object() && is<NumberObject>(this_value.as_object()))
        return static_cast<NumberObject&>(this_value.as_object()).value_of();
    vm.throw_exception<TypeError>(global_object, ErrorType::NumberIncompatibleThis, name);
    return {};
}

JS_DEFINE_NATIVE_FUNCTION(NumberPrototype::to_string)
{
    auto number_value = this_number_value(global_object, "toString");
    if (vm.exception())
        return {};

    double radix_argument = 10;
    auto argument = vm.argument(0);
    if (!vm.argument(0).is_undefined()) {
        radix_argument = argument.to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
    }
    int radix = (int)radix_argument;

    if (vm.exception() || radix < 2 || radix > 36) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidRadix);
        return {};
    }

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

JS_DEFINE_NATIVE_FUNCTION(NumberPrototype::value_of)
{
    return this_number_value(global_object, "valueOf");
}

}
