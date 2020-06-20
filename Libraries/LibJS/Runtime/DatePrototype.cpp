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

static Date* typed_this(Interpreter& interpreter, GlobalObject& global_object)
{
    auto* this_object = interpreter.this_value(global_object).to_object(interpreter);
    if (!this_object)
        return nullptr;
    if (!this_object->is_date()) {
        interpreter.throw_exception<TypeError>(ErrorType::NotA, "Date");
        return nullptr;
    }
    return static_cast<Date*>(this_object);
}

DatePrototype::DatePrototype(GlobalObject& global_object)
    : Object(global_object.object_prototype())
{
}

void DatePrototype::initialize(Interpreter&, GlobalObject&)
{
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function("getDate", get_date, 0, attr);
    define_native_function("getDay", get_day, 0, attr);
    define_native_function("getFullYear", get_full_year, 0, attr);
    define_native_function("getHours", get_hours, 0, attr);
    define_native_function("getMilliseconds", get_milliseconds, 0, attr);
    define_native_function("getMinutes", get_minutes, 0, attr);
    define_native_function("getMonth", get_month, 0, attr);
    define_native_function("getSeconds", get_seconds, 0, attr);
    define_native_function("getTime", get_time, 0, attr);
    define_native_function("toDateString", to_date_string, 0, attr);
    define_native_function("toTimeString", to_time_string, 0, attr);
    define_native_function("toString", to_string, 0, attr);
}

DatePrototype::~DatePrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_date)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto date = this_object->datetime().day();
    return Value(static_cast<double>(date));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_day)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto day = this_object->datetime().weekday();
    return Value(static_cast<double>(day));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_full_year)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto full_year = this_object->datetime().year();
    return Value(static_cast<double>(full_year));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_hours)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto hours = this_object->datetime().hour();
    return Value(static_cast<double>(hours));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_milliseconds)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto milliseconds = this_object->milliseconds();
    return Value(static_cast<double>(milliseconds));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_minutes)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto minutes = this_object->datetime().minute();
    return Value(static_cast<double>(minutes));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_month)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto months = this_object->datetime().month() - 1;
    return Value(static_cast<double>(months));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_seconds)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto seconds = this_object->datetime().second();
    return Value(static_cast<double>(seconds));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_time)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto seconds = this_object->datetime().timestamp();
    auto milliseconds = this_object->milliseconds();
    return Value(static_cast<double>(seconds * 1000 + milliseconds));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_date_string)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto string = this_object->date_string();
    return js_string(interpreter, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_time_string)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto string = this_object->time_string();
    return js_string(interpreter, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_string)
{
    auto* this_object = typed_this(interpreter, global_object);
    if (!this_object)
        return {};
    auto string = this_object->string();
    return js_string(interpreter, move(string));
}

}
