/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Function.h>
#include <AK/Utf16View.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/RegExpPrototype.h>
#include <LibJS/Runtime/RegExpStringIterator.h>
#include <LibJS/Runtime/StringPrototype.h>

namespace JS {

RegExpPrototype::RegExpPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void RegExpPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.toString, to_string, 0, attr);
    define_native_function(vm.names.test, test, 1, attr);
    define_native_function(vm.names.exec, exec, 1, attr);
    define_native_function(vm.names.compile, compile, 2, attr);

    define_native_function(*vm.well_known_symbol_match(), symbol_match, 1, attr);
    define_native_function(*vm.well_known_symbol_match_all(), symbol_match_all, 1, attr);
    define_native_function(*vm.well_known_symbol_replace(), symbol_replace, 2, attr);
    define_native_function(*vm.well_known_symbol_search(), symbol_search, 1, attr);
    define_native_function(*vm.well_known_symbol_split(), symbol_split, 2, attr);

    define_native_accessor(vm.names.flags, flags, {}, Attribute::Configurable);
    define_native_accessor(vm.names.source, source, {}, Attribute::Configurable);

#define __JS_ENUMERATE(flagName, flag_name, flag_char) \
    define_native_accessor(vm.names.flagName, flag_name, {}, Attribute::Configurable);
    JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE
}

RegExpPrototype::~RegExpPrototype()
{
}

// Non-standard abstraction around steps used by multiple prototypes.
static ThrowCompletionOr<void> increment_last_index(GlobalObject& global_object, Object& regexp_object, Utf16View const& string, bool unicode)
{
    auto& vm = global_object.vm();

    // Let thisIndex be ℝ(? ToLength(? Get(rx, "lastIndex"))).
    auto last_index_value = TRY(regexp_object.get(vm.names.lastIndex));
    auto last_index = TRY(last_index_value.to_length(global_object));

    // Let nextIndex be AdvanceStringIndex(S, thisIndex, fullUnicode).
    last_index = advance_string_index(string, last_index, unicode);

    // Perform ? Set(rx, "lastIndex", 𝔽(nextIndex), true).
    TRY(regexp_object.set(vm.names.lastIndex, Value(last_index), Object::ShouldThrowExceptions::Yes));
    return {};
}

// 1.1.2.1 Match Records, https://tc39.es/proposal-regexp-match-indices/#sec-match-records
struct Match {
    static Match create(regex::Match const& match)
    {
        return { match.global_offset, match.global_offset + match.view.length() };
    }

    size_t start_index { 0 };
    size_t end_index { 0 };
};

// 1.1.4.1.4 GetMatchIndicesArray ( S, match ), https://tc39.es/proposal-regexp-match-indices/#sec-getmatchindicesarray
static Value get_match_indices_array(GlobalObject& global_object, Utf16View const& string, Match const& match)
{
    // 1. Assert: Type(S) is String.
    // 2. Assert: match is a Match Record.

    // 3. Assert: match.[[StartIndex]] is an integer value ≥ 0 and ≤ the length of S.
    VERIFY(match.start_index <= string.length_in_code_units());

    // 4. Assert: match.[[EndIndex]] is an integer value ≥ match.[[StartIndex]] and ≤ the length of S.
    VERIFY(match.end_index >= match.start_index);
    VERIFY(match.end_index <= string.length_in_code_units());

    // 5. Return CreateArrayFromList(« match.[[StartIndex]], match.[[EndIndex]] »).
    return Array::create_from(global_object, { Value(match.start_index), Value(match.end_index) });
}

// 1.1.4.1.5 MakeIndicesArray ( S , indices, groupNames, hasGroups ), https://tc39.es/proposal-regexp-match-indices/#sec-makeindicesarray
static Value make_indices_array(GlobalObject& global_object, Utf16View const& string, Vector<Optional<Match>> const& indices, HashMap<FlyString, Match> const& group_names, bool has_groups)
{
    // Note: This implementation differs from the spec, but has the same behavior.
    //
    // The spec dictates that [[RegExpMatcher]] results should contain one list of capture groups,
    // where each entry holds its group name (if it has one). However, LibRegex stores named capture
    // groups in a separate hash map.
    //
    // The spec further specifies that the group names provided to this abstraction align with the
    // provided indices starting at indices[1], where any entry in indices that does not have a group
    // name is undefined in the group names list. But, the undefined groups names are then just
    // dropped when copying them to the output array.
    //
    // Therefore, this implementation tracks the group names without the assertion that the group
    // names align with the indices. The end result is the same.

    auto& vm = global_object.vm();

    // 1. Assert: Type(S) is String.
    // 2. Assert: indices is a List.
    // 3. Assert: Type(hasGroups) is Boolean.

    // 4. Let n be the number of elements in indices.
    // 5. Assert: n < 2^32-1.
    VERIFY(indices.size() < NumericLimits<u32>::max());

    // 6. Assert: groupNames is a List with n - 1 elements.
    // 7. NOTE: The groupNames List contains elements aligned with the indices List starting at indices[1].

    // 8. Set A to ! ArrayCreate(n).
    auto* array = MUST(Array::create(global_object, indices.size()));

    // 9. Assert: The value of A's "length" property is n.

    // 10. If hasGroups is true, then
    //     a. Let groups be ! ObjectCreate(null).
    // 11. Else,
    //     a. Let groups be undefined.
    auto groups = has_groups ? Object::create(global_object, nullptr) : js_undefined();

    // 13. For each integer i such that i ≥ 0 and i < n, do
    for (size_t i = 0; i < indices.size(); ++i) {
        // a. Let matchIndices be indices[i].
        auto const& match_indices = indices[i];

        // b. If matchIndices is not undefined, then
        //     i. Let matchIndicesArray be ! GetMatchIndicesArray(S, matchIndices).
        // c. Else,
        //     i. Let matchIndicesArray be undefined.
        auto match_indices_array = js_undefined();
        if (match_indices.has_value())
            match_indices_array = get_match_indices_array(global_object, string, *match_indices);

        // d. Perform ! CreateDataProperty(A, ! ToString(i), matchIndicesArray).
        MUST(array->create_data_property(i, match_indices_array));
    }

    for (auto const& entry : group_names) {
        auto match_indices_array = get_match_indices_array(global_object, string, entry.value);

        // e. If i > 0 and groupNames[i - 1] is not undefined, then
        //     i. Perform ! CreateDataProperty(groups, groupNames[i - 1], matchIndicesArray).
        MUST(groups.as_object().create_data_property(entry.key, match_indices_array));
    }

    // 12. Perform ! CreateDataProperty(A, "groups", groups).
    // NOTE: This step must be performed after the above loops in order for groups to be populated.
    MUST(array->create_data_property(vm.names.groups, groups));

    // 14. Return A.
    return array;
}

// 1.1.4.1.1 RegExpBuiltinExec ( R, S ), https://tc39.es/proposal-regexp-match-indices/#sec-regexpbuiltinexec
static ThrowCompletionOr<Value> regexp_builtin_exec(GlobalObject& global_object, RegExpObject& regexp_object, Utf16String string)
{
    auto& vm = global_object.vm();

    // 1. Assert: R is an initialized RegExp instance.
    // 2. Assert: Type(S) is String.

    // 3. Let length be the number of code units in S.
    // 4. Let lastIndex be ℝ(? ToLength(? Get(R, "lastIndex"))).
    auto last_index_value = TRY(regexp_object.get(vm.names.lastIndex));
    auto last_index = TRY(last_index_value.to_length(global_object));

    auto& regex = regexp_object.regex();

    // 5. Let flags be R.[[OriginalFlags]].
    // 6. If flags contains "g", let global be true; else let global be false.
    bool global = regex.options().has_flag_set(ECMAScriptFlags::Global);
    // 7. If flags contains "y", let sticky be true; else let sticky be false.
    bool sticky = regex.options().has_flag_set(ECMAScriptFlags::Sticky);
    // 8. If flags contains "d", let hasIndices be true, else let hasIndices be false.
    bool has_indices = regexp_object.flags().find('d').has_value();

    // 9. If global is false and sticky is false, set lastIndex to 0.
    if (!global && !sticky)
        last_index = 0;

    // 10. Let matcher be R.[[RegExpMatcher]].

    // 11. If flags contains "u", let fullUnicode be true; else let fullUnicode be false.
    bool full_unicode = regex.options().has_flag_set(ECMAScriptFlags::Unicode);

    RegexResult result;

    // NOTE: For optimisation purposes, this whole loop is implemented in LibRegex.
    // 12. Let matchSucceeded be false.
    // 13. Let Input be a List consisting of all of the characters, in order, of S. If fullUnicode is true, each character is a code unit, otherwise each character is a code point.
    // 14. Repeat, while matchSucceeded is false
    //   a. If lastIndex > length, then
    //       i. If global is true or sticky is true, then
    //           1. Perform ? Set(R, "lastIndex", 0, true).
    //       ii. Return null.
    //   b. Let r be matcher(Input, lastIndex).
    //   c. If r is failure, then
    //       i. If sticky is true, then
    //           1. Perform ? Set(R, "lastIndex", 0, true).
    //           2. Return null.
    //       ii. Set lastIndex to AdvanceStringIndex(S, lastIndex, fullUnicode).
    //   d. Else,
    //       i. Assert: r is a State.
    //       ii. Set matchSucceeded to true.

    // 14.b
    regex.start_offset = full_unicode ? string.view().code_point_offset_of(last_index) : last_index;
    result = regex.match(string.view());

    // 14.c and 14.a
    if (!result.success) {
        // 14.c.i, 14.a.i
        if (sticky || global)
            TRY(regexp_object.set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));

        // 14.a.ii, 14.c.i.2
        return js_null();
    }

    auto& match = result.matches[0];
    auto match_index = match.global_offset;

    // 15. Let e be r's endIndex value.
    // https://tc39.es/ecma262/#sec-notation: The endIndex is one plus the index of the last input character matched so far by the pattern.
    auto end_index = match_index + match.view.length();

    // 17. If fullUnicode is true, set e to ! GetStringIndex(S, Input, e).
    if (full_unicode) {
        match_index = string.view().code_unit_offset_of(match.global_offset);
        end_index = string.view().code_unit_offset_of(end_index);
    }

    // 18. If global is true or sticky is true, then
    if (global || sticky) {
        // a. Perform ? Set(R, "lastIndex", 𝔽(e), true).
        TRY(regexp_object.set(vm.names.lastIndex, Value(end_index), Object::ShouldThrowExceptions::Yes));
    }

    // 19. Let n be the number of elements in r's captures List. (This is the same value as 22.2.2.1's NcapturingParens.)
    // 20. Assert: n < 2^32 - 1.
    VERIFY(result.n_named_capture_groups < NumericLimits<u32>::max());

    // 21. Let A be ! ArrayCreate(n + 1).
    auto* array = MUST(Array::create(global_object, result.n_named_capture_groups + 1));

    // 22. Assert: The mathematical value of A's "length" property is n + 1.

    // 23. Perform ! CreateDataPropertyOrThrow(A, "index", 𝔽(lastIndex)).
    MUST(array->create_data_property_or_throw(vm.names.index, Value(match_index)));

    // 25. Let match be the Match { [[StartIndex]]: lastIndex, [[EndIndex]]: e }.
    auto match_indices = Match::create(match);

    // 26. Let indices be a new empty List.
    Vector<Optional<Match>> indices;

    // 27. Let groupNames be a new empty List.
    HashMap<FlyString, Match> group_names;

    // 28. Add match as the last element of indices.
    indices.append(move(match_indices));

    // 29. Let matchedValue be ! GetMatchString(S, match).
    // 30. Perform ! CreateDataPropertyOrThrow(A, "0", matchedValue).
    MUST(array->create_data_property_or_throw(0, js_string(vm, match.view.u16_view())));

    // 31. If R contains any GroupName, then
    //     a. Let groups be ! OrdinaryObjectCreate(null).
    //     b. Let hasGroups be true.
    // 32. Else,
    //     a. Let groups be undefined.
    //     b. Let hasGroups be false.
    bool has_groups = result.n_named_capture_groups != 0;
    Object* groups_object = has_groups ? Object::create(global_object, nullptr) : nullptr;

    // 34. For each integer i such that i ≥ 1 and i ≤ n, in ascending order, do
    for (size_t i = 1; i <= result.n_capture_groups; ++i) {
        // a. Let captureI be ith element of r's captures List.
        auto& capture = result.capture_group_matches[0][i];

        Value captured_value;

        // b. If captureI is undefined, then
        if (capture.view.is_null()) {
            // i. Let capturedValue be undefined.
            captured_value = js_undefined();
            // ii. Append undefined to indices.
            indices.append({});
        }
        // c. Else,
        else {
            // i. Let captureStart be captureI's startIndex.
            // ii. Let captureEnd be captureI's endIndex.
            // iii. If fullUnicode is true, then
            //     1. Set captureStart to ! GetStringIndex(S, Input, captureStart).
            //     2. Set captureEnd to ! GetStringIndex(S, Input, captureEnd).
            // iv. Let capture be the Match { [[StartIndex]]: captureStart, [[EndIndex]: captureEnd }.
            // v. Let capturedValue be ! GetMatchString(S, capture).
            captured_value = js_string(vm, capture.view.u16_view());
            // vi Append capture to indices.
            indices.append(Match::create(capture));
        }

        // d. Perform ! CreateDataPropertyOrThrow(A, ! ToString(𝔽(i)), capturedValue).
        MUST(array->create_data_property_or_throw(i, captured_value));

        // e. If the ith capture of R was defined with a GroupName, then
        if (capture.capture_group_name.has_value()) {
            // i. Let s be the CapturingGroupName of the corresponding RegExpIdentifierName.
            auto group_name = capture.capture_group_name.release_value();

            // ii. Perform ! CreateDataPropertyOrThrow(groups, s, capturedValue).
            MUST(groups_object->create_data_property_or_throw(group_name, captured_value));

            // iii. Append s to groupNames.
            group_names.set(move(group_name), Match::create(capture));
        }
        // f. Else,
        else {
            // i. Append undefined to groupNames.
            // See the note in MakeIndicesArray for why this step is skipped.
        }
    }

    // 33. Perform ! CreateDataPropertyOrThrow(A, "groups", groups).
    // NOTE: This step must be performed after the above loop in order for groups to be populated.
    Value groups = has_groups ? groups_object : js_undefined();
    MUST(array->create_data_property_or_throw(vm.names.groups, groups));

    // 35. If hasIndices is true, then
    if (has_indices) {
        // a. Let indicesArray be ! MakeIndicesArray(S, indices, groupNames, hasGroups).
        auto indices_array = make_indices_array(global_object, string.view(), indices, group_names, has_groups);
        // b. Perform ! CreateDataProperty(A, "indices", indicesArray).
        MUST(array->create_data_property(vm.names.indices, indices_array));
    }

    // 24. Perform ! CreateDataPropertyOrThrow(A, "input", S).
    // NOTE: This step is performed last to allow the string to be moved into the js_string invocation.
    MUST(array->create_data_property_or_throw(vm.names.input, js_string(vm, move(string))));

    // 36. Return A.
    return array;
}

// 22.2.5.2.1 RegExpExec ( R, S ), https://tc39.es/ecma262/#sec-regexpexec
ThrowCompletionOr<Value> regexp_exec(GlobalObject& global_object, Object& regexp_object, Utf16String string)
{
    auto& vm = global_object.vm();

    // 1. Let exec be ? Get(R, "exec").
    auto exec = TRY(regexp_object.get(vm.names.exec));

    // 2. If IsCallable(exec) is true, then
    if (exec.is_function()) {
        // a. Let result be ? Call(exec, R, « S »).
        auto result = TRY(call(global_object, exec.as_function(), &regexp_object, js_string(vm, move(string))));

        // b. If Type(result) is neither Object nor Null, throw a TypeError exception.
        if (!result.is_object() && !result.is_null())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOrNull, result.to_string_without_side_effects());

        // c. Return result.
        return result;
    }

    // 3. Perform ? RequireInternalSlot(R, [[RegExpMatcher]]).
    if (!is<RegExpObject>(regexp_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "RegExp");

    // 4. Return ? RegExpBuiltinExec(R, S).
    return regexp_builtin_exec(global_object, static_cast<RegExpObject&>(regexp_object), move(string));
}

// 22.2.5.2.3 AdvanceStringIndex ( S, index, unicode ), https://tc39.es/ecma262/#sec-advancestringindex
size_t advance_string_index(Utf16View const& string, size_t index, bool unicode)
{
    // 1. Assert: index ≤ 2^53 - 1.

    // 2. If unicode is false, return index + 1.
    if (!unicode)
        return index + 1;

    // 3. Let length be the number of code units in S.
    // 4. If index + 1 ≥ length, return index + 1.
    if (index + 1 >= string.length_in_code_units())
        return index + 1;

    // 5. Let cp be ! CodePointAt(S, index).
    auto code_point = code_point_at(string, index);

    // 6. Return index + cp.[[CodeUnitCount]].
    return index + code_point.code_unit_count;
}

// 1.1.4.3 get RegExp.prototype.hasIndices, https://tc39.es/proposal-regexp-match-indices/#sec-get-regexp.prototype.hasIndices
// 22.2.5.3 get RegExp.prototype.dotAll, https://tc39.es/ecma262/#sec-get-regexp.prototype.dotAll
// 22.2.5.5 get RegExp.prototype.global, https://tc39.es/ecma262/#sec-get-regexp.prototype.global
// 22.2.5.6 get RegExp.prototype.ignoreCase, https://tc39.es/ecma262/#sec-get-regexp.prototype.ignorecase
// 22.2.5.9 get RegExp.prototype.multiline, https://tc39.es/ecma262/#sec-get-regexp.prototype.multiline
// 22.2.5.14 get RegExp.prototype.sticky, https://tc39.es/ecma262/#sec-get-regexp.prototype.sticky
// 22.2.5.17 get RegExp.prototype.unicode, https://tc39.es/ecma262/#sec-get-regexp.prototype.unicode
#define __JS_ENUMERATE(flagName, flag_name, flag_char)                                                    \
    JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::flag_name)                                                 \
    {                                                                                                     \
        /* 1. If Type(R) is not Object, throw a TypeError exception. */                                   \
        auto* regexp_object = TRY(this_object(global_object));                                            \
        /* 2. If R does not have an [[OriginalFlags]] internal slot, then */                              \
        if (!is<RegExpObject>(regexp_object)) {                                                           \
            /* a. If SameValue(R, %RegExp.prototype%) is true, return undefined. */                       \
            if (same_value(regexp_object, global_object.regexp_prototype()))                              \
                return js_undefined();                                                                    \
            /* b. Otherwise, throw a TypeError exception. */                                              \
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "RegExp"); \
        }                                                                                                 \
        /* 3. Let flags be R.[[OriginalFlags]]. */                                                        \
        auto const& flags = static_cast<RegExpObject*>(regexp_object)->flags();                           \
        /* 4. If flags contains codeUnit, return true. */                                                 \
        /* 5. Return false. */                                                                            \
        return Value(flags.contains(#flag_char##sv));                                                     \
    }
JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE

// 22.2.5.2 RegExp.prototype.exec ( string ), https://tc39.es/ecma262/#sec-regexp.prototype.exec
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::exec)
{
    // 1. Let R be the this value.
    // 2. Perform ? RequireInternalSlot(R, [[RegExpMatcher]]).
    auto* regexp_object = TRY(typed_this_object(global_object));

    // 3. Let S be ? ToString(string).
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    // 4. Return ? RegExpBuiltinExec(R, S).
    return TRY(regexp_builtin_exec(global_object, *regexp_object, move(string)));
}

// 22.2.5.4 get RegExp.prototype.flags, https://tc39.es/ecma262/#sec-get-regexp.prototype.flags
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::flags)
{

    // 1. Let R be the this value.
    // 2. If Type(R) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let result be the empty String.
    StringBuilder builder(8);

    // 4. Let global be ! ToBoolean(? Get(R, "global")).
    // 5. If global is true, append the code unit 0x0067 (LATIN SMALL LETTER G) as the last code unit of result.
    // 6. Let ignoreCase be ! ToBoolean(? Get(R, "ignoreCase")).
    // 7. If ignoreCase is true, append the code unit 0x0069 (LATIN SMALL LETTER I) as the last code unit of result.
    // 8. Let multiline be ! ToBoolean(? Get(R, "multiline")).
    // 9. If multiline is true, append the code unit 0x006D (LATIN SMALL LETTER M) as the last code unit of result.
    // 10. Let dotAll be ! ToBoolean(? Get(R, "dotAll")).
    // 11. If dotAll is true, append the code unit 0x0073 (LATIN SMALL LETTER S) as the last code unit of result.
    // 12. Let unicode be ! ToBoolean(? Get(R, "unicode")).
    // 13. If unicode is true, append the code unit 0x0075 (LATIN SMALL LETTER U) as the last code unit of result.
    // 14. Let sticky be ! ToBoolean(? Get(R, "sticky")).
    // 15. If sticky is true, append the code unit 0x0079 (LATIN SMALL LETTER Y) as the last code unit of result.
#define __JS_ENUMERATE(flagName, flag_name, flag_char)                  \
    auto flag_##flag_name = TRY(regexp_object->get(vm.names.flagName)); \
    if (flag_##flag_name.to_boolean())                                  \
        builder.append(#flag_char);
    JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE

    // 16. Return result.
    return js_string(vm, builder.to_string());
}

// 22.2.5.7 RegExp.prototype [ @@match ] ( string ), https://tc39.es/ecma262/#sec-regexp.prototype-@@match
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_match)
{
    // 1. Let rx be the this value.
    // 2. If Type(rx) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let S be ? ToString(string).
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    // 4. Let global be ! ToBoolean(? Get(rx, "global")).
    bool global = TRY(regexp_object->get(vm.names.global)).to_boolean();

    // 5. If global is false, then
    if (!global) {
        // a. Return ? RegExpExec(rx, S).
        return TRY(regexp_exec(global_object, *regexp_object, move(string)));
    }

    // 6. Else,
    // a. Assert: global is true.

    // b. Let fullUnicode be ! ToBoolean(? Get(rx, "unicode")).
    bool full_unicode = TRY(regexp_object->get(vm.names.unicode)).to_boolean();

    // c. Perform ? Set(rx, "lastIndex", +0𝔽, true).
    TRY(regexp_object->set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));

    // d. Let A be ! ArrayCreate(0).
    auto* array = MUST(Array::create(global_object, 0));

    // e. Let n be 0.
    size_t n = 0;

    // f. Repeat,
    while (true) {
        // i. Let result be ? RegExpExec(rx, S).
        auto result = TRY(regexp_exec(global_object, *regexp_object, string));

        // ii. If result is null, then
        if (result.is_null()) {
            // 1. If n = 0, return null.
            if (n == 0)
                return js_null();

            // 2. Return A.
            return array;
        }

        // iii. Else,

        // 1. Let matchStr be ? ToString(? Get(result, "0")).
        auto match_value = TRY(result.get(global_object, 0));
        auto match_str = TRY(match_value.to_string(global_object));

        // 2. Perform ! CreateDataPropertyOrThrow(A, ! ToString(𝔽(n)), matchStr).
        MUST(array->create_data_property_or_throw(n, js_string(vm, match_str)));

        // 3. If matchStr is the empty String, then
        if (match_str.is_empty()) {
            // Steps 3a-3c are implemented by increment_last_index.
            TRY(increment_last_index(global_object, *regexp_object, string.view(), full_unicode));
        }

        // 4. Set n to n + 1.
        ++n;
    }
}

// 22.2.5.8 RegExp.prototype [ @@matchAll ] ( string ), https://tc39.es/ecma262/#sec-regexp-prototype-matchall
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_match_all)
{
    // 1. Let R be the this value.
    // 2. If Type(R) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let S be ? ToString(string).
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    // 4. Let C be ? SpeciesConstructor(R, %RegExp%).
    auto* constructor = TRY(species_constructor(global_object, *regexp_object, *global_object.regexp_constructor()));

    // 5. Let flags be ? ToString(? Get(R, "flags")).
    auto flags_value = TRY(regexp_object->get(vm.names.flags));
    auto flags = TRY(flags_value.to_string(global_object));

    // Steps 9-12 are performed early so that flags can be moved.

    // 9. If flags contains "g", let global be true.
    // 10. Else, let global be false.
    bool global = flags.contains('g');

    // 11. If flags contains "u", let fullUnicode be true.
    // 12. Else, let fullUnicode be false.
    bool full_unicode = flags.contains('u');

    // 6. Let matcher be ? Construct(C, « R, flags »).
    auto* matcher = TRY(construct(global_object, *constructor, regexp_object, js_string(vm, move(flags))));

    // 7. Let lastIndex be ? ToLength(? Get(R, "lastIndex")).
    auto last_index_value = TRY(regexp_object->get(vm.names.lastIndex));
    auto last_index = TRY(last_index_value.to_length(global_object));

    // 8. Perform ? Set(matcher, "lastIndex", lastIndex, true).
    TRY(matcher->set(vm.names.lastIndex, Value(last_index), Object::ShouldThrowExceptions::Yes));

    // 13. Return ! CreateRegExpStringIterator(matcher, S, global, fullUnicode).
    return RegExpStringIterator::create(global_object, *matcher, move(string), global, full_unicode);
}

// 22.2.5.10 RegExp.prototype [ @@replace ] ( string, replaceValue ), https://tc39.es/ecma262/#sec-regexp.prototype-@@replace
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_replace)
{
    auto string_value = vm.argument(0);
    auto replace_value = vm.argument(1);

    // 1. Let rx be the this value.
    // 2. If Type(rx) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let S be ? ToString(string).
    auto string = TRY(string_value.to_utf16_string(global_object));

    // 4. Let lengthS be the number of code unit elements in S.
    // 5. Let functionalReplace be IsCallable(replaceValue).

    // 6. If functionalReplace is false, then
    if (!replace_value.is_function()) {
        // a. Set replaceValue to ? ToString(replaceValue).
        auto replace_string = TRY(replace_value.to_string(global_object));
        replace_value = js_string(vm, move(replace_string));
    }

    // 7. Let global be ! ToBoolean(? Get(rx, "global")).
    bool global = TRY(regexp_object->get(vm.names.global)).to_boolean();
    bool full_unicode = false;

    // 8. If global is true, then
    if (global) {
        // a. Let fullUnicode be ! ToBoolean(? Get(rx, "unicode")).
        full_unicode = TRY(regexp_object->get(vm.names.unicode)).to_boolean();

        // b. Perform ? Set(rx, "lastIndex", +0𝔽, true).
        TRY(regexp_object->set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));
    }

    // 9. Let results be a new empty List.
    MarkedVector<Value> results(vm.heap());

    // 10. Let done be false.
    // 11. Repeat, while done is false,
    while (true) {
        // a. Let result be ? RegExpExec(rx, S).
        auto result = TRY(regexp_exec(global_object, *regexp_object, string));

        // b. If result is null, set done to true.
        if (result.is_null())
            break;

        // c. Else,

        // i. Append result to the end of results.
        results.append(result);

        // ii. If global is false, set done to true.
        if (!global)
            break;

        // iii. Else,

        // 1. Let matchStr be ? ToString(? Get(result, "0")).
        auto match_value = TRY(result.get(global_object, 0));
        auto match_str = TRY(match_value.to_string(global_object));

        // 2. If matchStr is the empty String, then
        if (match_str.is_empty()) {
            // Steps 2a-2c are implemented by increment_last_index.
            TRY(increment_last_index(global_object, *regexp_object, string.view(), full_unicode));
        }
    }

    // 12. Let accumulatedResult be the empty String.
    StringBuilder accumulated_result;

    // 13. Let nextSourcePosition be 0.
    size_t next_source_position = 0;

    // 14. For each element result of results, do
    for (auto& result : results) {
        // a. Let resultLength be ? LengthOfArrayLike(result).
        size_t result_length = TRY(length_of_array_like(global_object, result.as_object()));

        // b. Let nCaptures be max(resultLength - 1, 0).
        size_t n_captures = result_length == 0 ? 0 : result_length - 1;

        // c. Let matched be ? ToString(? Get(result, "0")).
        auto matched_value = TRY(result.get(global_object, 0));
        auto matched = TRY(matched_value.to_utf16_string(global_object));

        // d. Let matchLength be the number of code units in matched.
        auto matched_length = matched.length_in_code_units();

        // e. Let position be ? ToIntegerOrInfinity(? Get(result, "index")).
        auto position_value = TRY(result.get(global_object, vm.names.index));
        double position = TRY(position_value.to_integer_or_infinity(global_object));

        // f. Set position to the result of clamping position between 0 and lengthS.
        position = clamp(position, static_cast<double>(0), static_cast<double>(string.length_in_code_units()));

        // g. Let n be 1.

        // h. Let captures be a new empty List.
        MarkedVector<Value> captures(vm.heap());

        // i. Repeat, while n ≤ nCaptures,
        for (size_t n = 1; n <= n_captures; ++n) {
            // i. Let capN be ? Get(result, ! ToString(𝔽(n))).
            auto capture = TRY(result.get(global_object, n));

            // ii. If capN is not undefined, then
            if (!capture.is_undefined()) {
                // 1. Set capN to ? ToString(capN).
                capture = js_string(vm, TRY(capture.to_string(global_object)));
            }

            // iii. Append capN as the last element of captures.
            captures.append(move(capture));

            // iv. NOTE: When n = 1, the preceding step puts the first element into captures (at index 0). More generally, the nth capture (the characters captured by the nth set of capturing parentheses) is at captures[n - 1].
            // v. Set n to n + 1.
        }

        // j. Let namedCaptures be ? Get(result, "groups").
        auto named_captures = TRY(result.get(global_object, vm.names.groups));

        String replacement;

        // k. If functionalReplace is true, then
        if (replace_value.is_function()) {
            // i. Let replacerArgs be « matched ».
            MarkedVector<Value> replacer_args(vm.heap());
            replacer_args.append(js_string(vm, move(matched)));

            // ii. Append in List order the elements of captures to the end of the List replacerArgs.
            replacer_args.extend(move(captures));

            // iii. Append 𝔽(position) and S to replacerArgs.
            replacer_args.append(Value(position));
            replacer_args.append(js_string(vm, string));

            // iv. If namedCaptures is not undefined, then
            if (!named_captures.is_undefined()) {
                // 1. Append namedCaptures as the last element of replacerArgs.
                replacer_args.append(move(named_captures));
            }

            // v. Let replValue be ? Call(replaceValue, undefined, replacerArgs).
            auto replace_result = TRY(call(global_object, replace_value.as_function(), js_undefined(), move(replacer_args)));

            // vi. Let replacement be ? ToString(replValue).
            replacement = TRY(replace_result.to_string(global_object));
        }
        // l. Else,
        else {
            /// i. If namedCaptures is not undefined, then
            if (!named_captures.is_undefined()) {
                // 1. Set namedCaptures to ? ToObject(namedCaptures).
                named_captures = TRY(named_captures.to_object(global_object));
            }

            // ii. Let replacement be ? GetSubstitution(matched, S, position, captures, namedCaptures, replaceValue).
            replacement = TRY(get_substitution(global_object, matched.view(), string.view(), position, captures, named_captures, replace_value));
        }

        // m. If position ≥ nextSourcePosition, then
        if (position >= next_source_position) {
            // i. NOTE: position should not normally move backwards. If it does, it is an indication of an ill-behaving RegExp subclass or use of an access triggered side-effect to change the global flag or other characteristics of rx. In such cases, the corresponding substitution is ignored.

            // ii. Set accumulatedResult to the string-concatenation of accumulatedResult, the substring of S from nextSourcePosition to position, and replacement.
            auto substring = string.substring_view(next_source_position, position - next_source_position);
            accumulated_result.append(substring);
            accumulated_result.append(replacement);

            // iii. Set nextSourcePosition to position + matchLength.
            next_source_position = position + matched_length;
        }
    }

    // 15. If nextSourcePosition ≥ lengthS, return accumulatedResult.
    if (next_source_position >= string.length_in_code_units())
        return js_string(vm, accumulated_result.build());

    // 16. Return the string-concatenation of accumulatedResult and the substring of S from nextSourcePosition.
    auto substring = string.substring_view(next_source_position);
    accumulated_result.append(substring);

    return js_string(vm, accumulated_result.build());
}

// 22.2.5.11 RegExp.prototype [ @@search ] ( string ), https://tc39.es/ecma262/#sec-regexp.prototype-@@search
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_search)
{
    // 1. Let rx be the this value.
    // 2. If Type(rx) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let S be ? ToString(string).
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    // 4. Let previousLastIndex be ? Get(rx, "lastIndex").
    auto previous_last_index = TRY(regexp_object->get(vm.names.lastIndex));

    // 5. If SameValue(previousLastIndex, +0𝔽) is false, then
    if (!same_value(previous_last_index, Value(0))) {
        // a. Perform ? Set(rx, "lastIndex", +0𝔽, true).
        TRY(regexp_object->set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));
    }

    // 6. Let result be ? RegExpExec(rx, S).
    auto result = TRY(regexp_exec(global_object, *regexp_object, move(string)));

    // 7. Let currentLastIndex be ? Get(rx, "lastIndex").
    auto current_last_index = TRY(regexp_object->get(vm.names.lastIndex));

    // 8. If SameValue(currentLastIndex, previousLastIndex) is false, then
    if (!same_value(current_last_index, previous_last_index)) {
        // a. Perform ? Set(rx, "lastIndex", previousLastIndex, true).
        TRY(regexp_object->set(vm.names.lastIndex, previous_last_index, Object::ShouldThrowExceptions::Yes));
    }

    // 9. If result is null, return -1𝔽.
    if (result.is_null())
        return Value(-1);

    // 10. Return ? Get(result, "index").
    return TRY(result.get(global_object, vm.names.index));
}

// 22.2.5.12 get RegExp.prototype.source, https://tc39.es/ecma262/#sec-get-regexp.prototype.source
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::source)
{
    // 1. Let R be the this value.
    // 2. If Type(R) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. If R does not have an [[OriginalSource]] internal slot, then
    if (!is<RegExpObject>(regexp_object)) {
        // a. If SameValue(R, %RegExp.prototype%) is true, return "(?:)".
        if (same_value(regexp_object, global_object.regexp_prototype()))
            return js_string(vm, "(?:)");

        // b. Otherwise, throw a TypeError exception.
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "RegExp");
    }

    // 4. Assert: R has an [[OriginalFlags]] internal slot.
    // 5. Let src be R.[[OriginalSource]].
    // 6. Let flags be R.[[OriginalFlags]].
    // 7. Return EscapeRegExpPattern(src, flags).
    return js_string(vm, static_cast<RegExpObject&>(*regexp_object).escape_regexp_pattern());
}

// 22.2.5.13 RegExp.prototype [ @@split ] ( string, limit ), https://tc39.es/ecma262/#sec-regexp.prototype-@@split
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_split)
{
    // 1. Let rx be the this value.
    // 2. If Type(rx) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let S be ? ToString(string).
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    // 4. Let C be ? SpeciesConstructor(rx, %RegExp%).
    auto* constructor = TRY(species_constructor(global_object, *regexp_object, *global_object.regexp_constructor()));

    // 5. Let flags be ? ToString(? Get(rx, "flags")).
    auto flags_value = TRY(regexp_object->get(vm.names.flags));
    auto flags = TRY(flags_value.to_string(global_object));

    // 6. If flags contains "u", let unicodeMatching be true.
    // 7. Else, let unicodeMatching be false.
    bool unicode_matching = flags.find('u').has_value();

    // 8. If flags contains "y", let newFlags be flags.
    // 9. Else, let newFlags be the string-concatenation of flags and "y".
    auto new_flags = flags.find('y').has_value() ? move(flags) : String::formatted("{}y", flags);

    // 10. Let splitter be ? Construct(C, « rx, newFlags »).
    auto* splitter = TRY(construct(global_object, *constructor, regexp_object, js_string(vm, move(new_flags))));

    // 11. Let A be ! ArrayCreate(0).
    auto* array = MUST(Array::create(global_object, 0));

    // 12. Let lengthA be 0.
    size_t array_length = 0;

    // 13. If limit is undefined, let lim be 2^32 - 1; else let lim be ℝ(? ToUint32(limit)).
    auto limit = NumericLimits<u32>::max();
    if (!vm.argument(1).is_undefined())
        limit = TRY(vm.argument(1).to_u32(global_object));

    // 14. If lim is 0, return A.
    if (limit == 0)
        return array;

    // 15. Let size be the length of S.
    // 16. If size is 0, then
    if (string.is_empty()) {
        // a. Let z be ? RegExpExec(splitter, S).
        auto result = TRY(regexp_exec(global_object, *splitter, string));

        // b. If z is not null, return A.
        if (!result.is_null())
            return array;

        // c. Perform ! CreateDataPropertyOrThrow(A, "0", S).
        MUST(array->create_data_property_or_throw(0, js_string(vm, move(string))));

        // d. Return A.
        return array;
    }

    // 17. Let p be 0.
    size_t last_match_end = 0;

    // 18. Let q be p.
    size_t next_search_from = 0;

    // 19. Repeat, while q < size,
    while (next_search_from < string.length_in_code_units()) {
        // a. Perform ? Set(splitter, "lastIndex", 𝔽(q), true).
        TRY(splitter->set(vm.names.lastIndex, Value(next_search_from), Object::ShouldThrowExceptions::Yes));

        // b. Let z be ? RegExpExec(splitter, S).
        auto result = TRY(regexp_exec(global_object, *splitter, string));

        // c. If z is null, set q to AdvanceStringIndex(S, q, unicodeMatching).
        if (result.is_null()) {
            next_search_from = advance_string_index(string.view(), next_search_from, unicode_matching);
            continue;
        }

        // d. Else,

        // i. Let e be ℝ(? ToLength(? Get(splitter, "lastIndex"))).
        auto last_index_value = TRY(splitter->get(vm.names.lastIndex));
        auto last_index = TRY(last_index_value.to_length(global_object));

        // ii. Set e to min(e, size).
        last_index = min(last_index, string.length_in_code_units());

        // iii. If e = p, set q to AdvanceStringIndex(S, q, unicodeMatching).
        if (last_index == last_match_end) {
            next_search_from = advance_string_index(string.view(), next_search_from, unicode_matching);
            continue;
        }

        // iv. Else,

        // 1. Let T be the substring of S from p to q.
        auto substring = string.substring_view(last_match_end, next_search_from - last_match_end);

        // 2. Perform ! CreateDataPropertyOrThrow(A, ! ToString(𝔽(lengthA)), T).
        MUST(array->create_data_property_or_throw(array_length, js_string(vm, substring)));

        // 3. Set lengthA to lengthA + 1.
        ++array_length;

        // 4. If lengthA = lim, return A.
        if (array_length == limit)
            return array;

        // 5. Set p to e.
        last_match_end = last_index;

        // 6. Let numberOfCaptures be ? LengthOfArrayLike(z).
        auto number_of_captures = TRY(length_of_array_like(global_object, result.as_object()));

        // 7. Set numberOfCaptures to max(numberOfCaptures - 1, 0).
        if (number_of_captures > 0)
            --number_of_captures;

        // 8. Let i be 1.
        // 9. Repeat, while i ≤ numberOfCaptures,
        for (size_t i = 1; i <= number_of_captures; ++i) {

            // a. Let nextCapture be ? Get(z, ! ToString(𝔽(i))).
            auto next_capture = TRY(result.get(global_object, i));

            // b. Perform ! CreateDataPropertyOrThrow(A, ! ToString(𝔽(lengthA)), nextCapture).
            MUST(array->create_data_property_or_throw(array_length, next_capture));

            // c. Set i to i + 1.

            // d. Set lengthA to lengthA + 1.
            ++array_length;

            // e. If lengthA = lim, return A.
            if (array_length == limit)
                return array;
        }

        // 10. Set q to p.
        next_search_from = last_match_end;
    }

    // 20. Let T be the substring of S from p to size.
    auto substring = string.substring_view(last_match_end);

    // 21. Perform ! CreateDataPropertyOrThrow(A, ! ToString(𝔽(lengthA)), T).
    MUST(array->create_data_property_or_throw(array_length, js_string(vm, substring)));

    // 22. Return A.
    return array;
}

// 22.2.5.15 RegExp.prototype.test ( S ), https://tc39.es/ecma262/#sec-regexp.prototype.test
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::test)
{
    // 1. Let R be the this value.
    // 2. If Type(R) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let string be ? ToString(S).
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    // 4. Let match be ? RegExpExec(R, string).
    auto match = TRY(regexp_exec(global_object, *regexp_object, move(string)));

    // 5. If match is not null, return true; else return false.
    return Value(!match.is_null());
}

// 22.2.5.16 RegExp.prototype.toString ( ), https://tc39.es/ecma262/#sec-regexp.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::to_string)
{
    // 1. Let R be the this value.
    // 2. If Type(R) is not Object, throw a TypeError exception.
    auto* regexp_object = TRY(this_object(global_object));

    // 3. Let pattern be ? ToString(? Get(R, "source")).
    auto source_attr = TRY(regexp_object->get(vm.names.source));
    auto pattern = TRY(source_attr.to_string(global_object));

    // 4. Let flags be ? ToString(? Get(R, "flags")).
    auto flags_attr = TRY(regexp_object->get(vm.names.flags));
    auto flags = TRY(flags_attr.to_string(global_object));

    // 5. Let result be the string-concatenation of "/", pattern, "/", and flags.
    // 6. Return result.
    return js_string(vm, String::formatted("/{}/{}", pattern, flags));
}

// B.2.4.1 RegExp.prototype.compile ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp.prototype.compile
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::compile)
{
    auto pattern = vm.argument(0);
    auto flags = vm.argument(1);

    // 1. Let O be the this value.
    // 2. Perform ? RequireInternalSlot(O, [[RegExpMatcher]]).
    auto* regexp_object = TRY(typed_this_object(global_object));

    // 3. If Type(pattern) is Object and pattern has a [[RegExpMatcher]] internal slot, then
    if (pattern.is_object() && is<RegExpObject>(pattern.as_object())) {
        // a. If flags is not undefined, throw a TypeError exception.
        if (!flags.is_undefined())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotUndefined, flags.to_string_without_side_effects());

        auto& regexp_pattern = static_cast<RegExpObject&>(pattern.as_object());

        // b. Let P be pattern.[[OriginalSource]].
        pattern = js_string(vm, regexp_pattern.pattern());

        // c. Let F be pattern.[[OriginalFlags]].
        flags = js_string(vm, regexp_pattern.flags());
    }
    // 4. Else,
    //     a. Let P be pattern.
    //     b. Let F be flags.

    // 5. Return ? RegExpInitialize(O, P, F).
    return TRY(regexp_object->regexp_initialize(global_object, pattern, flags));
}

}
