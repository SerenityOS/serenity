/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Set.h>

namespace JS {

Set* Set::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<Set>(global_object, *global_object.set_prototype());
}

Set::Set(Object& prototype)
    : Object(prototype)
{
}

Set::~Set()
{
}

Set* Set::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!is<Set>(this_object)) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Set");
        return nullptr;
    }
    return static_cast<Set*>(this_object);
}

void Set::visit_edges(Cell::Visitor& visitor)
{
    Object::visit_edges(visitor);
    for (auto& value : m_values)
        visitor.visit(value);
}

}
