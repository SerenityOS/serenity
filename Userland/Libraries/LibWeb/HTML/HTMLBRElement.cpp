/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLBRElement.h>
#include <LibWeb/Layout/BreakNode.h>

namespace Web::HTML {

HTMLBRElement::HTMLBRElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLBRElement::~HTMLBRElement()
{
}

RefPtr<Layout::Node> HTMLBRElement::create_layout_node()
{
    return adopt_ref(*new Layout::BreakNode(document(), *this));
}

}
