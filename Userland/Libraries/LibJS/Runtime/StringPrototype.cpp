/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <mail@linusgroh.de>
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

#include <AK/Checked.h>
#include <AK/Function.h>
#include <AK/StringBuilder.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/RegExpObject.h>
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
    if (!is<StringObject>(this_object)) {
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

static Optional<size_t> split_match(const String& haystack, size_t start, const String& needle)
{
    auto r = needle.length();
    auto s = haystack.length();
    if (start + r > s)
        return {};
    if (!haystack.substring_view(start).starts_with(needle))
        return {};
    return start + r;
}

StringPrototype::StringPrototype(GlobalObject& global_object)
    : StringObject(*js_string(global_object.heap(), String::empty()), *global_object.object_prototype())
{
}

void StringPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    StringObject::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_property(vm.names.length, length_getter, nullptr, 0);
    define_native_function(vm.names.charAt, char_at, 1, attr);
    define_native_function(vm.names.charCodeAt, char_code_at, 1, attr);
    define_native_function(vm.names.repeat, repeat, 1, attr);
    define_native_function(vm.names.startsWith, starts_with, 1, attr);
    define_native_function(vm.names.endsWith, ends_with, 1, attr);
    define_native_function(vm.names.indexOf, index_of, 1, attr);
    define_native_function(vm.names.toLowerCase, to_lowercase, 0, attr);
    define_native_function(vm.names.toUpperCase, to_uppercase, 0, attr);
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.padStart, pad_start, 1, attr);
    define_native_function(vm.names.padEnd, pad_end, 1, attr);
    define_native_function(vm.names.trim, trim, 0, attr);
    define_native_function(vm.names.trimStart, trim_start, 0, attr);
    define_native_function(vm.names.trimEnd, trim_end, 0, attr);
    define_native_function(vm.names.concat, concat, 1, attr);
    define_native_function(vm.names.substr, substr, 2, attr);
    define_native_function(vm.names.substring, substring, 2, attr);
    define_native_function(vm.names.includes, includes, 1, attr);
    define_native_function(vm.names.slice, slice, 2, attr);
    define_native_function(vm.names.split, split, 2, attr);
    define_native_function(vm.names.lastIndexOf, last_index_of, 1, attr);
    define_native_function(vm.names.at, at, 1, attr);
    define_native_function(vm.names.match, match, 1, attr);
    define_native_function(vm.well_known_symbol_iterator(), symbol_iterator, 0, attr);
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
    auto count = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    if (count < 0) {
        vm.throw_exception<RangeError>(global_object, ErrorType::StringRepeatCountMustBe, "positive");
        return {};
    }
    if (Value(count).is_infinity()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::StringRepeatCountMustBe, "finite");
        return {};
    }
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
    if (!vm.argument(1).is_undefined()) {
        auto position = vm.argument(1).to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
        start = clamp(position, static_cast<double>(0), static_cast<double>(string_length));
    }
    if (start + search_string_length > string_length)
        return Value(false);
    if (search_string_length == 0)
        return Value(true);
    return Value(string.substring(start, search_string_length) == search_string);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::ends_with)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};

    auto search_string_value = vm.argument(0);

    bool search_is_regexp = search_string_value.is_regexp(global_object);
    if (vm.exception())
        return {};
    if (search_is_regexp) {
        vm.throw_exception<TypeError>(global_object, ErrorType::IsNotA, "searchString", "string, but a regular expression");
        return {};
    }

    auto search_string = search_string_value.to_string(global_object);
    if (vm.exception())
        return {};

    auto string_length = string.length();
    auto search_string_length = search_string.length();

    size_t pos = string_length;

    auto end_position_value = vm.argument(1);
    if (!end_position_value.is_undefined()) {
        double pos_as_double = end_position_value.to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
        pos = clamp(pos_as_double, static_cast<double>(0), static_cast<double>(string_length));
    }

    if (search_string_length == 0)
        return Value(true);
    if (pos < search_string_length)
        return Value(false);

    auto start = pos - search_string_length;
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
    auto max_length = vm.argument(0).to_length(global_object);
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
    auto start = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    auto end = (double)string_length;
    if (!vm.argument(1).is_undefined()) {
        end = vm.argument(1).to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
    }
    size_t index_start = clamp(start, static_cast<double>(0), static_cast<double>(string_length));
    size_t index_end = clamp(end, static_cast<double>(0), static_cast<double>(string_length));

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

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::substr)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    if (vm.argument_count() == 0)
        return js_string(vm, string);

    // FIXME: this should index a UTF-16 code_point view of the string.
    auto string_length = (i32)string.length();

    auto start_argument = vm.argument(0).to_i32(global_object);
    if (vm.exception())
        return {};

    auto start = start_argument < 0 ? (string_length - -start_argument) : start_argument;

    auto length = string_length - start;
    if (vm.argument_count() >= 2) {
        auto length_argument = vm.argument(1).to_i32(global_object);
        if (vm.exception())
            return {};
        length = max(0, min(length_argument, length));
        if (vm.exception())
            return {};
    }

    if (length == 0)
        return js_string(vm, String(""));

    auto string_part = string.substring(start, length);
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
    auto string_length = string.length();
    // FIXME: start should index a UTF-16 code_point view of the string.
    size_t start = 0;
    if (!vm.argument(1).is_undefined()) {
        auto position = vm.argument(1).to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
        start = clamp(position, static_cast<double>(0), static_cast<double>(string_length));
    }
    if (start == 0)
        return Value(string.contains(search_string));
    auto substring_length = string_length - start;
    auto substring_search = string.substring(start, substring_length);
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

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::split)
{
    // FIXME Implement the @@split part

    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};

    auto* result = Array::create(global_object);
    size_t result_len = 0;

    auto limit = static_cast<u32>(MAX_U32);
    if (!vm.argument(1).is_undefined()) {
        limit = vm.argument(1).to_u32(global_object);
        if (vm.exception())
            return {};
    }

    auto separator = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    if (limit == 0)
        return result;

    if (vm.argument(0).is_undefined()) {
        result->define_property(0, js_string(vm, string));
        return result;
    }

    auto len = string.length();
    auto separator_len = separator.length();
    if (len == 0) {
        if (separator_len > 0)
            result->define_property(0, js_string(vm, string));
        return result;
    }

    size_t start = 0;
    auto pos = start;
    if (separator_len == 0) {
        for (pos = 0; pos < len; pos++)
            result->define_property(pos, js_string(vm, string.substring(pos, 1)));
        return result;
    }

    while (pos != len) {
        auto e = split_match(string, pos, separator);
        if (!e.has_value()) {
            pos += 1;
            continue;
        }

        auto segment = string.substring_view(start, pos - start);
        result->define_property(result_len, js_string(vm, segment));
        result_len++;
        if (result_len == limit)
            return result;
        start = e.value();
        pos = start;
    }

    auto rest = string.substring(start, len - start);
    result->define_property(result_len, js_string(vm, rest));

    return result;
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::last_index_of)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    auto search_string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto position = vm.argument(1).to_number(global_object);
    if (vm.exception())
        return {};
    if (search_string.length() > string.length())
        return Value(-1);
    auto max_index = string.length() - search_string.length();
    auto from_index = max_index;
    if (!position.is_nan()) {
        // FIXME: from_index should index a UTF-16 code_point view of the string.
        auto p = position.to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};
        from_index = clamp(p, static_cast<double>(0), static_cast<double>(max_index));
    }

    for (i32 i = from_index; i >= 0; --i) {
        auto part_view = string.substring_view(i, search_string.length());
        if (part_view == search_string)
            return Value(i);
    }

    return Value(-1);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::at)
{
    auto string = ak_string_from(vm, global_object);
    if (string.is_null())
        return {};
    auto length = string.length();
    auto relative_index = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    if (Value(relative_index).is_infinity())
        return js_undefined();
    Checked<size_t> index { 0 };
    if (relative_index >= 0) {
        index += relative_index;
    } else {
        index += length;
        index -= -relative_index;
    }
    if (index.has_overflow() || index.value() >= length)
        return js_undefined();
    return js_string(vm, String::formatted("{}", string[index.value()]));
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::symbol_iterator)
{
    auto this_object = vm.this_value(global_object);
    if (this_object.is_nullish()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ToObjectNullOrUndefined);
        return {};
    }

    auto string = this_object.to_string(global_object);
    if (vm.exception())
        return {};
    return StringIterator::create(global_object, string);
}

JS_DEFINE_NATIVE_FUNCTION(StringPrototype::match)
{
    // https://tc39.es/ecma262/#sec-string.prototype.match
    auto this_object = vm.this_value(global_object);
    if (this_object.is_nullish()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::ToObjectNullOrUndefined);
        return {};
    }
    auto regexp = vm.argument(0);
    if (!regexp.is_nullish()) {
        if (auto* matcher = get_method(global_object, regexp, vm.well_known_symbol_match()))
            return vm.call(*matcher, regexp, this_object);
    }
    auto s = this_object.to_string(global_object);
    if (vm.exception())
        return {};
    auto rx = regexp_create(global_object, regexp, js_undefined());
    if (!rx)
        return {};
    return rx->invoke(vm.well_known_symbol_match(), js_string(vm, s));
}

}
