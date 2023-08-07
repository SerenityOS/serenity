/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLStyleElement.h>

namespace Web::HTML {

HTMLStyleElement::HTMLStyleElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLStyleElement::~HTMLStyleElement() = default;

void HTMLStyleElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLStyleElementPrototype>(realm, "HTMLStyleElement"));
}

void HTMLStyleElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_style_element_utils.sheet());
}

void HTMLStyleElement::children_changed()
{
    m_style_element_utils.update_a_style_block(*this);
    Base::children_changed();
}

void HTMLStyleElement::inserted()
{
    m_style_element_utils.update_a_style_block(*this);
    Base::inserted();
}

void HTMLStyleElement::removed_from(Node* old_parent)
{
    m_style_element_utils.update_a_style_block(*this);
    Base::removed_from(old_parent);
}

// https://www.w3.org/TR/cssom/#dom-linkstyle-sheet
CSS::CSSStyleSheet* HTMLStyleElement::sheet()
{
    // The sheet attribute must return the associated CSS style sheet for the node or null if there is no associated CSS style sheet.
    return m_style_element_utils.sheet();
}

// https://www.w3.org/TR/cssom/#dom-linkstyle-sheet
CSS::CSSStyleSheet const* HTMLStyleElement::sheet() const
{
    // The sheet attribute must return the associated CSS style sheet for the node or null if there is no associated CSS style sheet.
    return m_style_element_utils.sheet();
}

}
