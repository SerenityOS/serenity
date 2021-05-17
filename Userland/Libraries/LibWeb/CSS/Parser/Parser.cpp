/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SourceLocation.h>
#include <LibWeb/CSS/Parser/AtStyleRule.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/Parser/QualifiedStyleRule.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/Dump.h>

#define CSS_PARSER_TRACE 1

static void log_parse_error(const SourceLocation& location = SourceLocation::current())
{
    dbgln_if(CSS_PARSER_TRACE, "Parse error (CSS) {}", location);
}

namespace Web::CSS {

Parser::Parser(const StringView& input, const String& encoding)
    : m_tokenizer(input, encoding)
{
    m_tokens = m_tokenizer.parse();
}

Parser::~Parser()
{
}

Token Parser::peek_token()
{
    size_t next_offset = m_iterator_offset + 1;

    if (next_offset < m_tokens.size()) {
        return m_tokens.at(next_offset);
    }

    return m_tokens.at(m_iterator_offset);
}

Token Parser::next_token()
{
    if (m_iterator_offset < (int)m_tokens.size()) {
        ++m_iterator_offset;
    }

    auto token = m_tokens.at(m_iterator_offset);

    return token;
}

Token Parser::current_token()
{
    return m_tokens.at(m_iterator_offset);
}

Vector<QualifiedStyleRule> Parser::parse_as_stylesheet()
{
    auto rules = consume_a_list_of_rules(true);

    dbgln("Printing rules:");

    for (auto& rule : rules) {
        dbgln("PRE:");
        for (auto& pre : rule.m_prelude) {
            dbgln("{}", pre);
        }
        dbgln("BLOCK:");
        dbgln("{}", rule.m_block.to_string());
        dbgln("");

        auto selectors = parse_selectors(rule.m_prelude);
        CSS::Selector selector = Selector(move(selectors));
        dump_selector(selector);
    }

    return rules;
}

Vector<CSS::Selector::ComplexSelector> Parser::parse_selectors(Vector<String> parts)
{
    // TODO:
    // This is a mess because the prelude is parsed as a string.
    // It should really be parsed as its class, but the cpp gods have forsaken me
    // and I can't make it work due to cyclic includes.

    Vector<CSS::Selector::ComplexSelector> selectors;

    size_t index = 0;
    auto parse_simple_selector = [&]() -> Optional<CSS::Selector::SimpleSelector> {
        if (index >= parts.size()) {
            return {};
        }

        auto currentToken = parts.at(index);
        CSS::Selector::SimpleSelector::Type type;
        if (currentToken == "*") {
            type = CSS::Selector::SimpleSelector::Type::Universal;
            index++;
            CSS::Selector::SimpleSelector result;
            result.type = type;
            return result;
        }

        if (currentToken == ".") {
            type = CSS::Selector::SimpleSelector::Type::Class;
        } else if (currentToken == "#") {
            type = CSS::Selector::SimpleSelector::Type::Id;
        } else if (currentToken == "*") {
            type = CSS::Selector::SimpleSelector::Type::Universal;
        } else {
            type = CSS::Selector::SimpleSelector::Type::TagName;
        }

        index++;
        auto value = currentToken;

        if (type == CSS::Selector::SimpleSelector::Type::TagName) {
            value = value.to_lowercase();
        }

        CSS::Selector::SimpleSelector simple_selector;
        simple_selector.type = type;
        simple_selector.value = value;

        if (index >= parts.size()) {
            return simple_selector;
        }

        currentToken = parts.at(index);
        if (currentToken.starts_with('[')) {
            auto adjusted = currentToken.substring(1, currentToken.length() - 2);

            // TODO: split on String :^)
            Vector<String> attribute_parts = adjusted.split(',');

            simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::HasAttribute;
            simple_selector.attribute_name = attribute_parts.first();

            size_t attribute_index = 1;
            if (attribute_index >= attribute_parts.size()) {
                return simple_selector;
            }

            if (attribute_parts.at(attribute_index) == " =") {
                simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::ExactValueMatch;
                attribute_index++;
            }

            if (attribute_parts.at(attribute_index) == " ~") {
                simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::Contains;
                attribute_index += 2;
            }

            if (attribute_parts.at(attribute_index) == " |") {
                simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::StartsWith;
                attribute_index += 2;
            }

            simple_selector.attribute_value = attribute_parts.at(attribute_index);
            return simple_selector;
        }

        if (currentToken == ":") {
            bool is_pseudo = false;
            index++;

            if (index >= parts.size()) {
                return {};
            }

            currentToken = parts.at(index);
            if (currentToken == ":") {
                is_pseudo = true;
                index++;
            }

            if (index >= parts.size()) {
                return {};
            }

            currentToken = parts.at(index);
            auto pseudo_name = currentToken;
            index++;

            // Ignore for now, otherwise we produce a "false positive" selector
            // and apply styles to the element itself, not its pseudo element
            if (is_pseudo) {
                return {};
            }

            if (pseudo_name.equals_ignoring_case("link")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Link;
            } else if (pseudo_name.equals_ignoring_case("visited")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Visited;
            } else if (pseudo_name.equals_ignoring_case("hover")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Hover;
            } else if (pseudo_name.equals_ignoring_case("focus")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Focus;
            } else if (pseudo_name.equals_ignoring_case("first-child")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::FirstChild;
            } else if (pseudo_name.equals_ignoring_case("last-child")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::LastChild;
            } else if (pseudo_name.equals_ignoring_case("only-child")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::OnlyChild;
            } else if (pseudo_name.equals_ignoring_case("empty")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Empty;
            } else if (pseudo_name.equals_ignoring_case("root")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Root;
            } else if (pseudo_name.equals_ignoring_case("first-of-type")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::FirstOfType;
            } else if (pseudo_name.equals_ignoring_case("last-of-type")) {
                simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::LastOfType;
            } else if (pseudo_name.equals_ignoring_case("before")) {
                simple_selector.pseudo_element = CSS::Selector::SimpleSelector::PseudoElement::Before;
            } else if (pseudo_name.equals_ignoring_case("after")) {
                simple_selector.pseudo_element = CSS::Selector::SimpleSelector::PseudoElement::After;
            } else {
                dbgln("Unknown pseudo class: '{}'", pseudo_name);
                return simple_selector;
            }
        }

        return simple_selector;
    };

    auto parse_complex_selector = [&]() -> Optional<CSS::Selector::ComplexSelector> {
        auto relation = CSS::Selector::ComplexSelector::Relation::Descendant;

        auto currentToken = parts.at(index);
        if (is_combinator(currentToken)) {
            if (currentToken == ">") {
                relation = CSS::Selector::ComplexSelector::Relation::ImmediateChild;
            }
            if (currentToken == "+") {
                relation = CSS::Selector::ComplexSelector::Relation::AdjacentSibling;
            }
            if (currentToken == "~") {
                relation = CSS::Selector::ComplexSelector::Relation::GeneralSibling;
            }
            if (currentToken == "||") {
                relation = CSS::Selector::ComplexSelector::Relation::Column;
            }
            index++;
        }

        Vector<CSS::Selector::SimpleSelector> simple_selectors;

        for (;;) {
            auto component = parse_simple_selector();
            if (!component.has_value()) {
                break;
            }
            simple_selectors.append(component.value());
        }

        if (simple_selectors.is_empty())
            return {};

        return CSS::Selector::ComplexSelector { relation, move(simple_selectors) };
    };

    for (;;) {
        auto complex = parse_complex_selector();
        if (complex.has_value()) {
            selectors.append(complex.value());
        }

        if (index >= parts.size()) {
            break;
        }

        auto currentToken = parts.at(index);
        if (currentToken != ",") {
            break;
        }

        index++;
    }

    if (selectors.is_empty()) {
        return {};
    }

    selectors.first().relation = CSS::Selector::ComplexSelector::Relation::None;

    return selectors;
}

void Parser::dump_all_tokens()
{
    dbgln("Dumping all tokens:");
    for (auto& token : m_tokens)
        dbgln("{}", token.to_string());
}

void Parser::reconsume_current_input_token()
{
    --m_iterator_offset;
}

bool Parser::is_combinator(String input)
{
    return input == ">" || input == "+" || input == "~" || input == "||";
}

Vector<QualifiedStyleRule> Parser::consume_a_list_of_rules(bool top_level)
{
    Vector<QualifiedStyleRule> rules;

    for (;;) {
        auto token = next_token();

        if (token.is_whitespace()) {
            continue;
        }

        if (token.is_eof()) {
            break;
        }

        if (token.is_cdo() || token.is_cdc()) {
            if (top_level) {
                continue;
            }

            reconsume_current_input_token();
            auto maybe_qualified = consume_a_qualified_rule();
            if (maybe_qualified.has_value()) {
                rules.append(maybe_qualified.value());
            }

            continue;
        }

        if (token.is_at()) {
            reconsume_current_input_token();
            rules.append(consume_an_at_rule());
            continue;
        }

        reconsume_current_input_token();
        auto maybe_qualified = consume_a_qualified_rule();
        if (maybe_qualified.has_value()) {
            rules.append(maybe_qualified.value());
        }
    }

    return rules;
}

AtStyleRule Parser::consume_an_at_rule()
{
    auto initial = next_token();

    AtStyleRule rule;
    rule.m_name = initial.m_value.to_string();

    for (;;) {
        auto token = next_token();
        if (token.is_semicolon()) {
            return rule;
        }

        if (token.is_eof()) {
            log_parse_error();
            return rule;
        }

        if (token.is_open_curly()) {
            rule.m_block = consume_a_simple_block();
            return rule;
        }

        // how is "simple block with an associated token of <{-token>" a valid token?

        reconsume_current_input_token();
        auto value = consume_a_component_value();
        if (value.m_type == StyleComponentValueRule::ComponentType::Token) {
            if (value.m_token.is_whitespace()) {
                continue;
            }
        }
        rule.m_prelude.append(value.to_string());
    }
}

Optional<QualifiedStyleRule> Parser::consume_a_qualified_rule()
{
    QualifiedStyleRule rule;

    for (;;) {
        auto token = next_token();

        if (token.is_eof()) {
            log_parse_error();
            return {};
        }

        if (token.is_open_curly()) {
            rule.m_block = consume_a_simple_block();
            return rule;
        }

        // how is "simple block with an associated token of <{-token>" a valid token?

        reconsume_current_input_token();
        auto value = consume_a_component_value();
        if (value.m_type == StyleComponentValueRule::ComponentType::Token) {
            if (value.m_token.is_whitespace()) {
                continue;
            }
        }
        rule.m_prelude.append(value.to_string());
    }

    return rule;
}

StyleComponentValueRule Parser::consume_a_component_value()
{
    auto token = next_token();

    if (token.is_open_curly() || token.is_open_square() || token.is_open_paren()) {
        auto component = StyleComponentValueRule(StyleComponentValueRule::ComponentType::Block);
        component.m_block = consume_a_simple_block();
        return component;
    }

    if (token.is_function()) {
        auto component = StyleComponentValueRule(StyleComponentValueRule::ComponentType::Function);
        component.m_function = consume_a_function();
        return component;
    }

    auto component = StyleComponentValueRule(StyleComponentValueRule::ComponentType::Token);
    component.m_token = token;
    return component;
}

StyleBlockRule Parser::consume_a_simple_block()
{
    auto ending_token = current_token().mirror_variant();

    StyleBlockRule block;
    block.m_token = current_token();

    for (;;) {
        auto token = next_token();

        if (token.m_type == ending_token) {
            return block;
        }

        if (token.is_eof()) {
            log_parse_error();
            return block;
        }

        reconsume_current_input_token();
        auto value = consume_a_component_value();
        if (value.m_type == StyleComponentValueRule::ComponentType::Token) {
            if (value.m_token.is_whitespace()) {
                continue;
            }
        }
        block.m_values.append(value.to_string());
    }
}

StyleFunctionRule Parser::consume_a_function()
{
    StyleFunctionRule function;
    function.m_name = current_token().m_value.to_string();

    for (;;) {
        auto token = next_token();
        if (token.is_close_paren()) {
            return function;
        }

        if (token.is_eof()) {
            log_parse_error();
            return function;
        }

        reconsume_current_input_token();
        auto value = consume_a_component_value();
        if (value.m_type == StyleComponentValueRule::ComponentType::Token) {
            if (value.m_token.is_whitespace()) {
                continue;
            }
        }
        function.m_values.append(value.to_string());
    }

    return function;
}
Optional<StyleDeclarationRule> Parser::consume_a_declaration(Vector<StyleComponentValueRule>)
{
    TODO();
}

Optional<StyleDeclarationRule> Parser::consume_a_declaration()
{
    auto token = next_token();

    StyleDeclarationRule declaration;
    declaration.m_name = token.m_value.to_string();

    for (;;) {
        if (!peek_token().is_whitespace()) {
            break;
        }
        next_token();
    }

    auto colon = next_token();

    if (!colon.is_colon()) {
        log_parse_error();
        return {};
    }

    for (;;) {
        if (!peek_token().is_whitespace()) {
            break;
        }
        next_token();
    }

    for (;;) {
        if (peek_token().is_eof()) {
            break;
        }
        declaration.m_values.append(consume_a_component_value());
    }

    auto second_last = declaration.m_values.at(declaration.m_values.size() - 2);
    auto last = declaration.m_values.at(declaration.m_values.size() - 1);

    if (second_last.m_type == StyleComponentValueRule::ComponentType::Token && last.m_type == StyleComponentValueRule::ComponentType::Token) {
        auto last_token = last.m_token;
        auto second_last_token = second_last.m_token;

        if (second_last_token.is_delim() && second_last_token.m_value.to_string().equals_ignoring_case("!")) {
            if (last_token.is_ident() && last_token.m_value.to_string().equals_ignoring_case("important")) {
                declaration.m_values.remove(declaration.m_values.size() - 2);
                declaration.m_values.remove(declaration.m_values.size() - 1);
                declaration.m_important = true;
            }
        }
    }

    for (;;) {
        auto maybe_whitespace = declaration.m_values.at(declaration.m_values.size() - 1);
        if (!(maybe_whitespace.m_type == StyleComponentValueRule::ComponentType::Token && maybe_whitespace.m_token.is_whitespace())) {
            break;
        }
        declaration.m_values.remove(declaration.m_values.size() - 1);
    }

    return declaration;
}

Vector<DeclarationOrAtRule> Parser::consume_a_list_of_declarations()
{
    Vector<DeclarationOrAtRule> list;

    for (;;) {
        auto token = next_token();
        if (token.is_whitespace() || token.is_semicolon()) {
            continue;
        }

        if (token.is_eof()) {
            return list;
        }

        if (token.is_at()) {
            reconsume_current_input_token();
            list.append(DeclarationOrAtRule(consume_an_at_rule()));
            continue;
        }

        if (token.is_ident()) {
            Vector<StyleComponentValueRule> temp;

            auto component = StyleComponentValueRule(StyleComponentValueRule::ComponentType::Token);
            component.m_token = token;
            temp.append(component);

            for (;;) {
                auto peek = peek_token();
                if (peek.is_semicolon() || peek.is_eof()) {
                    break;
                }
                temp.append(consume_a_component_value());
            }

            auto maybe_declaration = consume_a_declaration(temp);
            if (maybe_declaration.has_value()) {
                list.append(DeclarationOrAtRule(maybe_declaration.value()));
            }
        }

        log_parse_error();
        reconsume_current_input_token();
        auto peek = peek_token();
        if (!(peek.is_semicolon() || peek.is_eof())) {
            consume_a_component_value();
        }
    }

    return list;
}

Optional<QualifiedStyleRule> Parser::parse_as_rule()
{
    Optional<QualifiedStyleRule> rule;

    for (;;) {
        auto maybe_whitespace = peek_token();
        if (!maybe_whitespace.is_whitespace()) {
            break;
        }
        next_token();
    }

    auto token = peek_token();

    if (token.is_eof()) {
        return {};
    }

    if (token.is_at()) {
        rule = consume_an_at_rule();
    } else {
        rule = consume_a_qualified_rule();
    }

    for (;;) {
        auto maybe_whitespace = peek_token();
        if (!maybe_whitespace.is_whitespace()) {
            break;
        }
        next_token();
    }

    auto maybe_eof = peek_token();
    if (maybe_eof.is_eof()) {
        return rule;
    }

    return {};
}

Vector<QualifiedStyleRule> Parser::parse_as_list_of_rules()
{
    return consume_a_list_of_rules(false);
}

Optional<StyleDeclarationRule> Parser::parse_as_declaration()
{
    for (;;) {
        auto maybe_whitespace = peek_token();
        if (!maybe_whitespace.is_whitespace()) {
            break;
        }
        next_token();
    }

    auto token = peek_token();

    if (!token.is_ident()) {
        return {};
    }

    return consume_a_declaration();
}
Vector<DeclarationOrAtRule> Parser::parse_as_list_of_declarations()
{
    return consume_a_list_of_declarations();
}

Optional<StyleComponentValueRule> Parser::parse_as_component_value()
{
    for (;;) {
        auto maybe_whitespace = peek_token();
        if (!maybe_whitespace.is_whitespace()) {
            break;
        }
        next_token();
    }

    auto token = peek_token();

    if (token.is_eof()) {
        return {};
    }

    auto value = consume_a_component_value();

    for (;;) {
        auto maybe_whitespace = peek_token();
        if (!maybe_whitespace.is_whitespace()) {
            break;
        }
        next_token();
    }

    auto maybe_eof = peek_token();
    if (maybe_eof.is_eof()) {
        return value;
    }

    return {};
}
Vector<StyleComponentValueRule> Parser::parse_as_list_of_component_values()
{
    Vector<StyleComponentValueRule> rules;

    for (;;) {
        if (peek_token().is_eof()) {
            break;
        }

        rules.append(consume_a_component_value());
    }

    return rules;
}

Vector<StyleComponentValueRule> Parser::parse_as_list_of_comma_separated_component_values()
{
    Vector<StyleComponentValueRule> rules;

    for (;;) {
        rules.append(consume_a_component_value());

        if (peek_token().is_comma())
            continue;
        if (peek_token().is_eof())
            break;
    }

    return rules;
}

}
