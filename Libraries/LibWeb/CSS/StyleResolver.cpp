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
#include <LibWeb/CSS/Parser/CSSParser.h>
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
        extern const char default_stylesheet_source[];
        String css = default_stylesheet_source;
        sheet = parse_css(CSS::ParsingContext(), css).leak_ref();
    }
    return *sheet;
}

static StyleSheet& quirks_mode_stylesheet()
{
    static StyleSheet* sheet;
    if (!sheet) {
        extern const char quirks_mode_stylesheet_source[];
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

Vector<MatchingRule> StyleResolver::collect_matching_rules(const DOM::Element& element) const
{
    Vector<MatchingRule> matching_rules;

    size_t style_sheet_index = 0;
    for_each_stylesheet([&](auto& sheet) {
        size_t rule_index = 0;
        for (auto& rule : sheet.rules()) {
            size_t selector_index = 0;
            for (auto& selector : rule.selectors()) {
                if (SelectorEngine::matches(selector, element)) {
                    matching_rules.append({ rule, style_sheet_index, rule_index, selector_index });
                    break;
                }
                ++selector_index;
            }
            ++rule_index;
        }
        ++style_sheet_index;
    });

#ifdef HTML_DEBUG
    dbgprintf("Rules matching Element{%p}\n", &element);
    for (auto& rule : matching_rules) {
        dump_rule(rule);
    }
#endif

    return matching_rules;
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
        inherited_properties.set(CSS::PropertyID::TextDecoration);
    }
    return inherited_properties.contains(property_id);
}

static Vector<String> split_on_whitespace(const StringView& string)
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

static inline void set_property_border_width(StyleProperties& style, const StyleValue& value, Edge edge)
{
    ASSERT(value.is_length());
    if (contains(Edge::Top, edge))
        style.set_property(CSS::PropertyID::BorderTopWidth, value);
    if (contains(Edge::Right, edge))
        style.set_property(CSS::PropertyID::BorderRightWidth, value);
    if (contains(Edge::Bottom, edge))
        style.set_property(CSS::PropertyID::BorderBottomWidth, value);
    if (contains(Edge::Left, edge))
        style.set_property(CSS::PropertyID::BorderLeftWidth, value);
}

static inline void set_property_border_color(StyleProperties& style, const StyleValue& value, Edge edge)
{
    ASSERT(value.is_color());
    if (contains(Edge::Top, edge))
        style.set_property(CSS::PropertyID::BorderTopColor, value);
    if (contains(Edge::Right, edge))
        style.set_property(CSS::PropertyID::BorderRightColor, value);
    if (contains(Edge::Bottom, edge))
        style.set_property(CSS::PropertyID::BorderBottomColor, value);
    if (contains(Edge::Left, edge))
        style.set_property(CSS::PropertyID::BorderLeftColor, value);
}

static inline void set_property_border_style(StyleProperties& style, const StyleValue& value, Edge edge)
{
    ASSERT(value.is_string());
    if (contains(Edge::Top, edge))
        style.set_property(CSS::PropertyID::BorderTopStyle, value);
    if (contains(Edge::Right, edge))
        style.set_property(CSS::PropertyID::BorderRightStyle, value);
    if (contains(Edge::Bottom, edge))
        style.set_property(CSS::PropertyID::BorderBottomStyle, value);
    if (contains(Edge::Left, edge))
        style.set_property(CSS::PropertyID::BorderLeftStyle, value);
}

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, const StyleValue& value, DOM::Document& document)
{
    CSS::ParsingContext context(document);

    if (property_id == CSS::PropertyID::Border) {
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderTop, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderRight, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderBottom, value, document);
        set_property_expanding_shorthands(style, CSS::PropertyID::BorderLeft, value, document);
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
        if (value.is_string()) {
            auto parts = split_on_whitespace(value.to_string());

            if (parts.size() == 1) {
                if (auto value = parse_line_style(context, parts[0])) {
                    set_property_border_style(style, value.release_nonnull(), edge);
                    set_property_border_color(style, ColorStyleValue::create(Gfx::Color::Black), edge);
                    set_property_border_width(style, LengthStyleValue::create(Length(3, Length::Type::Px)), edge);
                    return;
                }
            }

            RefPtr<LengthStyleValue> line_width_value;
            RefPtr<ColorStyleValue> color_value;
            RefPtr<StringStyleValue> line_style_value;

            for (auto& part : parts) {
                if (auto value = parse_line_width(context, part)) {
                    if (line_width_value)
                        return;
                    line_width_value = move(value);
                    continue;
                }
                if (auto value = parse_color(context, part)) {
                    if (color_value)
                        return;
                    color_value = move(value);
                    continue;
                }
                if (auto value = parse_line_style(context, part)) {
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
        auto parts = split_on_whitespace(value.to_string());
        if (value.is_string() && parts.size() == 3) {
            auto top = parse_css_value(context, parts[0]);
            auto right = parse_css_value(context, parts[1]);
            auto bottom = parse_css_value(context, parts[2]);
            auto left = parse_css_value(context, parts[1]);
            if (top && right && bottom && left) {
                style.set_property(CSS::PropertyID::BorderTopStyle, *top);
                style.set_property(CSS::PropertyID::BorderRightStyle, *right);
                style.set_property(CSS::PropertyID::BorderBottomStyle, *bottom);
                style.set_property(CSS::PropertyID::BorderLeftStyle, *left);
            }
        } else {
            style.set_property(CSS::PropertyID::BorderTopStyle, value);
            style.set_property(CSS::PropertyID::BorderRightStyle, value);
            style.set_property(CSS::PropertyID::BorderBottomStyle, value);
            style.set_property(CSS::PropertyID::BorderLeftStyle, value);
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderWidth) {
        auto parts = split_on_whitespace(value.to_string());
        if (value.is_string() && parts.size() == 2) {
            auto vertical_border_width = parse_css_value(context, parts[0]);
            auto horizontal_border_width = parse_css_value(context, parts[1]);
            if (vertical_border_width && horizontal_border_width) {
                style.set_property(CSS::PropertyID::BorderTopWidth, *vertical_border_width);
                style.set_property(CSS::PropertyID::BorderRightWidth, *horizontal_border_width);
                style.set_property(CSS::PropertyID::BorderBottomWidth, *vertical_border_width);
                style.set_property(CSS::PropertyID::BorderLeftWidth, *horizontal_border_width);
            }
        } else {
            style.set_property(CSS::PropertyID::BorderTopWidth, value);
            style.set_property(CSS::PropertyID::BorderRightWidth, value);
            style.set_property(CSS::PropertyID::BorderBottomWidth, value);
            style.set_property(CSS::PropertyID::BorderLeftWidth, value);
        }
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        auto parts = split_on_whitespace(value.to_string());
        if (value.is_string() && parts.size() == 4) {
            auto top = parse_css_value(context, parts[0]);
            auto right = parse_css_value(context, parts[1]);
            auto bottom = parse_css_value(context, parts[2]);
            auto left = parse_css_value(context, parts[3]);
            if (top && right && bottom && left) {
                style.set_property(CSS::PropertyID::BorderTopColor, *top);
                style.set_property(CSS::PropertyID::BorderRightColor, *right);
                style.set_property(CSS::PropertyID::BorderBottomColor, *bottom);
                style.set_property(CSS::PropertyID::BorderLeftColor, *left);
            }
        } else {
            style.set_property(CSS::PropertyID::BorderTopColor, value);
            style.set_property(CSS::PropertyID::BorderRightColor, value);
            style.set_property(CSS::PropertyID::BorderBottomColor, value);
            style.set_property(CSS::PropertyID::BorderLeftColor, value);
        }
        return;
    }

    if (property_id == CSS::PropertyID::Background) {
        if (value.to_string() == "none") {
            style.set_property(CSS::PropertyID::BackgroundColor, ColorStyleValue::create(Color::Transparent));
            return;
        }
        auto parts = split_on_whitespace(value.to_string());
        NonnullRefPtrVector<StyleValue> values;
        for (auto& part : parts) {
            auto value = parse_css_value(context, part);
            if (!value)
                return;
            values.append(value.release_nonnull());
        }

        // HACK: Disallow more than one color value in a 'background' shorthand
        size_t color_value_count = 0;
        for (auto& value : values)
            color_value_count += value.is_color();

        if (values[0].is_color() && color_value_count == 1)
            style.set_property(CSS::PropertyID::BackgroundColor, values[0]);

        for (auto& value : values) {
            if (!value.is_string())
                continue;
            auto string = value.to_string();
            set_property_expanding_shorthands(style, CSS::PropertyID::BackgroundImage, value, document);
        }
        return;
    }

    if (property_id == CSS::PropertyID::BackgroundImage) {
        if (!value.is_string())
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

    if (property_id == CSS::PropertyID::Margin) {
        if (value.is_length()) {
            style.set_property(CSS::PropertyID::MarginTop, value);
            style.set_property(CSS::PropertyID::MarginRight, value);
            style.set_property(CSS::PropertyID::MarginBottom, value);
            style.set_property(CSS::PropertyID::MarginLeft, value);
            return;
        }
        if (value.is_string()) {
            auto parts = split_on_whitespace(value.to_string());
            if (value.is_string() && parts.size() == 2) {
                auto vertical = parse_css_value(context, parts[0]);
                auto horizontal = parse_css_value(context, parts[1]);
                if (vertical && horizontal) {
                    style.set_property(CSS::PropertyID::MarginTop, *vertical);
                    style.set_property(CSS::PropertyID::MarginBottom, *vertical);
                    style.set_property(CSS::PropertyID::MarginLeft, *horizontal);
                    style.set_property(CSS::PropertyID::MarginRight, *horizontal);
                }
                return;
            }
            if (value.is_string() && parts.size() == 3) {
                auto top = parse_css_value(context, parts[0]);
                auto horizontal = parse_css_value(context, parts[1]);
                auto bottom = parse_css_value(context, parts[2]);
                if (top && horizontal && bottom) {
                    style.set_property(CSS::PropertyID::MarginTop, *top);
                    style.set_property(CSS::PropertyID::MarginBottom, *bottom);
                    style.set_property(CSS::PropertyID::MarginLeft, *horizontal);
                    style.set_property(CSS::PropertyID::MarginRight, *horizontal);
                }
                return;
            }
            if (value.is_string() && parts.size() == 4) {
                auto top = parse_css_value(context, parts[0]);
                auto right = parse_css_value(context, parts[1]);
                auto bottom = parse_css_value(context, parts[2]);
                auto left = parse_css_value(context, parts[3]);
                if (top && right && bottom && left) {
                    style.set_property(CSS::PropertyID::MarginTop, *top);
                    style.set_property(CSS::PropertyID::MarginBottom, *bottom);
                    style.set_property(CSS::PropertyID::MarginLeft, *left);
                    style.set_property(CSS::PropertyID::MarginRight, *right);
                }
                return;
            }
            dbg() << "Unsure what to do with CSS margin value '" << value.to_string() << "'";
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
        if (value.is_string()) {
            auto parts = split_on_whitespace(value.to_string());
            if (value.is_string() && parts.size() == 2) {
                auto vertical = parse_css_value(context, parts[0]);
                auto horizontal = parse_css_value(context, parts[1]);
                if (vertical && horizontal) {
                    style.set_property(CSS::PropertyID::PaddingTop, *vertical);
                    style.set_property(CSS::PropertyID::PaddingBottom, *vertical);
                    style.set_property(CSS::PropertyID::PaddingLeft, *horizontal);
                    style.set_property(CSS::PropertyID::PaddingRight, *horizontal);
                }
                return;
            }
            if (value.is_string() && parts.size() == 3) {
                auto top = parse_css_value(context, parts[0]);
                auto horizontal = parse_css_value(context, parts[1]);
                auto bottom = parse_css_value(context, parts[2]);
                if (top && bottom && horizontal) {
                    style.set_property(CSS::PropertyID::PaddingTop, *top);
                    style.set_property(CSS::PropertyID::PaddingBottom, *bottom);
                    style.set_property(CSS::PropertyID::PaddingLeft, *horizontal);
                    style.set_property(CSS::PropertyID::PaddingRight, *horizontal);
                }
                return;
            }
            if (value.is_string() && parts.size() == 4) {
                auto top = parse_css_value(context, parts[0]);
                auto right = parse_css_value(context, parts[1]);
                auto bottom = parse_css_value(context, parts[2]);
                auto left = parse_css_value(context, parts[3]);
                if (top && bottom && left && right) {
                    style.set_property(CSS::PropertyID::PaddingTop, *top);
                    style.set_property(CSS::PropertyID::PaddingBottom, *bottom);
                    style.set_property(CSS::PropertyID::PaddingLeft, *left);
                    style.set_property(CSS::PropertyID::PaddingRight, *right);
                }
                return;
            }
            dbg() << "Unsure what to do with CSS padding value '" << value.to_string() << "'";
            return;
        }
        return;
    }

    if (property_id == CSS::PropertyID::ListStyle) {
        auto parts = split_on_whitespace(value.to_string());
        if (!parts.is_empty()) {
            auto value = parse_css_value(context, parts[0]);
            if (!value)
                return;
            style.set_property(CSS::PropertyID::ListStyleType, value.release_nonnull());
        }
        return;
    }

    style.set_property(property_id, value);
}

NonnullRefPtr<StyleProperties> StyleResolver::resolve_style(const DOM::Element& element, const StyleProperties* parent_style) const
{
    auto style = StyleProperties::create();

    if (parent_style) {
        parent_style->for_each_property([&](auto property_id, auto& value) {
            if (is_inherited_property(property_id))
                set_property_expanding_shorthands(style, property_id, value, m_document);
        });
    }

    element.apply_presentational_hints(*style);

    auto matching_rules = collect_matching_rules(element);

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

    for (auto& match : matching_rules) {
        for (auto& property : match.rule->declaration().properties()) {
            set_property_expanding_shorthands(style, property.property_id, property.value, m_document);
        }
    }

    auto style_attribute = element.attribute(HTML::AttributeNames::style);
    if (!style_attribute.is_null()) {
        if (auto declaration = parse_css_declaration(CSS::ParsingContext(document()), style_attribute)) {
            for (auto& property : declaration->properties()) {
                set_property_expanding_shorthands(style, property.property_id, property.value, m_document);
            }
        }
    }

    return style;
}

}
