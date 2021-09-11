/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>

namespace JS {

void Realm::visit_edges(Visitor& visitor)
{
    visitor.visit(m_global_object);
    visitor.visit(m_global_environment);
}

}
