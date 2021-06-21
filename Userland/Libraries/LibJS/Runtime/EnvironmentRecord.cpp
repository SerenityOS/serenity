/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/EnvironmentRecord.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

EnvironmentRecord::EnvironmentRecord(EnvironmentRecord* parent)
    : Object(vm().environment_record_shape())
    , m_parent(parent)
{
}

EnvironmentRecord::EnvironmentRecord(GlobalObjectTag tag)
    : Object(tag)
{
}

void EnvironmentRecord::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_parent);
}

}
