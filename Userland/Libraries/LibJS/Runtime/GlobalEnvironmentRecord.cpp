/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/DeclarativeEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ObjectEnvironmentRecord.h>

namespace JS {

GlobalEnvironmentRecord::GlobalEnvironmentRecord(GlobalObject& global_object)
    : EnvironmentRecord(nullptr)
    , m_global_object(global_object)
{
    m_object_record = global_object.heap().allocate<ObjectEnvironmentRecord>(global_object, global_object, nullptr);
    m_declarative_record = global_object.heap().allocate<DeclarativeEnvironmentRecord>(global_object);
}

void GlobalEnvironmentRecord::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_object_record);
    visitor.visit(m_declarative_record);
}

Optional<Variable> GlobalEnvironmentRecord::get_from_environment_record(FlyString const& name) const
{
    // FIXME: This should be a "composite" of the object record and the declarative record.
    return m_object_record->get_from_environment_record(name);
}

void GlobalEnvironmentRecord::put_into_environment_record(FlyString const& name, Variable variable)
{
    // FIXME: This should be a "composite" of the object record and the declarative record.
    m_object_record->put_into_environment_record(name, variable);
}

bool GlobalEnvironmentRecord::delete_from_environment_record(FlyString const& name)
{
    // FIXME: This should be a "composite" of the object record and the declarative record.
    return object_record().delete_property(name);
}

Value GlobalEnvironmentRecord::get_this_binding(GlobalObject&) const
{
    return &m_global_object;
}

Value GlobalEnvironmentRecord::global_this_value() const
{
    return &m_global_object;
}

}
