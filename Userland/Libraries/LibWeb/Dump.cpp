/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSMediaRule.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/CSSSupportsRule.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/DOM/Comment.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/SVGBox.h>
#include <LibWeb/Layout/TextNode.h>
#include <stdio.h>

namespace Web {

static void indent(StringBuilder& builder, int levels)
{
    for (int i = 0; i < levels; i++)
        builder.append("  ");
}

void dump_tree(DOM::Node const& node)
{
    StringBuilder builder;
    dump_tree(builder, node);
    dbgln("{}", builder.string_view());
}

void dump_tree(StringBuilder& builder, DOM::Node const& node)
{
    static int indent = 0;
    for (int i = 0; i < indent; ++i)
        builder.append("  ");
    if (is<DOM::Element>(node)) {
        builder.appendff("<{}", verify_cast<DOM::Element>(node).local_name());
        verify_cast<DOM::Element>(node).for_each_attribute([&](auto& name, auto& value) {
            builder.appendff(" {}={}", name, value);
        });
        builder.append(">\n");
    } else if (is<DOM::Text>(node)) {
        builder.appendff("\"{}\"\n", verify_cast<DOM::Text>(node).data());
    } else {
        builder.appendff("{}\n", node.node_name());
    }
    ++indent;
    if (is<DOM::Element>(node) && verify_cast<DOM::Element>(node).shadow_root()) {
        dump_tree(builder, *verify_cast<DOM::Element>(node).shadow_root());
    }
    if (is<DOM::ParentNode>(node)) {
        if (!is<HTML::HTMLTemplateElement>(node)) {
            static_cast<DOM::ParentNode const&>(node).for_each_child([&](auto& child) {
                dump_tree(builder, child);
            });
        } else {
            auto& template_element = verify_cast<HTML::HTMLTemplateElement>(node);
            dump_tree(builder, template_element.content());
        }
    }
    --indent;
}

void dump_tree(Layout::Node const& layout_node, bool show_box_model, bool show_specified_style)
{
    StringBuilder builder;
    dump_tree(builder, layout_node, show_box_model, show_specified_style, true);
    dbgln("{}", builder.string_view());
}

void dump_tree(StringBuilder& builder, Layout::Node const& layout_node, bool show_box_model, bool show_specified_style, bool interactive)
{
    static size_t indent = 0;
    for (size_t i = 0; i < indent; ++i)
        builder.append("  ");

    FlyString tag_name;
    if (layout_node.is_anonymous())
        tag_name = "(anonymous)";
    else if (is<DOM::Element>(layout_node.dom_node()))
        tag_name = verify_cast<DOM::Element>(*layout_node.dom_node()).local_name();
    else
        tag_name = layout_node.dom_node()->node_name();

    String identifier = "";
    if (layout_node.dom_node() && is<DOM::Element>(*layout_node.dom_node())) {
        auto& element = verify_cast<DOM::Element>(*layout_node.dom_node());
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

    char const* nonbox_color_on = "";
    char const* box_color_on = "";
    char const* svg_box_color_on = "";
    char const* positioned_color_on = "";
    char const* floating_color_on = "";
    char const* inline_block_color_on = "";
    char const* line_box_color_on = "";
    char const* fragment_color_on = "";
    char const* flex_color_on = "";
    char const* color_off = "";

    if (interactive) {
        nonbox_color_on = "\033[33m";
        box_color_on = "\033[34m";
        svg_box_color_on = "\033[31m";
        positioned_color_on = "\033[31;1m";
        floating_color_on = "\033[32;1m";
        inline_block_color_on = "\033[36;1m";
        line_box_color_on = "\033[34;1m";
        fragment_color_on = "\033[35;1m";
        flex_color_on = "\033[34;1m";
        color_off = "\033[0m";
    }

    if (!is<Layout::Box>(layout_node)) {
        builder.appendff("{}{}{} <{}{}{}{}>",
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
        auto& box = verify_cast<Layout::Box>(layout_node);
        StringView color_on = is<Layout::SVGBox>(box) ? svg_box_color_on : box_color_on;

        builder.appendff("{}{}{} <{}{}{}{}> ",
            color_on,
            box.class_name().substring_view(13),
            color_off,
            color_on,
            tag_name,
            color_off,
            identifier.characters());

        if (interactive)
            builder.appendff("@{:p} ", &layout_node);

        builder.appendff("at ({},{}) size {}x{}",
            box.absolute_x(),
            box.absolute_y(),
            box.content_width(),
            box.content_height());

        if (box.is_positioned())
            builder.appendff(" {}positioned{}", positioned_color_on, color_off);
        if (box.is_floating())
            builder.appendff(" {}floating{}", floating_color_on, color_off);
        if (box.is_inline_block())
            builder.appendff(" {}inline-block{}", inline_block_color_on, color_off);
        if (box.computed_values().display().is_flex_inside()) {
            StringView direction;
            switch (box.computed_values().flex_direction()) {
            case CSS::FlexDirection::Column:
                direction = "column"sv;
                break;
            case CSS::FlexDirection::ColumnReverse:
                direction = "column-reverse"sv;
                break;
            case CSS::FlexDirection::Row:
                direction = "row"sv;
                break;
            case CSS::FlexDirection::RowReverse:
                direction = "row-reverse"sv;
                break;
            }
            builder.appendff(" {}flex-container({}){}", flex_color_on, direction, color_off);
        }
        if (box.is_flex_item())
            builder.appendff(" {}flex-item{}", flex_color_on, color_off);

        if (show_box_model) {
            // Dump the horizontal box properties
            builder.appendff(" [{}+{}+{} {} {}+{}+{}]",
                box.box_model().margin.left,
                box.box_model().border.left,
                box.box_model().padding.left,
                box.content_width(),
                box.box_model().padding.right,
                box.box_model().border.right,
                box.box_model().margin.right);

            // And the vertical box properties
            builder.appendff(" [{}+{}+{} {} {}+{}+{}]",
                box.box_model().margin.top,
                box.box_model().border.top,
                box.box_model().padding.top,
                box.content_height(),
                box.box_model().padding.bottom,
                box.box_model().border.bottom,
                box.box_model().margin.bottom);
        }

        builder.append("\n");
    }

    if (is<Layout::BlockContainer>(layout_node) && static_cast<Layout::BlockContainer const&>(layout_node).children_are_inline()) {
        auto& block = static_cast<Layout::BlockContainer const&>(layout_node);
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
                    fragment.absolute_rect().to_string());
                if (is<Layout::TextNode>(fragment.layout_node())) {
                    for (size_t i = 0; i < indent; ++i)
                        builder.append("  ");
                    auto& layout_text = static_cast<Layout::TextNode const&>(fragment.layout_node());
                    auto fragment_text = layout_text.text_for_rendering().substring(fragment.start(), fragment.length());
                    builder.appendff("      \"{}\"\n", fragment_text);
                }
            }
        }
    }

    if (show_specified_style && layout_node.dom_node() && layout_node.dom_node()->is_element() && verify_cast<DOM::Element>(layout_node.dom_node())->specified_css_values()) {
        struct NameAndValue {
            String name;
            String value;
        };
        Vector<NameAndValue> properties;
        verify_cast<DOM::Element>(*layout_node.dom_node()).specified_css_values()->for_each_property([&](auto property_id, auto& value) {
            properties.append({ CSS::string_from_property_id(property_id), value.to_string() });
        });
        quick_sort(properties, [](auto& a, auto& b) { return a.name < b.name; });

        for (auto& property : properties) {
            for (size_t i = 0; i < indent; ++i)
                builder.append("    ");
            builder.appendff("  ({}: {})\n", property.name, property.value);
        }
    }

    ++indent;
    layout_node.for_each_child([&](auto& child) {
        dump_tree(builder, child, show_box_model, show_specified_style, interactive);
    });
    --indent;
}

void dump_selector(CSS::Selector const& selector)
{
    StringBuilder builder;
    dump_selector(builder, selector);
    dbgln("{}", builder.string_view());
}

void dump_selector(StringBuilder& builder, CSS::Selector const& selector)
{
    builder.append("  CSS::Selector:\n");

    for (auto& relative_selector : selector.compound_selectors()) {
        builder.append("    ");

        char const* relation_description = "";
        switch (relative_selector.combinator) {
        case CSS::Selector::Combinator::None:
            relation_description = "None";
            break;
        case CSS::Selector::Combinator::ImmediateChild:
            relation_description = "ImmediateChild";
            break;
        case CSS::Selector::Combinator::Descendant:
            relation_description = "Descendant";
            break;
        case CSS::Selector::Combinator::NextSibling:
            relation_description = "AdjacentSibling";
            break;
        case CSS::Selector::Combinator::SubsequentSibling:
            relation_description = "GeneralSibling";
            break;
        case CSS::Selector::Combinator::Column:
            relation_description = "Column";
            break;
        }

        if (*relation_description)
            builder.appendff("{{{}}} ", relation_description);

        for (size_t i = 0; i < relative_selector.simple_selectors.size(); ++i) {
            auto& simple_selector = relative_selector.simple_selectors[i];
            char const* type_description = "Unknown";
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
            case CSS::Selector::SimpleSelector::Type::Attribute:
                type_description = "Attribute";
                break;
            case CSS::Selector::SimpleSelector::Type::PseudoClass:
                type_description = "PseudoClass";
                break;
            case CSS::Selector::SimpleSelector::Type::PseudoElement:
                type_description = "PseudoElement";
                break;
            }

            builder.appendff("{}:{}", type_description, simple_selector.value);

            if (simple_selector.type == CSS::Selector::SimpleSelector::Type::PseudoClass) {
                auto const& pseudo_class = simple_selector.pseudo_class;

                char const* pseudo_class_description = "";
                switch (pseudo_class.type) {
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Link:
                    pseudo_class_description = "Link";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Visited:
                    pseudo_class_description = "Visited";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Active:
                    pseudo_class_description = "Active";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::None:
                    pseudo_class_description = "None";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Root:
                    pseudo_class_description = "Root";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::FirstOfType:
                    pseudo_class_description = "FirstOfType";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::LastOfType:
                    pseudo_class_description = "LastOfType";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::NthChild:
                    pseudo_class_description = "NthChild";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::NthLastChild:
                    pseudo_class_description = "NthLastChild";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Focus:
                    pseudo_class_description = "Focus";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Empty:
                    pseudo_class_description = "Empty";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Hover:
                    pseudo_class_description = "Hover";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::LastChild:
                    pseudo_class_description = "LastChild";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::FirstChild:
                    pseudo_class_description = "FirstChild";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::OnlyChild:
                    pseudo_class_description = "OnlyChild";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Disabled:
                    pseudo_class_description = "Disabled";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Enabled:
                    pseudo_class_description = "Enabled";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Checked:
                    pseudo_class_description = "Checked";
                    break;
                case CSS::Selector::SimpleSelector::PseudoClass::Type::Not:
                    pseudo_class_description = "Not";
                    break;
                }

                builder.appendff(" pseudo_class={}", pseudo_class_description);
                if (pseudo_class.type == CSS::Selector::SimpleSelector::PseudoClass::Type::Not) {
                    builder.append("([");
                    for (auto& selector : pseudo_class.not_selector)
                        dump_selector(builder, selector);
                    builder.append("])");
                } else if ((pseudo_class.type == CSS::Selector::SimpleSelector::PseudoClass::Type::NthChild)
                    || (pseudo_class.type == CSS::Selector::SimpleSelector::PseudoClass::Type::NthLastChild)) {
                    builder.appendff("(step={}, offset={})", pseudo_class.nth_child_pattern.step_size, pseudo_class.nth_child_pattern.offset);
                }
            }

            if (simple_selector.type == CSS::Selector::SimpleSelector::Type::PseudoElement) {
                char const* pseudo_element_description = "";
                switch (simple_selector.pseudo_element) {
                case CSS::Selector::SimpleSelector::PseudoElement::None:
                    pseudo_element_description = "NONE";
                    break;
                case CSS::Selector::SimpleSelector::PseudoElement::Before:
                    pseudo_element_description = "before";
                    break;
                case CSS::Selector::SimpleSelector::PseudoElement::After:
                    pseudo_element_description = "after";
                    break;
                case CSS::Selector::SimpleSelector::PseudoElement::FirstLine:
                    pseudo_element_description = "first-line";
                    break;
                case CSS::Selector::SimpleSelector::PseudoElement::FirstLetter:
                    pseudo_element_description = "first-letter";
                    break;
                }

                builder.appendff(" pseudo_element={}", pseudo_element_description);
            }

            if (simple_selector.type == CSS::Selector::SimpleSelector::Type::Attribute) {
                char const* attribute_match_type_description = "";

                switch (simple_selector.attribute.match_type) {
                case CSS::Selector::SimpleSelector::Attribute::MatchType::None:
                    attribute_match_type_description = "NONE";
                    break;
                case CSS::Selector::SimpleSelector::Attribute::MatchType::HasAttribute:
                    attribute_match_type_description = "HasAttribute";
                    break;
                case CSS::Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch:
                    attribute_match_type_description = "ExactValueMatch";
                    break;
                case CSS::Selector::SimpleSelector::Attribute::MatchType::ContainsWord:
                    attribute_match_type_description = "ContainsWord";
                    break;
                case CSS::Selector::SimpleSelector::Attribute::MatchType::ContainsString:
                    attribute_match_type_description = "ContainsString";
                    break;
                case CSS::Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment:
                    attribute_match_type_description = "StartsWithSegment";
                    break;
                case CSS::Selector::SimpleSelector::Attribute::MatchType::StartsWithString:
                    attribute_match_type_description = "StartsWithString";
                    break;
                case CSS::Selector::SimpleSelector::Attribute::MatchType::EndsWithString:
                    attribute_match_type_description = "EndsWithString";
                    break;
                }

                builder.appendff(" [{}, name='{}', value='{}']", attribute_match_type_description, simple_selector.attribute.name, simple_selector.attribute.value);
            }

            if (i != relative_selector.simple_selectors.size() - 1)
                builder.append(", ");
        }
        builder.append("\n");
    }
}

void dump_rule(CSS::CSSRule const& rule)
{
    StringBuilder builder;
    dump_rule(builder, rule);
    dbgln("{}", builder.string_view());
}

void dump_rule(StringBuilder& builder, CSS::CSSRule const& rule, int indent_levels)
{
    indent(builder, indent_levels);
    builder.appendff("{}:\n", rule.class_name());

    switch (rule.type()) {
    case CSS::CSSRule::Type::Style:
        dump_style_rule(builder, verify_cast<CSS::CSSStyleRule const>(rule), indent_levels);
        break;
    case CSS::CSSRule::Type::Import:
        dump_import_rule(builder, verify_cast<CSS::CSSImportRule const>(rule), indent_levels);
        break;
    case CSS::CSSRule::Type::Media:
        dump_media_rule(builder, verify_cast<CSS::CSSMediaRule const>(rule), indent_levels);
        break;
    case CSS::CSSRule::Type::Supports:
        dump_supports_rule(builder, verify_cast<CSS::CSSSupportsRule const>(rule), indent_levels);
        break;
    case CSS::CSSRule::Type::__Count:
        VERIFY_NOT_REACHED();
    }
}

void dump_import_rule(StringBuilder& builder, CSS::CSSImportRule const& rule, int indent_levels)
{
    indent(builder, indent_levels);
    builder.appendff("  Document URL: {}\n", rule.url());
}

void dump_media_rule(StringBuilder& builder, CSS::CSSMediaRule const& media, int indent_levels)
{
    indent(builder, indent_levels);
    builder.appendff("  Media: {}\n  Rules ({}):\n", media.condition_text(), media.css_rules().length());

    for (auto& rule : media.css_rules()) {
        dump_rule(builder, rule, indent_levels + 1);
    }
}

void dump_supports_rule(StringBuilder& builder, CSS::CSSSupportsRule const& supports, int indent_levels)
{
    indent(builder, indent_levels);
    builder.appendff("  Supports: {}\n  Rules ({}):\n", supports.condition_text(), supports.css_rules().length());

    for (auto& rule : supports.css_rules()) {
        dump_rule(builder, rule, indent_levels + 1);
    }
}

void dump_style_rule(StringBuilder& builder, CSS::CSSStyleRule const& rule, int indent_levels)
{
    for (auto& selector : rule.selectors()) {
        dump_selector(builder, selector);
    }
    indent(builder, indent_levels);
    builder.append("  Declarations:\n");
    auto& style_declaration = verify_cast<CSS::PropertyOwningCSSStyleDeclaration>(rule.declaration());
    for (auto& property : style_declaration.properties()) {
        indent(builder, indent_levels);
        builder.appendff("    {}: '{}'", CSS::string_from_property_id(property.property_id), property.value->to_string());
        if (property.important)
            builder.append(" \033[31;1m!important\033[0m");
        builder.append('\n');
    }
    for (auto& property : style_declaration.custom_properties()) {
        indent(builder, indent_levels);
        builder.appendff("    {}: '{}'", property.key, property.value.value->to_string());
        if (property.value.important)
            builder.append(" \033[31;1m!important\033[0m");
        builder.append('\n');
    }
}

void dump_sheet(CSS::StyleSheet const& sheet)
{
    StringBuilder builder;
    dump_sheet(builder, sheet);
    dbgln("{}", builder.string_view());
}

void dump_sheet(StringBuilder& builder, CSS::StyleSheet const& sheet)
{
    auto& css_stylesheet = verify_cast<CSS::CSSStyleSheet>(sheet);

    builder.appendff("CSSStyleSheet{{{}}}: {} rule(s)\n", &sheet, css_stylesheet.rules().length());

    for (auto& rule : css_stylesheet.rules()) {
        dump_rule(builder, rule);
    }
}

}
