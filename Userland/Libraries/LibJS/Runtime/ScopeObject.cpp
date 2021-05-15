/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/ScopeObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

ScopeObject::ScopeObject(ScopeObject* parent)
    : Object(vm().scope_object_shape())
    , m_parent(parent)
{
}

ScopeObject::ScopeObject(GlobalObjectTag tag)
    : Object(tag)
{
}

void ScopeObject::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_parent);
}

}
