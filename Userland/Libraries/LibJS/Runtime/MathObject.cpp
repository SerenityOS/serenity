/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Random.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MathObject.h>
#include <math.h>

namespace JS {

MathObject::MathObject(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void MathObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.abs, abs, 1, attr);
    define_native_function(vm.names.random, random, 0, attr);
    define_native_function(vm.names.sqrt, sqrt, 1, attr);
    define_native_function(vm.names.floor, floor, 1, attr);
    define_native_function(vm.names.ceil, ceil, 1, attr);
    define_native_function(vm.names.round, round, 1, attr);
    define_native_function(vm.names.max, max, 2, attr);
    define_native_function(vm.names.min, min, 2, attr);
    define_native_function(vm.names.trunc, trunc, 1, attr);
    define_native_function(vm.names.sin, sin, 1, attr);
    define_native_function(vm.names.cos, cos, 1, attr);
    define_native_function(vm.names.tan, tan, 1, attr);
    define_native_function(vm.names.pow, pow, 2, attr);
    define_native_function(vm.names.exp, exp, 1, attr);
    define_native_function(vm.names.expm1, expm1, 1, attr);
    define_native_function(vm.names.sign, sign, 1, attr);
    define_native_function(vm.names.clz32, clz32, 1, attr);
    define_native_function(vm.names.acos, acos, 1, attr);
    define_native_function(vm.names.acosh, acosh, 1, attr);
    define_native_function(vm.names.asin, asin, 1, attr);
    define_native_function(vm.names.asinh, asinh, 1, attr);
    define_native_function(vm.names.atan, atan, 1, attr);
    define_native_function(vm.names.atanh, atanh, 1, attr);
    define_native_function(vm.names.log1p, log1p, 1, attr);
    define_native_function(vm.names.cbrt, cbrt, 1, attr);
    define_native_function(vm.names.atan2, atan2, 2, attr);
    define_native_function(vm.names.fround, fround, 1, attr);
    define_native_function(vm.names.hypot, hypot, 2, attr);
    define_native_function(vm.names.imul, imul, 2, attr);
    define_native_function(vm.names.log, log, 1, attr);
    define_native_function(vm.names.log2, log2, 1, attr);
    define_native_function(vm.names.log10, log10, 1, attr);
    define_native_function(vm.names.sinh, sinh, 1, attr);
    define_native_function(vm.names.cosh, cosh, 1, attr);
    define_native_function(vm.names.tanh, tanh, 1, attr);

    define_property(vm.names.E, Value(M_E), 0);
    define_property(vm.names.LN2, Value(M_LN2), 0);
    define_property(vm.names.LN10, Value(M_LN10), 0);
    define_property(vm.names.LOG2E, Value(::log2(M_E)), 0);
    define_property(vm.names.LOG10E, Value(::log10(M_E)), 0);
    define_property(vm.names.PI, Value(M_PI), 0);
    define_property(vm.names.SQRT1_2, Value(M_SQRT1_2), 0);
    define_property(vm.names.SQRT2, Value(M_SQRT2), 0);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), "Math"), Attribute::Configurable);
}

MathObject::~MathObject()
{
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::abs)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    if (number.is_negative_zero())
        return Value(0);
    if (number.is_negative_infinity())
        return js_infinity();
    return Value(number.as_double() < 0 ? -number.as_double() : number.as_double());
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::random)
{
#ifdef __serenity__
    double r = (double)get_random<u32>() / (double)UINT32_MAX;
#else
    double r = (double)rand() / (double)RAND_MAX;
#endif
    return Value(r);
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::sqrt)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::sqrt(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::floor)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::floor(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::ceil)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    auto number_double = number.as_double();
    if (number_double < 0 && number_double > -1)
        return Value(-0.f);
    return Value(::ceil(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::round)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    double intpart = 0;
    double frac = modf(number.as_double(), &intpart);
    if (intpart >= 0) {
        if (frac >= 0.5)
            intpart += 1.0;
    } else {
        if (frac < -0.5)
            intpart -= 1.0;
    }
    return Value(intpart);
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::max)
{
    Vector<Value> coerced;
    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto number = vm.argument(i).to_number(global_object);
        if (vm.exception())
            return {};
        coerced.append(number);
    }

    auto highest = js_negative_infinity();
    for (auto& number : coerced) {
        if (number.is_nan())
            return js_nan();
        if ((number.is_positive_zero() && highest.is_negative_zero()) || number.as_double() > highest.as_double())
            highest = number;
    }
    return highest;
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::min)
{
    Vector<Value> coerced;
    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto number = vm.argument(i).to_number(global_object);
        if (vm.exception())
            return {};
        coerced.append(number);
    }

    auto lowest = js_infinity();
    for (auto& number : coerced) {
        if (number.is_nan())
            return js_nan();
        if ((number.is_negative_zero() && lowest.is_positive_zero()) || number.as_double() < lowest.as_double())
            lowest = number;
    }
    return lowest;
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::trunc)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    if (number.as_double() < 0)
        return MathObject::ceil(vm, global_object);
    return MathObject::floor(vm, global_object);
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::sin)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::sin(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::cos)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::cos(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::tan)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::tan(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::pow)
{
    auto base = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    auto exponent = vm.argument(1).to_number(global_object);
    if (vm.exception())
        return {};
    if (exponent.is_nan())
        return js_nan();
    if (exponent.is_positive_zero() || exponent.is_negative_zero())
        return Value(1);
    if (base.is_nan())
        return js_nan();
    if (base.is_positive_infinity())
        return exponent.as_double() > 0 ? js_infinity() : Value(0);
    if (base.is_negative_infinity()) {
        auto is_odd_integral_number = exponent.is_integer() && (exponent.as_i32() % 2 != 0);
        if (exponent.as_double() > 0)
            return is_odd_integral_number ? js_negative_infinity() : js_infinity();
        else
            return is_odd_integral_number ? Value(-0.0) : Value(0);
    }
    if (base.is_positive_zero())
        return exponent.as_double() > 0 ? Value(0) : js_infinity();
    if (base.is_negative_zero()) {
        auto is_odd_integral_number = exponent.is_integer() && (exponent.as_i32() % 2 != 0);
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
    if (base.as_double() < 0 && !exponent.is_integer())
        return js_nan();
    return Value(::pow(base.as_double(), exponent.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::exp)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::exp(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::expm1)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::expm1(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::sign)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_positive_zero())
        return Value(0);
    if (number.is_negative_zero())
        return Value(-0.0);
    if (number.as_double() > 0)
        return Value(1);
    if (number.as_double() < 0)
        return Value(-1);
    return js_nan();
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::clz32)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (!number.is_finite_number() || (unsigned)number.as_double() == 0)
        return Value(32);
    return Value(__builtin_clz((unsigned)number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::acos)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan() || number.as_double() > 1 || number.as_double() < -1)
        return js_nan();
    if (number.as_double() == 1)
        return Value(0);
    return Value(::acos(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::acosh)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.as_double() < 1)
        return js_nan();
    return Value(::acosh(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::asin)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan() || number.is_positive_zero() || number.is_negative_zero())
        return number;
    return Value(::asin(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::asinh)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    return Value(::asinh(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::atan)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan() || number.is_positive_zero() || number.is_negative_zero())
        return number;
    if (number.is_positive_infinity())
        return Value(M_PI_2);
    if (number.is_negative_infinity())
        return Value(-M_PI_2);
    return Value(::atan(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::atanh)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.as_double() > 1 || number.as_double() < -1)
        return js_nan();
    return Value(::atanh(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::log1p)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.as_double() < -1)
        return js_nan();
    return Value(::log1p(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::cbrt)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    return Value(::cbrt(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::atan2)
{
    auto constexpr three_quarters_pi = M_PI_4 + M_PI_2;

    auto y = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    auto x = vm.argument(1).to_number(global_object);
    if (vm.exception())
        return {};

    if (y.is_nan() || x.is_nan())
        return js_nan();
    if (y.is_positive_infinity()) {
        if (x.is_positive_infinity())
            return Value(M_PI_4);
        else if (x.is_negative_infinity())
            return Value(three_quarters_pi);
        else
            return Value(M_PI_2);
    }
    if (y.is_negative_infinity()) {
        if (x.is_positive_infinity())
            return Value(-M_PI_4);
        else if (x.is_negative_infinity())
            return Value(-three_quarters_pi);
        else
            return Value(-M_PI_2);
    }
    if (y.is_positive_zero()) {
        if (x.as_double() > 0 || x.is_positive_zero())
            return Value(0.0);
        else
            return Value(M_PI);
    }
    if (y.is_negative_zero()) {
        if (x.as_double() > 0 || x.is_positive_zero())
            return Value(-0.0);
        else
            return Value(-M_PI);
    }
    VERIFY(y.is_finite_number() && !y.is_positive_zero() && !y.is_negative_zero());
    if (y.as_double() > 0) {
        if (x.is_positive_infinity())
            return Value(0);
        else if (x.is_negative_infinity())
            return Value(M_PI);
        else if (x.is_positive_zero() || x.is_negative_zero())
            return Value(M_PI_2);
    }
    if (y.as_double() < 0) {
        if (x.is_positive_infinity())
            return Value(-0.0);
        else if (x.is_negative_infinity())
            return Value(-M_PI);
        else if (x.is_positive_zero() || x.is_negative_zero())
            return Value(-M_PI_2);
    }
    VERIFY(x.is_finite_number() && !x.is_positive_zero() && !x.is_negative_zero());
    return Value(::atan2(y.as_double(), x.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::fround)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value((float)number.as_double());
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::hypot)
{
    Vector<Value> coerced;
    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto number = vm.argument(i).to_number(global_object);
        if (vm.exception())
            return {};
        coerced.append(number);
    }

    for (auto& number : coerced) {
        if (number.is_positive_infinity() || number.is_negative_infinity())
            return js_infinity();
    }

    auto only_zero = true;
    double sum_of_squares = 0;
    for (auto& number : coerced) {
        if (number.is_nan() || number.is_positive_infinity())
            return number;
        if (number.is_negative_infinity())
            return js_infinity();
        if (!number.is_positive_zero() && !number.is_negative_zero())
            only_zero = false;
        sum_of_squares += number.as_double() * number.as_double();
    }
    if (only_zero)
        return Value(0);
    return Value(::sqrt(sum_of_squares));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::imul)
{
    auto a = vm.argument(0).to_u32(global_object);
    if (vm.exception())
        return {};
    auto b = vm.argument(1).to_u32(global_object);
    if (vm.exception())
        return {};
    return Value(static_cast<i32>(a * b));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::log)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.as_double() < 0)
        return js_nan();
    return Value(::log(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::log2)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.as_double() < 0)
        return js_nan();
    return Value(::log2(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::log10)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.as_double() < 0)
        return js_nan();
    return Value(::log10(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::sinh)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::sinh(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::cosh)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::cosh(number.as_double()));
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::tanh)
{
    auto number = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    if (number.is_positive_infinity())
        return Value(1);
    if (number.is_negative_infinity())
        return Value(-1);
    return Value(::tanh(number.as_double()));
}

}
