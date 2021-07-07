/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/RegExpPrototype.h>
#include <LibJS/Token.h>

namespace JS {

RegExpPrototype::RegExpPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
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

    define_native_function(*vm.well_known_symbol_match(), symbol_match, 1, attr);
    define_native_function(*vm.well_known_symbol_replace(), symbol_replace, 2, attr);

    define_native_accessor(vm.names.flags, flags, {}, Attribute::Configurable);
    define_native_accessor(vm.names.source, source, {}, Attribute::Configurable);

#define __JS_ENUMERATE(flagName, flag_name, flag_char, ECMAScriptFlagName) \
    define_native_accessor(vm.names.flagName, flag_name, {}, Attribute::Configurable);
    JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE
}

RegExpPrototype::~RegExpPrototype()
{
}

static Object* this_object_from(VM& vm, GlobalObject& global_object)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, this_value.to_string_without_side_effects());
        return {};
    }
    return &this_value.as_object();
}

static RegExpObject* regexp_object_from(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!is<RegExpObject>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "RegExp");
        return nullptr;
    }
    return static_cast<RegExpObject*>(this_object);
}

static String escape_regexp_pattern(const RegExpObject& regexp_object)
{
    auto pattern = regexp_object.pattern();
    if (pattern.is_empty())
        return "(?:)";
    // FIXME: Check u flag and escape accordingly
    pattern.replace("\n", "\\n", true);
    pattern.replace("\r", "\\r", true);
    pattern.replace(LINE_SEPARATOR, "\\u2028", true);
    pattern.replace(PARAGRAPH_SEPARATOR, "\\u2029", true);
    pattern.replace("/", "\\/", true);
    return pattern;
}

static RegexResult do_match(const Regex<ECMA262>& re, const StringView& subject)
{
    auto result = re.match(subject);
    // The 'lastIndex' property is reset on failing tests (if 'global')
    if (!result.success && re.options().has_flag_set(ECMAScriptFlags::Global))
        re.start_offset = 0;

    return result;
}

// 22.2.5.2.2 RegExpBuiltinExec ( R, S ), https://tc39.es/ecma262/#sec-regexpbuiltinexec
static Value regexp_builtin_exec(GlobalObject& global_object, RegExpObject& regexp_object, String const& string)
{
    // FIXME: This should try using dynamic properties for 'lastIndex',
    //        and internal slots [[RegExpMatcher]], [[OriginalFlags]], etc.
    auto& vm = global_object.vm();

    auto str = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    StringView str_to_match = string;

    // RegExps without "global" and "sticky" always start at offset 0.
    if (!regexp_object.regex().options().has_flag_set((ECMAScriptFlags)regex::AllFlags::Internal_Stateful)) {
        regexp_object.set(vm.names.lastIndex, Value(0), true);
        if (vm.exception())
            return {};
    }

    auto last_index = regexp_object.get(vm.names.lastIndex);
    if (vm.exception())
        return {};
    regexp_object.regex().start_offset = last_index.to_length(global_object);
    if (vm.exception())
        return {};

    auto result = do_match(regexp_object.regex(), str_to_match);

    regexp_object.set(vm.names.lastIndex, Value(regexp_object.regex().start_offset), true);
    if (vm.exception())
        return {};

    if (!result.success)
        return js_null();

    auto& match = result.matches[0];

    // FIXME: Do code point index correction if the Unicode flag is set.
    auto* array = Array::create(global_object, result.n_capture_groups + 1);
    if (vm.exception())
        return {};
    array->create_data_property_or_throw(vm.names.index, Value((i32)match.global_offset));
    array->create_data_property_or_throw(vm.names.input, js_string(vm, string));
    array->create_data_property_or_throw(0, js_string(vm, match.view.to_string()));

    for (size_t i = 0; i < result.n_capture_groups; ++i) {
        auto capture_value = js_undefined();
        auto& capture = result.capture_group_matches[0][i + 1];
        if (!capture.view.is_null())
            capture_value = js_string(vm, capture.view.to_string());
        array->create_data_property_or_throw(i + 1, capture_value);
    }

    Value groups = js_undefined();
    if (result.n_named_capture_groups > 0) {
        auto groups_object = Object::create(global_object, nullptr);
        for (auto& entry : result.named_capture_group_matches[0])
            groups_object->create_data_property_or_throw(entry.key, js_string(vm, entry.value.view.to_string()));
        groups = move(groups_object);
    }

    array->create_data_property_or_throw(vm.names.groups, groups);

    return array;
}

// 22.2.5.2.1 RegExpExec ( R, S ), https://tc39.es/ecma262/#sec-regexpexec
static Value regexp_exec(GlobalObject& global_object, Object& rx, String const& string)
{
    auto& vm = global_object.vm();

    auto exec = rx.get(vm.names.exec);
    if (vm.exception())
        return {};

    if (exec.is_function()) {
        auto result = vm.call(exec.as_function(), &rx, js_string(vm, string));
        if (vm.exception())
            return {};

        if (!result.is_object() && !result.is_null())
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObjectOrNull, result.to_string_without_side_effects());

        return result;
    }

    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return regexp_builtin_exec(global_object, *regexp_object, string);
}

// 22.2.5.3 get RegExp.prototype.dotAll, https://tc39.es/ecma262/#sec-get-regexp.prototype.dotAll
// 22.2.5.5 get RegExp.prototype.global, https://tc39.es/ecma262/#sec-get-regexp.prototype.global
// 22.2.5.6 get RegExp.prototype.ignoreCase, https://tc39.es/ecma262/#sec-get-regexp.prototype.ignorecase
// 22.2.5.9 get RegExp.prototype.multiline, https://tc39.es/ecma262/#sec-get-regexp.prototype.multiline
// 22.2.5.14 get RegExp.prototype.sticky, https://tc39.es/ecma262/#sec-get-regexp.prototype.sticky
// 22.2.5.17 get RegExp.prototype.unicode, https://tc39.es/ecma262/#sec-get-regexp.prototype.unicode
#define __JS_ENUMERATE(flagName, flag_name, flag_char, ECMAScriptFlagName)                                 \
    JS_DEFINE_NATIVE_GETTER(RegExpPrototype::flag_name)                                                    \
    {                                                                                                      \
        auto regexp_object = regexp_object_from(vm, global_object);                                        \
        if (!regexp_object)                                                                                \
            return {};                                                                                     \
                                                                                                           \
        return Value(regexp_object->declared_options().has_flag_set(ECMAScriptFlags::ECMAScriptFlagName)); \
    }
JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE

// 22.2.5.4 get RegExp.prototype.flags, https://tc39.es/ecma262/#sec-get-regexp.prototype.flags
JS_DEFINE_NATIVE_GETTER(RegExpPrototype::flags)
{
    auto this_object = this_object_from(vm, global_object);
    if (!this_object)
        return {};

    StringBuilder builder(8);

#define __JS_ENUMERATE(flagName, flag_name, flag_char, ECMAScriptFlagName) \
    auto flag_##flag_name = this_object->get(vm.names.flagName);           \
    if (vm.exception())                                                    \
        return {};                                                         \
    if (flag_##flag_name.to_boolean())                                     \
        builder.append(#flag_char);
    JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE

    return js_string(vm, builder.to_string());
}

// 22.2.5.12 get RegExp.prototype.source, https://tc39.es/ecma262/#sec-get-regexp.prototype.source
JS_DEFINE_NATIVE_GETTER(RegExpPrototype::source)
{
    auto this_object = this_object_from(vm, global_object);
    if (!this_object)
        return {};

    auto* regexp_prototype = global_object.regexp_prototype();
    if (this_object == regexp_prototype)
        return js_string(vm, "(?:)");

    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return js_string(vm, escape_regexp_pattern(*regexp_object));
}

// 22.2.5.2 RegExp.prototype.exec ( string ), https://tc39.es/ecma262/#sec-regexp.prototype.exec
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::exec)
{
    auto* regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    auto string = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    return regexp_builtin_exec(global_object, *regexp_object, string);
}

// 22.2.5.15 RegExp.prototype.test ( S ), https://tc39.es/ecma262/#sec-regexp.prototype.test
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::test)
{
    auto* regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    auto str = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    auto match = regexp_exec(global_object, *regexp_object, str);
    if (vm.exception())
        return {};

    return Value(!match.is_null());
}

// 22.2.5.16 RegExp.prototype.toString ( ), https://tc39.es/ecma262/#sec-regexp.prototype.tostring
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::to_string)
{
    auto this_object = this_object_from(vm, global_object);
    if (!this_object)
        return {};

    auto source_attr = this_object->get(vm.names.source);
    if (vm.exception())
        return {};
    auto pattern = source_attr.to_string(global_object);
    if (vm.exception())
        return {};

    auto flags_attr = this_object->get(vm.names.flags);
    if (vm.exception())
        return {};
    auto flags = flags_attr.to_string(global_object);
    if (vm.exception())
        return {};

    return js_string(vm, String::formatted("/{}/{}", pattern, flags));
}

// 22.2.5.7 RegExp.prototype [ @@match ] ( string ), https://tc39.es/ecma262/#sec-regexp.prototype-@@match
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_match)
{
    auto* rx = this_object_from(vm, global_object);
    if (!rx)
        return {};
    auto s = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto global_value = rx->get(vm.names.global);
    if (vm.exception())
        return {};
    bool global = global_value.to_boolean();
    // FIXME: Implement and use RegExpExec, this does something different - https://tc39.es/ecma262/#sec-regexpexec
    auto* exec = Value(rx).get_method(global_object, vm.names.exec);
    if (!exec)
        return js_undefined();
    // FIXME end
    if (!global)
        return vm.call(*exec, rx, js_string(vm, s));

    // FIXME: This should exec the RegExp repeatedly while updating "lastIndex"
    return vm.call(*exec, rx, js_string(vm, s));
}

// 22.2.5.10 RegExp.prototype [ @@replace ] ( string, replaceValue ), https://tc39.es/ecma262/#sec-regexp.prototype-@@replace
JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_replace)
{
    auto string_value = vm.argument(0);
    auto replace_value = vm.argument(1);

    auto rx = regexp_object_from(vm, global_object);
    if (!rx)
        return {};
    auto string = string_value.to_string(global_object);
    if (vm.exception())
        return {};

    if (!replace_value.is_function()) {
        auto replace_string = replace_value.to_string(global_object);
        if (vm.exception())
            return {};

        replace_value = js_string(vm, move(replace_string));
        if (vm.exception())
            return {};
    }

    auto global_value = rx->get(vm.names.global);
    if (vm.exception())
        return {};

    bool global = global_value.to_boolean();
    if (global) {
        rx->set(vm.names.lastIndex, Value(0), true);
        if (vm.exception())
            return {};
    }

    // FIXME: Implement and use RegExpExec - https://tc39.es/ecma262/#sec-regexpexec
    auto* exec = Value(rx).get_method(global_object, vm.names.exec);
    if (!exec)
        return {};

    MarkedValueList results(vm.heap());

    while (true) {
        auto result = vm.call(*exec, rx, string_value);
        if (vm.exception())
            return {};
        if (result.is_null())
            break;

        auto* result_object = result.to_object(global_object);
        if (!result_object)
            return {};

        results.append(result_object);
        if (!global)
            break;

        auto match_object = result_object->get(0);
        if (vm.exception())
            return {};

        String match_str = match_object.to_string(global_object);
        if (vm.exception())
            return {};
        if (match_str.is_empty()) {
            // FIXME: Implement AdvanceStringIndex to take Unicode code points into account - https://tc39.es/ecma262/#sec-advancestringindex
            //        Once implemented, step (8a) of the @@replace algorithm must also be implemented.
            auto last_index = rx->get(vm.names.lastIndex);
            if (vm.exception())
                return {};
            auto this_index = last_index.to_length(global_object);
            if (vm.exception())
                return {};
            rx->set(vm.names.lastIndex, Value(this_index + 1), true);
            if (vm.exception())
                return {};
        }
    }

    String accumulated_result;
    size_t next_source_position = 0;

    for (auto& result_value : results) {
        auto& result = result_value.as_object();
        size_t result_length = length_of_array_like(global_object, result);
        size_t n_captures = result_length == 0 ? 0 : result_length - 1;

        auto matched_value = result.get(0);
        if (vm.exception())
            return {};

        auto matched = matched_value.to_string(global_object);
        if (vm.exception())
            return {};

        auto position_value = result.get(vm.names.index);
        if (vm.exception())
            return {};

        double position = position_value.to_integer_or_infinity(global_object);
        if (vm.exception())
            return {};

        position = clamp(position, static_cast<double>(0), static_cast<double>(string.length()));

        MarkedValueList captures(vm.heap());
        for (size_t n = 1; n <= n_captures; ++n) {
            auto capture = result.get(n);
            if (vm.exception())
                return {};

            if (!capture.is_undefined()) {
                auto capture_string = capture.to_string(global_object);
                if (vm.exception())
                    return {};

                capture = Value(js_string(vm, capture_string));
                if (vm.exception())
                    return {};
            }

            captures.append(move(capture));
        }

        auto named_captures = result.get(vm.names.groups);
        if (vm.exception())
            return {};

        String replacement;

        if (replace_value.is_function()) {
            MarkedValueList replacer_args(vm.heap());
            replacer_args.append(js_string(vm, matched));
            replacer_args.extend(move(captures));
            replacer_args.append(Value(position));
            replacer_args.append(js_string(vm, string));
            if (!named_captures.is_undefined()) {
                replacer_args.append(move(named_captures));
            }

            auto replace_result = vm.call(replace_value.as_function(), js_undefined(), move(replacer_args));
            if (vm.exception())
                return {};

            replacement = replace_result.to_string(global_object);
            if (vm.exception())
                return {};
        } else {
            auto named_captures_object = js_undefined();
            if (!named_captures.is_undefined()) {
                named_captures_object = named_captures.to_object(global_object);
                if (vm.exception())
                    return {};
            }

            replacement = get_substitution(global_object, matched, string, position, captures, named_captures_object, replace_value);
            if (vm.exception())
                return {};
        }

        if (position >= next_source_position) {
            StringBuilder builder;
            builder.append(accumulated_result);
            builder.append(string.substring(next_source_position, position - next_source_position));
            builder.append(replacement);

            accumulated_result = builder.build();
            next_source_position = position + matched.length();
        }
    }

    if (next_source_position >= string.length())
        return js_string(vm, accumulated_result);

    StringBuilder builder;
    builder.append(accumulated_result);
    builder.append(string.substring(next_source_position));

    return js_string(vm, builder.build());
}

}
