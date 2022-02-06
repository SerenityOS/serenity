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
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSMediaRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/CSSSupportsRule.h>
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

ParsingContext::ParsingContext(DOM::Document const& document)
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

AK::URL ParsingContext::complete_url(String const& addr) const
{
    return m_document ? m_document->url().complete_url(addr) : AK::URL::create_with_url_or_path(addr);
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
    if (m_iterator_offset >= 0)
        --m_iterator_offset;
}

template<typename T>
void TokenStream<T>::rewind_to_position(int position)
{
    VERIFY(position <= m_iterator_offset);
    m_iterator_offset = position;
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

Parser::Parser(ParsingContext const& context, StringView input, String const& encoding)
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
    auto parser_rules = consume_a_list_of_rules(tokens, true);
    NonnullRefPtrVector<CSSRule> rules;

    for (auto& raw_rule : parser_rules) {
        auto rule = convert_to_rule(raw_rule);
        if (rule)
            rules.append(*rule);
    }

    return CSSStyleSheet::create(rules);
}

Optional<SelectorList> Parser::parse_as_selector()
{
    auto selector_list = parse_a_selector(m_token_stream);
    if (!selector_list.is_error())
        return selector_list.release_value();

    return {};
}

template<typename T>
Result<SelectorList, Parser::ParsingResult> Parser::parse_a_selector(TokenStream<T>& tokens)
{
    return parse_a_selector_list(tokens);
}

Optional<SelectorList> Parser::parse_as_relative_selector()
{
    auto selector_list = parse_a_relative_selector(m_token_stream);
    if (!selector_list.is_error())
        return selector_list.release_value();

    return {};
}

template<typename T>
Result<SelectorList, Parser::ParsingResult> Parser::parse_a_relative_selector(TokenStream<T>& tokens)
{
    return parse_a_relative_selector_list(tokens);
}

template<typename T>
Result<SelectorList, Parser::ParsingResult> Parser::parse_a_selector_list(TokenStream<T>& tokens)
{
    auto comma_separated_lists = parse_a_comma_separated_list_of_component_values(tokens);

    NonnullRefPtrVector<Selector> selectors;
    for (auto& selector_parts : comma_separated_lists) {
        auto stream = TokenStream(selector_parts);
        auto selector = parse_complex_selector(stream, false);
        if (selector.is_error())
            return selector.error();
        selectors.append(selector.release_value());
    }

    if (selectors.is_empty())
        return ParsingResult::SyntaxError;

    return selectors;
}

template<typename T>
Result<SelectorList, Parser::ParsingResult> Parser::parse_a_relative_selector_list(TokenStream<T>& tokens)
{
    auto comma_separated_lists = parse_a_comma_separated_list_of_component_values(tokens);

    NonnullRefPtrVector<Selector> selectors;
    for (auto& selector_parts : comma_separated_lists) {
        auto stream = TokenStream(selector_parts);
        auto selector = parse_complex_selector(stream, true);
        if (selector.is_error())
            return selector.error();
        selectors.append(selector.release_value());
    }

    if (selectors.is_empty())
        return ParsingResult::SyntaxError;

    return selectors;
}

Result<NonnullRefPtr<Selector>, Parser::ParsingResult> Parser::parse_complex_selector(TokenStream<StyleComponentValueRule>& tokens, bool allow_starting_combinator)
{
    Vector<Selector::CompoundSelector> compound_selectors;

    auto first_selector = parse_compound_selector(tokens);
    if (first_selector.is_error())
        return first_selector.error();
    if (!allow_starting_combinator) {
        if (first_selector.value().combinator != Selector::Combinator::Descendant)
            return ParsingResult::SyntaxError;
        first_selector.value().combinator = Selector::Combinator::None;
    }
    compound_selectors.append(first_selector.value());

    while (tokens.has_next_token()) {
        auto compound_selector = parse_compound_selector(tokens);
        if (compound_selector.is_error()) {
            if (compound_selector.error() == ParsingResult::Done)
                break;
            else
                return compound_selector.error();
        }
        compound_selectors.append(compound_selector.value());
    }

    if (compound_selectors.is_empty())
        return ParsingResult::SyntaxError;

    return Selector::create(move(compound_selectors));
}

Result<Selector::CompoundSelector, Parser::ParsingResult> Parser::parse_compound_selector(TokenStream<StyleComponentValueRule>& tokens)
{
    tokens.skip_whitespace();

    auto combinator = parse_selector_combinator(tokens).value_or(Selector::Combinator::Descendant);

    tokens.skip_whitespace();

    Vector<Selector::SimpleSelector> simple_selectors;

    while (tokens.has_next_token()) {
        auto component = parse_simple_selector(tokens);
        if (component.is_error()) {
            if (component.error() == ParsingResult::Done)
                break;
            else
                return component.error();
        }

        simple_selectors.append(component.value());
    }

    if (simple_selectors.is_empty())
        return ParsingResult::Done;

    return Selector::CompoundSelector { combinator, move(simple_selectors) };
}

Optional<Selector::Combinator> Parser::parse_selector_combinator(TokenStream<StyleComponentValueRule>& tokens)
{
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

Result<Selector::SimpleSelector, Parser::ParsingResult> Parser::parse_simple_selector(TokenStream<StyleComponentValueRule>& tokens)
{
    auto peek_token_ends_selector = [&]() -> bool {
        auto& value = tokens.peek_token();
        return (value.is(Token::Type::EndOfFile) || value.is(Token::Type::Whitespace) || value.is(Token::Type::Comma));
    };

    if (peek_token_ends_selector())
        return ParsingResult::Done;

    auto& first_value = tokens.next_token();

    if (first_value.is(Token::Type::Delim) && first_value.token().delim() == "*"sv) {
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::Universal
        };

    } else if (first_value.is(Token::Type::Hash)) {
        if (first_value.token().hash_type() != Token::HashType::Id) {
            dbgln_if(CSS_PARSER_DEBUG, "Selector contains hash token that is not an id: {}", first_value.to_debug_string());
            return ParsingResult::SyntaxError;
        }
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::Id,
            .value = first_value.token().hash_value()
        };

    } else if (first_value.is(Token::Type::Delim) && first_value.token().delim() == "."sv) {
        if (peek_token_ends_selector())
            return ParsingResult::SyntaxError;

        auto& class_name_value = tokens.next_token();
        if (!class_name_value.is(Token::Type::Ident)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected an ident after '.', got: {}", class_name_value.to_debug_string());
            return ParsingResult::SyntaxError;
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
        auto attribute_tokens = TokenStream { first_value.block().values() };

        attribute_tokens.skip_whitespace();

        if (!attribute_tokens.has_next_token()) {
            dbgln_if(CSS_PARSER_DEBUG, "CSS attribute selector is empty!");
            return ParsingResult::SyntaxError;
        }

        // FIXME: Handle namespace prefix for attribute name.
        auto& attribute_part = attribute_tokens.next_token();
        if (!attribute_part.is(Token::Type::Ident)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected ident for attribute name, got: '{}'", attribute_part.to_debug_string());
            return ParsingResult::SyntaxError;
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

        attribute_tokens.skip_whitespace();
        if (!attribute_tokens.has_next_token())
            return simple_selector;

        auto& delim_part = attribute_tokens.next_token();
        if (!delim_part.is(Token::Type::Delim)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected a delim for attribute comparison, got: '{}'", delim_part.to_debug_string());
            return ParsingResult::SyntaxError;
        }

        if (delim_part.token().delim() == "="sv) {
            simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch;
        } else {
            if (!attribute_tokens.has_next_token()) {
                dbgln_if(CSS_PARSER_DEBUG, "Attribute selector ended part way through a match type.");
                return ParsingResult::SyntaxError;
            }

            auto& delim_second_part = attribute_tokens.next_token();
            if (!(delim_second_part.is(Token::Type::Delim) && delim_second_part.token().delim() == "=")) {
                dbgln_if(CSS_PARSER_DEBUG, "Expected a double delim for attribute comparison, got: '{}{}'", delim_part.to_debug_string(), delim_second_part.to_debug_string());
                return ParsingResult::SyntaxError;
            }

            if (delim_part.token().delim() == "~"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::ContainsWord;
            } else if (delim_part.token().delim() == "*"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::ContainsString;
            } else if (delim_part.token().delim() == "|"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment;
            } else if (delim_part.token().delim() == "^"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::StartsWithString;
            } else if (delim_part.token().delim() == "$"sv) {
                simple_selector.attribute.match_type = Selector::SimpleSelector::Attribute::MatchType::EndsWithString;
            } else {
                attribute_tokens.reconsume_current_input_token();
            }
        }

        attribute_tokens.skip_whitespace();
        if (!attribute_tokens.has_next_token()) {
            dbgln_if(CSS_PARSER_DEBUG, "Attribute selector ended without a value to match.");
            return ParsingResult::SyntaxError;
        }

        auto& value_part = attribute_tokens.next_token();
        if (!value_part.is(Token::Type::Ident) && !value_part.is(Token::Type::String)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected a string or ident for the value to match attribute against, got: '{}'", value_part.to_debug_string());
            return ParsingResult::SyntaxError;
        }
        simple_selector.attribute.value = value_part.token().is(Token::Type::Ident) ? value_part.token().ident() : value_part.token().string();

        attribute_tokens.skip_whitespace();

        // FIXME: Handle case-sensitivity suffixes. https://www.w3.org/TR/selectors-4/#attribute-case
        return simple_selector;

    } else if (first_value.is(Token::Type::Colon)) {
        if (peek_token_ends_selector())
            return ParsingResult::SyntaxError;

        bool is_pseudo = false;
        if (tokens.peek_token().is(Token::Type::Colon)) {
            is_pseudo = true;
            tokens.next_token();
            if (peek_token_ends_selector())
                return ParsingResult::SyntaxError;
        }

        if (is_pseudo) {
            Selector::SimpleSelector simple_selector {
                .type = Selector::SimpleSelector::Type::PseudoElement
            };

            auto& name_token = tokens.next_token();
            if (!name_token.is(Token::Type::Ident)) {
                dbgln_if(CSS_PARSER_DEBUG, "Expected an ident for pseudo-element, got: '{}'", name_token.to_debug_string());
                return ParsingResult::SyntaxError;
            }

            auto pseudo_name = name_token.token().ident();
            if (has_ignored_vendor_prefix(pseudo_name))
                return ParsingResult::IncludesIgnoredVendorPrefix;

            if (pseudo_name.equals_ignoring_case("after")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::After;
            } else if (pseudo_name.equals_ignoring_case("before")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::Before;
            } else if (pseudo_name.equals_ignoring_case("first-letter")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::FirstLetter;
            } else if (pseudo_name.equals_ignoring_case("first-line")) {
                simple_selector.pseudo_element = Selector::SimpleSelector::PseudoElement::FirstLine;
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-element: '::{}'", pseudo_name);
                return ParsingResult::SyntaxError;
            }

            return simple_selector;
        }

        if (peek_token_ends_selector())
            return ParsingResult::SyntaxError;

        auto& pseudo_class_token = tokens.next_token();
        Selector::SimpleSelector simple_selector {
            .type = Selector::SimpleSelector::Type::PseudoClass
        };

        if (pseudo_class_token.is(Token::Type::Ident)) {
            auto pseudo_name = pseudo_class_token.token().ident();
            if (has_ignored_vendor_prefix(pseudo_name))
                return ParsingResult::IncludesIgnoredVendorPrefix;

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
                dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-class: ':{}'", pseudo_name);
                return ParsingResult::SyntaxError;
            }

            return simple_selector;

        } else if (pseudo_class_token.is_function()) {

            auto& pseudo_function = pseudo_class_token.function();
            if (pseudo_function.name().equals_ignoring_case("not")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::Not;
                auto function_token_stream = TokenStream(pseudo_function.values());
                auto not_selector = parse_a_selector(function_token_stream);
                if (not_selector.is_error()) {
                    dbgln_if(CSS_PARSER_DEBUG, "Invalid selector in :not() clause");
                    return ParsingResult::SyntaxError;
                }
                simple_selector.pseudo_class.not_selector = not_selector.release_value();
            } else if (pseudo_function.name().equals_ignoring_case("nth-child")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::NthChild;
                auto function_values = TokenStream<StyleComponentValueRule>(pseudo_function.values());
                auto nth_child_pattern = parse_a_n_plus_b_pattern(function_values);
                if (nth_child_pattern.has_value()) {
                    simple_selector.pseudo_class.nth_child_pattern = nth_child_pattern.value();
                } else {
                    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid nth-child format");
                    return ParsingResult::SyntaxError;
                }
            } else if (pseudo_function.name().equals_ignoring_case("nth-last-child")) {
                simple_selector.pseudo_class.type = Selector::SimpleSelector::PseudoClass::Type::NthLastChild;
                auto function_values = TokenStream<StyleComponentValueRule>(pseudo_function.values());
                auto nth_child_pattern = parse_a_n_plus_b_pattern(function_values);
                if (nth_child_pattern.has_value()) {
                    simple_selector.pseudo_class.nth_child_pattern = nth_child_pattern.value();
                } else {
                    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid nth-child format");
                    return ParsingResult::SyntaxError;
                }
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-class function: ':{}'()", pseudo_function.name());
                return ParsingResult::SyntaxError;
            }

            return simple_selector;

        } else {
            dbgln_if(CSS_PARSER_DEBUG, "Unexpected Block in pseudo-class name, expected a function or identifier. '{}'", pseudo_class_token.to_debug_string());
            return ParsingResult::SyntaxError;
        }
    }

    // Whitespace is not required between the compound-selector and a combinator.
    // So, if we see a combinator, return that this compound-selector is done, instead of a syntax error.
    if (first_value.is(Token::Type::Delim)) {
        auto delim = first_value.token().delim();
        if ((delim == ">"sv) || (delim == "+"sv) || (delim == "~"sv) || (delim == "|"sv)) {
            tokens.reconsume_current_input_token();
            return ParsingResult::Done;
        }
    }

    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid simple selector!");
    return ParsingResult::SyntaxError;
}

NonnullRefPtrVector<MediaQuery> Parser::parse_as_media_query_list()
{
    return parse_a_media_query_list(m_token_stream);
}

template<typename T>
NonnullRefPtrVector<MediaQuery> Parser::parse_a_media_query_list(TokenStream<T>& tokens)
{
    // https://www.w3.org/TR/mediaqueries-4/#mq-list

    auto comma_separated_lists = parse_a_comma_separated_list_of_component_values(tokens);

    AK::NonnullRefPtrVector<MediaQuery> media_queries;
    for (auto& media_query_parts : comma_separated_lists) {
        auto stream = TokenStream(media_query_parts);
        media_queries.append(parse_media_query(stream));
    }

    return media_queries;
}

RefPtr<MediaQuery> Parser::parse_as_media_query()
{
    // https://www.w3.org/TR/cssom-1/#parse-a-media-query
    auto media_query_list = parse_as_media_query_list();
    if (media_query_list.is_empty())
        return MediaQuery::create_not_all();
    if (media_query_list.size() == 1)
        return media_query_list.first();
    return nullptr;
}

// `<media-query>`, https://www.w3.org/TR/mediaqueries-4/#typedef-media-query
NonnullRefPtr<MediaQuery> Parser::parse_media_query(TokenStream<StyleComponentValueRule>& tokens)
{
    // `<media-query> = <media-condition>
    //                | [ not | only ]? <media-type> [ and <media-condition-without-or> ]?`
    auto position = tokens.position();
    tokens.skip_whitespace();

    // `[ not | only ]?`, Returns whether to negate the query
    auto parse_initial_modifier = [](auto& tokens) -> Optional<bool> {
        auto position = tokens.position();
        tokens.skip_whitespace();
        auto& token = tokens.next_token();

        if (!token.is(Token::Type::Ident)) {
            tokens.rewind_to_position(position);
            return {};
        }

        auto ident = token.token().ident();
        if (ident.equals_ignoring_case("not")) {
            return true;
        } else if (ident.equals_ignoring_case("only")) {
            return false;
        }
        tokens.rewind_to_position(position);
        return {};
    };

    auto invalid_media_query = [&]() {
        // "A media query that does not match the grammar in the previous section must be replaced by `not all`
        // during parsing." - https://www.w3.org/TR/mediaqueries-5/#error-handling
        if constexpr (CSS_PARSER_DEBUG) {
            dbgln("Invalid media query:");
            tokens.dump_all_tokens();
        }
        tokens.rewind_to_position(position);
        return MediaQuery::create_not_all();
    };

    auto media_query = MediaQuery::create();
    tokens.skip_whitespace();

    // `<media-condition>`
    if (auto media_condition = parse_media_condition(tokens, MediaCondition::AllowOr::Yes)) {
        tokens.skip_whitespace();
        if (tokens.has_next_token())
            return invalid_media_query();
        media_query->m_media_condition = move(media_condition);
        return media_query;
    }

    // `[ not | only ]?`
    if (auto modifier = parse_initial_modifier(tokens); modifier.has_value()) {
        media_query->m_negated = modifier.value();
        tokens.skip_whitespace();
    }

    // `<media-type>`
    if (auto media_type = parse_media_type(tokens); media_type.has_value()) {
        media_query->m_media_type = media_type.value();
        tokens.skip_whitespace();
    } else {
        return invalid_media_query();
    }

    if (!tokens.has_next_token())
        return media_query;

    // `[ and <media-condition-without-or> ]?`
    if (auto maybe_and = tokens.next_token(); maybe_and.is(Token::Type::Ident) && maybe_and.token().ident().equals_ignoring_case("and")) {
        if (auto media_condition = parse_media_condition(tokens, MediaCondition::AllowOr::No)) {
            tokens.skip_whitespace();
            if (tokens.has_next_token())
                return invalid_media_query();
            media_query->m_media_condition = move(media_condition);
            return media_query;
        }
        return invalid_media_query();
    }

    return invalid_media_query();
}

// `<media-condition>`, https://www.w3.org/TR/mediaqueries-4/#typedef-media-condition
// `<media-condition-widthout-or>`, https://www.w3.org/TR/mediaqueries-4/#typedef-media-condition-without-or
// (We distinguish between these two with the `allow_or` parameter.)
OwnPtr<MediaCondition> Parser::parse_media_condition(TokenStream<StyleComponentValueRule>& tokens, MediaCondition::AllowOr allow_or)
{
    // `<media-not> | <media-in-parens> [ <media-and>* | <media-or>* ]`
    auto position = tokens.position();
    tokens.skip_whitespace();

    // `<media-not> = not <media-in-parens>`
    auto parse_media_not = [&](auto& tokens) -> OwnPtr<MediaCondition> {
        auto position = tokens.position();
        tokens.skip_whitespace();

        auto& first_token = tokens.next_token();
        if (first_token.is(Token::Type::Ident) && first_token.token().ident().equals_ignoring_case("not"sv)) {
            if (auto child_condition = parse_media_condition(tokens, MediaCondition::AllowOr::Yes))
                return MediaCondition::from_not(child_condition.release_nonnull());
        }

        tokens.rewind_to_position(position);
        return {};
    };

    auto parse_media_with_combinator = [&](auto& tokens, StringView combinator) -> OwnPtr<MediaCondition> {
        auto position = tokens.position();
        tokens.skip_whitespace();

        auto& first = tokens.next_token();
        if (first.is(Token::Type::Ident) && first.token().ident().equals_ignoring_case(combinator)) {
            tokens.skip_whitespace();
            if (auto media_in_parens = parse_media_in_parens(tokens))
                return media_in_parens;
        }

        tokens.rewind_to_position(position);
        return {};
    };

    // `<media-and> = and <media-in-parens>`
    auto parse_media_and = [&](auto& tokens) { return parse_media_with_combinator(tokens, "and"sv); };
    // `<media-or> = or <media-in-parens>`
    auto parse_media_or = [&](auto& tokens) { return parse_media_with_combinator(tokens, "or"sv); };

    // `<media-not>`
    if (auto maybe_media_not = parse_media_not(tokens))
        return maybe_media_not.release_nonnull();

    // `<media-in-parens> [ <media-and>* | <media-or>* ]`
    if (auto maybe_media_in_parens = parse_media_in_parens(tokens)) {
        tokens.skip_whitespace();
        // Only `<media-in-parens>`
        if (!tokens.has_next_token())
            return maybe_media_in_parens.release_nonnull();

        NonnullOwnPtrVector<MediaCondition> child_conditions;
        child_conditions.append(maybe_media_in_parens.release_nonnull());

        // `<media-and>*`
        if (auto media_and = parse_media_and(tokens)) {
            child_conditions.append(media_and.release_nonnull());

            tokens.skip_whitespace();
            while (tokens.has_next_token()) {
                if (auto next_media_and = parse_media_and(tokens)) {
                    child_conditions.append(next_media_and.release_nonnull());
                    tokens.skip_whitespace();
                    continue;
                }
                // We failed - invalid syntax!
                tokens.rewind_to_position(position);
                return {};
            }

            return MediaCondition::from_and_list(move(child_conditions));
        }

        // `<media-or>*`
        if (allow_or == MediaCondition::AllowOr::Yes) {
            if (auto media_or = parse_media_or(tokens)) {
                child_conditions.append(media_or.release_nonnull());

                tokens.skip_whitespace();
                while (tokens.has_next_token()) {
                    if (auto next_media_or = parse_media_or(tokens)) {
                        child_conditions.append(next_media_or.release_nonnull());
                        tokens.skip_whitespace();
                        continue;
                    }
                    // We failed - invalid syntax!
                    tokens.rewind_to_position(position);
                    return {};
                }

                return MediaCondition::from_or_list(move(child_conditions));
            }
        }
    }

    tokens.rewind_to_position(position);
    return {};
}

// `<media-feature>`, https://www.w3.org/TR/mediaqueries-4/#typedef-media-feature
Optional<MediaFeature> Parser::parse_media_feature(TokenStream<StyleComponentValueRule>& tokens)
{
    // `[ <mf-plain> | <mf-boolean> | <mf-range> ]`
    auto position = tokens.position();
    tokens.skip_whitespace();

    // `<mf-name> = <ident>`
    auto parse_mf_name = [](auto& tokens, bool allow_min_max_prefix) -> Optional<String> {
        auto& token = tokens.peek_token();
        if (token.is(Token::Type::Ident)) {
            auto name = token.token().ident();
            if (is_media_feature_name(name)) {
                tokens.next_token();
                return name;
            }

            if (allow_min_max_prefix && (name.starts_with("min-", CaseSensitivity::CaseInsensitive) || name.starts_with("max-", CaseSensitivity::CaseInsensitive))) {
                auto adjusted_name = name.substring_view(4);
                if (is_media_feature_name(adjusted_name)) {
                    tokens.next_token();
                    return name;
                }
            }
        }
        return {};
    };

    // `<mf-boolean> = <mf-name>`
    auto parse_mf_boolean = [&](auto& tokens) -> Optional<MediaFeature> {
        auto position = tokens.position();
        tokens.skip_whitespace();

        auto maybe_name = parse_mf_name(tokens, false);
        if (maybe_name.has_value()) {
            tokens.skip_whitespace();
            if (!tokens.has_next_token())
                return MediaFeature::boolean(maybe_name.release_value());
        }

        tokens.rewind_to_position(position);
        return {};
    };

    // `<mf-plain> = <mf-name> : <mf-value>`
    auto parse_mf_plain = [&](auto& tokens) -> Optional<MediaFeature> {
        auto position = tokens.position();
        tokens.skip_whitespace();

        if (auto maybe_name = parse_mf_name(tokens, true); maybe_name.has_value()) {
            tokens.skip_whitespace();
            if (tokens.next_token().is(Token::Type::Colon)) {
                tokens.skip_whitespace();
                if (auto maybe_value = parse_media_feature_value(tokens); maybe_value.has_value()) {
                    tokens.skip_whitespace();
                    if (!tokens.has_next_token())
                        return MediaFeature::plain(maybe_name.release_value(), maybe_value.release_value());
                }
            }
        }

        tokens.rewind_to_position(position);
        return {};
    };

    // `<mf-lt> = '<' '='?
    //  <mf-gt> = '>' '='?
    //  <mf-eq> = '='
    //  <mf-comparison> = <mf-lt> | <mf-gt> | <mf-eq>`
    auto parse_comparison = [](auto& tokens) -> Optional<MediaFeature::Comparison> {
        auto position = tokens.position();
        tokens.skip_whitespace();

        auto& first = tokens.next_token();
        if (first.is(Token::Type::Delim)) {
            auto first_delim = first.token().delim();
            if (first_delim == "="sv)
                return MediaFeature::Comparison::Equal;
            if (first_delim == "<"sv) {
                auto& second = tokens.peek_token();
                if (second.is(Token::Type::Delim) && second.token().delim() == "="sv) {
                    tokens.next_token();
                    return MediaFeature::Comparison::LessThanOrEqual;
                }
                return MediaFeature::Comparison::LessThan;
            }
            if (first_delim == ">"sv) {
                auto& second = tokens.peek_token();
                if (second.is(Token::Type::Delim) && second.token().delim() == "="sv) {
                    tokens.next_token();
                    return MediaFeature::Comparison::GreaterThanOrEqual;
                }
                return MediaFeature::Comparison::GreaterThan;
            }
        }

        tokens.rewind_to_position(position);
        return {};
    };

    auto flip = [](MediaFeature::Comparison comparison) {
        switch (comparison) {
        case MediaFeature::Comparison::Equal:
            return MediaFeature::Comparison::Equal;
        case MediaFeature::Comparison::LessThan:
            return MediaFeature::Comparison::GreaterThan;
        case MediaFeature::Comparison::LessThanOrEqual:
            return MediaFeature::Comparison::GreaterThanOrEqual;
        case MediaFeature::Comparison::GreaterThan:
            return MediaFeature::Comparison::LessThan;
        case MediaFeature::Comparison::GreaterThanOrEqual:
            return MediaFeature::Comparison::LessThanOrEqual;
        }
        VERIFY_NOT_REACHED();
    };

    auto comparisons_match = [](MediaFeature::Comparison a, MediaFeature::Comparison b) -> bool {
        switch (a) {
        case MediaFeature::Comparison::Equal:
            return b == MediaFeature::Comparison::Equal;
        case MediaFeature::Comparison::LessThan:
        case MediaFeature::Comparison::LessThanOrEqual:
            return b == MediaFeature::Comparison::LessThan || b == MediaFeature::Comparison::LessThanOrEqual;
        case MediaFeature::Comparison::GreaterThan:
        case MediaFeature::Comparison::GreaterThanOrEqual:
            return b == MediaFeature::Comparison::GreaterThan || b == MediaFeature::Comparison::GreaterThanOrEqual;
        }
        VERIFY_NOT_REACHED();
    };

    // `<mf-range> = <mf-name> <mf-comparison> <mf-value>
    //             | <mf-value> <mf-comparison> <mf-name>
    //             | <mf-value> <mf-lt> <mf-name> <mf-lt> <mf-value>
    //             | <mf-value> <mf-gt> <mf-name> <mf-gt> <mf-value>`
    auto parse_mf_range = [&](auto& tokens) -> Optional<MediaFeature> {
        auto position = tokens.position();
        tokens.skip_whitespace();

        // `<mf-name> <mf-comparison> <mf-value>`
        // NOTE: We have to check for <mf-name> first, since all <mf-name>s will also parse as <mf-value>.
        if (auto maybe_name = parse_mf_name(tokens, false); maybe_name.has_value()) {
            tokens.skip_whitespace();
            if (auto maybe_comparison = parse_comparison(tokens); maybe_comparison.has_value()) {
                tokens.skip_whitespace();
                if (auto maybe_value = parse_media_feature_value(tokens); maybe_value.has_value()) {
                    tokens.skip_whitespace();
                    if (!tokens.has_next_token() && !maybe_value->is_ident())
                        return MediaFeature::half_range(maybe_value.release_value(), flip(maybe_comparison.release_value()), maybe_name.release_value());
                }
            }
        }

        //  `<mf-value> <mf-comparison> <mf-name>
        // | <mf-value> <mf-lt> <mf-name> <mf-lt> <mf-value>
        // | <mf-value> <mf-gt> <mf-name> <mf-gt> <mf-value>`
        if (auto maybe_left_value = parse_media_feature_value(tokens); maybe_left_value.has_value()) {
            tokens.skip_whitespace();
            if (auto maybe_left_comparison = parse_comparison(tokens); maybe_left_comparison.has_value()) {
                tokens.skip_whitespace();
                if (auto maybe_name = parse_mf_name(tokens, false); maybe_name.has_value()) {
                    tokens.skip_whitespace();

                    if (!tokens.has_next_token())
                        return MediaFeature::half_range(maybe_left_value.release_value(), maybe_left_comparison.release_value(), maybe_name.release_value());

                    if (auto maybe_right_comparison = parse_comparison(tokens); maybe_right_comparison.has_value()) {
                        tokens.skip_whitespace();
                        if (auto maybe_right_value = parse_media_feature_value(tokens); maybe_right_value.has_value()) {
                            tokens.skip_whitespace();
                            // For this to be valid, the following must be true:
                            // - Comparisons must either both be >/>= or both be </<=.
                            // - Neither comparison can be `=`.
                            // - Neither value can be an ident.
                            auto left_comparison = maybe_left_comparison.release_value();
                            auto right_comparison = maybe_right_comparison.release_value();

                            if (!tokens.has_next_token()
                                && comparisons_match(left_comparison, right_comparison)
                                && left_comparison != MediaFeature::Comparison::Equal
                                && !maybe_left_value->is_ident() && !maybe_right_value->is_ident()) {
                                return MediaFeature::range(maybe_left_value.release_value(), left_comparison, maybe_name.release_value(), right_comparison, maybe_right_value.release_value());
                            }
                        }
                    }
                }
            }
        }

        tokens.rewind_to_position(position);
        return {};
    };

    if (auto maybe_mf_boolean = parse_mf_boolean(tokens); maybe_mf_boolean.has_value())
        return maybe_mf_boolean.release_value();

    if (auto maybe_mf_plain = parse_mf_plain(tokens); maybe_mf_plain.has_value())
        return maybe_mf_plain.release_value();

    if (auto maybe_mf_range = parse_mf_range(tokens); maybe_mf_range.has_value())
        return maybe_mf_range.release_value();

    tokens.rewind_to_position(position);
    return {};
}

Optional<MediaQuery::MediaType> Parser::parse_media_type(TokenStream<StyleComponentValueRule>& tokens)
{
    auto position = tokens.position();
    tokens.skip_whitespace();
    auto& token = tokens.next_token();

    if (!token.is(Token::Type::Ident)) {
        tokens.rewind_to_position(position);
        return {};
    }

    auto ident = token.token().ident();
    if (ident.equals_ignoring_case("all")) {
        return MediaQuery::MediaType::All;
    } else if (ident.equals_ignoring_case("aural")) {
        return MediaQuery::MediaType::Aural;
    } else if (ident.equals_ignoring_case("braille")) {
        return MediaQuery::MediaType::Braille;
    } else if (ident.equals_ignoring_case("embossed")) {
        return MediaQuery::MediaType::Embossed;
    } else if (ident.equals_ignoring_case("handheld")) {
        return MediaQuery::MediaType::Handheld;
    } else if (ident.equals_ignoring_case("print")) {
        return MediaQuery::MediaType::Print;
    } else if (ident.equals_ignoring_case("projection")) {
        return MediaQuery::MediaType::Projection;
    } else if (ident.equals_ignoring_case("screen")) {
        return MediaQuery::MediaType::Screen;
    } else if (ident.equals_ignoring_case("speech")) {
        return MediaQuery::MediaType::Speech;
    } else if (ident.equals_ignoring_case("tty")) {
        return MediaQuery::MediaType::TTY;
    } else if (ident.equals_ignoring_case("tv")) {
        return MediaQuery::MediaType::TV;
    }

    tokens.rewind_to_position(position);
    return {};
}

// `<media-in-parens>`, https://www.w3.org/TR/mediaqueries-4/#typedef-media-in-parens
OwnPtr<MediaCondition> Parser::parse_media_in_parens(TokenStream<StyleComponentValueRule>& tokens)
{
    // `<media-in-parens> = ( <media-condition> ) | ( <media-feature> ) | <general-enclosed>`
    auto position = tokens.position();
    tokens.skip_whitespace();

    // `( <media-condition> ) | ( <media-feature> )`
    auto& first_token = tokens.peek_token();
    if (first_token.is_block() && first_token.block().is_paren()) {
        TokenStream inner_token_stream { first_token.block().values() };
        if (auto maybe_media_condition = parse_media_condition(inner_token_stream, MediaCondition::AllowOr::Yes)) {
            tokens.next_token();
            return maybe_media_condition.release_nonnull();
        }
        if (auto maybe_media_feature = parse_media_feature(inner_token_stream); maybe_media_feature.has_value()) {
            tokens.next_token();
            return MediaCondition::from_feature(maybe_media_feature.release_value());
        }
    }

    // `<general-enclosed>`
    if (auto maybe_general_enclosed = parse_general_enclosed(tokens); maybe_general_enclosed.has_value())
        return MediaCondition::from_general_enclosed(maybe_general_enclosed.release_value());

    tokens.rewind_to_position(position);
    return {};
}

// `<mf-value>`, https://www.w3.org/TR/mediaqueries-4/#typedef-mf-value
Optional<MediaFeatureValue> Parser::parse_media_feature_value(TokenStream<StyleComponentValueRule>& tokens)
{
    // `<number> | <dimension> | <ident> | <ratio>`
    auto position = tokens.position();
    tokens.skip_whitespace();
    auto& first = tokens.next_token();

    // `<number>`
    if (first.is(Token::Type::Number))
        return MediaFeatureValue(first.token().number_value());

    // `<dimension>`
    if (auto length = parse_length(first); length.has_value())
        return MediaFeatureValue(length.release_value());

    // `<ident>`
    if (first.is(Token::Type::Ident))
        return MediaFeatureValue(first.token().ident());

    // FIXME: `<ratio>`, once we have ratios.

    tokens.rewind_to_position(position);
    return {};
}

RefPtr<Supports> Parser::parse_as_supports()
{
    return parse_a_supports(m_token_stream);
}

template<typename T>
RefPtr<Supports> Parser::parse_a_supports(TokenStream<T>& tokens)
{
    auto component_values = parse_a_list_of_component_values(tokens);
    TokenStream<StyleComponentValueRule> token_stream { component_values };
    auto maybe_condition = parse_supports_condition(token_stream);
    token_stream.skip_whitespace();
    if (maybe_condition && !token_stream.has_next_token())
        return Supports::create(maybe_condition.release_nonnull());

    return {};
}

OwnPtr<Supports::Condition> Parser::parse_supports_condition(TokenStream<StyleComponentValueRule>& tokens)
{
    tokens.skip_whitespace();
    auto start_position = tokens.position();

    auto& peeked_token = tokens.peek_token();
    // `not <supports-in-parens>`
    if (peeked_token.is(Token::Type::Ident) && peeked_token.token().ident().equals_ignoring_case("not")) {
        tokens.next_token();
        tokens.skip_whitespace();
        auto child = parse_supports_in_parens(tokens);
        if (child.has_value()) {
            auto* condition = new Supports::Condition;
            condition->type = Supports::Condition::Type::Not;
            condition->children.append(child.release_value());
            return adopt_own(*condition);
        }

        tokens.rewind_to_position(start_position);
        return {};
    }

    // `  <supports-in-parens> [ and <supports-in-parens> ]*
    //  | <supports-in-parens> [ or <supports-in-parens> ]*`
    Vector<Supports::InParens> children;
    Optional<Supports::Condition::Type> condition_type {};
    auto as_condition_type = [](auto& token) -> Optional<Supports::Condition::Type> {
        if (!token.is(Token::Type::Ident))
            return {};
        auto ident = token.token().ident();
        if (ident.equals_ignoring_case("and"))
            return Supports::Condition::Type::And;
        if (ident.equals_ignoring_case("or"))
            return Supports::Condition::Type::Or;
        return {};
    };

    bool is_invalid = false;
    while (tokens.has_next_token()) {
        if (!children.is_empty()) {
            // Expect `and` or `or` here
            auto maybe_combination = as_condition_type(tokens.next_token());
            if (!maybe_combination.has_value()) {
                is_invalid = true;
                break;
            }
            if (!condition_type.has_value()) {
                condition_type = maybe_combination.value();
            } else if (maybe_combination != condition_type) {
                is_invalid = true;
                break;
            }
        }

        tokens.skip_whitespace();

        if (auto in_parens = parse_supports_in_parens(tokens); in_parens.has_value()) {
            children.append(in_parens.release_value());
        } else {
            is_invalid = true;
            break;
        }

        tokens.skip_whitespace();
    }

    if (!is_invalid && !children.is_empty()) {
        auto* condition = new Supports::Condition;
        condition->type = condition_type.value_or(Supports::Condition::Type::Or);
        condition->children = move(children);
        return adopt_own(*condition);
    }

    tokens.rewind_to_position(start_position);
    return {};
}

Optional<Supports::InParens> Parser::parse_supports_in_parens(TokenStream<StyleComponentValueRule>& tokens)
{
    tokens.skip_whitespace();
    auto start_position = tokens.position();

    auto& first_token = tokens.peek_token();
    // `( <supports-condition> )`
    if (first_token.is_block() && first_token.block().is_paren()) {
        tokens.next_token();
        tokens.skip_whitespace();

        TokenStream child_tokens { first_token.block().values() };
        if (auto condition = parse_supports_condition(child_tokens)) {
            if (child_tokens.has_next_token()) {
                tokens.rewind_to_position(start_position);
                return {};
            }
            return Supports::InParens {
                .value = { condition.release_nonnull() }
            };
        }

        tokens.rewind_to_position(start_position);
    }

    // `<supports-feature>`
    if (auto feature = parse_supports_feature(tokens); feature.has_value()) {
        return Supports::InParens {
            .value = { feature.release_value() }
        };
    }

    // `<general-enclosed>`
    if (auto general_enclosed = parse_general_enclosed(tokens); general_enclosed.has_value()) {
        return Supports::InParens {
            .value = general_enclosed.release_value()
        };
    }

    tokens.rewind_to_position(start_position);
    return {};
}

Optional<Supports::Feature> Parser::parse_supports_feature(TokenStream<StyleComponentValueRule>& tokens)
{
    tokens.skip_whitespace();
    auto start_position = tokens.position();

    auto& first_token = tokens.next_token();
    // `<supports-decl>`
    if (first_token.is_block() && first_token.block().is_paren()) {
        TokenStream block_tokens { first_token.block().values() };
        if (auto declaration = consume_a_declaration(block_tokens); declaration.has_value()) {
            return Supports::Feature {
                .declaration = declaration->to_string()
            };
        }
    }

    tokens.rewind_to_position(start_position);
    return {};
}

// https://www.w3.org/TR/mediaqueries-4/#typedef-general-enclosed
Optional<GeneralEnclosed> Parser::parse_general_enclosed(TokenStream<StyleComponentValueRule>& tokens)
{
    tokens.skip_whitespace();
    auto start_position = tokens.position();

    auto& first_token = tokens.next_token();

    // `[ <function-token> <any-value>? ) ]`
    if (first_token.is_function())
        return GeneralEnclosed { first_token.to_string() };

    // `( <any-value>? )`
    if (first_token.is_block() && first_token.block().is_paren())
        return GeneralEnclosed { first_token.to_string() };

    tokens.rewind_to_position(start_position);
    return {};
}

template<typename T>
NonnullRefPtrVector<StyleRule> Parser::consume_a_list_of_rules(TokenStream<T>& tokens, bool top_level)
{
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

template<typename T>
NonnullRefPtr<StyleRule> Parser::consume_an_at_rule(TokenStream<T>& tokens)
{
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

        if constexpr (IsSame<T, StyleComponentValueRule>) {
            StyleComponentValueRule const& component_value = token;
            if (component_value.is_block() && component_value.block().is_curly()) {
                rule->m_block = component_value.block();
                return rule;
            }
        }

        tokens.reconsume_current_input_token();
        auto value = consume_a_component_value(tokens);
        rule->m_prelude.append(value);
    }
}

template<typename T>
RefPtr<StyleRule> Parser::consume_a_qualified_rule(TokenStream<T>& tokens)
{
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

        if constexpr (IsSame<T, StyleComponentValueRule>) {
            StyleComponentValueRule const& component_value = token;
            if (component_value.is_block() && component_value.block().is_curly()) {
                rule->m_block = component_value.block();
                return rule;
            }
        }

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
    auto& token = tokens.next_token();

    if (token.is(Token::Type::OpenCurly) || token.is(Token::Type::OpenSquare) || token.is(Token::Type::OpenParen))
        return StyleComponentValueRule(consume_a_simple_block(tokens));

    if (token.is(Token::Type::Function))
        return StyleComponentValueRule(consume_a_function(tokens));

    return StyleComponentValueRule(token);
}

template<typename T>
NonnullRefPtr<StyleBlockRule> Parser::consume_a_simple_block(TokenStream<T>& tokens)
{
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

template<typename T>
NonnullRefPtr<StyleFunctionRule> Parser::consume_a_function(TokenStream<T>& tokens)
{
    auto name_ident = tokens.current_token();
    VERIFY(name_ident.is(Token::Type::Function));
    auto function = make_ref_counted<StyleFunctionRule>(((Token)name_ident).function());

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

// https://www.w3.org/TR/css-syntax-3/#consume-declaration
template<typename T>
Optional<StyleDeclarationRule> Parser::consume_a_declaration(TokenStream<T>& tokens)
{
    // Note: This algorithm assumes that the next input token has already been checked to
    // be an <ident-token>.

    // To consume a declaration:

    // Consume the next input token.
    tokens.skip_whitespace();
    auto start_position = tokens.position();
    auto& token = tokens.next_token();

    if (!token.is(Token::Type::Ident)) {
        tokens.rewind_to_position(start_position);
        return {};
    }

    // Create a new declaration with its name set to the value of the current input token
    // and its value initially set to the empty list.
    StyleDeclarationRule declaration;
    declaration.m_name = ((Token)token).ident();

    // 1. While the next input token is a <whitespace-token>, consume the next input token.
    tokens.skip_whitespace();

    // 2. If the next input token is anything other than a <colon-token>, this is a parse error.
    // Return nothing.
    auto& maybe_colon = tokens.peek_token();
    if (!maybe_colon.is(Token::Type::Colon)) {
        log_parse_error();
        tokens.rewind_to_position(start_position);
        return {};
    }
    // Otherwise, consume the next input token.
    tokens.next_token();

    // 3. While the next input token is a <whitespace-token>, consume the next input token.
    tokens.skip_whitespace();

    // 4. As long as the next input token is anything other than an <EOF-token>, consume a
    //    component value and append it to the declaration’s value.
    for (;;) {
        if (tokens.peek_token().is(Token::Type::EndOfFile)) {
            break;
        }
        declaration.m_values.append(consume_a_component_value(tokens));
    }

    // 5. If the last two non-<whitespace-token>s in the declaration’s value are a <delim-token>
    //    with the value "!" followed by an <ident-token> with a value that is an ASCII case-insensitive
    //    match for "important", remove them from the declaration’s value and set the declaration’s
    //    important flag to true.
    if (declaration.m_values.size() >= 2) {
        // Walk backwards from the end until we find "important"
        Optional<size_t> important_index;
        for (size_t i = declaration.m_values.size() - 1; i > 0; i--) {
            auto value = declaration.m_values[i];
            if (value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("important")) {
                important_index = i;
                break;
            }
            if (value.is(Token::Type::Whitespace))
                continue;
            break;
        }

        // Walk backwards from important until we find "!"
        if (important_index.has_value()) {
            Optional<size_t> bang_index;
            for (size_t i = important_index.value() - 1; i > 0; i--) {
                auto value = declaration.m_values[i];
                if (value.is(Token::Type::Delim) && value.token().delim() == "!"sv) {
                    bang_index = i;
                    break;
                }
                if (value.is(Token::Type::Whitespace))
                    continue;
                break;
            }

            if (bang_index.has_value()) {
                declaration.m_values.remove(important_index.value());
                declaration.m_values.remove(bang_index.value());
                declaration.m_important = true;
            }
        }
    }

    // 6. While the last token in the declaration’s value is a <whitespace-token>, remove that token.
    while (!declaration.m_values.is_empty()) {
        auto maybe_whitespace = declaration.m_values.last();
        if (!(maybe_whitespace.is(Token::Type::Whitespace))) {
            break;
        }
        declaration.m_values.take_last();
    }

    // 7. Return the declaration.
    return declaration;
}

template<typename T>
Vector<DeclarationOrAtRule> Parser::consume_a_list_of_declarations(TokenStream<T>& tokens)
{
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
            dbgln_if(CSS_PARSER_DEBUG, "Discarding token: '{}'", peek.to_debug_string());
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
    RefPtr<CSSRule> rule;

    tokens.skip_whitespace();

    auto& token = tokens.peek_token();

    if (token.is(Token::Type::EndOfFile)) {
        return {};
    } else if (token.is(Token::Type::AtKeyword)) {
        auto at_rule = consume_an_at_rule(m_token_stream);
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

RefPtr<PropertyOwningCSSStyleDeclaration> Parser::parse_as_list_of_declarations()
{
    return parse_a_list_of_declarations(m_token_stream);
}

template<typename T>
RefPtr<PropertyOwningCSSStyleDeclaration> Parser::parse_a_list_of_declarations(TokenStream<T>& tokens)
{
    auto declarations_and_at_rules = consume_a_list_of_declarations(tokens);

    Vector<StyleProperty> properties;
    HashMap<String, StyleProperty> custom_properties;

    for (auto& declaration_or_at_rule : declarations_and_at_rules) {
        if (declaration_or_at_rule.is_at_rule()) {
            dbgln_if(CSS_PARSER_DEBUG, "!!! CSS at-rule is not allowed here!");
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

    return PropertyOwningCSSStyleDeclaration::create(move(properties), move(custom_properties));
}

Optional<StyleComponentValueRule> Parser::parse_as_component_value()
{
    return parse_a_component_value(m_token_stream);
}

template<typename T>
Optional<StyleComponentValueRule> Parser::parse_a_component_value(TokenStream<T>& tokens)
{
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

Optional<AK::URL> Parser::parse_url_function(StyleComponentValueRule const& component_value, AllowedDataUrlType allowed_data_url_type)
{
    // FIXME: Handle list of media queries. https://www.w3.org/TR/css-cascade-3/#conditional-import
    // FIXME: Handle data: urls (RFC2397)

    auto convert_string_to_url = [&](StringView& url_string) -> Optional<AK::URL> {
        if (url_string.starts_with("data:", CaseSensitivity::CaseInsensitive)) {
            auto data_url = AK::URL(url_string);

            switch (allowed_data_url_type) {
            case AllowedDataUrlType::Image:
                if (data_url.data_mime_type().starts_with("image"sv, CaseSensitivity::CaseInsensitive))
                    return data_url;
                break;

            default:
                break;
            }

            return {};
        }

        return m_context.complete_url(url_string);
    };

    if (component_value.is(Token::Type::Url)) {
        auto url_string = component_value.token().url();
        return convert_string_to_url(url_string);
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
                return convert_string_to_url(url_string);
            }
            break;
        }
    }

    return {};
}

RefPtr<CSSRule> Parser::convert_to_rule(NonnullRefPtr<StyleRule> rule)
{
    if (rule->m_type == StyleRule::Type::At) {
        if (has_ignored_vendor_prefix(rule->m_name)) {
            return {};
        } else if (rule->m_name.equals_ignoring_case("media"sv)) {

            auto media_query_tokens = TokenStream { rule->prelude() };
            auto media_query_list = parse_a_media_query_list(media_query_tokens);

            auto child_tokens = TokenStream { rule->block().values() };
            auto parser_rules = consume_a_list_of_rules(child_tokens, false);
            NonnullRefPtrVector<CSSRule> child_rules;
            for (auto& raw_rule : parser_rules) {
                if (auto child_rule = convert_to_rule(raw_rule))
                    child_rules.append(*child_rule);
            }

            return CSSMediaRule::create(MediaList::create(move(media_query_list)), move(child_rules));

        } else if (rule->m_name.equals_ignoring_case("import"sv) && !rule->prelude().is_empty()) {

            Optional<AK::URL> url;
            for (auto& token : rule->prelude()) {
                if (token.is(Token::Type::Whitespace))
                    continue;

                if (token.is(Token::Type::String)) {
                    url = m_context.complete_url(token.token().string());
                } else {
                    url = parse_url_function(token);
                }

                // FIXME: Handle list of media queries. https://www.w3.org/TR/css-cascade-3/#conditional-import
                if (url.has_value())
                    break;
            }

            if (url.has_value())
                return CSSImportRule::create(url.value(), const_cast<DOM::Document&>(*m_context.document()));
            else
                dbgln_if(CSS_PARSER_DEBUG, "Unable to parse url from @import rule");

        } else if (rule->m_name.equals_ignoring_case("supports"sv)) {

            auto supports_tokens = TokenStream { rule->prelude() };
            auto supports = parse_a_supports(supports_tokens);
            if (!supports) {
                if constexpr (CSS_PARSER_DEBUG) {
                    dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @supports rule invalid; discarding.");
                    supports_tokens.dump_all_tokens();
                }
                return {};
            }

            auto child_tokens = TokenStream { rule->block().values() };
            auto parser_rules = consume_a_list_of_rules(child_tokens, false);
            NonnullRefPtrVector<CSSRule> child_rules;
            for (auto& raw_rule : parser_rules) {
                if (auto child_rule = convert_to_rule(raw_rule))
                    child_rules.append(*child_rule);
            }

            return CSSSupportsRule::create(supports.release_nonnull(), move(child_rules));

        } else {
            dbgln_if(CSS_PARSER_DEBUG, "Unrecognized CSS at-rule: @{}", rule->m_name);
        }

        // FIXME: More at rules!

    } else {
        auto prelude_stream = TokenStream(rule->m_prelude);
        auto selectors = parse_a_selector(prelude_stream);

        if (selectors.is_error()) {
            if (selectors.error() != ParsingResult::IncludesIgnoredVendorPrefix) {
                dbgln_if(CSS_PARSER_DEBUG, "CSSParser: style rule selectors invalid; discarding.");
                if constexpr (CSS_PARSER_DEBUG) {
                    prelude_stream.dump_all_tokens();
                }
            }
            return {};
        }

        if (selectors.value().is_empty()) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: empty selector; discarding.");
            return {};
        }

        auto declaration = convert_to_declaration(*rule->m_block);
        if (!declaration) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: style rule declaration invalid; discarding.");
            return {};
        }

        return CSSStyleRule::create(move(selectors.value()), move(*declaration));
    }

    return {};
}

RefPtr<PropertyOwningCSSStyleDeclaration> Parser::convert_to_declaration(NonnullRefPtr<StyleBlockRule> block)
{
    if (!block->is_curly())
        return {};

    auto stream = TokenStream(block->m_values);
    return parse_a_list_of_declarations(stream);
}

Optional<StyleProperty> Parser::convert_to_style_property(StyleDeclarationRule const& declaration)
{
    auto& property_name = declaration.m_name;
    auto property_id = property_id_from_string(property_name);

    if (property_id == PropertyID::Invalid) {
        if (property_name.starts_with("--")) {
            property_id = PropertyID::Custom;
        } else if (has_ignored_vendor_prefix(property_name)) {
            return {};
        } else if (!property_name.starts_with("-")) {
            dbgln_if(CSS_PARSER_DEBUG, "Unrecognized CSS property '{}'", property_name);
            return {};
        }
    }

    auto value_token_stream = TokenStream(declaration.m_values);
    auto value = parse_css_value(property_id, value_token_stream);
    if (value.is_error()) {
        if (value.error() != ParsingResult::IncludesIgnoredVendorPrefix) {
            dbgln_if(CSS_PARSER_DEBUG, "Unable to parse value for CSS property '{}'.", property_name);
            if constexpr (CSS_PARSER_DEBUG) {
                value_token_stream.dump_all_tokens();
            }
        }
        return {};
    }

    if (property_id == PropertyID::Custom) {
        return StyleProperty { declaration.m_important, property_id, value.release_value(), declaration.m_name };
    } else {
        return StyleProperty { declaration.m_important, property_id, value.release_value(), {} };
    }
}

RefPtr<StyleValue> Parser::parse_builtin_value(StyleComponentValueRule const& component_value)
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

RefPtr<StyleValue> Parser::parse_calculated_value(Vector<StyleComponentValueRule> const& component_values)
{
    auto calc_expression = parse_calc_expression(component_values);
    if (calc_expression == nullptr)
        return nullptr;

    auto calc_type = calc_expression->resolved_type();
    if (!calc_type.has_value()) {
        dbgln_if(CSS_PARSER_DEBUG, "calc() resolved as invalid!!!");
        return nullptr;
    }

    [[maybe_unused]] auto to_string = [](CalculatedStyleValue::ResolvedType type) {
        switch (type) {
        case CalculatedStyleValue::ResolvedType::Angle:
            return "Angle"sv;
        case CalculatedStyleValue::ResolvedType::Frequency:
            return "Frequency"sv;
        case CalculatedStyleValue::ResolvedType::Integer:
            return "Integer"sv;
        case CalculatedStyleValue::ResolvedType::Length:
            return "Length"sv;
        case CalculatedStyleValue::ResolvedType::Number:
            return "Number"sv;
        case CalculatedStyleValue::ResolvedType::Percentage:
            return "Percentage"sv;
        case CalculatedStyleValue::ResolvedType::Time:
            return "Time"sv;
        }
        VERIFY_NOT_REACHED();
    };
    dbgln_if(CSS_PARSER_DEBUG, "Deduced calc() resolved type as: {}", to_string(calc_type.value()));

    return CalculatedStyleValue::create(calc_expression.release_nonnull(), calc_type.release_value());
}

RefPtr<StyleValue> Parser::parse_dynamic_value(StyleComponentValueRule const& component_value)
{
    if (component_value.is_function()) {
        auto& function = component_value.function();

        if (function.name().equals_ignoring_case("calc"))
            return parse_calculated_value(function.values());

        if (function.name().equals_ignoring_case("var")) {
            // Declarations using `var()` should already be parsed as an UnresolvedStyleValue before this point.
            VERIFY_NOT_REACHED();
        }
    }

    return {};
}

Optional<Parser::Dimension> Parser::parse_dimension(StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Dimension)) {
        float numeric_value = component_value.token().dimension_value();
        auto unit_string = component_value.token().dimension_unit();
        Optional<Length::Type> length_type = Length::Type::Undefined;

        if (unit_string.equals_ignoring_case("px"sv)) {
            length_type = Length::Type::Px;
        } else if (unit_string.equals_ignoring_case("pt"sv)) {
            length_type = Length::Type::Pt;
        } else if (unit_string.equals_ignoring_case("pc"sv)) {
            length_type = Length::Type::Pc;
        } else if (unit_string.equals_ignoring_case("mm"sv)) {
            length_type = Length::Type::Mm;
        } else if (unit_string.equals_ignoring_case("rem"sv)) {
            length_type = Length::Type::Rem;
        } else if (unit_string.equals_ignoring_case("em"sv)) {
            length_type = Length::Type::Em;
        } else if (unit_string.equals_ignoring_case("ex"sv)) {
            length_type = Length::Type::Ex;
        } else if (unit_string.equals_ignoring_case("ch"sv)) {
            length_type = Length::Type::Ch;
        } else if (unit_string.equals_ignoring_case("vw"sv)) {
            length_type = Length::Type::Vw;
        } else if (unit_string.equals_ignoring_case("vh"sv)) {
            length_type = Length::Type::Vh;
        } else if (unit_string.equals_ignoring_case("vmax"sv)) {
            length_type = Length::Type::Vmax;
        } else if (unit_string.equals_ignoring_case("vmin"sv)) {
            length_type = Length::Type::Vmin;
        } else if (unit_string.equals_ignoring_case("cm"sv)) {
            length_type = Length::Type::Cm;
        } else if (unit_string.equals_ignoring_case("in"sv)) {
            length_type = Length::Type::In;
        } else if (unit_string.equals_ignoring_case("Q"sv)) {
            length_type = Length::Type::Q;
        } else if (unit_string.equals_ignoring_case("%"sv)) {
            // A number followed by `%` must always result in a Percentage token.
            VERIFY_NOT_REACHED();
        }

        if (length_type.has_value())
            return Length { numeric_value, length_type.value() };
    }

    if (component_value.is(Token::Type::Percentage))
        return Percentage { static_cast<float>(component_value.token().percentage()) };

    if (component_value.is(Token::Type::Number)) {
        float numeric_value = component_value.token().number_value();
        if (numeric_value == 0)
            return Length::make_px(0);
        if (m_context.in_quirks_mode() && property_has_quirk(m_context.current_property_id(), Quirk::UnitlessLength)) {
            // https://quirks.spec.whatwg.org/#quirky-length-value
            // FIXME: Disallow quirk when inside a CSS sub-expression (like `calc()`)
            // "The <quirky-length> value must not be supported in arguments to CSS expressions other than the rect()
            // expression, and must not be supported in the supports() static method of the CSS interface."
            return Length::make_px(numeric_value);
        }
    }

    return {};
}

Optional<Length> Parser::parse_length(StyleComponentValueRule const& component_value)
{
    auto dimension = parse_dimension(component_value);
    if (!dimension.has_value())
        return {};

    if (dimension->is_length())
        return dimension->length();

    // FIXME: auto isn't a length!
    if (component_value.is(Token::Type::Ident) && component_value.token().ident().equals_ignoring_case("auto"))
        return Length::make_auto();

    return {};
}

RefPtr<StyleValue> Parser::parse_dimension_value(StyleComponentValueRule const& component_value)
{
    // Numbers with no units can be lengths, in two situations:
    // 1) We're in quirks mode, and it's an integer.
    // 2) It's a 0.
    // We handle case 1 here. Case 2 is handled by NumericStyleValue pretending to be a LengthStyleValue if it is 0.

    if (component_value.is(Token::Type::Number) && !(m_context.in_quirks_mode() && property_has_quirk(m_context.current_property_id(), Quirk::UnitlessLength)))
        return {};

    if (component_value.is(Token::Type::Ident) && component_value.token().ident().equals_ignoring_case("auto"))
        return LengthStyleValue::create(Length::make_auto());

    auto dimension = parse_dimension(component_value);
    if (!dimension.has_value())
        return {};

    if (dimension->is_length())
        return LengthStyleValue::create(dimension->length());
    if (dimension->is_percentage())
        return PercentageStyleValue::create(dimension->percentage());
    VERIFY_NOT_REACHED();
}

RefPtr<StyleValue> Parser::parse_numeric_value(StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Number)) {
        auto number = component_value.token();
        if (number.number_type() == Token::NumberType::Integer) {
            return NumericStyleValue::create_integer(number.to_integer());
        } else {
            return NumericStyleValue::create_float(number.number_value());
        }
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_identifier_value(StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::Ident)) {
        auto value_id = value_id_from_string(component_value.token().ident());
        if (value_id != ValueID::Invalid)
            return IdentifierStyleValue::create(value_id);
    }

    return {};
}

Optional<Color> Parser::parse_color(StyleComponentValueRule const& component_value)
{
    // https://www.w3.org/TR/css-color-3/
    if (component_value.is(Token::Type::Ident)) {
        auto ident = component_value.token().ident();

        auto color = Color::from_string(ident);
        if (color.has_value())
            return color;

    } else if (component_value.is(Token::Type::Hash)) {
        auto color = Color::from_string(String::formatted("#{}", component_value.token().hash_value()));
        if (color.has_value())
            return color;
        return {};

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

                auto r = r_val.to_integer();
                auto g = g_val.to_integer();
                auto b = b_val.to_integer();
                if (AK::is_within_range<u8>(r) && AK::is_within_range<u8>(g) && AK::is_within_range<u8>(b))
                    return Color(r, g, b);

            } else if (r_val.is(Token::Type::Percentage)
                && g_val.is(Token::Type::Percentage)
                && b_val.is(Token::Type::Percentage)) {

                u8 r = clamp(lroundf(r_val.percentage() * 2.55), 0, 255);
                u8 g = clamp(lroundf(g_val.percentage() * 2.55), 0, 255);
                u8 b = clamp(lroundf(b_val.percentage() * 2.55), 0, 255);
                return Color(r, g, b);
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

                auto r = r_val.to_integer();
                auto g = g_val.to_integer();
                auto b = b_val.to_integer();
                auto a = clamp(lroundf(a_val.number_value() * 255.0), 0, 255);
                if (AK::is_within_range<u8>(r) && AK::is_within_range<u8>(g) && AK::is_within_range<u8>(b))
                    return Color(r, g, b, a);

            } else if (r_val.is(Token::Type::Percentage)
                && g_val.is(Token::Type::Percentage)
                && b_val.is(Token::Type::Percentage)
                && a_val.is(Token::Type::Number)) {

                auto r = r_val.percentage();
                auto g = g_val.percentage();
                auto b = b_val.percentage();
                auto a = a_val.number_value();

                u8 r_255 = clamp(lroundf(r * 2.55), 0, 255);
                u8 g_255 = clamp(lroundf(g * 2.55), 0, 255);
                u8 b_255 = clamp(lroundf(b * 2.55), 0, 255);
                u8 a_255 = clamp(lroundf(a * 255.0), 0, 255);
                return Color(r_255, g_255, b_255, a_255);
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

                auto h = h_val.number_value();
                auto s = s_val.percentage() / 100.0;
                auto l = l_val.percentage() / 100.0;
                return Color::from_hsl(h, s, l);
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

                auto h = h_val.number_value();
                auto s = s_val.percentage() / 100.0;
                auto l = l_val.percentage() / 100.0;
                auto a = a_val.number_value();
                return Color::from_hsla(h, s, l, a);
            }
        }
        return {};
    }

    // https://quirks.spec.whatwg.org/#the-hashless-hex-color-quirk
    if (m_context.in_quirks_mode() && property_has_quirk(m_context.current_property_id(), Quirk::HashlessHexColor)) {
        // The value of a quirky color is obtained from the possible component values using the following algorithm,
        // aborting on the first step that returns a value:

        // 1. Let cv be the component value.
        auto& cv = component_value;
        String serialization;
        // 2. If cv is a <number-token> or a <dimension-token>, follow these substeps:
        if (cv.is(Token::Type::Number) || cv.is(Token::Type::Dimension)) {
            // 1. If cv’s type flag is not "integer", return an error.
            //    This means that values that happen to use scientific notation, e.g., 5e5e5e, will fail to parse.
            if (cv.token().number_type() != Token::NumberType::Integer)
                return {};

            // 2. If cv’s value is less than zero, return an error.
            auto value = cv.is(Token::Type::Number) ? cv.token().to_integer() : cv.token().dimension_value_int();
            if (value < 0)
                return {};

            // 3. Let serialization be the serialization of cv’s value, as a base-ten integer using digits 0-9 (U+0030 to U+0039) in the shortest form possible.
            StringBuilder serialization_builder;
            serialization_builder.appendff("{}", value);

            // 4. If cv is a <dimension-token>, append the unit to serialization.
            if (cv.is(Token::Type::Dimension))
                serialization_builder.append(cv.token().dimension_unit());

            // 5. If serialization consists of fewer than six characters, prepend zeros (U+0030) so that it becomes six characters.
            serialization = serialization_builder.to_string();
            if (serialization_builder.length() < 6) {
                StringBuilder builder;
                for (size_t i = 0; i < (6 - serialization_builder.length()); i++)
                    builder.append('0');
                builder.append(serialization_builder.string_view());
                serialization = builder.to_string();
            }
        }
        // 3. Otherwise, cv is an <ident-token>; let serialization be cv’s value.
        else {
            if (!cv.is(Token::Type::Ident))
                return {};
            serialization = cv.token().ident();
        }

        // 4. If serialization does not consist of three or six characters, return an error.
        if (serialization.length() != 3 && serialization.length() != 6)
            return {};

        // 5. If serialization contains any characters not in the range [0-9A-Fa-f] (U+0030 to U+0039, U+0041 to U+0046, U+0061 to U+0066), return an error.
        for (auto c : serialization) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f')))
                return {};
        }

        // 6. Return the concatenation of "#" (U+0023) and serialization.
        String concatenation = String::formatted("#{}", serialization);
        return Color::from_string(concatenation);
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_color_value(StyleComponentValueRule const& component_value)
{
    auto color = parse_color(component_value);
    if (color.has_value())
        return ColorStyleValue::create(color.value());

    return {};
}

RefPtr<StyleValue> Parser::parse_string_value(StyleComponentValueRule const& component_value)
{
    if (component_value.is(Token::Type::String))
        return StringStyleValue::create(component_value.token().string());

    return {};
}

RefPtr<StyleValue> Parser::parse_image_value(StyleComponentValueRule const& component_value)
{
    auto url = parse_url_function(component_value, AllowedDataUrlType::Image);
    if (url.has_value())
        return ImageStyleValue::create(url.value());
    // FIXME: Handle gradients.

    return {};
}

template<typename ParseFunction>
RefPtr<StyleValue> Parser::parse_comma_separated_value_list(Vector<StyleComponentValueRule> const& component_values, ParseFunction parse_one_value)
{
    auto tokens = TokenStream { component_values };
    auto first = parse_one_value(tokens);
    if (!first || !tokens.has_next_token())
        return first;

    NonnullRefPtrVector<StyleValue> values;
    values.append(first.release_nonnull());

    while (tokens.has_next_token()) {
        if (!tokens.next_token().is(Token::Type::Comma))
            return {};

        if (auto maybe_value = parse_one_value(tokens)) {
            values.append(maybe_value.release_nonnull());
            continue;
        }
        return {};
    }

    return StyleValueList::create(move(values), StyleValueList::Separator::Comma);
}

RefPtr<StyleValue> Parser::parse_simple_comma_separated_value_list(Vector<StyleComponentValueRule> const& component_values)
{
    return parse_comma_separated_value_list(component_values, [=, this](auto& tokens) -> RefPtr<StyleValue> {
        auto& token = tokens.next_token();
        if (auto value = parse_css_value(token); value && property_accepts_value(m_context.current_property_id(), *value))
            return value;
        tokens.reconsume_current_input_token();
        return nullptr;
    });
}

RefPtr<StyleValue> Parser::parse_background_value(Vector<StyleComponentValueRule> const& component_values)
{
    NonnullRefPtrVector<StyleValue> background_images;
    NonnullRefPtrVector<StyleValue> background_positions;
    NonnullRefPtrVector<StyleValue> background_sizes;
    NonnullRefPtrVector<StyleValue> background_repeats;
    NonnullRefPtrVector<StyleValue> background_attachments;
    NonnullRefPtrVector<StyleValue> background_clips;
    NonnullRefPtrVector<StyleValue> background_origins;
    RefPtr<StyleValue> background_color;

    // Per-layer values
    RefPtr<StyleValue> background_image;
    RefPtr<StyleValue> background_position;
    RefPtr<StyleValue> background_size;
    RefPtr<StyleValue> background_repeat;
    RefPtr<StyleValue> background_attachment;
    RefPtr<StyleValue> background_clip;
    RefPtr<StyleValue> background_origin;

    bool has_multiple_layers = false;

    auto background_layer_is_valid = [&](bool allow_background_color) -> bool {
        if (allow_background_color) {
            if (background_color)
                return true;
        } else {
            if (background_color)
                return false;
        }
        return background_image || background_position || background_size || background_repeat || background_attachment || background_clip || background_origin;
    };

    auto complete_background_layer = [&]() {
        background_images.append(background_image ? background_image.release_nonnull() : property_initial_value(PropertyID::BackgroundImage));
        background_positions.append(background_position ? background_position.release_nonnull() : property_initial_value(PropertyID::BackgroundPosition));
        background_sizes.append(background_size ? background_size.release_nonnull() : property_initial_value(PropertyID::BackgroundSize));
        background_repeats.append(background_repeat ? background_repeat.release_nonnull() : property_initial_value(PropertyID::BackgroundRepeat));
        background_attachments.append(background_attachment ? background_attachment.release_nonnull() : property_initial_value(PropertyID::BackgroundAttachment));

        if (!background_origin && !background_clip) {
            background_origin = property_initial_value(PropertyID::BackgroundOrigin);
            background_clip = property_initial_value(PropertyID::BackgroundClip);
        } else if (!background_clip) {
            background_clip = background_origin;
        }
        background_origins.append(background_origin.release_nonnull());
        background_clips.append(background_clip.release_nonnull());

        background_image = nullptr;
        background_position = nullptr;
        background_size = nullptr;
        background_repeat = nullptr;
        background_attachment = nullptr;
        background_clip = nullptr;
        background_origin = nullptr;
    };

    auto tokens = TokenStream { component_values };
    while (tokens.has_next_token()) {
        auto& part = tokens.next_token();

        if (part.is(Token::Type::Comma)) {
            has_multiple_layers = true;
            if (!background_layer_is_valid(false))
                return nullptr;
            complete_background_layer();
            continue;
        }

        auto value = parse_css_value(part);
        if (!value)
            return nullptr;

        if (property_accepts_value(PropertyID::BackgroundAttachment, *value)) {
            if (background_attachment)
                return nullptr;
            background_attachment = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::BackgroundColor, *value)) {
            if (background_color)
                return nullptr;
            background_color = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::BackgroundImage, *value)) {
            if (background_image)
                return nullptr;
            background_image = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::BackgroundOrigin, *value)) {
            // background-origin and background-clip accept the same values. From the spec:
            //   "If one <box> value is present then it sets both background-origin and background-clip to that value.
            //    If two values are present, then the first sets background-origin and the second background-clip."
            //        - https://www.w3.org/TR/css-backgrounds-3/#background
            // So, we put the first one in background-origin, then if we get a second, we put it in background-clip.
            // If we only get one, we copy the value before creating the BackgroundStyleValue.
            if (!background_origin) {
                background_origin = value.release_nonnull();
                continue;
            }
            if (!background_clip) {
                background_clip = value.release_nonnull();
                continue;
            }
            return nullptr;
        }
        if (property_accepts_value(PropertyID::BackgroundPosition, *value)) {
            if (background_position)
                return nullptr;
            tokens.reconsume_current_input_token();
            if (auto maybe_background_position = parse_single_background_position_value(tokens)) {
                background_position = maybe_background_position.release_nonnull();

                // Attempt to parse `/ <background-size>`
                auto before_slash = tokens.position();
                auto& maybe_slash = tokens.next_token();
                if (maybe_slash.is(Token::Type::Delim) && maybe_slash.token().delim() == "/"sv) {
                    if (auto maybe_background_size = parse_single_background_size_value(tokens)) {
                        background_size = maybe_background_size.release_nonnull();
                        continue;
                    }
                    return nullptr;
                }

                tokens.rewind_to_position(before_slash);
                continue;
            }
            return nullptr;
        }
        if (property_accepts_value(PropertyID::BackgroundRepeat, *value)) {
            if (background_repeat)
                return nullptr;
            tokens.reconsume_current_input_token();
            if (auto maybe_repeat = parse_single_background_repeat_value(tokens)) {
                background_repeat = maybe_repeat.release_nonnull();
                continue;
            }
            return nullptr;
        }

        return nullptr;
    }

    if (!background_layer_is_valid(true))
        return nullptr;

    // We only need to create StyleValueLists if there are multiple layers.
    // Otherwise, we can pass the single StyleValues directly.
    if (has_multiple_layers) {
        complete_background_layer();

        if (!background_color)
            background_color = property_initial_value(PropertyID::BackgroundColor);
        return BackgroundStyleValue::create(
            background_color.release_nonnull(),
            StyleValueList::create(move(background_images), StyleValueList::Separator::Comma),
            StyleValueList::create(move(background_positions), StyleValueList::Separator::Comma),
            StyleValueList::create(move(background_sizes), StyleValueList::Separator::Comma),
            StyleValueList::create(move(background_repeats), StyleValueList::Separator::Comma),
            StyleValueList::create(move(background_attachments), StyleValueList::Separator::Comma),
            StyleValueList::create(move(background_origins), StyleValueList::Separator::Comma),
            StyleValueList::create(move(background_clips), StyleValueList::Separator::Comma));
    }

    if (!background_color)
        background_color = property_initial_value(PropertyID::BackgroundColor);
    if (!background_image)
        background_image = property_initial_value(PropertyID::BackgroundImage);
    if (!background_position)
        background_position = property_initial_value(PropertyID::BackgroundPosition);
    if (!background_size)
        background_size = property_initial_value(PropertyID::BackgroundSize);
    if (!background_repeat)
        background_repeat = property_initial_value(PropertyID::BackgroundRepeat);
    if (!background_attachment)
        background_attachment = property_initial_value(PropertyID::BackgroundAttachment);

    if (!background_origin && !background_clip) {
        background_origin = property_initial_value(PropertyID::BackgroundOrigin);
        background_clip = property_initial_value(PropertyID::BackgroundClip);
    } else if (!background_clip) {
        background_clip = background_origin;
    }

    return BackgroundStyleValue::create(
        background_color.release_nonnull(),
        background_image.release_nonnull(),
        background_position.release_nonnull(),
        background_size.release_nonnull(),
        background_repeat.release_nonnull(),
        background_attachment.release_nonnull(),
        background_origin.release_nonnull(),
        background_clip.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_single_background_position_value(TokenStream<StyleComponentValueRule>& tokens)
{
    // NOTE: This *looks* like it parses a <position>, but it doesn't. From the spec:
    //      "Note: The background-position property also accepts a three-value syntax.
    //       This has been disallowed generically because it creates parsing ambiguities
    //       when combined with other length or percentage components in a property value."
    //           - https://www.w3.org/TR/css-values-4/#typedef-position
    //       So, we'll need a separate function to parse <position> later.

    auto start_position = tokens.position();
    auto error = [&]() {
        tokens.rewind_to_position(start_position);
        return nullptr;
    };

    auto to_edge = [](ValueID identifier) -> Optional<PositionEdge> {
        switch (identifier) {
        case ValueID::Top:
            return PositionEdge::Top;
        case ValueID::Bottom:
            return PositionEdge::Bottom;
        case ValueID::Left:
            return PositionEdge::Left;
        case ValueID::Right:
            return PositionEdge::Right;
        default:
            return {};
        }
    };
    auto is_horizontal = [](ValueID identifier) -> bool {
        switch (identifier) {
        case ValueID::Left:
        case ValueID::Right:
            return true;
        default:
            return false;
        }
    };
    auto is_vertical = [](ValueID identifier) -> bool {
        switch (identifier) {
        case ValueID::Top:
        case ValueID::Bottom:
            return true;
        default:
            return false;
        }
    };

    LengthPercentage zero_offset = Length::make_px(0);
    LengthPercentage center_offset = Percentage { 50 };

    struct EdgeOffset {
        PositionEdge edge;
        LengthPercentage offset;
        bool edge_provided;
        bool offset_provided;
    };

    Optional<EdgeOffset> horizontal;
    Optional<EdgeOffset> vertical;
    bool found_center = false;

    while (tokens.has_next_token()) {
        // Check if we're done
        auto seen_items = (horizontal.has_value() ? 1 : 0) + (vertical.has_value() ? 1 : 0) + (found_center ? 1 : 0);
        if (seen_items == 2)
            break;

        auto& token = tokens.peek_token();
        auto maybe_value = parse_css_value(token);
        if (!maybe_value || !property_accepts_value(PropertyID::BackgroundPosition, *maybe_value))
            break;
        tokens.next_token();
        auto value = maybe_value.release_nonnull();

        if (value->is_percentage()) {
            if (!horizontal.has_value()) {
                horizontal = EdgeOffset { PositionEdge::Left, value->as_percentage().percentage(), false, true };
            } else if (!vertical.has_value()) {
                vertical = EdgeOffset { PositionEdge::Top, value->as_percentage().percentage(), false, true };
            } else {
                return error();
            }
            continue;
        }

        if (value->has_length()) {
            if (!horizontal.has_value()) {
                horizontal = EdgeOffset { PositionEdge::Left, value->to_length(), false, true };
            } else if (!vertical.has_value()) {
                vertical = EdgeOffset { PositionEdge::Top, value->to_length(), false, true };
            } else {
                return error();
            }
            continue;
        }

        if (value->has_identifier()) {
            auto identifier = value->to_identifier();
            if (is_horizontal(identifier)) {
                LengthPercentage offset = zero_offset;
                bool offset_provided = false;
                if (tokens.has_next_token()) {
                    auto maybe_offset = parse_dimension(tokens.peek_token());
                    if (maybe_offset.has_value() && maybe_offset.value().is_length_percentage()) {
                        offset = maybe_offset.value().length_percentage();
                        offset_provided = true;
                        tokens.next_token();
                    }
                }
                horizontal = EdgeOffset { *to_edge(identifier), offset, true, offset_provided };
            } else if (is_vertical(identifier)) {
                LengthPercentage offset = zero_offset;
                bool offset_provided = false;
                if (tokens.has_next_token()) {
                    auto maybe_offset = parse_dimension(tokens.peek_token());
                    if (maybe_offset.has_value() && maybe_offset.value().is_length_percentage()) {
                        offset = maybe_offset.value().length_percentage();
                        offset_provided = true;
                        tokens.next_token();
                    }
                }
                vertical = EdgeOffset { *to_edge(identifier), offset, true, offset_provided };
            } else if (identifier == ValueID::Center) {
                found_center = true;
            } else {
                return error();
            }
            continue;
        }

        tokens.reconsume_current_input_token();
        break;
    }

    if (found_center) {
        if (horizontal.has_value() && vertical.has_value())
            return error();
        if (!horizontal.has_value())
            horizontal = EdgeOffset { PositionEdge::Left, center_offset, true, false };
        if (!vertical.has_value())
            vertical = EdgeOffset { PositionEdge::Top, center_offset, true, false };
    }

    if (!horizontal.has_value() && !vertical.has_value())
        return error();

    // Unpack `<edge> <length>`:
    // The loop above reads this pattern as a single EdgeOffset, when actually, it should be treated
    // as `x y` if the edge is horizontal, and `y` (with the second token reconsumed) otherwise.
    if (!vertical.has_value() && horizontal->edge_provided && horizontal->offset_provided) {
        // Split into `x y`
        vertical = EdgeOffset { PositionEdge::Top, horizontal->offset, false, true };
        horizontal->offset = zero_offset;
        horizontal->offset_provided = false;
    } else if (!horizontal.has_value() && vertical->edge_provided && vertical->offset_provided) {
        // `y`, reconsume
        vertical->offset = zero_offset;
        vertical->offset_provided = false;
        tokens.reconsume_current_input_token();
    }

    // If only one value is specified, the second value is assumed to be center.
    if (!horizontal.has_value())
        horizontal = EdgeOffset { PositionEdge::Left, center_offset, false, false };
    if (!vertical.has_value())
        vertical = EdgeOffset { PositionEdge::Top, center_offset, false, false };

    return PositionStyleValue::create(
        horizontal->edge, horizontal->offset,
        vertical->edge, vertical->offset);
}

RefPtr<StyleValue> Parser::parse_single_background_repeat_value(TokenStream<StyleComponentValueRule>& tokens)
{
    auto start_position = tokens.position();
    auto error = [&]() {
        tokens.rewind_to_position(start_position);
        return nullptr;
    };

    auto is_directional_repeat = [](StyleValue const& value) -> bool {
        auto value_id = value.to_identifier();
        return value_id == ValueID::RepeatX || value_id == ValueID::RepeatY;
    };

    auto as_repeat = [](ValueID identifier) {
        switch (identifier) {
        case ValueID::NoRepeat:
            return Repeat::NoRepeat;
        case ValueID::Repeat:
            return Repeat::Repeat;
        case ValueID::Round:
            return Repeat::Round;
        case ValueID::Space:
            return Repeat::Space;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    auto& token = tokens.next_token();
    auto maybe_x_value = parse_css_value(token);
    if (!maybe_x_value || !property_accepts_value(PropertyID::BackgroundRepeat, *maybe_x_value))
        return error();
    auto x_value = maybe_x_value.release_nonnull();

    if (is_directional_repeat(*x_value)) {
        auto value_id = x_value->to_identifier();
        return BackgroundRepeatStyleValue::create(
            value_id == ValueID::RepeatX ? Repeat::Repeat : Repeat::NoRepeat,
            value_id == ValueID::RepeatX ? Repeat::NoRepeat : Repeat::Repeat);
    }

    // See if we have a second value for Y
    auto& second_token = tokens.peek_token();
    auto maybe_y_value = parse_css_value(second_token);
    if (!maybe_y_value || !property_accepts_value(PropertyID::BackgroundRepeat, *maybe_y_value)) {
        // We don't have a second value, so use x for both
        return BackgroundRepeatStyleValue::create(as_repeat(x_value->to_identifier()), as_repeat(x_value->to_identifier()));
    }
    tokens.next_token();
    auto y_value = maybe_y_value.release_nonnull();
    if (is_directional_repeat(*y_value))
        return error();
    return BackgroundRepeatStyleValue::create(as_repeat(x_value->to_identifier()), as_repeat(y_value->to_identifier()));
}

RefPtr<StyleValue> Parser::parse_single_background_size_value(TokenStream<StyleComponentValueRule>& tokens)
{
    auto start_position = tokens.position();
    auto error = [&]() {
        tokens.rewind_to_position(start_position);
        return nullptr;
    };

    auto get_length_percentage = [](StyleValue& style_value) -> Optional<LengthPercentage> {
        if (style_value.is_percentage())
            return LengthPercentage { style_value.as_percentage().percentage() };
        if (style_value.has_length())
            return LengthPercentage { style_value.to_length() };
        return {};
    };

    auto maybe_x_value = parse_css_value(tokens.next_token());
    if (!maybe_x_value || !property_accepts_value(PropertyID::BackgroundSize, *maybe_x_value))
        return error();
    auto x_value = maybe_x_value.release_nonnull();

    if (x_value->to_identifier() == ValueID::Cover || x_value->to_identifier() == ValueID::Contain)
        return x_value;

    auto maybe_y_value = parse_css_value(tokens.peek_token());
    if (!maybe_y_value || !property_accepts_value(PropertyID::BackgroundSize, *maybe_y_value)) {
        auto x_size = get_length_percentage(*x_value);
        if (!x_size.has_value())
            return error();
        return BackgroundSizeStyleValue::create(x_size.value(), x_size.value());
    }

    tokens.next_token();
    auto y_value = maybe_y_value.release_nonnull();
    auto x_size = get_length_percentage(*x_value);
    auto y_size = get_length_percentage(*y_value);

    if (x_size.has_value() && y_size.has_value())
        return BackgroundSizeStyleValue::create(x_size.release_value(), y_size.release_value());

    return error();
}

RefPtr<StyleValue> Parser::parse_border_value(Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> border_width;
    RefPtr<StyleValue> border_color;
    RefPtr<StyleValue> border_style;

    for (auto& part : component_values) {
        auto value = parse_css_value(part);
        if (!value)
            return nullptr;

        if (property_accepts_value(PropertyID::BorderWidth, *value)) {
            if (border_width)
                return nullptr;
            border_width = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::BorderColor, *value)) {
            if (border_color)
                return nullptr;
            border_color = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::BorderStyle, *value)) {
            if (border_style)
                return nullptr;
            border_style = value.release_nonnull();
            continue;
        }

        return nullptr;
    }

    if (!border_width)
        border_width = property_initial_value(PropertyID::BorderWidth);
    if (!border_style)
        border_style = property_initial_value(PropertyID::BorderStyle);
    if (!border_color)
        border_color = property_initial_value(PropertyID::BorderColor);

    return BorderStyleValue::create(border_width.release_nonnull(), border_style.release_nonnull(), border_color.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_border_radius_value(Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() == 2) {
        auto horizontal = parse_dimension(component_values[0]);
        auto vertical = parse_dimension(component_values[1]);
        if (horizontal.has_value() && horizontal->is_length_percentage() && vertical.has_value() && vertical->is_length_percentage())
            return BorderRadiusStyleValue::create(horizontal->length_percentage(), vertical->length_percentage());

        return nullptr;
    }

    if (component_values.size() == 1) {
        auto radius = parse_dimension(component_values[0]);
        if (radius.has_value() && radius->is_length_percentage())
            return BorderRadiusStyleValue::create(radius->length_percentage(), radius->length_percentage());
        return nullptr;
    }

    return nullptr;
}

RefPtr<StyleValue> Parser::parse_border_radius_shorthand_value(Vector<StyleComponentValueRule> const& component_values)
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

        auto maybe_length = parse_length(value);
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

    return StyleValueList::create(move(border_radii), StyleValueList::Separator::Space);
}

RefPtr<StyleValue> Parser::parse_box_shadow_value(Vector<StyleComponentValueRule> const& component_values)
{
    // "none"
    if (component_values.size() == 1 && component_values.first().is(Token::Type::Ident)) {
        auto ident = parse_identifier_value(component_values.first());
        if (ident && ident->to_identifier() == ValueID::None)
            return ident;
    }

    // FIXME: Also support inset, spread-radius and multiple comma-separated box-shadows
    Length offset_x {};
    Length offset_y {};
    Length blur_radius {};
    Color color {};

    if (component_values.size() < 3 || component_values.size() > 4)
        return nullptr;

    auto maybe_x = parse_length(component_values[0]);
    if (!maybe_x.has_value())
        return nullptr;
    offset_x = maybe_x.value();

    auto maybe_y = parse_length(component_values[1]);
    if (!maybe_y.has_value())
        return nullptr;
    offset_y = maybe_y.value();

    if (component_values.size() == 3) {
        auto parsed_color = parse_color(component_values[2]);
        if (!parsed_color.has_value())
            return nullptr;
        color = parsed_color.value();
    } else if (component_values.size() == 4) {
        auto maybe_blur_radius = parse_length(component_values[2]);
        if (!maybe_blur_radius.has_value())
            return nullptr;
        blur_radius = maybe_blur_radius.value();

        auto parsed_color = parse_color(component_values[3]);
        if (!parsed_color.has_value())
            return nullptr;
        color = parsed_color.value();
    }

    return BoxShadowStyleValue::create(offset_x, offset_y, blur_radius, color);
}

RefPtr<StyleValue> Parser::parse_flex_value(Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() == 1) {
        auto value = parse_css_value(component_values[0]);
        if (!value)
            return nullptr;

        switch (value->to_identifier()) {
        case ValueID::Auto: {
            auto one = NumericStyleValue::create_integer(1);
            return FlexStyleValue::create(one, one, IdentifierStyleValue::create(ValueID::Auto));
        }
        case ValueID::None: {
            auto zero = NumericStyleValue::create_integer(0);
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
        auto value = parse_css_value(component_values[i]);
        if (!value)
            return nullptr;

        // Zero is a valid value for basis, but only if grow and shrink are already specified.
        if (value->has_number() && value->to_number() == 0) {
            if (flex_grow && flex_shrink && !flex_basis) {
                flex_basis = LengthStyleValue::create(Length(0, Length::Type::Px));
                continue;
            }
        }

        if (property_accepts_value(PropertyID::FlexGrow, *value)) {
            if (flex_grow)
                return nullptr;
            flex_grow = value.release_nonnull();

            // Flex-shrink may optionally follow directly after.
            if (i + 1 < component_values.size()) {
                auto second_value = parse_css_value(component_values[i + 1]);
                if (second_value && property_accepts_value(PropertyID::FlexShrink, *second_value)) {
                    flex_shrink = second_value.release_nonnull();
                    i++;
                }
            }
            continue;
        }

        if (property_accepts_value(PropertyID::FlexBasis, *value)) {
            if (flex_basis)
                return nullptr;
            flex_basis = value.release_nonnull();
            continue;
        }

        return nullptr;
    }

    if (!flex_grow)
        flex_grow = property_initial_value(PropertyID::FlexGrow);
    if (!flex_shrink)
        flex_shrink = property_initial_value(PropertyID::FlexShrink);
    if (!flex_basis)
        flex_basis = property_initial_value(PropertyID::FlexBasis);

    return FlexStyleValue::create(flex_grow.release_nonnull(), flex_shrink.release_nonnull(), flex_basis.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_flex_flow_value(Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() > 2)
        return nullptr;

    RefPtr<StyleValue> flex_direction;
    RefPtr<StyleValue> flex_wrap;

    for (auto& part : component_values) {
        auto value = parse_css_value(part);
        if (!value)
            return nullptr;
        if (property_accepts_value(PropertyID::FlexDirection, *value)) {
            if (flex_direction)
                return nullptr;
            flex_direction = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::FlexWrap, *value)) {
            if (flex_wrap)
                return nullptr;
            flex_wrap = value.release_nonnull();
            continue;
        }
    }

    if (!flex_direction)
        flex_direction = property_initial_value(PropertyID::FlexDirection);
    if (!flex_wrap)
        flex_wrap = property_initial_value(PropertyID::FlexWrap);

    return FlexFlowStyleValue::create(flex_direction.release_nonnull(), flex_wrap.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_font_value(Vector<StyleComponentValueRule> const& component_values)
{
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
        auto value = parse_css_value(component_values[i]);
        if (!value)
            return nullptr;

        if (value->to_identifier() == ValueID::Normal) {
            normal_count++;
            continue;
        }
        // FIXME: Handle angle parameter to `oblique`: https://www.w3.org/TR/css-fonts-4/#font-style-prop
        if (property_accepts_value(PropertyID::FontStyle, *value)) {
            if (font_style)
                return nullptr;
            font_style = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::FontWeight, *value)) {
            if (font_weight)
                return nullptr;
            font_weight = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::FontSize, *value)) {
            if (font_size)
                return nullptr;
            font_size = value.release_nonnull();

            // Consume `/ line-height` if present
            if (i + 2 < component_values.size()) {
                auto maybe_solidus = component_values[i + 1];
                if (maybe_solidus.is(Token::Type::Delim) && maybe_solidus.token().delim() == "/"sv) {
                    auto maybe_line_height = parse_css_value(component_values[i + 2]);
                    if (!(maybe_line_height && property_accepts_value(PropertyID::LineHeight, *maybe_line_height)))
                        return nullptr;
                    line_height = maybe_line_height.release_nonnull();
                    i += 2;
                }
            }

            // Consume font-families
            auto maybe_font_families = parse_font_family_value(component_values, i + 1);
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
        font_style = property_initial_value(PropertyID::FontStyle);
    if (!font_weight)
        font_weight = property_initial_value(PropertyID::FontWeight);
    if (!line_height)
        line_height = property_initial_value(PropertyID::LineHeight);

    return FontStyleValue::create(font_style.release_nonnull(), font_weight.release_nonnull(), font_size.release_nonnull(), line_height.release_nonnull(), font_families.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_font_family_value(Vector<StyleComponentValueRule> const& component_values, size_t start_index)
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
            auto maybe_ident = parse_css_value(part);
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
    return StyleValueList::create(move(font_families), StyleValueList::Separator::Comma);
}

RefPtr<StyleValue> Parser::parse_list_style_value(Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> list_position;
    RefPtr<StyleValue> list_image;
    RefPtr<StyleValue> list_type;
    int found_nones = 0;

    for (auto& part : component_values) {
        auto value = parse_css_value(part);
        if (!value)
            return nullptr;

        if (value->to_identifier() == ValueID::None) {
            found_nones++;
            continue;
        }

        if (property_accepts_value(PropertyID::ListStylePosition, *value)) {
            if (list_position)
                return nullptr;
            list_position = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::ListStyleImage, *value)) {
            if (list_image)
                return nullptr;
            list_image = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::ListStyleType, *value)) {
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
        list_position = property_initial_value(PropertyID::ListStylePosition);
    if (!list_image)
        list_image = property_initial_value(PropertyID::ListStyleImage);
    if (!list_type)
        list_type = property_initial_value(PropertyID::ListStyleType);

    return ListStyleStyleValue::create(list_position.release_nonnull(), list_image.release_nonnull(), list_type.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_overflow_value(Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() == 1) {
        auto maybe_value = parse_css_value(component_values.first());
        if (!maybe_value)
            return nullptr;
        auto value = maybe_value.release_nonnull();
        if (property_accepts_value(PropertyID::Overflow, *value))
            return OverflowStyleValue::create(value, value);
        return nullptr;
    }

    if (component_values.size() == 2) {
        auto maybe_x_value = parse_css_value(component_values[0]);
        auto maybe_y_value = parse_css_value(component_values[1]);

        if (!maybe_x_value || !maybe_y_value)
            return nullptr;
        auto x_value = maybe_x_value.release_nonnull();
        auto y_value = maybe_y_value.release_nonnull();
        if (!property_accepts_value(PropertyID::OverflowX, x_value) || !property_accepts_value(PropertyID::OverflowY, y_value)) {
            return nullptr;
        }
        return OverflowStyleValue::create(x_value, y_value);
    }

    return nullptr;
}

RefPtr<StyleValue> Parser::parse_text_decoration_value(Vector<StyleComponentValueRule> const& component_values)
{
    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> decoration_line;
    RefPtr<StyleValue> decoration_style;
    RefPtr<StyleValue> decoration_color;
    // FIXME: Implement 'text-decoration-thickness' parameter. https://www.w3.org/TR/css-text-decor-4/#text-decoration-width-property

    for (auto& part : component_values) {
        auto value = parse_css_value(part);
        if (!value)
            return nullptr;

        if (property_accepts_value(PropertyID::TextDecorationColor, *value)) {
            if (decoration_color)
                return nullptr;
            decoration_color = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::TextDecorationLine, *value)) {
            if (decoration_line)
                return nullptr;
            decoration_line = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::TextDecorationStyle, *value)) {
            if (decoration_style)
                return nullptr;
            decoration_style = value.release_nonnull();
            continue;
        }

        return nullptr;
    }

    if (!decoration_line)
        decoration_line = property_initial_value(PropertyID::TextDecorationLine);
    if (!decoration_style)
        decoration_style = property_initial_value(PropertyID::TextDecorationStyle);
    if (!decoration_color)
        decoration_color = property_initial_value(PropertyID::TextDecorationColor);

    return TextDecorationStyleValue::create(decoration_line.release_nonnull(), decoration_style.release_nonnull(), decoration_color.release_nonnull());
}

static Optional<CSS::TransformFunction> parse_transform_function_name(StringView name)
{
    if (name == "translateY")
        return CSS::TransformFunction::TranslateY;
    return {};
}

RefPtr<StyleValue> Parser::parse_transform_value(Vector<StyleComponentValueRule> const& component_values)
{
    NonnullRefPtrVector<StyleValue> transformations;

    for (auto& part : component_values) {
        if (part.is(Token::Type::Ident) && part.token().ident().equals_ignoring_case("none")) {
            if (!transformations.is_empty())
                return nullptr;
            return IdentifierStyleValue::create(ValueID::None);
        }

        if (!part.is_function())
            return nullptr;
        auto maybe_function = parse_transform_function_name(part.function().name());
        if (!maybe_function.has_value())
            return nullptr;

        NonnullRefPtrVector<StyleValue> values;
        for (auto& value : part.function().values()) {
            if (value.is(Token::Type::Dimension)) {
                auto maybe_length = parse_length(value);
                if (!maybe_length.has_value())
                    return nullptr;
                values.append(LengthStyleValue::create(maybe_length.release_value()));
            } else if (value.is(Token::Type::Number)) {
                auto number = parse_numeric_value(value);
                values.append(number.release_nonnull());
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "FIXME: Unsupported value type for transformation!");
                return nullptr;
            }
        }

        transformations.append(TransformationStyleValue::create(maybe_function.value(), move(values)));
    }
    return StyleValueList::create(move(transformations), StyleValueList::Separator::Space);
}

RefPtr<StyleValue> Parser::parse_as_css_value(PropertyID property_id)
{
    auto component_values = parse_as_list_of_component_values();
    auto tokens = TokenStream(component_values);
    auto parsed_value = parse_css_value(property_id, tokens);
    if (parsed_value.is_error())
        return {};
    return parsed_value.release_value();
}

Result<NonnullRefPtr<StyleValue>, Parser::ParsingResult> Parser::parse_css_value(PropertyID property_id, TokenStream<StyleComponentValueRule>& tokens)
{
    auto block_contains_var = [](StyleBlockRule const& block, auto&& recurse) -> bool {
        for (auto const& token : block.values()) {
            if (token.is_function() && token.function().name().equals_ignoring_case("var"sv))
                return true;
            if (token.is_block() && recurse(token.block(), recurse))
                return true;
        }
        return false;
    };

    m_context.set_current_property_id(property_id);
    Vector<StyleComponentValueRule> component_values;
    bool contains_var = false;

    while (tokens.has_next_token()) {
        auto& token = tokens.next_token();

        if (token.is(Token::Type::Semicolon)) {
            tokens.reconsume_current_input_token();
            break;
        }

        if (property_id != PropertyID::Custom) {
            if (token.is(Token::Type::Whitespace))
                continue;

            if (token.is(Token::Type::Ident) && has_ignored_vendor_prefix(token.token().ident()))
                return ParsingResult::IncludesIgnoredVendorPrefix;
        }

        if (!contains_var) {
            if (token.is_function() && token.function().name().equals_ignoring_case("var"sv))
                contains_var = true;
            else if (token.is_block() && block_contains_var(token.block(), block_contains_var))
                contains_var = true;
        }

        component_values.append(token);
    }

    if (property_id == PropertyID::Custom || contains_var)
        return { UnresolvedStyleValue::create(move(component_values), contains_var) };

    if (component_values.is_empty())
        return ParsingResult::SyntaxError;

    if (component_values.size() == 1) {
        if (auto parsed_value = parse_builtin_value(component_values.first()))
            return parsed_value.release_nonnull();
    }

    // Special-case property handling
    switch (property_id) {
    case PropertyID::Background:
        if (auto parsed_value = parse_background_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::BackgroundAttachment:
    case PropertyID::BackgroundClip:
    case PropertyID::BackgroundImage:
    case PropertyID::BackgroundOrigin:
        if (auto parsed_value = parse_simple_comma_separated_value_list(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::BackgroundPosition:
        if (auto parsed_value = parse_comma_separated_value_list(component_values, [this](auto& tokens) { return parse_single_background_position_value(tokens); }))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::BackgroundRepeat:
        if (auto parsed_value = parse_comma_separated_value_list(component_values, [this](auto& tokens) { return parse_single_background_repeat_value(tokens); }))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::BackgroundSize:
        if (auto parsed_value = parse_comma_separated_value_list(component_values, [this](auto& tokens) { return parse_single_background_size_value(tokens); }))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::Border:
    case PropertyID::BorderBottom:
    case PropertyID::BorderLeft:
    case PropertyID::BorderRight:
    case PropertyID::BorderTop:
        if (auto parsed_value = parse_border_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::BorderTopLeftRadius:
    case PropertyID::BorderTopRightRadius:
    case PropertyID::BorderBottomRightRadius:
    case PropertyID::BorderBottomLeftRadius:
        if (auto parsed_value = parse_border_radius_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::BorderRadius:
        if (auto parsed_value = parse_border_radius_shorthand_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::BoxShadow:
        if (auto parsed_value = parse_box_shadow_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::Flex:
        if (auto parsed_value = parse_flex_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::FlexFlow:
        if (auto parsed_value = parse_flex_flow_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::Font:
        if (auto parsed_value = parse_font_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::FontFamily:
        if (auto parsed_value = parse_font_family_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::ListStyle:
        if (auto parsed_value = parse_list_style_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::Overflow:
        if (auto parsed_value = parse_overflow_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::TextDecoration:
        if (auto parsed_value = parse_text_decoration_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    case PropertyID::Transform:
        if (auto parsed_value = parse_transform_value(component_values))
            return parsed_value.release_nonnull();
        return ParsingResult::SyntaxError;
    default:
        break;
    }

    if (component_values.size() == 1) {
        if (auto parsed_value = parse_css_value(component_values.first())) {
            if (property_accepts_value(property_id, *parsed_value))
                return parsed_value.release_nonnull();
        }
        return ParsingResult::SyntaxError;
    }

    // We have multiple values, so treat them as a StyleValueList.
    if (property_maximum_value_count(property_id) > 1) {
        NonnullRefPtrVector<StyleValue> parsed_values;
        for (auto& component_value : component_values) {
            auto parsed_value = parse_css_value(component_value);
            if (!parsed_value || !property_accepts_value(property_id, *parsed_value))
                return ParsingResult::SyntaxError;
            parsed_values.append(parsed_value.release_nonnull());
        }
        if (!parsed_values.is_empty() && parsed_values.size() <= property_maximum_value_count(property_id))
            return { StyleValueList::create(move(parsed_values), StyleValueList::Separator::Space) };
    }

    return ParsingResult::SyntaxError;
}

RefPtr<StyleValue> Parser::parse_css_value(StyleComponentValueRule const& component_value)
{
    if (auto builtin = parse_builtin_value(component_value))
        return builtin;

    if (auto dynamic = parse_dynamic_value(component_value))
        return dynamic;

    // We parse colors before numbers, to catch hashless hex colors.
    if (auto color = parse_color_value(component_value))
        return color;

    if (auto dimension = parse_dimension_value(component_value))
        return dimension;

    if (auto numeric = parse_numeric_value(component_value))
        return numeric;

    if (auto identifier = parse_identifier_value(component_value))
        return identifier;

    if (auto string = parse_string_value(component_value))
        return string;

    if (auto image = parse_image_value(component_value))
        return image;

    return {};
}

Optional<Selector::SimpleSelector::ANPlusBPattern> Parser::parse_a_n_plus_b_pattern(TokenStream<StyleComponentValueRule>& values)
{
    int a = 0;
    int b = 0;

    auto syntax_error = [&]() -> Optional<Selector::SimpleSelector::ANPlusBPattern> {
        if constexpr (CSS_PARSER_DEBUG) {
            dbgln_if(CSS_PARSER_DEBUG, "Invalid An+B value:");
            values.dump_all_tokens();
        }
        return {};
    };

    auto make_return_value = [&]() -> Optional<Selector::SimpleSelector::ANPlusBPattern> {
        // When we think we are done, but there are more non-whitespace tokens, then it's a parse error.
        values.skip_whitespace();
        if (values.has_next_token()) {
            if constexpr (CSS_PARSER_DEBUG) {
                dbgln_if(CSS_PARSER_DEBUG, "Extra tokens at end of An+B value:");
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
    auto is_delim = [](StyleComponentValueRule const& value, StringView delim) -> bool {
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

OwnPtr<CalculatedStyleValue::CalcSum> Parser::parse_calc_expression(Vector<StyleComponentValueRule> const& values)
{
    auto tokens = TokenStream(values);
    return parse_calc_sum(tokens);
}

Optional<CalculatedStyleValue::CalcValue> Parser::parse_calc_value(TokenStream<StyleComponentValueRule>& tokens)
{
    auto current_token = tokens.next_token();

    if (current_token.is_block() && current_token.block().is_paren()) {
        auto block_values = TokenStream(current_token.block().values());
        auto parsed_calc_sum = parse_calc_sum(block_values);
        if (!parsed_calc_sum)
            return {};
        return CalculatedStyleValue::CalcValue { parsed_calc_sum.release_nonnull() };
    }

    if (current_token.is(Token::Type::Number))
        return CalculatedStyleValue::CalcValue {
            CalculatedStyleValue::Number {
                .is_integer = current_token.token().number_type() == Token::NumberType::Integer,
                .value = static_cast<float>(current_token.token().number_value()) }
        };

    if (current_token.is(Token::Type::Dimension) || current_token.is(Token::Type::Percentage)) {
        auto maybe_dimension = parse_dimension(current_token);
        if (!maybe_dimension.has_value())
            return {};
        auto& dimension = maybe_dimension.value();

        if (dimension.is_length())
            return CalculatedStyleValue::CalcValue { dimension.length() };
        if (dimension.is_percentage())
            return CalculatedStyleValue::CalcValue { dimension.percentage() };
        VERIFY_NOT_REACHED();
    }

    return {};
}

OwnPtr<CalculatedStyleValue::CalcProductPartWithOperator> Parser::parse_calc_product_part_with_operator(TokenStream<StyleComponentValueRule>& tokens)
{
    // Note: The default value is not used or passed around.
    auto product_with_operator = make<CalculatedStyleValue::CalcProductPartWithOperator>(
        CalculatedStyleValue::ProductOperation::Multiply,
        CalculatedStyleValue::CalcNumberValue { CalculatedStyleValue::Number { false, 0 } });

    tokens.skip_whitespace();

    auto& op_token = tokens.peek_token();
    if (!op_token.is(Token::Type::Delim))
        return nullptr;

    auto op = op_token.token().delim();
    if (op == "*"sv) {
        tokens.next_token();
        tokens.skip_whitespace();
        product_with_operator->op = CalculatedStyleValue::ProductOperation::Multiply;
        auto parsed_calc_value = parse_calc_value(tokens);
        if (!parsed_calc_value.has_value())
            return nullptr;
        product_with_operator->value = { parsed_calc_value.release_value() };

    } else if (op == "/"sv) {
        // FIXME: Detect divide-by-zero if possible
        tokens.next_token();
        tokens.skip_whitespace();
        product_with_operator->op = CalculatedStyleValue::ProductOperation::Divide;
        auto parsed_calc_number_value = parse_calc_number_value(tokens);
        if (!parsed_calc_number_value.has_value())
            return nullptr;
        product_with_operator->value = { parsed_calc_number_value.release_value() };
    } else {
        return nullptr;
    }

    return product_with_operator;
}

OwnPtr<CalculatedStyleValue::CalcNumberProductPartWithOperator> Parser::parse_calc_number_product_part_with_operator(TokenStream<StyleComponentValueRule>& tokens)
{
    // Note: The default value is not used or passed around.
    auto number_product_with_operator = make<CalculatedStyleValue::CalcNumberProductPartWithOperator>(
        CalculatedStyleValue::ProductOperation::Multiply,
        CalculatedStyleValue::CalcNumberValue { CalculatedStyleValue::Number { false, 0 } });

    tokens.skip_whitespace();

    auto& op_token = tokens.peek_token();
    if (!op_token.is(Token::Type::Delim))
        return nullptr;

    auto op = op_token.token().delim();
    if (op == "*"sv) {
        tokens.next_token();
        tokens.skip_whitespace();
        number_product_with_operator->op = CalculatedStyleValue::ProductOperation::Multiply;
    } else if (op == "/"sv) {
        // FIXME: Detect divide-by-zero if possible
        tokens.next_token();
        tokens.skip_whitespace();
        number_product_with_operator->op = CalculatedStyleValue::ProductOperation::Divide;
    } else {
        return nullptr;
    }

    auto parsed_calc_value = parse_calc_number_value(tokens);
    if (!parsed_calc_value.has_value())
        return nullptr;
    number_product_with_operator->value = parsed_calc_value.release_value();

    return number_product_with_operator;
}

OwnPtr<CalculatedStyleValue::CalcNumberProduct> Parser::parse_calc_number_product(TokenStream<StyleComponentValueRule>& tokens)
{
    auto calc_number_product = make<CalculatedStyleValue::CalcNumberProduct>(
        CalculatedStyleValue::CalcNumberValue { CalculatedStyleValue::Number { false, 0 } },
        NonnullOwnPtrVector<CalculatedStyleValue::CalcNumberProductPartWithOperator> {});

    auto first_calc_number_value_or_error = parse_calc_number_value(tokens);
    if (!first_calc_number_value_or_error.has_value())
        return nullptr;
    calc_number_product->first_calc_number_value = first_calc_number_value_or_error.release_value();

    while (tokens.has_next_token()) {
        auto number_product_with_operator = parse_calc_number_product_part_with_operator(tokens);
        if (!number_product_with_operator)
            break;
        calc_number_product->zero_or_more_additional_calc_number_values.append(number_product_with_operator.release_nonnull());
    }

    return calc_number_product;
}

OwnPtr<CalculatedStyleValue::CalcNumberSumPartWithOperator> Parser::parse_calc_number_sum_part_with_operator(TokenStream<StyleComponentValueRule>& tokens)
{
    if (!(tokens.peek_token().is(Token::Type::Delim)
            && tokens.peek_token().token().delim().is_one_of("+"sv, "-"sv)
            && tokens.peek_token(1).is(Token::Type::Whitespace)))
        return nullptr;

    auto& token = tokens.next_token();
    tokens.skip_whitespace();

    CalculatedStyleValue::SumOperation op;
    auto delim = token.token().delim();
    if (delim == "+"sv)
        op = CalculatedStyleValue::SumOperation::Add;
    else if (delim == "-"sv)
        op = CalculatedStyleValue::SumOperation::Subtract;
    else
        return nullptr;

    auto calc_number_product = parse_calc_number_product(tokens);
    if (!calc_number_product)
        return nullptr;
    return make<CalculatedStyleValue::CalcNumberSumPartWithOperator>(op, calc_number_product.release_nonnull());
}

OwnPtr<CalculatedStyleValue::CalcNumberSum> Parser::parse_calc_number_sum(TokenStream<StyleComponentValueRule>& tokens)
{
    auto first_calc_number_product_or_error = parse_calc_number_product(tokens);
    if (!first_calc_number_product_or_error)
        return nullptr;

    NonnullOwnPtrVector<CalculatedStyleValue::CalcNumberSumPartWithOperator> additional {};
    while (tokens.has_next_token()) {
        auto calc_sum_part = parse_calc_number_sum_part_with_operator(tokens);
        if (!calc_sum_part)
            return nullptr;
        additional.append(calc_sum_part.release_nonnull());
    }

    tokens.skip_whitespace();

    auto calc_number_sum = make<CalculatedStyleValue::CalcNumberSum>(first_calc_number_product_or_error.release_nonnull(), move(additional));
    return calc_number_sum;
}

Optional<CalculatedStyleValue::CalcNumberValue> Parser::parse_calc_number_value(TokenStream<StyleComponentValueRule>& tokens)
{
    auto& first = tokens.peek_token();
    if (first.is_block() && first.block().is_paren()) {
        tokens.next_token();
        auto block_values = TokenStream(first.block().values());
        auto calc_number_sum = parse_calc_number_sum(block_values);
        if (calc_number_sum)
            return CalculatedStyleValue::CalcNumberValue { calc_number_sum.release_nonnull() };
    }

    if (!first.is(Token::Type::Number))
        return {};
    tokens.next_token();

    return CalculatedStyleValue::CalcNumberValue {
        CalculatedStyleValue::Number {
            .is_integer = first.token().number_type() == Token::NumberType::Integer,
            .value = static_cast<float>(first.token().number_value()) }
    };
}

OwnPtr<CalculatedStyleValue::CalcProduct> Parser::parse_calc_product(TokenStream<StyleComponentValueRule>& tokens)
{
    auto calc_product = make<CalculatedStyleValue::CalcProduct>(
        CalculatedStyleValue::CalcValue { CalculatedStyleValue::Number { false, 0 } },
        NonnullOwnPtrVector<CalculatedStyleValue::CalcProductPartWithOperator> {});

    auto first_calc_value_or_error = parse_calc_value(tokens);
    if (!first_calc_value_or_error.has_value())
        return nullptr;
    calc_product->first_calc_value = first_calc_value_or_error.release_value();

    while (tokens.has_next_token()) {
        auto product_with_operator = parse_calc_product_part_with_operator(tokens);
        if (!product_with_operator)
            break;
        calc_product->zero_or_more_additional_calc_values.append(product_with_operator.release_nonnull());
    }

    return calc_product;
}

OwnPtr<CalculatedStyleValue::CalcSumPartWithOperator> Parser::parse_calc_sum_part_with_operator(TokenStream<StyleComponentValueRule>& tokens)
{
    // The following has to have the shape of <Whitespace><+ or -><Whitespace>
    // But the first whitespace gets eaten in parse_calc_product_part_with_operator().
    if (!(tokens.peek_token().is(Token::Type::Delim)
            && tokens.peek_token().token().delim().is_one_of("+"sv, "-"sv)
            && tokens.peek_token(1).is(Token::Type::Whitespace)))
        return nullptr;

    auto& token = tokens.next_token();
    tokens.skip_whitespace();

    CalculatedStyleValue::SumOperation op;
    auto delim = token.token().delim();
    if (delim == "+"sv)
        op = CalculatedStyleValue::SumOperation::Add;
    else if (delim == "-"sv)
        op = CalculatedStyleValue::SumOperation::Subtract;
    else
        return nullptr;

    auto calc_product = parse_calc_product(tokens);
    if (!calc_product)
        return nullptr;
    return make<CalculatedStyleValue::CalcSumPartWithOperator>(op, calc_product.release_nonnull());
};

OwnPtr<CalculatedStyleValue::CalcSum> Parser::parse_calc_sum(TokenStream<StyleComponentValueRule>& tokens)
{
    auto parsed_calc_product = parse_calc_product(tokens);
    if (!parsed_calc_product)
        return nullptr;

    NonnullOwnPtrVector<CalculatedStyleValue::CalcSumPartWithOperator> additional {};
    while (tokens.has_next_token()) {
        auto calc_sum_part = parse_calc_sum_part_with_operator(tokens);
        if (!calc_sum_part)
            return nullptr;
        additional.append(calc_sum_part.release_nonnull());
    }

    tokens.skip_whitespace();

    return make<CalculatedStyleValue::CalcSum>(parsed_calc_product.release_nonnull(), move(additional));
}

bool Parser::has_ignored_vendor_prefix(StringView string)
{
    if (!string.starts_with('-'))
        return false;
    if (string.starts_with("--"))
        return false;
    if (string.starts_with("-libweb-"))
        return false;
    return true;
}

RefPtr<StyleValue> Parser::parse_css_value(Badge<StyleComputer>, ParsingContext const& context, PropertyID property_id, Vector<StyleComponentValueRule> const& tokens)
{
    if (tokens.is_empty() || property_id == CSS::PropertyID::Invalid || property_id == CSS::PropertyID::Custom)
        return {};

    CSS::Parser parser(context, "");
    CSS::TokenStream<CSS::StyleComponentValueRule> token_stream { tokens };
    auto result = parser.parse_css_value(property_id, token_stream);
    if (result.is_error())
        return {};
    return result.release_value();
}

bool Parser::Dimension::is_length() const
{
    return m_value.has<Length>();
}

Length Parser::Dimension::length() const
{
    return m_value.get<Length>();
}

bool Parser::Dimension::is_percentage() const
{
    return m_value.has<Percentage>();
}

Percentage Parser::Dimension::percentage() const
{
    return m_value.get<Percentage>();
}

bool Parser::Dimension::is_length_percentage() const
{
    return is_length() || is_percentage();
}

LengthPercentage Parser::Dimension::length_percentage() const
{
    if (is_length())
        return length();
    if (is_percentage())
        return percentage();
    VERIFY_NOT_REACHED();
}

}

namespace Web {

RefPtr<CSS::CSSStyleSheet> parse_css(CSS::ParsingContext const& context, StringView css)
{
    if (css.is_empty())
        return CSS::CSSStyleSheet::create({});
    CSS::Parser parser(context, css);
    return parser.parse_as_stylesheet();
}

RefPtr<CSS::PropertyOwningCSSStyleDeclaration> parse_css_declaration(CSS::ParsingContext const& context, StringView css)
{
    if (css.is_empty())
        return CSS::PropertyOwningCSSStyleDeclaration::create({}, {});
    CSS::Parser parser(context, css);
    return parser.parse_as_list_of_declarations();
}

RefPtr<CSS::StyleValue> parse_css_value(CSS::ParsingContext const& context, StringView string, CSS::PropertyID property_id)
{
    if (string.is_empty())
        return {};
    CSS::Parser parser(context, string);
    return parser.parse_as_css_value(property_id);
}

RefPtr<CSS::CSSRule> parse_css_rule(CSS::ParsingContext const& context, StringView css_text)
{
    CSS::Parser parser(context, css_text);
    return parser.parse_as_rule();
}

Optional<CSS::SelectorList> parse_selector(CSS::ParsingContext const& context, StringView selector_text)
{
    CSS::Parser parser(context, selector_text);
    return parser.parse_as_selector();
}

RefPtr<CSS::MediaQuery> parse_media_query(CSS::ParsingContext const& context, StringView string)
{
    CSS::Parser parser(context, string);
    return parser.parse_as_media_query();
}

NonnullRefPtrVector<CSS::MediaQuery> parse_media_query_list(CSS::ParsingContext const& context, StringView string)
{
    CSS::Parser parser(context, string);
    return parser.parse_as_media_query_list();
}

RefPtr<CSS::Supports> parse_css_supports(CSS::ParsingContext const& context, StringView string)
{
    if (string.is_empty())
        return {};
    CSS::Parser parser(context, string);
    return parser.parse_as_supports();
}

RefPtr<CSS::StyleValue> parse_html_length(DOM::Document const& document, StringView string)
{
    auto integer = string.to_int();
    if (integer.has_value())
        return CSS::LengthStyleValue::create(CSS::Length::make_px(integer.value()));
    return parse_css_value(CSS::ParsingContext(document), string);
}

}
