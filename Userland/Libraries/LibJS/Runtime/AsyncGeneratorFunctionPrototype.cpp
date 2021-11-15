/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncGeneratorFunctionConstructor.h>
#include <LibJS/Runtime/AsyncGeneratorFunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AsyncGeneratorFunctionPrototype::AsyncGeneratorFunctionPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.function_prototype())
{
}

void AsyncGeneratorFunctionPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    // The constructor cannot be set at this point since it has not been initialized.

    // 27.4.3.2 AsyncGeneratorFunction.prototype.prototype, https://tc39.es/ecma262/#sec-asyncgeneratorfunction-prototype-prototype
    // FIXME: AsyncGenerator does not exist yet.
    // define_direct_property(vm.names.prototype, global_object.async_generator_prototype(), Attribute::Configurable);

    // 27.4.3.3 AsyncGeneratorFunction.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-asyncgeneratorfunction-prototype-tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.AsyncGeneratorFunction.as_string()), Attribute::Configurable);
}

}
