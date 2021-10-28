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
    : NativeFunction(vm().names.Symbol.as_string(), *global_object.function_prototype())
{
}

void SymbolConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 20.4.2.9 Symbol.prototype, https://tc39.es/ecma262/#sec-symbol.prototype
    define_direct_property(vm.names.prototype, global_object.symbol_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.for_, for_, 1, attr);
    define_native_function(vm.names.keyFor, key_for, 1, attr);

#define __JS_ENUMERATE(SymbolName, snake_name) \
    define_direct_property(vm.names.SymbolName, vm.well_known_symbol_##snake_name(), 0);
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

SymbolConstructor::~SymbolConstructor()
{
}

// 20.4.1.1 Symbol ( [ description ] ), https://tc39.es/ecma262/#sec-symbol-description
ThrowCompletionOr<Value> SymbolConstructor::call()
{
    if (vm().argument(0).is_undefined())
        return js_symbol(heap(), {}, false);
    return js_symbol(heap(), TRY(vm().argument(0).to_string(global_object())), false);
}

// 20.4.1.1 Symbol ( [ description ] ), https://tc39.es/ecma262/#sec-symbol-description
ThrowCompletionOr<Object*> SymbolConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<TypeError>(global_object(), ErrorType::NotAConstructor, "Symbol");
}

// 20.4.2.2 Symbol.for ( key ), https://tc39.es/ecma262/#sec-symbol.for
JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::for_)
{
    auto description = TRY(vm.argument(0).to_string(global_object));
    return global_object.vm().get_global_symbol(description);
}

// 20.4.2.6 Symbol.keyFor ( sym ), https://tc39.es/ecma262/#sec-symbol.keyfor
JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::key_for)
{
    auto argument = vm.argument(0);
    if (!argument.is_symbol())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotASymbol, argument.to_string_without_side_effects());

    auto& symbol = argument.as_symbol();
    if (symbol.is_global())
        return js_string(vm, symbol.description());

    return js_undefined();
}

}
