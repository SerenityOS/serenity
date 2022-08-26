/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/TypeCasts.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/MapIterator.h>
#include <LibJS/Runtime/MapPrototype.h>

namespace JS {

MapPrototype::MapPrototype(Realm& realm)
    : PrototypeObject(*realm.intrinsics().object_prototype())
{
}

void MapPrototype::initialize(Realm& realm)
{
    auto& vm = this->vm();
    Object::initialize(realm);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(realm, vm.names.clear, clear, 0, attr);
    define_native_function(realm, vm.names.delete_, delete_, 1, attr);
    define_native_function(realm, vm.names.entries, entries, 0, attr);
    define_native_function(realm, vm.names.forEach, for_each, 1, attr);
    define_native_function(realm, vm.names.get, get, 1, attr);
    define_native_function(realm, vm.names.has, has, 1, attr);
    define_native_function(realm, vm.names.keys, keys, 0, attr);
    define_native_function(realm, vm.names.set, set, 2, attr);
    define_native_function(realm, vm.names.values, values, 0, attr);

    define_native_accessor(realm, vm.names.size, size_getter, {}, Attribute::Configurable);

    define_direct_property(*vm.well_known_symbol_iterator(), get_without_side_effects(vm.names.entries), attr);
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.Map.as_string()), Attribute::Configurable);
}

// 24.1.3.1 Map.prototype.clear ( ), https://tc39.es/ecma262/#sec-map.prototype.clear
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::clear)
{
    auto* map = TRY(typed_this_object(vm));
    map->map_clear();
    return js_undefined();
}

// 24.1.3.3 Map.prototype.delete ( key ), https://tc39.es/ecma262/#sec-map.prototype.delete
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::delete_)
{
    auto* map = TRY(typed_this_object(vm));
    return Value(map->map_remove(vm.argument(0)));
}

// 24.1.3.4 Map.prototype.entries ( ), https://tc39.es/ecma262/#sec-map.prototype.entries
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::entries)
{
    auto& realm = *vm.current_realm();

    auto* map = TRY(typed_this_object(vm));

    return MapIterator::create(realm, *map, Object::PropertyKind::KeyAndValue);
}

// 24.1.3.5 Map.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-map.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::for_each)
{
    auto* map = TRY(typed_this_object(vm));
    if (!vm.argument(0).is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, vm.argument(0).to_string_without_side_effects());
    auto this_value = vm.this_value();
    for (auto& entry : *map)
        TRY(call(vm, vm.argument(0).as_function(), vm.argument(1), entry.value, entry.key, this_value));
    return js_undefined();
}

// 24.1.3.6 Map.prototype.get ( key ), https://tc39.es/ecma262/#sec-map.prototype.get
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::get)
{
    auto* map = TRY(typed_this_object(vm));
    auto result = map->map_get(vm.argument(0));
    if (!result.has_value())
        return js_undefined();
    return result.value();
}

// 24.1.3.7 Map.prototype.has ( key ), https://tc39.es/ecma262/#sec-map.prototype.has
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::has)
{
    auto* map = TRY(typed_this_object(vm));
    return map->map_has(vm.argument(0));
}

// 24.1.3.8 Map.prototype.keys ( ), https://tc39.es/ecma262/#sec-map.prototype.keys
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::keys)
{
    auto& realm = *vm.current_realm();

    auto* map = TRY(typed_this_object(vm));

    return MapIterator::create(realm, *map, Object::PropertyKind::Key);
}

// 24.1.3.9 Map.prototype.set ( key, value ), https://tc39.es/ecma262/#sec-map.prototype.set
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::set)
{
    auto* map = TRY(typed_this_object(vm));
    auto key = vm.argument(0);
    if (key.is_negative_zero())
        key = Value(0);
    map->map_set(key, vm.argument(1));
    return map;
}

// 24.1.3.11 Map.prototype.values ( ), https://tc39.es/ecma262/#sec-map.prototype.values
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::values)
{
    auto& realm = *vm.current_realm();

    auto* map = TRY(typed_this_object(vm));

    return MapIterator::create(realm, *map, Object::PropertyKind::Value);
}

// 24.1.3.10 get Map.prototype.size, https://tc39.es/ecma262/#sec-get-map.prototype.size
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::size_getter)
{
    auto* map = TRY(typed_this_object(vm));
    return Value(map->map_size());
}

}
