/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/DeclarativeEnvironmentRecord.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord()
    : EnvironmentRecord(nullptr)
{
}

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord(EnvironmentRecordType environment_record_type)
    : EnvironmentRecord(nullptr)
    , m_environment_record_type(environment_record_type)
{
}

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord(EnvironmentRecord* parent_scope)
    : EnvironmentRecord(parent_scope)
{
}

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope)
    : EnvironmentRecord(parent_scope)
    , m_variables(move(variables))
{
}

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope, EnvironmentRecordType environment_record_type)
    : EnvironmentRecord(parent_scope)
    , m_environment_record_type(environment_record_type)
    , m_variables(move(variables))
{
}

DeclarativeEnvironmentRecord::~DeclarativeEnvironmentRecord()
{
}

void DeclarativeEnvironmentRecord::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& it : m_variables)
        visitor.visit(it.value.value);
}

Optional<Variable> DeclarativeEnvironmentRecord::get_from_environment_record(FlyString const& name) const
{
    return m_variables.get(name);
}

void DeclarativeEnvironmentRecord::put_into_environment_record(FlyString const& name, Variable variable)
{
    m_variables.set(name, variable);
}

bool DeclarativeEnvironmentRecord::delete_from_environment_record(FlyString const& name)
{
    return m_variables.remove(name);
}

}
