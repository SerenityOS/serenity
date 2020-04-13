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
#include <string.h>

namespace JS {

StringPrototype::StringPrototype()
    : StringObject(js_string(interpreter(), String::empty()))
{
    set_prototype(interpreter().object_prototype());
    put_native_property("length", length_getter, nullptr);
    put_native_function("charAt", char_at, 1);
    put_native_function("repeat", repeat, 1);
    put_native_function("startsWith", starts_with, 1);
    put_native_function("indexOf", index_of, 1);
    put_native_function("toLowerCase", to_lowercase, 0);
    put_native_function("toUpperCase", to_uppercase, 0);
    put_native_function("toString", to_string, 0);
    put_native_function("padStart", pad_start, 1);
    put_native_function("padEnd", pad_end, 1);

    put_native_function("trim", trim, 0);
    put_native_function("trimStart", trimStart, 0);
    put_native_function("trimEnd", trimEnd, 0);
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
        return js_string(interpreter, String::empty());
    return js_string(interpreter, underlying_string.substring(index, 1));
}

Value StringPrototype::repeat(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    ASSERT(this_object->is_string_object());
    if (!interpreter.argument_count())
        return js_string(interpreter, String::empty());
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
    return js_string(interpreter, builder.to_string());
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

Value StringPrototype::index_of(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    if (!this_object->is_string_object())
        return interpreter.throw_exception<TypeError>("Not a String object");

    Value needle_value = js_undefined();
    if (interpreter.argument_count() >= 1)
        needle_value = interpreter.argument(0);
    auto needle = needle_value.to_string();
    auto haystack = static_cast<const StringObject*>(this_object)->primitive_string()->string();

    // FIXME: We should have a helper in AK::String for this.
    auto* ptr = strstr(haystack.characters(), needle.characters());
    if (!ptr)
        return Value(-1);
    return Value((i32)(ptr - haystack.characters()));
}

static StringObject* string_object_from(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return nullptr;
    if (!this_object->is_string_object()) {
        interpreter.throw_exception<TypeError>("Not a String object");
        return nullptr;
    }
    return static_cast<StringObject*>(this_object);
}

Value StringPrototype::to_lowercase(Interpreter& interpreter)
{
    auto* string_object = string_object_from(interpreter);
    if (!string_object)
        return {};
    return js_string(interpreter, string_object->primitive_string()->string().to_lowercase());
}

Value StringPrototype::to_uppercase(Interpreter& interpreter)
{
    auto* string_object = string_object_from(interpreter);
    if (!string_object)
        return {};
    return js_string(interpreter, string_object->primitive_string()->string().to_uppercase());
}

Value StringPrototype::length_getter(Interpreter& interpreter)
{
    auto* string_object = string_object_from(interpreter);
    if (!string_object)
        return {};
    return Value((i32)string_object->primitive_string()->string().length());
}

Value StringPrototype::to_string(Interpreter& interpreter)
{
    auto* string_object = string_object_from(interpreter);
    if (!string_object)
        return {};
    return js_string(interpreter, string_object->primitive_string()->string());
}

enum class PadPlacement {
    Start,
    End,
};

static Value pad_string(Interpreter& interpreter, Object* object, PadPlacement placement)
{
    auto string = object->to_string().as_string()->string();
    if (interpreter.argument(0).to_number().is_nan()
        || interpreter.argument(0).to_number().is_undefined()
        || interpreter.argument(0).to_number().to_i32() < 0) {
        return js_string(interpreter, string);
    }
    auto max_length = static_cast<size_t>(interpreter.argument(0).to_i32());
    if (max_length <= string.length())
        return js_string(interpreter, string);

    String fill_string = " ";
    if (!interpreter.argument(1).is_undefined())
        fill_string = interpreter.argument(1).to_string();
    if (fill_string.is_empty())
        return js_string(interpreter, string);

    auto fill_length = max_length - string.length();

    StringBuilder filler_builder;
    while (filler_builder.length() < fill_length)
        filler_builder.append(fill_string);
    auto filler = filler_builder.build().substring(0, fill_length);

    if (placement == PadPlacement::Start)
        return js_string(interpreter, String::format("%s%s", filler.characters(), string.characters()));
    return js_string(interpreter, String::format("%s%s", string.characters(), filler.characters()));
}

Value StringPrototype::pad_start(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    return pad_string(interpreter, this_object, PadPlacement::Start);
}

Value StringPrototype::pad_end(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    return pad_string(interpreter, this_object, PadPlacement::End);
}


enum class TrimMode {
    Left,
    Right,
    Both
};

static Value trim_string(Interpreter& interpreter, Object* object, TrimMode mode)
{
    auto& string = object->to_string().as_string()->string();

    size_t substring_start = 0;
    size_t substring_length = string.length();

    if((mode == TrimMode::Left || mode == TrimMode::Both) && string.starts_with(' '))
    {
        for(size_t i = 0; i < string.length(); ++i)
        {
            if(string.characters()[i] != ' ')
            {   
                substring_start = i;
                substring_length -= substring_start;
                break;
            }
        }
    }

    if((mode == TrimMode::Right || mode == TrimMode::Both) && string.ends_with(' '))
    {
        size_t count = 0;
        for(size_t i = string.length() -1; i > 0; --i)
        {
            if(string.characters()[i] != ' ')
            {   
                substring_length -= count;
                break;
            }
            count++;
        }
    }

    auto substring = string.substring(substring_start, substring_length);
    return js_string(interpreter, substring);
}

Value StringPrototype::trim(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());

    return trim_string(interpreter, this_object, TrimMode::Both);
}

Value StringPrototype::trimStart(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());

    return trim_string(interpreter, this_object, TrimMode::Left);
}

Value StringPrototype::trimEnd(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());

    return trim_string(interpreter, this_object, TrimMode::Right);
}

}
