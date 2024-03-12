/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/CustomElements/CustomElementDefinition.h>

namespace Web::HTML {

void CustomElementDefinition::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& callback : m_lifecycle_callbacks)
        visitor.visit(callback.value);
}

}
