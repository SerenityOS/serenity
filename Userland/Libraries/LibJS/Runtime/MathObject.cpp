/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/FlyString.h>
#include <AK/Function.h>
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
    return Value(number.as_double() >= 0 ? number.as_double() : -number.as_double());
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::random)
{
#ifdef __serenity__
    double r = (double)arc4random() / (double)UINT32_MAX;
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
    if (!vm.argument_count())
        return js_negative_infinity();

    auto max = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    for (size_t i = 1; i < vm.argument_count(); ++i) {
        auto cur = vm.argument(i).to_number(global_object);
        if (vm.exception())
            return {};
        max = Value(cur.as_double() > max.as_double() ? cur : max);
    }
    return max;
}

JS_DEFINE_NATIVE_FUNCTION(MathObject::min)
{
    if (!vm.argument_count())
        return js_infinity();

    auto min = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    for (size_t i = 1; i < vm.argument_count(); ++i) {
        auto cur = vm.argument(i).to_number(global_object);
        if (vm.exception())
            return {};
        min = Value(cur.as_double() < min.as_double() ? cur : min);
    }
    return min;
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
    return JS::exp(global_object, vm.argument(0), vm.argument(1));
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
    auto y = vm.argument(0).to_number(global_object), x = vm.argument(1).to_number(global_object);
    auto pi_4 = M_PI_2 / 2;
    auto three_pi_4 = pi_4 + M_PI_2;
    if (vm.exception())
        return {};
    if (x.is_positive_zero()) {
        if (y.is_positive_zero() || y.is_negative_zero())
            return y;
        else
            return (y.as_double() > 0) ? Value(M_PI_2) : Value(-M_PI_2);
    }
    if (x.is_negative_zero()) {
        if (y.is_positive_zero())
            return Value(M_PI);
        else if (y.is_negative_zero())
            return Value(-M_PI);
        else
            return (y.as_double() > 0) ? Value(M_PI_2) : Value(-M_PI_2);
    }
    if (x.is_positive_infinity()) {
        if (y.is_infinity())
            return (y.is_positive_infinity()) ? Value(pi_4) : Value(-pi_4);
        else
            return (y.as_double() > 0) ? Value(+0.0) : Value(-0.0);
    }
    if (x.is_negative_infinity()) {
        if (y.is_infinity())
            return (y.is_positive_infinity()) ? Value(three_pi_4) : Value(-three_pi_4);
        else
            return (y.as_double() > 0) ? Value(M_PI) : Value(-M_PI);
    }
    if (y.is_infinity())
        return (y.is_positive_infinity()) ? Value(M_PI_2) : Value(-M_PI_2);
    if (y.is_positive_zero())
        return (x.as_double() > 0) ? Value(+0.0) : Value(M_PI);
    if (y.is_negative_zero())
        return (x.as_double() > 0) ? Value(-0.0) : Value(-M_PI);

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
    if (!vm.argument_count())
        return Value(0);

    auto hypot = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    hypot = Value(hypot.as_double() * hypot.as_double());
    for (size_t i = 1; i < vm.argument_count(); ++i) {
        auto cur = vm.argument(i).to_number(global_object);
        if (vm.exception())
            return {};
        hypot = Value(hypot.as_double() + cur.as_double() * cur.as_double());
    }
    return Value(::sqrt(hypot.as_double()));
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
