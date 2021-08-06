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
        if (!is<CSSStyleSheet>(sheet))
            return;
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

static Vector<String> split_on_whitespace(StringView const& string)
{
    if (string.is_empty())
        return {};

    Vector<String> v;
    size_t substart = 0;
    for (size_t i = 0; i < string.length(); ++i) {
        char ch = string.characters_without_null_termination()[i];
        if (isspace(ch)) {
            size_t sublen = i - substart;
            if (sublen != 0)
                v.append(string.substring_view(substart, sublen));
            substart = i + 1;
        }
    }
    size_t taillen = string.length() - substart;
    if (taillen != 0)
        v.append(string.substring_view(substart, taillen));
    return v;
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

static inline void set_property_border_width(StyleProperties& style, StyleValue const& value, Edge edge)
{
    VERIFY(value.is_length());
    if (contains(Edge::Top, edge))
        style.set_property(CSS::PropertyID::BorderTopWidth, value);
    if (contains(Edge::Right, edge))
        style.set_property(CSS::PropertyID::BorderRightWidth, value);
    if (contains(Edge::Bottom, edge))
        style.set_property(CSS::PropertyID::BorderBottomWidth, value);
    if (contains(Edge::Left, edge))
        style.set_property(CSS::PropertyID::BorderLeftWidth, value);
}

static inline void set_property_border_color(StyleProperties& style, StyleValue const& value, Edge edge)
{
    VERIFY(value.is_color());
    if (contains(Edge::Top, edge))
        style.set_property(CSS::PropertyID::BorderTopColor, value);
    if (contains(Edge::Right, edge))
        style.set_property(CSS::PropertyID::BorderRightColor, value);
    if (contains(Edge::Bottom, edge))
        style.set_property(CSS::PropertyID::BorderBottomColor, value);
    if (contains(Edge::Left, edge))
        style.set_property(CSS::PropertyID::BorderLeftColor, value);
}

static inline void set_property_border_style(StyleProperties& style, StyleValue const& value, Edge edge)
{
    VERIFY(value.type() == CSS::StyleValue::Type::Identifier);
    if (contains(Edge::Top, edge))
        style.set_property(CSS::PropertyID::BorderTopStyle, value);
    if (contains(Edge::Right, edge))
        style.set_property(CSS::PropertyID::BorderRightStyle, value);
    if (contains(Edge::Bottom, edge))
        style.set_property(CSS::PropertyID::BorderBottomStyle, value);
    if (contains(Edge::Left, edge))
        style.set_property(CSS::PropertyID::BorderLeftStyle, value);
}

static inline bool is_background_repeat(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    switch (value.to_identifier()) {
    case CSS::ValueID::NoRepeat:
    case CSS::ValueID::Repeat:
    case CSS::ValueID::RepeatX:
    case CSS::ValueID::RepeatY:
    case CSS::ValueID::Round:
    case CSS::ValueID::Space:
        return true;
    default:
        return false;
    }
}

static inline bool is_background_image(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;
    if (value.is_image())
        return true;
    if (value.to_identifier() == ValueID::None)
        return true;
    return false;
}

static inline bool is_color(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    if (value.is_color())
        return true;

    return false;
}

static inline bool is_flex_direction(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    switch (value.to_identifier()) {
    case ValueID::Row:
    case ValueID::RowReverse:
    case ValueID::Column:
    case ValueID::ColumnReverse:
        return true;
    default:
        return false;
    }
}

static inline bool is_flex_wrap(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    switch (value.to_identifier()) {
    case ValueID::Wrap:
    case ValueID::Nowrap:
    case ValueID::WrapReverse:
        return true;
    default:
        return false;
    }
}

static inline bool is_flex_grow_or_shrink(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    if (value.is_numeric())
        return true;

    return false;
}

static inline bool is_flex_basis(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    if (value.is_length())
        return true;

    if (value.is_identifier() && value.to_identifier() == ValueID::Content)
        return true;

    return false;
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

static inline bool is_font_size(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;
    if (value.is_length())
        return true;
    switch (value.to_identifier()) {
    case ValueID::XxSmall:
    case ValueID::XSmall:
    case ValueID::Small:
    case ValueID::Medium:
    case ValueID::Large:
    case ValueID::XLarge:
    case ValueID::XxLarge:
    case ValueID::XxxLarge:
    case ValueID::Smaller:
    case ValueID::Larger:
        return true;
    default:
        return false;
    }
}

static inline bool is_font_style(StyleValue const& value)
{
    // FIXME: Handle angle parameter to `oblique`: https://www.w3.org/TR/css-fonts-4/#font-style-prop
    if (value.is_builtin_or_dynamic())
        return true;
    switch (value.to_identifier()) {
    case ValueID::Normal:
    case ValueID::Italic:
    case ValueID::Oblique:
        return true;
    default:
        return false;
    }
}

static inline bool is_font_weight(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;
    if (value.is_numeric()) {
        auto weight = static_cast<NumericStyleValue const&>(value).value();
        return (weight >= 1 && weight <= 1000);
    }
    switch (value.to_identifier()) {
    case ValueID::Normal:
    case ValueID::Bold:
    case ValueID::Bolder:
    case ValueID::Lighter:
        return true;
    default:
        return false;
    }
}

static inline bool is_line_height(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;
    if (value.is_numeric())
        return true;
    if (value.is_length())
        return true;
    if (value.to_identifier() == ValueID::Normal)
        return true;
    return false;
}

static inline bool is_line_style(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    switch (value.to_identifier()) {
    case ValueID::Dotted:
    case ValueID::Dashed:
    case ValueID::Solid:
    case ValueID::Double:
    case ValueID::Groove:
    case ValueID::Ridge:
    case ValueID::None:
    case ValueID::Hidden:
    case ValueID::Inset:
    case ValueID::Outset:
        return true;
    default:
        return false;
    }
}

static inline bool is_line_width(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    if (value.is_length())
        return true;

    // FIXME: Implement thin/medium/thick
    switch (value.to_identifier()) {
    case ValueID::None:
        return true;
    default:
        return false;
    }
}

static inline bool is_list_style_image(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;
    if (value.is_image())
        return true;
    if (value.is_identifier() && value.to_identifier() == ValueID::None)
        return true;

    return false;
}

static inline bool is_list_style_position(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    switch (value.to_identifier()) {
    case ValueID::Inside:
    case ValueID::Outside:
        return true;
    default:
        return false;
    }
}

static inline bool is_list_style_type(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;
    // FIXME: Handle strings and symbols("...") syntax
    switch (value.to_identifier()) {
    case CSS::ValueID::None:
    case CSS::ValueID::Disc:
    case CSS::ValueID::Circle:
    case CSS::ValueID::Square:
    case CSS::ValueID::Decimal:
    case CSS::ValueID::DecimalLeadingZero:
    case CSS::ValueID::LowerAlpha:
    case CSS::ValueID::LowerLatin:
    case CSS::ValueID::UpperAlpha:
    case CSS::ValueID::UpperLatin:
    case CSS::ValueID::UpperRoman:
    case CSS::ValueID::LowerRoman:
        return true;
    default:
        return true;
    }
}

static inline bool is_text_decoration_line(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    switch (value.to_identifier()) {
    case ValueID::None:
    case ValueID::Underline:
    case ValueID::Overline:
    case ValueID::LineThrough:
    case ValueID::Blink:
        return true;
    default:
        return false;
    }
}

static inline bool is_text_decoration_style(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    switch (value.to_identifier()) {
    case ValueID::Solid:
    case ValueID::Double:
    case ValueID::Dotted:
    case ValueID::Dashed:
    case ValueID::Wavy:
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

    if (property_id == CSS::PropertyID::TextDecoration) {
        if (value.is_color()) {
            style.set_property(CSS::PropertyID::TextDecorationColor, value);
            return;
        }
        if (is_text_decoration_line(value)) {
            style.set_property(CSS::PropertyID::TextDecorationLine, value);
            return;
        }
        if (is_text_decoration_style(value)) {
            style.set_property(CSS::PropertyID::TextDecorationStyle, value);
            return;
        }

        if (value.is_value_list()) {
            auto& parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (!parts.is_empty() && parts.size() <= 3) {
                RefPtr<StyleValue> color_value;
                RefPtr<StyleValue> line_value;
                RefPtr<StyleValue> style_value;

                for (auto& part : parts) {
                    auto value = Parser::parse_css_value(context, property_id, part);
                    if (!value)
                        return;

                    if (value->is_color()) {
                        if (color_value)
                            return;
                        color_value = move(value);
                        continue;
                    }
                    if (is_text_decoration_line(*value)) {
                        if (line_value)
                            return;
                        line_value = move(value);
                        continue;
                    }
                    if (is_text_decoration_style(*value)) {
                        if (style_value)
                            return;
                        style_value = move(value);
                        continue;
                    }

                    return;
                }

                if (color_value)
                    style.set_property(CSS::PropertyID::TextDecorationColor, *color_value);
                if (line_value)
                    style.set_property(CSS::PropertyID::TextDecorationLine, *line_value);
                if (style_value)
                    style.set_property(CSS::PropertyID::TextDecorationStyle, *style_value);
            }
            return;
        }

        return;
    }

    if (property_id == CSS::PropertyID::Overflow) {
        style.set_property(CSS::PropertyID::OverflowX, value);
        style.set_property(CSS::PropertyID::OverflowY, value);
        return;
    }

    if (property_id == CSS::PropertyID::Border) {
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderTop, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderRight, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderBottom, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderLeft, value, document);
        return;
    }

    if (property_id == CSS::PropertyID::BorderRadius) {
        // FIXME: Allow for two values per corner to support elliptical radii.
        // FIXME: Add support the '/' to specify elliptical radii.
        if (value.is_length()) {
            style.set_property(CSS::PropertyID::BorderTopLeftRadius, value);
            style.set_property(CSS::PropertyID::BorderTopRightRadius, value);
            style.set_property(CSS::PropertyID::BorderBottomRightRadius, value);
            style.set_property(CSS::PropertyID::BorderBottomLeftRadius, value);
            return;
        }

        if (value.is_value_list()) {
            auto& parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.size() == 2) {
                auto diagonal1 = Parser::parse_css_value(context, property_id, parts[0]);
                auto diagonal2 = Parser::parse_css_value(context, property_id, parts[1]);
                if (diagonal1 && diagonal2) {
                    style.set_property(CSS::PropertyID::BorderTopLeftRadius, *diagonal1);
                    style.set_property(CSS::PropertyID::BorderBottomRightRadius, *diagonal1);
                    style.set_property(CSS::PropertyID::BorderTopRightRadius, *diagonal2);
                    style.set_property(CSS::PropertyID::BorderBottomLeftRadius, *diagonal2);
                }
                return;
            }
            if (parts.size() == 3) {
                auto top_left = Parser::parse_css_value(context, property_id, parts[0]);
                auto diagonal = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom_right = Parser::parse_css_value(context, property_id, parts[2]);
                if (top_left && diagonal && bottom_right) {
                    style.set_property(CSS::PropertyID::BorderTopLeftRadius, *top_left);
                    style.set_property(CSS::PropertyID::BorderBottomRightRadius, *bottom_right);
                    style.set_property(CSS::PropertyID::BorderTopRightRadius, *diagonal);
                    style.set_property(CSS::PropertyID::BorderBottomLeftRadius, *diagonal);
                }
                return;
            }
            if (parts.size() == 4) {
                auto top_left = Parser::parse_css_value(context, property_id, parts[0]);
                auto top_right = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom_right = Parser::parse_css_value(context, property_id, parts[2]);
                auto bottom_left = Parser::parse_css_value(context, property_id, parts[3]);
                if (top_left && top_right && bottom_right && bottom_left) {
                    style.set_property(CSS::PropertyID::BorderTopLeftRadius, *top_left);
                    style.set_property(CSS::PropertyID::BorderBottomRightRadius, *bottom_right);
                    style.set_property(CSS::PropertyID::BorderTopRightRadius, *top_right);
                    style.set_property(CSS::PropertyID::BorderBottomLeftRadius, *bottom_left);
                }
                return;
            }
            dbgln("Unsure what to do with CSS border-radius value '{}'", value.to_string());
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

        auto parts = split_on_whitespace(value.to_string());
        if (value.is_length()) {
            set_property_border_width(style, value, edge);
            return;
        }
        if (value.is_color()) {
            set_property_border_color(style, value, edge);
            return;
        }

        if (value.is_value_list()) {
            auto& parts = static_cast<CSS::ValueListStyleValue const&>(value).values();

            if (parts.size() == 1) {
                auto value = Parser::parse_css_value(context, property_id, parts[0]);
                if (value && is_line_style(*value)) {
                    set_property_border_style(style, value.release_nonnull(), edge);
                    set_property_border_color(style, ColorStyleValue::create(Gfx::Color::Black), edge);
                    set_property_border_width(style, LengthStyleValue::create(Length(3, Length::Type::Px)), edge);
                    return;
                }
            }

            RefPtr<StyleValue> line_width_value;
            RefPtr<StyleValue> color_value;
            RefPtr<StyleValue> line_style_value;

            for (auto& part : parts) {
                auto value = Parser::parse_css_value(context, property_id, part);
                if (!value)
                    return;

                if (is_line_width(*value)) {
                    if (line_width_value)
                        return;
                    line_width_value = move(value);
                    continue;
                }
                if (is_color(*value)) {
                    if (color_value)
                        return;
                    color_value = move(value);
                    continue;
                }
                if (is_line_style(*value)) {
                    if (line_style_value)
                        return;
                    line_style_value = move(value);
                    continue;
                }
            }

            if (line_width_value)
                set_property_border_width(style, line_width_value.release_nonnull(), edge);
            if (color_value)
                set_property_border_color(style, color_value.release_nonnull(), edge);
            if (line_style_value)
                set_property_border_style(style, line_style_value.release_nonnull(), edge);

            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderStyle) {
        if (value.is_value_list()) {
            auto& parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.size() == 4) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto right = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                auto left = Parser::parse_css_value(context, property_id, parts[3]);
                if (top && right && bottom && left) {
                    style.set_property(CSS::PropertyID::BorderTopStyle, *top);
                    style.set_property(CSS::PropertyID::BorderRightStyle, *right);
                    style.set_property(CSS::PropertyID::BorderBottomStyle, *bottom);
                    style.set_property(CSS::PropertyID::BorderLeftStyle, *left);
                }
            } else if (parts.size() == 3) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto right = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                auto left = Parser::parse_css_value(context, property_id, parts[1]);
                if (top && right && bottom && left) {
                    style.set_property(CSS::PropertyID::BorderTopStyle, *top);
                    style.set_property(CSS::PropertyID::BorderRightStyle, *right);
                    style.set_property(CSS::PropertyID::BorderBottomStyle, *bottom);
                    style.set_property(CSS::PropertyID::BorderLeftStyle, *left);
                }
            } else if (parts.size() == 2) {
                auto vertical = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal = Parser::parse_css_value(context, property_id, parts[1]);
                if (vertical && horizontal) {
                    style.set_property(CSS::PropertyID::BorderTopStyle, *vertical);
                    style.set_property(CSS::PropertyID::BorderRightStyle, *horizontal);
                    style.set_property(CSS::PropertyID::BorderBottomStyle, *vertical);
                    style.set_property(CSS::PropertyID::BorderLeftStyle, *horizontal);
                }
            } else {
                auto line_style = Parser::parse_css_value(context, property_id, parts[0]);
                if (line_style) {
                    style.set_property(CSS::PropertyID::BorderTopStyle, *line_style);
                    style.set_property(CSS::PropertyID::BorderRightStyle, *line_style);
                    style.set_property(CSS::PropertyID::BorderBottomStyle, *line_style);
                    style.set_property(CSS::PropertyID::BorderLeftStyle, *line_style);
                }
            }
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
            auto& parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.size() == 4) {
                auto top_border_width = Parser::parse_css_value(context, property_id, parts[0]);
                auto right_border_width = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom_border_width = Parser::parse_css_value(context, property_id, parts[2]);
                auto left_border_width = Parser::parse_css_value(context, property_id, parts[3]);
                if (top_border_width && right_border_width && bottom_border_width && left_border_width) {
                    style.set_property(CSS::PropertyID::BorderTopWidth, *top_border_width);
                    style.set_property(CSS::PropertyID::BorderRightWidth, *right_border_width);
                    style.set_property(CSS::PropertyID::BorderBottomWidth, *bottom_border_width);
                    style.set_property(CSS::PropertyID::BorderLeftWidth, *left_border_width);
                }
            } else if (parts.size() == 3) {
                auto top_border_width = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal_border_width = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom_border_width = Parser::parse_css_value(context, property_id, parts[2]);
                if (top_border_width && horizontal_border_width && bottom_border_width) {
                    style.set_property(CSS::PropertyID::BorderTopWidth, *top_border_width);
                    style.set_property(CSS::PropertyID::BorderRightWidth, *horizontal_border_width);
                    style.set_property(CSS::PropertyID::BorderBottomWidth, *bottom_border_width);
                    style.set_property(CSS::PropertyID::BorderLeftWidth, *horizontal_border_width);
                }
            } else if (parts.size() == 2) {
                auto vertical_border_width = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal_border_width = Parser::parse_css_value(context, property_id, parts[1]);
                if (vertical_border_width && horizontal_border_width) {
                    style.set_property(CSS::PropertyID::BorderTopWidth, *vertical_border_width);
                    style.set_property(CSS::PropertyID::BorderRightWidth, *horizontal_border_width);
                    style.set_property(CSS::PropertyID::BorderBottomWidth, *vertical_border_width);
                    style.set_property(CSS::PropertyID::BorderLeftWidth, *horizontal_border_width);
                }
            } else {
                auto border_width = Parser::parse_css_value(context, property_id, parts[0]);
                if (border_width) {
                    style.set_property(CSS::PropertyID::BorderTopWidth, *border_width);
                    style.set_property(CSS::PropertyID::BorderRightWidth, *border_width);
                    style.set_property(CSS::PropertyID::BorderBottomWidth, *border_width);
                    style.set_property(CSS::PropertyID::BorderLeftWidth, *border_width);
                }
            }
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopStyle, value);
        style.set_property(CSS::PropertyID::BorderRightStyle, value);
        style.set_property(CSS::PropertyID::BorderBottomStyle, value);
        style.set_property(CSS::PropertyID::BorderLeftStyle, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        if (value.is_value_list()) {
            auto& parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.size() == 4) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto right = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                auto left = Parser::parse_css_value(context, property_id, parts[3]);
                if (top && right && bottom && left) {
                    style.set_property(CSS::PropertyID::BorderTopColor, *top);
                    style.set_property(CSS::PropertyID::BorderRightColor, *right);
                    style.set_property(CSS::PropertyID::BorderBottomColor, *bottom);
                    style.set_property(CSS::PropertyID::BorderLeftColor, *left);
                }
            } else if (parts.size() == 3) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                if (top && horizontal && bottom) {
                    style.set_property(CSS::PropertyID::BorderTopColor, *top);
                    style.set_property(CSS::PropertyID::BorderRightColor, *horizontal);
                    style.set_property(CSS::PropertyID::BorderBottomColor, *bottom);
                    style.set_property(CSS::PropertyID::BorderLeftColor, *horizontal);
                }
            } else if (parts.size() == 2) {
                auto vertical = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal = Parser::parse_css_value(context, property_id, parts[1]);
                if (vertical && horizontal) {
                    style.set_property(CSS::PropertyID::BorderTopColor, *vertical);
                    style.set_property(CSS::PropertyID::BorderRightColor, *horizontal);
                    style.set_property(CSS::PropertyID::BorderBottomColor, *vertical);
                    style.set_property(CSS::PropertyID::BorderLeftColor, *horizontal);
                }
            } else {
                auto color = Parser::parse_css_value(context, property_id, parts[0]);
                if (color) {
                    style.set_property(CSS::PropertyID::BorderTopColor, *color);
                    style.set_property(CSS::PropertyID::BorderRightColor, *color);
                    style.set_property(CSS::PropertyID::BorderBottomColor, *color);
                    style.set_property(CSS::PropertyID::BorderLeftColor, *color);
                }
            }
            return;
        }

        style.set_property(CSS::PropertyID::BorderTopColor, value);
        style.set_property(CSS::PropertyID::BorderRightColor, value);
        style.set_property(CSS::PropertyID::BorderBottomColor, value);
        style.set_property(CSS::PropertyID::BorderLeftColor, value);
        return;
    }

    if (property_id == CSS::PropertyID::Background) {
        if (value.to_identifier() == ValueID::None) {
            style.set_property(CSS::PropertyID::BackgroundColor, ColorStyleValue::create(Color::Transparent));
            return;
        }

        if (is_background_image(value)) {
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, value, document);
            return;
        }

        if (is_color(value)) {
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, value, document);
            return;
        }

        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();

            RefPtr<StyleValue> background_color_value;
            RefPtr<StyleValue> background_image_value;
            RefPtr<StyleValue> repeat_x_value;
            RefPtr<StyleValue> repeat_y_value;
            // FIXME: Implement background-position.
            // FIXME: Implement background-size.
            // FIXME: Implement background-attachment.
            // FIXME: Implement background-clip.
            // FIXME: Implement background-origin.

            for (size_t i = 0; i < parts.size(); ++i) {
                auto& part = parts[i];

                // FIXME: Handle multiple backgrounds.
                if (part.is(Token::Type::Comma))
                    break;

                auto value = Parser::parse_css_value(context, property_id, part);
                if (!value) {
                    dbgln("Unable to parse token in `background` as a CSS value: '{}'", part.to_debug_string());
                    return;
                }

                if (value->is_color()) {
                    if (background_color_value)
                        return;
                    background_color_value = move(value);
                    continue;
                }
                if (is_background_image(*value)) {
                    if (background_image_value)
                        return;
                    background_image_value = move(value);
                    continue;
                }
                if (is_background_repeat(*value)) {
                    if (repeat_x_value)
                        return;

                    auto value_id = value->to_identifier();
                    if (value_id == ValueID::RepeatX || value_id == ValueID::RepeatY) {
                        repeat_x_value = IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::Repeat : ValueID::NoRepeat);
                        repeat_y_value = IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::NoRepeat : ValueID::Repeat);
                        continue;
                    }

                    // Check following value, if it's also a repeat, set both.
                    if (i + 1 < parts.size()) {
                        auto next_value = Parser::parse_css_value(context, property_id, parts[i + 1]);
                        if (next_value && is_background_repeat(*next_value)) {
                            ++i;
                            repeat_x_value = move(value);
                            repeat_y_value = move(next_value);
                            continue;
                        }
                    }
                    repeat_x_value = value;
                    repeat_y_value = value;
                    continue;
                }

                dbgln("Unhandled token in `background` declaration: '{}'", part.to_debug_string());
                return;
            }

            if (background_color_value)
                set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundColor, background_color_value.release_nonnull(), document);
            if (background_image_value)
                set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, background_image_value.release_nonnull(), document);
            if (repeat_x_value)
                set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatX, repeat_x_value.release_nonnull(), document, true);
            if (repeat_y_value)
                set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundRepeatY, repeat_y_value.release_nonnull(), document, true);

            return;
        }

        return;
    }

    if (property_id == CSS::PropertyID::BackgroundImage) {
        if (is_background_image(value)) {
            style.set_property(CSS::PropertyID::BackgroundImage, value);
            return;
        }

        // FIXME: Remove string parsing once DeprecatedCSSParser is gone.
        if (value.is_string()) {
            return;
            auto string = value.to_string();
            if (!string.starts_with("url("))
                return;
            if (!string.ends_with(')'))
                return;
            auto url = string.substring_view(4, string.length() - 5);
            if (url.length() >= 2 && url.starts_with('"') && url.ends_with('"'))
                url = url.substring_view(1, url.length() - 2);
            else if (url.length() >= 2 && url.starts_with('\'') && url.ends_with('\''))
                url = url.substring_view(1, url.length() - 2);

            auto background_image_value = ImageStyleValue::create(document.complete_url(url), document);
            style.set_property(CSS::PropertyID::BackgroundImage, move(background_image_value));
            return;
        }

        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            // FIXME: Handle multiple backgrounds.
            if (!parts.is_empty()) {
                auto first_value = Parser::parse_css_value(context, property_id, parts[0]);
                if (first_value)
                    style.set_property(CSS::PropertyID::BackgroundImage, *first_value);
            }
            return;
        }

        dbgln("Unsure what to do with CSS background-image value '{}'", value.to_string());
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundRepeat) {
        auto assign_background_repeat_from_single_value = [&](StyleValue const& value) {
            auto value_id = value.to_identifier();
            if (value_id == ValueID::RepeatX || value_id == ValueID::RepeatY) {
                auto repeat_x = IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::Repeat : ValueID::NoRepeat);
                auto repeat_y = IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::NoRepeat : ValueID::Repeat);
                set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, repeat_x, document, true);
                set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, repeat_y, document, true);
            } else {
                set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, value, document, true);
                set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, value, document, true);
            }
        };

        if (is_background_repeat(value)) {
            assign_background_repeat_from_single_value(value);
        }

        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            NonnullRefPtrVector<StyleValue> repeat_values;
            for (auto& part : parts) {
                if (part.is(Token::Type::Comma)) {
                    // FIXME: Handle multiple backgrounds.
                    break;
                }
                auto parsed_value = Parser::parse_css_value(context, property_id, part);
                if (parsed_value)
                    repeat_values.append(parsed_value.release_nonnull());
            }
            if (repeat_values.size() == 1) {
                assign_background_repeat_from_single_value(repeat_values[0]);
            } else if (repeat_values.size() == 2) {
                set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatX, repeat_values[0], document, true);
                set_property_expanding_shorthands(style, PropertyID::BackgroundRepeatY, repeat_values[1], document, true);
            }
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
        if (value.is_length()) {
            style.set_property(CSS::PropertyID::MarginTop, value);
            style.set_property(CSS::PropertyID::MarginRight, value);
            style.set_property(CSS::PropertyID::MarginBottom, value);
            style.set_property(CSS::PropertyID::MarginLeft, value);
            return;
        }

        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.size() == 2) {
                auto vertical = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal = Parser::parse_css_value(context, property_id, parts[1]);
                if (vertical && horizontal) {
                    style.set_property(CSS::PropertyID::MarginTop, *vertical);
                    style.set_property(CSS::PropertyID::MarginBottom, *vertical);
                    style.set_property(CSS::PropertyID::MarginLeft, *horizontal);
                    style.set_property(CSS::PropertyID::MarginRight, *horizontal);
                }
                return;
            } else if (parts.size() == 3) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                if (top && horizontal && bottom) {
                    style.set_property(CSS::PropertyID::MarginTop, *top);
                    style.set_property(CSS::PropertyID::MarginBottom, *bottom);
                    style.set_property(CSS::PropertyID::MarginLeft, *horizontal);
                    style.set_property(CSS::PropertyID::MarginRight, *horizontal);
                }
                return;
            } else if (parts.size() == 4) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto right = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                auto left = Parser::parse_css_value(context, property_id, parts[3]);
                if (top && right && bottom && left) {
                    style.set_property(CSS::PropertyID::MarginTop, *top);
                    style.set_property(CSS::PropertyID::MarginBottom, *bottom);
                    style.set_property(CSS::PropertyID::MarginLeft, *left);
                    style.set_property(CSS::PropertyID::MarginRight, *right);
                }
                return;
            }
            dbgln("Unsure what to do with CSS margin value '{}'", value.to_string());
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::Padding) {
        if (value.is_length()) {
            style.set_property(CSS::PropertyID::PaddingTop, value);
            style.set_property(CSS::PropertyID::PaddingRight, value);
            style.set_property(CSS::PropertyID::PaddingBottom, value);
            style.set_property(CSS::PropertyID::PaddingLeft, value);
            return;
        }

        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.size() == 2) {
                auto vertical = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal = Parser::parse_css_value(context, property_id, parts[1]);
                if (vertical && horizontal) {
                    style.set_property(CSS::PropertyID::PaddingTop, *vertical);
                    style.set_property(CSS::PropertyID::PaddingBottom, *vertical);
                    style.set_property(CSS::PropertyID::PaddingLeft, *horizontal);
                    style.set_property(CSS::PropertyID::PaddingRight, *horizontal);
                }
                return;
            } else if (parts.size() == 3) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto horizontal = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                if (top && bottom && horizontal) {
                    style.set_property(CSS::PropertyID::PaddingTop, *top);
                    style.set_property(CSS::PropertyID::PaddingBottom, *bottom);
                    style.set_property(CSS::PropertyID::PaddingLeft, *horizontal);
                    style.set_property(CSS::PropertyID::PaddingRight, *horizontal);
                }
                return;
            } else if (parts.size() == 4) {
                auto top = Parser::parse_css_value(context, property_id, parts[0]);
                auto right = Parser::parse_css_value(context, property_id, parts[1]);
                auto bottom = Parser::parse_css_value(context, property_id, parts[2]);
                auto left = Parser::parse_css_value(context, property_id, parts[3]);
                if (top && bottom && left && right) {
                    style.set_property(CSS::PropertyID::PaddingTop, *top);
                    style.set_property(CSS::PropertyID::PaddingBottom, *bottom);
                    style.set_property(CSS::PropertyID::PaddingLeft, *left);
                    style.set_property(CSS::PropertyID::PaddingRight, *right);
                }
                return;
            }
            dbgln("Unsure what to do with CSS padding value '{}'", value.to_string());
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::ListStyle) {
        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();

            if (!parts.is_empty() && parts.size() <= 3) {
                RefPtr<StyleValue> position_value;
                RefPtr<StyleValue> image_value;
                RefPtr<StyleValue> type_value;

                // FIXME: `none` is ambiguous as it is a valid value for ListStyleImage and ListStyleType,
                // so requires special handling. https://www.w3.org/TR/css-lists-3/#propdef-list-style

                for (auto& part : parts) {
                    auto value = Parser::parse_css_value(context, property_id, part);
                    if (!value)
                        return;

                    if (is_list_style_position(*value)) {
                        if (position_value)
                            return;
                        position_value = move(value);
                        continue;
                    }
                    if (is_list_style_image(*value)) {
                        if (image_value)
                            return;
                        image_value = move(value);
                        continue;
                    }
                    if (is_list_style_type(*value)) {
                        if (type_value)
                            return;
                        type_value = move(value);
                        continue;
                    }
                }

                if (position_value)
                    style.set_property(CSS::PropertyID::ListStylePosition, *position_value);
                if (image_value)
                    style.set_property(CSS::PropertyID::ListStyleImage, *image_value);
                if (type_value)
                    style.set_property(CSS::PropertyID::ListStyleType, *type_value);
            }
            return;
        }

        if (is_list_style_position(value))
            style.set_property(CSS::PropertyID::ListStylePosition, value);
        else if (is_list_style_image(value))
            style.set_property(CSS::PropertyID::ListStyleImage, value);
        else if (is_list_style_type(value))
            style.set_property(CSS::PropertyID::ListStyleType, value);

        return;
    }

    if (property_id == CSS::PropertyID::Font) {
        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();

            RefPtr<StyleValue> font_style_value;
            RefPtr<StyleValue> font_weight_value;
            RefPtr<StyleValue> font_size_value;
            RefPtr<StyleValue> line_height_value;
            RefPtr<StyleValue> font_family_value;
            // FIXME: Implement font-stretch and font-variant.

            for (size_t i = 0; i < parts.size(); ++i) {
                auto value = Parser::parse_css_value(context, property_id, parts[i]);
                if (!value)
                    return;

                if (is_font_style(*value)) {
                    if (font_style_value)
                        return;
                    font_style_value = move(value);
                    continue;
                }
                if (is_font_weight(*value)) {
                    if (font_weight_value)
                        return;
                    font_weight_value = move(value);
                    continue;
                }
                if (is_font_size(*value)) {
                    if (font_size_value)
                        return;
                    font_size_value = move(value);

                    // Consume `/ line-height` if present
                    if (i + 2 < parts.size()) {
                        auto solidus_part = parts[i + 1];
                        if (!(solidus_part.is(Token::Type::Delim) && solidus_part.token().delim() == "/"sv))
                            break;
                        auto line_height = Parser::parse_css_value(context, property_id, parts[i + 2]);
                        if (!(line_height && is_line_height(*line_height)))
                            return;
                        line_height_value = move(line_height);
                        i += 2;
                    }

                    // Consume font-family
                    // FIXME: Handle multiple font-families separated by commas, for fallback purposes.
                    if (i + 1 < parts.size()) {
                        auto& font_family_part = parts[i + 1];
                        auto font_family = Parser::parse_css_value(context, property_id, font_family_part);
                        if (!font_family) {
                            // Single-word font-families may not be quoted. We convert it to a String for convenience.
                            if (font_family_part.is(Token::Type::Ident))
                                font_family = StringStyleValue::create(font_family_part.token().ident());
                            else
                                return;
                        } else if (!is_font_family(*font_family)) {
                            dbgln("*** Unable to parse '{}' as a font-family.", font_family_part.to_debug_string());
                            return;
                        }

                        font_family_value = move(font_family);
                    }
                    break;
                }

                return;
            }

            if (!font_size_value || !font_family_value)
                return;

            style.set_property(CSS::PropertyID::FontSize, *font_size_value);
            style.set_property(CSS::PropertyID::FontFamily, *font_family_value);

            if (font_style_value)
                style.set_property(CSS::PropertyID::FontStyle, *font_style_value);
            if (font_weight_value)
                style.set_property(CSS::PropertyID::FontWeight, *font_weight_value);
            if (line_height_value)
                style.set_property(CSS::PropertyID::LineHeight, *line_height_value);

            return;
        }

        if (value.is_inherit()) {
            style.set_property(CSS::PropertyID::FontSize, value);
            style.set_property(CSS::PropertyID::FontFamily, value);
            style.set_property(CSS::PropertyID::FontStyle, value);
            style.set_property(CSS::PropertyID::FontVariant, value);
            style.set_property(CSS::PropertyID::FontWeight, value);
            style.set_property(CSS::PropertyID::LineHeight, value);
            // FIXME: Implement font-stretch
            return;
        }

        // FIXME: Handle system fonts. (caption, icon, menu, message-box, small-caption, status-bar)

        return;
    }

    if (property_id == CSS::PropertyID::FontFamily) {
        if (value.is_value_list()) {
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

        if (is_font_family(value)) {
            style.set_property(CSS::PropertyID::FontFamily, value);
            return;
        }

        return;
    }

    if (property_id == CSS::PropertyID::Flex) {

        if (value.is_auto()) {
            style.set_property(CSS::PropertyID::FlexGrow, CSS::NumericStyleValue::create(1.0));
            style.set_property(CSS::PropertyID::FlexShrink, CSS::NumericStyleValue::create(1.0));
            // FIXME: Support auto
            style.set_property(CSS::PropertyID::FlexBasis, CSS::IdentifierStyleValue::create(CSS::ValueID::Content));
            return;
        }

        if (value.is_length() || (value.is_identifier() && value.to_identifier() == CSS::ValueID::Content)) {
            style.set_property(CSS::PropertyID::FlexBasis, value);
            return;
        }

        if (value.is_identifier() && value.to_identifier() == CSS::ValueID::None) {
            style.set_property(CSS::PropertyID::FlexGrow, CSS::NumericStyleValue::create(0.0));
            style.set_property(CSS::PropertyID::FlexShrink, CSS::NumericStyleValue::create(0.0));
            // FIXME: Support auto
            style.set_property(CSS::PropertyID::FlexBasis, CSS::IdentifierStyleValue::create(CSS::ValueID::Content));
            return;
        }

        if (value.is_initial()) {
            style.set_property(CSS::PropertyID::FlexGrow, CSS::NumericStyleValue::create(0.0));
            style.set_property(CSS::PropertyID::FlexShrink, CSS::NumericStyleValue::create(1.0));
            // FIXME: Support auto
            style.set_property(CSS::PropertyID::FlexBasis, CSS::IdentifierStyleValue::create(CSS::ValueID::Content));
            return;
        }

        if (value.is_numeric()) {
            auto number = verify_cast<NumericStyleValue>(value).value();
            if (number >= 0) {
                style.set_property(CSS::PropertyID::FlexGrow, value);
                style.set_property(CSS::PropertyID::FlexShrink, CSS::NumericStyleValue::create(1.0));
                style.set_property(CSS::PropertyID::FlexBasis, CSS::NumericStyleValue::create(0.0));
                return;
            }
        }

        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.size() == 1) {
                auto value = Parser::parse_css_value(context, property_id, parts[0]);
                if (!value)
                    return;
                if (is_flex_basis(*value)) {
                    style.set_property(CSS::PropertyID::FlexBasis, *value);
                } else if (is_flex_grow_or_shrink(*value)) {
                    style.set_property(CSS::PropertyID::FlexGrow, *value);
                }
                return;
            }

            if (parts.size() == 2) {
                auto flex_grow = Parser::parse_css_value(context, property_id, parts[0]);
                auto second_value = Parser::parse_css_value(context, property_id, parts[1]);
                if (!flex_grow || !second_value)
                    return;

                style.set_property(CSS::PropertyID::FlexGrow, *flex_grow);

                if (is_flex_basis(*second_value)) {
                    style.set_property(CSS::PropertyID::FlexBasis, *second_value);
                } else if (is_flex_grow_or_shrink(*second_value)) {
                    style.set_property(CSS::PropertyID::FlexShrink, *second_value);
                }
                return;
            }

            if (parts.size() == 3) {
                auto flex_grow = Parser::parse_css_value(context, property_id, parts[0]);
                auto flex_shrink = Parser::parse_css_value(context, property_id, parts[1]);
                auto flex_basis = Parser::parse_css_value(context, property_id, parts[2]);
                if (!flex_grow || !flex_shrink || !flex_basis)
                    return;

                style.set_property(CSS::PropertyID::FlexGrow, *flex_grow);
                style.set_property(CSS::PropertyID::FlexShrink, *flex_shrink);

                if (is_flex_basis(*flex_basis))
                    style.set_property(CSS::PropertyID::FlexBasis, *flex_basis);

                return;
            }

            return;
        }

        dbgln("Unsure what to do with CSS flex value '{}'", value.to_string());
        return;
    }

    if (property_id == CSS::PropertyID::FlexFlow) {
        if (value.is_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            if (parts.is_empty() || parts.size() > 2)
                return;

            RefPtr<StyleValue> flex_direction_value;
            RefPtr<StyleValue> flex_wrap_value;

            for (auto& part : parts) {
                auto value = Parser::parse_css_value(context, property_id, part);
                if (!value)
                    return;
                if (is_flex_direction(*value)) {
                    if (flex_direction_value)
                        return;
                    flex_direction_value = move(value);
                    continue;
                }
                if (is_flex_wrap(*value)) {
                    if (flex_wrap_value)
                        return;
                    flex_wrap_value = move(value);
                    continue;
                }
            }

            if (flex_direction_value)
                style.set_property(CSS::PropertyID::FlexDirection, *flex_direction_value);
            if (flex_wrap_value)
                style.set_property(CSS::PropertyID::FlexWrap, *flex_wrap_value);

            return;
        }

        return;
    }

    if (value.is_value_list()) {
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
