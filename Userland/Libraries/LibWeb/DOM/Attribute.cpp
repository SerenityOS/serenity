/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Attribute.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::DOM {

NonnullRefPtr<Attribute> Attribute::create(Document& document, FlyString local_name, String value, Element const* owner_element)
{
    return adopt_ref(*new Attribute(document, move(local_name), move(value), owner_element));
}

Attribute::Attribute(Document& document, FlyString local_name, String value, Element const* owner_element)
    : Node(document, NodeType::ATTRIBUTE_NODE)
    , m_qualified_name(move(local_name), {}, {})
    , m_value(move(value))
    , m_owner_element(owner_element)
{
}

Element const* Attribute::owner_element() const
{
    return m_owner_element;
}

void Attribute::set_owner_element(Element const* owner_element)
{
    m_owner_element = owner_element;
}

}
