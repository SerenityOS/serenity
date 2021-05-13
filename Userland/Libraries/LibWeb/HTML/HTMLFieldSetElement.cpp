/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/Layout/FieldSet.h>

namespace Web::HTML {

HTMLFieldSetElement::HTMLFieldSetElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLFieldSetElement::~HTMLFieldSetElement()
{
}

RefPtr<Layout::Node> HTMLFieldSetElement::create_layout_node()
{
    auto style = document().style_resolver().resolve_style(*this);
    if (style->display() == CSS::Display::None)
        return nullptr;

    auto layout_node = adopt_ref(*new Layout::FieldSet(document(), this, move(style)));
    return layout_node;
}

}
