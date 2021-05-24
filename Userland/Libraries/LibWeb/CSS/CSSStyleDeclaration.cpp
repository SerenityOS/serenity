/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

CSSStyleDeclaration::CSSStyleDeclaration(Vector<StyleProperty>&& properties, HashMap<String, StyleProperty>&& custom_properties)
    : m_properties(move(properties))
    , m_custom_properties(move(custom_properties))
{
}

CSSStyleDeclaration::~CSSStyleDeclaration()
{
}

String CSSStyleDeclaration::item(size_t index) const
{
    if (index >= m_properties.size())
        return {};
    return CSS::string_from_property_id(m_properties[index].property_id);
}

ElementInlineCSSStyleDeclaration::ElementInlineCSSStyleDeclaration(DOM::Element& element)
    : CSSStyleDeclaration({}, {})
    , m_element(element.make_weak_ptr<DOM::Element>())
{
}

ElementInlineCSSStyleDeclaration::~ElementInlineCSSStyleDeclaration()
{
}

}
