/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
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

static void log_parse_error(const SourceLocation& location = SourceLocation::current())
{
    dbgln_if(CSS_PARSER_DEBUG, "Parse error (CSS) {}", location);
}

namespace Web::CSS {

ParsingContext::ParsingContext()
{
}

ParsingContext::ParsingContext(DOM::Document& document)
    : m_document(&document)
{
}

ParsingContext::ParsingContext(DOM::ParentNode& parent_node)
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
    for (size_t i = 0; i < m_tokens.size(); ++i) {
        auto& token = m_tokens[i];
        if ((i - 1) == (size_t)m_iterator_offset)
            dbgln("-> {}", token.to_debug_string());
        else
            dbgln("   {}", token.to_debug_string());
    }
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_stylesheet");

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

Optional<SelectorList> Parser::parse_a_selector()
{
    return parse_a_selector(m_token_stream);
}

template<typename T>
Optional<SelectorList> Parser::parse_a_selector(TokenStream<T>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_a_selector");

    auto selector_list = parse_a_selector_list(tokens);
    if (selector_list.has_value())
        return selector_list;

    return {};
}

Optional<SelectorList> Parser::parse_a_relative_selector()
{
    return parse_a_relative_selector(m_token_stream);
}

template<typename T>
Optional<SelectorList> Parser::parse_a_relative_selector(TokenStream<T>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_a_relative_selector");

    auto selector_list = parse_a_relative_selector_list(tokens);
    if (selector_list.has_value())
        return selector_list;

    return {};
}

template<typename T>
Optional<SelectorList> Parser::parse_a_selector_list(TokenStream<T>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_a_selector_list");

    auto comma_separated_lists = parse_as_comma_separated_list_of_component_values(tokens);

    NonnullRefPtrVector<Selector> selectors;
    for (auto& selector_parts : comma_separated_lists) {
        auto stream = TokenStream(selector_parts);
        auto selector = parse_complex_selector(stream, false);
        if (selector)
            selectors.append(selector.release_nonnull());
        else
            return {};
    }

    if (selectors.is_empty())
        return {};

    return selectors;
}

template<typename T>
Optional<SelectorList> Parser::parse_a_relative_selector_list(TokenStream<T>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_a_relative_selector_list");

    auto comma_separated_lists = parse_as_comma_separated_list_of_component_values(tokens);

    NonnullRefPtrVector<Selector> selectors;
    for (auto& selector_parts : comma_separated_lists) {
        auto stream = TokenStream(selector_parts);
        auto selector = parse_complex_selector(stream, true);
        if (selector)
            selectors.append(selector.release_nonnull());
        else
            return {};
    }

    if (selectors.is_empty())
        return {};

    return selectors;
}

RefPtr<Selector> Parser::parse_complex_selector(TokenStream<StyleComponentValueRule>& tokens, bool allow_starting_combinator)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_complex_selector");

    Vector<Selector::CompoundSelector> compound_selectors;

    auto first_selector = parse_compound_selector(tokens);
    if (first_selector.is_error())
        return {};
    if (!allow_starting_combinator) {
        if (first_selector.value().combinator != Selector::Combinator::Descendant)
            return {};
        first_selector.value().combinator = Selector::Combinator::None;
    }
    compound_selectors.append(first_selector.value());

    while (tokens.has_next_token()) {
        auto compound_selector = parse_compound_selector(tokens);
        if (compound_selector.is_error()) {
            if (compound_selector.error() == SelectorParsingResult::Done)
                break;
            else
                return {};
        }
        compound_selectors.append(compound_selector.value());
    }

    if (compound_selectors.is_empty())
        return {};

    return Selector::create(move(compound_selectors));
}

Result<Selector::CompoundSelector, Parser::SelectorParsingResult> Parser::parse_compound_selector(TokenStream<StyleComponentValueRule>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_compound_selector");

    tokens.skip_whitespace();

    auto combinator = parse_selector_combinator(tokens).value_or(Selector::Combinator::Descendant);

    tokens.skip_whitespace();

    Vector<Selector::SimpleSelector> simple_selectors;

    while (tokens.has_next_token()) {
        auto component = parse_simple_selector(tokens);
        if (component.is_error()) {
            if (component.error() == SelectorParsingResult::Done)
                break;
            else
                return component.error();
        }

        simple_selectors.append(component.value());
    }

    if (simple_selectors.is_empty())
        return SelectorParsingResult::Done;

    return Selector::CompoundSelector { combinator, move(simple_selectors) };
}

Optional<Selector::Combinator> Parser::parse_selector_combinator(TokenStream<StyleComponentValueRule>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_selector_combinator");

    auto& current_value = tokens.next_token();
    if (current_value.is(Token::Type::Delim)) {
        auto delim = current_value.token().delim();
        if (delim == ">"sv) {
            return Selector::Combinator::ImmediateChild;
        } else if (delim == "+"sv) {
            return Selector::Combinator::NextSibling;
        } else if (delim == "~"sv) {
            return Selector::Combinator::SubsequentSibling;
        } else if (delim == "|"sv) {
            auto& next = tokens.peek_token();
            if (next.is(Token::Type::EndOfFile))
                return {};

            if (next.is(Token::Type::Delim) && next.token().delim() == "|"sv) {
                tokens.next_token();
                return Selector::Combinator::Column;
            }
        }
    }

    tokens.reconsume_current_input_token();
    return {};
}

Result<Selector::SimpleSelector, Parser::SelectorParsingResult> Parser::parse_simple_selector(TokenStream<StyleComponentValueRule>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_simple_selector");

    auto peek_token_ends_selector = [&]() -> bool {
        auto& value = tokens.peek_token();
        return (value.is(Token::Type::EndOfFile) || value.is(Token::Type::Whitespace) || value.is(Token::Type::Comma));
    };

    if (peek_token_ends_selector())
        return SelectorParsingResult::Done;

    auto& first_value = tokens.next_token();

    if (first_value.is(Token::Type::Delim) && first_value.token().delim() == "*"sv) {
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::Universal
        };

    } else if (first_value.is(Token::Type::Hash)) {
        if (first_value.token().hash_type() != Token::HashType::Id) {
            dbgln_if(CSS_PARSER_DEBUG, "Selector contains hash token that is not an id: {}", first_value.to_debug_string());
            return SelectorParsingResult::SyntaxError;
        }
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::Id,
            .value = first_value.token().hash_value()
        };

    } else if (first_value.is(Token::Type::Delim) && first_value.token().delim() == "."sv) {
        if (peek_token_ends_selector())
            return SelectorParsingResult::SyntaxError;

        auto& class_name_value = tokens.next_token();
        if (!class_name_value.is(Token::Type::Ident)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected an ident after '.', got: {}", class_name_value.to_debug_string());
            return SelectorParsingResult::SyntaxError;
        }
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::Class,
            .value = class_name_value.token().ident()
        };

    } else if (first_value.is(Token::Type::Ident)) {
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::TagName,
            .value = first_value.token().ident()
        };

    } else if (first_value.is_block() && first_value.block().is_square()) {
        auto& attribute_parts = first_value.block().values();

        if (attribute_parts.is_empty()) {
            dbgln_if(CSS_PARSER_DEBUG, "CSS attribute selector is empty!");
            return SelectorParsingResult::SyntaxError;
        }

        // FIXME: Handle namespace prefix for attribute name.
        auto& attribute_part = attribute_parts.first();
        if (!attribute_part.is(Token::Type::Ident)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected ident for attribute name, got: '{}'", attribute_part.to_debug_string());
            return SelectorParsingResult::SyntaxError;
        }

        Selector::SimpleSelector simple_selector {
            .type = Selector::SimpleSelector::Type::Attribute,
            .attribute = {
                .match_type = Selector::SimpleSelector::Attribute::MatchType::HasAttribute,
                // FIXME: Case-sensitivity is defined by the document language.
                // HTML is insensitive with attribute names, and our code generally assumes
                // they are converted to lowercase, so we do that here too. If we want to be
                // correct with XML later, we'll need to keep the original case and then do
                // a case-insensitive compare later.
                .name = attribute_part.token().ident().to_lowercase_string(),
            }
        };

        if (attribute_parts.size() == 1)
            return simple_selector;

        size_t attribute_index = 1;
        auto& delim_part = attribute_parts.at(attribute_index);
        if (!delim_part.is(Token::Type::Delim)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected a delim for attribute comparison, got: '{}'", delim_part.to_debug_string());
            return SelectorParsingResult::SyntaxError;
        }

        if (delim_part.token().delim() == "="sv) {
            simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch;
            attribute_index++;
        } else {
            attribute_index++;
            if (attribute_index >= attribute_parts.size()) {
                dbgln_if(CSS_PARSER_DEBUG, "Attribute selector ended part way through a match type.");
                return SelectorParsingResult::SyntaxError;
            }

            auto& delim_second_part = attribute_parts.at(attribute_index);
            if (!(delim_second_part.is(Token::Type::Delim) && delim_second_part.token().delim() == "=")) {
                dbgln_if(CSS_PARSER_DEBUG, "Expected a double delim for attribute comparison, got: '{}{}'", delim_part.to_debug_string(), delim_second_part.to_debug_string());
                return SelectorParsingResult::SyntaxError;
            }

            if (delim_part.token().delim() == "~"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::ContainsWord;
                attribute_index++;
            } else if (delim_part.token().delim() == "*"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::ContainsString;
                attribute_index++;
            } else if (delim_part.token().delim() == "|"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment;
                attribute_index++;
            } else if (delim_part.token().delim() == "^"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::StartsWithString;
                attribute_index++;
            } else if (delim_part.token().delim() == "$"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::EndsWithString;
                attribute_index++;
            }
        }

        if (attribute_index >= attribute_parts.size()) {
            dbgln_if(CSS_PARSER_DEBUG, "Attribute selector ended without a value to match.");
            return SelectorParsingResult::SyntaxError;
        }

        auto& value_part = attribute_parts.at(attribute_index);
        if (!value_part.is(Token::Type::Ident) && !value_part.is(Token::Type::String)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected a string or ident for the value to match attribute against, got: '{}'", value_part.to_debug_string());
            return SelectorParsingResult::SyntaxError;
        }
        simple_selector.attribute.value = value_part.token().is(Token::Type::Ident) ? value_part.token().ident() : value_part.token().string();

        // FIXME: Handle case-sensitivity suffixes. https://www.w3.org/TR/selectors-4/#attribute-case
        return simple_selector;

    } else if (first_value.is(Token::Type::Colon)) {
        if (peek_token_ends_selector())
            return SelectorParsingResult::SyntaxError;

        bool is_pseudo = false;
        if (tokens.peek_token().is(Token::Type::Colon)) {
            is_pseudo = true;
            tokens.next_token();
            if (peek_token_ends_selector())
                return SelectorParsingResult::SyntaxError;
        }

        if (is_pseudo) {
            Selector::SimpleSelector simple_selector {
                .type = Selector::SimpleSelector::Type::PseudoElement
            };

            auto& name_token = tokens.next_token();
            if (!name_token.is(Token::Type::Ident)) {
                dbgln_if(CSS_PARSER_DEBUG, "Expected an ident for pseudo-element, got: '{}'", name_token.to_debug_string());
                return SelectorParsingResult::SyntaxError;
            }

            auto pseudo_name = name_token.token().ident();

            if (pseudo_name.equals_ignoring_case("after")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::After;
            } else if (pseudo_name.equals_ignoring_case("before")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::Before;
            } else if (pseudo_name.equals_ignoring_case("first-letter")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::FirstLetter;
            } else if (pseudo_name.equals_ignoring_case("first-line")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::FirstLine;
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-element: '{}'", pseudo_name);
                return SelectorParsingResult::SyntaxError;
            }

            return simple_selector;
        }

        if (peek_token_ends_selector())
            return SelectorParsingResult::SyntaxError;

        auto& pseudo_class_token = tokens.next_token();
        Selector::SimpleSelector simple_selector {
            .type = Selector::SimpleSelector::Type::PseudoClass
        };

        if (pseudo_class_token.is(Token::Type::Ident)) {
            auto pseudo_name = pseudo_class_token.token().ident();
            if (pseudo_name.equals_ignoring_case("active")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Active;
            } else if (pseudo_name.equals_ignoring_case("checked")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Checked;
            } else if (pseudo_name.equals_ignoring_case("disabled")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Disabled;
            } else if (pseudo_name.equals_ignoring_case("empty")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Empty;
            } else if (pseudo_name.equals_ignoring_case("enabled")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Enabled;
            } else if (pseudo_name.equals_ignoring_case("first-child")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::FirstChild;
            } else if (pseudo_name.equals_ignoring_case("first-of-type")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::FirstOfType;
            } else if (pseudo_name.equals_ignoring_case("focus")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Focus;
            } else if (pseudo_name.equals_ignoring_case("hover")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Hover;
            } else if (pseudo_name.equals_ignoring_case("last-child")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::LastChild;
            } else if (pseudo_name.equals_ignoring_case("last-of-type")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::LastOfType;
            } else if (pseudo_name.equals_ignoring_case("link")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Link;
            } else if (pseudo_name.equals_ignoring_case("only-child")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::OnlyChild;
            } else if (pseudo_name.equals_ignoring_case("root")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Root;
            } else if (pseudo_name.equals_ignoring_case("visited")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Visited;

            } else if (pseudo_name.equals_ignoring_case("after")) {
                // Single-colon syntax allowed for compatibility. https://www.w3.org/TR/selectors/#pseudo-element-syntax
                simple_selector.type = Selector::SimpleSelector::Type::PseudoElement;
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::After;
            } else if (pseudo_name.equals_ignoring_case("before")) {
                // See :after
                simple_selector.type = Selector::SimpleSelector::Type::PseudoElement;
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::Before;
            } else if (pseudo_name.equals_ignoring_case("first-letter")) {
                // See :after
                simple_selector.type = Selector::SimpleSelector::Type::PseudoElement;
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::FirstLetter;
            } else if (pseudo_name.equals_ignoring_case("first-line")) {
                // See :after
                simple_selector.type = Selector::SimpleSelector::Type::PseudoElement;
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::FirstLine;
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "Unknown pseudo class: '{}'", pseudo_name);
                return SelectorParsingResult::SyntaxError;
            }

            return simple_selector;

        } else if (pseudo_class_token.is_function()) {

            auto& pseudo_function = pseudo_class_token.function();
            if (pseudo_function.name().equals_ignoring_case("not")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Not;
                auto function_token_stream = TokenStream(pseudo_function.values());
                auto not_selector = parse_a_selector(function_token_stream);
                if (!not_selector.has_value()) {
                    dbgln_if(CSS_PARSER_DEBUG, "Invalid selector in :not() clause");
                    return SelectorParsingResult::SyntaxError;
                }
                simple_selector.pseudo_class.not_selector = not_selector.value();
            } else if (pseudo_function.name().equals_ignoring_case("nth-child")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::NthChild;
                auto function_values = TokenStream<StyleComponentValueRule>(pseudo_function.values());
                auto nth_child_pattern = parse_a_n_plus_b_pattern(function_values);
                if (nth_child_pattern.has_value()) {
                    simple_selector.pseudo_class.nth_child_pattern = nth_child_pattern.value();
                } else {
                    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid nth-child format");
                    return SelectorParsingResult::SyntaxError;
                }
            } else if (pseudo_function.name().equals_ignoring_case("nth-last-child")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::NthLastChild;
                auto function_values = TokenStream<StyleComponentValueRule>(pseudo_function.values());
                auto nth_child_pattern = parse_a_n_plus_b_pattern(function_values);
                if (nth_child_pattern.has_value()) {
                    simple_selector.pseudo_class.nth_child_pattern = nth_child_pattern.value();
                } else {
                    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid nth-child format");
                    return SelectorParsingResult::SyntaxError;
                }
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "Unknown pseudo class: '{}'()", pseudo_function.name());
                return SelectorParsingResult::SyntaxError;
            }

            return simple_selector;

        } else {
            dbgln_if(CSS_PARSER_DEBUG, "Unexpected Block in pseudo-class name, expected a function or identifier. '{}'", pseudo_class_token.to_debug_string());
            return SelectorParsingResult::SyntaxError;
        }
    }

    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid simple selector!");
    return SelectorParsingResult::SyntaxError;
}

NonnullRefPtrVector<StyleRule> Parser::consume_a_list_of_rules(bool top_level)
{
    return consume_a_list_of_rules(m_token_stream, top_level);
}

template<typename T>
NonnullRefPtrVector<StyleRule> Parser::consume_a_list_of_rules(TokenStream<T>& tokens, bool top_level)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_list_of_rules");

    NonnullRefPtrVector<StyleRule> rules;

    for (;;) {
        auto& token = tokens.next_token();

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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_an_at_rule");

    auto& name_ident = tokens.next_token();
    VERIFY(name_ident.is(Token::Type::AtKeyword));

    NonnullRefPtr<StyleRule> rule = create<StyleRule>(StyleRule::Type::At);
    rule->m_name = ((Token)name_ident).at_keyword();

    for (;;) {
        auto& token = tokens.next_token();
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_qualified_rule");

    NonnullRefPtr<StyleRule> rule = create<StyleRule>(StyleRule::Type::Qualified);

    for (;;) {
        auto& token = tokens.next_token();

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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_component_value - shortcut: '{}'", tokens.peek_token().to_debug_string());

    return tokens.next_token();
}

template<typename T>
StyleComponentValueRule Parser::consume_a_component_value(TokenStream<T>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_component_value");

    auto& token = tokens.next_token();

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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_simple_block");

    auto ending_token = ((Token)tokens.current_token()).mirror_variant();

    NonnullRefPtr<StyleBlockRule> block = create<StyleBlockRule>();
    block->m_token = tokens.current_token();

    for (;;) {
        auto& token = tokens.next_token();

        if (token.is(ending_token)) {
            return block;
        }

        if (token.is(Token::Type::EndOfFile)) {
            log_parse_error();
            return block;
        }

        tokens.reconsume_current_input_token();
        auto value = consume_a_component_value(tokens);
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_function");

    auto name_ident = tokens.current_token();
    VERIFY(name_ident.is(Token::Type::Function));
    NonnullRefPtr<StyleFunctionRule> function = create<StyleFunctionRule>(((Token)name_ident).m_value.to_string());

    for (;;) {
        auto& token = tokens.next_token();
        if (token.is(Token::Type::CloseParen)) {
            return function;
        }

        if (token.is(Token::Type::EndOfFile)) {
            log_parse_error();
            return function;
        }

        tokens.reconsume_current_input_token();
        auto value = consume_a_component_value(tokens);
        function->m_values.append(value);
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_declaration");

    auto& token = tokens.next_token();

    StyleDeclarationRule declaration;
    VERIFY(token.is(Token::Type::Ident));
    declaration.m_name = ((Token)token).ident();

    tokens.skip_whitespace();

    auto& maybe_colon = tokens.next_token();
    if (!maybe_colon.is(Token::Type::Colon)) {
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

    while (!declaration.m_values.is_empty()) {
        auto maybe_whitespace = declaration.m_values.last();
        if (!(maybe_whitespace.is(Token::Type::Whitespace))) {
            break;
        }
        declaration.m_values.take_last();
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::consume_a_list_of_declarations");

    Vector<DeclarationOrAtRule> list;

    for (;;) {
        auto& token = tokens.next_token();
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
                auto& peek = tokens.peek_token();
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

        for (;;) {
            auto& peek = tokens.peek_token();
            if (peek.is(Token::Type::Semicolon) || peek.is(Token::Type::EndOfFile))
                break;
            dbgln("Discarding token: '{}'", peek.to_debug_string());
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_rule");

    RefPtr<CSSRule> rule;

    tokens.skip_whitespace();

    auto& token = tokens.peek_token();

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

    auto& maybe_eof = tokens.peek_token();
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_list_of_rules");

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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_declaration");

    tokens.skip_whitespace();

    auto& token = tokens.peek_token();

    if (!token.is(Token::Type::Ident)) {
        return {};
    }

    auto declaration = consume_a_declaration(tokens);
    if (declaration.has_value())
        return convert_to_style_property(declaration.value());

    return {};
}

RefPtr<CSSStyleDeclaration> Parser::parse_as_list_of_declarations()
{
    return parse_as_list_of_declarations(m_token_stream);
}

template<typename T>
RefPtr<CSSStyleDeclaration> Parser::parse_as_list_of_declarations(TokenStream<T>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_list_of_declarations");

    auto declarations_and_at_rules = consume_a_list_of_declarations(tokens);

    Vector<StyleProperty> properties;
    HashMap<String, StyleProperty> custom_properties;

    for (auto& declaration_or_at_rule : declarations_and_at_rules) {
        if (declaration_or_at_rule.is_at_rule()) {
            dbgln("Parser::parse_as_list_of_declarations(): At-rule is not allowed here!");
            continue;
        }

        auto& declaration = declaration_or_at_rule.m_declaration;

        auto maybe_property = convert_to_style_property(declaration);
        if (maybe_property.has_value()) {
            auto property = maybe_property.value();
            if (property.property_id == PropertyID::Custom) {
                custom_properties.set(property.custom_name, property);
            } else {
                properties.append(property);
            }
        }
    }

    return CSSStyleDeclaration::create(move(properties), move(custom_properties));
}

Optional<StyleComponentValueRule> Parser::parse_as_component_value()
{
    return parse_as_component_value(m_token_stream);
}

template<typename T>
Optional<StyleComponentValueRule> Parser::parse_as_component_value(TokenStream<T>& tokens)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_component_value");

    tokens.skip_whitespace();

    auto& token = tokens.peek_token();

    if (token.is(Token::Type::EndOfFile)) {
        return {};
    }

    auto value = consume_a_component_value(tokens);

    tokens.skip_whitespace();

    auto& maybe_eof = tokens.peek_token();
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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_list_of_component_values");

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
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_as_comma_separated_list_of_component_values");

    Vector<Vector<StyleComponentValueRule>> lists;
    lists.append({});

    for (;;) {
        auto& next = tokens.next_token();

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

Optional<URL> Parser::parse_url_function(ParsingContext const& context, StyleComponentValueRule const& component_value)
{
    // FIXME: Handle list of media queries. https://www.w3.org/TR/css-cascade-3/#conditional-import

    if (component_value.is(Token::Type::Url))
        return context.complete_url(component_value.token().url());
    if (component_value.is_function() && component_value.function().name().equals_ignoring_case("url")) {
        auto& function_values = component_value.function().values();
        // FIXME: Handle url-modifiers. https://www.w3.org/TR/css-values-4/#url-modifiers
        for (size_t i = 0; i < function_values.size(); ++i) {
            auto& value = function_values[i];
            if (value.is(Token::Type::Whitespace))
                continue;
            if (value.is(Token::Type::String)) {
                // FIXME: RFC2397
                if (value.token().string().starts_with("data:"))
                    break;
                return context.complete_url(value.token().string());
            }
        }
    }

    return {};
}

RefPtr<CSSRule> Parser::convert_to_rule(NonnullRefPtr<StyleRule> rule)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::convert_to_rule");

    if (rule->m_type == StyleRule::Type::At) {
        if (rule->m_name.equals_ignoring_case("import"sv) && !rule->prelude().is_empty()) {

            Optional<URL> url;
            for (auto& token : rule->prelude()) {
                if (token.is(Token::Type::Whitespace))
                    continue;

                if (token.is(Token::Type::String)) {
                    url = m_context.complete_url(token.token().string());
                } else {
                    url = parse_url_function(m_context, token);
                }

                // FIXME: Handle list of media queries. https://www.w3.org/TR/css-cascade-3/#conditional-import
                if (url.has_value())
                    break;
            }

            if (url.has_value())
                return CSSImportRule::create(url.value());
            else
                dbgln("Unable to parse url from @import rule");
        } else {
            dbgln("Unrecognized CSS at-rule: {}", rule->m_name);
        }

        // FIXME: More at rules!

    } else {
        auto prelude_stream = TokenStream(rule->m_prelude);
        auto selectors = parse_a_selector(prelude_stream);
        if (!selectors.has_value() || selectors.value().is_empty()) {
            dbgln("CSSParser: style rule selectors invalid; discarding.");
            prelude_stream.dump_all_tokens();
            return {};
        }

        auto declaration = convert_to_declaration(*rule->m_block);
        if (!declaration) {
            dbgln("CSSParser: style rule declaration invalid; discarding.");
            return {};
        }

        return CSSStyleRule::create(move(selectors.value()), move(*declaration));
    }

    return {};
}

RefPtr<CSSStyleDeclaration> Parser::convert_to_declaration(NonnullRefPtr<StyleBlockRule> block)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::convert_to_declaration");

    if (!block->is_curly())
        return {};

    auto stream = TokenStream(block->m_values);
    return parse_as_list_of_declarations(stream);
}

Optional<StyleProperty> Parser::convert_to_style_property(StyleDeclarationRule& declaration)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::convert_to_style_property");

    auto& property_name = declaration.m_name;
    auto property_id = property_id_from_string(property_name);
    if (property_id == PropertyID::Invalid && property_name.starts_with("--"))
        property_id = PropertyID::Custom;

    if (property_id == PropertyID::Invalid && !property_name.starts_with("-")) {
        dbgln("Parser::convert_to_style_property(): Unrecognized property '{}'", property_name);
        return {};
    }

    auto value_token_stream = TokenStream(declaration.m_values);
    auto value = parse_css_value(property_id, value_token_stream);
    if (!value) {
        dbgln("Parser::convert_to_style_property(): Property '{}' has no value.", property_name);
        return {};
    }

    if (property_id == PropertyID::Custom) {
        return StyleProperty { property_id, value.release_nonnull(), declaration.m_name, declaration.m_important };
    } else {
        return StyleProperty { property_id, value.release_nonnull(), {}, declaration.m_important };
    }
}

Optional<float> Parser::try_parse_float(StringView string)
{
    // FIXME: This is copied from DeprecatedCSSParser, so may not be to spec.

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

RefPtr<StyleValue> Parser::parse_keyword_or_custom_value(ParsingContext const&, StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Ident)) {
        auto ident = component_value.token().ident();
        if (ident.equals_ignoring_case("inherit"))
            return InheritStyleValue::create();
        if (ident.equals_ignoring_case("initial"))
            return InitialStyleValue::create();
        if (ident.equals_ignoring_case("auto"))
            return LengthStyleValue::create(Length::make_auto());
        // FIXME: Implement `unset` keyword
    }

    if (component_value.is_function() && component_value.function().name().equals_ignoring_case("var")) {
        // FIXME: Handle fallback value as second parameter
        // https://www.w3.org/TR/css-variables-1/#using-variables
        if (!component_value.function().values().is_empty()) {
            auto& property_name_token = component_value.function().values().first();
            if (property_name_token.is(Token::Type::Ident))
                return CustomStyleValue::create(property_name_token.token().ident());
            else
                dbgln("First argument to var() function was not an ident: '{}'", property_name_token.to_debug_string());
        }
    }

    return {};
}

Optional<Length> Parser::parse_length(ParsingContext const& context, StyleComponentValueRule const& component_value)
{
    Length::Type type = Length::Type::Undefined;
    Optional<float> numeric_value;

    if (component_value.is(Token::Type::Dimension)) {
        auto length_string = component_value.token().m_value.string_view();
        auto unit_string = component_value.token().m_unit.string_view();

        if (unit_string.equals_ignoring_case("%")) {
            type = Length::Type::Percentage;
        } else if (unit_string.equals_ignoring_case("px")) {
            type = Length::Type::Px;
        } else if (unit_string.equals_ignoring_case("pt")) {
            type = Length::Type::Pt;
        } else if (unit_string.equals_ignoring_case("pc")) {
            type = Length::Type::Pc;
        } else if (unit_string.equals_ignoring_case("mm")) {
            type = Length::Type::Mm;
        } else if (unit_string.equals_ignoring_case("rem")) {
            type = Length::Type::Rem;
        } else if (unit_string.equals_ignoring_case("em")) {
            type = Length::Type::Em;
        } else if (unit_string.equals_ignoring_case("ex")) {
            type = Length::Type::Ex;
        } else if (unit_string.equals_ignoring_case("vw")) {
            type = Length::Type::Vw;
        } else if (unit_string.equals_ignoring_case("vh")) {
            type = Length::Type::Vh;
        } else if (unit_string.equals_ignoring_case("vmax")) {
            type = Length::Type::Vmax;
        } else if (unit_string.equals_ignoring_case("vmin")) {
            type = Length::Type::Vmin;
        } else if (unit_string.equals_ignoring_case("cm")) {
            type = Length::Type::Cm;
        } else if (unit_string.equals_ignoring_case("in")) {
            type = Length::Type::In;
        } else if (unit_string.equals_ignoring_case("Q")) {
            type = Length::Type::Q;
        } else if (context.in_quirks_mode()) {
            type = Length::Type::Px;
        }

        numeric_value = try_parse_float(length_string);
    } else if (component_value.is(Token::Type::Number)) {
        auto value_string = component_value.token().m_value.string_view();
        if (value_string == "0") {
            type = Length::Type::Px;
            numeric_value = 0;
        } else if (context.in_quirks_mode()) {
            type = Length::Type::Px;
            numeric_value = try_parse_float(value_string);
        }
    } else if (component_value.is(Token::Type::Percentage)) {
        type = Length::Type::Percentage;
        auto value_string = component_value.token().m_value.string_view();
        numeric_value = try_parse_float(value_string);
    }

    if (!numeric_value.has_value())
        return {};

    return Length(numeric_value.value(), type);
}

RefPtr<StyleValue> Parser::parse_length_value(ParsingContext const& context, StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Dimension) || component_value.is(Token::Type::Number) || component_value.is(Token::Type::Percentage)) {
        auto length = parse_length(context, component_value);
        if (length.has_value())
            return LengthStyleValue::create(length.value());
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_numeric_value(ParsingContext const&, StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Number)) {
        auto number = component_value.token();
        if (number.m_number_type == Token::NumberType::Integer) {
            return NumericStyleValue::create(number.to_integer());
        } else {
            auto float_value = try_parse_float(number.m_value.string_view());
            if (float_value.has_value())
                return NumericStyleValue::create(float_value.value());
        }
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_identifier_value(ParsingContext const&, StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Ident)) {
        auto value_id = value_id_from_string(component_value.token().ident());
        if (value_id != ValueID::Invalid)
            return IdentifierStyleValue::create(value_id);
    }

    return {};
}

Optional<Color> Parser::parse_color(ParsingContext const&, StyleComponentValueRule const& component_value)
{
    // https://www.w3.org/TR/css-color-3/
    if (component_value.is(Token::Type::Ident)) {
        auto ident = component_value.token().ident();
        if (ident.equals_ignoring_case("transparent"))
            return Color::from_rgba(0x00000000);

        auto color = Color::from_string(ident.to_string().to_lowercase());
        if (color.has_value())
            return color;

    } else if (component_value.is(Token::Type::Hash)) {
        // FIXME: Read it directly
        auto color = Color::from_string(String::formatted("#{}", component_value.token().m_value.to_string().to_lowercase()));
        if (color.has_value())
            return color;

    } else if (component_value.is_function()) {
        auto& function = component_value.function();
        auto& values = function.values();

        Vector<Token> params;
        for (size_t i = 0; i < values.size(); ++i) {
            auto& value = values.at(i);
            if (value.is(Token::Type::Whitespace))
                continue;

            if (value.is(Token::Type::Percentage) || value.is(Token::Type::Number)) {
                params.append(value.token());
                // Eat following comma and whitespace
                while ((i + 1) < values.size()) {
                    auto& next = values.at(i + 1);
                    if (next.is(Token::Type::Whitespace))
                        i++;
                    else if (next.is(Token::Type::Comma))
                        break;

                    return {};
                }
            }
        }

        if (function.name().equals_ignoring_case("rgb")) {
            if (params.size() != 3)
                return {};

            auto r_val = params[0];
            auto g_val = params[1];
            auto b_val = params[2];

            if (r_val.is(Token::NumberType::Integer)
                && g_val.is(Token::NumberType::Integer)
                && b_val.is(Token::NumberType::Integer)) {

                auto maybe_r = r_val.m_value.string_view().to_uint<u8>();
                auto maybe_g = g_val.m_value.string_view().to_uint<u8>();
                auto maybe_b = b_val.m_value.string_view().to_uint<u8>();
                if (maybe_r.has_value() && maybe_g.has_value() && maybe_b.has_value())
                    return Color(maybe_r.value(), maybe_g.value(), maybe_b.value());

            } else if (r_val.is(Token::Type::Percentage)
                && g_val.is(Token::Type::Percentage)
                && b_val.is(Token::Type::Percentage)) {

                auto maybe_r = try_parse_float(r_val.m_value.string_view());
                auto maybe_g = try_parse_float(g_val.m_value.string_view());
                auto maybe_b = try_parse_float(b_val.m_value.string_view());
                if (maybe_r.has_value() && maybe_g.has_value() && maybe_b.has_value()) {
                    u8 r = clamp(lroundf(maybe_r.value() * 2.55f), 0, 255);
                    u8 g = clamp(lroundf(maybe_g.value() * 2.55f), 0, 255);
                    u8 b = clamp(lroundf(maybe_b.value() * 2.55f), 0, 255);
                    return Color(r, g, b);
                }
            }
        } else if (function.name().equals_ignoring_case("rgba")) {
            if (params.size() != 4)
                return {};

            auto r_val = params[0];
            auto g_val = params[1];
            auto b_val = params[2];
            auto a_val = params[3];

            if (r_val.is(Token::NumberType::Integer)
                && g_val.is(Token::NumberType::Integer)
                && b_val.is(Token::NumberType::Integer)
                && a_val.is(Token::Type::Number)) {

                auto maybe_r = r_val.m_value.string_view().to_uint<u8>();
                auto maybe_g = g_val.m_value.string_view().to_uint<u8>();
                auto maybe_b = b_val.m_value.string_view().to_uint<u8>();
                auto maybe_a = try_parse_float(a_val.m_value.string_view());
                if (maybe_r.has_value() && maybe_g.has_value() && maybe_b.has_value() && maybe_a.has_value()) {
                    u8 a = clamp(lroundf(maybe_a.value() * 255.0f), 0, 255);
                    return Color(maybe_r.value(), maybe_g.value(), maybe_b.value(), a);
                }

            } else if (r_val.is(Token::Type::Percentage)
                && g_val.is(Token::Type::Percentage)
                && b_val.is(Token::Type::Percentage)
                && a_val.is(Token::Type::Number)) {

                auto maybe_r = try_parse_float(r_val.m_value.string_view());
                auto maybe_g = try_parse_float(g_val.m_value.string_view());
                auto maybe_b = try_parse_float(b_val.m_value.string_view());
                auto maybe_a = try_parse_float(a_val.m_value.string_view());
                if (maybe_r.has_value() && maybe_g.has_value() && maybe_b.has_value() && maybe_a.has_value()) {
                    u8 r = clamp(lroundf(maybe_r.value() * 2.55f), 0, 255);
                    u8 g = clamp(lroundf(maybe_g.value() * 2.55f), 0, 255);
                    u8 b = clamp(lroundf(maybe_b.value() * 2.55f), 0, 255);
                    u8 a = clamp(lroundf(maybe_a.value() * 255.0f), 0, 255);
                    return Color(r, g, b, a);
                }
            }
        } else if (function.name().equals_ignoring_case("hsl")) {
            if (params.size() != 3)
                return {};

            auto h_val = params[0];
            auto s_val = params[1];
            auto l_val = params[2];

            if (h_val.is(Token::Type::Number)
                && s_val.is(Token::Type::Percentage)
                && l_val.is(Token::Type::Percentage)) {

                auto maybe_h = try_parse_float(h_val.m_value.string_view());
                auto maybe_s = try_parse_float(s_val.m_value.string_view());
                auto maybe_l = try_parse_float(l_val.m_value.string_view());
                if (maybe_h.has_value() && maybe_s.has_value() && maybe_l.has_value()) {
                    float h = maybe_h.value();
                    float s = maybe_s.value() / 100.0f;
                    float l = maybe_l.value() / 100.0f;
                    return Color::from_hsl(h, s, l);
                }
            }
        } else if (function.name().equals_ignoring_case("hsla")) {
            if (params.size() != 4)
                return {};

            auto h_val = params[0];
            auto s_val = params[1];
            auto l_val = params[2];
            auto a_val = params[3];

            if (h_val.is(Token::Type::Number)
                && s_val.is(Token::Type::Percentage)
                && l_val.is(Token::Type::Percentage)
                && a_val.is(Token::Type::Number)) {

                auto maybe_h = try_parse_float(h_val.m_value.string_view());
                auto maybe_s = try_parse_float(s_val.m_value.string_view());
                auto maybe_l = try_parse_float(l_val.m_value.string_view());
                auto maybe_a = try_parse_float(a_val.m_value.string_view());
                if (maybe_h.has_value() && maybe_s.has_value() && maybe_l.has_value() && maybe_a.has_value()) {
                    float h = maybe_h.value();
                    float s = maybe_s.value() / 100.0f;
                    float l = maybe_l.value() / 100.0f;
                    float a = maybe_a.value();
                    return Color::from_hsla(h, s, l, a);
                }
            }
        }
        return {};
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_color_value(ParsingContext const& context, StyleComponentValueRule const& component_value)
{
    auto color = parse_color(context, component_value);
    if (color.has_value())
        return ColorStyleValue::create(color.value());

    return {};
}

RefPtr<StyleValue> Parser::parse_string_value(ParsingContext const&, StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::String))
        return StringStyleValue::create(component_value.token().string());

    return {};
}

RefPtr<StyleValue> Parser::parse_image_value(ParsingContext const& context, StyleComponentValueRule const& component_value)
{
    auto url = parse_url_function(context, component_value);
    if (url.has_value())
        return ImageStyleValue::create(url.value(), *context.document());
    // FIXME: Handle gradients.

    return {};
}

RefPtr<StyleValue> Parser::parse_box_shadow_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    // FIXME: Also support inset, spread-radius and multiple comma-seperated box-shadows
    Length offset_x {};
    Length offset_y {};
    Length blur_radius {};
    Color color {};

    if (component_values.size() < 3 || component_values.size() > 4)
        return nullptr;

    auto maybe_x = parse_length(context, component_values[0]);
    if (!maybe_x.has_value())
        return nullptr;
    offset_x = maybe_x.value();

    auto maybe_y = parse_length(context, component_values[1]);
    if (!maybe_y.has_value())
        return nullptr;
    offset_y = maybe_y.value();

    if (component_values.size() == 3) {
        auto parsed_color = parse_color(context, component_values[2]);
        if (!parsed_color.has_value())
            return nullptr;
        color = parsed_color.value();
    } else if (component_values.size() == 4) {
        auto maybe_blur_radius = parse_length(context, component_values[2]);
        if (!maybe_blur_radius.has_value())
            return nullptr;
        blur_radius = maybe_blur_radius.value();

        auto parsed_color = parse_color(context, component_values[3]);
        if (!parsed_color.has_value())
            return nullptr;
        color = parsed_color.value();
    }

    return BoxShadowStyleValue::create(offset_x, offset_y, blur_radius, color);
}

RefPtr<StyleValue> Parser::parse_css_value(PropertyID property_id, TokenStream<StyleComponentValueRule>& tokens)
{
    Vector<StyleComponentValueRule> component_values;

    while (tokens.has_next_token()) {
        auto& token = tokens.next_token();

        if (token.is(Token::Type::Semicolon)) {
            tokens.reconsume_current_input_token();
            break;
        }

        if (token.is(Token::Type::Whitespace))
            continue;

        component_values.append(token);
    }

    if (component_values.is_empty())
        return {};

    // Special-case property handling
    if (property_id == PropertyID::BoxShadow) {
        if (auto parsed_box_shadow = parse_box_shadow_value(m_context, component_values))
            return parsed_box_shadow;
    }

    if (component_values.size() == 1)
        return parse_css_value(m_context, property_id, component_values.first());

    return ValueListStyleValue::create(move(component_values));
}

RefPtr<StyleValue> Parser::parse_css_value(ParsingContext const& context, PropertyID property_id, StyleComponentValueRule const& component_value)
{
    // FIXME: Figure out if we still need takes_integer_value, and if so, move this information
    // into Properties.json.
    auto takes_integer_value = [](PropertyID property_id) -> bool {
        return property_id == PropertyID::ZIndex
            || property_id == PropertyID::FontWeight
            || property_id == PropertyID::Custom;
    };
    if (takes_integer_value(property_id) && component_value.is(Token::Type::Number)) {
        auto number = component_value.token();
        if (number.m_number_type == Token::NumberType::Integer) {
            return LengthStyleValue::create(Length::make_px(number.to_integer()));
        }
    }

    if (auto keyword_or_custom = parse_keyword_or_custom_value(context, component_value))
        return keyword_or_custom;

    if (auto length = parse_length_value(context, component_value))
        return length;

    if (auto numeric = parse_numeric_value(context, component_value))
        return numeric;

    if (auto identifier = parse_identifier_value(context, component_value))
        return identifier;

    if (auto color = parse_color_value(context, component_value))
        return color;

    if (auto string = parse_string_value(context, component_value))
        return string;

    if (auto image = parse_image_value(context, component_value))
        return image;

    return {};
}

Optional<Selector::SimpleSelector::ANPlusBPattern> Parser::parse_a_n_plus_b_pattern(TokenStream<StyleComponentValueRule>& values)
{
    dbgln_if(CSS_PARSER_DEBUG, "Parser::parse_a_n_plus_b_pattern");

    int a = 0;
    int b = 0;

    auto syntax_error = [&]() -> Optional<Selector::SimpleSelector::ANPlusBPattern> {
        if constexpr (CSS_PARSER_DEBUG) {
            dbgln("Invalid An+B value:");
            values.dump_all_tokens();
        }
        return {};
    };

    auto make_return_value = [&]() -> Optional<Selector::SimpleSelector::ANPlusBPattern> {
        // When we think we are done, but there are more non-whitespace tokens, then it's a parse error.
        values.skip_whitespace();
        if (values.has_next_token()) {
            if constexpr (CSS_PARSER_DEBUG) {
                dbgln("Extra tokens at end of An+B value:");
                values.dump_all_tokens();
            }
            return syntax_error();
        } else {
            return Selector::SimpleSelector::ANPlusBPattern { a, b };
        }
    };

    auto is_n = [](StyleComponentValueRule const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("n"sv);
    };
    auto is_ndash = [](StyleComponentValueRule const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("n-"sv);
    };
    auto is_dashn = [](StyleComponentValueRule const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("-n"sv);
    };
    auto is_dashndash = [](StyleComponentValueRule const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("-n-"sv);
    };
    auto is_delim = [](StyleComponentValueRule const& value, StringView const& delim) -> bool {
        return value.is(Token::Type::Delim) && value.token().delim().equals_ignoring_case(delim);
    };
    auto is_n_dimension = [](StyleComponentValueRule const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (value.token().number_type() != Token::NumberType::Integer)
            return false;
        if (!value.token().dimension_unit().equals_ignoring_case("n"sv))
            return false;
        return true;
    };
    auto is_ndash_dimension = [](StyleComponentValueRule const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (value.token().number_type() != Token::NumberType::Integer)
            return false;
        if (!value.token().dimension_unit().equals_ignoring_case("n-"sv))
            return false;
        return true;
    };
    auto is_ndashdigit_dimension = [](StyleComponentValueRule const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (value.token().number_type() != Token::NumberType::Integer)
            return false;
        auto dimension_unit = value.token().dimension_unit();
        if (!dimension_unit.starts_with("n-"sv, CaseSensitivity::CaseInsensitive))
            return false;
        for (size_t i = 2; i < dimension_unit.length(); ++i) {
            if (!is_ascii_digit(dimension_unit[i]))
                return false;
        }
        return true;
    };
    auto is_ndashdigit_ident = [](StyleComponentValueRule const& value) -> bool {
        if (!value.is(Token::Type::Ident))
            return false;
        auto ident = value.token().ident();
        if (!ident.starts_with("n-"sv, CaseSensitivity::CaseInsensitive))
            return false;
        for (size_t i = 2; i < ident.length(); ++i) {
            if (!is_ascii_digit(ident[i]))
                return false;
        }
        return true;
    };
    auto is_dashndashdigit_ident = [](StyleComponentValueRule const& value) -> bool {
        if (!value.is(Token::Type::Ident))
            return false;
        auto ident = value.token().ident();
        if (!ident.starts_with("-n-"sv, CaseSensitivity::CaseInsensitive))
            return false;
        for (size_t i = 3; i < ident.length(); ++i) {
            if (!is_ascii_digit(ident[i]))
                return false;
        }
        return true;
    };
    auto is_integer = [](StyleComponentValueRule const& value) -> bool {
        return value.is(Token::Type::Number) && value.token().is(Token::NumberType::Integer);
    };
    auto is_signed_integer = [is_integer](StyleComponentValueRule const& value) -> bool {
        return is_integer(value) && value.token().is_integer_value_signed();
    };
    auto is_signless_integer = [is_integer](StyleComponentValueRule const& value) -> bool {
        return is_integer(value) && !value.token().is_integer_value_signed();
    };

    // https://www.w3.org/TR/css-syntax-3/#the-anb-type
    // Unfortunately these can't be in the same order as in the spec.

    values.skip_whitespace();
    auto& first_value = values.next_token();

    // odd | even
    if (first_value.is(Token::Type::Ident)) {
        auto ident = first_value.token().ident();
        if (ident.equals_ignoring_case("odd")) {
            a = 2;
            b = 1;
            return make_return_value();
        } else if (ident.equals_ignoring_case("even")) {
            a = 2;
            return make_return_value();
        }
    }
    // <integer>
    if (is_integer(first_value)) {
        b = first_value.token().to_integer();
        return make_return_value();
    }
    // <n-dimension>
    // <n-dimension> <signed-integer>
    // <n-dimension> ['+' | '-'] <signless-integer>
    if (is_n_dimension(first_value)) {
        a = first_value.token().dimension_value_int();

        values.skip_whitespace();
        auto& second_value = values.next_token();
        if (second_value.is(Token::Type::EndOfFile)) {
            // <n-dimension>
            return make_return_value();
        } else if (is_signed_integer(second_value)) {
            // <n-dimension> <signed-integer>
            b = second_value.token().to_integer();
            return make_return_value();
        }

        values.skip_whitespace();
        auto& third_value = values.next_token();
        if ((is_delim(second_value, "+"sv) || is_delim(second_value, "-"sv)) && is_signless_integer(third_value)) {
            // <n-dimension> ['+' | '-'] <signless-integer>
            b = third_value.token().to_integer() * (is_delim(second_value, "+"sv) ? 1 : -1);
            return make_return_value();
        }

        return syntax_error();
    }
    // <ndash-dimension> <signless-integer>
    if (is_ndash_dimension(first_value)) {
        values.skip_whitespace();
        auto& second_value = values.next_token();
        if (is_signless_integer(second_value)) {
            a = first_value.token().dimension_value_int();
            b = -second_value.token().to_integer();
            return make_return_value();
        }

        return syntax_error();
    }
    // <ndashdigit-dimension>
    if (is_ndashdigit_dimension(first_value)) {
        auto& dimension = first_value.token();
        a = dimension.dimension_value_int();
        auto maybe_b = dimension.dimension_unit().substring_view(1).to_int();
        if (maybe_b.has_value()) {
            b = maybe_b.value();
            return make_return_value();
        }

        return syntax_error();
    }
    // <dashndashdigit-ident>
    if (is_dashndashdigit_ident(first_value)) {
        a = -1;
        auto maybe_b = first_value.token().ident().substring_view(2).to_int();
        if (maybe_b.has_value()) {
            b = maybe_b.value();
            return make_return_value();
        }

        return syntax_error();
    }
    // -n
    // -n <signed-integer>
    // -n ['+' | '-'] <signless-integer>
    if (is_dashn(first_value)) {
        a = -1;
        values.skip_whitespace();
        auto& second_value = values.next_token();
        if (second_value.is(Token::Type::EndOfFile)) {
            // -n
            return make_return_value();
        } else if (is_signed_integer(second_value)) {
            // -n <signed-integer>
            b = second_value.token().to_integer();
            return make_return_value();
        }

        values.skip_whitespace();
        auto& third_value = values.next_token();
        if ((is_delim(second_value, "+"sv) || is_delim(second_value, "-"sv)) && is_signless_integer(third_value)) {
            // -n ['+' | '-'] <signless-integer>
            b = third_value.token().to_integer() * (is_delim(second_value, "+"sv) ? 1 : -1);
            return make_return_value();
        }

        return syntax_error();
    }
    // -n- <signless-integer>
    if (is_dashndash(first_value)) {
        values.skip_whitespace();
        auto& second_value = values.next_token();
        if (is_signless_integer(second_value)) {
            a = -1;
            b = -second_value.token().to_integer();
            return make_return_value();
        }

        return syntax_error();
    }

    // All that's left now are these:
    // '+'?† n
    // '+'?† n <signed-integer>
    // '+'?† n ['+' | '-'] <signless-integer>
    // '+'?† n- <signless-integer>
    // '+'?† <ndashdigit-ident>
    // In all of these cases, the + is optional, and has no effect.
    // So, we just skip the +, and carry on.
    if (!is_delim(first_value, "+"sv)) {
        values.reconsume_current_input_token();
        // We do *not* skip whitespace here.
    }

    auto& first_after_plus = values.next_token();
    // '+'?† n
    // '+'?† n <signed-integer>
    // '+'?† n ['+' | '-'] <signless-integer>
    if (is_n(first_after_plus)) {
        a = 1;
        values.skip_whitespace();
        auto& second_value = values.next_token();
        if (second_value.is(Token::Type::EndOfFile)) {
            // '+'?† n
            return make_return_value();
        } else if (is_signed_integer(second_value)) {
            // '+'?† n <signed-integer>
            b = second_value.token().to_integer();
            return make_return_value();
        }

        values.skip_whitespace();
        auto& third_value = values.next_token();
        if ((is_delim(second_value, "+"sv) || is_delim(second_value, "-"sv)) && is_signless_integer(third_value)) {
            // '+'?† n ['+' | '-'] <signless-integer>
            b = third_value.token().to_integer() * (is_delim(second_value, "+"sv) ? 1 : -1);
            return make_return_value();
        }

        return syntax_error();
    }

    // '+'?† n- <signless-integer>
    if (is_ndash(first_after_plus)) {
        values.skip_whitespace();
        auto& second_value = values.next_token();
        if (is_signless_integer(second_value)) {
            a = 1;
            b = -second_value.token().to_integer();
            return make_return_value();
        }

        return syntax_error();
    }

    // '+'?† <ndashdigit-ident>
    if (is_ndashdigit_ident(first_after_plus)) {
        a = 1;
        auto maybe_b = first_after_plus.token().ident().substring_view(1).to_int();
        if (maybe_b.has_value()) {
            b = maybe_b.value();
            return make_return_value();
        }

        return syntax_error();
    }

    return syntax_error();
}
}
