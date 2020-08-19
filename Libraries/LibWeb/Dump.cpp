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

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Layout/LayoutText.h>
#include <stdio.h>

namespace Web {

void dump_tree(const DOM::Node& node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        dbgprintf("  ");
    if (is<DOM::Document>(node)) {
        dbgprintf("*Document*\n");
    } else if (is<DOM::Element>(node)) {
        dbgprintf("<%s", downcast<DOM::Element>(node).local_name().characters());
        downcast<DOM::Element>(node).for_each_attribute([](auto& name, auto& value) {
            dbgprintf(" %s=%s", name.characters(), value.characters());
        });
        dbgprintf(">\n");
    } else if (is<DOM::Text>(node)) {
        dbgprintf("\"%s\"\n", downcast<DOM::Text>(node).data().characters());
    } else if (is<DOM::DocumentType>(node)) {
        dbgprintf("<!DOCTYPE html>\n");
    } else if (is<DOM::Comment>(node)) {
        dbgprintf("<!--%s-->\n", downcast<DOM::Comment>(node).data().characters());
    } else if (is<DOM::DocumentFragment>(node)) {
        dbgprintf("#document-fragment\n");
    }
    ++indent;
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

void dump_tree(const LayoutNode& layout_node)
{
    static size_t indent = 0;
    for (size_t i = 0; i < indent; ++i)
        dbgprintf("    ");

    FlyString tag_name;
    if (layout_node.is_anonymous())
        tag_name = "(anonymous)";
    else if (is<DOM::Text>(layout_node.node()))
        tag_name = "#text";
    else if (is<DOM::Document>(layout_node.node()))
        tag_name = "#document";
    else if (is<DOM::Element>(layout_node.node()))
        tag_name = downcast<DOM::Element>(*layout_node.node()).local_name();
    else
        tag_name = "???";

    String identifier = "";
    if (layout_node.node() && is<DOM::Element>(*layout_node.node())) {
        auto& element = downcast<DOM::Element>(*layout_node.node());
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

    if (!layout_node.is_box()) {
        dbgprintf("%s {\033[33m%s\033[0m%s}\n", layout_node.class_name(), tag_name.characters(), identifier.characters());
    } else {
        auto& layout_box = downcast<LayoutBox>(layout_node);
        dbgprintf("%s {\033[34m%s\033[0m%s} at (%g,%g) size %gx%g",
            layout_box.class_name(),
            tag_name.characters(),
            identifier.characters(),
            layout_box.absolute_x(),
            layout_box.absolute_y(),
            layout_box.width(),
            layout_box.height());

        // Dump the horizontal box properties
        dbgprintf(" [%g+%g+%g %g %g+%g+%g]",
            layout_box.box_model().margin.left.to_px(layout_box),
            layout_box.box_model().border.left.to_px(layout_box),
            layout_box.box_model().padding.left.to_px(layout_box),
            layout_box.width(),
            layout_box.box_model().padding.right.to_px(layout_box),
            layout_box.box_model().border.right.to_px(layout_box),
            layout_box.box_model().margin.right.to_px(layout_box));

        // And the vertical box properties
        dbgprintf(" [%g+%g+%g %g %g+%g+%g]",
            layout_box.box_model().margin.top.to_px(layout_box),
            layout_box.box_model().border.top.to_px(layout_box),
            layout_box.box_model().padding.top.to_px(layout_box),
            layout_box.height(),
            layout_box.box_model().padding.bottom.to_px(layout_box),
            layout_box.box_model().border.bottom.to_px(layout_box),
            layout_box.box_model().margin.bottom.to_px(layout_box));

        dbgprintf("\n");
    }

    if (layout_node.is_block() && static_cast<const LayoutBlock&>(layout_node).children_are_inline()) {
        auto& block = static_cast<const LayoutBlock&>(layout_node);
        for (size_t i = 0; i < indent; ++i)
            dbgprintf("    ");
        dbgprintf("  Line boxes (%d):\n", block.line_boxes().size());
        for (size_t line_box_index = 0; line_box_index < block.line_boxes().size(); ++line_box_index) {
            auto& line_box = block.line_boxes()[line_box_index];
            for (size_t i = 0; i < indent; ++i)
                dbgprintf("    ");
            dbgprintf("    [%d] width: %g\n", line_box_index, line_box.width());
            for (size_t fragment_index = 0; fragment_index < line_box.fragments().size(); ++fragment_index) {
                auto& fragment = line_box.fragments()[fragment_index];
                for (size_t i = 0; i < indent; ++i)
                    dbgprintf("    ");
                dbgprintf("      [%d] layout_node: %s{%p}, start: %d, length: %d, rect: %s\n",
                    fragment_index,
                    fragment.layout_node().class_name(),
                    &fragment.layout_node(),
                    fragment.start(),
                    fragment.length(),
                    fragment.absolute_rect().to_string().characters());
                if (fragment.layout_node().is_text()) {
                    for (size_t i = 0; i < indent; ++i)
                        dbgprintf("    ");
                    auto& layout_text = static_cast<const LayoutText&>(fragment.layout_node());
                    auto fragment_text = layout_text.text_for_rendering().substring(fragment.start(), fragment.length());
                    dbgprintf("        text: \"%s\"\n", fragment_text.characters());
                }
            }
        }
    }

    struct NameAndValue {
        String name;
        String value;
    };
    Vector<NameAndValue> properties;
    layout_node.specified_style().for_each_property([&](auto property_id, auto& value) {
        properties.append({ CSS::string_from_property_id(property_id), value.to_string() });
    });
    quick_sort(properties, [](auto& a, auto& b) { return a.name < b.name; });

    for (auto& property : properties) {
        for (size_t i = 0; i < indent; ++i)
            dbgprintf("    ");
        dbgprintf("  (%s: %s)\n", property.name.characters(), property.value.characters());
    }

    ++indent;
    layout_node.for_each_child([](auto& child) {
        dump_tree(child);
    });
    --indent;
}

void dump_selector(const CSS::Selector& selector)
{
    dbgprintf("  CSS::Selector:\n");

    for (auto& complex_selector : selector.complex_selectors()) {
        dbgprintf("    ");

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
            dbgprintf("{%s} ", relation_description);

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

            dbgprintf("%s:%s", type_description, simple_selector.value.characters());
            if (simple_selector.pseudo_class != CSS::Selector::SimpleSelector::PseudoClass::None)
                dbgprintf(" pseudo_class=%s", pseudo_class_description);
            if (simple_selector.attribute_match_type != CSS::Selector::SimpleSelector::AttributeMatchType::None) {
                dbgprintf(" [%s, name='%s', value='%s']", attribute_match_type_description, simple_selector.attribute_name.characters(), simple_selector.attribute_value.characters());
            }

            if (i != complex_selector.compound_selector.size() - 1)
                dbgprintf(", ");
        }
        dbgprintf("\n");
    }
}

void dump_rule(const CSS::StyleRule& rule)
{
    dbgprintf("Rule:\n");
    for (auto& selector : rule.selectors()) {
        dump_selector(selector);
    }
    dbgprintf("  Declarations:\n");
    for (auto& property : rule.declaration().properties()) {
        dbgprintf("    %s: '%s'\n", CSS::string_from_property_id(property.property_id), property.value->to_string().characters());
    }
}

void dump_sheet(const CSS::StyleSheet& sheet)
{
    dbgprintf("StyleSheet{%p}: %d rule(s)\n", &sheet, sheet.rules().size());

    for (auto& rule : sheet.rules()) {
        dump_rule(rule);
    }
}

}
