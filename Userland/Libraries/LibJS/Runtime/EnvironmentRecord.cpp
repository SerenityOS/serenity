/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/EnvironmentRecord.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

EnvironmentRecord::EnvironmentRecord(EnvironmentRecord* outer_environment)
    : Object(vm().environment_record_shape())
    , m_outer_environment(outer_environment)
{
}

EnvironmentRecord::EnvironmentRecord(GlobalObjectTag tag)
    : Object(tag)
{
}

void EnvironmentRecord::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_outer_environment);
}

}
