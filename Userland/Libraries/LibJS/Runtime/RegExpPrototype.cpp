/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
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

// 22.2.5.2.3 AdvanceStringIndex ( S, index, unicode ), https://tc39.es/ecma262/#sec-advancestringindex
size_t advance_string_index(Utf16View const& string, size_t index, bool unicode)
{
    if (!unicode)
        return index + 1;

    if (index + 1 >= string.length_in_code_units())
        return index + 1;

    auto code_point = code_point_at(string, index);
    return index + code_point.code_unit_count;
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
    VERIFY(match.start_index <= string.length_in_code_units());
    VERIFY(match.end_index >= match.start_index);
    VERIFY(match.end_index <= string.length_in_code_units());

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

    VERIFY(indices.size() < NumericLimits<u32>::max());
    auto* array = MUST(Array::create(global_object, indices.size()));

    auto groups = has_groups ? Object::create(global_object, nullptr) : js_undefined();

    for (size_t i = 0; i < indices.size(); ++i) {
        auto const& match_indices = indices[i];

        auto match_indices_array = js_undefined();
        if (match_indices.has_value())
            match_indices_array = get_match_indices_array(global_object, string, *match_indices);

        MUST(array->create_data_property(i, match_indices_array));
    }

    for (auto const& entry : group_names) {
        auto match_indices_array = get_match_indices_array(global_object, string, entry.value);

        MUST(groups.as_object().create_data_property(entry.key, match_indices_array));
    }

    MUST(array->create_data_property(vm.names.groups, groups));

    return array;
}

// 22.2.5.2.2 RegExpBuiltinExec ( R, S ), https://tc39.es/ecma262/#sec-regexpbuiltinexec
static ThrowCompletionOr<Value> regexp_builtin_exec(GlobalObject& global_object, RegExpObject& regexp_object, Utf16String string)
{
    // FIXME: This should try using internal slots [[RegExpMatcher]], [[OriginalFlags]], etc.
    auto& vm = global_object.vm();

    auto last_index_value = TRY(regexp_object.get(vm.names.lastIndex));
    auto last_index = TRY(last_index_value.to_length(global_object));

    auto& regex = regexp_object.regex();
    bool global = regex.options().has_flag_set(ECMAScriptFlags::Global);
    bool sticky = regex.options().has_flag_set(ECMAScriptFlags::Sticky);
    bool unicode = regex.options().has_flag_set(ECMAScriptFlags::Unicode);
    bool has_indices = regexp_object.flags().find('d').has_value();

    if (!global && !sticky)
        last_index = 0;

    auto string_view = string.view();
    RegexResult result;

    while (true) {
        if (last_index > string.length_in_code_units()) {
            if (global || sticky)
                TRY(regexp_object.set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));

            return js_null();
        }

        regex.start_offset = unicode ? string_view.code_point_offset_of(last_index) : last_index;
        result = regex.match(string_view);

        if (result.success)
            break;

        if (sticky) {
            TRY(regexp_object.set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));

            return js_null();
        }

        last_index = advance_string_index(string_view, last_index, unicode);
    }

    auto& match = result.matches[0];
    auto match_index = match.global_offset;

    // https://tc39.es/ecma262/#sec-notation:
    // The endIndex is one plus the index of the last input character matched so far by the pattern.
    auto end_index = match_index + match.view.length();

    if (unicode) {
        match_index = string_view.code_unit_offset_of(match.global_offset);
        end_index = string_view.code_unit_offset_of(end_index);
    }

    if (global || sticky)
        TRY(regexp_object.set(vm.names.lastIndex, Value(end_index), Object::ShouldThrowExceptions::Yes));

    VERIFY(result.n_named_capture_groups < NumericLimits<u32>::max());
    auto* array = MUST(Array::create(global_object, result.n_named_capture_groups + 1));

    Vector<Optional<Match>> indices { Match::create(match) };
    HashMap<FlyString, Match> group_names;

    bool has_groups = result.n_named_capture_groups != 0;
    Object* groups_object = has_groups ? Object::create(global_object, nullptr) : nullptr;

    for (size_t i = 0; i < result.n_capture_groups; ++i) {
        auto capture_value = js_undefined();
        auto& capture = result.capture_group_matches[0][i + 1];
        if (capture.view.is_null()) {
            indices.append({});
        } else {
            capture_value = js_string(vm, capture.view.u16_view());
            indices.append(Match::create(capture));
        }
        MUST(array->create_data_property_or_throw(i + 1, capture_value));

        if (capture.capture_group_name.has_value()) {
            auto group_name = capture.capture_group_name.release_value();
            MUST(groups_object->create_data_property_or_throw(group_name, js_string(vm, capture.view.u16_view())));
            group_names.set(move(group_name), Match::create(capture));
        }
    }

    Value groups = has_groups ? groups_object : js_undefined();
    MUST(array->create_data_property_or_throw(vm.names.groups, groups));

    if (has_indices) {
        auto indices_array = make_indices_array(global_object, string_view, indices, group_names, has_groups);
        TRY(array->create_data_property(vm.names.indices, indices_array));
    }

    MUST(array->create_data_property_or_throw(vm.names.index, Value(match_index)));
    MUST(array->create_data_property_or_throw(0, js_string(vm, match.view.u16_view())));
    MUST(array->create_data_property_or_throw(vm.names.input, js_string(vm, move(string))));

    return array;
}

// 22.2.5.2.1 RegExpExec ( R, S ), https://tc39.es/ecma262/#sec-regexpexec
ThrowCompletionOr<Value> regexp_exec(GlobalObject& global_object, Object& regexp_object, Utf16String string)
{
    auto& vm = global_object.vm();

    auto exec = TRY(regexp_object.get(vm.names.exec));
    if (exec.is_function()) {
        auto result = TRY(vm.call(exec.as_function(), &regexp_object, js_string(vm, move(string))));

        if (!result.is_object() && !result.is_null())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOrNull, result.to_string_without_side_effects());

        return result;
    }

    if (!is<RegExpObject>(regexp_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "RegExp");

    return regexp_builtin_exec(global_object, static_cast<RegExpObject&>(regexp_object), move(string));
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
            // Stepsp 3a-3c are implemented by increment_last_index.
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
    MarkedValueList arguments(vm.heap());
    arguments.append(regexp_object);
    arguments.append(js_string(vm, move(flags)));
    auto* matcher = TRY(construct(global_object, *constructor, move(arguments)));

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
    MarkedValueList results(vm.heap());

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
            // Stepsp 2a-2c are implemented by increment_last_index.
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
        MarkedValueList captures(vm.heap());

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
            MarkedValueList replacer_args(vm.heap());
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
            auto replace_result = TRY(vm.call(replace_value.as_function(), js_undefined(), move(replacer_args)));

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
    MarkedValueList arguments(vm.heap());
    arguments.append(regexp_object);
    arguments.append(js_string(vm, move(new_flags)));
    auto* splitter = TRY(construct(global_object, *constructor, move(arguments)));

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
