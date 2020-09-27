/*
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

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/NumberObject.h>
#include <math.h>

#define EPSILON pow(2, -52)
#define MAX_SAFE_INTEGER pow(2, 53) - 1
#define MIN_SAFE_INTEGER -(pow(2, 53) - 1)

namespace JS {

NumberConstructor::NumberConstructor(GlobalObject& global_object)
    : NativeFunction("Number", *global_object.function_prototype())
{
}

void NumberConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("isFinite", is_finite, 1, attr);
    define_native_function("isInteger", is_integer, 1, attr);
    define_native_function("isNaN", is_nan, 1, attr);
    define_native_function("isSafeInteger", is_safe_integer, 1, attr);
    define_property("parseFloat", global_object.get("parseFloat"));
    define_property("prototype", global_object.number_prototype(), 0);
    define_property("length", Value(1), Attribute::Configurable);
    define_property("EPSILON", Value(EPSILON), 0);
    define_property("MAX_SAFE_INTEGER", Value(MAX_SAFE_INTEGER), 0);
    define_property("MIN_SAFE_INTEGER", Value(MIN_SAFE_INTEGER), 0);
    define_property("NEGATIVE_INFINITY", js_negative_infinity(), 0);
    define_property("POSITIVE_INFINITY", js_infinity(), 0);
    define_property("NaN", js_nan(), 0);
}

NumberConstructor::~NumberConstructor()
{
}

Value NumberConstructor::call()
{
    if (!vm().argument_count())
        return Value(0);
    return vm().argument(0).to_number(global_object());
}

Value NumberConstructor::construct(Function&)
{
    double number = 0;
    if (vm().argument_count()) {
        number = vm().argument(0).to_double(global_object());
        if (vm().exception())
            return {};
    }
    return NumberObject::create(global_object(), number);
}

JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_finite)
{
    return Value(vm.argument(0).is_finite_number());
}

JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_integer)
{
    return Value(vm.argument(0).is_integer());
}

JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_nan)
{
    return Value(vm.argument(0).is_nan());
}

JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_safe_integer)
{
    if (!vm.argument(0).is_number())
        return Value(false);
    auto value = vm.argument(0).as_double();
    return Value((int64_t)value == value && value >= MIN_SAFE_INTEGER && value <= MAX_SAFE_INTEGER);
}

}
