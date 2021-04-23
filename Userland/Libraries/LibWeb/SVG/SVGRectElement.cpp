/*
 * Copyright (c) 2021, Simon Danner <danner.simon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>
#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Layout/SVGRectBox.h>
#include <LibWeb/SVG/SVGRectElement.h>
#include <ctype.h>

namespace Web::SVG {

RefPtr<Layout::Node> SVGRectElement::create_layout_node()
{
    auto style = document().style_resolver().resolve_style(*this);
    if (style->display() == CSS::Display::None)
        return nullptr;

    dbgln("create layout node");
    return adopt_ref(*new Layout::SVGRectBox(document(), *this, move(style)));
}

void SVGRectElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    for_each_attribute([&](auto& name, auto& value) {
        if (name == HTML::AttributeNames::width) {
            if (auto parsed_value = parse_html_length(document(), value)) {
                style.set_property(CSS::PropertyID::Width, parsed_value.release_nonnull());
            }
        } else if (name == HTML::AttributeNames::height) {
            if (auto parsed_value = parse_html_length(document(), value)) {
                style.set_property(CSS::PropertyID::Height, parsed_value.release_nonnull());
            }
        }
    });
}

void SVGRectElement::parse_attribute(const FlyString& name, const String& value)
{
    SVGGeometryElement::parse_attribute(name, value);

    dbgln("parse attribute ");
    dbgln(name);
    dbgln(value);
    if (name == "x") {
        auto integer = value.to_int();
        if (integer.has_value()) {
            x = integer.value();
        }
        return;
    }
    if (name == "y") {
        auto integer = value.to_int();
        if (integer.has_value()) {
            y = integer.value();
        }
        return;
    }
}

}
