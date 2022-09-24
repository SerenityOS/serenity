/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

StyleSheet::StyleSheet(JS::Realm& realm)
    : PlatformObject(realm)
{
}

void StyleSheet::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_owner_node);
    visitor.visit(m_parent_style_sheet);
}

void StyleSheet::set_owner_node(DOM::Element* element)
{
    m_owner_node = element;
}

void StyleSheet::set_parent_css_style_sheet(CSSStyleSheet* parent)
{
    m_parent_style_sheet = parent;
}

}
