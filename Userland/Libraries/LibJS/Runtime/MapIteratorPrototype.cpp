/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/MapIterator.h>
#include <LibJS/Runtime/MapIteratorPrototype.h>

namespace JS {

MapIteratorPrototype::MapIteratorPrototype(GlobalObject& global_object)
    : Object(*global_object.iterator_prototype())
{
}

void MapIteratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_function(vm.names.next, next, 0, Attribute::Configurable | Attribute::Writable);
    define_property(vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Map Iterator"), Attribute::Configurable);
}

MapIteratorPrototype::~MapIteratorPrototype()
{
}

// 24.1.5.2.1 %MapIteratorPrototype%.next ( ), https://tc39.es/ecma262/#sec-%mapiteratorprototype%.next
JS_DEFINE_NATIVE_FUNCTION(MapIteratorPrototype::next)
{
    auto this_value = vm.this_value(global_object);
    if (!this_value.is_object() || !is<MapIterator>(this_value.as_object())) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Map Iterator");
        return {};
    }

    auto& map_iterator = static_cast<MapIterator&>(this_value.as_object());
    if (map_iterator.done())
        return create_iterator_result_object(global_object, js_undefined(), true);

    auto& map = map_iterator.map();
    if (map_iterator.m_iterator == map.entries().end()) {
        map_iterator.m_done = true;
        return create_iterator_result_object(global_object, js_undefined(), true);
    }

    auto iteration_kind = map_iterator.iteration_kind();

    auto entry = *map_iterator.m_iterator;
    ++map_iterator.m_iterator;
    if (iteration_kind == Object::PropertyKind::Key)
        return create_iterator_result_object(global_object, entry.key, false);
    else if (iteration_kind == Object::PropertyKind::Value)
        return create_iterator_result_object(global_object, entry.value, false);

    auto* entry_array = Array::create(global_object);
    entry_array->define_property(0, entry.key);
    entry_array->define_property(1, entry.value);
    return create_iterator_result_object(global_object, entry_array, false);
}

}
