/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLabelElement.h>
#include <LibWeb/Layout/Label.h>

namespace Web::HTML {

HTMLLabelElement::HTMLLabelElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLabelElement::~HTMLLabelElement()
{
}

RefPtr<Layout::Node> HTMLLabelElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    auto layout_node = adopt_ref(*new Layout::Label(document(), this, move(style)));
    layout_node->set_inline(true);
    return layout_node;
}

}
