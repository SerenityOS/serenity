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
    Object::initialize(object);

    define_native_function("toString", to_string, 1, Attribute::Configurable | Attribute::Writable);
}

NumberPrototype::~NumberPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(NumberPrototype::to_string)
{
    Value number_value;

    auto this_value = vm.this_value(global_object);
    if (this_value.is_number()) {
        number_value = this_value;
    } else if (this_value.is_object() && this_value.as_object().is_number_object()) {
        number_value = static_cast<NumberObject&>(this_value.as_object()).value_of();
    } else {
        vm.throw_exception<TypeError>(global_object, ErrorType::NumberIncompatibleThis, "toString");
        return {};
    }

    int radix;
    auto argument = vm.argument(0);
    if (argument.is_undefined()) {
        radix = 10;
    } else {
        radix = argument.to_i32(global_object);
    }

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

    int int_part = floor(number);
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

        int precision = max_precision_for_radix[radix];

        for (int i = 0; i < precision; ++i) {
            decimal_part *= radix;
            int integral = floor(decimal_part);
            characters.append(digits[integral]);
            decimal_part -= integral;
        }

        while (characters.last() == '0')
            characters.take_last();
    }

    return js_string(vm, String(characters.data(), characters.size()));
}

}
