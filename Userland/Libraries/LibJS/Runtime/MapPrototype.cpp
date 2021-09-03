/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/HashMap.h>
#include <YAK/TypeCasts.h>
#include <LibJS/Runtime/MapIterator.h>
#include <LibJS/Runtime/MapPrototype.h>

namespace JS {

MapPrototype::MapPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void MapPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.clear, clear, 0, attr);
    define_native_function(vm.names.delete_, delete_, 1, attr);
    define_native_function(vm.names.entries, entries, 0, attr);
    define_native_function(vm.names.forEach, for_each, 1, attr);
    define_native_function(vm.names.get, get, 1, attr);
    define_native_function(vm.names.has, has, 1, attr);
    define_native_function(vm.names.keys, keys, 0, attr);
    define_native_function(vm.names.set, set, 2, attr);
    define_native_function(vm.names.values, values, 0, attr);

    define_native_accessor(vm.names.size, size_getter, {}, Attribute::Configurable);

    define_direct_property(*vm.well_known_symbol_iterator(), Object::get(vm.names.entries), attr);
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.Map.as_string()), Attribute::Configurable);
}

MapPrototype::~MapPrototype()
{
}

Map* MapPrototype::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<Map>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Map");
        return nullptr;
    }
    return static_cast<Map*>(this_object);
}

// 24.1.3.1 Map.prototype.clear ( ), https://tc39.es/ecma262/#sec-map.prototype.clear
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::clear)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    map->entries().clear();
    return js_undefined();
}

// 24.1.3.3 Map.prototype.delete ( key ), https://tc39.es/ecma262/#sec-map.prototype.delete
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::delete_)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    return Value(map->entries().remove(vm.argument(0)));
}

// 24.1.3.4 Map.prototype.entries ( ), https://tc39.es/ecma262/#sec-map.prototype.entries
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::entries)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};

    return MapIterator::create(global_object, *map, Object::PropertyKind::KeyAndValue);
}

// 24.1.3.5 Map.prototype.forEach ( callbackfn [ , thisArg ] ), https://tc39.es/ecma262/#sec-map.prototype.foreach
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::for_each)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    if (!vm.argument(0).is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, vm.argument(0).to_string_without_side_effects());
        return {};
    }
    auto this_value = vm.this_value(global_object);
    for (auto& entry : map->entries()) {
        (void)vm.call(vm.argument(0).as_function(), vm.argument(1), entry.value, entry.key, this_value);
        if (vm.exception())
            return {};
    }
    return js_undefined();
}

// 24.1.3.6 Map.prototype.get ( key ), https://tc39.es/ecma262/#sec-map.prototype.get
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::get)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    auto result = map->entries().get(vm.argument(0));
    if (!result.has_value())
        return js_undefined();
    return result.value();
}

// 24.1.3.7 Map.prototype.has ( key ), https://tc39.es/ecma262/#sec-map.prototype.has
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::has)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    auto& entries = map->entries();
    return Value(entries.find(vm.argument(0)) != entries.end());
}

// 24.1.3.8 Map.prototype.keys ( ), https://tc39.es/ecma262/#sec-map.prototype.keys
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::keys)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};

    return MapIterator::create(global_object, *map, Object::PropertyKind::Key);
}

// 24.1.3.9 Map.prototype.set ( key, value ), https://tc39.es/ecma262/#sec-map.prototype.set
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::set)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    auto key = vm.argument(0);
    if (key.is_negative_zero())
        key = Value(0);
    map->entries().set(key, vm.argument(1));
    return map;
}

// 24.1.3.11 Map.prototype.values ( ), https://tc39.es/ecma262/#sec-map.prototype.values
JS_DEFINE_NATIVE_FUNCTION(MapPrototype::values)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};

    return MapIterator::create(global_object, *map, Object::PropertyKind::Value);
}

// 24.1.3.10 get Map.prototype.size, https://tc39.es/ecma262/#sec-get-map.prototype.size
JS_DEFINE_NATIVE_GETTER(MapPrototype::size_getter)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    return Value(map->entries().size());
}

}
