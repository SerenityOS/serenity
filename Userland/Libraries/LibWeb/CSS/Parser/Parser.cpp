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

template<typename T>
TokenStream<T>::TokenStream(Vector<T> const& tokens)
    : m_tokens(tokens)
    , m_eof(make_eof())
{
}

template<typename T>
TokenStream<T>::~TokenStream()
{
}

template<typename T>
bool TokenStream<T>::has_next_token()
{
    return (size_t)(m_iterator_offset + 1) < m_tokens.size();
}

template<typename T>
T const& TokenStream<T>::peek_token()
{
    if (!has_next_token())
        return m_eof;

    return m_tokens.at(m_iterator_offset + 1);
}

template<typename T>
T const& TokenStream<T>::next_token()
{
    if (!has_next_token())
        return m_eof;

    ++m_iterator_offset;

    return m_tokens.at(m_iterator_offset);
}

template<typename T>
T const& TokenStream<T>::current_token()
{
    if ((size_t)m_iterator_offset >= m_tokens.size())
        return m_eof;

    return m_tokens.at(m_iterator_offset);
}

template<typename T>
void TokenStream<T>::reconsume_current_input_token()
{
    VERIFY(m_iterator_offset >= 0);
    --m_iterator_offset;
}

template<typename T>
void TokenStream<T>::skip_whitespace()
{
    while (peek_token().is(Token::Type::Whitespace))
        next_token();
}

template<>
Token TokenStream<Token>::make_eof()
{
    return Tokenizer::create_eof_token();
}

template<>
StyleComponentValueRule TokenStream<StyleComponentValueRule>::make_eof()
{
    return StyleComponentValueRule(Tokenizer::create_eof_token());
}

template<typename T>
void TokenStream<T>::dump_all_tokens()
{
    dbgln("Dumping all tokens:");
    for (auto& token : m_tokens)
        dbgln("{}", token.to_string());
}

Parser::Parser(ParsingContext const& context, StringView const& input, String const& encoding)
    : m_context(context)
    , m_tokenizer(input, encoding)
    , m_tokens(m_tokenizer.parse())
    , m_token_stream(TokenStream(m_tokens))
{
}

Parser::~Parser()
{
}

NonnullRefPtr<CSSStyleSheet> Parser::parse_as_stylesheet()
{
    return parse_as_stylesheet(m_token_stream);
}

template<typename T>
NonnullRefPtr<CSSStyleSheet> Parser::parse_as_stylesheet(TokenStream<T>& tokens)
{
    auto parser_rules = consume_a_list_of_rules(tokens, true);
    NonnullRefPtrVector<CSSRule> rules;

    for (auto& raw_rule : parser_rules) {
        auto rule = convert_to_rule(raw_rule);
        if (rule)
            rules.append(*rule);
    }

    auto stylesheet = CSSStyleSheet::create(rules);
    dump_sheet(stylesheet);
    return stylesheet;
}

Vector<Selector> Parser::parse_a_selector()
{
    return parse_a_selector(m_token_stream);
}

template<typename T>
Vector<Selector> Parser::parse_a_selector(TokenStream<T>& tokens)
{
    auto comma_separated_lists = parse_as_comma_separated_list_of_component_values(tokens);
    Vector<Selector> selectors;

    for (auto& selector_parts : comma_separated_lists) {
        auto stream = TokenStream(selector_parts);
        auto selector = parse_single_selector(stream);
        if (selector.has_value())
            selectors.append(selector.value());
    }

    return selectors;
}

Vector<Selector> Parser::parse_a_relative_selector()
{
    return parse_a_relative_selector(m_token_stream);
}

template<typename T>
Vector<Selector> Parser::parse_a_relative_selector(TokenStream<T>& tokens)
{
    auto comma_separated_lists = parse_as_comma_separated_list_of_component_values(tokens);

    Vector<Selector> selectors;

    for (auto& selector_parts : comma_separated_lists) {
        auto stream = TokenStream(selector_parts);
        auto selector = parse_single_selector(stream, true);
        if (selector.has_value())
            selectors.append(selector.value());
    }

    return selectors;
}

template<typename T>
Optional<Selector> Parser::parse_single_selector(TokenStream<T>& tokens, bool is_relative)
{
    // FIXME: Bring this all in line with the spec. https://www.w3.org/TR/selectors-4/

    Vector<CSS::Selector::ComplexSelector> selectors;

    auto parse_simple_selector = [&]() -> Optional<CSS::Selector::SimpleSelector> {
        auto current_value = tokens.next_token();
        if (current_value.is(Token::Type::EndOfFile))
            return {};

        CSS::Selector::SimpleSelector::Type type;
        String value;
        // FIXME: Handle namespace prefixes.

        if (current_value.is(Token::Type::Delim) && ((Token)current_value).delim() == "*") {

            // FIXME: Handle selectors like `*.foo`.
            type = CSS::Selector::SimpleSelector::Type::Universal;
            CSS::Selector::SimpleSelector result;
            result.type = type;
            return result;
        }

        if (current_value.is(Token::Type::Hash)) {
            if (((Token)current_value).m_hash_type != Token::HashType::Id) {
                dbgln("Selector contains hash token that is not an id: {}", current_value.to_string());
                return {};
            }
            type = CSS::Selector::SimpleSelector::Type::Id;
            value = ((Token)current_value).m_value.to_string();
        } else if (current_value.is(Token::Type::Delim) && ((Token)current_value).delim() == ".") {
            current_value = tokens.next_token();
            if (current_value.is(Token::Type::EndOfFile))
                return {};

            if (!current_value.is(Token::Type::Ident)) {
                dbgln("Expected an ident after '.', got: {}", current_value.to_string());
                return {};
            }

            type = CSS::Selector::SimpleSelector::Type::Class;
            value = current_value.to_string();
        } else if (current_value.is(Token::Type::Delim) && ((Token)current_value).delim() == "*") {
            type = CSS::Selector::SimpleSelector::Type::Universal;
        } else {
            type = CSS::Selector::SimpleSelector::Type::TagName;
            value = current_value.to_string().to_lowercase();
        }

        CSS::Selector::SimpleSelector simple_selector;
        simple_selector.type = type;
        simple_selector.value = value;

        current_value = tokens.next_token();
        if (current_value.is(Token::Type::EndOfFile))
            return simple_selector;

        // FIXME: Attribute selectors want to be their own Selector::SimpleSelector::Type according to the spec.
        if (current_value.is_block() && current_value.block().is_square()) {

            Vector<StyleComponentValueRule> const& attribute_parts = current_value.block().values();

            if (attribute_parts.is_empty()) {
                dbgln("CSS attribute selector is empty!");
                return {};
            }

            // FIXME: Handle namespace prefix for attribute name.
            auto& attribute_part = attribute_parts.first();
            if (!attribute_part.is(Token::Type::Ident)) {
                dbgln("Expected ident for attribute name, got: '{}'", attribute_part.to_string());
                return {};
            }

            simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::HasAttribute;
            simple_selector.attribute_name = attribute_part.token().ident();

            size_t attribute_index = 0;
            while (attribute_parts.at(attribute_index).is(Token::Type::Whitespace)) {
                attribute_index++;
                if (attribute_index >= attribute_parts.size())
                    return simple_selector;
            }

            auto& delim_part = attribute_parts.at(attribute_index);
            if (!delim_part.is(Token::Type::Delim)) {
                dbgln("Expected a delim for attribute comparison, got: '{}'", delim_part.to_string());
                return {};
            }

            if (delim_part.token().delim() == "=") {
                simple_selector.attribute_match_type = CSS::Selector::SimpleSelector::AttributeMatchType::ExactValueMatch;
                attribute_index++;
            } else {
                attribute_index++;
                if (attribute_index >= attribute_parts.size()) {
                    dbgln("Attribute selector ended part way through a match type.");
                    return {};
                }

                auto& delim_second_part = attribute_parts.at(attribute_index);
                if (!(delim_part.is(Token::Type::Delim) && delim_part.token().delim() == "=")) {
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

            while (attribute_parts.at(attribute_index).is(Token::Type::Whitespace)) {
                attribute_index++;
                if (attribute_index >= attribute_parts.size()) {
                    dbgln("Attribute selector ended without a value to match.");
                    return {};
                }
            }

            if (attribute_index >= attribute_parts.size()) {
                dbgln("Attribute selector ended without a value to match.");
                return {};
            }

            auto& value_part = attribute_parts.at(attribute_index);
            if (!value_part.is(Token::Type::Ident) && !value_part.is(Token::Type::String)) {
                dbgln("Expected a string or ident for the value to match attribute against, got: '{}'", value_part.to_string());
                return {};
            }
            simple_selector.attribute_value = value_part.token().is(Token::Type::Ident) ? value_part.token().ident() : value_part.token().string();

            // FIXME: Handle case-sensitivity suffixes. https://www.w3.org/TR/selectors-4/#attribute-case
            return simple_selector;
        }

        // FIXME: Pseudo-class selectors want to be their own Selector::SimpleSelector::Type according to the spec.
        if (current_value.is(Token::Type::Colon)) {
            bool is_pseudo = false;

            current_value = tokens.next_token();
            if (current_value.is(Token::Type::EndOfFile))
                return {};

            if (current_value.is(Token::Type::Colon)) {
                is_pseudo = true;
                current_value = tokens.next_token();
                if (current_value.is(Token::Type::EndOfFile))
                    return {};
            }

            // Ignore for now, otherwise we produce a "false positive" selector
            // and apply styles to the element itself, not its pseudo element
            if (is_pseudo)
                return {};

            current_value = tokens.next_token();
            if (current_value.is(Token::Type::EndOfFile))
                return simple_selector;

            if (current_value.is(Token::Type::Ident)) {
                auto pseudo_name = ((Token)current_value).ident();
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
            } else if (current_value.is(Token::Type::Function)) {
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

        auto current_value = tokens.peek_token();
        if (current_value.is(Token::Type::Delim)) {
            auto delim = ((Token)current_value).delim();
            if (delim == ">") {
                relation = CSS::Selector::ComplexSelector::Relation::ImmediateChild;
                tokens.next_token();
            } else if (delim == "+") {
                relation = CSS::Selector::ComplexSelector::Relation::AdjacentSibling;
                tokens.next_token();
            } else if (delim == "~") {
                relation = CSS::Selector::ComplexSelector::Relation::GeneralSibling;
                tokens.next_token();
            } else if (delim == "|") {
                tokens.next_token();

                auto next = tokens.peek_token();
                if (next.is(Token::Type::EndOfFile))
                    return {};

                if (next.is(Token::Type::Delim) && next.token().delim() == "|") {
                    relation = CSS::Selector::ComplexSelector::Relation::Column;
                    tokens.next_token();
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

        auto current_value = tokens.peek_token();
        if (current_value.is(Token::Type::EndOfFile))
            break;
        if (current_value.is(Token::Type::Comma))
            break;

        tokens.next_token();
    }

    if (selectors.is_empty())
        return {};

    if (!is_relative)
        selectors.first().relation = CSS::Selector::ComplexSelector::Relation::None;

    return Selector(move(selectors));
}

NonnullRefPtrVector<StyleRule> Parser::consume_a_list_of_rules(bool top_level)
{
    return consume_a_list_of_rules(m_token_stream, top_level);
}

template<typename T>
NonnullRefPtrVector<StyleRule> Parser::consume_a_list_of_rules(TokenStream<T>& tokens, bool top_level)
{
    NonnullRefPtrVector<StyleRule> rules;

    for (;;) {
        auto token = tokens.next_token();

        if (token.is(Token::Type::Whitespace)) {
            continue;
        }

        if (token.is(Token::Type::EndOfFile)) {
            break;
        }

        if (token.is(Token::Type::CDO) || token.is(Token::Type::CDC)) {
            if (top_level) {
                continue;
            }

            tokens.reconsume_current_input_token();
            auto maybe_qualified = consume_a_qualified_rule(tokens);
            if (maybe_qualified) {
                rules.append(maybe_qualified.release_nonnull());
            }

            continue;
        }

        if (token.is(Token::Type::AtKeyword)) {
            tokens.reconsume_current_input_token();
            rules.append(consume_an_at_rule(tokens));
            continue;
        }

        tokens.reconsume_current_input_token();
        auto maybe_qualified = consume_a_qualified_rule(tokens);
        if (maybe_qualified) {
            rules.append(maybe_qualified.release_nonnull());
        }
    }

    return rules;
}

NonnullRefPtr<StyleRule> Parser::consume_an_at_rule()
{
    return consume_an_at_rule(m_token_stream);
}

template<typename T>
NonnullRefPtr<StyleRule> Parser::consume_an_at_rule(TokenStream<T>& tokens)
{
    auto name_ident = tokens.next_token();
    VERIFY(name_ident.is(Token::Type::Ident));

    NonnullRefPtr<StyleRule> rule = create<StyleRule>(StyleRule::Type::At);
    rule->m_name = ((Token)name_ident).ident();

    for (;;) {
        auto token = tokens.next_token();
        if (token.is(Token::Type::Semicolon)) {
            return rule;
        }

        if (token.is(Token::Type::EndOfFile)) {
            log_parse_error();
            return rule;
        }

        if (token.is(Token::Type::OpenCurly)) {
            rule->m_block = consume_a_simple_block(tokens);
            return rule;
        }

        // how is "simple block with an associated token of <{-token>" a valid token?

        tokens.reconsume_current_input_token();
        auto value = consume_a_component_value(tokens);
        rule->m_prelude.append(value);
    }
}

RefPtr<StyleRule> Parser::consume_a_qualified_rule()
{
    return consume_a_qualified_rule(m_token_stream);
}

template<typename T>
RefPtr<StyleRule> Parser::consume_a_qualified_rule(TokenStream<T>& tokens)
{
    NonnullRefPtr<StyleRule> rule = create<StyleRule>(StyleRule::Type::Qualified);

    for (;;) {
        auto token = tokens.next_token();

        if (token.is(Token::Type::EndOfFile)) {
            log_parse_error();
            return {};
        }

        if (token.is(Token::Type::OpenCurly)) {
            rule->m_block = consume_a_simple_block(tokens);
            return rule;
        }

        // how is "simple block with an associated token of <{-token>" a valid token?

        tokens.reconsume_current_input_token();
        auto value = consume_a_component_value(tokens);
        rule->m_prelude.append(value);
    }

    return rule;
}

template<>
StyleComponentValueRule Parser::consume_a_component_value(TokenStream<StyleComponentValueRule>& tokens)
{
    return tokens.next_token();
}

template<typename T>
StyleComponentValueRule Parser::consume_a_component_value(TokenStream<T>& tokens)
{
    auto token = tokens.next_token();

    if (token.is(Token::Type::OpenCurly) || token.is(Token::Type::OpenSquare) || token.is(Token::Type::OpenParen))
        return StyleComponentValueRule(consume_a_simple_block(tokens));

    if (token.is(Token::Type::Function))
        return StyleComponentValueRule(consume_a_function(tokens));

    return StyleComponentValueRule(token);
}

StyleComponentValueRule Parser::consume_a_component_value()
{
    return consume_a_component_value(m_token_stream);
}

NonnullRefPtr<StyleBlockRule> Parser::consume_a_simple_block()
{
    return consume_a_simple_block(m_token_stream);
}

template<typename T>
NonnullRefPtr<StyleBlockRule> Parser::consume_a_simple_block(TokenStream<T>& tokens)
{
    auto ending_token = ((Token)tokens.current_token()).mirror_variant();

    NonnullRefPtr<StyleBlockRule> block = create<StyleBlockRule>();
    block->m_token = tokens.current_token();

    for (;;) {
        auto token = tokens.next_token();

        if (token.is(ending_token)) {
            return block;
        }

        if (token.is(Token::Type::EndOfFile)) {
            log_parse_error();
            return block;
        }

        tokens.reconsume_current_input_token();
        auto value = consume_a_component_value(tokens);
        if (value.is(Token::Type::Whitespace))
            continue;

        block->m_values.append(value);
    }
}

NonnullRefPtr<StyleFunctionRule> Parser::consume_a_function()
{
    return consume_a_function(m_token_stream);
}

template<typename T>
NonnullRefPtr<StyleFunctionRule> Parser::consume_a_function(TokenStream<T>& tokens)
{
    auto name_ident = tokens.current_token();
    VERIFY(name_ident.is(Token::Type::Function));
    NonnullRefPtr<StyleFunctionRule> function = create<StyleFunctionRule>(((Token)name_ident).m_value.to_string());

    for (;;) {
        auto token = tokens.next_token();
        if (token.is(Token::Type::CloseParen)) {
            return function;
        }

        if (token.is(Token::Type::EndOfFile)) {
            log_parse_error();
            return function;
        }

        tokens.reconsume_current_input_token();
        auto value = consume_a_component_value(tokens);
        if (value.is(Token::Type::Whitespace))
            continue;

        function->m_values.append(value.to_string());
    }

    return function;
}

Optional<StyleDeclarationRule> Parser::consume_a_declaration()
{
    return consume_a_declaration(m_token_stream);
}

template<typename T>
Optional<StyleDeclarationRule> Parser::consume_a_declaration(TokenStream<T>& tokens)
{
    auto token = tokens.next_token();

    StyleDeclarationRule declaration;
    VERIFY(token.is(Token::Type::Ident));
    declaration.m_name = ((Token)token).ident();

    tokens.skip_whitespace();

    auto colon = tokens.next_token();
    if (!colon.is(Token::Type::Colon)) {
        log_parse_error();
        return {};
    }

    tokens.skip_whitespace();

    for (;;) {
        if (tokens.peek_token().is(Token::Type::EndOfFile)) {
            break;
        }
        declaration.m_values.append(consume_a_component_value(tokens));
    }

    if (declaration.m_values.size() >= 2) {
        auto second_last = declaration.m_values.at(declaration.m_values.size() - 2);
        auto last = declaration.m_values.at(declaration.m_values.size() - 1);

        if (second_last.m_type == StyleComponentValueRule::ComponentType::Token && last.m_type == StyleComponentValueRule::ComponentType::Token) {
            auto last_token = last.m_token;
            auto second_last_token = second_last.m_token;

            if (second_last_token.is(Token::Type::Delim) && second_last_token.m_value.to_string().equals_ignoring_case("!")) {
                if (last_token.is(Token::Type::Ident) && last_token.m_value.to_string().equals_ignoring_case("important")) {
                    declaration.m_values.remove(declaration.m_values.size() - 2);
                    declaration.m_values.remove(declaration.m_values.size() - 1);
                    declaration.m_important = true;
                }
            }
        }
    }

    for (;;) {
        auto maybe_whitespace = declaration.m_values.at(declaration.m_values.size() - 1);
        if (!(maybe_whitespace.is(Token::Type::Whitespace))) {
            break;
        }
        declaration.m_values.remove(declaration.m_values.size() - 1);
    }

    return declaration;
}

Vector<DeclarationOrAtRule> Parser::consume_a_list_of_declarations()
{
    return consume_a_list_of_declarations(m_token_stream);
}

template<typename T>
Vector<DeclarationOrAtRule> Parser::consume_a_list_of_declarations(TokenStream<T>& tokens)
{
    Vector<DeclarationOrAtRule> list;

    for (;;) {
        auto token = tokens.next_token();
        if (token.is(Token::Type::Whitespace) || token.is(Token::Type::Semicolon)) {
            continue;
        }

        if (token.is(Token::Type::EndOfFile)) {
            return list;
        }

        if (token.is(Token::Type::AtKeyword)) {
            tokens.reconsume_current_input_token();
            list.append(DeclarationOrAtRule(consume_an_at_rule(tokens)));
            continue;
        }

        if (token.is(Token::Type::Ident)) {
            Vector<StyleComponentValueRule> temp;
            temp.append(token);

            for (;;) {
                auto peek = tokens.peek_token();
                if (peek.is(Token::Type::Semicolon) || peek.is(Token::Type::EndOfFile)) {
                    break;
                }
                temp.append(consume_a_component_value(tokens));
            }

            auto token_stream = TokenStream(temp);
            auto maybe_declaration = consume_a_declaration(token_stream);
            if (maybe_declaration.has_value()) {
                list.append(DeclarationOrAtRule(maybe_declaration.value()));
            }
            continue;
        }

        log_parse_error();
        tokens.reconsume_current_input_token();
        auto peek = tokens.peek_token();
        if (!(peek.is(Token::Type::Semicolon) || peek.is(Token::Type::EndOfFile))) {
            (void)consume_a_component_value(tokens);
        }
    }

    return list;
}

RefPtr<CSSRule> Parser::parse_as_rule()
{
    return parse_as_rule(m_token_stream);
}

template<typename T>
RefPtr<CSSRule> Parser::parse_as_rule(TokenStream<T>& tokens)
{
    RefPtr<CSSRule> rule;

    tokens.skip_whitespace();

    auto token = tokens.peek_token();

    if (token.is(Token::Type::EndOfFile)) {
        return {};
    } else if (token.is(Token::Type::AtKeyword)) {
        auto at_rule = consume_an_at_rule();
        rule = convert_to_rule(at_rule);
    } else {
        auto qualified_rule = consume_a_qualified_rule(tokens);
        if (!qualified_rule)
            return {};

        rule = convert_to_rule(*qualified_rule);
    }

    tokens.skip_whitespace();

    auto maybe_eof = tokens.peek_token();
    if (maybe_eof.is(Token::Type::EndOfFile)) {
        return rule;
    }

    return {};
}

NonnullRefPtrVector<CSSRule> Parser::parse_as_list_of_rules()
{
    return parse_as_list_of_rules(m_token_stream);
}

template<typename T>
NonnullRefPtrVector<CSSRule> Parser::parse_as_list_of_rules(TokenStream<T>& tokens)
{
    auto parsed_rules = consume_a_list_of_rules(tokens, false);
    NonnullRefPtrVector<CSSRule> rules;

    for (auto& rule : parsed_rules) {
        auto converted_rule = convert_to_rule(rule);
        if (converted_rule)
            rules.append(*converted_rule);
    }

    return rules;
}

Optional<StyleProperty> Parser::parse_as_declaration()
{
    return parse_as_declaration(m_token_stream);
}

template<typename T>
Optional<StyleProperty> Parser::parse_as_declaration(TokenStream<T>& tokens)
{
    tokens.skip_whitespace();

    auto token = tokens.peek_token();

    if (!token.is(Token::Type::Ident)) {
        return {};
    }

    auto declaration = consume_a_declaration(tokens);
    // FIXME: Return declaration

    return {};
}

RefPtr<CSSStyleDeclaration> Parser::parse_as_list_of_declarations()
{
    return parse_as_list_of_declarations(m_token_stream);
}

template<typename T>
RefPtr<CSSStyleDeclaration> Parser::parse_as_list_of_declarations(TokenStream<T>&)
{
    // FIXME: Return the declarations.
    return {};
}

Optional<StyleComponentValueRule> Parser::parse_as_component_value()
{
    return parse_as_component_value(m_token_stream);
}

template<typename T>
Optional<StyleComponentValueRule> Parser::parse_as_component_value(TokenStream<T>& tokens)
{
    tokens.skip_whitespace();

    auto token = tokens.peek_token();

    if (token.is(Token::Type::EndOfFile)) {
        return {};
    }

    auto value = consume_a_component_value(tokens);

    tokens.skip_whitespace();

    auto maybe_eof = tokens.peek_token();
    if (maybe_eof.is(Token::Type::EndOfFile)) {
        return value;
    }

    return {};
}

Vector<StyleComponentValueRule> Parser::parse_as_list_of_component_values()
{
    return parse_as_list_of_component_values(m_token_stream);
}

template<typename T>
Vector<StyleComponentValueRule> Parser::parse_as_list_of_component_values(TokenStream<T>& tokens)
{
    Vector<StyleComponentValueRule> rules;

    for (;;) {
        if (tokens.peek_token().is(Token::Type::EndOfFile)) {
            break;
        }

        rules.append(consume_a_component_value(tokens));
    }

    return rules;
}

Vector<Vector<StyleComponentValueRule>> Parser::parse_as_comma_separated_list_of_component_values()
{
    return parse_as_comma_separated_list_of_component_values(m_token_stream);
}

template<typename T>
Vector<Vector<StyleComponentValueRule>> Parser::parse_as_comma_separated_list_of_component_values(TokenStream<T>& tokens)
{
    Vector<Vector<StyleComponentValueRule>> lists;
    lists.append({});

    for (;;) {
        auto next = tokens.next_token();

        if (next.is(Token::Type::Comma)) {
            lists.append({});
            continue;
        } else if (next.is(Token::Type::EndOfFile)) {
            break;
        }

        tokens.reconsume_current_input_token();
        auto component_value = consume_a_component_value(tokens);
        lists.last().append(component_value);
    }

    return lists;
}

RefPtr<CSSRule> Parser::convert_to_rule(NonnullRefPtr<StyleRule> rule)
{
    dbgln("Converting a rule: {}", rule->to_string());

    if (rule->m_type == StyleRule::Type::At) {
        dbgln("... It's an at rule");
    } else {
        dbgln("... It's a style rule");

        auto prelude_stream = TokenStream(rule->m_prelude);
        Vector<Selector> selectors = parse_a_selector(prelude_stream);
        auto declaration = convert_to_declaration(*rule->m_block);
        if (declaration && !selectors.is_empty())
            return CSSStyleRule::create(move(selectors), move(*declaration));
    }

    dbgln("... discarding because it's invalid or unsupported.");
    return {};
}

RefPtr<CSSStyleDeclaration> Parser::convert_to_declaration(NonnullRefPtr<StyleBlockRule> block)
{
    if (!block->is_curly())
        return {};

    Vector<StyleProperty> properties;
    HashMap<String, StyleProperty> custom_properties;

    auto stream = TokenStream(block->m_values);
    auto declarations_and_at_rules = consume_a_list_of_declarations(stream);

    for (auto& declaration_or_at_rule : declarations_and_at_rules) {
        if (declaration_or_at_rule.is_at_rule()) {
            dbgln("CSS::Parser::convert_to_declaration(): Skipping @ rule.");
            continue;
        }

        auto& declaration = declaration_or_at_rule.m_declaration;

        auto& property_name = declaration.m_name;
        auto property_id = property_id_from_string(property_name);
        if (property_id == CSS::PropertyID::Invalid && property_name.starts_with("--"))
            property_id = CSS::PropertyID::Custom;

        if (property_id == CSS::PropertyID::Invalid && !property_name.starts_with("-")) {
            dbgln("CSS::Parser::convert_to_declaration(): Unrecognized property '{}'", property_name);
            continue;
        }

        auto value_token_stream = TokenStream(declaration.m_values);
        auto value = parse_css_value(property_id, value_token_stream);
        if (!value) {
            dbgln("CSS::Parser::convert_to_declaration(): Property '{}' has no value.", property_name);
            continue;
        }

        if (property_id == CSS::PropertyID::Custom) {
            custom_properties.set(property_name, CSS::StyleProperty { property_id, value.release_nonnull(), declaration.m_name, declaration.m_important });
        } else {
            properties.append(CSS::StyleProperty { property_id, value.release_nonnull(), {}, declaration.m_important });
        }
    }

    return CSSStyleDeclaration::create(move(properties), move(custom_properties));
}

template<typename T>
RefPtr<StyleValue> Parser::parse_css_value(PropertyID, TokenStream<T>&)
{
    // FIXME: This is mostly copied from the old, deprecated parser. It may or may not be to spec.

    return {};
}
}
