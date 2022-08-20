/*
 * Copyright (c) 2021-2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/WeakRefPrototype.h>

namespace JS {

WeakRefPrototype::WeakRefPrototype(Realm& realm)
    : PrototypeObject(*realm.global_object().object_prototype())
{
}

void WeakRefPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Object::initialize(realm);

    define_native_function(vm.names.deref, deref, 0, Attribute::Writable | Attribute::Configurable);

    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.WeakRef.as_string()), Attribute::Configurable);
}

// 26.1.3.2 WeakRef.prototype.deref ( ), https://tc39.es/ecma262/#sec-weak-ref.prototype.deref
JS_DEFINE_NATIVE_FUNCTION(WeakRefPrototype::deref)
{
    auto* weak_ref = TRY(typed_this_object(vm));

    weak_ref->update_execution_generation();
    return weak_ref->value().visit(
        [](Empty) -> Value { return js_undefined(); },
        [](auto* value) -> Value { return value; });
}

}
