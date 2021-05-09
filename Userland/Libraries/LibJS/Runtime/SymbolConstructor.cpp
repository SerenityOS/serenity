/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/SymbolConstructor.h>

namespace JS {

SymbolConstructor::SymbolConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Symbol, *global_object.function_prototype())
{
}

void SymbolConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, global_object.symbol_prototype(), 0);
    define_property(vm.names.length, Value(0), Attribute::Configurable);

    define_native_function(vm.names.for_, for_, 1, Attribute::Writable | Attribute::Configurable);
    define_native_function(vm.names.keyFor, key_for, 1, Attribute::Writable | Attribute::Configurable);

#define __JS_ENUMERATE(SymbolName, snake_name) \
    define_property(vm.names.SymbolName, vm.well_known_symbol_##snake_name(), 0);
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
}

SymbolConstructor::~SymbolConstructor()
{
}

Value SymbolConstructor::call()
{
    if (!vm().argument_count())
        return js_symbol(heap(), "", false);
    return js_symbol(heap(), vm().argument(0).to_string(global_object()), false);
}

Value SymbolConstructor::construct(Function&)
{
    vm().throw_exception<TypeError>(global_object(), ErrorType::NotAConstructor, "Symbol");
    return {};
}

JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::for_)
{
    String description;
    if (!vm.argument_count()) {
        description = "undefined";
    } else {
        description = vm.argument(0).to_string(global_object);
    }

    return global_object.vm().get_global_symbol(description);
}

JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::key_for)
{
    auto argument = vm.argument(0);
    if (!argument.is_symbol()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotASymbol, argument.to_string_without_side_effects());
        return {};
    }

    auto& symbol = argument.as_symbol();
    if (symbol.is_global())
        return js_string(vm, symbol.description());

    return js_undefined();
}

}
