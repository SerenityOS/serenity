/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/CSS/StyleResolver.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Dump.h>
#include <LibWeb/FontCache.h>
#include <LibWeb/Page/BrowsingContext.h>
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
void StyleResolver::for_each_stylesheet(CascadeOrigin cascade_origin, Callback callback) const
{
    if (cascade_origin == CascadeOrigin::Any || cascade_origin == CascadeOrigin::UserAgent) {
        callback(default_stylesheet());
        if (document().in_quirks_mode())
            callback(quirks_mode_stylesheet());
    }
    if (cascade_origin == CascadeOrigin::Any || cascade_origin == CascadeOrigin::Author) {
        for (auto& sheet : document().style_sheets().sheets()) {
            callback(sheet);
        }
    }
}

Vector<MatchingRule> StyleResolver::collect_matching_rules(DOM::Element const& element, CascadeOrigin declaration_type) const
{
    Vector<MatchingRule> matching_rules;

    size_t style_sheet_index = 0;
    for_each_stylesheet(declaration_type, [&](auto& sheet) {
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

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, StyleValue const& value, DOM::Document& document, bool is_internally_generated_pseudo_property = false)
{
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
            auto& text_decoration = value.as_text_decoration();
            style.set_property(CSS::PropertyID::TextDecorationLine, text_decoration.line());
            style.set_property(CSS::PropertyID::TextDecorationStyle, text_decoration.style());
            style.set_property(CSS::PropertyID::TextDecorationColor, text_decoration.color());
            return;
        }

        style.set_property(CSS::PropertyID::TextDecorationLine, value);
        style.set_property(CSS::PropertyID::TextDecorationStyle, value);
        style.set_property(CSS::PropertyID::TextDecorationColor, value);
        return;
    }

    if (property_id == CSS::PropertyID::Overflow) {
        if (value.is_overflow()) {
            auto& overflow = value.as_overflow();
            style.set_property(CSS::PropertyID::OverflowX, overflow.overflow_x());
            style.set_property(CSS::PropertyID::OverflowY, overflow.overflow_y());
            return;
        }

        style.set_property(CSS::PropertyID::OverflowX, value);
        style.set_property(CSS::PropertyID::OverflowY, value);
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
            auto& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopLeftRadius, PropertyID::BorderTopRightRadius, PropertyID::BorderBottomRightRadius, PropertyID::BorderBottomLeftRadius, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopLeftRadius, value);
        style.set_property(CSS::PropertyID::BorderTopRightRadius, value);
        style.set_property(CSS::PropertyID::BorderBottomRightRadius, value);
        style.set_property(CSS::PropertyID::BorderBottomLeftRadius, value);
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
            auto& border = value.as_border();
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
            auto& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopStyle, PropertyID::BorderRightStyle, PropertyID::BorderBottomStyle, PropertyID::BorderLeftStyle, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopStyle, value);
        style.set_property(CSS::PropertyID::BorderRightStyle, value);
        style.set_property(CSS::PropertyID::BorderBottomStyle, value);
        style.set_property(CSS::PropertyID::BorderLeftStyle, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderWidth) {
        if (value.is_value_list()) {
            auto& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopWidth, PropertyID::BorderRightWidth, PropertyID::BorderBottomWidth, PropertyID::BorderLeftWidth, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopWidth, value);
        style.set_property(CSS::PropertyID::BorderRightWidth, value);
        style.set_property(CSS::PropertyID::BorderBottomWidth, value);
        style.set_property(CSS::PropertyID::BorderLeftWidth, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        if (value.is_value_list()) {
            auto& values_list = value.as_value_list();
            assign_edge_values(PropertyID::BorderTopColor, PropertyID::BorderRightColor, PropertyID::BorderBottomColor, PropertyID::BorderLeftColor, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopColor, value);
        style.set_property(CSS::PropertyID::BorderRightColor, value);
        style.set_property(CSS::PropertyID::BorderBottomColor, value);
        style.set_property(CSS::PropertyID::BorderLeftColor, value);
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
            auto& background = value.as_background();
            set_single_background(background);
            return;
        }
        if (value.is_value_list()) {
            auto& background_list = value.as_value_list().values();
            // FIXME: Handle multiple backgrounds.
            if (!background_list.is_empty()) {
                auto& background = background_list.first();
                if (background.is_background())
                    set_single_background(background.as_background());
            }
            return;
        }

        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatX, value, document, true);
        set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatY, value, document, true);
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundImage) {
        if (value.is_value_list()) {
            auto& background_image_list = value.as_value_list().values();
            // FIXME: Handle multiple backgrounds.
            if (!background_image_list.is_empty()) {
                auto& background_image = background_image_list.first();
                style.set_property(CSS::PropertyID::BackgroundImage, background_image);
            }
            return;
        }

        style.set_property(CSS::PropertyID::BackgroundImage, value);
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundRepeat) {
        if (value.is_value_list()) {
            auto& background_repeat_list = value.as_value_list().values();
            // FIXME: Handle multiple backgrounds.
            if (!background_repeat_list.is_empty()) {
                auto& maybe_background_repeat = background_repeat_list.first();
                if (maybe_background_repeat.is_background_repeat()) {
                    auto& background_repeat = maybe_background_repeat.as_background_repeat();
                    set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, background_repeat.repeat_x(), document, true);
                    set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, background_repeat.repeat_y(), document, true);
                }
            }
            return;
        }
        if (value.is_background_repeat()) {
            auto& background_repeat = value.as_background_repeat();
            set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, background_repeat.repeat_x(), document, true);
            set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, background_repeat.repeat_y(), document, true);
            return;
        }

        set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, value, document, true);
        set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, value, document, true);
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
            auto& values_list = value.as_value_list();
            assign_edge_values(PropertyID::MarginTop, PropertyID::MarginRight, PropertyID::MarginBottom, PropertyID::MarginLeft, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::MarginTop, value);
        style.set_property(CSS::PropertyID::MarginRight, value);
        style.set_property(CSS::PropertyID::MarginBottom, value);
        style.set_property(CSS::PropertyID::MarginLeft, value);
        return;
    }

    if (property_id == CSS::PropertyID::Padding) {
        if (value.is_value_list()) {
            auto& values_list = value.as_value_list();
            assign_edge_values(PropertyID::PaddingTop, PropertyID::PaddingRight, PropertyID::PaddingBottom, PropertyID::PaddingLeft, values_list.values());
            return;
        }

        style.set_property(CSS::PropertyID::PaddingTop, value);
        style.set_property(CSS::PropertyID::PaddingRight, value);
        style.set_property(CSS::PropertyID::PaddingBottom, value);
        style.set_property(CSS::PropertyID::PaddingLeft, value);
        return;
    }

    if (property_id == CSS::PropertyID::ListStyle) {
        if (value.is_list_style()) {
            auto& list_style = value.as_list_style();
            style.set_property(CSS::PropertyID::ListStylePosition, list_style.position());
            style.set_property(CSS::PropertyID::ListStyleImage, list_style.image());
            style.set_property(CSS::PropertyID::ListStyleType, list_style.style_type());
            return;
        }

        style.set_property(CSS::PropertyID::ListStylePosition, value);
        style.set_property(CSS::PropertyID::ListStyleImage, value);
        style.set_property(CSS::PropertyID::ListStyleType, value);
        return;
    }

    if (property_id == CSS::PropertyID::Font) {
        if (value.is_font()) {
            auto& font_shorthand = value.as_font();
            style.set_property(CSS::PropertyID::FontSize, font_shorthand.font_size());
            style.set_property(CSS::PropertyID::FontFamily, font_shorthand.font_families());
            style.set_property(CSS::PropertyID::FontStyle, font_shorthand.font_style());
            style.set_property(CSS::PropertyID::FontWeight, font_shorthand.font_weight());
            style.set_property(CSS::PropertyID::LineHeight, font_shorthand.line_height());
            // FIXME: Implement font-stretch and font-variant
            return;
        }

        style.set_property(CSS::PropertyID::FontSize, value);
        style.set_property(CSS::PropertyID::FontFamily, value);
        style.set_property(CSS::PropertyID::FontStyle, value);
        style.set_property(CSS::PropertyID::FontWeight, value);
        style.set_property(CSS::PropertyID::LineHeight, value);
        // FIXME: Implement font-stretch and font-variant
        return;
    }

    if (property_id == CSS::PropertyID::Flex) {
        if (value.is_flex()) {
            auto& flex = value.as_flex();
            style.set_property(CSS::PropertyID::FlexGrow, flex.grow());
            style.set_property(CSS::PropertyID::FlexShrink, flex.shrink());
            style.set_property(CSS::PropertyID::FlexBasis, flex.basis());
            return;
        }

        style.set_property(CSS::PropertyID::FlexGrow, value);
        style.set_property(CSS::PropertyID::FlexShrink, value);
        style.set_property(CSS::PropertyID::FlexBasis, value);
        return;
    }

    if (property_id == CSS::PropertyID::FlexFlow) {
        if (value.is_flex_flow()) {
            auto& flex_flow = value.as_flex_flow();
            style.set_property(CSS::PropertyID::FlexDirection, flex_flow.flex_direction());
            style.set_property(CSS::PropertyID::FlexWrap, flex_flow.flex_wrap());
            return;
        }

        style.set_property(CSS::PropertyID::FlexDirection, value);
        style.set_property(CSS::PropertyID::FlexWrap, value);
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

        auto custom_property_style = verify_cast<PropertyOwningCSSStyleDeclaration>(match.rule->declaration()).custom_property(custom_property_name);
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

struct MatchingDeclarations {
    Vector<MatchingRule> user_agent_rules;
    Vector<MatchingRule> author_rules;
};

void StyleResolver::cascade_declarations(StyleProperties& style, DOM::Element& element, Vector<MatchingRule> const& matching_rules, CascadeOrigin cascade_origin, bool important) const
{
    for (auto& match : matching_rules) {
        for (auto& property : verify_cast<PropertyOwningCSSStyleDeclaration>(match.rule->declaration()).properties()) {
            if (important != property.important)
                continue;
            auto property_value = property.value;
            if (property.value->is_custom_property()) {
                auto custom_property_name = property.value->as_custom_property().custom_property_name();
                auto resolved = resolve_custom_property(element, custom_property_name);
                if (resolved.has_value()) {
                    property_value = resolved.value().value;
                }
            }
            set_property_expanding_shorthands(style, property.property_id, property_value, m_document);
        }
    }

    if (cascade_origin == CascadeOrigin::Author) {
        if (auto* inline_style = verify_cast<ElementInlineCSSStyleDeclaration>(element.inline_style())) {
            for (auto& property : inline_style->properties()) {
                if (important != property.important)
                    continue;
                set_property_expanding_shorthands(style, property.property_id, property.value, m_document);
            }
        }
    }
}

// https://drafts.csswg.org/css-cascade/#cascading
void StyleResolver::compute_cascaded_values(StyleProperties& style, DOM::Element& element) const
{
    // First, we collect all the CSS rules whose selectors match `element`:
    MatchingRuleSet matching_rule_set;
    matching_rule_set.user_agent_rules = collect_matching_rules(element, CascadeOrigin::UserAgent);
    sort_matching_rules(matching_rule_set.user_agent_rules);
    matching_rule_set.author_rules = collect_matching_rules(element, CascadeOrigin::Author);
    sort_matching_rules(matching_rule_set.author_rules);

    // Then we apply the declarations from the matched rules in cascade order:

    // Normal user agent declarations
    cascade_declarations(style, element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, false);

    // FIXME: Normal user declarations

    // Normal author declarations
    cascade_declarations(style, element, matching_rule_set.author_rules, CascadeOrigin::Author, false);

    // Author presentational hints (NOTE: The spec doesn't say exactly how to prioritize these.)
    element.apply_presentational_hints(style);

    // FIXME: Animation declarations [css-animations-1]

    // Important author declarations
    cascade_declarations(style, element, matching_rule_set.author_rules, CascadeOrigin::Author, true);

    // FIXME: Important user declarations

    // Important user agent declarations
    cascade_declarations(style, element, matching_rule_set.user_agent_rules, CascadeOrigin::UserAgent, true);

    // FIXME: Transition declarations [css-transitions-1]
}

static NonnullRefPtr<StyleValue> get_initial_value(CSS::PropertyID property_id)
{
    auto value = property_initial_value(property_id);
    if (!value)
        return InitialStyleValue::the();
    return value.release_nonnull();
};

static NonnullRefPtr<StyleValue> get_inherit_value(CSS::PropertyID property_id, DOM::Element const& element)
{
    if (!element.parent_element() || !element.parent_element()->specified_css_values())
        return get_initial_value(property_id);
    auto& map = element.parent_element()->specified_css_values()->properties();
    auto it = map.find(property_id);
    VERIFY(it != map.end());
    return *it->value;
};

void StyleResolver::compute_defaulted_property_value(StyleProperties& style, DOM::Element const& element, CSS::PropertyID property_id) const
{
    // FIXME: If we don't know the correct initial value for a property, we fall back to InitialStyleValue.

    auto it = style.m_property_values.find(property_id);
    if (it == style.m_property_values.end()) {
        if (is_inherited_property(property_id))
            style.m_property_values.set(property_id, get_inherit_value(property_id, element));
        else
            style.m_property_values.set(property_id, get_initial_value(property_id));
        return;
    }

    if (it->value->is_initial()) {
        it->value = get_initial_value(property_id);
        return;
    }

    if (it->value->is_inherit()) {
        it->value = get_inherit_value(property_id, element);
        return;
    }
}

// https://drafts.csswg.org/css-cascade/#defaulting
void StyleResolver::compute_defaulted_values(StyleProperties& style, DOM::Element const& element) const
{
    // Walk the list of all known CSS properties and:
    // - Add them to `style` if they are missing.
    // - Resolve `inherit` and `initial` as needed.
    for (auto i = to_underlying(CSS::first_longhand_property_id); i <= to_underlying(CSS::last_longhand_property_id); ++i) {
        auto property_id = (CSS::PropertyID)i;
        compute_defaulted_property_value(style, element, property_id);
    }
}

void StyleResolver::compute_font(StyleProperties& style, DOM::Element const& element) const
{
    // To compute the font, first ensure that we've defaulted the relevant CSS font properties.
    // FIXME: This should be more sophisticated.
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontFamily);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontSize);
    compute_defaulted_property_value(style, element, CSS::PropertyID::FontWeight);

    auto viewport_rect = document().browsing_context()->viewport_rect();

    auto font_size = style.property(CSS::PropertyID::FontSize).value();
    auto font_weight = style.property(CSS::PropertyID::FontWeight).value();

    int weight = Gfx::FontWeight::Regular;
    if (font_weight->is_identifier()) {
        switch (static_cast<IdentifierStyleValue const&>(*font_weight).id()) {
        case CSS::ValueID::Normal:
            weight = Gfx::FontWeight::Regular;
            break;
        case CSS::ValueID::Bold:
            weight = Gfx::FontWeight::Bold;
            break;
        case CSS::ValueID::Lighter:
            // FIXME: This should be relative to the parent.
            weight = Gfx::FontWeight::Regular;
            break;
        case CSS::ValueID::Bolder:
            // FIXME: This should be relative to the parent.
            weight = Gfx::FontWeight::Bold;
            break;
        default:
            break;
        }
    } else if (font_weight->is_numeric()) {
        int font_weight_integer = roundf(static_cast<NumericStyleValue const&>(*font_weight).value());
        if (font_weight_integer <= Gfx::FontWeight::Regular)
            weight = Gfx::FontWeight::Regular;
        else if (font_weight_integer <= Gfx::FontWeight::Bold)
            weight = Gfx::FontWeight::Bold;
        else
            weight = Gfx::FontWeight::Black;
    }
    // FIXME: calc() for font-weight

    bool bold = weight > Gfx::FontWeight::Regular;

    int size = 10;

    if (font_size->is_identifier()) {
        switch (static_cast<const IdentifierStyleValue&>(*font_size).id()) {
        case CSS::ValueID::XxSmall:
        case CSS::ValueID::XSmall:
        case CSS::ValueID::Small:
        case CSS::ValueID::Medium:
            // FIXME: Should be based on "user's default font size"
            size = 10;
            break;
        case CSS::ValueID::Large:
        case CSS::ValueID::XLarge:
        case CSS::ValueID::XxLarge:
        case CSS::ValueID::XxxLarge:
            // FIXME: Should be based on "user's default font size"
            size = 12;
            break;
        case CSS::ValueID::Smaller:
        case CSS::ValueID::Larger:
            // FIXME: Should be based on parent element
            break;
        default:
            break;
        }
    } else {
        float root_font_size = 10;
        if (element.document().document_element() != &element)
            root_font_size = element.document().document_element()->layout_node()->font().presentation_size();

        Gfx::FontMetrics font_metrics;
        if (element.parent_element())
            font_metrics = element.parent_element()->specified_css_values()->computed_font().metrics('M');
        else
            font_metrics = Gfx::FontDatabase::default_font().metrics('M');

        Optional<Length> maybe_length;
        if (font_size->is_length()) {
            maybe_length = font_size->to_length();
            if (maybe_length->is_percentage()) {
                auto parent_font_size = size;
                if (element.parent_element() && element.parent_element()->layout_node() && element.parent_element()->specified_css_values()) {
                    auto value = element.parent_element()->specified_css_values()->property(CSS::PropertyID::FontSize).value();
                    if (value->is_length()) {
                        auto length = static_cast<LengthStyleValue const&>(*value).to_length();
                        if (length.is_absolute() || length.is_relative())
                            parent_font_size = length.to_px(viewport_rect, font_metrics, root_font_size);
                    }
                }

                maybe_length = Length::make_px(maybe_length->raw_value() / 100.0f * (parent_font_size));
            }
        } else if (font_size->is_calculated()) {
            Length length = Length(0, Length::Type::Calculated);
            length.set_calculated_style(verify_cast<CalculatedStyleValue>(font_size.ptr()));
            maybe_length = length;
        }
        if (maybe_length.has_value()) {
            auto calculated_size = maybe_length.value().to_px(viewport_rect, font_metrics, root_font_size);
            if (calculated_size != 0)
                size = calculated_size;
        }
    }

    // FIXME: Implement the full font-matching algorithm: https://www.w3.org/TR/css-fonts-4/#font-matching-algorithm

    // Note: This is modified by the find_font() lambda
    FontSelector font_selector;
    bool monospace = false;

    auto find_font = [&](String const& family) -> RefPtr<Gfx::Font> {
        font_selector = { family, size, weight };

        if (auto found_font = FontCache::the().get(font_selector))
            return found_font;

        if (auto found_font = Gfx::FontDatabase::the().get(family, size, weight))
            return found_font;

        return {};
    };

    // FIXME: Replace hard-coded font names with a relevant call to FontDatabase.
    // Currently, we cannot request the default font's name, or request it at a specific size and weight.
    // So, hard-coded font names it is.
    auto find_generic_font = [&](ValueID font_id) -> RefPtr<Gfx::Font> {
        switch (font_id) {
        case ValueID::Monospace:
        case ValueID::UiMonospace:
            monospace = true;
            return find_font("Csilla");
        case ValueID::Serif:
        case ValueID::SansSerif:
        case ValueID::Cursive:
        case ValueID::Fantasy:
        case ValueID::UiSerif:
        case ValueID::UiSansSerif:
        case ValueID::UiRounded:
            return find_font("Katica");
        default:
            return {};
        }
    };

    RefPtr<Gfx::Font> found_font;

    auto family_value = style.property(PropertyID::FontFamily).value();
    if (family_value->is_value_list()) {
        auto& family_list = static_cast<StyleValueList const&>(*family_value).values();
        for (auto& family : family_list) {
            if (family.is_identifier()) {
                found_font = find_generic_font(family.to_identifier());
            } else if (family.is_string()) {
                found_font = find_font(family.to_string());
            }
            if (found_font)
                break;
        }
    } else if (family_value->is_identifier()) {
        found_font = find_generic_font(family_value->to_identifier());
    } else if (family_value->is_string()) {
        found_font = find_font(family_value->to_string());
    }

    if (!found_font) {
        found_font = StyleProperties::font_fallback(monospace, bold);
    }

    FontCache::the().set(font_selector, *found_font);

    style.set_computed_font(found_font.release_nonnull());
}

void StyleResolver::absolutize_values(StyleProperties& style, DOM::Element const&) const
{
    auto viewport_rect = document().browsing_context()->viewport_rect();
    auto font_metrics = style.computed_font().metrics('M');
    // FIXME: Get the root element font.
    float root_font_size = 10;

    for (auto& it : style.properties()) {
        if (!it.value->is_length())
            continue;
        auto length = it.value->to_length();
        if (length.is_relative()) {
            auto px = length.relative_length_to_px(viewport_rect, font_metrics, root_font_size);
            it.value = LengthStyleValue::create(CSS::Length::make_px(px));
        }
    }
}

NonnullRefPtr<StyleProperties> StyleResolver::resolve_style(DOM::Element& element) const
{
    auto style = StyleProperties::create();
    // 1. Perform the cascade. This produces the "specified style"
    compute_cascaded_values(style, element);

    // 2. Compute the font, since that may be needed for font-relative CSS units
    compute_font(style, element);

    // 3. Absolutize values, turning font/viewport relative lengths into absolute lengths
    absolutize_values(style, element);

    // 4. Default the values, applying inheritance and 'initial' as needed
    compute_defaulted_values(style, element);

    return style;
}
}
