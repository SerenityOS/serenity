/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/PropertyID.h>
#include <LibHTML/CSS/Length.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutInline.h>
#include <LibHTML/Layout/LayoutListItem.h>
#include <LibHTML/Layout/LayoutTable.h>
#include <LibHTML/Layout/LayoutTableCell.h>
#include <LibHTML/Layout/LayoutTableRow.h>
#include <LibHTML/Layout/LayoutTreeBuilder.h>

Element::Element(Document& document, const String& tag_name)
    : ParentNode(document, NodeType::ELEMENT_NODE)
    , m_tag_name(tag_name)
{
}

Element::~Element()
{
}

Attribute* Element::find_attribute(const String& name)
{
    for (auto& attribute : m_attributes) {
        if (attribute.name() == name)
            return &attribute;
    }
    return nullptr;
}

const Attribute* Element::find_attribute(const String& name) const
{
    for (auto& attribute : m_attributes) {
        if (attribute.name() == name)
            return &attribute;
    }
    return nullptr;
}

String Element::attribute(const String& name) const
{
    if (auto* attribute = find_attribute(name))
        return attribute->value();
    return {};
}

void Element::set_attribute(const String& name, const String& value)
{
    if (auto* attribute = find_attribute(name))
        attribute->set_value(value);
    else
        m_attributes.empend(name, value);

    parse_attribute(name, value);
}

void Element::set_attributes(Vector<Attribute>&& attributes)
{
    m_attributes = move(attributes);

    for (auto& attribute : m_attributes)
        parse_attribute(attribute.name(), attribute.value());
}

bool Element::has_class(const StringView& class_name) const
{
    auto value = attribute("class");
    if (value.is_empty())
        return false;
    auto parts = value.split_view(' ');
    for (auto& part : parts) {
        if (part == class_name)
            return true;
    }
    return false;
}

RefPtr<LayoutNode> Element::create_layout_node(const StyleProperties* parent_style) const
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    const_cast<Element&>(*this).m_resolved_style = style;
    auto display = style->string_or_fallback(CSS::PropertyID::Display, "inline");

    if (display == "none")
        return nullptr;
    if (display == "block")
        return adopt(*new LayoutBlock(this, move(style)));
    if (display == "inline")
        return adopt(*new LayoutInline(*this, move(style)));
    if (display == "list-item")
        return adopt(*new LayoutListItem(*this, move(style)));
    if (display == "table")
        return adopt(*new LayoutTable(*this, move(style)));
    if (display == "table-row")
        return adopt(*new LayoutTableRow(*this, move(style)));
    if (display == "table-cell")
        return adopt(*new LayoutTableCell(*this, move(style)));
    if (display == "inline-block")
        return adopt(*new LayoutBlock(this, move(style)));

    ASSERT_NOT_REACHED();
}

void Element::parse_attribute(const String&, const String&)
{
}

enum class StyleDifference {
    None,
    NeedsRepaint,
    NeedsRelayout,
};

static StyleDifference compute_style_difference(const StyleProperties& old_style, const StyleProperties& new_style, const Document& document)
{
    if (old_style == new_style)
        return StyleDifference::None;

    bool needs_repaint = false;
    bool needs_relayout = false;

    if (new_style.color_or_fallback(CSS::PropertyID::Color, document, Color::Black) != old_style.color_or_fallback(CSS::PropertyID::Color, document, Color::Black))
        needs_repaint = true;
    else if (new_style.color_or_fallback(CSS::PropertyID::BackgroundColor, document, Color::Black) != old_style.color_or_fallback(CSS::PropertyID::BackgroundColor, document, Color::Black))
        needs_repaint = true;

    if (needs_relayout)
        return StyleDifference::NeedsRelayout;
    if (needs_repaint)
        return StyleDifference::NeedsRepaint;
    return StyleDifference::None;
}

void Element::recompute_style()
{
    set_needs_style_update(false);
    ASSERT(parent());
    auto* parent_layout_node = parent()->layout_node();
    if (!parent_layout_node)
        return;
    ASSERT(parent_layout_node);
    auto style = document().style_resolver().resolve_style(*this, &parent_layout_node->style());
    m_resolved_style = style;
    if (!layout_node()) {
        if (style->string_or_fallback(CSS::PropertyID::Display, "inline") == "none")
            return;
        // We need a new layout tree here!
        LayoutTreeBuilder tree_builder;
        tree_builder.build(*this);
        return;
    }
    auto diff = compute_style_difference(layout_node()->style(), *style, document());
    if (diff == StyleDifference::None)
        return;
    layout_node()->set_style(*style);
    if (diff == StyleDifference::NeedsRelayout) {
        ASSERT_NOT_REACHED();
    }
    if (diff == StyleDifference::NeedsRepaint) {
        layout_node()->set_needs_display();
    }
}

NonnullRefPtr<StyleProperties> Element::computed_style()
{
    auto properties = m_resolved_style->clone();
    if (layout_node() && layout_node()->has_style()) {
        CSS::PropertyID box_model_metrics[] = {
            CSS::PropertyID::MarginTop,
            CSS::PropertyID::MarginBottom,
            CSS::PropertyID::MarginLeft,
            CSS::PropertyID::MarginRight,
            CSS::PropertyID::PaddingTop,
            CSS::PropertyID::PaddingBottom,
            CSS::PropertyID::PaddingLeft,
            CSS::PropertyID::PaddingRight,
            CSS::PropertyID::BorderTopWidth,
            CSS::PropertyID::BorderBottomWidth,
            CSS::PropertyID::BorderLeftWidth,
            CSS::PropertyID::BorderRightWidth,
        };
        for (CSS::PropertyID id : box_model_metrics) {
            auto prop = layout_node()->style().property(id);
            if (prop)
                properties->set_property(id, prop.value());
        }
    }
    return properties;
}
