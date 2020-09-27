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
#include <LibJS/Runtime/Date.h>
#include <LibJS/Runtime/DatePrototype.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

static Date* typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!this_object->is_date()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Date");
        return nullptr;
    }
    return static_cast<Date*>(this_object);
}

DatePrototype::DatePrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void DatePrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);
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
    define_native_function("getUTCDate", get_utc_date, 0, attr);
    define_native_function("getUTCDay", get_utc_day, 0, attr);
    define_native_function("getUTCFullYear", get_utc_full_year, 0, attr);
    define_native_function("getUTCHours", get_utc_hours, 0, attr);
    define_native_function("getUTCMilliseconds", get_utc_milliseconds, 0, attr);
    define_native_function("getUTCMinutes", get_utc_minutes, 0, attr);
    define_native_function("getUTCMonth", get_utc_month, 0, attr);
    define_native_function("getUTCSeconds", get_utc_seconds, 0, attr);
    define_native_function("toDateString", to_date_string, 0, attr);
    define_native_function("toISOString", to_iso_string, 0, attr);
    define_native_function("toLocaleDateString", to_locale_date_string, 0, attr);
    define_native_function("toLocaleString", to_locale_string, 0, attr);
    define_native_function("toLocaleTimeString", to_locale_time_string, 0, attr);
    define_native_function("toTimeString", to_time_string, 0, attr);
    define_native_function("toString", to_string, 0, attr);

    // Aliases.
    define_native_function("valueOf", get_time, 0, attr);
    // toJSON() isn't quite an alias for toISOString():
    // - it returns null instead of throwing RangeError
    // - its .length is 1, not 0
    // - it can be transferred to other prototypes
}

DatePrototype::~DatePrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_date)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->date()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_day)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->day()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_full_year)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->full_year()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_hours)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->hours()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_milliseconds)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->milliseconds()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_minutes)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->minutes()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_month)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->month()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_seconds)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->seconds()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_time)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(this_object->time());
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_date)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_date()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_day)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_day()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_full_year)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_full_year()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_hours)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_hours()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_milliseconds)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_milliseconds()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_month)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_month()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_minutes)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_minutes()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::get_utc_seconds)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    return Value(static_cast<double>(this_object->utc_seconds()));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_date_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    auto string = this_object->date_string();
    return js_string(vm, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_iso_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    auto string = this_object->iso_date_string();
    return js_string(vm, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_locale_date_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    // FIXME: Optional locales, options params.
    auto string = this_object->locale_date_string();
    return js_string(vm, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_locale_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    // FIXME: Optional locales, options params.
    auto string = this_object->locale_string();
    return js_string(vm, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_locale_time_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    // FIXME: Optional locales, options params.
    auto string = this_object->locale_time_string();
    return js_string(vm, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_time_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    auto string = this_object->time_string();
    return js_string(vm, move(string));
}

JS_DEFINE_NATIVE_FUNCTION(DatePrototype::to_string)
{
    auto* this_object = typed_this(vm, global_object);
    if (!this_object)
        return {};
    auto string = this_object->string();
    return js_string(vm, move(string));
}

}
