/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/DocumentState.h>

namespace Web::HTML {

DocumentState::DocumentState() = default;

DocumentState::~DocumentState() = default;

void DocumentState::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_document);
}

}
