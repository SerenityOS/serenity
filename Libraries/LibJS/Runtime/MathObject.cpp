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

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/MathObject.h>
#include <math.h>

namespace JS {

MathObject::MathObject()
{
    put_native_function("abs", abs, 1);
    put_native_function("random", random);
    put_native_function("sqrt", sqrt, 1);
    put_native_function("floor", floor, 1);
    put_native_function("ceil", ceil, 1);
    put_native_function("round", round, 1);
    put_native_function("max", max, 2);
    put_native_function("trunc", trunc, 1);
    put_native_function("sin", sin, 1);
    put_native_function("cos", cos, 1);
    put_native_function("tan", tan, 1);

    put("E", Value(M_E));
    put("LN2", Value(M_LN2));
    put("LN10", Value(M_LN10));
    put("LOG2E", Value(log2(M_E)));
    put("LOG10E", Value(log10(M_E)));
    put("PI", Value(M_PI));
    put("SQRT1_2", Value(::sqrt(1.0 / 2.0)));
    put("SQRT2", Value(::sqrt(2)));
}

MathObject::~MathObject()
{
}

Value MathObject::abs(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
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
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();
    return Value(::sqrt(number.as_double()));
}

Value MathObject::floor(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();
    return Value(::floor(number.as_double()));
}

Value MathObject::ceil(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();
    return Value(::ceil(number.as_double()));
}

Value MathObject::round(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();
    // FIXME: Use ::round() instead of ::roundf().
    return Value(::roundf(number.as_double()));
}

Value MathObject::max(Interpreter& interpreter)
{
    if (!interpreter.argument_count()) {
        return Value(-js_infinity().as_double());
    } else if (interpreter.argument_count() == 1) {
        return interpreter.argument(0).to_number();
    } else {
        Value max = interpreter.argument(0).to_number();
        for (size_t i = 1; i < interpreter.argument_count(); ++i) {
            Value cur = interpreter.argument(i).to_number();
            max = Value(cur.as_double() > max.as_double() ? cur : max);
        }
        return max;
    }
}

Value MathObject::trunc(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();

    if (number.as_double() < 0)
        return MathObject::ceil(interpreter);
    return MathObject::floor(interpreter);
}

Value MathObject::sin(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();
    return Value(::sin(number.as_double()));
}

Value MathObject::cos(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();
    return Value(::cos(number.as_double()));
}

Value MathObject::tan(Interpreter& interpreter)
{
    auto number = interpreter.argument(0).to_number();
    if (number.is_nan())
        return js_nan();
    return Value(::tan(number.as_double()));
}

}
