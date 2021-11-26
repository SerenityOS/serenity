/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/WeakRef.h>
#include <LibJS/Runtime/WeakRefConstructor.h>

namespace JS {

WeakRefConstructor::WeakRefConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.WeakRef.as_string(), *global_object.function_prototype())
{
}

void WeakRefConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 26.1.2.1 WeakRef.prototype, https://tc39.es/ecma262/#sec-weak-ref.prototype
    define_direct_property(vm.names.prototype, global_object.weak_ref_prototype(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

WeakRefConstructor::~WeakRefConstructor()
{
}

// 26.1.1.1 WeakRef ( target ), https://tc39.es/ecma262/#sec-weak-ref-target
ThrowCompletionOr<Value> WeakRefConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.WeakRef);
}

// 26.1.1.1 WeakRef ( target ), https://tc39.es/ecma262/#sec-weak-ref-target
ThrowCompletionOr<Object*> WeakRefConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto target = vm.argument(0);
    if (!target.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, target.to_string_without_side_effects());
    return TRY(ordinary_create_from_constructor<WeakRef>(global_object, new_target, &GlobalObject::weak_ref_prototype, &target.as_object()));
}

}
