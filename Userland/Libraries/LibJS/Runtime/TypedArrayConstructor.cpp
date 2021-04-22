/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

TypedArrayConstructor::TypedArrayConstructor(const FlyString& name, Object& prototype)
    : NativeFunction(name, prototype)
{
}

TypedArrayConstructor::TypedArrayConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.TypedArray, *global_object.function_prototype())
{
}

void TypedArrayConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, global_object.typed_array_prototype(), 0);
    define_property(vm.names.length, Value(0), Attribute::Configurable);
}

TypedArrayConstructor::~TypedArrayConstructor()
{
}

Value TypedArrayConstructor::call()
{
    return construct(*this);
}

Value TypedArrayConstructor::construct(Function&)
{
    vm().throw_exception<TypeError>(global_object(), ErrorType::ClassIsAbstract, "TypedArray");
    return {};
}

}
