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
#include <LibJS/Runtime/SetIteratorPrototype.h>

namespace JS {

SetIteratorPrototype::SetIteratorPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.iterator_prototype())
{
}

void SetIteratorPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);

    define_native_function(vm.names.next, next, 0, Attribute::Configurable | Attribute::Writable);

    // 24.2.5.2.2 %SetIteratorPrototype% [ @@toStringTag ], https://tc39.es/ecma262/#sec-%setiteratorprototype%-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(global_object.heap(), "Set Iterator"), Attribute::Configurable);
}

SetIteratorPrototype::~SetIteratorPrototype()
{
}

// 24.2.5.2.1 %SetIteratorPrototype%.next ( ), https://tc39.es/ecma262/#sec-%setiteratorprototype%.next
JS_DEFINE_NATIVE_FUNCTION(SetIteratorPrototype::next)
{
    auto* set_iterator = TRY(typed_this_value(global_object));
    if (set_iterator->done())
        return create_iterator_result_object(global_object, js_undefined(), true);

    auto& set = set_iterator->set();
    if (set_iterator->m_iterator == set.values().end()) {
        set_iterator->m_done = true;
        return create_iterator_result_object(global_object, js_undefined(), true);
    }

    auto iteration_kind = set_iterator->iteration_kind();
    VERIFY(iteration_kind != Object::PropertyKind::Key);

    auto value = *set_iterator->m_iterator;
    ++set_iterator->m_iterator;
    if (iteration_kind == Object::PropertyKind::Value)
        return create_iterator_result_object(global_object, value, false);

    return create_iterator_result_object(global_object, Array::create_from(global_object, { value, value }), false);
}

}
