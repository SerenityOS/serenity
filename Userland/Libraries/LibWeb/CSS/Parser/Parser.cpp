/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
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
T const& TokenStream<T>::peek_token(int offset)
{
    if (!has_next_token())
        return m_eof;

    return m_tokens.at(m_iterator_offset + offset + 1);
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
    return parse_a_stylesheet(m_token_stream);
}

template<typename T>
NonnullRefPtr<CSSStyleSheet> Parser::parse_a_stylesheet(TokenStream<T>& tokens)
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
    if constexpr (CSS_PARSER_DEBUG) {
        dump_sheet(stylesheet);
    }
    return stylesheet;
}

Optional<SelectorList> Parser::parse_as_selector()
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

Optional<SelectorList> Parser::parse_as_relative_selector()
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

    auto comma_separated_lists = parse_a_comma_separated_list_of_component_values(tokens);

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

    auto comma_separated_lists = parse_a_comma_separated_list_of_component_values(tokens);

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

    auto rule = make_ref_counted<StyleRule>(StyleRule::Type::At);
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

    auto rule = make_ref_counted<StyleRule>(StyleRule::Type::Qualified);

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

    auto block = make_ref_counted<StyleBlockRule>();
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
    auto function = make_ref_counted<StyleFunctionRule>(((Token)name_ident).m_value.to_string());

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
    return parse_a_rule(m_token_stream);
}

template<typename T>
RefPtr<CSSRule> Parser::parse_a_rule(TokenStream<T>& tokens)
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
    return parse_a_list_of_rules(m_token_stream);
}

template<typename T>
NonnullRefPtrVector<CSSRule> Parser::parse_a_list_of_rules(TokenStream<T>& tokens)
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
    return parse_a_declaration(m_token_stream);
}

template<typename T>
Optional<StyleProperty> Parser::parse_a_declaration(TokenStream<T>& tokens)
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
    return parse_a_list_of_declarations(m_token_stream);
}

template<typename T>
RefPtr<CSSStyleDeclaration> Parser::parse_a_list_of_declarations(TokenStream<T>& tokens)
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
    return parse_a_component_value(m_token_stream);
}

template<typename T>
Optional<StyleComponentValueRule> Parser::parse_a_component_value(TokenStream<T>& tokens)
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
    return parse_a_list_of_component_values(m_token_stream);
}

template<typename T>
Vector<StyleComponentValueRule> Parser::parse_a_list_of_component_values(TokenStream<T>& tokens)
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
    return parse_a_comma_separated_list_of_component_values(m_token_stream);
}

template<typename T>
Vector<Vector<StyleComponentValueRule>> Parser::parse_a_comma_separated_list_of_component_values(TokenStream<T>& tokens)
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
    // FIXME: Handle data: urls (RFC2397)

    auto is_data_url = [](StringView& url_string) -> bool {
        return url_string.starts_with("data:", CaseSensitivity::CaseInsensitive);
    };

    if (component_value.is(Token::Type::Url)) {
        auto url_string = component_value.token().url();
        if (is_data_url(url_string))
            return {};
        return context.complete_url(url_string);
    }
    if (component_value.is_function() && component_value.function().name().equals_ignoring_case("url")) {
        auto& function_values = component_value.function().values();
        // FIXME: Handle url-modifiers. https://www.w3.org/TR/css-values-4/#url-modifiers
        for (size_t i = 0; i < function_values.size(); ++i) {
            auto& value = function_values[i];
            if (value.is(Token::Type::Whitespace))
                continue;
            if (value.is(Token::Type::String)) {
                auto url_string = value.token().string();
                if (is_data_url(url_string))
                    return {};
                return context.complete_url(url_string);
            }
            break;
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
    return parse_a_list_of_declarations(stream);
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

RefPtr<StyleValue> Parser::parse_builtin_value(ParsingContext const&, StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Ident)) {
        auto ident = component_value.token().ident();
        if (ident.equals_ignoring_case("inherit"))
            return InheritStyleValue::the();
        if (ident.equals_ignoring_case("initial"))
            return InitialStyleValue::the();
        if (ident.equals_ignoring_case("unset"))
            return UnsetStyleValue::the();
        // FIXME: Implement `revert` and `revert-layer` keywords, from Cascade4 and Cascade5 respectively
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_dynamic_value(ParsingContext const& context, StyleComponentValueRule const& component_value)
{
    if (component_value.is_function()) {
        auto& function = component_value.function();

        if (function.name().equals_ignoring_case("calc")) {
            auto calc_expression = parse_calc_expression(context, function.values());
            // FIXME: Either produce a string value of calc() here, or do so in CalculatedStyleValue::to_string().
            if (calc_expression)
                return CalculatedStyleValue::create("(FIXME:calc to string)", calc_expression.release_nonnull());
        } else if (function.name().equals_ignoring_case("var")) {
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
        } else if (unit_string.equals_ignoring_case("ch")) {
            type = Length::Type::Ch;
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
        } else {
            return {};
        }

        numeric_value = try_parse_float(length_string);
    } else if (component_value.is(Token::Type::Percentage)) {
        type = Length::Type::Percentage;
        auto value_string = component_value.token().m_value.string_view();
        numeric_value = try_parse_float(value_string);
    } else if (component_value.is(Token::Type::Ident) && component_value.token().ident().equals_ignoring_case("auto")) {
        return Length::make_auto();
    } else if (component_value.is(Token::Type::Number)) {
        auto value_string = component_value.token().m_value.string_view();
        if (value_string == "0") {
            type = Length::Type::Px;
            numeric_value = 0;
        } else if (context.in_quirks_mode() && property_has_quirk(context.current_property_id(), Quirk::UnitlessLength)) {
            // https://quirks.spec.whatwg.org/#quirky-length-value
            // FIXME: Disallow quirk when inside a CSS sub-expression (like `calc()`)
            // "The <quirky-length> value must not be supported in arguments to CSS expressions other than the rect()
            // expression, and must not be supported in the supports() static method of the CSS interface."
            type = Length::Type::Px;
            numeric_value = try_parse_float(value_string);
        }
    }

    if (!numeric_value.has_value())
        return {};

    return Length(numeric_value.value(), type);
}

RefPtr<StyleValue> Parser::parse_length_value(ParsingContext const& context, StyleComponentValueRule const& component_value)
{
    // Numbers with no units can be lengths, in two situations:
    // 1) We're in quirks mode, and it's an integer.
    // 2) It's a 0.
    // We handle case 1 here. Case 2 is handled by NumericStyleValue pretending to be a LengthStyleValue if it is 0.

    // FIXME: "auto" is also treated as a Length, and most of the time that is how it is used, but not always.
    // Possibly it should always be an Identifier, and then quietly converted to a Length when needed, like 0 above.
    // Right now, it instead is quietly converted to an Identifier when needed.
    if (component_value.is(Token::Type::Dimension) || component_value.is(Token::Type::Percentage)
        || (component_value.is(Token::Type::Ident) && component_value.token().ident().equals_ignoring_case("auto"sv))
        || (context.in_quirks_mode() && component_value.is(Token::Type::Number) && component_value.token().m_value.string_view() != "0"sv)) {
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

static inline bool is_background_repeat(StyleValue const& value)
{
    switch (value.to_identifier()) {
    case ValueID::NoRepeat:
    case ValueID::Repeat:
    case ValueID::RepeatX:
    case ValueID::RepeatY:
    case ValueID::Round:
    case ValueID::Space:
        return true;
    default:
        return false;
    }
}

static inline bool is_background_image(StyleValue const& value)
{
    if (value.is_image())
        return true;
    if (value.to_identifier() == ValueID::None)
        return true;
    return false;
}

RefPtr<StyleValue> Parser::parse_background_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    RefPtr<StyleValue> background_color;
    RefPtr<StyleValue> background_image;
    RefPtr<StyleValue> repeat_x;
    RefPtr<StyleValue> repeat_y;
    // FIXME: Implement background-position.
    // FIXME: Implement background-size.
    // FIXME: Implement background-attachment.
    // FIXME: Implement background-clip.
    // FIXME: Implement background-origin.

    for (size_t i = 0; i < component_values.size(); ++i) {
        auto& part = component_values[i];

        // FIXME: Handle multiple backgrounds, by returning a List of BackgroundStyleValues.
        if (part.is(Token::Type::Comma)) {
            dbgln("CSS Parser does not yet support multiple comma-separated values for background.");
            break;
        }

        auto value = parse_css_value(context, PropertyID::Background, part);
        if (!value) {
            return nullptr;
        }

        if (value->is_color()) {
            if (background_color)
                return nullptr;
            background_color = value.release_nonnull();
            continue;
        }
        if (is_background_image(*value)) {
            if (background_image)
                return nullptr;
            background_image = value.release_nonnull();
            continue;
        }
        if (is_background_repeat(*value)) {
            if (repeat_x)
                return nullptr;

            auto value_id = value->to_identifier();
            if (value_id == ValueID::RepeatX || value_id == ValueID::RepeatY) {
                repeat_x = IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::Repeat : ValueID::NoRepeat);
                repeat_y = IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::NoRepeat : ValueID::Repeat);
                continue;
            }

            // Check following value, if it's also a repeat, set both.
            if (i + 1 < component_values.size()) {
                auto next_value = parse_css_value(context, PropertyID::Background, component_values[i + 1]);
                if (next_value && is_background_repeat(*next_value)) {
                    ++i;
                    repeat_x = value.release_nonnull();
                    repeat_y = next_value.release_nonnull();
                    continue;
                }
            }
            auto repeat = value.release_nonnull();
            repeat_x = repeat;
            repeat_y = repeat;
            continue;
        }

        return nullptr;
    }

    if (!background_color)
        background_color = ColorStyleValue::create(Color::Transparent);
    if (!background_image)
        background_image = IdentifierStyleValue::create(ValueID::None);
    if (!repeat_x)
        repeat_x = IdentifierStyleValue::create(ValueID::Repeat);
    if (!repeat_y)
        repeat_y = IdentifierStyleValue::create(ValueID::Repeat);

    return BackgroundStyleValue::create(background_color.release_nonnull(), background_image.release_nonnull(), repeat_x.release_nonnull(), repeat_y.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_background_image_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() == 1) {
        auto maybe_value = parse_css_value(context, PropertyID::BackgroundImage, component_values.first());
        if (!maybe_value)
            return nullptr;
        auto value = maybe_value.release_nonnull();
        if (is_background_image(*value))
            return value;
        return nullptr;
    }

    // FIXME: Handle multiple sets of comma-separated values.
    dbgln("CSS Parser does not yet support multiple comma-separated values for background-image.");
    return nullptr;
}

RefPtr<StyleValue> Parser::parse_background_repeat_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_directional_repeat = [](StyleValue const& value) -> bool {
        auto value_id = value.to_identifier();
        return value_id == ValueID::RepeatX || value_id == ValueID::RepeatY;
    };

    if (component_values.size() == 1) {
        auto maybe_value = parse_css_value(context, PropertyID::BackgroundRepeat, component_values.first());
        if (!maybe_value)
            return nullptr;
        auto value = maybe_value.release_nonnull();
        if (!is_background_repeat(*value))
            return nullptr;

        if (is_directional_repeat(value)) {
            auto value_id = value->to_identifier();
            return BackgroundRepeatStyleValue::create(
                IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::Repeat : ValueID::NoRepeat),
                IdentifierStyleValue::create(value_id == ValueID::RepeatX ? ValueID::NoRepeat : ValueID::Repeat));
        }
        return BackgroundRepeatStyleValue::create(value, value);
    }

    if (component_values.size() == 2) {
        auto maybe_x_value = parse_css_value(context, PropertyID::BackgroundRepeatX, component_values[0]);
        auto maybe_y_value = parse_css_value(context, PropertyID::BackgroundRepeatY, component_values[1]);
        if (!maybe_x_value || !maybe_y_value)
            return nullptr;

        auto x_value = maybe_x_value.release_nonnull();
        auto y_value = maybe_y_value.release_nonnull();
        if (!is_background_repeat(x_value) || !is_background_repeat(y_value))
            return nullptr;
        if (is_directional_repeat(x_value) || is_directional_repeat(y_value))
            return nullptr;
        return BackgroundRepeatStyleValue::create(x_value, y_value);
    }

    // FIXME: Handle multiple sets of comma-separated values.
    dbgln("CSS Parser does not yet support multiple comma-separated values for background-repeat.");
    return nullptr;
}

RefPtr<StyleValue> Parser::parse_border_value(ParsingContext const& context, PropertyID property_id, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_line_style = [](StyleValue const& value) -> bool {
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
    };

    auto is_line_width = [](StyleValue const& value) -> bool {
        if (value.is_length())
            return true;

        // FIXME: Implement thin/medium/thick
        switch (value.to_identifier()) {
        case ValueID::None:
            return true;
        default:
            return false;
        }
    };

    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> border_width;
    RefPtr<StyleValue> border_color;
    RefPtr<StyleValue> border_style;

    for (auto& part : component_values) {
        auto value = parse_css_value(context, property_id, part);
        if (!value)
            return nullptr;

        if (is_line_width(*value)) {
            if (border_width)
                return nullptr;
            border_width = value.release_nonnull();
            continue;
        }
        if (value->is_color()) {
            if (border_color)
                return nullptr;
            border_color = value.release_nonnull();
            continue;
        }
        if (is_line_style(*value)) {
            if (border_style)
                return nullptr;
            border_style = value.release_nonnull();
            continue;
        }

        return nullptr;
    }

    if (!border_width)
        border_width = IdentifierStyleValue::create(ValueID::Medium);
    if (!border_style)
        border_style = IdentifierStyleValue::create(ValueID::None);
    // FIXME: Default should be `currentcolor` special value. https://www.w3.org/TR/css-color-4/#currentcolor-color
    if (!border_color)
        border_color = ColorStyleValue::create(Gfx::Color::Black);

    return BorderStyleValue::create(border_width.release_nonnull(), border_style.release_nonnull(), border_color.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_border_radius_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() == 2) {
        auto horizontal = parse_length(context, component_values[0]);
        auto vertical = parse_length(context, component_values[1]);
        if (horizontal.has_value() && vertical.has_value())
            return BorderRadiusStyleValue::create(horizontal.value(), vertical.value());

        return nullptr;
    }

    if (component_values.size() == 1) {
        auto radius = parse_length(context, component_values[0]);
        if (radius.has_value())
            return BorderRadiusStyleValue::create(radius.value(), radius.value());
        return nullptr;
    }

    return nullptr;
}

RefPtr<StyleValue> Parser::parse_border_radius_shorthand_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto top_left = [&](Vector<Length>& radii) { return radii[0]; };
    auto top_right = [&](Vector<Length>& radii) {
        switch (radii.size()) {
        case 4:
        case 3:
        case 2:
            return radii[1];
        case 1:
            return radii[0];
        default:
            VERIFY_NOT_REACHED();
        }
    };
    auto bottom_right = [&](Vector<Length>& radii) {
        switch (radii.size()) {
        case 4:
        case 3:
            return radii[2];
        case 2:
        case 1:
            return radii[0];
        default:
            VERIFY_NOT_REACHED();
        }
    };
    auto bottom_left = [&](Vector<Length>& radii) {
        switch (radii.size()) {
        case 4:
            return radii[3];
        case 3:
        case 2:
            return radii[1];
        case 1:
            return radii[0];
        default:
            VERIFY_NOT_REACHED();
        }
    };

    Vector<Length> horizontal_radii;
    Vector<Length> vertical_radii;
    bool reading_vertical = false;

    for (auto& value : component_values) {
        if (value.is(Token::Type::Delim) && value.token().delim() == "/"sv) {
            if (reading_vertical || horizontal_radii.is_empty())
                return nullptr;

            reading_vertical = true;
            continue;
        }

        auto maybe_length = parse_length(context, value);
        if (!maybe_length.has_value())
            return nullptr;
        if (reading_vertical) {
            vertical_radii.append(maybe_length.value());
        } else {
            horizontal_radii.append(maybe_length.value());
        }
    }

    if (horizontal_radii.size() > 4 || vertical_radii.size() > 4
        || horizontal_radii.is_empty()
        || (reading_vertical && vertical_radii.is_empty()))
        return nullptr;

    NonnullRefPtrVector<StyleValue> border_radii;
    border_radii.append(BorderRadiusStyleValue::create(top_left(horizontal_radii),
        vertical_radii.is_empty() ? top_left(horizontal_radii) : top_left(vertical_radii)));
    border_radii.append(BorderRadiusStyleValue::create(top_right(horizontal_radii),
        vertical_radii.is_empty() ? top_right(horizontal_radii) : top_right(vertical_radii)));
    border_radii.append(BorderRadiusStyleValue::create(bottom_right(horizontal_radii),
        vertical_radii.is_empty() ? bottom_right(horizontal_radii) : bottom_right(vertical_radii)));
    border_radii.append(BorderRadiusStyleValue::create(bottom_left(horizontal_radii),
        vertical_radii.is_empty() ? bottom_left(horizontal_radii) : bottom_left(vertical_radii)));

    return StyleValueList::create(move(border_radii));
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

RefPtr<StyleValue> Parser::parse_flex_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_flex_grow_or_shrink = [](StyleValue const& value) -> bool {
        if (value.is_numeric())
            return true;
        return false;
    };

    auto is_flex_basis = [](StyleValue const& value) -> bool {
        if (value.is_length())
            return true;
        switch (value.to_identifier()) {
        case ValueID::Auto:
        case ValueID::Content:
            return true;
        default:
            return false;
        }
    };

    if (component_values.size() == 1) {
        auto value = parse_css_value(context, PropertyID::Flex, component_values[0]);
        if (!value)
            return nullptr;

        switch (value->to_identifier()) {
        case ValueID::Auto: {
            auto one = NumericStyleValue::create(1);
            return FlexStyleValue::create(one, one, IdentifierStyleValue::create(ValueID::Auto));
        }
        case ValueID::None: {
            auto zero = NumericStyleValue::create(0);
            return FlexStyleValue::create(zero, zero, IdentifierStyleValue::create(ValueID::Auto));
        }
        default:
            break;
        }
    }

    RefPtr<StyleValue> flex_grow;
    RefPtr<StyleValue> flex_shrink;
    RefPtr<StyleValue> flex_basis;

    for (size_t i = 0; i < component_values.size(); ++i) {
        auto value = parse_css_value(context, PropertyID::Flex, component_values[i]);
        if (!value)
            return nullptr;

        // Zero is a valid value for basis, but only if grow and shrink are already specified.
        if (value->is_numeric() && static_cast<NumericStyleValue&>(*value).value() == 0) {
            if (flex_grow && flex_shrink && !flex_basis) {
                flex_basis = LengthStyleValue::create(Length(0, Length::Type::Px));
                continue;
            }
        }

        if (is_flex_grow_or_shrink(*value)) {
            if (flex_grow)
                return nullptr;
            flex_grow = value.release_nonnull();

            // Flex-shrink may optionally follow directly after.
            if (i + 1 < component_values.size()) {
                auto second_value = parse_css_value(context, PropertyID::Flex, component_values[i + 1]);
                if (second_value && is_flex_grow_or_shrink(*second_value)) {
                    flex_shrink = second_value.release_nonnull();
                    i++;
                }
            }
            continue;
        }

        if (is_flex_basis(*value)) {
            if (flex_basis)
                return nullptr;
            flex_basis = value.release_nonnull();
            continue;
        }

        return nullptr;
    }

    if (!flex_grow)
        flex_grow = NumericStyleValue::create(0);
    if (!flex_shrink)
        flex_shrink = NumericStyleValue::create(1);
    if (!flex_basis)
        flex_basis = IdentifierStyleValue::create(ValueID::Auto);

    return FlexStyleValue::create(flex_grow.release_nonnull(), flex_shrink.release_nonnull(), flex_basis.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_flex_flow_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_flex_direction = [](StyleValue const& value) -> bool {
        switch (value.to_identifier()) {
        case ValueID::Row:
        case ValueID::RowReverse:
        case ValueID::Column:
        case ValueID::ColumnReverse:
            return true;
        default:
            return false;
        }
    };

    auto is_flex_wrap = [](StyleValue const& value) -> bool {
        switch (value.to_identifier()) {
        case ValueID::Wrap:
        case ValueID::Nowrap:
        case ValueID::WrapReverse:
            return true;
        default:
            return false;
        }
    };

    if (component_values.size() > 2)
        return nullptr;

    RefPtr<StyleValue> flex_direction;
    RefPtr<StyleValue> flex_wrap;

    for (auto& part : component_values) {
        auto value = parse_css_value(context, PropertyID::FlexFlow, part);
        if (!value)
            return nullptr;
        if (is_flex_direction(*value)) {
            if (flex_direction)
                return nullptr;
            flex_direction = value.release_nonnull();
            continue;
        }
        if (is_flex_wrap(*value)) {
            if (flex_wrap)
                return nullptr;
            flex_wrap = value.release_nonnull();
            continue;
        }
    }

    if (!flex_direction)
        flex_direction = IdentifierStyleValue::create(ValueID::Row);
    if (!flex_wrap)
        flex_wrap = IdentifierStyleValue::create(ValueID::Nowrap);

    return FlexFlowStyleValue::create(flex_direction.release_nonnull(), flex_wrap.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_font_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_font_size = [](StyleValue const& value) -> bool {
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
    };

    auto is_font_style = [](StyleValue const& value) -> bool {
        // FIXME: Handle angle parameter to `oblique`: https://www.w3.org/TR/css-fonts-4/#font-style-prop
        switch (value.to_identifier()) {
        case ValueID::Normal:
        case ValueID::Italic:
        case ValueID::Oblique:
            return true;
        default:
            return false;
        }
    };

    auto is_font_weight = [](StyleValue const& value) -> bool {
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
    };

    auto is_line_height = [](StyleValue const& value) -> bool {
        if (value.is_numeric())
            return true;
        if (value.is_length())
            return true;
        if (value.to_identifier() == ValueID::Normal)
            return true;
        return false;
    };

    RefPtr<StyleValue> font_style;
    RefPtr<StyleValue> font_weight;
    RefPtr<StyleValue> font_size;
    RefPtr<StyleValue> line_height;
    RefPtr<StyleValue> font_families;
    // FIXME: Implement font-stretch and font-variant.

    // FIXME: Handle system fonts. (caption, icon, menu, message-box, small-caption, status-bar)

    // Several sub-properties can be "normal", and appear in any order: style, variant, weight, stretch
    // So, we have to handle that separately.
    int normal_count = 0;

    for (size_t i = 0; i < component_values.size(); ++i) {
        auto value = parse_css_value(context, PropertyID::Font, component_values[i]);
        if (!value)
            return nullptr;

        if (value->to_identifier() == ValueID::Normal) {
            normal_count++;
            continue;
        }
        if (is_font_style(*value)) {
            if (font_style)
                return nullptr;
            font_style = value.release_nonnull();
            continue;
        }
        if (is_font_weight(*value)) {
            if (font_weight)
                return nullptr;
            font_weight = value.release_nonnull();
            continue;
        }
        if (is_font_size(*value)) {
            if (font_size)
                return nullptr;
            font_size = value.release_nonnull();

            // Consume `/ line-height` if present
            if (i + 2 < component_values.size()) {
                auto maybe_solidus = component_values[i + 1];
                if (maybe_solidus.is(Token::Type::Delim) && maybe_solidus.token().delim() == "/"sv) {
                    auto maybe_line_height = parse_css_value(context, PropertyID::Font, component_values[i + 2]);
                    if (!(maybe_line_height && is_line_height(*maybe_line_height)))
                        return nullptr;
                    line_height = maybe_line_height.release_nonnull();
                    i += 2;
                }
            }

            // Consume font-families
            auto maybe_font_families = parse_font_family_value(context, component_values, i + 1);
            if (!maybe_font_families)
                return nullptr;
            font_families = maybe_font_families.release_nonnull();
            break;
        }
        return nullptr;
    }

    // Since normal is the default value for all the properties that can have it, we don't have to actually
    // set anything to normal here. It'll be set when we create the FontStyleValue below.
    // We just need to make sure we were not given more normals than will fit.
    int unset_value_count = (font_style ? 0 : 1) + (font_weight ? 0 : 1);
    if (unset_value_count < normal_count)
        return nullptr;

    if (!font_size || !font_families)
        return nullptr;

    if (!font_style)
        font_style = IdentifierStyleValue::create(ValueID::Normal);
    if (!font_weight)
        font_weight = IdentifierStyleValue::create(ValueID::Normal);
    if (!line_height)
        line_height = IdentifierStyleValue::create(ValueID::Normal);

    return FontStyleValue::create(font_style.release_nonnull(), font_weight.release_nonnull(), font_size.release_nonnull(), line_height.release_nonnull(), font_families.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_font_family_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values, size_t start_index)
{
    auto is_generic_font_family = [](ValueID identifier) -> bool {
        switch (identifier) {
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
    };

    auto is_comma_or_eof = [&](size_t i) -> bool {
        if (i < component_values.size()) {
            auto& maybe_comma = component_values[i];
            if (!maybe_comma.is(Token::Type::Comma))
                return false;
        }
        return true;
    };

    // Note: Font-family names can either be a quoted string, or a keyword, or a series of custom-idents.
    // eg, these are equivalent:
    //     font-family: my cool     font\!, serif;
    //     font-family: "my cool font!", serif;
    NonnullRefPtrVector<StyleValue> font_families;
    Vector<String> current_name_parts;
    for (size_t i = start_index; i < component_values.size(); ++i) {
        auto& part = component_values[i];

        if (part.is(Token::Type::String)) {
            // `font-family: my cool "font";` is invalid.
            if (!current_name_parts.is_empty())
                return nullptr;
            if (!is_comma_or_eof(i + 1))
                return nullptr;
            font_families.append(StringStyleValue::create(part.token().string()));
            i++;
            continue;
        }
        if (part.is(Token::Type::Ident)) {
            // If this is a valid identifier, it's NOT a custom-ident and can't be part of a larger name.
            auto maybe_ident = parse_css_value(context, PropertyID::FontFamily, part);
            if (maybe_ident) {
                // CSS-wide keywords are not allowed
                if (maybe_ident->is_builtin())
                    return nullptr;
                if (is_generic_font_family(maybe_ident->to_identifier())) {
                    // Can't have a generic-font-name as a token in an unquoted font name.
                    if (!current_name_parts.is_empty())
                        return nullptr;
                    if (!is_comma_or_eof(i + 1))
                        return nullptr;
                    font_families.append(maybe_ident.release_nonnull());
                    i++;
                    continue;
                }
            }
            current_name_parts.append(part.token().ident());
            continue;
        }
        if (part.is(Token::Type::Comma)) {
            if (current_name_parts.is_empty())
                return nullptr;
            font_families.append(StringStyleValue::create(String::join(' ', current_name_parts)));
            current_name_parts.clear();
            // Can't have a trailing comma
            if (i + 1 == component_values.size())
                return nullptr;
            continue;
        }
    }

    if (!current_name_parts.is_empty()) {
        font_families.append(StringStyleValue::create(String::join(' ', current_name_parts)));
        current_name_parts.clear();
    }

    if (font_families.is_empty())
        return nullptr;
    return StyleValueList::create(move(font_families));
}

RefPtr<StyleValue> Parser::parse_list_style_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_list_style_image = [](StyleValue const& value) -> bool {
        if (value.is_image())
            return true;
        if (value.is_identifier() && value.to_identifier() == ValueID::None)
            return true;

        return false;
    };

    auto is_list_style_position = [](StyleValue const& value) -> bool {
        switch (value.to_identifier()) {
        case ValueID::Inside:
        case ValueID::Outside:
            return true;
        default:
            return false;
        }
    };

    auto is_list_style_type = [](StyleValue const& value) -> bool {
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
            return false;
        }
    };

    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> list_position;
    RefPtr<StyleValue> list_image;
    RefPtr<StyleValue> list_type;
    int found_nones = 0;

    for (auto& part : component_values) {
        auto value = parse_css_value(context, PropertyID::ListStyle, part);
        if (!value)
            return nullptr;

        if (value->to_identifier() == ValueID::None) {
            found_nones++;
            continue;
        }

        if (is_list_style_position(*value)) {
            if (list_position)
                return nullptr;
            list_position = value.release_nonnull();
            continue;
        }
        if (is_list_style_image(*value)) {
            if (list_image)
                return nullptr;
            list_image = value.release_nonnull();
            continue;
        }
        if (is_list_style_type(*value)) {
            if (list_type)
                return nullptr;
            list_type = value.release_nonnull();
            continue;
        }
    }

    if (found_nones > 2)
        return nullptr;

    if (found_nones == 2) {
        if (list_image || list_type)
            return nullptr;
        auto none = IdentifierStyleValue::create(ValueID::None);
        list_image = none;
        list_type = none;

    } else if (found_nones == 1) {
        if (list_image && list_type)
            return nullptr;
        auto none = IdentifierStyleValue::create(ValueID::None);
        if (!list_image)
            list_image = none;
        if (!list_type)
            list_type = none;
    }

    if (!list_position)
        list_position = IdentifierStyleValue::create(ValueID::Outside);
    if (!list_image)
        list_image = IdentifierStyleValue::create(ValueID::None);
    if (!list_type)
        list_type = IdentifierStyleValue::create(ValueID::Disc);

    return ListStyleStyleValue::create(list_position.release_nonnull(), list_image.release_nonnull(), list_type.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_overflow_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_overflow = [](StyleValue const& value) -> bool {
        switch (value.to_identifier()) {
        case ValueID::Auto:
        case ValueID::Clip:
        case ValueID::Hidden:
        case ValueID::Scroll:
        case ValueID::Visible:
            return true;
        default:
            return false;
        }
    };

    if (component_values.size() == 1) {
        auto maybe_value = parse_css_value(context, PropertyID::Overflow, component_values.first());
        if (!maybe_value)
            return nullptr;
        auto value = maybe_value.release_nonnull();
        if (is_overflow(*value))
            return OverflowStyleValue::create(value, value);
        return nullptr;
    }

    if (component_values.size() == 2) {
        auto maybe_x_value = parse_css_value(context, PropertyID::OverflowX, component_values[0]);
        auto maybe_y_value = parse_css_value(context, PropertyID::OverflowY, component_values[1]);

        if (!maybe_x_value || !maybe_y_value)
            return nullptr;
        auto x_value = maybe_x_value.release_nonnull();
        auto y_value = maybe_y_value.release_nonnull();
        if (!is_overflow(x_value) || !is_overflow(y_value))
            return nullptr;
        return OverflowStyleValue::create(x_value, y_value);
    }

    return nullptr;
}

RefPtr<StyleValue> Parser::parse_text_decoration_value(ParsingContext const& context, Vector<StyleComponentValueRule> const& component_values)
{
    auto is_text_decoration_line = [](StyleValue const& value) -> bool {
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
    };

    auto is_text_decoration_style = [](StyleValue const& value) -> bool {
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
    };

    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> decoration_line;
    RefPtr<StyleValue> decoration_style;
    RefPtr<StyleValue> decoration_color;
    // FIXME: Implement 'text-decoration-thickness' parameter. https://www.w3.org/TR/css-text-decor-4/#text-decoration-width-property

    for (auto& part : component_values) {
        auto value = parse_css_value(context, PropertyID::TextDecoration, part);
        if (!value)
            return nullptr;

        if (value->is_color()) {
            if (decoration_color)
                return nullptr;
            decoration_color = value.release_nonnull();
            continue;
        }
        if (is_text_decoration_line(*value)) {
            if (decoration_line)
                return nullptr;
            decoration_line = value.release_nonnull();
            continue;
        }
        if (is_text_decoration_style(*value)) {
            if (decoration_style)
                return nullptr;
            decoration_style = value.release_nonnull();
            continue;
        }

        return nullptr;
    }

    if (!decoration_line)
        decoration_line = IdentifierStyleValue::create(ValueID::None);
    if (!decoration_style)
        decoration_style = IdentifierStyleValue::create(ValueID::Solid);
    // FIXME: Should default to 'currentcolor' special value: https://www.w3.org/TR/css-color-3/#currentcolor
    if (!decoration_color)
        decoration_color = InitialStyleValue::the();

    return TextDecorationStyleValue::create(decoration_line.release_nonnull(), decoration_style.release_nonnull(), decoration_color.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_as_css_value(PropertyID property_id)
{
    auto component_values = parse_as_list_of_component_values();
    auto tokens = TokenStream(component_values);
    return parse_css_value(property_id, tokens);
}

RefPtr<StyleValue> Parser::parse_css_value(PropertyID property_id, TokenStream<StyleComponentValueRule>& tokens)
{
    m_context.set_current_property_id(property_id);
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

    if (component_values.size() == 1) {
        if (auto parsed_value = parse_builtin_value(m_context, component_values.first()))
            return parsed_value;
    }

    // Special-case property handling
    switch (property_id) {
    case PropertyID::Background:
        if (auto parsed_value = parse_background_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::BackgroundImage:
        if (auto parsed_value = parse_background_image_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::BackgroundRepeat:
        if (auto parsed_value = parse_background_repeat_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::Border:
    case PropertyID::BorderBottom:
    case PropertyID::BorderLeft:
    case PropertyID::BorderRight:
    case PropertyID::BorderTop:
        if (auto parsed_value = parse_border_value(m_context, property_id, component_values))
            return parsed_value;
        break;
    case PropertyID::BorderTopLeftRadius:
    case PropertyID::BorderTopRightRadius:
    case PropertyID::BorderBottomRightRadius:
    case PropertyID::BorderBottomLeftRadius:
        if (auto parsed_value = parse_border_radius_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::BorderRadius:
        if (auto parsed_value = parse_border_radius_shorthand_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::BoxShadow:
        if (auto parsed_box_shadow = parse_box_shadow_value(m_context, component_values))
            return parsed_box_shadow;
        break;
    case PropertyID::Flex:
        if (auto parsed_value = parse_flex_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::FlexFlow:
        if (auto parsed_value = parse_flex_flow_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::Font:
        if (auto parsed_value = parse_font_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::FontFamily:
        if (auto parsed_value = parse_font_family_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::ListStyle:
        if (auto parsed_value = parse_list_style_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::Overflow:
        if (auto parsed_value = parse_overflow_value(m_context, component_values))
            return parsed_value;
        break;
    case PropertyID::TextDecoration:
        if (auto parsed_value = parse_text_decoration_value(m_context, component_values))
            return parsed_value;
        break;
    default:
        break;
    }

    if (component_values.size() == 1)
        return parse_css_value(m_context, property_id, component_values.first());

    // We have multiple values, so treat them as a StyleValueList.
    // FIXME: Specify in Properties.json whether to permit this for each property.
    NonnullRefPtrVector<StyleValue> parsed_values;
    for (auto& component_value : component_values) {
        auto parsed = parse_css_value(m_context, property_id, component_value);
        if (!parsed)
            return {};
        parsed_values.append(parsed.release_nonnull());
    }
    if (!parsed_values.is_empty())
        return StyleValueList::create(move(parsed_values));

    return {};
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

    if (auto builtin = parse_builtin_value(context, component_value))
        return builtin;

    if (auto dynamic = parse_dynamic_value(context, component_value))
        return dynamic;

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

OwnPtr<CalculatedStyleValue::CalcSum> Parser::parse_calc_expression(ParsingContext const& context, Vector<StyleComponentValueRule> const& values)
{
    auto tokens = TokenStream(values);
    return parse_calc_sum(context, tokens);
}

Optional<CalculatedStyleValue::CalcValue> Parser::parse_calc_value(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    auto current_token = tokens.next_token();

    if (current_token.is_block() && current_token.block().is_paren()) {
        auto block_values = TokenStream(current_token.block().values());
        auto parsed_calc_sum = parse_calc_sum(context, block_values);
        if (!parsed_calc_sum)
            return {};
        return (CalculatedStyleValue::CalcValue) { parsed_calc_sum.release_nonnull() };
    }

    if (current_token.is(Token::Type::Number)) {
        auto try_the_number = try_parse_float(current_token.token().number_string_value());
        if (try_the_number.has_value())
            return (CalculatedStyleValue::CalcValue) { try_the_number.value() };
        return {};
    }

    if (current_token.is(Token::Type::Dimension) || current_token.is(Token::Type::Percentage)) {
        auto maybe_length = parse_length(context, current_token);
        if (maybe_length.has_value() && !maybe_length.value().is_undefined())
            return (CalculatedStyleValue::CalcValue) { maybe_length.value() };
        return {};
    }

    return {};
}

OwnPtr<CalculatedStyleValue::CalcProductPartWithOperator> Parser::parse_calc_product_part_with_operator(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    // Note: The default value is not used or passed around.
    auto product_with_operator = make<CalculatedStyleValue::CalcProductPartWithOperator>(
        CalculatedStyleValue::CalcProductPartWithOperator::Multiply,
        CalculatedStyleValue::CalcNumberValue(0));

    tokens.skip_whitespace();

    auto& op_token = tokens.peek_token();
    if (!op_token.is(Token::Type::Delim))
        return nullptr;

    auto op = op_token.token().delim();
    if (op == "*"sv) {
        tokens.next_token();
        tokens.skip_whitespace();
        product_with_operator->op = CalculatedStyleValue::CalcProductPartWithOperator::Multiply;
        auto parsed_calc_value = parse_calc_value(context, tokens);
        if (!parsed_calc_value.has_value())
            return nullptr;
        product_with_operator->value = { parsed_calc_value.release_value() };

    } else if (op == "/"sv) {
        tokens.next_token();
        tokens.skip_whitespace();
        product_with_operator->op = CalculatedStyleValue::CalcProductPartWithOperator::Divide;
        auto parsed_calc_number_value = parse_calc_number_value(context, tokens);
        if (!parsed_calc_number_value.has_value())
            return nullptr;
        product_with_operator->value = { parsed_calc_number_value.release_value() };
    } else {
        return nullptr;
    }

    return product_with_operator;
}

OwnPtr<CalculatedStyleValue::CalcNumberProductPartWithOperator> Parser::parse_calc_number_product_part_with_operator(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    // Note: The default value is not used or passed around.
    auto number_product_with_operator = make<CalculatedStyleValue::CalcNumberProductPartWithOperator>(
        CalculatedStyleValue::CalcNumberProductPartWithOperator::Multiply,
        CalculatedStyleValue::CalcNumberValue(0));

    tokens.skip_whitespace();

    auto& op_token = tokens.peek_token();
    if (!op_token.is(Token::Type::Delim))
        return nullptr;

    auto op = op_token.token().delim();
    if (op == "*"sv) {
        tokens.next_token();
        tokens.skip_whitespace();
        number_product_with_operator->op = CalculatedStyleValue::CalcNumberProductPartWithOperator::Multiply;
    } else if (op == "/"sv) {
        tokens.next_token();
        tokens.skip_whitespace();
        number_product_with_operator->op = CalculatedStyleValue::CalcNumberProductPartWithOperator::Divide;
    } else {
        return nullptr;
    }

    auto parsed_calc_value = parse_calc_number_value(context, tokens);
    if (!parsed_calc_value.has_value())
        return nullptr;
    number_product_with_operator->value = parsed_calc_value.release_value();

    return number_product_with_operator;
}

OwnPtr<CalculatedStyleValue::CalcNumberProduct> Parser::parse_calc_number_product(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    auto calc_number_product = make<CalculatedStyleValue::CalcNumberProduct>(
        CalculatedStyleValue::CalcNumberValue(0),
        NonnullOwnPtrVector<CalculatedStyleValue::CalcNumberProductPartWithOperator> {});

    auto first_calc_number_value_or_error = parse_calc_number_value(context, tokens);
    if (!first_calc_number_value_or_error.has_value())
        return nullptr;
    calc_number_product->first_calc_number_value = first_calc_number_value_or_error.release_value();

    while (tokens.has_next_token()) {
        auto number_product_with_operator = parse_calc_number_product_part_with_operator(context, tokens);
        if (!number_product_with_operator)
            break;
        calc_number_product->zero_or_more_additional_calc_number_values.append(number_product_with_operator.release_nonnull());
    }

    return calc_number_product;
}

OwnPtr<CalculatedStyleValue::CalcNumberSumPartWithOperator> Parser::parse_calc_number_sum_part_with_operator(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    if (!(tokens.peek_token().is(Token::Type::Delim)
            && tokens.peek_token().token().delim().is_one_of("+"sv, "-"sv)
            && tokens.peek_token(1).is(Token::Type::Whitespace)))
        return nullptr;

    auto& token = tokens.next_token();
    tokens.skip_whitespace();

    CalculatedStyleValue::CalcNumberSumPartWithOperator::Operation op;
    auto delim = token.token().delim();
    if (delim == "+"sv)
        op = CalculatedStyleValue::CalcNumberSumPartWithOperator::Operation::Add;
    else if (delim == "-"sv)
        op = CalculatedStyleValue::CalcNumberSumPartWithOperator::Operation::Subtract;
    else
        return nullptr;

    auto calc_number_product = parse_calc_number_product(context, tokens);
    if (!calc_number_product)
        return nullptr;
    return make<CalculatedStyleValue::CalcNumberSumPartWithOperator>(op, calc_number_product.release_nonnull());
}

OwnPtr<CalculatedStyleValue::CalcNumberSum> Parser::parse_calc_number_sum(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    auto first_calc_number_product_or_error = parse_calc_number_product(context, tokens);
    if (!first_calc_number_product_or_error)
        return nullptr;

    NonnullOwnPtrVector<CalculatedStyleValue::CalcNumberSumPartWithOperator> additional {};
    while (tokens.has_next_token()) {
        auto calc_sum_part = parse_calc_number_sum_part_with_operator(context, tokens);
        if (!calc_sum_part)
            return nullptr;
        additional.append(calc_sum_part.release_nonnull());
    }

    tokens.skip_whitespace();

    auto calc_number_sum = make<CalculatedStyleValue::CalcNumberSum>(first_calc_number_product_or_error.release_nonnull(), move(additional));
    return calc_number_sum;
}

Optional<CalculatedStyleValue::CalcNumberValue> Parser::parse_calc_number_value(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    auto& first = tokens.peek_token();
    if (first.is_block() && first.block().is_paren()) {
        tokens.next_token();
        auto block_values = TokenStream(first.block().values());
        auto calc_number_sum = parse_calc_number_sum(context, block_values);
        if (calc_number_sum)
            return { calc_number_sum.release_nonnull() };
    }

    if (!first.is(Token::Type::Number))
        return {};
    tokens.next_token();

    auto try_the_number = try_parse_float(first.token().number_string_value());
    if (!try_the_number.has_value())
        return {};
    return try_the_number.value();
}

OwnPtr<CalculatedStyleValue::CalcProduct> Parser::parse_calc_product(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    auto calc_product = make<CalculatedStyleValue::CalcProduct>(
        CalculatedStyleValue::CalcValue(0),
        NonnullOwnPtrVector<CalculatedStyleValue::CalcProductPartWithOperator> {});

    auto first_calc_value_or_error = parse_calc_value(context, tokens);
    if (!first_calc_value_or_error.has_value())
        return nullptr;
    calc_product->first_calc_value = first_calc_value_or_error.release_value();

    while (tokens.has_next_token()) {
        auto product_with_operator = parse_calc_product_part_with_operator(context, tokens);
        if (!product_with_operator)
            break;
        calc_product->zero_or_more_additional_calc_values.append(product_with_operator.release_nonnull());
    }

    return calc_product;
}

OwnPtr<CalculatedStyleValue::CalcSumPartWithOperator> Parser::parse_calc_sum_part_with_operator(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    // The following has to have the shape of <Whitespace><+ or -><Whitespace>
    // But the first whitespace gets eaten in parse_calc_product_part_with_operator().
    if (!(tokens.peek_token().is(Token::Type::Delim)
            && tokens.peek_token().token().delim().is_one_of("+"sv, "-"sv)
            && tokens.peek_token(1).is(Token::Type::Whitespace)))
        return nullptr;

    auto& token = tokens.next_token();
    tokens.skip_whitespace();

    CalculatedStyleValue::CalcSumPartWithOperator::Operation op;
    auto delim = token.token().delim();
    if (delim == "+"sv)
        op = CalculatedStyleValue::CalcSumPartWithOperator::Operation::Add;
    else if (delim == "-"sv)
        op = CalculatedStyleValue::CalcSumPartWithOperator::Operation::Subtract;
    else
        return nullptr;

    auto calc_product = parse_calc_product(context, tokens);
    if (!calc_product)
        return nullptr;
    return make<CalculatedStyleValue::CalcSumPartWithOperator>(op, calc_product.release_nonnull());
};

OwnPtr<CalculatedStyleValue::CalcSum> Parser::parse_calc_sum(ParsingContext const& context, TokenStream<StyleComponentValueRule>& tokens)
{
    auto parsed_calc_product = parse_calc_product(context, tokens);
    if (!parsed_calc_product)
        return nullptr;

    NonnullOwnPtrVector<CalculatedStyleValue::CalcSumPartWithOperator> additional {};
    while (tokens.has_next_token()) {
        auto calc_sum_part = parse_calc_sum_part_with_operator(context, tokens);
        if (!calc_sum_part)
            return nullptr;
        additional.append(calc_sum_part.release_nonnull());
    }

    tokens.skip_whitespace();

    return make<CalculatedStyleValue::CalcSum>(parsed_calc_product.release_nonnull(), move(additional));
}

}

namespace Web {

RefPtr<CSS::CSSStyleSheet> parse_css(CSS::ParsingContext const& context, StringView const& css)
{
    if (css.is_empty())
        return CSS::CSSStyleSheet::create({});
    CSS::Parser parser(context, css);
    return parser.parse_as_stylesheet();
}

RefPtr<CSS::CSSStyleDeclaration> parse_css_declaration(CSS::ParsingContext const& context, StringView const& css)
{
    if (css.is_empty())
        return CSS::CSSStyleDeclaration::create({}, {});
    CSS::Parser parser(context, css);
    return parser.parse_as_list_of_declarations();
}

RefPtr<CSS::StyleValue> parse_css_value(CSS::ParsingContext const& context, StringView const& string, CSS::PropertyID property_id)
{
    if (string.is_empty())
        return {};
    CSS::Parser parser(context, string);
    return parser.parse_as_css_value(property_id);
}

Optional<CSS::SelectorList> parse_selector(CSS::ParsingContext const& context, StringView const& selector_text)
{
    CSS::Parser parser(context, selector_text);
    return parser.parse_as_selector();
}

RefPtr<CSS::StyleValue> parse_html_length(DOM::Document const& document, StringView const& string)
{
    auto integer = string.to_int();
    if (integer.has_value())
        return CSS::LengthStyleValue::create(CSS::Length::make_px(integer.value()));
    // FIXME: The const_cast is a hack.
    return parse_css_value(CSS::ParsingContext(const_cast<DOM::Document&>(document)), string);
}

}
