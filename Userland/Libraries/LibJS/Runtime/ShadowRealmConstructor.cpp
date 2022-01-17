/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ShadowRealm.h>
#include <LibJS/Runtime/ShadowRealmConstructor.h>

namespace JS {

// 3.2 The ShadowRealm Constructor, https://tc39.es/proposal-shadowrealm/#sec-shadowrealm-constructor
ShadowRealmConstructor::ShadowRealmConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.ShadowRealm.as_string(), *global_object.function_prototype())
{
}

void ShadowRealmConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 3.3.1 ShadowRealm.prototype, https://tc39.es/proposal-shadowrealm/#sec-shadowrealm.prototype
    define_direct_property(vm.names.prototype, global_object.shadow_realm_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 3.2.1 ShadowRealm ( ), https://tc39.es/proposal-shadowrealm/#sec-shadowrealm
ThrowCompletionOr<Value> ShadowRealmConstructor::call()
{
    auto& vm = this->vm();

    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.ShadowRealm);
}

// 3.2.1 ShadowRealm ( ), https://tc39.es/proposal-shadowrealm/#sec-shadowrealm
ThrowCompletionOr<Object*> ShadowRealmConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 3. Let realmRec be CreateRealm().
    auto* realm = Realm::create(vm);

    // 5. Let context be a new execution context.
    auto context = ExecutionContext { vm.heap() };

    // 6. Set the Function of context to null.
    context.function = nullptr;

    // 7. Set the Realm of context to realmRec.
    context.realm = realm;

    // 8. Set the ScriptOrModule of context to null.
    // Note: This is already the default value.

    // 2. Let O be ? OrdinaryCreateFromConstructor(NewTarget, "%ShadowRealm.prototype%", « [[ShadowRealm]], [[ExecutionContext]] »).
    // 4. Set O.[[ShadowRealm]] to realmRec.
    // 9. Set O.[[ExecutionContext]] to context.
    auto* object = TRY(ordinary_create_from_constructor<ShadowRealm>(global_object, new_target, &GlobalObject::shadow_realm_prototype, *realm, move(context)));

    // 10. Perform ? SetRealmGlobalObject(realmRec, undefined, undefined).
    auto* new_global_object = vm.heap().allocate_without_global_object<GlobalObject>();
    new_global_object->initialize_global_object();
    realm->set_global_object(*new_global_object, nullptr);

    // TODO: I don't think we should have these exactly like this, that doesn't work well with how
    //       we create global objects. Still, it should be possible to make a ShadowRealm with a
    //       non-LibJS GlobalObject somehow.
    // 11. Perform ? SetDefaultGlobalBindings(O.[[ShadowRealm]]).
    // 12. Perform ? HostInitializeShadowRealm(O.[[ShadowRealm]]).

    // 13. Return O.
    return object;
}

}
