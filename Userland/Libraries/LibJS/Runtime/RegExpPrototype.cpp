/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

    define_native_function(vm.well_known_symbol_match(), symbol_match, 1, attr);

    u8 readable_attr = Attribute::Configurable;
    define_native_property(vm.names.flags, flags, {}, readable_attr);
    define_native_property(vm.names.source, source, {}, readable_attr);

#define __JS_ENUMERATE(flagName, flag_name, flag_char, ECMAScriptFlagName) \
    define_native_property(vm.names.flagName, flag_name, {}, readable_attr);
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

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::flags)
{
    auto this_object = this_object_from(vm, global_object);
    if (!this_object)
        return {};

    StringBuilder builder(8);

#define __JS_ENUMERATE(flagName, flag_name, flag_char, ECMAScriptFlagName)                \
    auto flag_##flag_name = this_object->get(vm.names.flagName).value_or(js_undefined()); \
    if (vm.exception())                                                                   \
        return {};                                                                        \
    if (flag_##flag_name.to_boolean())                                                    \
        builder.append(#flag_char);
    JS_ENUMERATE_REGEXP_FLAGS
#undef __JS_ENUMERATE

    return js_string(vm, builder.to_string());
}

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

RegexResult RegExpPrototype::do_match(const Regex<ECMA262>& re, const StringView& subject)
{
    auto result = re.match(subject);
    // The 'lastIndex' property is reset on failing tests (if 'global')
    if (!result.success && re.options().has_flag_set(ECMAScriptFlags::Global))
        re.start_offset = 0;

    return result;
}

JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::exec)
{
    // FIXME: This should try using dynamic properties for 'lastIndex',
    //        and internal slots [[RegExpMatcher]], [[OriginalFlags]], etc.
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    auto str = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    StringView str_to_match = str;

    // RegExps without "global" and "sticky" always start at offset 0.
    if (!regexp_object->regex().options().has_flag_set((ECMAScriptFlags)regex::AllFlags::Internal_Stateful))
        regexp_object->regex().start_offset = 0;

    auto result = do_match(regexp_object->regex(), str_to_match);
    if (!result.success)
        return js_null();

    auto& match = result.matches[0];

    // FIXME: Do code point index correction if the Unicode flag is set.
    auto* array = Array::create(global_object);
    array->indexed_properties().set_array_like_size(result.n_capture_groups + 1);
    array->define_property(vm.names.index, Value((i32)match.column));
    array->define_property(vm.names.input, js_string(vm, str));
    array->indexed_properties().put(array, 0, js_string(vm, match.view.to_string()));

    for (size_t i = 0; i < result.n_capture_groups; ++i) {
        auto capture_value = js_undefined();
        if (result.capture_group_matches[0].size() > i) {
            auto& capture = result.capture_group_matches[0][i];
            capture_value = js_string(vm, capture.view.to_string());
        }
        array->indexed_properties().put(array, i + 1, capture_value);
    }

    Value groups = js_undefined();
    if (result.n_named_capture_groups > 0) {
        auto groups_object = create_empty(global_object);
        for (auto& entry : result.named_capture_group_matches[0])
            groups_object->define_property(entry.key, js_string(vm, entry.value.view.to_string()));
        groups = move(groups_object);
    }

    array->define_property(vm.names.groups, groups);

    return array;
}

JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::test)
{
    // FIXME: This should try using dynamic properties for 'exec' first,
    //        before falling back to builtin_exec.
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    auto str = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};

    // RegExps without "global" and "sticky" always start at offset 0.
    if (!regexp_object->regex().options().has_flag_set((ECMAScriptFlags)regex::AllFlags::Internal_Stateful))
        regexp_object->regex().start_offset = 0;

    auto result = do_match(regexp_object->regex(), str);
    return Value(result.success);
}

JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::to_string)
{
    auto this_object = this_object_from(vm, global_object);
    if (!this_object)
        return {};

    auto source_attr = this_object->get(vm.names.source).value_or(js_undefined());
    if (vm.exception())
        return {};
    auto pattern = source_attr.to_string(global_object);
    if (vm.exception())
        return {};

    auto flags_attr = this_object->get(vm.names.flags).value_or(js_undefined());
    if (vm.exception())
        return {};
    auto flags = flags_attr.to_string(global_object);
    if (vm.exception())
        return {};

    return js_string(vm, String::formatted("/{}/{}", pattern, flags));
}

JS_DEFINE_NATIVE_FUNCTION(RegExpPrototype::symbol_match)
{
    // https://tc39.es/ecma262/#sec-regexp.prototype-@@match
    auto* rx = this_object_from(vm, global_object);
    if (!rx)
        return {};
    auto s = vm.argument(0).to_string(global_object);
    if (vm.exception())
        return {};
    auto global_value = rx->get(vm.names.global).value_or(js_undefined());
    if (vm.exception())
        return {};
    bool global = global_value.to_boolean();
    // FIXME: Implement and use RegExpExec, this does something different - https://tc39.es/ecma262/#sec-regexpexec
    auto* exec = get_method(global_object, rx, vm.names.exec);
    if (!exec)
        return js_undefined();
    // FIXME end
    if (!global)
        return vm.call(*exec, rx, js_string(vm, s));

    // FIXME: This should exec the RegExp repeatedly while updating "lastIndex"
    return vm.call(*exec, rx, js_string(vm, s));
}

}
