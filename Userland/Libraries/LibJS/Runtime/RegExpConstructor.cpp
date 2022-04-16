/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

RegExpConstructor::RegExpConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.RegExp.as_string(), *global_object.function_prototype())
{
}

void RegExpConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 22.2.4.1 RegExp.prototype, https://tc39.es/ecma262/#sec-regexp.prototype
    define_direct_property(vm.names.prototype, global_object.regexp_prototype(), 0);

    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);

    u8 attr = Attribute::Configurable;

    // RegExp.input [[Get]] && [[Set]]
    define_native_accessor("input", input_getter, input_setter, attr);
    define_native_accessor("$_", input_getter, input_setter, attr);

    // RegExp.lastMatch [[Get]]
    define_native_accessor("lastMatch", last_match_getter, nullptr, attr);
    define_native_accessor("$&", last_match_getter, nullptr, attr);

    // RegExp.lastParen [[Get]]
    define_native_accessor("lastParen", last_paren_getter, nullptr, attr);
    define_native_accessor("$+", last_paren_getter, nullptr, attr);

    // RegExp.leftContext [[Get]]
    define_native_accessor("leftContext", left_context_getter, nullptr, attr);
    define_native_accessor("$`", left_context_getter, nullptr, attr);

    // RegExp.rightContext [[Get]]
    define_native_accessor("rightContext", right_context_getter, nullptr, attr);
    define_native_accessor("$'", right_context_getter, nullptr, attr);

    // RegExp.$1...$9 [[Get]]
    define_native_accessor("$1", $1_getter, nullptr, attr);
    define_native_accessor("$2", $2_getter, nullptr, attr);
    define_native_accessor("$3", $3_getter, nullptr, attr);
    define_native_accessor("$4", $4_getter, nullptr, attr);
    define_native_accessor("$5", $5_getter, nullptr, attr);
    define_native_accessor("$6", $6_getter, nullptr, attr);
    define_native_accessor("$7", $7_getter, nullptr, attr);
    define_native_accessor("$8", $8_getter, nullptr, attr);
    define_native_accessor("$9", $9_getter, nullptr, attr);
}

// 22.2.3.1 RegExp ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp-pattern-flags
ThrowCompletionOr<Value> RegExpConstructor::call()
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto pattern = vm.argument(0);
    auto flags = vm.argument(1);

    bool pattern_is_regexp = TRY(pattern.is_regexp(global_object));

    if (pattern_is_regexp && flags.is_undefined()) {
        auto pattern_constructor = TRY(pattern.as_object().get(vm.names.constructor));
        if (same_value(this, pattern_constructor))
            return pattern;
    }

    return TRY(construct(*this));
}

// 22.2.3.1 RegExp ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp-pattern-flags
ThrowCompletionOr<Object*> RegExpConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto pattern = vm.argument(0);
    auto flags = vm.argument(1);

    bool pattern_is_regexp = TRY(pattern.is_regexp(global_object));

    Value pattern_value;
    Value flags_value;

    if (pattern.is_object() && is<RegExpObject>(pattern.as_object())) {
        auto& regexp_pattern = static_cast<RegExpObject&>(pattern.as_object());
        pattern_value = js_string(vm, regexp_pattern.pattern());

        if (flags.is_undefined())
            flags_value = js_string(vm, regexp_pattern.flags());
        else
            flags_value = flags;
    } else if (pattern_is_regexp) {
        pattern_value = TRY(pattern.as_object().get(vm.names.source));

        if (flags.is_undefined()) {
            flags_value = TRY(pattern.as_object().get(vm.names.flags));
        } else {
            flags_value = flags;
        }
    } else {
        pattern_value = pattern;
        flags_value = flags;
    }

    return TRY(regexp_create(global_object, pattern_value, flags_value));
}

// 22.2.4.2 get RegExp [ @@species ], https://tc39.es/ecma262/#sec-get-regexp-@@species
JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::input_setter)
{
    // SetLegacyRegExpStaticProperty( C, thisValue, internalSlotName, val ).
    // TODO: Assert C is an object that has an internal slot named internalSlotName.
    // TODO: If SameValue(C, thisValue) is false, throw a TypeError exception.

    // Let strVal be ? ToString(val).
    auto val = vm.argument(0);
    auto str_val = TRY(val.to_primitive(global_object, Value::PreferredType::String));

    // Set the value of the internal slot of C named internalSlotName to strVal.
    global_object.regexp_constructor()->set_input(str_val);

    return str_val;
}

// RegExp.input / RegExp.$_ (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/input)
JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::input_getter)
{
    auto input = global_object.regexp_constructor()->m_input;
    return input.is_undefined() ? js_string(vm, "") : input;
}

// RegExp.lastMatch / RegExp.$& (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/lastMatch)
JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::last_match_getter)
{
    auto last_match = global_object.regexp_constructor()->m_last_match;
    return last_match.is_undefined() ? js_string(vm, "") : last_match;
}

// RegExp.lastParen / RegExp.$+ (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/lastParen)
JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::last_paren_getter)
{
    auto last_paren = global_object.regexp_constructor()->m_last_paren;
    return last_paren.is_undefined() ? js_string(vm, "") : last_paren;
}

// RegExp.leftContext / RegExp.$` (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/leftContex)
JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::left_context_getter)
{
    auto left_context = global_object.regexp_constructor()->m_left_context;
    return left_context.is_undefined() ? js_string(vm, "") : left_context;
}

// RegExp.rightContext / RegExp.$' (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/rightContext)
JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::right_context_getter)
{
    auto right_context = global_object.regexp_constructor()->m_right_context;
    return right_context.is_undefined() ? js_string(vm, "") : right_context;
}

// RegExp.$1...$9 (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp/n)
#define GENERATE_CAPTURED_VALUE_GETTER(index)                       \
    JS_DEFINE_NATIVE_FUNCTION(RegExpConstructor::$##index##_getter) \
    {                                                               \
        auto regexp = global_object.regexp_constructor();           \
        auto array = regexp->m_captured_values;                     \
        if (!array) {                                               \
            return js_string(vm, "");                               \
        }                                                           \
        return array->get(index);                                   \
    }

GENERATE_CAPTURED_VALUE_GETTER(1)
GENERATE_CAPTURED_VALUE_GETTER(2)
GENERATE_CAPTURED_VALUE_GETTER(3)
GENERATE_CAPTURED_VALUE_GETTER(4)
GENERATE_CAPTURED_VALUE_GETTER(5)
GENERATE_CAPTURED_VALUE_GETTER(6)
GENERATE_CAPTURED_VALUE_GETTER(7)
GENERATE_CAPTURED_VALUE_GETTER(8)
GENERATE_CAPTURED_VALUE_GETTER(9)

#undef GENERATE_CAPTURED_VALUE_GETTER

}
