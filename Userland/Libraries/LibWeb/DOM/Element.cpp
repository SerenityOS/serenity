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

#include <AK/AnyOf.h>
#include <AK/StringBuilder.h>
#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleInvalidator.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/Parser/HTMLDocumentParser.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/InlineNode.h>
#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/TableBox.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableRowBox.h>
#include <LibWeb/Layout/TableRowGroupBox.h>
#include <LibWeb/Layout/TreeBuilder.h>
#include <LibWeb/Namespace.h>

namespace Web::DOM {

Element::Element(Document& document, QualifiedName qualified_name)
    : ParentNode(document, NodeType::ELEMENT_NODE)
    , m_qualified_name(move(qualified_name))
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

ExceptionOr<void> Element::set_attribute(const FlyString& name, const String& value)
{
    // FIXME: Proper name validation
    if (name.is_empty())
        return InvalidCharacterError::create("Attribute name must not be empty");

    CSS::StyleInvalidator style_invalidator(document());

    if (auto* attribute = find_attribute(name))
        attribute->set_value(value);
    else
        m_attributes.empend(name, value);

    parse_attribute(name, value);
    return {};
}

void Element::remove_attribute(const FlyString& name)
{
    CSS::StyleInvalidator style_invalidator(document());

    m_attributes.remove_first_matching([&](auto& attribute) { return attribute.name() == name; });
}

bool Element::has_class(const FlyString& class_name, CaseSensitivity case_sensitivity) const
{
    return any_of(m_classes.begin(), m_classes.end(), [&](auto& it) {
        return case_sensitivity == CaseSensitivity::CaseSensitive
            ? it == class_name
            : it.to_lowercase() == class_name.to_lowercase();
    });
}

RefPtr<Layout::Node> Element::create_layout_node()
{
    auto style = document().style_resolver().resolve_style(*this);
    const_cast<Element&>(*this).m_specified_css_values = style;
    auto display = style->display();

    if (display == CSS::Display::None)
        return nullptr;

    if (local_name() == "noscript" && document().is_scripting_enabled())
        return nullptr;

    switch (display) {
    case CSS::Display::None:
        VERIFY_NOT_REACHED();
        break;
    case CSS::Display::Block:
        return adopt(*new Layout::BlockBox(document(), this, move(style)));
    case CSS::Display::Inline:
        if (style->float_().value_or(CSS::Float::None) != CSS::Float::None)
            return adopt(*new Layout::BlockBox(document(), this, move(style)));
        return adopt(*new Layout::InlineNode(document(), *this, move(style)));
    case CSS::Display::ListItem:
        return adopt(*new Layout::ListItemBox(document(), *this, move(style)));
    case CSS::Display::Table:
        return adopt(*new Layout::TableBox(document(), this, move(style)));
    case CSS::Display::TableRow:
        return adopt(*new Layout::TableRowBox(document(), this, move(style)));
    case CSS::Display::TableCell:
        return adopt(*new Layout::TableCellBox(document(), this, move(style)));
    case CSS::Display::TableRowGroup:
    case CSS::Display::TableHeaderGroup:
    case CSS::Display::TableFooterGroup:
        return adopt(*new Layout::TableRowGroupBox(document(), *this, move(style)));
    case CSS::Display::InlineBlock: {
        auto inline_block = adopt(*new Layout::BlockBox(document(), this, move(style)));
        inline_block->set_inline(true);
        return inline_block;
    }
    case CSS::Display::Flex:
        return adopt(*new Layout::BlockBox(document(), this, move(style)));
    case CSS::Display::TableColumn:
    case CSS::Display::TableColumnGroup:
    case CSS::Display::TableCaption:
        // FIXME: This is just an incorrect placeholder until we improve table layout support.
        return adopt(*new Layout::BlockBox(document(), this, move(style)));
    }
    VERIFY_NOT_REACHED();
}

void Element::parse_attribute(const FlyString& name, const String& value)
{
    if (name == HTML::AttributeNames::class_) {
        auto new_classes = value.split_view(' ');
        m_classes.clear();
        m_classes.ensure_capacity(new_classes.size());
        for (auto& new_class : new_classes) {
            m_classes.unchecked_append(new_class);
        }
    } else if (name == HTML::AttributeNames::style) {
        m_inline_style = parse_css_declaration(CSS::ParsingContext(document()), value);
        set_needs_style_update(true);
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
    VERIFY(parent());
    auto old_specified_css_values = m_specified_css_values;
    auto new_specified_css_values = document().style_resolver().resolve_style(*this);
    m_specified_css_values = new_specified_css_values;
    if (!layout_node()) {
        if (new_specified_css_values->display() == CSS::Display::None)
            return;
        // We need a new layout tree here!
        Layout::TreeBuilder tree_builder;
        tree_builder.build(*this);
        return;
    }

    auto diff = StyleDifference::NeedsRelayout;
    if (old_specified_css_values)
        diff = compute_style_difference(*old_specified_css_values, *new_specified_css_values, document());
    if (diff == StyleDifference::None)
        return;
    layout_node()->apply_style(*new_specified_css_values);
    if (diff == StyleDifference::NeedsRelayout) {
        document().schedule_forced_layout();
        return;
    }
    if (diff == StyleDifference::NeedsRepaint) {
        layout_node()->set_needs_display();
    }
}

NonnullRefPtr<CSS::StyleProperties> Element::computed_style()
{
    // FIXME: This implementation is not doing anything it's supposed to.
    auto properties = m_specified_css_values->clone();
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
            auto prop = m_specified_css_values->property(id);
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
    document().invalidate_layout();
}

String Element::inner_html() const
{
    auto escape_string = [](const StringView& string, bool attribute_mode) -> String {
        // https://html.spec.whatwg.org/multipage/parsing.html#escapingString
        StringBuilder builder;
        for (auto& ch : string) {
            if (ch == '&')
                builder.append("&amp;");
            // FIXME: also replace U+00A0 NO-BREAK SPACE with &nbsp;
            else if (ch == '"' && attribute_mode)
                builder.append("&quot;");
            else if (ch == '<' && !attribute_mode)
                builder.append("&lt;");
            else if (ch == '>' && !attribute_mode)
                builder.append("&gt;");
            else
                builder.append(ch);
        }
        return builder.to_string();
    };

    StringBuilder builder;

    Function<void(const Node&)> recurse = [&](auto& node) {
        for (auto* child = node.first_child(); child; child = child->next_sibling()) {
            if (child->is_element()) {
                auto& element = downcast<Element>(*child);
                builder.append('<');
                builder.append(element.local_name());
                element.for_each_attribute([&](auto& name, auto& value) {
                    builder.append(' ');
                    builder.append(name);
                    builder.append('=');
                    builder.append('"');
                    builder.append(escape_string(value, true));
                    builder.append('"');
                });
                builder.append('>');

                recurse(*child);

                // FIXME: This should be skipped for void elements
                builder.append("</");
                builder.append(element.local_name());
                builder.append('>');
            }
            if (child->is_text()) {
                auto& text = downcast<Text>(*child);
                builder.append(escape_string(text.data(), false));
            }
            // FIXME: Also handle Comment, ProcessingInstruction, DocumentType
        }
    };
    recurse(*this);

    return builder.to_string();
}

bool Element::is_focused() const
{
    return document().focused_element() == this;
}

NonnullRefPtrVector<Element> Element::get_elements_by_tag_name(const FlyString& tag_name) const
{
    // FIXME: Support "*" for tag_name
    // https://dom.spec.whatwg.org/#concept-getelementsbytagname
    NonnullRefPtrVector<Element> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.namespace_() == Namespace::HTML
                ? element.local_name().to_lowercase() == tag_name.to_lowercase()
                : element.local_name() == tag_name) {
            elements.append(element);
        }
        return IterationDecision::Continue;
    });
    return elements;
}

NonnullRefPtrVector<Element> Element::get_elements_by_class_name(const FlyString& class_name) const
{
    NonnullRefPtrVector<Element> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (element.has_class(class_name, m_document->in_quirks_mode() ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive))
            elements.append(element);
        return IterationDecision::Continue;
    });
    return elements;
}

void Element::set_shadow_root(RefPtr<ShadowRoot> shadow_root)
{
    if (m_shadow_root == shadow_root)
        return;
    m_shadow_root = move(shadow_root);
    invalidate_style();
}

NonnullRefPtr<CSS::CSSStyleDeclaration> Element::style_for_bindings()
{
    if (!m_inline_style)
        m_inline_style = CSS::CSSStyleDeclaration::create({});
    return *m_inline_style;
}

}
