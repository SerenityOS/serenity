/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpObject.h>

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
}

RegExpConstructor::~RegExpConstructor()
{
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

}
