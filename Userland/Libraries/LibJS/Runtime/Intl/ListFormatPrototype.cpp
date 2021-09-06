/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/StringBuilder.h>
#include <AK/TypeCasts.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/ListFormat.h>
#include <LibJS/Runtime/Intl/ListFormatPrototype.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

static ListFormat* typed_this(GlobalObject& global_object)
{
    auto& vm = global_object.vm();

    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;

    if (!is<ListFormat>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Intl.ListFormat");
        return nullptr;
    }

    return static_cast<ListFormat*>(this_object);
}

using Placeables = HashMap<StringView, Variant<PatternPartition, Vector<PatternPartition>>>;

// 13.1.1 DeconstructPattern ( pattern, placeables ), https://tc39.es/ecma402/#sec-deconstructpattern
static Vector<PatternPartition> deconstruct_pattern(StringView pattern, Placeables placeables)
{
    // 1. Let patternParts be PartitionPattern(pattern).
    auto pattern_parts = partition_pattern(pattern);

    // 2. Let result be a new empty List.
    Vector<PatternPartition> result {};

    // 3. For each Record { [[Type]], [[Value]] } patternPart of patternParts, do
    for (auto const& pattern_part : pattern_parts) {
        // a. Let part be patternPart.[[Type]].
        auto part = pattern_part.type;

        // b. If part is "literal", then
        if (part == "literal"sv) {
            // i. Append Record { [[Type]]: "literal", [[Value]]: patternPart.[[Value]] } to result.
            result.append({ part, pattern_part.value });
        }
        // c. Else,
        else {
            // i. Assert: placeables has a field [[<part>]].
            // ii. Let subst be placeables.[[<part>]].
            auto subst = placeables.get(part);
            VERIFY(subst.has_value());

            subst.release_value().visit(
                // iii. If Type(subst) is List, then
                [&](Vector<PatternPartition>& partition) {
                    // 1. For each element s of subst, do
                    for (auto& element : partition) {
                        // a. Append s to result.
                        result.append(move(element));
                    }
                },
                // iv. Else,
                [&](PatternPartition& partition) {
                    // 1. Append subst to result.
                    result.append(move(partition));
                });
        }
    }

    // 4. Return result.
    return result;
}

// 13.1.2 CreatePartsFromList ( listFormat, list ), https://tc39.es/ecma402/#sec-createpartsfromlist
static Vector<PatternPartition> create_parts_from_list(ListFormat const& list_format, Vector<String> const& list)
{
    auto list_patterns = Unicode::get_locale_list_patterns(list_format.locale(), list_format.type_string(), list_format.style_string());
    if (!list_patterns.has_value())
        return {};

    // 1. Let size be the number of elements of list.
    auto size = list.size();

    // 2. If size is 0, then
    if (size == 0) {
        // a. Return a new empty List.
        return {};
    }

    // 3. If size is 2, then
    if (size == 2) {
        // a. Let n be an index into listFormat.[[Templates]] based on listFormat.[[Locale]], list[0], and list[1].
        // b. Let pattern be listFormat.[[Templates]][n].[[Pair]].
        auto pattern = list_patterns->pair;

        // c. Let first be a new Record { [[Type]]: "element", [[Value]]: list[0] }.
        PatternPartition first { "element"sv, list[0] };

        // d. Let second be a new Record { [[Type]]: "element", [[Value]]: list[1] }.
        PatternPartition second { "element"sv, list[1] };

        // e. Let placeables be a new Record { [[0]]: first, [[1]]: second }.
        Placeables placeables;
        placeables.set("0"sv, move(first));
        placeables.set("1"sv, move(second));

        // f. Return DeconstructPattern(pattern, placeables).
        return deconstruct_pattern(pattern, move(placeables));
    }

    // 4. Let last be a new Record { [[Type]]: "element", [[Value]]: list[size - 1] }.
    PatternPartition last { "element"sv, list[size - 1] };

    // 5. Let parts be « last ».
    Vector<PatternPartition> parts { move(last) };

    // The spec does not say to do this, but because size_t is unsigned, we need to take care not to wrap around 0.
    if (size == 1)
        return parts;

    // 6. Let i be size - 2.
    size_t i = size - 2;

    // 7. Repeat, while i ≥ 0,
    do {
        // a. Let head be a new Record { [[Type]]: "element", [[Value]]: list[i] }.
        PatternPartition head { "element"sv, list[i] };

        // b. Let n be an implementation-defined index into listFormat.[[Templates]] based on listFormat.[[Locale]], head, and parts.
        StringView pattern;

        // c. If i is 0, then
        if (i == 0) {
            // i. Let pattern be listFormat.[[Templates]][n].[[Start]].
            pattern = list_patterns->start;
        }
        // d. Else if i is less than size - 2, then
        else if (i < (size - 2)) {
            // i. Let pattern be listFormat.[[Templates]][n].[[Middle]].
            pattern = list_patterns->middle;
        }
        // e. Else,
        else {
            // i. Let pattern be listFormat.[[Templates]][n].[[End]].
            pattern = list_patterns->end;
        }

        // f. Let placeables be a new Record { [[0]]: head, [[1]]: parts }.
        Placeables placeables;
        placeables.set("0"sv, move(head));
        placeables.set("1"sv, move(parts));

        // g. Set parts to DeconstructPattern(pattern, placeables).
        parts = deconstruct_pattern(pattern, move(placeables));

        // h. Decrement i by 1.
    } while (i-- != 0);

    // 8. Return parts.
    return parts;
}

// 13.1.3 FormatList ( listFormat, list )
static String format_list(ListFormat const& list_format, Vector<String> const& list)
{
    // 1. Let parts be CreatePartsFromList(listFormat, list).
    auto parts = create_parts_from_list(list_format, list);

    // 2. Let result be an empty String.
    StringBuilder result;

    // 3. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto const& part : parts) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(part.value);
    }

    // 4. Return result.
    return result.build();
}

// 13.1.4 FormatListToParts ( listFormat, list ), https://tc39.es/ecma402/#sec-formatlisttoparts
static Array* format_list_to_parts(GlobalObject& global_object, ListFormat const& list_format, Vector<String> const& list)
{
    auto& vm = global_object.vm();

    // 1. Let parts be CreatePartsFromList(listFormat, list).
    auto parts = create_parts_from_list(list_format, list);

    // 2. Let result be ArrayCreate(0).
    auto result = Array::create(global_object, 0);

    // 3. Let n be 0.
    size_t n = 0;

    // 4. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto const& part : parts) {
        // a. Let O be OrdinaryObjectCreate(%Object.prototype%).
        auto* object = Object::create(global_object, global_object.object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(O, "type", part.[[Type]]).
        object->create_data_property_or_throw(vm.names.type, js_string(vm, part.type));

        // c. Perform ! CreateDataPropertyOrThrow(O, "value", part.[[Value]]).
        object->create_data_property_or_throw(vm.names.value, js_string(vm, part.value));

        // d. Perform ! CreateDataPropertyOrThrow(result, ! ToString(n), O).
        result->create_data_property_or_throw(n, object);

        // e. Increment n by 1.
        ++n;
    }

    // 5. Return result.
    return result;
}

// 13.1.5 StringListFromIterable ( iterable ), https://tc39.es/ecma402/#sec-createstringlistfromiterable
static Vector<String> string_list_from_iterable(GlobalObject& global_object, Value iterable)
{
    auto& vm = global_object.vm();

    // 1. If iterable is undefined, then
    if (iterable.is_undefined()) {
        // a. Return a new empty List.
        return {};
    }

    // 2. Let iteratorRecord be ? GetIterator(iterable).
    auto* iterator_record = get_iterator(global_object, iterable);
    if (vm.exception())
        return {};

    // 3. Let list be a new empty List.
    Vector<String> list;

    // 4. Let next be true.
    Object* next = nullptr;

    // 5. Repeat, while next is not false,
    do {
        // a. Set next to ? IteratorStep(iteratorRecord).
        next = iterator_step(global_object, *iterator_record);
        if (vm.exception())
            return {};

        // b. If next is not false, then
        if (next != nullptr) {
            // i. Let nextValue be ? IteratorValue(next).
            auto next_value = iterator_value(global_object, *next);
            if (vm.exception())
                return {};

            // ii. If Type(nextValue) is not String, then
            if (!next_value.is_string()) {
                // 1. Let error be ThrowCompletion(a newly created TypeError object).
                vm.throw_exception<TypeError>(global_object, ErrorType::NotAString, next_value);

                // 2. Return ? IteratorClose(iteratorRecord, error).
                iterator_close(*iterator_record);
                return {};
            }

            // iii. Append nextValue to the end of the List list.
            list.append(next_value.as_string().string());
        }
    } while (next != nullptr);

    // 6. Return list.
    return list;
}

// 13.4 Properties of the Intl.ListFormat Prototype Object, https://tc39.es/ecma402/#sec-properties-of-intl-listformat-prototype-object
ListFormatPrototype::ListFormatPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void ListFormatPrototype::initialize(GlobalObject& global_object)
{
    Object::initialize(global_object);

    auto& vm = this->vm();

    // 13.4.2 Intl.ListFormat.prototype [ @@toStringTag ], https://tc39.es/ecma402/#sec-Intl.ListFormat.prototype-toStringTag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "Intl.ListFormat"), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.format, format, 1, attr);
    define_native_function(vm.names.formatToParts, format_to_parts, 1, attr);
    define_native_function(vm.names.resolvedOptions, resolved_options, 0, attr);
}

// 13.4.3 Intl.ListFormat.prototype.format ( list ), https://tc39.es/ecma402/#sec-Intl.ListFormat.prototype.format
JS_DEFINE_NATIVE_FUNCTION(ListFormatPrototype::format)
{
    auto list = vm.argument(0);

    // 1. Let lf be the this value.
    // 2. Perform ? RequireInternalSlot(lf, [[InitializedListFormat]]).
    auto* list_format = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let stringList be ? StringListFromIterable(list).
    auto string_list = string_list_from_iterable(global_object, list);
    if (vm.exception())
        return {};

    // 4. Return FormatList(lf, stringList).
    auto formatted = format_list(*list_format, string_list);
    return js_string(vm, move(formatted));
}

// 13.4.4 Intl.ListFormat.prototype.formatToParts ( list ), https://tc39.es/ecma402/#sec-Intl.ListFormat.prototype.formatToParts
JS_DEFINE_NATIVE_FUNCTION(ListFormatPrototype::format_to_parts)
{
    auto list = vm.argument(0);

    // 1. Let lf be the this value.
    // 2. Perform ? RequireInternalSlot(lf, [[InitializedListFormat]]).
    auto* list_format = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let stringList be ? StringListFromIterable(list).
    auto string_list = string_list_from_iterable(global_object, list);
    if (vm.exception())
        return {};

    // 4. Return FormatListToParts(lf, stringList).
    return format_list_to_parts(global_object, *list_format, string_list);
}

// 3.4.5 Intl.ListFormat.prototype.resolvedOptions ( ), https://tc39.es/ecma402/#sec-Intl.ListFormat.prototype.resolvedoptions
JS_DEFINE_NATIVE_FUNCTION(ListFormatPrototype::resolved_options)
{
    // 1. Let lf be the this value.
    // 2. Perform ? RequireInternalSlot(lf, [[InitializedListFormat]]).
    auto* list_format = typed_this(global_object);
    if (vm.exception())
        return {};

    // 3. Let options be ! OrdinaryObjectCreate(%Object.prototype%).
    auto* options = Object::create(global_object, global_object.object_prototype());

    // 4. For each row of Table 9, except the header row, in table order, do
    //     a. Let p be the Property value of the current row.
    //     b. Let v be the value of lf's internal slot whose name is the Internal Slot value of the current row.
    //     c. Assert: v is not undefined.
    //     d. Perform ! CreateDataPropertyOrThrow(options, p, v).
    options->create_data_property_or_throw(vm.names.locale, js_string(vm, list_format->locale()));
    options->create_data_property_or_throw(vm.names.type, js_string(vm, list_format->type_string()));
    options->create_data_property_or_throw(vm.names.style, js_string(vm, list_format->style_string()));

    // 5. Return options.
    return options;
}

}
