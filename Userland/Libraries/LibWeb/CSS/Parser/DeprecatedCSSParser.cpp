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
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/DOM/Document.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>

#define PARSE_VERIFY(x)                                     \
    if (!(x)) {                                             \
        dbgln("CSS PARSER ASSERTION FAILED: {}", #x);       \
        dbgln("At character# {} in CSS: _{}_", index, css); \
        VERIFY_NOT_REACHED();                               \
    }

#define PARSE_ERROR()             \
    do {                          \
        dbgln("CSS parse error"); \
    } while (0)

namespace Web {

namespace CSS {

ParsingContext::ParsingContext()
{
}

ParsingContext::ParsingContext(const DOM::Document& document)
    : m_document(&document)
{
}

ParsingContext::ParsingContext(const DOM::ParentNode& parent_node)
    : m_document(&parent_node.document())
{
}

bool ParsingContext::in_quirks_mode() const
{
    return m_document ? m_document->in_quirks_mode() : false;
}

URL ParsingContext::complete_url(const String& addr) const
{
    return m_document ? m_document->url().complete_url(addr) : URL::create_with_url_or_path(addr);
}

}

static Optional<Color> parse_css_color(const CSS::ParsingContext&, const StringView& view)
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

static CSS::Length parse_length(const CSS::ParsingContext& context, const StringView& view, bool& is_bad_length)
{
    CSS::Length::Type type = CSS::Length::Type::Undefined;
    Optional<float> value;

    if (view.ends_with('%')) {
        type = CSS::Length::Type::Percentage;
        value = try_parse_float(view.substring_view(0, view.length() - 1));
    } else if (view.ends_with("px", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Px;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("pt", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Pt;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("pc", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Pc;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("mm", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Mm;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("rem", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Rem;
        value = try_parse_float(view.substring_view(0, view.length() - 3));
    } else if (view.ends_with("em", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Em;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("ex", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Ex;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("vw", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vw;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("vh", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vh;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("vmax", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vmax;
        value = try_parse_float(view.substring_view(0, view.length() - 4));
    } else if (view.ends_with("vmin", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Vmin;
        value = try_parse_float(view.substring_view(0, view.length() - 4));
    } else if (view.ends_with("cm", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Cm;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("in", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::In;
        value = try_parse_float(view.substring_view(0, view.length() - 2));
    } else if (view.ends_with("Q", CaseSensitivity::CaseInsensitive)) {
        type = CSS::Length::Type::Q;
        value = try_parse_float(view.substring_view(0, view.length() - 1));
    } else if (view == "0") {
        type = CSS::Length::Type::Px;
        value = 0;
    } else if (context.in_quirks_mode()) {
        type = CSS::Length::Type::Px;
        value = try_parse_float(view);
    } else {
        value = try_parse_float(view);
        if (value.has_value())
            is_bad_length = true;
    }

    if (!value.has_value())
        return {};

    return CSS::Length(value.value(), type);
}

static bool takes_integer_value(CSS::PropertyID property_id)
{
    return property_id == CSS::PropertyID::ZIndex || property_id == CSS::PropertyID::FontWeight;
}

RefPtr<CSS::StyleValue> parse_css_value(const CSS::ParsingContext& context, const StringView& string, CSS::PropertyID property_id)
{
    bool is_bad_length = false;

    if (takes_integer_value(property_id)) {
        auto integer = string.to_int();
        if (integer.has_value())
            return CSS::LengthStyleValue::create(CSS::Length::make_px(integer.value()));
    }

    auto length = parse_length(context, string, is_bad_length);
    if (is_bad_length)
        return nullptr;
    if (!length.is_undefined())
        return CSS::LengthStyleValue::create(length);

    if (string.equals_ignoring_case("inherit"))
        return CSS::InheritStyleValue::create();
    if (string.equals_ignoring_case("initial"))
        return CSS::InitialStyleValue::create();
    if (string.equals_ignoring_case("auto"))
        return CSS::LengthStyleValue::create(CSS::Length::make_auto());

    auto value_id = CSS::value_id_from_string(string);
    if (value_id != CSS::ValueID::Invalid)
        return CSS::IdentifierStyleValue::create(value_id);

    auto color = parse_css_color(context, string);
    if (color.has_value())
        return CSS::ColorStyleValue::create(color.value());

    return CSS::StringStyleValue::create(string);
}

RefPtr<CSS::LengthStyleValue> parse_line_width(const CSS::ParsingContext& context, const StringView& part)
{
    auto value = parse_css_value(context, part);
    if (value && value->is_length())
        return static_ptr_cast<CSS::LengthStyleValue>(value);
    return nullptr;
}

RefPtr<CSS::ColorStyleValue> parse_color(const CSS::ParsingContext& context, const StringView& part)
{
    auto value = parse_css_value(context, part);
    if (value && value->is_color())
        return static_ptr_cast<CSS::ColorStyleValue>(value);
    return nullptr;
}

RefPtr<CSS::StringStyleValue> parse_line_style(const CSS::ParsingContext& context, const StringView& part)
{
    auto parsed_value = parse_css_value(context, part);
    if (!parsed_value || !parsed_value->is_string())
        return nullptr;
    auto value = static_ptr_cast<CSS::StringStyleValue>(parsed_value);
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
    CSSParser(const CSS::ParsingContext& context, const StringView& input)
        : m_context(context)
        , css(input)
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

    bool consume_specific(char ch)
    {
        if (peek() != ch) {
            dbgln("CSSParser: Peeked '{:c}' wanted specific '{:c}'", peek(), ch);
        }
        if (!peek()) {
            PARSE_ERROR();
            return false;
        }
        if (peek() != ch) {
            PARSE_ERROR();
            ++index;
            return false;
        }
        ++index;
        return true;
    }

    char consume_one()
    {
        PARSE_VERIFY(index < css.length());
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

    Optional<CSS::Selector::SimpleSelector> parse_simple_selector()
    {
        auto index_at_start = index;

        if (consume_whitespace_or_comments())
            return {};

        if (!peek() || peek() == '{' || peek() == ',' || is_combinator(peek()))
            return {};

        CSS::Selector::SimpleSelector::Type type;

        if (peek() == '*') {
            type = CSS::Selector::SimpleSelector::Type::Universal;
            consume_one();
            return CSS::Selector::SimpleSelector {
                type,
                CSS::Selector::SimpleSelector::PseudoClass::None,
                CSS::Selector::SimpleSelector::PseudoElement::None,
                String(),
                CSS::Selector::SimpleSelector::AttributeMatchType::None,
                String(),
                String()
            };
        }

        if (peek() == '.') {
            type = CSS::Selector::SimpleSelector::Type::Class;
            consume_one();
        } else if (peek() == '#') {
            type = CSS::Selector::SimpleSelector::Type::Id;
            consume_one();
        } else if (isalpha(peek())) {
            type = CSS::Selector::SimpleSelector::Type::TagName;
        } else {
            type = CSS::Selector::SimpleSelector::Type::Universal;
        }

        if (type != CSS::Selector::SimpleSelector::Type::Universal) {
            while (is_valid_selector_char(peek()))
                buffer.append(consume_one());
            PARSE_VERIFY(!buffer.is_null());
        }

        auto value = String::copy(buffer);

        if (type == CSS::Selector::SimpleSelector::Type::TagName) {
            // Some stylesheets use uppercase tag names, so here's a hack to just lowercase them internally.
            value = value.to_lowercase();
        }

        CSS::Selector::SimpleSelector simple_selector {
            type,
            CSS::Selector::SimpleSelector::PseudoClass::None,
            CSS::Selector::SimpleSelector::PseudoElement::None,
            value,
            CSS::Selector::SimpleSelector::AttributeMatchType::None,
            String(),
            String()
        };
        buffer.clear();

        if (peek() == '[') {
            CSS::Selector::SimpleSelector::AttributeMatchType attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::HasAttribute;
            String attribute_name;
            String attribute_value;
            bool in_value = false;
            consume_specific('[');
            char expected_end_of_attribute_selector = ']';
            while (peek() != expected_end_of_attribute_selector) {
                char ch = consume_one();
                if (ch == '=' || (ch == '~' && peek() == '=')) {
                    if (ch == '=') {
                        attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::ExactValueMatch;
                    } else if (ch == '~') {
                        consume_one();
                        attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::Contains;
                    }
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
                // FIXME: This is a hack that will go away when we replace this with a big boy CSS parser.
                if (ch == '\\')
                    ch = consume_one();
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
            if (expected_end_of_attribute_selector != ']') {
                if (!consume_specific(expected_end_of_attribute_selector))
                    return {};
            }
            consume_whitespace_or_comments();
            if (!consume_specific(']'))
                return {};
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
                if (!consume_specific('('))
                    return {};
                buffer.append('(');
                while (peek() != ')')
                    buffer.append(consume_one());
                if (!consume_specific(')'))
                    return {};
                buffer.append(')');
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
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Link;
            else if (pseudo_name.equals_ignoring_case("visited"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Visited;
            else if (pseudo_name.equals_ignoring_case("hover"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Hover;
            else if (pseudo_name.equals_ignoring_case("focus"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Focus;
            else if (pseudo_name.equals_ignoring_case("first-child"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::FirstChild;
            else if (pseudo_name.equals_ignoring_case("last-child"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::LastChild;
            else if (pseudo_name.equals_ignoring_case("only-child"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::OnlyChild;
            else if (pseudo_name.equals_ignoring_case("empty"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Empty;
            else if (pseudo_name.equals_ignoring_case("root"))
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Root;
            else if (pseudo_name.equals_ignoring_case("before"))
                simple_selector.pseudo_element = CSS::Selector::SimpleSelector::PseudoElement::Before;
            else if (pseudo_name.equals_ignoring_case("after"))
                simple_selector.pseudo_element = CSS::Selector::SimpleSelector::PseudoElement::After;
        }

        if (index == index_at_start) {
            // We consumed nothing.
            return {};
        }

        return simple_selector;
    }

    Optional<CSS::Selector::ComplexSelector> parse_complex_selector()
    {
        auto relation = CSS::Selector::ComplexSelector::Relation::Descendant;

        if (peek() == '{' || peek() == ',')
            return {};

        if (is_combinator(peek())) {
            switch (peek()) {
            case '>':
                relation = CSS::Selector::ComplexSelector::Relation::ImmediateChild;
                break;
            case '+':
                relation = CSS::Selector::ComplexSelector::Relation::AdjacentSibling;
                break;
            case '~':
                relation = CSS::Selector::ComplexSelector::Relation::GeneralSibling;
                break;
            }
            consume_one();
            consume_whitespace_or_comments();
        }

        consume_whitespace_or_comments();

        Vector<CSS::Selector::SimpleSelector> simple_selectors;
        for (;;) {
            auto component = parse_simple_selector();
            if (!component.has_value())
                break;
            simple_selectors.append(component.value());
            // If this assert triggers, we're most likely up to no good.
            PARSE_VERIFY(simple_selectors.size() < 100);
        }

        if (simple_selectors.is_empty())
            return {};

        return CSS::Selector::ComplexSelector { relation, move(simple_selectors) };
    }

    void parse_selector()
    {
        Vector<CSS::Selector::ComplexSelector> complex_selectors;

        for (;;) {
            auto index_before = index;
            auto complex_selector = parse_complex_selector();
            if (complex_selector.has_value())
                complex_selectors.append(complex_selector.value());
            consume_whitespace_or_comments();
            if (!peek() || peek() == ',' || peek() == '{')
                break;
            // HACK: If we didn't move forward, just let go.
            if (index == index_before)
                break;
        }

        if (complex_selectors.is_empty())
            return;
        complex_selectors.first().relation = CSS::Selector::ComplexSelector::Relation::None;

        current_rule.selectors.append(CSS::Selector(move(complex_selectors)));
    }

    Optional<CSS::Selector> parse_individual_selector()
    {
        parse_selector();
        if (current_rule.selectors.is_empty())
            return {};
        return current_rule.selectors.last();
    }

    void parse_selector_list()
    {
        for (;;) {
            auto index_before = index;
            parse_selector();
            consume_whitespace_or_comments();
            if (peek() == ',') {
                consume_one();
                continue;
            }
            if (peek() == '{')
                break;
            // HACK: If we didn't move forward, just let go.
            if (index_before == index)
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

    bool is_valid_string_quotes_char(char ch) const
    {
        return ch == '\'' || ch == '\"';
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
                PARSE_VERIFY(paren_nesting_level > 0);
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
            if (ch == '\\') {
                consume_one();
                buffer.append(consume_one());
                continue;
            }
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

    Optional<CSS::StyleProperty> parse_property()
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
        if (!consume_specific(':'))
            return {};
        consume_whitespace_or_comments();

        auto [property_value, important] = consume_css_value();

        consume_whitespace_or_comments();

        if (peek() && peek() != '}') {
            if (!consume_specific(';'))
                return {};
        }

        auto property_id = CSS::property_id_from_string(property_name);
        if (property_id == CSS::PropertyID::Invalid) {
            dbgln("CSSParser: Unrecognized property '{}'", property_name);
        }
        auto value = parse_css_value(m_context, property_value, property_id);
        if (!value)
            return {};
        return CSS::StyleProperty { property_id, value.release_nonnull(), important };
    }

    void parse_declaration()
    {
        for (;;) {
            auto property = parse_property();
            if (property.has_value())
                current_rule.properties.append(property.value());
            consume_whitespace_or_comments();
            if (!peek() || peek() == '}')
                break;
        }
    }

    void parse_style_rule()
    {
        parse_selector_list();
        if (!consume_specific('{')) {
            PARSE_ERROR();
            return;
        }
        parse_declaration();
        if (!consume_specific('}')) {
            PARSE_ERROR();
            return;
        }

        rules.append(CSS::CSSStyleRule::create(move(current_rule.selectors), CSS::CSSStyleDeclaration::create(move(current_rule.properties))));
    }

    Optional<String> parse_string()
    {
        if (!is_valid_string_quotes_char(peek())) {
            PARSE_ERROR();
            return {};
        }

        char end_char = consume_one();
        buffer.clear();
        while (peek() && peek() != end_char) {
            if (peek() == '\\') {
                consume_specific('\\');
                if (peek() == 0)
                    break;
            }
            buffer.append(consume_one());
        }

        String string_value(String::copy(buffer));
        buffer.clear();

        if (consume_specific(end_char)) {
            return { string_value };
        }
        return {};
    }

    Optional<String> parse_url()
    {
        if (is_valid_string_quotes_char(peek()))
            return parse_string();

        buffer.clear();
        while (peek() && peek() != ')')
            buffer.append(consume_one());

        String url_value(String::copy(buffer));
        buffer.clear();

        if (peek() == ')')
            return { url_value };
        return {};
    }

    void parse_at_import_rule()
    {
        consume_whitespace_or_comments();
        Optional<String> imported_address;
        if (is_valid_string_quotes_char(peek())) {
            imported_address = parse_string();
        } else if (next_is("url")) {
            consume_specific('u');
            consume_specific('r');
            consume_specific('l');

            consume_whitespace_or_comments();

            if (!consume_specific('('))
                return;
            imported_address = parse_url();
            if (!consume_specific(')'))
                return;
        } else {
            PARSE_ERROR();
            return;
        }

        if (imported_address.has_value())
            rules.append(CSS::CSSImportRule::create(m_context.complete_url(imported_address.value())));

        // FIXME: We ignore possilbe media query list
        while (peek() && peek() != ';')
            consume_one();

        consume_specific(';');
    }

    void parse_at_rule()
    {
        HashMap<String, void (CSSParser::*)()> at_rules_parsers({ { "@import", &CSSParser::parse_at_import_rule } });

        for (const auto& rule_parser_pair : at_rules_parsers) {
            if (next_is(rule_parser_pair.key.characters())) {
                for (char c : rule_parser_pair.key) {
                    consume_specific(c);
                }
                (this->*(rule_parser_pair.value))();
                return;
            }
        }

        // FIXME: We ignore other @-rules completely for now.
        while (peek() != 0 && peek() != '{')
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
    }

    void parse_rule()
    {
        consume_whitespace_or_comments();
        if (!peek())
            return;

        if (peek() == '@') {
            parse_at_rule();
        } else {
            parse_style_rule();
        }

        consume_whitespace_or_comments();
    }

    RefPtr<CSS::CSSStyleSheet> parse_sheet()
    {
        if (peek(0) == (char)0xef && peek(1) == (char)0xbb && peek(2) == (char)0xbf) {
            // HACK: Skip UTF-8 BOM.
            index += 3;
        }

        while (peek()) {
            parse_rule();
        }

        return CSS::CSSStyleSheet::create(move(rules));
    }

    RefPtr<CSS::CSSStyleDeclaration> parse_standalone_declaration()
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
        return CSS::CSSStyleDeclaration::create(move(current_rule.properties));
    }

private:
    CSS::ParsingContext m_context;

    NonnullRefPtrVector<CSS::CSSRule> rules;

    struct CurrentRule {
        Vector<CSS::Selector> selectors;
        Vector<CSS::StyleProperty> properties;
    };

    CurrentRule current_rule;
    Vector<char> buffer;

    size_t index = 0;

    StringView css;
};

Optional<CSS::Selector> parse_selector(const CSS::ParsingContext& context, const StringView& selector_text)
{
    CSSParser parser(context, selector_text);
    return parser.parse_individual_selector();
}

RefPtr<CSS::CSSStyleSheet> parse_css(const CSS::ParsingContext& context, const StringView& css)
{
    if (css.is_empty())
        return CSS::CSSStyleSheet::create({});
    CSSParser parser(context, css);
    return parser.parse_sheet();
}

RefPtr<CSS::CSSStyleDeclaration> parse_css_declaration(const CSS::ParsingContext& context, const StringView& css)
{
    if (css.is_empty())
        return CSS::CSSStyleDeclaration::create({});
    CSSParser parser(context, css);
    return parser.parse_standalone_declaration();
}

RefPtr<CSS::StyleValue> parse_html_length(const DOM::Document& document, const StringView& string)
{
    auto integer = string.to_int();
    if (integer.has_value())
        return CSS::LengthStyleValue::create(CSS::Length::make_px(integer.value()));
    return parse_css_value(CSS::ParsingContext(document), string);
}

}
