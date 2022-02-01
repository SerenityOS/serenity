/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/ListFormat.h>
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS::Intl {

// 13 ListFormat Objects, https://tc39.es/ecma402/#listformat-objects
ListFormat::ListFormat(Object& prototype)
    : Object(prototype)
{
}

void ListFormat::set_type(StringView type)
{
    if (type == "conjunction"sv) {
        m_type = Type::Conjunction;
    } else if (type == "disjunction"sv) {
        m_type = Type::Disjunction;
    } else if (type == "unit"sv) {
        m_type = Type::Unit;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView ListFormat::type_string() const
{
    switch (m_type) {
    case Type::Conjunction:
        return "conjunction"sv;
    case Type::Disjunction:
        return "disjunction"sv;
    case Type::Unit:
        return "unit"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 13.1.1 DeconstructPattern ( pattern, placeables ), https://tc39.es/ecma402/#sec-deconstructpattern
Vector<PatternPartition> deconstruct_pattern(StringView pattern, Placeables placeables)
{
    // 1. Let patternParts be PartitionPattern(pattern).
    auto pattern_parts = partition_pattern(pattern);

    // 2. Let result be a new empty List.
    Vector<PatternPartition> result {};

    // 3. For each Record { [[Type]], [[Value]] } patternPart of patternParts, do
    for (auto& pattern_part : pattern_parts) {
        // a. Let part be patternPart.[[Type]].
        auto part = pattern_part.type;

        // b. If part is "literal", then
        if (part == "literal"sv) {
            // i. Append Record { [[Type]]: "literal", [[Value]]: patternPart.[[Value]] } to result.
            result.append({ part, move(pattern_part.value) });
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
Vector<PatternPartition> create_parts_from_list(ListFormat const& list_format, Vector<String> const& list)
{
    auto list_patterns = Unicode::get_locale_list_patterns(list_format.locale(), list_format.type_string(), list_format.style());
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

// 13.1.3 FormatList ( listFormat, list ), https://tc39.es/ecma402/#sec-formatlist
String format_list(ListFormat const& list_format, Vector<String> const& list)
{
    // 1. Let parts be CreatePartsFromList(listFormat, list).
    auto parts = create_parts_from_list(list_format, list);

    // 2. Let result be an empty String.
    StringBuilder result;

    // 3. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto& part : parts) {
        // a. Set result to the string-concatenation of result and part.[[Value]].
        result.append(move(part.value));
    }

    // 4. Return result.
    return result.build();
}

// 13.1.4 FormatListToParts ( listFormat, list ), https://tc39.es/ecma402/#sec-formatlisttoparts
Array* format_list_to_parts(GlobalObject& global_object, ListFormat const& list_format, Vector<String> const& list)
{
    auto& vm = global_object.vm();

    // 1. Let parts be CreatePartsFromList(listFormat, list).
    auto parts = create_parts_from_list(list_format, list);

    // 2. Let result be ArrayCreate(0).
    auto* result = MUST(Array::create(global_object, 0));

    // 3. Let n be 0.
    size_t n = 0;

    // 4. For each Record { [[Type]], [[Value]] } part in parts, do
    for (auto& part : parts) {
        // a. Let O be OrdinaryObjectCreate(%Object.prototype%).
        auto* object = Object::create(global_object, global_object.object_prototype());

        // b. Perform ! CreateDataPropertyOrThrow(O, "type", part.[[Type]]).
        MUST(object->create_data_property_or_throw(vm.names.type, js_string(vm, part.type)));

        // c. Perform ! CreateDataPropertyOrThrow(O, "value", part.[[Value]]).
        MUST(object->create_data_property_or_throw(vm.names.value, js_string(vm, move(part.value))));

        // d. Perform ! CreateDataPropertyOrThrow(result, ! ToString(n), O).
        MUST(result->create_data_property_or_throw(n, object));

        // e. Increment n by 1.
        ++n;
    }

    // 5. Return result.
    return result;
}

// 13.1.5 StringListFromIterable ( iterable ), https://tc39.es/ecma402/#sec-createstringlistfromiterable
ThrowCompletionOr<Vector<String>> string_list_from_iterable(GlobalObject& global_object, Value iterable)
{
    auto& vm = global_object.vm();

    // 1. If iterable is undefined, then
    if (iterable.is_undefined()) {
        // a. Return a new empty List.
        return Vector<String> {};
    }

    // 2. Let iteratorRecord be ? GetIterator(iterable).
    auto iterator_record = TRY(get_iterator(global_object, iterable));

    // 3. Let list be a new empty List.
    Vector<String> list;

    // 4. Let next be true.
    Object* next = nullptr;

    // 5. Repeat, while next is not false,
    do {
        // a. Set next to ? IteratorStep(iteratorRecord).
        next = TRY(iterator_step(global_object, iterator_record));

        // b. If next is not false, then
        if (next != nullptr) {
            // i. Let nextValue be ? IteratorValue(next).
            auto next_value = TRY(iterator_value(global_object, *next));

            // ii. If Type(nextValue) is not String, then
            if (!next_value.is_string()) {
                // 1. Let error be ThrowCompletion(a newly created TypeError object).
                auto error = vm.throw_completion<TypeError>(global_object, ErrorType::NotAString, next_value);

                // 2. Return ? IteratorClose(iteratorRecord, error).
                return iterator_close(global_object, iterator_record, move(error));
            }

            // iii. Append nextValue to the end of the List list.
            list.append(next_value.as_string().string());
        }
    } while (next != nullptr);

    // 6. Return list.
    return list;
}

}
