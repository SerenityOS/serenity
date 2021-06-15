/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/FunctionConstructor.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GeneratorFunctionConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

GeneratorFunctionConstructor::GeneratorFunctionConstructor(GlobalObject& global_object)
    : NativeFunction(*static_cast<Object*>(global_object.function_constructor()))
{
}

void GeneratorFunctionConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 27.3.2.1 GeneratorFunction.length, https://tc39.es/ecma262/#sec-generatorfunction.length
    define_property(vm.names.length, Value(1), Attribute::Configurable);
    // 27.3.2.2 GeneratorFunction.prototype, https://tc39.es/ecma262/#sec-generatorfunction.length
    define_property(vm.names.prototype, global_object.generator_function_prototype(), 0);
}

GeneratorFunctionConstructor::~GeneratorFunctionConstructor()
{
}

// 27.3.1.1 GeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-generatorfunction
Value GeneratorFunctionConstructor::call()
{
    return construct(*this);
}

// 27.3.1.1 GeneratorFunction ( p1, p2, … , pn, body ), https://tc39.es/ecma262/#sec-generatorfunction
Value GeneratorFunctionConstructor::construct(Function&)
{
    TODO();
}

}
