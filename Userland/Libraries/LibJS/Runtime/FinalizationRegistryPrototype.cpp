/*
 * Copyright (c) 2021-2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/FinalizationRegistryPrototype.h>

namespace JS {

FinalizationRegistryPrototype::FinalizationRegistryPrototype(Realm& realm)
    : PrototypeObject(*realm.global_object().object_prototype())
{
}

void FinalizationRegistryPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Object::initialize(realm);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(realm, vm.names.cleanupSome, cleanup_some, 0, attr);
    define_native_function(realm, vm.names.register_, register_, 2, attr);
    define_native_function(realm, vm.names.unregister, unregister, 1, attr);

    // 26.2.3.4 FinalizationRegistry.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-finalization-registry.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.FinalizationRegistry.as_string()), Attribute::Configurable);
}

// @STAGE 2@ FinalizationRegistry.prototype.cleanupSome ( [ callback ] ), https://github.com/tc39/proposal-cleanup-some/blob/master/spec/finalization-registry.html
JS_DEFINE_NATIVE_FUNCTION(FinalizationRegistryPrototype::cleanup_some)
{
    auto* finalization_registry = TRY(typed_this_object(vm));

    auto callback = vm.argument(0);
    if (vm.argument_count() > 0 && !callback.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, callback.to_string_without_side_effects());

    // IMPLEMENTATION DEFINED: The specification for this function hasn't been updated to accommodate for JobCallback records.
    //                         This just follows how the constructor immediately converts the callback to a JobCallback using HostMakeJobCallback.
    TRY(finalization_registry->cleanup(callback.is_undefined() ? Optional<JobCallback> {} : vm.host_make_job_callback(callback.as_function())));

    return js_undefined();
}

// 26.2.3.2 FinalizationRegistry.prototype.register ( target, heldValue [ , unregisterToken ] ), https://tc39.es/ecma262/#sec-finalization-registry.prototype.register
JS_DEFINE_NATIVE_FUNCTION(FinalizationRegistryPrototype::register_)
{
    auto* finalization_registry = TRY(typed_this_object(vm));

    auto target = vm.argument(0);
    if (!can_be_held_weakly(target))
        return vm.throw_completion<TypeError>(ErrorType::CannotBeHeldWeakly, target.to_string_without_side_effects());

    auto held_value = vm.argument(1);
    if (same_value(target, held_value))
        return vm.throw_completion<TypeError>(ErrorType::FinalizationRegistrySameTargetAndValue);

    auto unregister_token = vm.argument(2);
    if (!can_be_held_weakly(unregister_token) && !unregister_token.is_undefined())
        return vm.throw_completion<TypeError>(ErrorType::CannotBeHeldWeakly, unregister_token.to_string_without_side_effects());

    finalization_registry->add_finalization_record(target.as_cell(), held_value, unregister_token.is_undefined() ? nullptr : &unregister_token.as_cell());

    return js_undefined();
}

// 26.2.3.3 FinalizationRegistry.prototype.unregister ( unregisterToken ), https://tc39.es/ecma262/#sec-finalization-registry.prototype.unregister
JS_DEFINE_NATIVE_FUNCTION(FinalizationRegistryPrototype::unregister)
{
    auto* finalization_registry = TRY(typed_this_object(vm));

    auto unregister_token = vm.argument(0);
    if (!can_be_held_weakly(unregister_token))
        return vm.throw_completion<TypeError>(ErrorType::CannotBeHeldWeakly, unregister_token.to_string_without_side_effects());

    return Value(finalization_registry->remove_by_token(unregister_token.as_cell()));
}

}
