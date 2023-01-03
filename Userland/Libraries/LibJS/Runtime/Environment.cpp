/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Environment::Environment(Environment* outer_environment)
    : m_outer_environment(outer_environment)
{
}

void Environment::visit_edges(Visitor& visitor)
{
    Cell::visit_edges(visitor);
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
