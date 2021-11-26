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

static ThrowCompletionOr<void> increment_last_index(GlobalObject& global_object, Object& regexp_object, Utf16View const& string, bool unicode)
{
    auto& vm = global_object.vm();

    auto last_index_value = TRY(regexp_object.get(vm.names.lastIndex));
    auto last_index = TRY(last_index_value.to_length(global_object));

    last_index = advance_string_index(string, last_index, unicode);
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
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOrNull, result.to_string_without_side_effects());

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
        auto* regexp_object = TRY(this_object(global_object));                                            \
        if (!is<RegExpObject>(regexp_object)) {                                                           \
            if (same_value(regexp_object, global_object.regexp_prototype()))                              \
                return js_undefined();                                                                    \
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "RegExp"); \
        }                                                                                                 \
                                                                                                          \
        auto const& flags = static_cast<RegExpObject*>(regexp_object)->flags();                           \
        return Value(flags.contains(#flag_char##sv));                                                     \
    }
JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE

// 22.2.5.4 get RegExp.prototype.flags, https://tc39.es/ecma262/#sec-get-regexp.prototype.flags
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::flags)
{
    auto* regexp_object = TRY(this_object(global_object));
    StringBuilder builder(8);

#define __JS_ENUMERATE(flagName, flag_name, flag_char)                  \
    auto flag_##flag_name = TRY(regexp_object->get(vm.names.flagName)); \
    if (flag_##flag_name.to_boolean())                                  \
        builder.append(#flag_char);
    JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE

    return js_string(vm, builder.to_string());
}

// 22.2.5.12 get RegExp.prototype.source, https://tc39.es/ecma262/#sec-get-regexp.prototype.source
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::source)
{
    auto* regexp_object = TRY(this_object(global_object));
    if (!is<RegExpObject>(regexp_object)) {
        if (same_value(regexp_object, global_object.regexp_prototype()))
            return js_string(vm, "(?:)");
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObjectOfType, "RegExp");
    }

    return js_string(vm, static_cast<RegExpObject&>(*regexp_object).escape_regexp_pattern());
}

// 22.2.5.2 RegExp.prototype.exec ( string ), https://tc39.es/ecma262/#sec-regexp.prototype.exec
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::exec)
{
    auto* regexp_object = TRY(typed_this_object(global_object));

    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    return TRY(regexp_builtin_exec(global_object, *regexp_object, move(string)));
}

// 22.2.5.15 RegExp.prototype.test ( S ), https://tc39.es/ecma262/#sec-regexp.prototype.test
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::test)
{
    auto* regexp_object = TRY(this_object(global_object));
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    auto match = TRY(regexp_exec(global_object, *regexp_object, move(string)));

    return Value(!match.is_null());
}

// 22.2.5.16 RegExp.prototype.toString ( ), https://tc39.es/ecma262/#sec-regexp.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::to_string)
{
    auto* regexp_object = TRY(this_object(global_object));
    auto source_attr = TRY(regexp_object->get(vm.names.source));
    auto pattern = TRY(source_attr.to_string(global_object));

    auto flags_attr = TRY(regexp_object->get(vm.names.flags));
    auto flags = TRY(flags_attr.to_string(global_object));

    return js_string(vm, String::formatted("/{}/{}", pattern, flags));
}

// 22.2.5.7 RegExp.prototype [ @@match ] ( string ), https://tc39.es/ecma262/#sec-regexp.prototype-@@match
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_match)
{
    auto* regexp_object = TRY(this_object(global_object));
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    bool global = TRY(regexp_object->get(vm.names.global)).to_boolean();

    if (!global)
        return TRY(regexp_exec(global_object, *regexp_object, move(string)));

    TRY(regexp_object->set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));

    auto* array = MUST(Array::create(global_object, 0));

    bool unicode = TRY(regexp_object->get(vm.names.unicode)).to_boolean();

    size_t n = 0;

    while (true) {
        auto result = TRY(regexp_exec(global_object, *regexp_object, string));

        if (result.is_null()) {
            if (n == 0)
                return js_null();
            return array;
        }

        auto* result_object = TRY(result.to_object(global_object));
        auto match_object = TRY(result_object->get(0));
        auto match_str = TRY(match_object.to_string(global_object));

        TRY(array->create_data_property_or_throw(n, js_string(vm, match_str)));

        if (match_str.is_empty())
            TRY(increment_last_index(global_object, *regexp_object, string.view(), unicode));

        ++n;
    }
}

// 22.2.5.8 RegExp.prototype [ @@matchAll ] ( string ), https://tc39.es/ecma262/#sec-regexp-prototype-matchall
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_match_all)
{
    auto* regexp_object = TRY(this_object(global_object));
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    auto* constructor = TRY(species_constructor(global_object, *regexp_object, *global_object.regexp_constructor()));

    auto flags_value = TRY(regexp_object->get(vm.names.flags));
    auto flags = TRY(flags_value.to_string(global_object));

    bool global = flags.find('g').has_value();
    bool unicode = flags.find('u').has_value();

    MarkedValueList arguments(vm.heap());
    arguments.append(regexp_object);
    arguments.append(js_string(vm, move(flags)));
    auto* matcher = TRY(construct(global_object, *constructor, move(arguments)));
    auto last_index_value = TRY(regexp_object->get(vm.names.lastIndex));
    auto last_index = TRY(last_index_value.to_length(global_object));

    TRY(matcher->set(vm.names.lastIndex, Value(last_index), Object::ShouldThrowExceptions::Yes));

    return RegExpStringIterator::create(global_object, *matcher, move(string), global, unicode);
}

// 22.2.5.10 RegExp.prototype [ @@replace ] ( string, replaceValue ), https://tc39.es/ecma262/#sec-regexp.prototype-@@replace
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_replace)
{
    auto string_value = vm.argument(0);
    auto replace_value = vm.argument(1);

    auto* regexp_object = TRY(this_object(global_object));
    auto string = TRY(string_value.to_utf16_string(global_object));
    auto string_view = string.view();

    if (!replace_value.is_function()) {
        auto replace_string = TRY(replace_value.to_string(global_object));
        replace_value = js_string(vm, move(replace_string));
    }

    bool global = TRY(regexp_object->get(vm.names.global)).to_boolean();
    bool unicode = false;

    if (global) {
        unicode = TRY(regexp_object->get(vm.names.unicode)).to_boolean();

        TRY(regexp_object->set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));
    }

    MarkedValueList results(vm.heap());

    while (true) {
        auto result = TRY(regexp_exec(global_object, *regexp_object, string));
        if (result.is_null())
            break;

        auto* result_object = TRY(result.to_object(global_object));
        results.append(result_object);
        if (!global)
            break;

        auto match_object = TRY(result_object->get(0));
        auto match_str = TRY(match_object.to_string(global_object));

        if (match_str.is_empty())
            TRY(increment_last_index(global_object, *regexp_object, string_view, unicode));
    }

    StringBuilder accumulated_result;
    size_t next_source_position = 0;

    for (auto& result_value : results) {
        auto& result = result_value.as_object();
        size_t result_length = TRY(length_of_array_like(global_object, result));
        size_t n_captures = result_length == 0 ? 0 : result_length - 1;

        auto matched_value = TRY(result.get(0));
        auto matched = TRY(matched_value.to_utf16_string(global_object));
        auto matched_length = matched.length_in_code_units();

        auto position_value = TRY(result.get(vm.names.index));
        double position = TRY(position_value.to_integer_or_infinity(global_object));

        position = clamp(position, static_cast<double>(0), static_cast<double>(string.length_in_code_units()));

        MarkedValueList captures(vm.heap());
        for (size_t n = 1; n <= n_captures; ++n) {
            auto capture = TRY(result.get(n));
            if (!capture.is_undefined())
                capture = js_string(vm, TRY(capture.to_string(global_object)));

            captures.append(move(capture));
        }

        auto named_captures = TRY(result.get(vm.names.groups));

        String replacement;

        if (replace_value.is_function()) {
            MarkedValueList replacer_args(vm.heap());
            replacer_args.append(js_string(vm, move(matched)));
            replacer_args.extend(move(captures));
            replacer_args.append(Value(position));
            replacer_args.append(js_string(vm, string));
            if (!named_captures.is_undefined()) {
                replacer_args.append(move(named_captures));
            }

            auto replace_result = TRY(vm.call(replace_value.as_function(), js_undefined(), move(replacer_args)));
            replacement = TRY(replace_result.to_string(global_object));
        } else {
            if (!named_captures.is_undefined())
                named_captures = TRY(named_captures.to_object(global_object));

            replacement = TRY(get_substitution(global_object, matched.view(), string_view, position, captures, named_captures, replace_value));
        }

        if (position >= next_source_position) {
            auto substring = string_view.substring_view(next_source_position, position - next_source_position);
            accumulated_result.append(substring);
            accumulated_result.append(replacement);

            next_source_position = position + matched_length;
        }
    }

    if (next_source_position >= string.length_in_code_units())
        return js_string(vm, accumulated_result.build());

    auto substring = string_view.substring_view(next_source_position);
    accumulated_result.append(substring);

    return js_string(vm, accumulated_result.build());
}

// 22.2.5.11 RegExp.prototype [ @@search ] ( string ), https://tc39.es/ecma262/#sec-regexp.prototype-@@search
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_search)
{
    auto* regexp_object = TRY(this_object(global_object));
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));

    auto previous_last_index = TRY(regexp_object->get(vm.names.lastIndex));
    if (!same_value(previous_last_index, Value(0)))
        TRY(regexp_object->set(vm.names.lastIndex, Value(0), Object::ShouldThrowExceptions::Yes));

    auto result = TRY(regexp_exec(global_object, *regexp_object, move(string)));

    auto current_last_index = TRY(regexp_object->get(vm.names.lastIndex));
    if (!same_value(current_last_index, previous_last_index))
        TRY(regexp_object->set(vm.names.lastIndex, previous_last_index, Object::ShouldThrowExceptions::Yes));

    if (result.is_null())
        return Value(-1);

    auto* result_object = TRY(result.to_object(global_object));
    return TRY(result_object->get(vm.names.index));
}

// 22.2.5.13 RegExp.prototype [ @@split ] ( string, limit ), https://tc39.es/ecma262/#sec-regexp.prototype-@@split
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_split)
{
    auto* regexp_object = TRY(this_object(global_object));
    auto string = TRY(vm.argument(0).to_utf16_string(global_object));
    auto string_view = string.view();

    auto* constructor = TRY(species_constructor(global_object, *regexp_object, *global_object.regexp_constructor()));

    auto flags_value = TRY(regexp_object->get(vm.names.flags));
    auto flags = TRY(flags_value.to_string(global_object));

    bool unicode = flags.find('u').has_value();
    auto new_flags = flags.find('y').has_value() ? move(flags) : String::formatted("{}y", flags);

    MarkedValueList arguments(vm.heap());
    arguments.append(regexp_object);
    arguments.append(js_string(vm, move(new_flags)));
    auto* splitter = TRY(construct(global_object, *constructor, move(arguments)));
    auto* array = MUST(Array::create(global_object, 0));
    size_t array_length = 0;

    auto limit = NumericLimits<u32>::max();
    if (!vm.argument(1).is_undefined())
        limit = TRY(vm.argument(1).to_u32(global_object));

    if (limit == 0)
        return array;

    if (string.is_empty()) {
        auto result = TRY(regexp_exec(global_object, *splitter, string));
        if (!result.is_null())
            return array;

        MUST(array->create_data_property_or_throw(0, js_string(vm, move(string))));
        return array;
    }

    size_t last_match_end = 0;   // 'p' in the spec.
    size_t next_search_from = 0; // 'q' in the spec.

    while (next_search_from < string_view.length_in_code_units()) {
        TRY(splitter->set(vm.names.lastIndex, Value(next_search_from), Object::ShouldThrowExceptions::Yes));

        auto result = TRY(regexp_exec(global_object, *splitter, string));
        if (result.is_null()) {
            next_search_from = advance_string_index(string_view, next_search_from, unicode);
            continue;
        }

        auto last_index_value = TRY(splitter->get(vm.names.lastIndex));
        auto last_index = TRY(last_index_value.to_length(global_object)); // 'e' in the spec.
        last_index = min(last_index, string_view.length_in_code_units());

        if (last_index == last_match_end) {
            next_search_from = advance_string_index(string_view, next_search_from, unicode);
            continue;
        }

        auto substring = string_view.substring_view(last_match_end, next_search_from - last_match_end);
        MUST(array->create_data_property_or_throw(array_length, js_string(vm, move(substring))));

        if (++array_length == limit)
            return array;

        auto* result_object = TRY(result.to_object(global_object));
        auto number_of_captures = TRY(length_of_array_like(global_object, *result_object));
        if (number_of_captures > 0)
            --number_of_captures;

        for (size_t i = 1; i <= number_of_captures; ++i) {
            auto next_capture = TRY(result_object->get(i));
            MUST(array->create_data_property_or_throw(array_length, next_capture));
            if (++array_length == limit)
                return array;
        }

        last_match_end = last_index;
        next_search_from = last_index;
    }

    auto substring = string_view.substring_view(last_match_end);
    MUST(array->create_data_property_or_throw(array_length, js_string(vm, move(substring))));

    return array;
}

// B.2.4.1 RegExp.prototype.compile ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp.prototype.compile
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::compile)
{
    auto* regexp_object = TRY(typed_this_object(global_object));

    auto pattern = vm.argument(0);
    auto flags = vm.argument(1);

    Value pattern_value;
    Value flags_value;

    if (pattern.is_object() && is<RegExpObject>(pattern.as_object())) {
        if (!flags.is_undefined())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotUndefined, flags.to_string_without_side_effects());

        auto& regexp_pattern = static_cast<RegExpObject&>(pattern.as_object());
        pattern_value = js_string(vm, regexp_pattern.pattern());
        flags_value = js_string(vm, regexp_pattern.flags());
    } else {
        pattern_value = pattern;
        flags_value = flags;
    }

    return TRY(regexp_object->regexp_initialize(global_object, pattern_value, flags_value));
}

}
