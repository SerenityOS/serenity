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
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/MathObject.h>
#include <math.h>

namespace JS {

MathObject::MathObject()
    : Object(interpreter().global_object().object_prototype())
{
    u8 attr = Attribute::Writable | Attribute::Configurable;
    put_native_function("abs", abs, 1, attr);
    put_native_function("random", random, 0, attr);
    put_native_function("sqrt", sqrt, 1, attr);
    put_native_function("floor", floor, 1, attr);
    put_native_function("ceil", ceil, 1, attr);
    put_native_function("round", round, 1, attr);
    put_native_function("max", max, 2, attr);
    put_native_function("min", min, 2, attr);
    put_native_function("trunc", trunc, 1, attr);
    put_native_function("sin", sin, 1, attr);
    put_native_function("cos", cos, 1, attr);
    put_native_function("tan", tan, 1, attr);
    put_native_function("pow", pow, 2, attr);
    put_native_function("exp", exp, 1, attr);
    put_native_function("expm1", expm1, 1, attr);
    put_native_function("sign", sign, 1, attr);
    put_native_function("clz32", clz32, 1, attr);

    put("E", Value(M_E), 0);
    put("LN2", Value(M_LN2), 0);
    put("LN10", Value(M_LN10), 0);
    put("LOG2E", Value(log2(M_E)), 0);
    put("LOG10E", Value(log10(M_E)), 0);
    put("PI", Value(M_PI), 0);
    put("SQRT1_2", Value(::sqrt(1.0 / 2.0)), 0);
    put("SQRT2", Value(::sqrt(2)), 0);
}

MathObject::~MathObject()
{
}

Value MathObject::abs(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(number.as_double() >= 0 ? number.as_double() : -number.as_double());
}

Value MathObject::random(Interpreter&)
{
#ifdef __serenity__
    double r = (double)arc4random() / (double)UINT32_MAX;
#else
    double r = (double)rand() / (double)RAND_MAX;
#endif
    return Value(r);
}

Value MathObject::sqrt(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::sqrt(number.as_double()));
}

Value MathObject::floor(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::floor(number.as_double()));
}

Value MathObject::ceil(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::ceil(number.as_double()));
}

Value MathObject::round(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::round(number.as_double()));
}

Value MathObject::max(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return js_negative_infinity();

    auto max = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    for (size_t i = 1; i < interpreter.argument_count(); ++i) {
        auto cur = interpreter.argument(i).to_number(interpreter);
        if (interpreter.exception())
            return {};
        max = Value(cur.as_double() > max.as_double() ? cur : max);
    }
    return max;
}

Value MathObject::min(Interpreter& interpreter)
{
    if (!interpreter.argument_count())
        return js_infinity();

    auto min = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    for (size_t i = 1; i < interpreter.argument_count(); ++i) {
        auto cur = interpreter.argument(i).to_number(interpreter);
        if (interpreter.exception())
            return {};
        min = Value(cur.as_double() < min.as_double() ? cur : min);
    }
    return min;
}

Value MathObject::trunc(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    if (number.as_double() < 0)
        return MathObject::ceil(interpreter);
    return MathObject::floor(interpreter);
}

Value MathObject::sin(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::sin(number.as_double()));
}

Value MathObject::cos(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::cos(number.as_double()));
}

Value MathObject::tan(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::tan(number.as_double()));
}

Value MathObject::pow(Interpreter& interpreter)
{
    return JS::exp(interpreter, interpreter.argument(0), interpreter.argument(1));
}

Value MathObject::exp(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::pow(M_E, number.as_double()));
}

Value MathObject::expm1(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (number.is_nan())
        return js_nan();
    return Value(::pow(M_E, number.as_double()) - 1);
}

Value MathObject::sign(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
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

Value MathObject::clz32(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number(interpreter);
    if (interpreter.exception())
        return {};
    if (!number.is_finite_number() || (unsigned)number.as_double() == 0)
        return Value(32);
    return Value(__builtin_clz((unsigned)number.as_double()));
}

}
