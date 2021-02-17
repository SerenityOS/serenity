/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>
#include <stdio.h>

namespace Web {

void dump_tree(const DOM::Node& node)
{
    StringBuilder builder;
    dump_tree(builder, node);
    dbgln("{}", builder.string_view());
}

void dump_tree(StringBuilder& builder, const DOM::Node& node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        builder.append("  ");
    if (is<DOM::Element>(node)) {
        builder.appendff("<{}", downcast<DOM::Element>(node).local_name());
        downcast<DOM::Element>(node).for_each_attribute([&](auto& name, auto& value) {
            builder.appendff(" {}={}", name, value);
        });
        builder.append(">\n");
    } else if (is<DOM::Text>(node)) {
        builder.appendff("\"{}\"\n", downcast<DOM::Text>(node).data());
    } else {
        builder.appendff("{}\n", node.node_name());
    }
    ++indent;
    if (is<DOM::Element>(node) && downcast<DOM::Element>(node).shadow_root()) {
        dump_tree(*downcast<DOM::Element>(node).shadow_root());
    }
    if (is<DOM::ParentNode>(node)) {
        if (!is<HTML::HTMLTemplateElement>(node)) {
            static_cast<const DOM::ParentNode&>(node).for_each_child([](auto& child) {
                dump_tree(child);
            });
        } else {
            auto& template_element = downcast<HTML::HTMLTemplateElement>(node);
            dump_tree(template_element.content());
        }
    }
    --indent;
}

void dump_tree(const Layout::Node& layout_node, bool show_box_model, bool show_specified_style)
{
    StringBuilder builder;
    dump_tree(builder, layout_node, show_box_model, show_specified_style, true);
    dbgln("{}", builder.string_view());
}

void dump_tree(StringBuilder& builder, const Layout::Node& layout_node, bool show_box_model, bool show_specified_style, bool interactive)
{
    static size_t indent = 0;
    for (size_t i = 0; i < indent; ++i)
        builder.append("  ");

    FlyString tag_name;
    if (layout_node.is_anonymous())
        tag_name = "(anonymous)";
    else if (is<DOM::Element>(layout_node.dom_node()))
        tag_name = downcast<DOM::Element>(*layout_node.dom_node()).local_name();
    else
        tag_name = layout_node.dom_node()->node_name();

    String identifier = "";
    if (layout_node.dom_node() && is<DOM::Element>(*layout_node.dom_node())) {
        auto& element = downcast<DOM::Element>(*layout_node.dom_node());
        StringBuilder builder;
        auto id = element.attribute(HTML::AttributeNames::id);
        if (!id.is_empty()) {
            builder.append('#');
            builder.append(id);
        }
        for (auto& class_name : element.class_names()) {
            builder.append('.');
            builder.append(class_name);
        }
        identifier = builder.to_string();
    }

    const char* nonbox_color_on = "";
    const char* box_color_on = "";
    const char* positioned_color_on = "";
    const char* floating_color_on = "";
    const char* inline_block_color_on = "";
    const char* line_box_color_on = "";
    const char* fragment_color_on = "";
    const char* flex_color_on = "";
    const char* color_off = "";

    if (interactive) {
        nonbox_color_on = "\033[33m";
        box_color_on = "\033[34m";
        positioned_color_on = "\033[31;1m";
        floating_color_on = "\033[32;1m";
        inline_block_color_on = "\033[36;1m";
        line_box_color_on = "\033[34;1m";
        fragment_color_on = "\033[35;1m";
        flex_color_on = "\033[34;1m";
        color_off = "\033[0m";
    }

    if (!is<Layout::Box>(layout_node)) {
        builder.appendff("{}{}{} <{}{}{}>",
            nonbox_color_on,
            layout_node.class_name().substring_view(13),
            color_off,
            tag_name,
            nonbox_color_on,
            identifier,
            color_off);
        if (interactive)
            builder.appendff(" @{:p}", &layout_node);
        builder.append("\n");
    } else {
        auto& box = downcast<Layout::Box>(layout_node);
        builder.appendff("{}{}{} <{}{}{}{}> ",
            box_color_on,
            box.class_name().substring_view(13),
            color_off,
            box_color_on,
            tag_name,
            color_off,
            identifier.characters());

        if (interactive)
            builder.appendff("@{:p} ", &layout_node);

        builder.appendff("at ({},{}) size {}x{}",
            (int)box.absolute_x(),
            (int)box.absolute_y(),
            (int)box.width(),
            (int)box.height());

        if (box.is_positioned())
            builder.appendff(" {}positioned{}", positioned_color_on, color_off);
        if (box.is_floating())
            builder.appendff(" {}floating{}", floating_color_on, color_off);
        if (box.is_inline_block())
            builder.appendff(" {}inline-block{}", inline_block_color_on, color_off);
        if (box.computed_values().display() == CSS::Display::Flex)
            builder.appendff(" {}flex{}", flex_color_on, color_off);

        if (show_box_model) {
            // Dump the horizontal box properties
            builder.appendf(" [%g+%g+%g %g %g+%g+%g]",
                box.box_model().margin.left,
                box.box_model().border.left,
                box.box_model().padding.left,
                box.width(),
                box.box_model().padding.right,
                box.box_model().border.right,
                box.box_model().margin.right);

            // And the vertical box properties
            builder.appendf(" [%g+%g+%g %g %g+%g+%g]",
                box.box_model().margin.top,
                box.box_model().border.top,
                box.box_model().padding.top,
                box.height(),
                box.box_model().padding.bottom,
                box.box_model().border.bottom,
                box.box_model().margin.bottom);
        }

        builder.append("\n");
    }

    if (is<Layout::BlockBox>(layout_node) && static_cast<const Layout::BlockBox&>(layout_node).children_are_inline()) {
        auto& block = static_cast<const Layout::BlockBox&>(layout_node);
        for (size_t line_box_index = 0; line_box_index < block.line_boxes().size(); ++line_box_index) {
            auto& line_box = block.line_boxes()[line_box_index];
            for (size_t i = 0; i < indent; ++i)
                builder.append("  ");
            builder.appendff("  {}line {}{} width: {}\n",
                line_box_color_on,
                line_box_index,
                color_off,
                (int)line_box.width());
            for (size_t fragment_index = 0; fragment_index < line_box.fragments().size(); ++fragment_index) {
                auto& fragment = line_box.fragments()[fragment_index];
                for (size_t i = 0; i < indent; ++i)
                    builder.append("  ");
                builder.appendff("    {}frag {}{} from {} ",
                    fragment_color_on,
                    fragment_index,
                    color_off,
                    fragment.layout_node().class_name());
                if (interactive)
                    builder.appendff("@{:p}, ", &fragment.layout_node());
                builder.appendff("start: {}, length: {}, rect: {}\n",
                    fragment.start(),
                    fragment.length(),
                    enclosing_int_rect(fragment.absolute_rect()).to_string());
                if (is<Layout::TextNode>(fragment.layout_node())) {
                    for (size_t i = 0; i < indent; ++i)
                        builder.append("  ");
                    auto& layout_text = static_cast<const Layout::TextNode&>(fragment.layout_node());
                    auto fragment_text = layout_text.text_for_rendering().substring(fragment.start(), fragment.length());
                    builder.appendff("      \"{}\"\n", fragment_text);
                }
            }
        }
    }

    if (show_specified_style && layout_node.dom_node() && layout_node.dom_node()->is_element() && downcast<DOM::Element>(layout_node.dom_node())->specified_css_values()) {
        struct NameAndValue {
            String name;
            String value;
        };
        Vector<NameAndValue> properties;
        downcast<DOM::Element>(*layout_node.dom_node()).specified_css_values()->for_each_property([&](auto property_id, auto& value) {
            properties.append({ CSS::string_from_property_id(property_id), value.to_string() });
        });
        quick_sort(properties, [](auto& a, auto& b) { return a.name < b.name; });

        for (auto& property : properties) {
            for (size_t i = 0; i < indent; ++i)
                builder.append("    ");
            builder.appendf("  (%s: %s)\n", property.name.characters(), property.value.characters());
        }
    }

    ++indent;
    layout_node.for_each_child([&](auto& child) {
        dump_tree(builder, child, show_box_model, show_specified_style, interactive);
    });
    --indent;
}

void dump_selector(const CSS::Selector& selector)
{
    StringBuilder builder;
    dump_selector(builder, selector);
    dbgln("{}", builder.string_view());
}

void dump_selector(StringBuilder& builder, const CSS::Selector& selector)
{
    builder.append("  CSS::Selector:\n");

    for (auto& complex_selector : selector.complex_selectors()) {
        builder.append("    ");

        const char* relation_description = "";
        switch (complex_selector.relation) {
        case CSS::Selector::ComplexSelector::Relation::None:
            relation_description = "None";
            break;
        case CSS::Selector::ComplexSelector::Relation::ImmediateChild:
            relation_description = "ImmediateChild";
            break;
        case CSS::Selector::ComplexSelector::Relation::Descendant:
            relation_description = "Descendant";
            break;
        case CSS::Selector::ComplexSelector::Relation::AdjacentSibling:
            relation_description = "AdjacentSibling";
            break;
        case CSS::Selector::ComplexSelector::Relation::GeneralSibling:
            relation_description = "GeneralSibling";
            break;
        }

        if (*relation_description)
            builder.appendff("{{{}}} ", relation_description);

        for (size_t i = 0; i < complex_selector.compound_selector.size(); ++i) {
            auto& simple_selector = complex_selector.compound_selector[i];
            const char* type_description = "Unknown";
            switch (simple_selector.type) {
            case CSS::Selector::SimpleSelector::Type::Invalid:
                type_description = "Invalid";
                break;
            case CSS::Selector::SimpleSelector::Type::Universal:
                type_description = "Universal";
                break;
            case CSS::Selector::SimpleSelector::Type::Id:
                type_description = "Id";
                break;
            case CSS::Selector::SimpleSelector::Type::Class:
                type_description = "Class";
                break;
            case CSS::Selector::SimpleSelector::Type::TagName:
                type_description = "TagName";
                break;
            }
            const char* attribute_match_type_description = "";
            switch (simple_selector.attribute_match_type) {
            case CSS::Selector::SimpleSelector::AttributeMatchType::None:
                break;
            case CSS::Selector::SimpleSelector::AttributeMatchType::HasAttribute:
                attribute_match_type_description = "HasAttribute";
                break;
            case CSS::Selector::SimpleSelector::AttributeMatchType::ExactValueMatch:
                attribute_match_type_description = "ExactValueMatch";
                break;
            case CSS::Selector::SimpleSelector::AttributeMatchType::Contains:
                attribute_match_type_description = "Contains";
                break;
            }

            const char* pseudo_class_description = "";
            switch (simple_selector.pseudo_class) {
            case CSS::Selector::SimpleSelector::PseudoClass::Link:
                pseudo_class_description = "Link";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::Visited:
                pseudo_class_description = "Visited";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::None:
                pseudo_class_description = "None";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::Root:
                pseudo_class_description = "Root";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::Focus:
                pseudo_class_description = "Focus";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::Empty:
                pseudo_class_description = "Empty";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::Hover:
                pseudo_class_description = "Hover";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::LastChild:
                pseudo_class_description = "LastChild";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::FirstChild:
                pseudo_class_description = "FirstChild";
                break;
            case CSS::Selector::SimpleSelector::PseudoClass::OnlyChild:
                pseudo_class_description = "OnlyChild";
                break;
            }

            builder.appendff("{}:{}", type_description, simple_selector.value);
            if (simple_selector.pseudo_class != CSS::Selector::SimpleSelector::PseudoClass::None)
                builder.appendff(" pseudo_class={}", pseudo_class_description);
            if (simple_selector.attribute_match_type != CSS::Selector::SimpleSelector::AttributeMatchType::None) {
                builder.appendff(" [{}, name='{}', value='{}']", attribute_match_type_description, simple_selector.attribute_name, simple_selector.attribute_value);
            }

            if (i != complex_selector.compound_selector.size() - 1)
                builder.append(", ");
        }
        builder.append("\n");
    }
}

void dump_rule(const CSS::StyleRule& rule)
{
    StringBuilder builder;
    dump_rule(builder, rule);
    dbgln("{}", builder.string_view());
}

void dump_rule(StringBuilder& builder, const CSS::StyleRule& rule)
{
    builder.append("Rule:\n");
    for (auto& selector : rule.selectors()) {
        dump_selector(builder, selector);
    }
    builder.append("  Declarations:\n");
    for (auto& property : rule.declaration().properties()) {
        builder.appendff("    {}: '{}'\n", CSS::string_from_property_id(property.property_id), property.value->to_string());
    }
}

void dump_sheet(const CSS::StyleSheet& sheet)
{
    StringBuilder builder;
    dump_sheet(builder, sheet);
    dbgln("{}", builder.string_view());
}

void dump_sheet(StringBuilder& builder, const CSS::StyleSheet& sheet)
{
    builder.appendff("StyleSheet{{{}}}: {} rule(s)", &sheet, sheet.rules().size());

    for (auto& rule : sheet.rules()) {
        dump_rule(builder, rule);
    }
}

}
