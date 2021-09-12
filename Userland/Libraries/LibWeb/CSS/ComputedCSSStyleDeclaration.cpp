/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/ComputedCSSStyleDeclaration.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

ComputedCSSStyleDeclaration::ComputedCSSStyleDeclaration(DOM::Element& element)
    : m_element(element)
{
}

ComputedCSSStyleDeclaration::~ComputedCSSStyleDeclaration()
{
}

size_t ComputedCSSStyleDeclaration::length() const
{
    return 0;
}

String ComputedCSSStyleDeclaration::item(size_t index) const
{
    (void)index;
    return {};
}

static CSS::ValueID to_css_value_id(CSS::Display value)
{
    switch (value) {
    case CSS::Display::None:
        return CSS::ValueID::None;
    case CSS::Display::Block:
        return CSS::ValueID::Block;
    case CSS::Display::Inline:
        return CSS::ValueID::Inline;
    case CSS::Display::InlineBlock:
        return CSS::ValueID::InlineBlock;
    case CSS::Display::ListItem:
        return CSS::ValueID::ListItem;
    case CSS::Display::Table:
        return CSS::ValueID::Table;
    case CSS::Display::TableRow:
        return CSS::ValueID::TableRow;
    case CSS::Display::TableCell:
        return CSS::ValueID::TableCell;
    case CSS::Display::TableHeaderGroup:
        return CSS::ValueID::TableHeaderGroup;
    case CSS::Display::TableRowGroup:
        return CSS::ValueID::TableRowGroup;
    case CSS::Display::TableFooterGroup:
        return CSS::ValueID::TableFooterGroup;
    case CSS::Display::TableColumn:
        return CSS::ValueID::TableColumn;
    case CSS::Display::TableColumnGroup:
        return CSS::ValueID::TableColumnGroup;
    case CSS::Display::TableCaption:
        return CSS::ValueID::TableCaption;
    case CSS::Display::Flex:
        return CSS::ValueID::Flex;
    }
    VERIFY_NOT_REACHED();
}

Optional<StyleProperty> ComputedCSSStyleDeclaration::property(PropertyID property_id) const
{
    const_cast<DOM::Document&>(m_element->document()).force_layout();

    if (!m_element->layout_node()) {
        auto style = m_element->document().style_resolver().resolve_style(const_cast<DOM::Element&>(*m_element));
        if (auto maybe_property = style->property(property_id); maybe_property.has_value()) {
            return StyleProperty {
                .property_id = property_id,
                .value = maybe_property.release_value(),
            };
        }
        return {};
    }

    auto& layout_node = *m_element->layout_node();

    switch (property_id) {
    case CSS::PropertyID::Color:
        return StyleProperty {
            .property_id = property_id,
            .value = ColorStyleValue::create(layout_node.computed_values().color()),
        };
    case CSS::PropertyID::Display: {
        return StyleProperty {
            .property_id = property_id,
            .value = IdentifierStyleValue::create(to_css_value_id(layout_node.computed_values().display())),
        };
    }
    default:
        return {};
    }
}

bool ComputedCSSStyleDeclaration::set_property(PropertyID, StringView)
{
    return false;
}

}
