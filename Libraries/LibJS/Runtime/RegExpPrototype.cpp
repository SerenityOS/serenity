/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

namespace JS {

RegExpPrototype::RegExpPrototype(GlobalObject& global_object)
    : RegExpObject({}, {}, *global_object.object_prototype())
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

    u8 readable_attr = Attribute::Configurable;
    define_native_property(vm.names.dotAll, dot_all, nullptr, readable_attr);
    define_native_property(vm.names.flags, flags, nullptr, readable_attr);
    define_native_property(vm.names.global, global, nullptr, readable_attr);
    define_native_property(vm.names.ignoreCase, ignore_case, nullptr, readable_attr);
    define_native_property(vm.names.multiline, multiline, nullptr, readable_attr);
    define_native_property(vm.names.source, source, nullptr, readable_attr);
    define_native_property(vm.names.sticky, sticky, nullptr, readable_attr);
    define_native_property(vm.names.unicode, unicode, nullptr, readable_attr);
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
    if (!this_object->is_regexp_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "RegExp");
        return nullptr;
    }
    return static_cast<RegExpObject*>(this_object);
}

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::dot_all)
{
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return Value(regexp_object->declared_options().has_flag_set(ECMAScriptFlags::SingleLine));
}

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

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::global)
{
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return Value(regexp_object->declared_options().has_flag_set(ECMAScriptFlags::Global)); // Note that this "Global" is actually "Global | Stateful"
}

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::ignore_case)
{
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return Value(regexp_object->declared_options().has_flag_set(ECMAScriptFlags::Insensitive));
}

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::multiline)
{
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return Value(regexp_object->declared_options().has_flag_set(ECMAScriptFlags::Multiline));
}

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::source)
{
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return js_string(vm, regexp_object->pattern());
}

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::sticky)
{
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return Value(regexp_object->declared_options().has_flag_set(ECMAScriptFlags::Sticky));
}

JS_DEFINE_NATIVE_GETTER(RegExpPrototype::unicode)
{
    auto regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};

    return Value(regexp_object->declared_options().has_flag_set(ECMAScriptFlags::Unicode));
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
        auto& capture = result.capture_group_matches[0][i];
        array->indexed_properties().put(array, i + 1, js_string(vm, capture.view.to_string()));
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
    auto* regexp_object = regexp_object_from(vm, global_object);
    if (!regexp_object)
        return {};
    return js_string(vm, String::formatted("/{}/{}", regexp_object->pattern(), regexp_object->flags()));
}

}
