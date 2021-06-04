/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/NumberObject.h>
#include <math.h>

#ifdef __clang__
#    define EPSILON_VALUE pow(2, -52)
#    define MAX_SAFE_INTEGER_VALUE pow(2, 53) - 1
#    define MIN_SAFE_INTEGER_VALUE -(pow(2, 53) - 1)
#else
constexpr const double EPSILON_VALUE { __builtin_pow(2, -52) };
constexpr const double MAX_SAFE_INTEGER_VALUE { __builtin_pow(2, 53) - 1 };
constexpr const double MIN_SAFE_INTEGER_VALUE { -(__builtin_pow(2, 53) - 1) };
#endif

namespace JS {

NumberConstructor::NumberConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Number, *global_object.function_prototype())
{
}

void NumberConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.isFinite, is_finite, 1, attr);
    define_native_function(vm.names.isInteger, is_integer, 1, attr);
    define_native_function(vm.names.isNaN, is_nan, 1, attr);
    define_native_function(vm.names.isSafeInteger, is_safe_integer, 1, attr);
    define_property(vm.names.parseFloat, global_object.get(vm.names.parseFloat));
    define_property(vm.names.prototype, global_object.number_prototype(), 0);
    define_property(vm.names.length, Value(1), Attribute::Configurable);
    define_property(vm.names.EPSILON, Value(EPSILON_VALUE), 0);
    define_property(vm.names.MAX_VALUE, Value(NumericLimits<double>::max()), 0);
    define_property(vm.names.MIN_VALUE, Value(NumericLimits<double>::min()), 0);
    define_property(vm.names.MAX_SAFE_INTEGER, Value(MAX_SAFE_INTEGER_VALUE), 0);
    define_property(vm.names.MIN_SAFE_INTEGER, Value(MIN_SAFE_INTEGER_VALUE), 0);
    define_property(vm.names.NEGATIVE_INFINITY, js_negative_infinity(), 0);
    define_property(vm.names.POSITIVE_INFINITY, js_infinity(), 0);
    define_property(vm.names.NaN, js_nan(), 0);
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
    return Value((int64_t)value == value && value >= MIN_SAFE_INTEGER_VALUE && value <= MAX_SAFE_INTEGER_VALUE);
}

}
