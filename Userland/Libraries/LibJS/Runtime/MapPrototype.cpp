/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
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

    define_native_accessor(vm.names.size, size_getter, {}, Attribute::Configurable);

    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), vm.names.Map), Attribute::Configurable);
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

// 24.1.3.10 get Map.prototype.size, https://tc39.es/ecma262/#sec-get-map.prototype.size
JS_DEFINE_NATIVE_GETTER(MapPrototype::size_getter)
{
    auto* map = typed_this(vm, global_object);
    if (!map)
        return {};
    return Value(map->entries().size());
}

}
