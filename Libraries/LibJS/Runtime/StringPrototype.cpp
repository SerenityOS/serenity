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

#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>
#include <string.h>

namespace JS {

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

static String string_from(Interpreter& interpreter)
{
    auto* this_object = interpreter.this_value().to_object(interpreter.heap());
    if (!this_object)
        return {};
    return Value(this_object).to_string();
}

StringPrototype::StringPrototype()
    : StringObject(*js_string(interpreter(), String::empty()), *interpreter().global_object().object_prototype())
{
    u8 attr = Attribute::Writable | Attribute::Configurable;

    put_native_property("length", length_getter, nullptr, 0);
    put_native_function("charAt", char_at, 1, attr);
    put_native_function("repeat", repeat, 1, attr);
    put_native_function("startsWith", starts_with, 1, attr);
    put_native_function("indexOf", index_of, 1, attr);
    put_native_function("toLowerCase", to_lowercase, 0, attr);
    put_native_function("toUpperCase", to_uppercase, 0, attr);
    put_native_function("toString", to_string, 0, attr);
    put_native_function("padStart", pad_start, 1, attr);
    put_native_function("padEnd", pad_end, 1, attr);
    put_native_function("trim", trim, 0, attr);
    put_native_function("trimStart", trim_start, 0, attr);
    put_native_function("trimEnd", trim_end, 0, attr);
    put_native_function("concat", concat, 1, attr);
    put_native_function("substring", substring, 2, attr);
    put_native_function("includes", includes, 1, attr);
}

StringPrototype::~StringPrototype()
{
}

Value StringPrototype::char_at(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    i32 index = 0;
    if (interpreter.argument_count())
        index = interpreter.argument(0).to_i32();
    if (index < 0 || index >= static_cast<i32>(string.length()))
        return js_string(interpreter, String::empty());
    return js_string(interpreter, string.substring(index, 1));
}

Value StringPrototype::repeat(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    if (!interpreter.argument_count())
        return js_string(interpreter, String::empty());
    auto count_value = interpreter.argument(0).to_number();
    if (count_value.as_double() < 0)
        return interpreter.throw_exception<RangeError>("repeat count must be a positive number");
    if (count_value.is_infinity())
        return interpreter.throw_exception<RangeError>("repeat count must be a finite number");
    auto count = count_value.to_i32();
    StringBuilder builder;
    for (i32 i = 0; i < count; ++i)
        builder.append(string);
    return js_string(interpreter, builder.to_string());
}

Value StringPrototype::starts_with(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
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
    auto string_length = static_cast<i32>(string.length());
    auto start = min(max(position, 0), string_length);
    if (start + search_string_length > string_length)
        return Value(false);
    if (search_string_length == 0)
        return Value(true);
    return Value(string.substring(start, search_string_length) == search_string);
}

Value StringPrototype::index_of(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    Value needle_value = js_undefined();
    if (interpreter.argument_count() >= 1)
        needle_value = interpreter.argument(0);
    auto needle = needle_value.to_string();
    return Value((i32)string.index_of(needle).value_or(-1));
}

Value StringPrototype::to_lowercase(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    return js_string(interpreter, string.to_lowercase());
}

Value StringPrototype::to_uppercase(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    return js_string(interpreter, string.to_uppercase());
}

Value StringPrototype::length_getter(Interpreter& interpreter)
{
    auto* string_object = string_object_from(interpreter);
    if (!string_object)
        return {};
    return Value((i32)string_object->primitive_string().string().length());
}

Value StringPrototype::to_string(Interpreter& interpreter)
{
    auto* string_object = string_object_from(interpreter);
    if (!string_object)
        return {};
    return js_string(interpreter, string_object->primitive_string().string());
}

enum class PadPlacement {
    Start,
    End,
};

static Value pad_string(Interpreter& interpreter, const String& string, PadPlacement placement)
{
    auto max_length_value = interpreter.argument(0).to_number();
    if (max_length_value.is_nan() || max_length_value.is_undefined() || max_length_value.as_double() < 0)
        return js_string(interpreter, string);

    auto max_length = static_cast<size_t>(max_length_value.to_i32());
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
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    return pad_string(interpreter, string, PadPlacement::Start);
}

Value StringPrototype::pad_end(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    return pad_string(interpreter, string, PadPlacement::End);
}

enum class TrimMode {
    Left,
    Right,
    Both
};

static Value trim_string(Interpreter& interpreter, const String& string, TrimMode mode)
{
    size_t substring_start = 0;
    size_t substring_length = string.length();

    auto is_white_space_character = [](char character) -> bool {
        return character == 0x9 || character == 0xa || character == 0xb || character == 0xc || character == 0xd || character == 0x20;
    };

    if (mode == TrimMode::Left || mode == TrimMode::Both) {
        for (size_t i = 0; i < string.length(); ++i) {
            if (!is_white_space_character(string[i])) {
                substring_start = i;
                substring_length -= substring_start;
                break;
            }
        }
    }

    if (substring_length == 0)
        return js_string(interpreter, "");

    if (mode == TrimMode::Right || mode == TrimMode::Both) {
        size_t count = 0;
        for (size_t i = string.length() - 1; i > 0; --i) {
            if (!is_white_space_character(string[i])) {
                substring_length -= count;
                break;
            }
            count++;
        }
    }

    return js_string(interpreter, string.substring(substring_start, substring_length));
}

Value StringPrototype::trim(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    return trim_string(interpreter, string, TrimMode::Both);
}

Value StringPrototype::trim_start(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    return trim_string(interpreter, string, TrimMode::Left);
}

Value StringPrototype::trim_end(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    return trim_string(interpreter, string, TrimMode::Right);
}

Value StringPrototype::concat(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    StringBuilder builder;
    builder.append(string);
    for (size_t i = 0; i < interpreter.argument_count(); ++i) {
        auto string_argument = interpreter.argument(i).to_string();
        builder.append(string_argument);
    }
    return js_string(interpreter, builder.to_string());
}

Value StringPrototype::substring(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    if (interpreter.argument_count() == 0)
        return js_string(interpreter, string);

    i32 string_length = static_cast<i32>(string.length());
    i32 index_start = interpreter.argument(0).to_number().to_i32();
    i32 index_end = string_length;

    if (index_start > string_length)
        index_start = string_length;
    else if (index_start < 0)
        index_start = 0;

    if (interpreter.argument_count() >= 2) {
        index_end = interpreter.argument(1).to_number().to_i32();

        if (index_end > string_length)
            index_end = string_length;
        else if (index_end < 0)
            index_end = 0;
    }

    if (index_start == index_end)
        return js_string(interpreter, String(""));

    if (index_start > index_end) {
        if (interpreter.argument_count() == 1) {
            return js_string(interpreter, String(""));
        } else {
            i32 temp_index_start = index_start;
            index_start = index_end;
            index_end = temp_index_start;
        }
    }

    auto part_length = index_end - index_start;
    auto string_part = string.substring(index_start, part_length);
    return js_string(interpreter, string_part);
}

Value StringPrototype::includes(Interpreter& interpreter)
{
    auto string = string_from(interpreter);
    if (string.is_null())
        return {};
    auto search_string = interpreter.argument(0).to_string();
    i32 position = 0;

    if (interpreter.argument_count() >= 2) {
        position = interpreter.argument(1).to_i32();

        if (position >= static_cast<i32>(string.length()))
            return Value(false);

        if (position < 0)
            position = 0;
    }

    if (position == 0)
        return Value(string.contains(search_string));

    auto substring_length = string.length() - position;
    auto substring_search = string.substring(position, substring_length);
    return Value(substring_search.contains(search_string));
}

}
