/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
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
    auto style = document().style_computer().compute_style(*this);
    if (style->display() == CSS::Display::None)
        return nullptr;
    return adopt_ref(*new Layout::BreakNode(document(), *this, move(style)));
}

}
