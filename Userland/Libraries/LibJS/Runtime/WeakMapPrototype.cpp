/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibJS/Runtime/WeakMapPrototype.h>

namespace JS {

WeakMapPrototype::WeakMapPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void WeakMapPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    define_native_function(vm.names.delete_, delete_, 1, attr);
    define_native_function(vm.names.get, get, 1, attr);
    define_native_function(vm.names.has, has, 1, attr);
    define_native_function(vm.names.set, set, 2, attr);

    // 24.3.3.6 WeakMap.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-weakmap.prototype-@@tostringtag
    define_property(vm.well_known_symbol_to_string_tag(), js_string(vm.heap(), vm.names.WeakMap.as_string()), Attribute::Configurable);
}

WeakMapPrototype::~WeakMapPrototype()
{
}

WeakMap* WeakMapPrototype::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<WeakMap>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "WeakMap");
        return nullptr;
    }
    return static_cast<WeakMap*>(this_object);
}

// 24.3.3.2 WeakMap.prototype.delete ( key ), https://tc39.es/ecma262/#sec-weakmap.prototype.delete
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::delete_)
{
    auto* weak_map = typed_this(vm, global_object);
    if (!weak_map)
        return {};
    auto value = vm.argument(0);
    if (!value.is_object())
        return Value(false);
    return Value(weak_map->values().remove(&value.as_object()));
}

// 24.3.3.3 WeakMap.prototype.get ( key ), https://tc39.es/ecma262/#sec-weakmap.prototype.get
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::get)
{
    auto* weak_map = typed_this(vm, global_object);
    if (!weak_map)
        return {};
    auto value = vm.argument(0);
    if (!value.is_object())
        return js_undefined();
    auto& values = weak_map->values();
    auto result = values.find(&value.as_object());
    if (result == values.end())
        return js_undefined();
    return result->value;
}

// 24.3.3.4 WeakMap.prototype.has ( key ), https://tc39.es/ecma262/#sec-weakmap.prototype.has
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::has)
{
    auto* weak_map = typed_this(vm, global_object);
    if (!weak_map)
        return {};
    auto value = vm.argument(0);
    if (!value.is_object())
        return Value(false);
    auto& values = weak_map->values();
    return Value(values.find(&value.as_object()) != values.end());
}

// 24.3.3.5 WeakMap.prototype.set ( key, value ), https://tc39.es/ecma262/#sec-weakmap.prototype.set
JS_DEFINE_NATIVE_FUNCTION(WeakMapPrototype::set)
{
    auto* weak_map = typed_this(vm, global_object);
    if (!weak_map)
        return {};
    auto value = vm.argument(0);
    if (!value.is_object()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, value.to_string_without_side_effects());
        return {};
    }
    weak_map->values().set(&value.as_object(), vm.argument(1));
    return weak_map;
}

}
