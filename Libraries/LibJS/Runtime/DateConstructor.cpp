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

#include <LibCore/DateTime.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DateConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <sys/time.h>
#include <time.h>

namespace JS {

DateConstructor::DateConstructor(GlobalObject& global_object)
    : NativeFunction("Date", *global_object.function_prototype())
{
}

void DateConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);
    define_property("prototype", global_object.date_prototype(), 0);
    define_property("length", Value(7), Attribute::Configurable);

    define_native_function("now", now, 0, Attribute::Writable | Attribute::Configurable);
}

DateConstructor::~DateConstructor()
{
}

Value DateConstructor::call(Interpreter& interpreter)
{
    auto date = construct(interpreter, *this);
    if (!date.is_object())
        return {};
    return js_string(interpreter, static_cast<Date&>(date.as_object()).string());
}

Value DateConstructor::construct(Interpreter& interpreter, Function&)
{
    if (interpreter.argument_count() == 0) {
        struct timeval tv;
        gettimeofday(&tv, nullptr);
        auto datetime = Core::DateTime::now();
        auto milliseconds = static_cast<u16>(tv.tv_usec / 1000);
        return Date::create(global_object(), datetime, milliseconds);
    }
    if (interpreter.argument_count() == 1 && interpreter.argument(0).is_string()) {
        // FIXME: Parse simplified ISO8601-like string, like Date.parse() will do.
        struct timeval tv;
        gettimeofday(&tv, nullptr);
        auto datetime = Core::DateTime::now();
        auto milliseconds = static_cast<u16>(tv.tv_usec / 1000);
        return Date::create(global_object(), datetime, milliseconds);
    }
    if (interpreter.argument_count() == 1) {
        // A timestamp since the epoch, in UTC.
        // FIXME: Date() probably should use a double as internal representation, so that NaN arguments and larger offsets are handled correctly.
        // FIXME: DateTime::from_timestamp() seems to not support negative offsets.
        double value = interpreter.argument(0).to_double(interpreter);
        auto datetime = Core::DateTime::from_timestamp(static_cast<time_t>(value / 1000));
        auto milliseconds = static_cast<u16>(fmod(value, 1000));
        return Date::create(global_object(), datetime, milliseconds);
    }
    // A date/time in components, in local time.
    // FIXME: This doesn't construct an "Invalid Date" object if one of the parameters is NaN.
    // FIXME: This doesn't range-check args and convert months > 12 to year increments etc.
    auto arg_or = [&interpreter](size_t i, i32 fallback) { return interpreter.argument_count() > i ? interpreter.argument(i).to_i32(interpreter) : fallback; };
    int year = interpreter.argument(0).to_i32(interpreter);
    int month_index = interpreter.argument(1).to_i32(interpreter);
    int day = arg_or(2, 1);
    int hours = arg_or(3, 0);
    int minutes = arg_or(4, 0);
    int seconds = arg_or(5, 0);
    int milliseconds = arg_or(6, 0);

    if (year >= 0 && year <= 99)
      year += 1900;
    int month = month_index + 1;
    auto datetime = Core::DateTime::create(year, month, day, hours, minutes, seconds);
    return Date::create(global_object(), datetime, milliseconds);
}

JS_DEFINE_NATIVE_FUNCTION(DateConstructor::now)
{
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    return Value(tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0);
}

}
