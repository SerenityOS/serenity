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

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

StringPrototype::StringPrototype()
{
    put_native_property("length", length_getter, nullptr);
    put_native_function("charAt", char_at);
    put_native_function("repeat", repeat);
    put_native_function("startsWith", starts_with);
}

StringPrototype::~StringPrototype()
{
}

Value StringPrototype::char_at(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    i32 index = 0;
    if (interpreter.argument_count())
        index = interpreter.argument(0).to_i32();
    ASSERT(this_object->is_string_object());
    auto underlying_string = static_cast<const StringObject*>(this_object)->primitive_string()->string();
    if (index < 0 || index >= static_cast<i32>(underlying_string.length()))
        return js_string(interpreter.heap(), String::empty());
    return js_string(interpreter.heap(), underlying_string.substring(index, 1));
}

Value StringPrototype::repeat(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    ASSERT(this_object->is_string_object());
    if (!interpreter.argument_count())
        return js_string(interpreter.heap(), String::empty());
    i32 count = 0;
    count = interpreter.argument(0).to_i32();
    if (count < 0) {
        // FIXME: throw RangeError
        return {};
    }
    auto* string_object = static_cast<StringObject*>(this_object);
    StringBuilder builder;
    for (i32 i = 0; i < count; ++i)
        builder.append(string_object->primitive_string()->string());
    return js_string(interpreter.heap(), builder.to_string());
}

Value StringPrototype::starts_with(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!interpreter.argument_count())
        return Value(false);
    auto search_string = interpreter.argument(0).to_string();
    auto search_string_length = static_cast<i32>(search_string.length());
    i32 position = 0;
    if (interpreter.argument_count() > 1) {
        auto number = interpreter.argument(1).to_number();
        if (!number.is_nan())
            position = number.to_i32();
    }
    ASSERT(this_object->is_string_object());
    auto underlying_string = static_cast<const StringObject*>(this_object)->primitive_string()->string();
    auto underlying_string_length = static_cast<i32>(underlying_string.length());
    auto start = min(max(position, 0), underlying_string_length);
    if (start + search_string_length > underlying_string_length)
        return Value(false);
    if (search_string_length == 0)
        return Value(true);
    return Value(underlying_string.substring(start, search_string_length) == search_string);
}

Value StringPrototype::length_getter(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_string_object())
        return interpreter.throw_exception<Error>("TypeError", "Not a String object");
    return Value((i32) static_cast<const StringObject*>(this_object)->primitive_string()->string().length());
}

}
