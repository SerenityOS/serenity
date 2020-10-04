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
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringIterator.h>
#include <LibJS/Runtime/StringObject.h>
#include <LibJS/Runtime/StringPrototype.h>
#include <LibJS/Runtime/Value.h>
#include <string.h>

namespace JS {

static StringObject* typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!this_object->is_string_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "String");
        return nullptr;
    }
    return static_cast<StringObject*>(this_object);
}

static String ak_string_from(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    return Value(this_object).to_string(global_object);
}

StringPrototype::StringPrototype(GlobalObject& global_object)
    : StringObject(*js_string(global_object.heap(), String::empty()), *global_object.object_prototype())
{
}

void StringPrototype::initialize(GlobalObject& global_object)
{
    StringObject::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_property("length", length_getter, nullptr, 0);
    define_native_function("charAt", char_at, 1, attr);
    define_native_function("charCodeAt", char_code_at, 1, attr);
    define_native_function("repeat", repeat, 1, attr);
    define_native_function("startsWith", starts_with, 1, attr);
    define_native_function("indexOf", index_of, 1, attr);
    define_native_function("toLowerCase", to_lowercase, 0, attr);
    define_native_function("toUpperCase", to_uppercase, 0, attr);
    define_native_function("toString", to_string, 0, attr);
    define_native_function("padStart", pad_start, 1, attr);
    define_native_function("padEnd", pad_end, 1, attr);
    define_native_function("trim", trim, 0, attr);
    define_native_function("trimStart", trim_start, 0, attr);
    define_native_function("trimEnd", trim_end, 0, attr);
    define_native_function("concat", concat, 1, attr);
    define_native_function("substring", substring, 2, attr);
    define_native_function("includes", includes, 1, attr);
    define_native_function("slice", slice, 2, attr);
    define_native_function("lastIndexOf", last_index_of, 1, attr);
    define_native_function(global_object.vm().well_known_symbol_iterator(), symbol_iterator, 0, attr);
}

StringPrototype::~StringPrototype()
{
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::char_at)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    i32 index = 0;
    if (vm.argument_count()) {
        index = vm.argument(0).to_i32(global_object);
        if (vm.exception())
            return {};
    }
    if (index < 0 || index >= static_cast<i32>(string.length()))
        return js_string(vm, String::empty());
    // FIXME: This should return a character corresponding to the i'th UTF-16 code point.
    return js_string(vm, string.substring(index, 1));
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::char_code_at)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    i32 index = 0;
    if (vm.argument_count()) {
        index = vm.argument(0).to_i32(global_object);
        if (vm.exception())
            return {};
    }
    if (index < 0 || index >= static_cast<i32>(string.length()))
        return js_nan();
    // FIXME: This should return the i'th UTF-16 code point.
    return Value((i32)string[index]);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::repeat)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    if (!vm.argument_count())
        return js_string(vm, String::empty());
    auto count_value = vm.argument(0).to_number(global_object);
    if (vm.exception())
        return {};
    if (count_value.as_double() < 0) {
        vm.throw_exception<RangeError>(global_object, ErrorType::StringRepeatCountMustBe, "positive");
        return {};
    }
    if (count_value.is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::StringRepeatCountMustBe, "finite");
        return {};
    }
    auto count = count_value.to_size_t(global_object);
    if (vm.exception())
        return {};
    StringBuilder builder;
    for (size_t i = 0; i < count; ++i)
        builder.append(string);
    return js_string(vm, builder.to_string());
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::starts_with)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    if (!vm.argument_count())
        return Value(false);
    auto search_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto string_length = string.length();
    auto search_string_length = search_string.length();
    size_t start = 0;
    if (vm.argument_count() > 1) {
        auto number = vm.argument(1).to_number(global_object);
        if (vm.exception())
            return {};
        if (!number.is_nan())
            start = min(number.to_size_t(global_object), string_length);
    }
    if (start + search_string_length > string_length)
        return Value(false);
    if (search_string_length == 0)
        return Value(true);
    return Value(string.substring(start, search_string_length) == search_string);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::index_of)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    auto needle = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    return Value((i32)string.index_of(needle).value_or(-1));
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::to_lowercase)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    return js_string(vm, string.to_lowercase());
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::to_uppercase)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    return js_string(vm, string.to_uppercase());
}

JS_DEFINE_NATIVE_GETTER(StringPrototype::length_getter)
{
    auto* string_object = typed_this(vm, global_object);
    if (!string_object)
        return {};
    return Value((i32)string_object->primitive_string().string().length());
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::to_string)
{
    auto* string_object = typed_this(vm, global_object);
    if (!string_object)
        return {};
    return js_string(vm, string_object->primitive_string().string());
}

enum class PadPlacement {
    Start,
    End,
};

static Value pad_string(GlobalObject& global_object, const String& string, PadPlacement placement)
{
    auto& vm = global_object.vm();
    auto max_length = vm.argument(0).to_size_t(global_object);
    if (vm.exception())
        return {};
    if (max_length <= string.length())
        return js_string(vm, string);

    String fill_string = " ";
    if (!vm.argument(1).is_undefined()) {
        fill_string = vm.argument(1).to_string(global_object);
        if (vm.exception())
            return {};
        if (fill_string.is_empty())
            return js_string(vm, string);
    }

    auto fill_length = max_length - string.length();

    StringBuilder filler_builder;
    while (filler_builder.length() < fill_length)
        filler_builder.append(fill_string);
    auto filler = filler_builder.build().substring(0, fill_length);

    auto formatted = placement == PadPlacement::Start
        ? String::formatted("{}{}", filler, string)
        : String::formatted("{}{}", string, filler);
    return js_string(vm, formatted);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::pad_start)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    return pad_string(global_object, string, PadPlacement::Start);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::pad_end)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    return pad_string(global_object, string, PadPlacement::End);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::trim)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    return js_string(vm, string.trim_whitespace(TrimMode::Both));
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::trim_start)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    return js_string(vm, string.trim_whitespace(TrimMode::Left));
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::trim_end)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    return js_string(vm, string.trim_whitespace(TrimMode::Right));
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::concat)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    StringBuilder builder;
    builder.append(string);
    for (size_t i = 0; i < vm.argument_count(); ++i) {
        auto string_argument = vm.argument(i).to_string(global_object);
        if (vm.exception())
            return {};
        builder.append(string_argument);
    }
    return js_string(vm, builder.to_string());
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::substring)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    if (vm.argument_count() == 0)
        return js_string(vm, string);

    // FIXME: index_start and index_end should index a UTF-16 code_point view of the string.
    auto string_length = string.length();
    auto index_start = min(vm.argument(0).to_size_t(global_object), string_length);
    if (vm.exception())
        return {};
    auto index_end = string_length;
    if (vm.argument_count() >= 2) {
        index_end = min(vm.argument(1).to_size_t(global_object), string_length);
        if (vm.exception())
            return {};
    }

    if (index_start == index_end)
        return js_string(vm, String(""));

    if (index_start > index_end) {
        if (vm.argument_count() == 1)
            return js_string(vm, String(""));
        auto temp_index_start = index_start;
        index_start = index_end;
        index_end = temp_index_start;
    }

    auto part_length = index_end - index_start;
    auto string_part = string.substring(index_start, part_length);
    return js_string(vm, string_part);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::includes)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    auto search_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    // FIXME: position should index a UTF-16 code_point view of the string.
    size_t position = 0;
    if (vm.argument_count() >= 2) {
        position = vm.argument(1).to_size_t(global_object);
        if (vm.exception())
            return {};
        if (position >= string.length())
            return Value(false);
    }

    if (position == 0)
        return Value(string.contains(search_string));

    auto substring_length = string.length() - position;
    auto substring_search = string.substring(position, substring_length);
    return Value(substring_search.contains(search_string));
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::slice)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};

    if (vm.argument_count() == 0)
        return js_string(vm, string);

    // FIXME: index_start and index_end should index a UTF-16 code_point view of the string.
    auto string_length = static_cast<i32>(string.length());
    auto index_start = vm.argument(0).to_i32(global_object);
    if (vm.exception())
        return {};
    auto index_end = string_length;

    auto negative_min_index = -(string_length - 1);
    if (index_start < negative_min_index)
        index_start = 0;
    else if (index_start < 0)
        index_start = string_length + index_start;

    if (vm.argument_count() >= 2) {
        index_end = vm.argument(1).to_i32(global_object);
        if (vm.exception())
            return {};

        if (index_end < negative_min_index)
            return js_string(vm, String::empty());

        if (index_end > string_length)
            index_end = string_length;
        else if (index_end < 0)
            index_end = string_length + index_end;
    }

    if (index_start >= index_end)
        return js_string(vm, String::empty());

    auto part_length = index_end - index_start;
    auto string_part = string.substring(index_start, part_length);
    return js_string(vm, string_part);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::last_index_of)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};

    if (vm.argument_count() == 0)
        return Value(-1);

    auto search_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    if (search_string.length() > string.length())
        return Value(-1);
    auto max_index = string.length() - search_string.length();
    auto from_index = max_index;
    if (vm.argument_count() >= 2) {
        // FIXME: from_index should index a UTF-16 code_point view of the string.
        from_index = min(vm.argument(1).to_size_t(global_object), max_index);
        if (vm.exception())
            return {};
    }

    for (i32 i = from_index; i >= 0; --i) {
        auto part_view = string.substring_view(i, search_string.length());
        if (part_view == search_string)
            return Value(i);
    }

    return Value(-1);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::symbol_iterator)
{
    auto this_object = vm.this_value(global_object);
    if (this_object.is_nullish()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ToObjectNullOrUndef);
        return {};
    }

    auto string = this_object.to_string(global_object);
    if (vm.exception())
        return {};
    return StringIterator::create(global_object, string);
}

}
