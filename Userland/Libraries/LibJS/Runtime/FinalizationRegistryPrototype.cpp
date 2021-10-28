/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/FinalizationRegistryPrototype.h>

namespace JS {

FinalizationRegistryPrototype::FinalizationRegistryPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void FinalizationRegistryPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.cleanupSome, cleanup_some, 0, attr);
    define_native_function(vm.names.register_, register_, 2, attr);
    define_native_function(vm.names.unregister, unregister, 1, attr);

    // 26.2.3.4 FinalizationRegistry.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-finalization-registry.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.FinalizationRegistry.as_string()), Attribute::Configurable);
}

FinalizationRegistryPrototype::~FinalizationRegistryPrototype()
{
}

// @STAGE 2@ FinalizationRegistry.prototype.cleanupSome ( [ callback ] ), https://github.com/tc39/proposal-cleanup-some/blob/master/spec/finalization-registry.html
JS_DEFINE_NATIVE_FUNCTION(FinalizationRegistryPrototype::cleanup_some)
{
    auto* finalization_registry = TRY(typed_this_object(global_object));

    auto callback = vm.argument(0);
    if (vm.argument_count() > 0 && !callback.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());

    finalization_registry->cleanup(callback.is_undefined() ? nullptr : &callback.as_function());

    return js_undefined();
}

// 26.2.3.2 FinalizationRegistry.prototype.register ( target, heldValue [ , unregisterToken ] ), https://tc39.es/ecma262/#sec-finalization-registry.prototype.register
JS_DEFINE_NATIVE_FUNCTION(FinalizationRegistryPrototype::register_)
{
    auto* finalization_registry = TRY(typed_this_object(global_object));

    auto target = vm.argument(0);
    if (!target.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, target.to_string_without_side_effects());

    auto held_value = vm.argument(1);
    if (same_value(target, held_value))
        return vm.throw_completion<TypeError>(global_object, ErrorType::FinalizationRegistrySameTargetAndValue);

    auto unregister_token = vm.argument(2);
    if (!unregister_token.is_object() && !unregister_token.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, unregister_token.to_string_without_side_effects());

    finalization_registry->add_finalization_record(target.as_cell(), held_value, unregister_token.is_undefined() ? nullptr : &unregister_token.as_object());

    return js_undefined();
}

// 26.2.3.3 FinalizationRegistry.prototype.unregister ( unregisterToken ), https://tc39.es/ecma262/#sec-finalization-registry.prototype.unregister
JS_DEFINE_NATIVE_FUNCTION(FinalizationRegistryPrototype::unregister)
{
    auto* finalization_registry = TRY(typed_this_object(global_object));

    auto unregister_token = vm.argument(0);
    if (!unregister_token.is_object())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, unregister_token.to_string_without_side_effects());

    return Value(finalization_registry->remove_by_token(unregister_token.as_object()));
}

}
