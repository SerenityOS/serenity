/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/BooleanConstructor.h>
#include <LibJS/Runtime/BooleanObject.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

BooleanConstructor::BooleanConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.Boolean.as_string(), *realm.intrinsics().function_prototype())
{
}

void BooleanConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    NativeFunction::initialize(realm);

    // 20.3.2.1 Boolean.prototype, https://tc39.es/ecma262/#sec-boolean.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().boolean_prototype(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 20.3.1.1 Boolean ( value ), https://tc39.es/ecma262/#sec-boolean-constructor-boolean-value
ThrowCompletionOr<Value> BooleanConstructor::call()
{
    auto& vm = this->vm();

    auto b = vm.argument(0).to_boolean();
    return Value(b);
}

// 20.3.1.1 Boolean ( value ), https://tc39.es/ecma262/#sec-boolean-constructor-boolean-value
ThrowCompletionOr<Object*> BooleanConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto b = vm.argument(0).to_boolean();
    return TRY(ordinary_create_from_constructor<BooleanObject>(vm, new_target, &Intrinsics::boolean_prototype, b));
}

}
