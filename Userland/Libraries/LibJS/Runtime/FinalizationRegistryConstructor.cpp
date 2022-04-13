/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/FinalizationRegistryConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/JobCallback.h>

namespace JS {

FinalizationRegistryConstructor::FinalizationRegistryConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.FinalizationRegistry.as_string(), *global_object.function_prototype())
{
}

void FinalizationRegistryConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 26.2.2.1 FinalizationRegistry.prototype, https://tc39.es/ecma262/#sec-finalization-registry.prototype
    define_direct_property(vm.names.prototype, global_object.finalization_registry_prototype(), 0);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 26.2.1.1 FinalizationRegistry ( cleanupCallback ), https://tc39.es/ecma262/#sec-finalization-registry-cleanup-callback
ThrowCompletionOr<Value> FinalizationRegistryConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.FinalizationRegistry);
}

// 26.2.1.1 FinalizationRegistry ( cleanupCallback ), https://tc39.es/ecma262/#sec-finalization-registry-cleanup-callback
ThrowCompletionOr<Object*> FinalizationRegistryConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // NOTE: Step 1 is implemented in FinalizationRegistryConstructor::call()

    // 2. If IsCallable(cleanupCallback) is false, throw a TypeError exception.
    auto cleanup_callback = vm.argument(0);
    if (!cleanup_callback.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, cleanup_callback.to_string_without_side_effects());

    // 3. Let finalizationRegistry be ? OrdinaryCreateFromConstructor(NewTarget, "%FinalizationRegistry.prototype%", « [[Realm]], [[CleanupCallback]], [[Cells]] »).
    // 4. Let fn be the active function object.
    // NOTE: This is not necessary, the active function object is `this`.
    // 5. Set finalizationRegistry.[[Realm]] to fn.[[Realm]].
    // 6. Set finalizationRegistry.[[CleanupCallback]] to HostMakeJobCallback(cleanupCallback).
    // 7. Set finalizationRegistry.[[Cells]] to a new empty List.
    // NOTE: This is done inside FinalizationRegistry instead of here.
    // 8. Return finalizationRegistry.
    return TRY(ordinary_create_from_constructor<FinalizationRegistry>(global_object, new_target, &GlobalObject::finalization_registry_prototype, *realm(), vm.host_make_job_callback(cleanup_callback.as_function())));
}

}
