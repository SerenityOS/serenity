/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AsyncFunctionPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

AsyncFunctionPrototype::AsyncFunctionPrototype(GlobalObject& global_object)
    : Object(*global_object.function_prototype())
{
}

void AsyncFunctionPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    // 27.7.3.2 AsyncFunction.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-async-function-prototype-properties-toStringTag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.AsyncFunction.as_string()), Attribute::Configurable);
}

}
