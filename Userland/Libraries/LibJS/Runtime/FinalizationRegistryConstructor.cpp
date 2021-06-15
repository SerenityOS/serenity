/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FinalizationRegistry.h>
#include <LibJS/Runtime/FinalizationRegistryConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

FinalizationRegistryConstructor::FinalizationRegistryConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.FinalizationRegistry, *global_object.function_prototype())
{
}

void FinalizationRegistryConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 26.2.2.1 FinalizationRegistry.prototype, https://tc39.es/ecma262/#sec-finalization-registry.prototype
    define_property(vm.names.prototype, global_object.finalization_registry_prototype(), 0);

    define_property(vm.names.length, Value(1), Attribute::Configurable);
}

FinalizationRegistryConstructor::~FinalizationRegistryConstructor()
{
}

// 26.2.1.1 FinalizationRegistry ( cleanupCallback ), https://tc39.es/ecma262/#sec-finalization-registry-cleanup-callback
Value FinalizationRegistryConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.FinalizationRegistry);
    return {};
}

// 26.2.1.1 FinalizationRegistry ( cleanupCallback ), https://tc39.es/ecma262/#sec-finalization-registry-cleanup-callback
Value FinalizationRegistryConstructor::construct(Function&)
{
    auto& vm = this->vm();
    auto cleanup_callback = vm.argument(0);
    if (!cleanup_callback.is_function()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::NotAFunction, cleanup_callback.to_string_without_side_effects());
        return {};
    }

    // FIXME: Use OrdinaryCreateFromConstructor(NewTarget, "%FinalizationRegistry.prototype%")
    return FinalizationRegistry::create(global_object(), cleanup_callback.as_function());
}

}
