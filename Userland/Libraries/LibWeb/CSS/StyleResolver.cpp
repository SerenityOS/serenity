/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Dump.h>
#include <ctype.h>
#include <stdio.h>

namespace Web::CSS {

StyleResolver::StyleResolver(DOM::Document& document)
    : m_document(document)
{
}

StyleResolver::~StyleResolver()
{
}

static StyleSheet& default_stylesheet()
{
    static StyleSheet* sheet;
    if (!sheet) {
        extern char const default_stylesheet_source[];
        String css = default_stylesheet_source;
        sheet = parse_css(CSS::ParsingContext(), css).leak_ref();
    }
    return *sheet;
}

static StyleSheet& quirks_mode_stylesheet()
{
    static StyleSheet* sheet;
    if (!sheet) {
        extern char const quirks_mode_stylesheet_source[];
        String css = quirks_mode_stylesheet_source;
        sheet = parse_css(CSS::ParsingContext(), css).leak_ref();
    }
    return *sheet;
}

template<typename Callback>
void StyleResolver::for_each_stylesheet(Callback callback) const
{
    callback(default_stylesheet());
    if (document().in_quirks_mode())
        callback(quirks_mode_stylesheet());
    for (auto& sheet : document().style_sheets().sheets()) {
        callback(sheet);
    }
}

Vector<MatchingRule> StyleResolver::collect_matching_rules(DOM::Element const& element) const
{
    Vector<MatchingRule> matching_rules;

    size_t style_sheet_index = 0;
    for_each_stylesheet([&](auto& sheet) {
        size_t rule_index = 0;
        static_cast<CSSStyleSheet const&>(sheet).for_each_effective_style_rule([&](auto& rule) {
            size_t selector_index = 0;
            for (auto& selector : rule.selectors()) {
                if (SelectorEngine::matches(selector, element)) {
                    matching_rules.append({ rule, style_sheet_index, rule_index, selector_index, selector.specificity() });
                    break;
                }
                ++selector_index;
            }
            ++rule_index;
        });
        ++style_sheet_index;
    });

    return matching_rules;
}

void StyleResolver::sort_matching_rules(Vector<MatchingRule>& matching_rules) const
{
    quick_sort(matching_rules, [&](MatchingRule& a, MatchingRule& b) {
        auto& a_selector = a.rule->selectors()[a.selector_index];
        auto& b_selector = b.rule->selectors()[b.selector_index];
        auto a_specificity = a_selector.specificity();
        auto b_specificity = b_selector.specificity();
        if (a_selector.specificity() == b_selector.specificity()) {
            if (a.style_sheet_index == b.style_sheet_index)
                return a.rule_index < b.rule_index;
            return a.style_sheet_index < b.style_sheet_index;
        }
        return a_specificity < b_specificity;
    });
}

bool StyleResolver::is_inherited_property(CSS::PropertyID property_id)
{
    static HashTable<CSS::PropertyID> inherited_properties;
    if (inherited_properties.is_empty()) {
        inherited_properties.set(CSS::PropertyID::BorderCollapse);
        inherited_properties.set(CSS::PropertyID::BorderSpacing);
        inherited_properties.set(CSS::PropertyID::Color);
        inherited_properties.set(CSS::PropertyID::FontFamily);
        inherited_properties.set(CSS::PropertyID::FontSize);
        inherited_properties.set(CSS::PropertyID::FontStyle);
        inherited_properties.set(CSS::PropertyID::FontVariant);
        inherited_properties.set(CSS::PropertyID::FontWeight);
        inherited_properties.set(CSS::PropertyID::LetterSpacing);
        inherited_properties.set(CSS::PropertyID::LineHeight);
        inherited_properties.set(CSS::PropertyID::ListStyle);
        inherited_properties.set(CSS::PropertyID::ListStyleImage);
        inherited_properties.set(CSS::PropertyID::ListStylePosition);
        inherited_properties.set(CSS::PropertyID::ListStyleType);
        inherited_properties.set(CSS::PropertyID::TextAlign);
        inherited_properties.set(CSS::PropertyID::TextIndent);
        inherited_properties.set(CSS::PropertyID::TextTransform);
        inherited_properties.set(CSS::PropertyID::Visibility);
        inherited_properties.set(CSS::PropertyID::WhiteSpace);
        inherited_properties.set(CSS::PropertyID::WordSpacing);

        // FIXME: This property is not supposed to be inherited, but we currently
        //        rely on inheritance to propagate decorations into line boxes.
        inherited_properties.set(CSS::PropertyID::TextDecorationLine);
    }
    return inherited_properties.contains(property_id);
}

enum class Edge {
    Top,
    Right,
    Bottom,
    Left,
    All,
};

static bool contains(Edge a, Edge b)
{
    return a == b || b == Edge::All;
}

static inline bool is_font_family(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;
    if (value.is_string())
        return true;
    switch (value.to_identifier()) {
    case ValueID::Cursive:
    case ValueID::Fantasy:
    case ValueID::Monospace:
    case ValueID::Serif:
    case ValueID::SansSerif:
    case ValueID::UiMonospace:
    case ValueID::UiRounded:
    case ValueID::UiSerif:
    case ValueID::UiSansSerif:
        return true;
    default:
        return false;
    }
}

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, StyleValue const& value, DOM::Document& document, bool is_internally_generated_pseudo_property = false)
{
    CSS::ParsingContext context(document);

    if (is_pseudo_property(property_id) && !is_internally_generated_pseudo_property) {
        dbgln("Ignoring non-internally-generated pseudo property: {}", string_from_property_id(property_id));
        return;
    }

    auto assign_edge_values = [&style](PropertyID top_property, PropertyID right_property, PropertyID bottom_property, PropertyID left_property, auto const& values) {
        if (values.size() == 4) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[1]);
            style.set_property(bottom_property, values[2]);
            style.set_property(left_property, values[3]);
        } else if (values.size() == 3) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[1]);
            style.set_property(bottom_property, values[2]);
            style.set_property(left_property, values[1]);
        } else if (values.size() == 2) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[1]);
            style.set_property(bottom_property, values[0]);
            style.set_property(left_property, values[1]);
        } else if (values.size() == 1) {
            style.set_property(top_property, values[0]);
            style.set_property(right_property, values[0]);
            style.set_property(bottom_property, values[0]);
            style.set_property(left_property, values[0]);
        }
    };

    if (property_id == CSS::PropertyID::TextDecoration) {
        if (value.is_text_decoration()) {
            auto& text_decoration = static_cast<TextDecorationStyleValue const&>(value);
            style.set_property(CSS::PropertyID::TextDecorationLine, text_decoration.line());
            style.set_property(CSS::PropertyID::TextDecorationStyle, text_decoration.style());
            style.set_property(CSS::PropertyID::TextDecorationColor, text_decoration.color());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::TextDecorationLine, value);
            style.set_property(CSS::PropertyID::TextDecorationStyle, value);
            style.set_property(CSS::PropertyID::TextDecorationColor, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::Overflow) {
        if (value.is_overflow()) {
            auto& overflow = static_cast<OverflowStyleValue const&>(value);
            style.set_property(CSS::PropertyID::OverflowX, overflow.overflow_x());
            style.set_property(CSS::PropertyID::OverflowY, overflow.overflow_y());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::OverflowX, value);
            style.set_property(CSS::PropertyID::OverflowY, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::Border) {
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderTop, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderRight, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderBottom, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderLeft, value, document);
        // FIXME: Also reset border-image, in line with the spec: https://www.w3.org/TR/css-backgrounds-3/#border-shorthands
        return;
    }

    if (property_id == CSS::PropertyID::BorderRadius) {
        if (value.is_value_list()) {
            auto& values_list = static_cast<StyleValueList const&>(value);
            assign_edge_values(PropertyID::BorderTopLeftRadius, PropertyID::BorderTopRightRadius, PropertyID::BorderBottomRightRadius, PropertyID::BorderBottomLeftRadius, values_list.values());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::BorderTopLeftRadius, value);
            style.set_property(CSS::PropertyID::BorderTopRightRadius, value);
            style.set_property(CSS::PropertyID::BorderBottomRightRadius, value);
            style.set_property(CSS::PropertyID::BorderBottomLeftRadius, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderTop
        || property_id == CSS::PropertyID::BorderRight
        || property_id == CSS::PropertyID::BorderBottom
        || property_id == CSS::PropertyID::BorderLeft) {

        Edge edge = Edge::All;
        switch (property_id) {
        case CSS::PropertyID::BorderTop:
            edge = Edge::Top;
            break;
        case CSS::PropertyID::BorderRight:
            edge = Edge::Right;
            break;
        case CSS::PropertyID::BorderBottom:
            edge = Edge::Bottom;
            break;
        case CSS::PropertyID::BorderLeft:
            edge = Edge::Left;
            break;
        default:
            break;
        }

        if (value.is_border()) {
            auto& border = static_cast<BorderStyleValue const&>(value);
            if (contains(Edge::Top, edge)) {
                style.set_property(PropertyID::BorderTopWidth, border.border_width());
                style.set_property(PropertyID::BorderTopStyle, border.border_style());
                style.set_property(PropertyID::BorderTopColor, border.border_color());
            }
            if (contains(Edge::Right, edge)) {
                style.set_property(PropertyID::BorderRightWidth, border.border_width());
                style.set_property(PropertyID::BorderRightStyle, border.border_style());
                style.set_property(PropertyID::BorderRightColor, border.border_color());
            }
            if (contains(Edge::Bottom, edge)) {
                style.set_property(PropertyID::BorderBottomWidth, border.border_width());
                style.set_property(PropertyID::BorderBottomStyle, border.border_style());
                style.set_property(PropertyID::BorderBottomColor, border.border_color());
            }
            if (contains(Edge::Left, edge)) {
                style.set_property(PropertyID::BorderLeftWidth, border.border_width());
                style.set_property(PropertyID::BorderLeftStyle, border.border_style());
                style.set_property(PropertyID::BorderLeftColor, border.border_color());
            }
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderStyle) {
        if (value.is_value_list()) {
            auto& values_list = static_cast<StyleValueList const&>(value);
            assign_edge_values(PropertyID::BorderTopStyle, PropertyID::BorderRightStyle, PropertyID::BorderBottomStyle, PropertyID::BorderLeftStyle, values_list.values());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::BorderTopStyle, value);
            style.set_property(CSS::PropertyID::BorderRightStyle, value);
            style.set_property(CSS::PropertyID::BorderBottomStyle, value);
            style.set_property(CSS::PropertyID::BorderLeftStyle, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderWidth) {
        if (value.is_value_list()) {
            auto& values_list = static_cast<StyleValueList const&>(value);
            assign_edge_values(PropertyID::BorderTopWidth, PropertyID::BorderRightWidth, PropertyID::BorderBottomWidth, PropertyID::BorderLeftWidth, values_list.values());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::BorderTopWidth, value);
            style.set_property(CSS::PropertyID::BorderRightWidth, value);
            style.set_property(CSS::PropertyID::BorderBottomWidth, value);
            style.set_property(CSS::PropertyID::BorderLeftWidth, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        if (value.is_value_list()) {
            auto& values_list = static_cast<StyleValueList const&>(value);
            assign_edge_values(PropertyID::BorderTopColor, PropertyID::BorderRightColor, PropertyID::BorderBottomColor, PropertyID::BorderLeftColor, values_list.values());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::BorderTopColor, value);
            style.set_property(CSS::PropertyID::BorderRightColor, value);
            style.set_property(CSS::PropertyID::BorderBottomColor, value);
            style.set_property(CSS::PropertyID::BorderLeftColor, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::Background) {
        auto set_single_background = [&](CSS::BackgroundStyleValue const& background) {
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, background.color(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, background.image(), document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatX, background.repeat_x(), document, true);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatY, background.repeat_y(), document, true);
        };

        if (value.is_background()) {
            auto& background = static_cast<CSS::BackgroundStyleValue const&>(value);
            set_single_background(background);
            return;
        }
        if (value.is_value_list()) {
            auto& background_list = static_cast<CSS::StyleValueList const&>(value).values();
            // FIXME: Handle multiple backgrounds.
            if (!background_list.is_empty()) {
                auto& background = background_list.first();
                if (background.is_background())
                    set_single_background(static_cast<CSS::BackgroundStyleValue const&>(background));
            }
            return;
        }
        if (value.is_builtin()) {
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, value, document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, value, document);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatX, value, document, true);
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatY, value, document, true);
            return;
        }

        return;
    }

    if (property_id == CSS::PropertyID::BackgroundImage) {
        if (value.is_value_list()) {
            auto& background_image_list = static_cast<CSS::StyleValueList const&>(value).values();
            // FIXME: Handle multiple backgrounds.
            if (!background_image_list.is_empty()) {
                auto& background_image = background_image_list.first();
                style.set_property(CSS::PropertyID::BackgroundImage, background_image);
            }
            return;
        }
        if (value.is_builtin() || value.is_image() || value.to_identifier() == ValueID::None) {
            style.set_property(CSS::PropertyID::BackgroundImage, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundRepeat) {
        if (value.is_value_list()) {
            auto& background_repeat_list = static_cast<CSS::StyleValueList const&>(value).values();
            // FIXME: Handle multiple backgrounds.
            if (!background_repeat_list.is_empty()) {
                auto& maybe_background_repeat = background_repeat_list.first();
                if (maybe_background_repeat.is_background_repeat()) {
                    auto& background_repeat = static_cast<BackgroundRepeatStyleValue const&>(maybe_background_repeat);
                    set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, background_repeat.repeat_x(), document, true);
                    set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, background_repeat.repeat_y(), document, true);
                }
            }
            return;
        }
        if (value.is_background_repeat()) {
            auto& background_repeat = static_cast<BackgroundRepeatStyleValue const&>(value);
            set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, background_repeat.repeat_x(), document, true);
            set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, background_repeat.repeat_y(), document, true);
            return;
        }
        if (value.is_builtin()) {
            set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, value, document, true);
            set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, value, document, true);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundRepeatX || property_id == CSS::PropertyID::BackgroundRepeatY) {
        auto value_id = value.to_identifier();
        if (value_id == CSS::ValueID::RepeatX || value_id == CSS::ValueID::RepeatY)
            return;

        style.set_property(property_id, value);
        return;
    }

    if (property_id == CSS::PropertyID::Margin) {
        if (value.is_value_list()) {
            auto& values_list = static_cast<StyleValueList const&>(value);
            assign_edge_values(PropertyID::MarginTop, PropertyID::MarginRight, PropertyID::MarginBottom, PropertyID::MarginLeft, values_list.values());
            return;
        }
        if (value.is_length() || value.is_builtin()) {
            style.set_property(CSS::PropertyID::MarginTop, value);
            style.set_property(CSS::PropertyID::MarginRight, value);
            style.set_property(CSS::PropertyID::MarginBottom, value);
            style.set_property(CSS::PropertyID::MarginLeft, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::Padding) {
        if (value.is_value_list()) {
            auto& values_list = static_cast<StyleValueList const&>(value);
            assign_edge_values(PropertyID::PaddingTop, PropertyID::PaddingRight, PropertyID::PaddingBottom, PropertyID::PaddingLeft, values_list.values());
            return;
        }
        if (value.is_length() || value.is_builtin()) {
            style.set_property(CSS::PropertyID::PaddingTop, value);
            style.set_property(CSS::PropertyID::PaddingRight, value);
            style.set_property(CSS::PropertyID::PaddingBottom, value);
            style.set_property(CSS::PropertyID::PaddingLeft, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::ListStyle) {
        if (value.is_list_style()) {
            auto& list_style = static_cast<CSS::ListStyleStyleValue const&>(value);
            style.set_property(CSS::PropertyID::ListStylePosition, list_style.position());
            style.set_property(CSS::PropertyID::ListStyleImage, list_style.image());
            style.set_property(CSS::PropertyID::ListStyleType, list_style.style_type());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::ListStylePosition, value);
            style.set_property(CSS::PropertyID::ListStyleImage, value);
            style.set_property(CSS::PropertyID::ListStyleType, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::Font) {
        if (value.is_font()) {
            auto& font_shorthand = static_cast<CSS::FontStyleValue const&>(value);
            style.set_property(CSS::PropertyID::FontSize, font_shorthand.font_size());
            // FIXME: Support multiple font-families
            style.set_property(CSS::PropertyID::FontFamily, font_shorthand.font_families().first());
            style.set_property(CSS::PropertyID::FontStyle, font_shorthand.font_style());
            style.set_property(CSS::PropertyID::FontWeight, font_shorthand.font_weight());
            style.set_property(CSS::PropertyID::LineHeight, font_shorthand.line_height());
            // FIXME: Implement font-stretch and font-variant
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::FontSize, value);
            // FIXME: Support multiple font-families
            style.set_property(CSS::PropertyID::FontFamily, value);
            style.set_property(CSS::PropertyID::FontStyle, value);
            style.set_property(CSS::PropertyID::FontWeight, value);
            style.set_property(CSS::PropertyID::LineHeight, value);
            // FIXME: Implement font-stretch and font-variant
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::FontFamily) {
        if (value.is_component_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            // FIXME: Handle multiple font-families separated by commas, for fallback purposes.
            for (auto& part : parts) {
                auto value = Parser::parse_css_value(context, property_id, part);
                if (!value)
                    return;
                if (is_font_family(*value))
                    style.set_property(CSS::PropertyID::FontFamily, *value);
                break;
            }
            return;
        }

        style.set_property(CSS::PropertyID::FontFamily, value);
        return;
    }

    if (property_id == CSS::PropertyID::Flex) {
        if (value.is_flex()) {
            auto& flex = static_cast<CSS::FlexStyleValue const&>(value);
            style.set_property(CSS::PropertyID::FlexGrow, flex.grow());
            style.set_property(CSS::PropertyID::FlexShrink, flex.shrink());
            style.set_property(CSS::PropertyID::FlexBasis, flex.basis());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::FlexGrow, value);
            style.set_property(CSS::PropertyID::FlexShrink, value);
            style.set_property(CSS::PropertyID::FlexBasis, value);
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::FlexFlow) {
        if (value.is_flex_flow()) {
            auto& flex_flow = static_cast<FlexFlowStyleValue const&>(value);
            style.set_property(CSS::PropertyID::FlexDirection, flex_flow.flex_direction());
            style.set_property(CSS::PropertyID::FlexWrap, flex_flow.flex_wrap());
            return;
        }
        if (value.is_builtin()) {
            style.set_property(CSS::PropertyID::FlexDirection, value);
            style.set_property(CSS::PropertyID::FlexWrap, value);
            return;
        }
        return;
    }

    if (value.is_component_value_list()) {
        dbgln("Values list for CSS property '{}' went unhandled. List: '{}'", string_from_property_id(property_id), value.to_string());
        return;
    }

    style.set_property(property_id, value);
}

StyleResolver::CustomPropertyResolutionTuple StyleResolver::resolve_custom_property_with_specificity(DOM::Element& element, String const& custom_property_name) const
{
    if (auto maybe_property = element.resolve_custom_property(custom_property_name); maybe_property.has_value())
        return maybe_property.value();

    auto parent_element = element.parent_element();
    CustomPropertyResolutionTuple parent_resolved {};
    if (parent_element)
        parent_resolved = resolve_custom_property_with_specificity(*parent_element, custom_property_name);

    auto matching_rules = collect_matching_rules(element);
    sort_matching_rules(matching_rules);

    for (int i = matching_rules.size() - 1; i >= 0; --i) {
        auto& match = matching_rules[i];
        if (match.specificity < parent_resolved.specificity)
            continue;

        auto custom_property_style = match.rule->declaration().custom_property(custom_property_name);
        if (custom_property_style.has_value()) {
            element.add_custom_property(custom_property_name, { custom_property_style.value(), match.specificity });
            return { custom_property_style.value(), match.specificity };
        }
    }

    return parent_resolved;
}

Optional<StyleProperty> StyleResolver::resolve_custom_property(DOM::Element& element, String const& custom_property_name) const
{
    auto resolved_with_specificity = resolve_custom_property_with_specificity(element, custom_property_name);

    return resolved_with_specificity.style;
}

NonnullRefPtr<StyleProperties> StyleResolver::resolve_style(DOM::Element& element) const
{
    auto style = StyleProperties::create();

    if (auto* parent_style = element.parent_element() ? element.parent_element()->specified_css_values() : nullptr) {
        parent_style->for_each_property([&](auto property_id, auto& value) {
            if (is_inherited_property(property_id))
                set_property_expanding_shorthands(style, property_id, value, m_document);
        });
    }

    element.apply_presentational_hints(*style);

    auto matching_rules = collect_matching_rules(element);
    sort_matching_rules(matching_rules);

    for (auto& match : matching_rules) {
        for (auto& property : match.rule->declaration().properties()) {
            auto property_value = property.value;
            if (property.value->is_custom_property()) {
                auto prop = reinterpret_cast<CSS::CustomStyleValue const*>(property.value.ptr());
                auto custom_prop_name = prop->custom_property_name();
                auto resolved = resolve_custom_property(element, custom_prop_name);
                if (resolved.has_value()) {
                    property_value = resolved.value().value;
                }
            }
            set_property_expanding_shorthands(style, property.property_id, property_value, m_document);
        }
    }

    if (auto* inline_style = element.inline_style()) {
        for (auto& property : inline_style->properties()) {
            set_property_expanding_shorthands(style, property.property_id, property.value, m_document);
        }
    }

    return style;
}

}
