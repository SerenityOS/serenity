/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/SVG/SVGGElement.h>

namespace Web::SVG {

SVGGElement::SVGGElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
}

RefPtr<Layout::Node> SVGGElement::create_layout_node()
{
    auto style = document().style_computer().compute_style(*this);
    if (style->display().is_none())
        return nullptr;
    return adopt_ref(*new Layout::SVGGraphicsBox(document(), *this, move(style)));
}

}
