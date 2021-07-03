/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <AK/SourceLocation.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/StyleRule.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>

#define CSS_PARSER_TRACE 1

static void log_parse_error(const SourceLocation& location = SourceLocation::current())
{
    dbgln_if(CSS_PARSER_TRACE, "Parse error (CSS) {}", location);
}

namespace Web::CSS {

ParsingContext::ParsingContext()
{
}

ParsingContext::ParsingContext(DOM::Document const& document)
    : m_document(&document)
{
}

ParsingContext::ParsingContext(DOM::ParentNode const& parent_node)
    : m_document(&parent_node.document())
{
}

bool ParsingContext::in_quirks_mode() const
{
    return m_document ? m_document->in_quirks_mode() : false;
}

URL ParsingContext::complete_url(String const& addr) const
{
    return m_document ? m_document->url().complete_url(addr) : URL::create_with_url_or_path(addr);
}

Parser::Parser(ParsingContext const& context, StringView const& input, String const& encoding)
    : m_context(context)
    , m_tokenizer(input, encoding)
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
    if (m_iterator_offset < (int)m_tokens.size() - 1) {
        ++m_iterator_offset;
    }

    auto token = m_tokens.at(m_iterator_offset);

    return token;
}

Token Parser::current_token()
{
    return m_tokens.at(m_iterator_offset);
}

NonnullRefPtr<CSSStyleSheet> Parser::parse_as_stylesheet()
{
    auto parser_rules = consume_a_list_of_rules(true);
    NonnullRefPtrVector<CSSRule> rules;

    for (auto& raw_rule : parser_rules) {
        auto rule = convert_rule(raw_rule);
        if (rule)
            rules.append(*rule);
    }

    return CSSStyleSheet::create(rules);
}

Vector<Selector> Parser::parse_a_selector()
{
    auto comma_separated_lists = parse_as_comma_separated_list_of_component_values();
    return parse_a_selector(comma_separated_lists);
}

Vector<Selector> Parser::parse_a_selector(Vector<Vector<StyleComponentValueRule>>& parts)
{
    Vector<Selector> selectors;

    for (auto& selector_parts : parts) {
        auto selector = parse_single_selector(selector_parts);
        if (selector.has_value())
            selectors.append(selector.value());
    }

    return selectors;
}

Vector<Selector> Parser::parse_a_relative_selector()
{
    auto comma_separated_lists = parse_as_comma_separated_list_of_component_values();

    Vector<Selector> selectors;

    for (auto& selector_parts : comma_separated_lists) {
        auto selector = parse_single_selector(selector_parts, true);
        if (selector.has_value())
            selectors.append(selector.value());
    }

    return selectors;
}

Optional<Selector> Parser::parse_single_selector(Vector<StyleComponentValueRule> parts, bool is_relative)
{
    // FIXME: Bring this all in line with the spec. https://www.w3.org/TR/selectors-4/

    Vector<CSS::Selector::ComplexSelector> selectors;

    size_t index = 0;

    auto parse_simple_selector = [&]() -> Optional<CSS::Selector::SimpleSelector> {
        if (index >= parts.size())
            return {};

        auto& current_value = parts.at(index);
        index++;

        CSS::Selector::SimpleSelector::Type type;
        String value;
        // FIXME: Handle namespace prefixes.

        if (current_value.is(Token::TokenType::Delim) && current_value.token().delim() == "*") {

            // FIXME: Handle selectors like `*.foo`.
            type = CSS::Selector::SimpleSelector::Type::Universal;
            CSS::Selector::SimpleSelector result;
            result.type = type;
            return result;
        }

        if (current_value.is(Token::TokenType::Hash)) {
            if (current_value.token().m_hash_type != Token::HashType::Id) {
                dbgln("Selector contains hash token that is not an id: {}", current_value.to_string());
                return {};
            }
            type = CSS::Selector::SimpleSelector::Type::Id;
            value = current_value.token().m_value.to_string();
        } else if (current_value.is(Token::TokenType::Delim) && current_value.token().delim() == ".") {
            if (index >= parts.size())
                return {};

            current_value = parts.at(index);
            index++;

            if (!current_value.is(Token::TokenType::Ident)) {
                dbgln("Expected an ident after '.', got: {}", current_value.to_string());
                return {};
            }

            type = CSS::Selector::SimpleSelector::Type::Class;
            value = current_value.to_string();
        } else if (current_value.is(Token::TokenType::Delim) && current_value.token().delim() == "*") {
            type = CSS::Selector::SimpleSelector::Type::Universal;
        } else {
            type = CSS::Selector::SimpleSelector::Type::TagName;
            value = current_value.to_string().to_lowercase();
        }

        CSS::Selector::SimpleSelector simple_selector;
        simple_selector.type = type;
        simple_selector.value = value;

        if (index >= parts.size())
            return simple_selector;

        current_value = parts.at(index);
        index++;

        // FIXME: Attribute selectors want to be their own Selector::SimpleSelector::Type according to the spec.
        if (current_value.is_block() && current_value.block().is_square()) {

            Vector<StyleComponentValueRule> const& attribute_parts = current_value.block().values();

            // FIXME: Handle namespace prefix for attribute name.
            auto& attribute_part = attribute_parts.first();
            if (!attribute_part.is(Token::TokenType::Ident)) {
                dbgln("Expected ident for attribute name, got: '{}'", attribute_part.to_string());
                return {};
            }

            simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::HasAttribute;
            simple_selector.attribute_name = attribute_part.token().ident();

            size_t attribute_index = 1;
            while (attribute_parts.at(attribute_index).is(Token::TokenType::Whitespace)) {
                attribute_index++;
                if (attribute_index >= attribute_parts.size())
                    return simple_selector;
            }

            auto& delim_part = attribute_parts.at(attribute_index);
            if (!delim_part.is(Token::TokenType::Delim)) {
                dbgln("Expected a delim for attribute comparison, got: '{}'", delim_part.to_string());
                return {};
            }

            if (delim_part.token().delim() == "=") {
                simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::ExactValueMatch;
                attribute_index++;
            } else {
                attribute_index++;
                auto& delim_second_part = attribute_parts.at(attribute_index);
                if (!(delim_part.is(Token::TokenType::Delim) && delim_part.token().delim() == "=")) {
                    dbgln("Expected a double delim for attribute comparison, got: '{}{}'", delim_part.to_string(), delim_second_part.to_string());
                    return {};
                }

                if (delim_part.token().delim() == "~") {
                    simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::ContainsWord;
                    attribute_index++;
                } else if (delim_part.token().delim() == "*") {
                    simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::ContainsString;
                    attribute_index++;
                } else if (delim_part.token().delim() == "|") {
                    simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::StartsWithSegment;
                    attribute_index++;
                } else if (delim_part.token().delim() == "^") {
                    simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::StartsWithString;
                    attribute_index++;
                } else if (delim_part.token().delim() == "$") {
                    simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::EndsWithString;
                    attribute_index++;
                }
            }

            while (attribute_parts.at(attribute_index).is(Token::TokenType::Whitespace)) {
                attribute_index++;
                if (attribute_index >= attribute_parts.size()) {
                    dbgln("Attribute selector ended without a value to match.");
                    return {};
                }
            }

            auto& value_part = attribute_parts.at(attribute_index);
            if (!value_part.is(Token::TokenType::Ident) && !value_part.is(Token::TokenType::String)) {
                dbgln("Expected a string or ident for the value to match attribute against, got: '{}'", value_part.to_string());
                return {};
            }
            simple_selector.attribute_value = value_part.token().is_ident() ? value_part.token().ident() : value_part.token().string();

            // FIXME: Handle case-sensitivity suffixes. https://www.w3.org/TR/selectors-4/#attribute-case
            return simple_selector;
        }

        // FIXME: Pseudo-class selectors want to be their own Selector::SimpleSelector::Type according to the spec.
        if (current_value.is(Token::TokenType::Colon)) {
            bool is_pseudo = false;

            current_value = parts.at(index);
            index++;
            if (index >= parts.size())
                return {};

            if (current_value.is(Token::TokenType::Colon)) {
                is_pseudo = true;
                current_value = parts.at(index);
                index++;
                if (index >= parts.size())
                    return {};
            }

            // Ignore for now, otherwise we produce a "false positive" selector
            // and apply styles to the element itself, not its pseudo element
            if (is_pseudo)
                return {};

            current_value = parts.at(index);
            index++;

            if (current_value.is(Token::TokenType::Ident)) {
                auto pseudo_name = current_value.token().ident();
                if (pseudo_name.equals_ignoring_case("link")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Link;
                } else if (pseudo_name.equals_ignoring_case("visited")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Visited;
                } else if (pseudo_name.equals_ignoring_case("active")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Active;
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
                } else if (pseudo_name.equals_ignoring_case("disabled")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Disabled;
                } else if (pseudo_name.equals_ignoring_case("enabled")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Enabled;
                } else if (pseudo_name.equals_ignoring_case("checked")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Checked;
                } else {
                    dbgln("Unknown pseudo class: '{}'", pseudo_name);
                    return simple_selector;
                }
            } else if (current_value.is_function()) {
                auto& pseudo_function = current_value.function();
                if (pseudo_function.name().equals_ignoring_case("nth-child")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::NthChild;
                    simple_selector.nth_child_pattern = CSS::Selector::SimpleSelector::NthChildPattern::parse(pseudo_function.values_as_string());
                } else if (pseudo_function.name().equals_ignoring_case("nth-last-child")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::NthLastChild;
                    simple_selector.nth_child_pattern = CSS::Selector::SimpleSelector::NthChildPattern::parse(pseudo_function.values_as_string());
                } else if (pseudo_function.name().equals_ignoring_case("not")) {
                    simple_selector.pseudo_class = CSS::Selector::SimpleSelector::PseudoClass::Not;
                    simple_selector.not_selector = pseudo_function.values_as_string();
                } else {
                    dbgln("Unknown pseudo class: '{}'()", pseudo_function.name());
                    return simple_selector;
                }
            } else {
                dbgln("Unexpected Block in pseudo-class name, expected a function or identifier. '{}'", current_value.to_string());
                return simple_selector;
            }
        }

        return simple_selector;
    };

    auto parse_complex_selector = [&]() -> Optional<CSS::Selector::ComplexSelector> {
        auto relation = CSS::Selector::ComplexSelector::Relation::Descendant;

        if (index >= parts.size())
            return {};

        auto current_value = parts.at(index);
        if (current_value.is(Token::TokenType::Delim)) {
            auto delim = current_value.token().delim();
            if (delim == ">") {
                relation = CSS::Selector::ComplexSelector::Relation::ImmediateChild;
                index++;
            } else if (delim == "+") {
                relation = CSS::Selector::ComplexSelector::Relation::AdjacentSibling;
                index++;
            } else if (delim == "~") {
                relation = CSS::Selector::ComplexSelector::Relation::GeneralSibling;
                index++;
            } else if (delim == "|") {
                if (index + 1 >= parts.size())
                    return {};

                auto next = parts.at(index + 1);
                if (next.is(Token::TokenType::Delim) && next.token().delim() == "|") {
                    relation = CSS::Selector::ComplexSelector::Relation::Column;
                    index += 2;
                }
            }
        }

        Vector<CSS::Selector::SimpleSelector> simple_selectors;

        for (;;) {
            auto component = parse_simple_selector();
            if (!component.has_value())
                break;

            simple_selectors.append(component.value());
        }

        if (simple_selectors.is_empty())
            return {};

        return CSS::Selector::ComplexSelector { relation, move(simple_selectors) };
    };

    for (;;) {
        auto complex = parse_complex_selector();
        if (complex.has_value())
            selectors.append(complex.value());

        if (index >= parts.size())
            break;

        auto current_value = parts.at(index);
        if (current_value.is(Token::TokenType::Comma))
            break;

        index++;
    }

    if (selectors.is_empty())
        return {};

    if (!is_relative)
        selectors.first().relation = CSS::Selector::ComplexSelector::Relation::None;

    return Selector(move(selectors));
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

NonnullRefPtrVector<StyleRule> Parser::consume_a_list_of_rules(bool top_level)
{
    NonnullRefPtrVector<StyleRule> rules;

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
            if (maybe_qualified) {
                rules.append(maybe_qualified.release_nonnull());
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
        if (maybe_qualified) {
            rules.append(maybe_qualified.release_nonnull());
        }
    }

    return rules;
}

NonnullRefPtr<StyleRule> Parser::consume_an_at_rule()
{
    auto initial = next_token();

    NonnullRefPtr<StyleRule> rule = create<StyleRule>(StyleRule::Type::At);
    rule->m_name = initial.m_value.to_string();

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
            rule->m_block = consume_a_simple_block();
            return rule;
        }

        // how is "simple block with an associated token of <{-token>" a valid token?

        reconsume_current_input_token();
        auto value = consume_a_component_value();
        rule->m_prelude.append(value);
    }
}

RefPtr<StyleRule> Parser::consume_a_qualified_rule()
{
    NonnullRefPtr<StyleRule> rule = create<StyleRule>(StyleRule::Type::Qualified);

    for (;;) {
        auto token = next_token();

        if (token.is_eof()) {
            log_parse_error();
            return {};
        }

        if (token.is_open_curly()) {
            rule->m_block = consume_a_simple_block();
            return rule;
        }

        // how is "simple block with an associated token of <{-token>" a valid token?

        reconsume_current_input_token();
        auto value = consume_a_component_value();
        rule->m_prelude.append(value);
    }

    return rule;
}

StyleComponentValueRule Parser::consume_a_component_value()
{
    auto token = next_token();

    if (token.is_open_curly() || token.is_open_square() || token.is_open_paren())
        return StyleComponentValueRule(consume_a_simple_block());

    if (token.is_function())
        return StyleComponentValueRule(consume_a_function());

    return StyleComponentValueRule(token);
}

NonnullRefPtr<StyleBlockRule> Parser::consume_a_simple_block()
{
    auto ending_token = current_token().mirror_variant();

    NonnullRefPtr<StyleBlockRule> block = create<StyleBlockRule>();
    block->m_token = current_token();

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
        block->m_values.append(value);
    }
}

NonnullRefPtr<StyleFunctionRule> Parser::consume_a_function()
{
    NonnullRefPtr<StyleFunctionRule> function = create<StyleFunctionRule>(current_token().m_value.to_string());

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
        function->m_values.append(value.to_string());
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
            temp.append(StyleComponentValueRule(token));

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

RefPtr<CSSRule> Parser::parse_as_rule()
{
    RefPtr<CSSRule> rule;

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
    } else if (token.is_at()) {
        auto at_rule = consume_an_at_rule();
        rule = convert_rule(at_rule);
    } else {
        auto qualified_rule = consume_a_qualified_rule();
        if (!qualified_rule)
            return {};

        rule = convert_rule(*qualified_rule);
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

NonnullRefPtrVector<CSSRule> Parser::parse_as_list_of_rules()
{
    auto parsed_rules = consume_a_list_of_rules(false);
    NonnullRefPtrVector<CSSRule> rules;

    for (auto& rule : parsed_rules) {
        auto converted_rule = convert_rule(rule);
        if (converted_rule)
            rules.append(*converted_rule);
    }

    return rules;
}

Optional<StyleProperty> Parser::parse_as_declaration()
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

    auto declaration = consume_a_declaration();
    // FIXME: Return the declaration.
    return {};
}
Vector<StyleProperty> Parser::parse_as_list_of_declarations()
{
    auto declarations = consume_a_list_of_declarations();

    // FIXME: Return the declarations.
    return {};
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

Vector<Vector<StyleComponentValueRule>> Parser::parse_as_comma_separated_list_of_component_values()
{
    Vector<Vector<StyleComponentValueRule>> lists;
    lists.append({});

    for (;;) {
        auto next = next_token();

        if (next.is_comma()) {
            lists.append({});
            continue;
        } else if (next.is_eof()) {
            break;
        }

        reconsume_current_input_token();
        auto component_value = consume_a_component_value();
        lists.last().append(component_value);
    }

    for (auto& list : lists) {
        if (!list.is_empty() && list.first().is(Token::TokenType::Whitespace))
            list.take_first();

        if (!list.is_empty() && list.last().is(Token::TokenType::Whitespace))
            list.take_last();
    }

    return lists;
}

RefPtr<CSSRule> Parser::convert_rule(NonnullRefPtr<StyleRule>)
{
    return {};
}

}
