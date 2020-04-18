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

#include <AK/Function.h>
#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

static Date* this_date_from_interpreter(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return nullptr;
    if (!this_object->is_date()) {
        interpreter.throw_exception<TypeError>("object must be of type Date");
        return nullptr;
    }
    return static_cast<Date*>(this_object);
}

DatePrototype::DatePrototype()
    : Object(interpreter().global_object().object_prototype())
{
    put_native_function("getDate", get_date);
    put_native_function("getDay", get_day);
    put_native_function("getFullYear", get_full_year);
    put_native_function("getHours", get_hours);
    put_native_function("getMilliseconds", get_milliseconds);
    put_native_function("getMinutes", get_minutes);
    put_native_function("getMonth", get_month);
    put_native_function("getSeconds", get_seconds);
    put_native_function("getTime", get_time);
    put_native_function("toDateString", to_date_string);
    put_native_function("toTimeString", to_time_string);
    put_native_function("toString", to_string);
}

DatePrototype::~DatePrototype()
{
}

Value DatePrototype::get_date(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto date = this_object->datetime().day();
    return Value(static_cast<double>(date));
}

Value DatePrototype::get_day(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto day = this_object->datetime().weekday();
    return Value(static_cast<double>(day));
}

Value DatePrototype::get_full_year(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto full_year = this_object->datetime().year();
    return Value(static_cast<double>(full_year));
}

Value DatePrototype::get_hours(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto hours = this_object->datetime().hour();
    return Value(static_cast<double>(hours));
}

Value DatePrototype::get_milliseconds(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto milliseconds = this_object->milliseconds();
    return Value(static_cast<double>(milliseconds));
}

Value DatePrototype::get_minutes(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto minutes = this_object->datetime().minute();
    return Value(static_cast<double>(minutes));
}

Value DatePrototype::get_month(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto months = this_object->datetime().month() - 1;
    return Value(static_cast<double>(months));
}

Value DatePrototype::get_seconds(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto seconds = this_object->datetime().second();
    return Value(static_cast<double>(seconds));
}

Value DatePrototype::get_time(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto seconds = this_object->datetime().timestamp();
    auto milliseconds = this_object->milliseconds();
    return Value(static_cast<double>(seconds * 1000 + milliseconds));
}

Value DatePrototype::to_date_string(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto string = this_object->date_string();
    return js_string(interpreter, move(string));
}

Value DatePrototype::to_time_string(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto string = this_object->time_string();
    return js_string(interpreter, move(string));
}

Value DatePrototype::to_string(Interpreter& interpreter)
{
    auto* this_object = this_date_from_interpreter(interpreter);
    if (!this_object)
        return {};
    auto string = this_object->string();
    return js_string(interpreter, move(string));
}

}
