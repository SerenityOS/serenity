/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/EnvironmentRecord.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

EnvironmentRecord::EnvironmentRecord(EnvironmentRecord* outer_environment)
    : m_outer_environment(outer_environment)
{
}

void EnvironmentRecord::initialize(GlobalObject& global_object)
{
    m_global_object = &global_object;
    Cell::initialize(global_object);
}

void EnvironmentRecord::visit_edges(Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_outer_environment);
}

}
