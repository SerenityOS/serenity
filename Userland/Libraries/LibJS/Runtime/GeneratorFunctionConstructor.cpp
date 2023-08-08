/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/GeneratorFunctionConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

GeneratorFunctionConstructor::GeneratorFunctionConstructor(Realm& realm)
    : NativeFunction(static_cast<Object&>(realm.intrinsics().function_constructor()))
{
}

void GeneratorFunctionConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Base::initialize(realm);

    // 27.3.2.1 GeneratorFunction.length, https://tc39.es/ecma262/#sec-generatorfunction.length
    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
    // 27.3.2.2 GeneratorFunction.prototype, https://tc39.es/ecma262/#sec-generatorfunction.length
    define_direct_property(vm.names.prototype, realm.intrinsics().generator_function_prototype(), 0);
}

// 27.3.1.1 GeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-generatorfunction
ThrowCompletionOr<Value> GeneratorFunctionConstructor::call()
{
    return TRY(construct(*this));
}

// 27.3.1.1 GeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-generatorfunction
ThrowCompletionOr<NonnullGCPtr<Object>> GeneratorFunctionConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    // 1. Let C be the active function object.
    auto* constructor = vm.active_function_object();

    // 2. Let args be the argumentsList that was passed to this function by [[Call]] or [[Construct]].
    auto& args = vm.running_execution_context().arguments;

    // 3. Return ? CreateDynamicFunction(C, NewTarget, generator, args).
    return *TRY(FunctionConstructor::create_dynamic_function(vm, *constructor, &new_target, FunctionKind::Generator, args));
}

}
