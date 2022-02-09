/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/MapIteratorPrototype.h>

namespace JS {

MapIteratorPrototype::MapIteratorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.iterator_prototype())
{
}

void MapIteratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_function(vm.names.next, next, 0, Attribute::Configurable | Attribute::Writable);
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Map Iterator"), Attribute::Configurable);
}

MapIteratorPrototype::~MapIteratorPrototype()
{
}

// 24.1.5.2.1 %MapIteratorPrototype%.next ( ), https://tc39.es/ecma262/#sec-%mapiteratorprototype%.next
JS_DEFINE_NATIVE_FUNCTION(MapIteratorPrototype::next)
{
    auto* map_iterator = TRY(typed_this_value(global_object));
    if (map_iterator->done())
        return create_iterator_result_object(global_object, js_undefined(), true);

    if (map_iterator->m_iterator.is_end()) {
        map_iterator->m_done = true;
        return create_iterator_result_object(global_object, js_undefined(), true);
    }

    auto iteration_kind = map_iterator->iteration_kind();

    auto entry = *map_iterator->m_iterator;
    ++map_iterator->m_iterator;
    if (iteration_kind == Object::PropertyKind::Key)
        return create_iterator_result_object(global_object, entry.key, false);
    if (iteration_kind == Object::PropertyKind::Value)
        return create_iterator_result_object(global_object, entry.value, false);

    return create_iterator_result_object(global_object, Array::create_from(global_object, { entry.key, entry.value }), false);
}

}
