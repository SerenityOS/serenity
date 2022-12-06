/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/SymbolConstructor.h>

namespace JS {

SymbolConstructor::SymbolConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.Symbol.as_string(), *realm.intrinsics().function_prototype())
{
}

void SymbolConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    NativeFunction::initialize(realm);

    // 20.4.2.9 Symbol.prototype, https://tc39.es/ecma262/#sec-symbol.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().symbol_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.for_, for_, 1, attr);
    define_native_function(realm, vm.names.keyFor, key_for, 1, attr);

#define __JS_ENUMERATE(SymbolName, snake_name) \
    define_direct_property(vm.names.SymbolName, vm.well_known_symbol_##snake_name(), 0);
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 20.4.1.1 Symbol ( [ description ] ), https://tc39.es/ecma262/#sec-symbol-description
ThrowCompletionOr<Value> SymbolConstructor::call()
{
    auto& vm = this->vm();
    if (vm.argument(0).is_undefined())
        return Symbol::create(vm, {}, false);
    return Symbol::create(vm, TRY(vm.argument(0).to_string(vm)), false);
}

// 20.4.1.1 Symbol ( [ description ] ), https://tc39.es/ecma262/#sec-symbol-description
ThrowCompletionOr<Object*> SymbolConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<TypeError>(ErrorType::NotAConstructor, "Symbol");
}

// 20.4.2.2 Symbol.for ( key ), https://tc39.es/ecma262/#sec-symbol.for
JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::for_)
{
    // 1. Let stringKey be ? ToString(key).
    auto string_key = TRY(vm.argument(0).to_string(vm));

    // 2. For each element e of the GlobalSymbolRegistry List, do
    auto result = vm.global_symbol_registry().get(string_key);
    if (result.has_value()) {
        // a. If SameValue(e.[[Key]], stringKey) is true, return e.[[Symbol]].
        return result.value();
    }

    // 3. Assert: GlobalSymbolRegistry does not currently contain an entry for stringKey.
    VERIFY(!result.has_value());

    // 4. Let newSymbol be a new unique Symbol value whose [[Description]] value is stringKey.
    auto new_symbol = Symbol::create(vm, string_key, true);

    // 5. Append the Record { [[Key]]: stringKey, [[Symbol]]: newSymbol } to the GlobalSymbolRegistry List.
    vm.global_symbol_registry().set(string_key, new_symbol);

    // 6. Return newSymbol.
    return new_symbol;
}

// 20.4.2.6 Symbol.keyFor ( sym ), https://tc39.es/ecma262/#sec-symbol.keyfor
JS_DEFINE_NATIVE_FUNCTION(SymbolConstructor::key_for)
{
    auto argument = vm.argument(0);
    if (!argument.is_symbol())
        return vm.throw_completion<TypeError>(ErrorType::NotASymbol, argument.to_string_without_side_effects());

    auto& symbol = argument.as_symbol();
    if (symbol.is_global())
        return PrimitiveString::create(vm, symbol.description());

    return js_undefined();
}

}
