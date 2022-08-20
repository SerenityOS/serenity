/*
 * Copyright (c) 2021-2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/WeakMapPrototype.h>

namespace JS {

WeakMapPrototype::WeakMapPrototype(Realm& realm)
    : PrototypeObject(*realm.global_object().object_prototype())
{
}

void WeakMapPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Object::initialize(realm);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.delete_, delete_, 1, attr);
    define_native_function(vm.names.get, get, 1, attr);
    define_native_function(vm.names.has, has, 1, attr);
    define_native_function(vm.names.set, set, 2, attr);

    // 24.3.3.6 WeakMap.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-weakmap.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.WeakMap.as_string()), Attribute::Configurable);
}

// 24.3.3.2 WeakMap.prototype.delete ( key ), https://tc39.es/ecma262/#sec-weakmap.prototype.delete
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::delete_)
{
    auto* weak_map = TRY(typed_this_object(vm));
    auto value = vm.argument(0);
    if (!can_be_held_weakly(value))
        return Value(false);
    return Value(weak_map->values().remove(&value.as_cell()));
}

// 24.3.3.3 WeakMap.prototype.get ( key ), https://tc39.es/ecma262/#sec-weakmap.prototype.get
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::get)
{
    auto* weak_map = TRY(typed_this_object(vm));
    auto value = vm.argument(0);
    if (!can_be_held_weakly(value))
        return js_undefined();
    auto& values = weak_map->values();
    auto result = values.find(&value.as_cell());
    if (result == values.end())
        return js_undefined();
    return result->value;
}

// 24.3.3.4 WeakMap.prototype.has ( key ), https://tc39.es/ecma262/#sec-weakmap.prototype.has
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::has)
{
    auto* weak_map = TRY(typed_this_object(vm));
    auto value = vm.argument(0);
    if (!can_be_held_weakly(value))
        return Value(false);
    auto& values = weak_map->values();
    return Value(values.find(&value.as_cell()) != values.end());
}

// 24.3.3.5 WeakMap.prototype.set ( key, value ), https://tc39.es/ecma262/#sec-weakmap.prototype.set
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::set)
{
    auto* weak_map = TRY(typed_this_object(vm));
    auto value = vm.argument(0);
    if (!can_be_held_weakly(value))
        return vm.throw_completion<TypeError>(ErrorType::CannotBeHeldWeakly, value.to_string_without_side_effects());
    weak_map->values().set(&value.as_cell(), vm.argument(1));
    return weak_map;
}

}
