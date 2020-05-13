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

#include <AK/HashMap.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/Parser/CSSParser.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>

#define PARSE_ASSERT(x)                                                   \
    if (!(x)) {                                                           \
        dbg() << "CSS PARSER ASSERTION FAILED: " << #x;                   \
        dbg() << "At character# " << index << " in CSS: _" << css << "_"; \
        ASSERT_NOT_REACHED();                                             \
    }

namespace Web {

static Optional<Color> parse_css_color(const StringView& view)
{
    if (view.equals_ignoring_case("transparent"))
        return Color::from_rgba(0x00000000);

    auto color = Color::from_string(view.to_string().to_lowercase());
    if (color.has_value())
        return color;

    return {};
}

static Optional<float> try_parse_float(const StringView& string)
{
    const char* str = string.characters_without_null_termination();
    size_t len = string.length();
    size_t weight = 1;
    int exp_val = 0;
    float value = 0.0f;
    float fraction = 0.0f;
    bool has_sign = false;
    bool is_negative = false;
    bool is_fractional = false;
    bool is_scientific = false;

    if (str[0] == '-') {
        is_negative = true;
        has_sign = true;
    }
    if (str[0] == '+') {
        has_sign = true;
    }

    for (size_t i = has_sign; i < len; i++) {

        // Looks like we're about to start working on the fractional part
        if (str[i] == '.') {
            is_fractional = true;
            continue;
        }

        if (str[i] == 'e' || str[i] == 'E') {
            if (str[i + 1] == '-' || str[i + 1] == '+')
                exp_val = atoi(str + i + 2);
            else
                exp_val = atoi(str + i + 1);

            is_scientific = true;
            continue;
        }

        if (str[i] < '0' || str[i] > '9' || exp_val != 0) {
            return {};
            continue;
        }

        if (is_fractional) {
            fraction *= 10;
            fraction += str[i] - '0';
            weight *= 10;
        } else {
            value = value * 10;
            value += str[i] - '0';
        }
    }

    fraction /= weight;
    value += fraction;

    if (is_scientific) {
        bool divide = exp_val < 0;
        if (divide)
            exp_val *= -1;

        for (int i = 0; i < exp_val; i++) {
            if (divide)
                value /= 10;
            else
                value *= 10;
        }
    }

    return is_negative ? -value : value;
}

static Optional<float> parse_number(const StringView& view)
{
    if (view.ends_with('%'))
        return parse_number(view.substring_view(0, view.length() - 1));

    // FIXME: Maybe we should have "ends_with_ignoring_case()" ?
    if (view.to_string().to_lowercase().ends_with("px"))
        return parse_number(view.substring_view(0, view.length() - 2));

    return try_parse_float(view);
}

NonnullRefPtr<StyleValue> parse_css_value(const StringView& string)
{
    auto number = parse_number(string);
    if (number.has_value()) {
        if (string.ends_with('%'))
            return PercentageStyleValue::create(number.value());
        return LengthStyleValue::create(Length(number.value(), Length::Type::Absolute));
    }

    if (string.equals_ignoring_case("inherit"))
        return InheritStyleValue::create();
    if (string.equals_ignoring_case("initial"))
        return InitialStyleValue::create();
    if (string.equals_ignoring_case("auto"))
        return LengthStyleValue::create(Length());

    auto color = parse_css_color(string);
    if (color.has_value())
        return ColorStyleValue::create(color.value());

    if (string == "-libhtml-link")
        return IdentifierStyleValue::create(CSS::ValueID::VendorSpecificLink);

    return StringStyleValue::create(string);
}

RefPtr<LengthStyleValue> parse_line_width(const StringView& part)
{
    NonnullRefPtr<StyleValue> value = parse_css_value(part);
    if (value->is_length())
        return static_ptr_cast<LengthStyleValue>(value);
    return nullptr;
}

RefPtr<ColorStyleValue> parse_color(const StringView& part)
{
    NonnullRefPtr<StyleValue> value = parse_css_value(part);
    if (value->is_color())
        return static_ptr_cast<ColorStyleValue>(value);
    return nullptr;
}

RefPtr<StringStyleValue> parse_line_style(const StringView& part)
{
    NonnullRefPtr<StyleValue> parsed_value = parse_css_value(part);
    if (!parsed_value->is_string())
        return nullptr;
    auto value = static_ptr_cast<StringStyleValue>(parsed_value);
    if (value->to_string() == "dotted")
        return value;
    if (value->to_string() == "dashed")
        return value;
    if (value->to_string() == "solid")
        return value;
    if (value->to_string() == "double")
        return value;
    if (value->to_string() == "groove")
        return value;
    if (value->to_string() == "ridge")
        return value;
    return nullptr;
}

class CSSParser {
public:
    CSSParser(const StringView& input)
        : css(input)
    {
    }

    bool next_is(const char* str) const
    {
        size_t len = strlen(str);
        for (size_t i = 0; i < len; ++i) {
            if (peek(i) != str[i])
                return false;
        }
        return true;
    }

    char peek(size_t offset = 0) const
    {
        if ((index + offset) < css.length())
            return css[index + offset];
        return 0;
    }

    char consume_specific(char ch)
    {
        if (peek() != ch) {
            dbg() << "peek() != '" << ch << "'";
        }
        PARSE_ASSERT(peek() == ch);
        PARSE_ASSERT(index < css.length());
        ++index;
        return ch;
    }

    char consume_one()
    {
        PARSE_ASSERT(index < css.length());
        return css[index++];
    };

    bool consume_whitespace_or_comments()
    {
        size_t original_index = index;
        bool in_comment = false;
        for (; index < css.length(); ++index) {
            char ch = peek();
            if (isspace(ch))
                continue;
            if (!in_comment && ch == '/' && peek(1) == '*') {
                in_comment = true;
                ++index;
                continue;
            }
            if (in_comment && ch == '*' && peek(1) == '/') {
                in_comment = false;
                ++index;
                continue;
            }
            if (in_comment)
                continue;
            break;
        }
        return original_index != index;
    }

    bool is_valid_selector_char(char ch) const
    {
        return isalnum(ch) || ch == '-' || ch == '_' || ch == '(' || ch == ')' || ch == '@';
    }

    bool is_combinator(char ch) const
    {
        return ch == '~' || ch == '>' || ch == '+';
    }

    Optional<Selector::SimpleSelector> parse_simple_selector()
    {
        auto index_at_start = index;

        if (consume_whitespace_or_comments())
            return {};

        if (!peek() || peek() == '{' || peek() == ',' || is_combinator(peek()))
            return {};

        Selector::SimpleSelector::Type type;

        if (peek() == '*') {
            type = Selector::SimpleSelector::Type::Universal;
            consume_one();
            return Selector::SimpleSelector {
                type,
                Selector::SimpleSelector::PseudoClass::None,
                String(),
                Selector::SimpleSelector::AttributeMatchType::None,
                String(),
                String()
            };
        }

        if (peek() == '.') {
            type = Selector::SimpleSelector::Type::Class;
            consume_one();
        } else if (peek() == '#') {
            type = Selector::SimpleSelector::Type::Id;
            consume_one();
        } else if (isalpha(peek())) {
            type = Selector::SimpleSelector::Type::TagName;
        } else {
            type = Selector::SimpleSelector::Type::Universal;
        }

        if (type != Selector::SimpleSelector::Type::Universal) {
            while (is_valid_selector_char(peek()))
                buffer.append(consume_one());
            PARSE_ASSERT(!buffer.is_null());
        }

        Selector::SimpleSelector simple_selector {
            type,
            Selector::SimpleSelector::PseudoClass::None,
            String::copy(buffer),
            Selector::SimpleSelector::AttributeMatchType::None,
            String(),
            String()
        };
        buffer.clear();

        if (peek() == '[') {
            Selector::SimpleSelector::AttributeMatchType attribute_match_type = Selector::SimpleSelector::AttributeMatchType::HasAttribute;
            String attribute_name;
            String attribute_value;
            bool in_value = false;
            consume_specific('[');
            char expected_end_of_attribute_selector = ']';
            while (peek() != expected_end_of_attribute_selector) {
                char ch = consume_one();
                if (ch == '=') {
                    attribute_match_type = Selector::SimpleSelector::AttributeMatchType::ExactValueMatch;
                    attribute_name = String::copy(buffer);
                    buffer.clear();
                    in_value = true;
                    consume_whitespace_or_comments();
                    if (peek() == '\'') {
                        expected_end_of_attribute_selector = '\'';
                        consume_one();
                    } else if (peek() == '"') {
                        expected_end_of_attribute_selector = '"';
                        consume_one();
                    }
                    continue;
                }
                buffer.append(ch);
            }
            if (in_value)
                attribute_value = String::copy(buffer);
            else
                attribute_name = String::copy(buffer);
            buffer.clear();
            simple_selector.attribute_match_type = attribute_match_type;
            simple_selector.attribute_name = attribute_name;
            simple_selector.attribute_value = attribute_value;
            if (expected_end_of_attribute_selector != ']')
                consume_specific(expected_end_of_attribute_selector);
            consume_whitespace_or_comments();
            consume_specific(']');
        }

        if (peek() == ':') {
            // FIXME: Implement pseudo elements.
            [[maybe_unused]] bool is_pseudo_element = false;
            consume_one();
            if (peek() == ':') {
                is_pseudo_element = true;
                consume_one();
            }
            if (next_is("not")) {
                buffer.append(consume_one());
                buffer.append(consume_one());
                buffer.append(consume_one());
                buffer.append(consume_specific('('));
                while (peek() != ')')
                    buffer.append(consume_one());
                buffer.append(consume_specific(')'));
            } else {
                while (is_valid_selector_char(peek()))
                    buffer.append(consume_one());
            }

            auto pseudo_name = String::copy(buffer);
            buffer.clear();


            // Ignore for now, otherwise we produce a "false positive" selector
            // and apply styles to the element itself, not its pseudo element
            if (is_pseudo_element)
                return {};

            if (pseudo_name.equals_ignoring_case("link"))
                simple_selector.pseudo_class = Selector::SimpleSelector::PseudoClass::Link;
            else if (pseudo_name.equals_ignoring_case("hover"))
                simple_selector.pseudo_class = Selector::SimpleSelector::PseudoClass::Hover;
            else if (pseudo_name.equals_ignoring_case("focus"))
                simple_selector.pseudo_class = Selector::SimpleSelector::PseudoClass::Focus;
            else if (pseudo_name.equals_ignoring_case("first-child"))
                simple_selector.pseudo_class = Selector::SimpleSelector::PseudoClass::FirstChild;
            else if (pseudo_name.equals_ignoring_case("last-child"))
                simple_selector.pseudo_class = Selector::SimpleSelector::PseudoClass::LastChild;
            else if (pseudo_name.equals_ignoring_case("only-child"))
                simple_selector.pseudo_class = Selector::SimpleSelector::PseudoClass::OnlyChild;
            else if (pseudo_name.equals_ignoring_case("empty"))
                simple_selector.pseudo_class = Selector::SimpleSelector::PseudoClass::Empty;
        }

        if (index == index_at_start) {
            // We consumed nothing.
            return {};
        }

        return simple_selector;
    }

    Optional<Selector::ComplexSelector> parse_complex_selector()
    {
        auto relation = Selector::ComplexSelector::Relation::Descendant;

        if (peek() == '{' || peek() == ',')
            return {};

        if (is_combinator(peek())) {
            switch (peek()) {
            case '>':
                relation = Selector::ComplexSelector::Relation::ImmediateChild;
                break;
            case '+':
                relation = Selector::ComplexSelector::Relation::AdjacentSibling;
                break;
            case '~':
                relation = Selector::ComplexSelector::Relation::GeneralSibling;
                break;
            }
            consume_one();
            consume_whitespace_or_comments();
        }

        consume_whitespace_or_comments();

        Vector<Selector::SimpleSelector> simple_selectors;
        for (;;) {
            auto component = parse_simple_selector();
            if (!component.has_value())
                break;
            simple_selectors.append(component.value());
            // If this assert triggers, we're most likely up to no good.
            PARSE_ASSERT(simple_selectors.size() < 100);
        }

        if (simple_selectors.is_empty())
            return {};

        return Selector::ComplexSelector { relation, move(simple_selectors) };
    }

    void parse_selector()
    {
        Vector<Selector::ComplexSelector> complex_selectors;

        for (;;) {
            auto complex_selector = parse_complex_selector();
            if (complex_selector.has_value())
                complex_selectors.append(complex_selector.value());
            consume_whitespace_or_comments();
            if (!peek() || peek() == ',' || peek() == '{')
                break;
        }

        if (complex_selectors.is_empty())
            return;
        complex_selectors.first().relation = Selector::ComplexSelector::Relation::None;

        current_rule.selectors.append(Selector(move(complex_selectors)));
    }

    Optional<Selector> parse_individual_selector()
    {
        parse_selector();
        if (current_rule.selectors.is_empty())
            return {};
        return current_rule.selectors.last();
    }

    void parse_selector_list()
    {
        for (;;) {
            parse_selector();
            consume_whitespace_or_comments();
            if (peek() == ',') {
                consume_one();
                continue;
            }
            if (peek() == '{')
                break;
        }
    }

    bool is_valid_property_name_char(char ch) const
    {
        return ch && !isspace(ch) && ch != ':';
    }

    bool is_valid_property_value_char(char ch) const
    {
        return ch && ch != '!' && ch != ';' && ch != '}';
    }

    struct ValueAndImportant {
        String value;
        bool important { false };
    };

    ValueAndImportant consume_css_value()
    {
        buffer.clear();

        int paren_nesting_level = 0;
        bool important = false;

        for (;;) {
            char ch = peek();
            if (ch == '(') {
                ++paren_nesting_level;
                buffer.append(consume_one());
                continue;
            }
            if (ch == ')') {
                PARSE_ASSERT(paren_nesting_level > 0);
                --paren_nesting_level;
                buffer.append(consume_one());
                continue;
            }
            if (paren_nesting_level > 0) {
                buffer.append(consume_one());
                continue;
            }
            if (next_is("!important")) {
                consume_specific('!');
                consume_specific('i');
                consume_specific('m');
                consume_specific('p');
                consume_specific('o');
                consume_specific('r');
                consume_specific('t');
                consume_specific('a');
                consume_specific('n');
                consume_specific('t');
                important = true;
                continue;
            }
            if (next_is("/*")) {
                consume_whitespace_or_comments();
                continue;
            }
            if (!ch)
                break;
            if (ch == '}')
                break;
            if (ch == ';')
                break;
            buffer.append(consume_one());
        }

        // Remove trailing whitespace.
        while (!buffer.is_empty() && isspace(buffer.last()))
            buffer.take_last();

        auto string = String::copy(buffer);
        buffer.clear();

        return { string, important };
    }

    Optional<StyleProperty> parse_property()
    {
        consume_whitespace_or_comments();
        if (peek() == ';') {
            consume_one();
            return {};
        }
        if (peek() == '}')
            return {};
        buffer.clear();
        while (is_valid_property_name_char(peek()))
            buffer.append(consume_one());
        auto property_name = String::copy(buffer);
        buffer.clear();
        consume_whitespace_or_comments();
        consume_specific(':');
        consume_whitespace_or_comments();

        auto [property_value, important] = consume_css_value();

        consume_whitespace_or_comments();

        if (peek() && peek() != '}')
            consume_specific(';');

        auto property_id = CSS::property_id_from_string(property_name);
        return StyleProperty { property_id, parse_css_value(property_value), important };
    }

    void parse_declaration()
    {
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace_or_comments();
            if (peek() == '}')
                break;
        }
    }

    void parse_rule()
    {
        consume_whitespace_or_comments();
        if (index >= css.length())
            return;

        // FIXME: We ignore @-rules for now.
        if (peek() == '@') {
            while (peek() != '{')
                consume_one();
            int level = 0;
            for (;;) {
                auto ch = consume_one();
                if (ch == '{') {
                    ++level;
                } else if (ch == '}') {
                    --level;
                    if (level == 0)
                        break;
                }
            }
            consume_whitespace_or_comments();
            return;
        }

        parse_selector_list();
        consume_specific('{');
        parse_declaration();
        consume_specific('}');
        rules.append(StyleRule::create(move(current_rule.selectors), StyleDeclaration::create(move(current_rule.properties))));
        consume_whitespace_or_comments();
    }

    RefPtr<StyleSheet> parse_sheet()
    {
        while (index < css.length()) {
            parse_rule();
        }

        return StyleSheet::create(move(rules));
    }

    RefPtr<StyleDeclaration> parse_standalone_declaration()
    {
        consume_whitespace_or_comments();
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace_or_comments();
            if (!peek())
                break;
        }
        return StyleDeclaration::create(move(current_rule.properties));
    }

private:
    NonnullRefPtrVector<StyleRule> rules;

    struct CurrentRule {
        Vector<Selector> selectors;
        Vector<StyleProperty> properties;
    };

    CurrentRule current_rule;
    Vector<char> buffer;

    size_t index = 0;

    StringView css;
};

Optional<Selector> parse_selector(const StringView& selector_text)
{
    CSSParser parser(selector_text);
    return parser.parse_individual_selector();
}

RefPtr<StyleSheet> parse_css(const StringView& css)
{
    CSSParser parser(css);
    return parser.parse_sheet();
}

RefPtr<StyleDeclaration> parse_css_declaration(const StringView& css)
{
    CSSParser parser(css);
    return parser.parse_standalone_declaration();
}

}
