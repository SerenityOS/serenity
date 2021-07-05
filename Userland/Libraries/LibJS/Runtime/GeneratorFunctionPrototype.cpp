/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GeneratorFunctionConstructor.h>
#include <LibJS/Runtime/GeneratorFunctionPrototype.h>
#include <LibJS/Runtime/GeneratorObjectPrototype.h>

namespace JS {

GeneratorFunctionPrototype::GeneratorFunctionPrototype(GlobalObject& global_object)
    : Object(*global_object.function_prototype())
{
}

void GeneratorFunctionPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    // 27.3.3.2 %GeneratorFunction.prototype% prototype, https://tc39.es/ecma262/#sec-generatorfunction.prototype.prototype
    define_direct_property(vm.names.prototype, global_object.generator_object_prototype(), Attribute::Configurable);
    // 27.3.3.3 %GeneratorFunction.prototype% [ @@toStringTag ], https://tc39.es/ecma262/#sec-generatorfunction.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, "GeneratorFunction"), Attribute::Configurable);
}

GeneratorFunctionPrototype::~GeneratorFunctionPrototype()
{
}

}
