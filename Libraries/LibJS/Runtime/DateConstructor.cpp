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

DateConstructor::DateConstructor()
    : NativeFunction("Date", *interpreter().global_object().function_prototype())
{
    define_property("prototype", interpreter().global_object().date_prototype(), 0);
    define_property("length", Value(7), Attribute::Configurable);

    define_native_function("now", now, 0, Attribute::Writable | Attribute::Configurable);
}

DateConstructor::~DateConstructor()
{
}

Value DateConstructor::call(Interpreter& interpreter)
{
    auto date = construct(interpreter);
    if (!date.is_object())
        return {};
    return js_string(interpreter, static_cast<Date&>(date.as_object()).string());
}

Value DateConstructor::construct(Interpreter&)
{
    // TODO: Support args
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    auto datetime = Core::DateTime::now();
    auto milliseconds = static_cast<u16>(tv.tv_usec / 1000);
    return Date::create(global_object(), datetime, milliseconds);
}

JS_DEFINE_NATIVE_FUNCTION(DateConstructor::now)
{
    struct timeval tv;
    gettimeofday(&tv, nullptr);
    return Value(tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0);
}

}
