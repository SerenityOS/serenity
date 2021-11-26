/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

Environment::Environment(Environment* outer_environment)
    : m_outer_environment(outer_environment)
{
}

void Environment::initialize(GlobalObject& global_object)
{
    m_global_object = &global_object;
    Cell::initialize(global_object);
}

void Environment::visit_edges(Visitor& visitor)
{
    Cell::visit_edges(visitor);
    visitor.visit(m_global_object);
    visitor.visit(m_outer_environment);
}

void Environment::set_permanently_screwed_by_eval()
{
    if (m_permanently_screwed_by_eval)
        return;
    m_permanently_screwed_by_eval = true;
    if (outer_environment())
        outer_environment()->set_permanently_screwed_by_eval();
}

}
