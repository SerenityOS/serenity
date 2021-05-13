/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLLegendElement.h>
#include <LibWeb/Layout/Legend.h>

namespace Web::HTML {

HTMLLegendElement::HTMLLegendElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLegendElement::~HTMLLegendElement()
{
}

RefPtr<Layout::Node> HTMLLegendElement::create_layout_node()
{
    auto style = document().style_resolver().resolve_style(*this);
    if (style->display() == CSS::Display::None)
        return nullptr;

    auto layout_node = adopt_ref(*new Layout::Legend(document(), this, move(style)));
    layout_node->set_inline(true);
    return layout_node;
}

}
