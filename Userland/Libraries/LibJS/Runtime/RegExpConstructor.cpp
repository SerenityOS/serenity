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

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);

    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);
}

RegExpConstructor::~RegExpConstructor()
{
}

// 22.2.3.1 RegExp ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp-pattern-flags
Value RegExpConstructor::call()
{
    return construct(*this);
}

// 22.2.3.1 RegExp ( pattern, flags ), https://tc39.es/ecma262/#sec-regexp-pattern-flags
Value RegExpConstructor::construct(FunctionObject&)
{
    auto& vm = this->vm();
    // FIXME: This is non-conforming
    return regexp_create(global_object(), vm.argument(0), vm.argument(1));
}

// 22.2.4.2 get RegExp [ @@species ], https://tc39.es/ecma262/#sec-get-regexp-@@species
JS_DEFINE_NATIVE_GETTER(RegExpConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
