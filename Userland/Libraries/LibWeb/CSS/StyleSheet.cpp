/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/StyleSheetPrototype.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

StyleSheet::StyleSheet(HTML::Window& window_object)
    : PlatformObject(window_object.ensure_web_prototype<Bindings::StyleSheetPrototype>("StyleSheet"))
{
}

void StyleSheet::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_parent_style_sheet);
}

void StyleSheet::set_owner_node(DOM::Element* element)
{
    if (element)
        m_owner_node = element->make_weak_ptr<DOM::Element>();
    else
        m_owner_node = nullptr;
}

void StyleSheet::set_parent_css_style_sheet(CSSStyleSheet* parent)
{
    m_parent_style_sheet = parent;
}

}
