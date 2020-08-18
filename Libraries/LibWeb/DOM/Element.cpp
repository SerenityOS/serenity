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

#include <AK/StringBuilder.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/Parser/HTMLDocumentParser.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutInline.h>
#include <LibWeb/Layout/LayoutListItem.h>
#include <LibWeb/Layout/LayoutTable.h>
#include <LibWeb/Layout/LayoutTableCell.h>
#include <LibWeb/Layout/LayoutTableRow.h>
#include <LibWeb/Layout/LayoutTableRowGroup.h>
#include <LibWeb/Layout/LayoutText.h>
#include <LibWeb/Layout/LayoutTreeBuilder.h>

namespace Web::DOM {

Element::Element(Document& document, const FlyString& tag_name)
    : ParentNode(document, NodeType::ELEMENT_NODE)
    , m_tag_name(tag_name)
{
}

Element::~Element()
{
}

Attribute* Element::find_attribute(const FlyString& name)
{
    for (auto& attribute : m_attributes) {
        if (attribute.name() == name)
            return &attribute;
    }
    return nullptr;
}

const Attribute* Element::find_attribute(const FlyString& name) const
{
    for (auto& attribute : m_attributes) {
        if (attribute.name() == name)
            return &attribute;
    }
    return nullptr;
}

String Element::attribute(const FlyString& name) const
{
    if (auto* attribute = find_attribute(name))
        return attribute->value();
    return {};
}

void Element::set_attribute(const FlyString& name, const String& value)
{
    if (auto* attribute = find_attribute(name))
        attribute->set_value(value);
    else
        m_attributes.empend(name, value);

    parse_attribute(name, value);
}

void Element::remove_attribute(const FlyString& name)
{
    m_attributes.remove_first_matching([&](auto& attribute) { return attribute.name() == name; });
}

void Element::set_attributes(Vector<Attribute>&& attributes)
{
    m_attributes = move(attributes);

    for (auto& attribute : m_attributes)
        parse_attribute(attribute.name(), attribute.value());
}

bool Element::has_class(const FlyString& class_name) const
{
    for (auto& class_ : m_classes) {
        if (class_ == class_name)
            return true;
    }
    return false;
}

RefPtr<LayoutNode> Element::create_layout_node(const CSS::StyleProperties* parent_style)
{
    auto style = document().style_resolver().resolve_style(*this, parent_style);
    const_cast<Element&>(*this).m_resolved_style = style;
    auto display = style->display();

    if (display == CSS::Display::None)
        return nullptr;

    if (local_name() == "noscript" && document().is_scripting_enabled())
        return nullptr;

    if (display == CSS::Display::Block)
        return adopt(*new LayoutBlock(document(), this, move(style)));

    if (display == CSS::Display::Inline) {
        if (style->float_().value_or(CSS::Float::None) != CSS::Float::None)
            return adopt(*new LayoutBlock(document(), this, move(style)));
        return adopt(*new LayoutInline(document(), *this, move(style)));
    }

    if (display == CSS::Display::ListItem)
        return adopt(*new LayoutListItem(document(), *this, move(style)));
    if (display == CSS::Display::Table)
        return adopt(*new LayoutTable(document(), *this, move(style)));
    if (display == CSS::Display::TableRow)
        return adopt(*new LayoutTableRow(document(), *this, move(style)));
    if (display == CSS::Display::TableCell)
        return adopt(*new LayoutTableCell(document(), *this, move(style)));
    if (display == CSS::Display::TableRowGroup || display == CSS::Display::TableHeaderGroup || display == CSS::Display::TableFooterGroup)
        return adopt(*new LayoutTableRowGroup(document(), *this, move(style)));
    if (display == CSS::Display::InlineBlock) {
        auto inline_block = adopt(*new LayoutBlock(document(), this, move(style)));
        inline_block->set_inline(true);
        return inline_block;
    }
    ASSERT_NOT_REACHED();
}

void Element::parse_attribute(const FlyString& name, const String& value)
{
    if (name == "class") {
        auto new_classes = value.split_view(' ');
        m_classes.clear();
        m_classes.ensure_capacity(new_classes.size());
        for (auto& new_class : new_classes) {
            m_classes.unchecked_append(new_class);
        }
    }
}

enum class StyleDifference {
    None,
    NeedsRepaint,
    NeedsRelayout,
};

static StyleDifference compute_style_difference(const CSS::StyleProperties& old_style, const CSS::StyleProperties& new_style, const Document& document)
{
    if (old_style == new_style)
        return StyleDifference::None;

    bool needs_repaint = false;
    bool needs_relayout = false;

    if (new_style.display() != old_style.display())
        needs_relayout = true;

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
    auto style = document().style_resolver().resolve_style(*this, &parent_layout_node->specified_style());
    m_resolved_style = style;
    if (!layout_node()) {
        if (style->display() == CSS::Display::None)
            return;
        // We need a new layout tree here!
        LayoutTreeBuilder tree_builder;
        tree_builder.build(*this);
        return;
    }

    // Don't bother with style on widgets. NATIVE LOOK & FEEL BABY!
    if (layout_node()->is_widget())
        return;

    auto diff = compute_style_difference(layout_node()->specified_style(), *style, document());
    if (diff == StyleDifference::None)
        return;
    layout_node()->set_specified_style(*style);
    if (diff == StyleDifference::NeedsRelayout) {
        document().force_layout();
        return;
    }
    if (diff == StyleDifference::NeedsRepaint) {
        layout_node()->set_needs_display();
    }
}

NonnullRefPtr<CSS::StyleProperties> Element::computed_style()
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
            auto prop = layout_node()->specified_style().property(id);
            if (prop.has_value())
                properties->set_property(id, prop.value());
        }
    }
    return properties;
}

void Element::set_inner_html(StringView markup)
{
    auto new_children = HTML::HTMLDocumentParser::parse_html_fragment(*this, markup);
    remove_all_children();
    while (!new_children.is_empty()) {
        append_child(new_children.take_first());
    }

    set_needs_style_update(true);
    document().schedule_style_update();
    document().invalidate_layout();
}

String Element::inner_html() const
{
    StringBuilder builder;

    Function<void(const Node&)> recurse = [&](auto& node) {
        for (auto* child = node.first_child(); child; child = child->next_sibling()) {
            if (child->is_element()) {
                builder.append('<');
                builder.append(downcast<Element>(*child).local_name());
                builder.append('>');

                recurse(*child);

                builder.append("</");
                builder.append(downcast<Element>(*child).local_name());
                builder.append('>');
            }
            if (child->is_text()) {
                builder.append(downcast<Text>(*child).data());
            }
        }
    };
    recurse(*this);

    return builder.to_string();
}

void Element::set_inner_text(StringView text)
{
    remove_all_children();
    append_child(document().create_text_node(text));

    set_needs_style_update(true);
    document().schedule_style_update();
    document().invalidate_layout();
}

String Element::inner_text()
{
    StringBuilder builder;

    // innerText for element being rendered takes visibility into account, so force a layout and then walk the layout tree.
    document().layout();
    if (!layout_node())
        return text_content();

    Function<void(const LayoutNode&)> recurse = [&](auto& node) {
        for (auto* child = node.first_child(); child; child = child->next_sibling()) {
            if (child->is_text())
                builder.append(downcast<LayoutText>(*child).text_for_rendering());
            if (child->is_break())
                builder.append('\n');
            recurse(*child);
        }
    };
    recurse(*layout_node());

    return builder.to_string();
}

bool Element::is_focused() const
{
    return document().focused_element() == this;
}

}
