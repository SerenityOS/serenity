/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/BooleanConstructor.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/BooleanPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BooleanConstructor::BooleanConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Boolean, *global_object.function_prototype())
{
}

void BooleanConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, Value(global_object.boolean_prototype()), 0);
    define_property(vm.names.length, Value(1), Attribute::Configurable);
}

BooleanConstructor::~BooleanConstructor()
{
}

Value BooleanConstructor::call()
{
    return Value(vm().argument(0).to_boolean());
}

Value BooleanConstructor::construct(Function&)
{
    return BooleanObject::create(global_object(), vm().argument(0).to_boolean());
}

}
