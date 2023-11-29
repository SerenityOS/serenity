/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/AbstractBrowsingContext.h>
#include <LibWeb/HTML/BrowsingContext.h>

namespace Web::HTML {

void AbstractBrowsingContext::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_opener_browsing_context);
}

}
