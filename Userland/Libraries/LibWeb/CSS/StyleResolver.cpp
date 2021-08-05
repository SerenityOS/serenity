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

static inline bool is_color(StyleValue const& value)
{
    if (value.is_builtin_or_dynamic())
        return true;

    if (value.is_color())
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

        if (value.is_component_value_list()) {
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

        if (value.is_component_value_list()) {
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

        if (value.is_component_value_list()) {
            auto parts = static_cast<CSS::ValueListStyleValue const&>(value).values();
            // FIXME: Handle multiple backgrounds.
            if (!parts.is_empty()) {
                auto first_value = Parser::parse_css_value(context, property_id, parts[0]);
                if (first_value)
                    style.set_property(CSS::PropertyID::BackgroundImage, *first_value);
            }
            return;
        }

        style.set_property(CSS::PropertyID::BackgroundImage, value);
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

        if (value.is_component_value_list()) {
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
            return;
        }

        assign_background_repeat_from_single_value(value);
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
