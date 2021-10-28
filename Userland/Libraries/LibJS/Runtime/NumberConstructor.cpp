/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Math.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/NumberConstructor.h>
#include <LibJS/Runtime/NumberObject.h>

#ifdef __clang__
#    define EPSILON_VALUE AK::exp2(-52.)
#    define MAX_SAFE_INTEGER_VALUE AK::exp2(53.) - 1
#    define MIN_SAFE_INTEGER_VALUE -(AK::exp2(53.) - 1)
#else
constexpr const double EPSILON_VALUE { __builtin_exp2(-52) };
constexpr const double MAX_SAFE_INTEGER_VALUE { __builtin_exp2(53) - 1 };
constexpr const double MIN_SAFE_INTEGER_VALUE { -(__builtin_exp2(53) - 1) };
#endif

namespace JS {

NumberConstructor::NumberConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Number.as_string(), *global_object.function_prototype())
{
}

void NumberConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 21.1.2.15 Number.prototype, https://tc39.es/ecma262/#sec-number.prototype
    define_direct_property(vm.names.prototype, global_object.number_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.isFinite, is_finite, 1, attr);
    define_native_function(vm.names.isInteger, is_integer, 1, attr);
    define_native_function(vm.names.isNaN, is_nan, 1, attr);
    define_native_function(vm.names.isSafeInteger, is_safe_integer, 1, attr);
    define_direct_property(vm.names.parseInt, global_object.get_without_side_effects(vm.names.parseInt), attr);
    define_direct_property(vm.names.parseFloat, global_object.get_without_side_effects(vm.names.parseFloat), attr);
    define_direct_property(vm.names.EPSILON, Value(EPSILON_VALUE), 0);
    define_direct_property(vm.names.MAX_VALUE, Value(NumericLimits<double>::max()), 0);
    define_direct_property(vm.names.MIN_VALUE, Value(NumericLimits<double>::min()), 0);
    define_direct_property(vm.names.MAX_SAFE_INTEGER, Value(MAX_SAFE_INTEGER_VALUE), 0);
    define_direct_property(vm.names.MIN_SAFE_INTEGER, Value(MIN_SAFE_INTEGER_VALUE), 0);
    define_direct_property(vm.names.NEGATIVE_INFINITY, js_negative_infinity(), 0);
    define_direct_property(vm.names.POSITIVE_INFINITY, js_infinity(), 0);
    define_direct_property(vm.names.NaN, js_nan(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

NumberConstructor::~NumberConstructor()
{
}

// Most of 21.1.1.1 Number ( value ) factored into a separate function for sharing between call() and construct().
static ThrowCompletionOr<Value> get_value_from_constructor_argument(GlobalObject& global_object)
{
    auto& vm = global_object.vm();

    Value number;
    if (vm.argument_count() > 0) {
        auto primitive = TRY(vm.argument(0).to_numeric(global_object));
        if (primitive.is_bigint()) {
            // FIXME: How should huge values be handled here?
            auto& big_integer = primitive.as_bigint().big_integer();
            number = Value(static_cast<double>(big_integer.unsigned_value().to_u64()) * (big_integer.is_negative() ? -1.0 : 1.0));
        } else {
            number = primitive;
        }
    } else {
        number = Value(0);
    }
    return number;
}

// 21.1.1.1 Number ( value ), https://tc39.es/ecma262/#sec-number-constructor-number-value
ThrowCompletionOr<Value> NumberConstructor::call()
{
    return get_value_from_constructor_argument(global_object());
}

// 21.1.1.1 Number ( value ), https://tc39.es/ecma262/#sec-number-constructor-number-value
ThrowCompletionOr<Object*> NumberConstructor::construct(FunctionObject& new_target)
{
    auto& global_object = this->global_object();

    auto number = TRY(get_value_from_constructor_argument(global_object));
    return TRY(ordinary_create_from_constructor<NumberObject>(global_object, new_target, &GlobalObject::number_prototype, number.as_double()));
}

// 21.1.2.2 Number.isFinite ( number ), https://tc39.es/ecma262/#sec-number.isfinite
JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_finite)
{
    return Value(vm.argument(0).is_finite_number());
}

// 21.1.2.3 Number.isInteger ( number ), https://tc39.es/ecma262/#sec-number.isinteger
JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_integer)
{
    return Value(vm.argument(0).is_integral_number());
}

// 21.1.2.4 Number.isNaN ( number ), https://tc39.es/ecma262/#sec-number.isnan
JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_nan)
{
    return Value(vm.argument(0).is_nan());
}

// 21.1.2.5 Number.isSafeInteger ( number ), https://tc39.es/ecma262/#sec-number.issafeinteger
JS_DEFINE_NATIVE_FUNCTION(NumberConstructor::is_safe_integer)
{
    if (!vm.argument(0).is_number())
        return Value(false);
    if (!vm.argument(0).is_integral_number())
        return Value(false);
    auto value = vm.argument(0).as_double();
    return Value(value >= MIN_SAFE_INTEGER_VALUE && value <= MAX_SAFE_INTEGER_VALUE);
}

}
