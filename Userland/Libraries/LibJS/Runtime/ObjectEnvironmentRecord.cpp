/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/ObjectEnvironmentRecord.h>

namespace JS {

ObjectEnvironmentRecord::ObjectEnvironmentRecord(Object& object, EnvironmentRecord* parent_scope)
    : EnvironmentRecord(parent_scope)
    , m_object(object)
{
}

void ObjectEnvironmentRecord::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_object);
}

Optional<Variable> ObjectEnvironmentRecord::get_from_environment_record(FlyString const& name) const
{
    auto value = m_object.get(name);
    if (value.is_empty())
        return {};
    return Variable { value, DeclarationKind::Var };
}

void ObjectEnvironmentRecord::put_into_environment_record(FlyString const& name, Variable variable)
{
    m_object.put(name, variable.value);
}

bool ObjectEnvironmentRecord::delete_from_environment_record(FlyString const& name)
{
    return m_object.delete_property(name);
}

}
