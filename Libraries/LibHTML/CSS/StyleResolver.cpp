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

#include <LibHTML/CSS/SelectorEngine.h>
#include <LibHTML/CSS/StyleResolver.h>
#include <LibHTML/CSS/StyleSheet.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Dump.h>
#include <LibHTML/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>

StyleResolver::StyleResolver(Document& document)
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
        sheet = parse_css(css).leak_ref();
    }
    return *sheet;
}

template<typename Callback>
void StyleResolver::for_each_stylesheet(Callback callback) const
{
    callback(default_stylesheet());
    for (auto& sheet : document().stylesheets()) {
        callback(sheet);
    }
}

NonnullRefPtrVector<StyleRule> StyleResolver::collect_matching_rules(const Element& element) const
{
    NonnullRefPtrVector<StyleRule> matching_rules;

    for_each_stylesheet([&](auto& sheet) {
        for (auto& rule : sheet.rules()) {
            for (auto& selector : rule.selectors()) {
                if (SelectorEngine::matches(selector, element)) {
                    matching_rules.append(rule);
                    break;
                }
            }
        }
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

static void set_property_expanding_shorthands(StyleProperties& style, CSS::PropertyID property_id, const StyleValue& value)
{
    if (property_id == CSS::PropertyID::BorderStyle) {
        style.set_property(CSS::PropertyID::BorderTopStyle, value);
        style.set_property(CSS::PropertyID::BorderRightStyle, value);
        style.set_property(CSS::PropertyID::BorderBottomStyle, value);
        style.set_property(CSS::PropertyID::BorderLeftStyle, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderWidth) {
        style.set_property(CSS::PropertyID::BorderTopWidth, value);
        style.set_property(CSS::PropertyID::BorderRightWidth, value);
        style.set_property(CSS::PropertyID::BorderBottomWidth, value);
        style.set_property(CSS::PropertyID::BorderLeftWidth, value);
        return;
    }

    if (property_id == CSS::PropertyID::BorderColor) {
        style.set_property(CSS::PropertyID::BorderTopColor, value);
        style.set_property(CSS::PropertyID::BorderRightColor, value);
        style.set_property(CSS::PropertyID::BorderBottomColor, value);
        style.set_property(CSS::PropertyID::BorderLeftColor, value);
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
            if (parts.size() == 2) {
                auto vertical = parse_css_value(parts[0]);
                auto horizontal = parse_css_value(parts[1]);
                style.set_property(CSS::PropertyID::MarginTop, vertical);
                style.set_property(CSS::PropertyID::MarginBottom, vertical);
                style.set_property(CSS::PropertyID::MarginLeft, horizontal);
                style.set_property(CSS::PropertyID::MarginRight, horizontal);
                return;
            }
            if (parts.size() == 3) {
                auto top = parse_css_value(parts[0]);
                auto horizontal = parse_css_value(parts[1]);
                auto bottom = parse_css_value(parts[2]);
                style.set_property(CSS::PropertyID::MarginTop, top);
                style.set_property(CSS::PropertyID::MarginBottom, bottom);
                style.set_property(CSS::PropertyID::MarginLeft, horizontal);
                style.set_property(CSS::PropertyID::MarginRight, horizontal);
                return;
            }
            if (parts.size() == 4) {
                auto top = parse_css_value(parts[0]);
                auto right = parse_css_value(parts[1]);
                auto bottom = parse_css_value(parts[2]);
                auto left = parse_css_value(parts[3]);
                style.set_property(CSS::PropertyID::MarginTop, top);
                style.set_property(CSS::PropertyID::MarginBottom, bottom);
                style.set_property(CSS::PropertyID::MarginLeft, left);
                style.set_property(CSS::PropertyID::MarginRight, right);
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
            if (parts.size() == 2) {
                auto vertical = parse_css_value(parts[0]);
                auto horizontal = parse_css_value(parts[1]);
                style.set_property(CSS::PropertyID::PaddingTop, vertical);
                style.set_property(CSS::PropertyID::PaddingBottom, vertical);
                style.set_property(CSS::PropertyID::PaddingLeft, horizontal);
                style.set_property(CSS::PropertyID::PaddingRight, horizontal);
                return;
            }
            if (parts.size() == 3) {
                auto top = parse_css_value(parts[0]);
                auto horizontal = parse_css_value(parts[1]);
                auto bottom = parse_css_value(parts[2]);
                style.set_property(CSS::PropertyID::PaddingTop, top);
                style.set_property(CSS::PropertyID::PaddingBottom, bottom);
                style.set_property(CSS::PropertyID::PaddingLeft, horizontal);
                style.set_property(CSS::PropertyID::PaddingRight, horizontal);
                return;
            }
            if (parts.size() == 4) {
                auto top = parse_css_value(parts[0]);
                auto right = parse_css_value(parts[1]);
                auto bottom = parse_css_value(parts[2]);
                auto left = parse_css_value(parts[3]);
                style.set_property(CSS::PropertyID::PaddingTop, top);
                style.set_property(CSS::PropertyID::PaddingBottom, bottom);
                style.set_property(CSS::PropertyID::PaddingLeft, left);
                style.set_property(CSS::PropertyID::PaddingRight, right);
                return;
            }
            dbg() << "Unsure what to do with CSS padding value '" << value.to_string() << "'";
            return;
        }
        return;
    }

    style.set_property(property_id, value);
}

NonnullRefPtr<StyleProperties> StyleResolver::resolve_style(const Element& element, const StyleProperties* parent_style) const
{
    auto style = StyleProperties::create();

    if (parent_style) {
        parent_style->for_each_property([&](auto property_id, auto& value) {
            if (is_inherited_property(property_id))
                set_property_expanding_shorthands(style, property_id, value);
        });
    }

    element.apply_presentational_hints(*style);

    auto matching_rules = collect_matching_rules(element);
    for (auto& rule : matching_rules) {
        for (auto& property : rule.declaration().properties()) {
            set_property_expanding_shorthands(style, property.property_id, property.value);
        }
    }

    auto style_attribute = element.attribute("style");
    if (!style_attribute.is_null()) {
        if (auto declaration = parse_css_declaration(style_attribute)) {
            for (auto& property : declaration->properties()) {
                set_property_expanding_shorthands(style, property.property_id, property.value);
            }
        }
    }

    return style;
}
