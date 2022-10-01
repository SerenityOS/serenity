/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/GenericLexer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/SourceLocation.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/CSS/CSSFontFaceRule.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSMediaRule.h>
#include <LibWeb/CSS/CSSStyleDeclaration.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/CSSSupportsRule.h>
#include <LibWeb/CSS/Parser/Block.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/Function.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/Parser/Rule.h>
#include <LibWeb/CSS/Selector.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>

static void log_parse_error(SourceLocation const& location = SourceLocation::current())
{
    dbgln_if(CSS_PARSER_DEBUG, "Parse error (CSS) {}", location);
}

namespace Web::CSS::Parser {

ParsingContext::ParsingContext()
    : m_realm(*Bindings::main_thread_vm().current_realm())
{
}

ParsingContext::ParsingContext(JS::Realm& realm)
    : m_realm(realm)
{
}

ParsingContext::ParsingContext(DOM::Document const& document, AK::URL url)
    : m_realm(const_cast<JS::Realm&>(document.realm()))
    , m_document(&document)
    , m_url(move(url))
{
}

ParsingContext::ParsingContext(DOM::Document const& document)
    : m_realm(const_cast<JS::Realm&>(document.realm()))
    , m_document(&document)
    , m_url(document.url())
{
}

ParsingContext::ParsingContext(DOM::ParentNode& parent_node)
    : m_realm(parent_node.realm())
    , m_document(&parent_node.document())
    , m_url(parent_node.document().url())
{
}

bool ParsingContext::in_quirks_mode() const
{
    return m_document ? m_document->in_quirks_mode() : false;
}

// https://www.w3.org/TR/css-values-4/#relative-urls
AK::URL ParsingContext::complete_url(String const& addr) const
{
    return m_url.complete_url(addr);
}

Parser::Parser(ParsingContext const& context, StringView input, String const& encoding)
    : m_context(context)
    , m_tokenizer(input, encoding)
    , m_tokens(m_tokenizer.parse())
    , m_token_stream(TokenStream(m_tokens))
{
}

// 5.3.3. Parse a stylesheet
// https://www.w3.org/TR/css-syntax-3/#parse-stylesheet
template<typename T>
Parser::ParsedStyleSheet Parser::parse_a_stylesheet(TokenStream<T>& tokens, Optional<AK::URL> location)
{
    // To parse a stylesheet from an input given an optional url location:

    // 1. If input is a byte stream for stylesheet, decode bytes from input, and set input to the result.
    // 2. Normalize input, and set input to the result.
    // NOTE: These are done automatically when creating the Parser.

    // 3. Create a new stylesheet, with its location set to location (or null, if location was not passed).
    ParsedStyleSheet style_sheet;
    style_sheet.location = move(location);

    // 4. Consume a list of rules from input, with the top-level flag set, and set the stylesheet’s value to the result.
    style_sheet.rules = consume_a_list_of_rules(tokens, TopLevel::Yes);

    // 5. Return the stylesheet.
    return style_sheet;
}

// https://www.w3.org/TR/css-syntax-3/#parse-a-css-stylesheet
CSSStyleSheet* Parser::parse_as_css_stylesheet(Optional<AK::URL> location)
{
    // To parse a CSS stylesheet, first parse a stylesheet.
    auto style_sheet = parse_a_stylesheet(m_token_stream, {});

    // Interpret all of the resulting top-level qualified rules as style rules, defined below.
    JS::MarkedVector<CSSRule*> rules(m_context.realm().heap());
    for (auto& raw_rule : style_sheet.rules) {
        auto* rule = convert_to_rule(raw_rule);
        // If any style rule is invalid, or any at-rule is not recognized or is invalid according to its grammar or context, it’s a parse error. Discard that rule.
        if (rule)
            rules.append(rule);
    }

    auto* rule_list = CSSRuleList::create(m_context.realm(), rules);
    return CSSStyleSheet::create(m_context.realm(), *rule_list, move(location));
}

Optional<SelectorList> Parser::parse_as_selector(SelectorParsingMode parsing_mode)
{
    auto selector_list = parse_a_selector_list(m_token_stream, SelectorType::Standalone, parsing_mode);
    if (!selector_list.is_error())
        return selector_list.release_value();

    return {};
}

Optional<SelectorList> Parser::parse_as_relative_selector(SelectorParsingMode parsing_mode)
{
    auto selector_list = parse_a_selector_list(m_token_stream, SelectorType::Relative, parsing_mode);
    if (!selector_list.is_error())
        return selector_list.release_value();

    return {};
}

template<typename T>
Parser::ParseErrorOr<SelectorList> Parser::parse_a_selector_list(TokenStream<T>& tokens, SelectorType mode, SelectorParsingMode parsing_mode)
{
    auto comma_separated_lists = parse_a_comma_separated_list_of_component_values(tokens);

    NonnullRefPtrVector<Selector> selectors;
    for (auto& selector_parts : comma_separated_lists) {
        auto stream = TokenStream(selector_parts);
        auto selector = parse_complex_selector(stream, mode);
        if (selector.is_error()) {
            if (parsing_mode == SelectorParsingMode::Forgiving)
                continue;
            return selector.error();
        }
        selectors.append(selector.release_value());
    }

    if (selectors.is_empty() && parsing_mode != SelectorParsingMode::Forgiving)
        return ParseError::SyntaxError;

    return selectors;
}

Parser::ParseErrorOr<NonnullRefPtr<Selector>> Parser::parse_complex_selector(TokenStream<ComponentValue>& tokens, SelectorType mode)
{
    Vector<Selector::CompoundSelector> compound_selectors;

    auto first_selector = TRY(parse_compound_selector(tokens));
    if (!first_selector.has_value())
        return ParseError::SyntaxError;

    if (mode == SelectorType::Standalone) {
        if (first_selector->combinator != Selector::Combinator::Descendant)
            return ParseError::SyntaxError;
        first_selector->combinator = Selector::Combinator::None;
    }
    compound_selectors.append(first_selector.release_value());

    while (tokens.has_next_token()) {
        auto compound_selector = TRY(parse_compound_selector(tokens));
        if (!compound_selector.has_value())
            break;
        compound_selectors.append(compound_selector.release_value());
    }

    if (compound_selectors.is_empty())
        return ParseError::SyntaxError;

    return Selector::create(move(compound_selectors));
}

Parser::ParseErrorOr<Optional<Selector::CompoundSelector>> Parser::parse_compound_selector(TokenStream<ComponentValue>& tokens)
{
    tokens.skip_whitespace();

    auto combinator = parse_selector_combinator(tokens).value_or(Selector::Combinator::Descendant);

    tokens.skip_whitespace();

    Vector<Selector::SimpleSelector> simple_selectors;

    while (tokens.has_next_token()) {
        auto component = TRY(parse_simple_selector(tokens));
        if (!component.has_value())
            break;
        simple_selectors.append(component.release_value());
    }

    if (simple_selectors.is_empty())
        return Optional<Selector::CompoundSelector> {};

    return Selector::CompoundSelector { combinator, move(simple_selectors) };
}

Optional<Selector::Combinator> Parser::parse_selector_combinator(TokenStream<ComponentValue>& tokens)
{
    auto const& current_value = tokens.next_token();
    if (current_value.is(Token::Type::Delim)) {
        switch (current_value.token().delim()) {
        case '>':
            return Selector::Combinator::ImmediateChild;
        case '+':
            return Selector::Combinator::NextSibling;
        case '~':
            return Selector::Combinator::SubsequentSibling;
        case '|': {
            auto const& next = tokens.peek_token();
            if (next.is(Token::Type::EndOfFile))
                return {};

            if (next.is(Token::Type::Delim) && next.token().delim() == '|') {
                tokens.next_token();
                return Selector::Combinator::Column;
            }
        }
        }
    }

    tokens.reconsume_current_input_token();
    return {};
}

Parser::ParseErrorOr<Selector::SimpleSelector> Parser::parse_attribute_simple_selector(ComponentValue const& first_value)
{
    auto attribute_tokens = TokenStream { first_value.block().values() };

    attribute_tokens.skip_whitespace();

    if (!attribute_tokens.has_next_token()) {
        dbgln_if(CSS_PARSER_DEBUG, "CSS attribute selector is empty!");
        return ParseError::SyntaxError;
    }

    // FIXME: Handle namespace prefix for attribute name.
    auto const& attribute_part = attribute_tokens.next_token();
    if (!attribute_part.is(Token::Type::Ident)) {
        dbgln_if(CSS_PARSER_DEBUG, "Expected ident for attribute name, got: '{}'", attribute_part.to_debug_string());
        return ParseError::SyntaxError;
    }

    Selector::SimpleSelector simple_selector {
        .type = Selector::SimpleSelector::Type::Attribute,
        .value = Selector::SimpleSelector::Attribute {
            .match_type = Selector::SimpleSelector::Attribute::MatchType::HasAttribute,
            // FIXME: Case-sensitivity is defined by the document language.
            // HTML is insensitive with attribute names, and our code generally assumes
            // they are converted to lowercase, so we do that here too. If we want to be
            // correct with XML later, we'll need to keep the original case and then do
            // a case-insensitive compare later.
            .name = attribute_part.token().ident().to_lowercase_string(),
            .case_type = Selector::SimpleSelector::Attribute::CaseType::DefaultMatch,
        }
    };

    attribute_tokens.skip_whitespace();
    if (!attribute_tokens.has_next_token())
        return simple_selector;

    auto const& delim_part = attribute_tokens.next_token();
    if (!delim_part.is(Token::Type::Delim)) {
        dbgln_if(CSS_PARSER_DEBUG, "Expected a delim for attribute comparison, got: '{}'", delim_part.to_debug_string());
        return ParseError::SyntaxError;
    }

    if (delim_part.token().delim() == '=') {
        simple_selector.attribute().match_type = Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch;
    } else {
        if (!attribute_tokens.has_next_token()) {
            dbgln_if(CSS_PARSER_DEBUG, "Attribute selector ended part way through a match type.");
            return ParseError::SyntaxError;
        }

        auto const& delim_second_part = attribute_tokens.next_token();
        if (!(delim_second_part.is(Token::Type::Delim) && delim_second_part.token().delim() == '=')) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected a double delim for attribute comparison, got: '{}{}'", delim_part.to_debug_string(), delim_second_part.to_debug_string());
            return ParseError::SyntaxError;
        }
        switch (delim_part.token().delim()) {
        case '~':
            simple_selector.attribute().match_type = Selector::SimpleSelector::Attribute::MatchType::ContainsWord;
            break;
        case '*':
            simple_selector.attribute().match_type = Selector::SimpleSelector::Attribute::MatchType::ContainsString;
            break;
        case '|':
            simple_selector.attribute().match_type = Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment;
            break;
        case '^':
            simple_selector.attribute().match_type = Selector::SimpleSelector::Attribute::MatchType::StartsWithString;
            break;
        case '$':
            simple_selector.attribute().match_type = Selector::SimpleSelector::Attribute::MatchType::EndsWithString;
            break;
        default:
            attribute_tokens.reconsume_current_input_token();
        }
    }

    attribute_tokens.skip_whitespace();
    if (!attribute_tokens.has_next_token()) {
        dbgln_if(CSS_PARSER_DEBUG, "Attribute selector ended without a value to match.");
        return ParseError::SyntaxError;
    }

    auto const& value_part = attribute_tokens.next_token();
    if (!value_part.is(Token::Type::Ident) && !value_part.is(Token::Type::String)) {
        dbgln_if(CSS_PARSER_DEBUG, "Expected a string or ident for the value to match attribute against, got: '{}'", value_part.to_debug_string());
        return ParseError::SyntaxError;
    }
    simple_selector.attribute().value = value_part.token().is(Token::Type::Ident) ? value_part.token().ident() : value_part.token().string();

    attribute_tokens.skip_whitespace();
    // Handle case-sensitivity suffixes. https://www.w3.org/TR/selectors-4/#attribute-case
    if (attribute_tokens.has_next_token()) {
        auto const& case_sensitivity_part = attribute_tokens.next_token();
        if (case_sensitivity_part.is(Token::Type::Ident)) {
            auto case_sensitivity = case_sensitivity_part.token().ident();
            if (case_sensitivity.equals_ignoring_case("i"sv)) {
                simple_selector.attribute().case_type = Selector::SimpleSelector::Attribute::CaseType::CaseInsensitiveMatch;
            } else if (case_sensitivity.equals_ignoring_case("s"sv)) {
                simple_selector.attribute().case_type = Selector::SimpleSelector::Attribute::CaseType::CaseSensitiveMatch;
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "Expected a \"i\" or \"s\" attribute selector case sensitivity identifier, got: '{}'", case_sensitivity_part.to_debug_string());
                return ParseError::SyntaxError;
            }
        } else {
            dbgln_if(CSS_PARSER_DEBUG, "Expected an attribute selector case sensitivity identifier, got: '{}'", case_sensitivity_part.to_debug_string());
            return ParseError::SyntaxError;
        }
    }

    if (attribute_tokens.has_next_token()) {
        dbgln_if(CSS_PARSER_DEBUG, "Was not expecting anything else inside attribute selector.");
        return ParseError::SyntaxError;
    }

    return simple_selector;
}

Parser::ParseErrorOr<Selector::SimpleSelector> Parser::parse_pseudo_simple_selector(TokenStream<ComponentValue>& tokens)
{
    auto peek_token_ends_selector = [&]() -> bool {
        auto const& value = tokens.peek_token();
        return (value.is(Token::Type::EndOfFile) || value.is(Token::Type::Whitespace) || value.is(Token::Type::Comma));
    };

    if (peek_token_ends_selector())
        return ParseError::SyntaxError;

    bool is_pseudo = false;
    if (tokens.peek_token().is(Token::Type::Colon)) {
        is_pseudo = true;
        tokens.next_token();
        if (peek_token_ends_selector())
            return ParseError::SyntaxError;
    }

    if (is_pseudo) {
        auto const& name_token = tokens.next_token();
        if (!name_token.is(Token::Type::Ident)) {
            dbgln_if(CSS_PARSER_DEBUG, "Expected an ident for pseudo-element, got: '{}'", name_token.to_debug_string());
            return ParseError::SyntaxError;
        }

        auto pseudo_name = name_token.token().ident();
        auto pseudo_element = pseudo_element_from_string(pseudo_name);

        // Note: We allow the "ignored" -webkit prefix here for -webkit-progress-bar/-webkit-progress-bar
        if (!pseudo_element.has_value() && has_ignored_vendor_prefix(pseudo_name))
            return ParseError::IncludesIgnoredVendorPrefix;

        if (!pseudo_element.has_value()) {
            dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-element: '::{}'", pseudo_name);
            return ParseError::SyntaxError;
        }

        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::PseudoElement,
            .value = pseudo_element.value()
        };
    }

    if (peek_token_ends_selector())
        return ParseError::SyntaxError;

    auto const& pseudo_class_token = tokens.next_token();

    if (pseudo_class_token.is(Token::Type::Ident)) {
        auto pseudo_name = pseudo_class_token.token().ident();
        if (has_ignored_vendor_prefix(pseudo_name))
            return ParseError::IncludesIgnoredVendorPrefix;

        auto make_pseudo_class_selector = [](auto pseudo_class) {
            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClass {
                    .type = pseudo_class }
            };
        };

        if (pseudo_name.equals_ignoring_case("active"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Active);
        if (pseudo_name.equals_ignoring_case("checked"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Checked);
        if (pseudo_name.equals_ignoring_case("disabled"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Disabled);
        if (pseudo_name.equals_ignoring_case("empty"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Empty);
        if (pseudo_name.equals_ignoring_case("enabled"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Enabled);
        if (pseudo_name.equals_ignoring_case("first-child"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::FirstChild);
        if (pseudo_name.equals_ignoring_case("first-of-type"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::FirstOfType);
        if (pseudo_name.equals_ignoring_case("focus"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Focus);
        if (pseudo_name.equals_ignoring_case("focus-within"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::FocusWithin);
        if (pseudo_name.equals_ignoring_case("hover"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Hover);
        if (pseudo_name.equals_ignoring_case("last-child"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::LastChild);
        if (pseudo_name.equals_ignoring_case("last-of-type"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::LastOfType);
        if (pseudo_name.equals_ignoring_case("link"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Link);
        if (pseudo_name.equals_ignoring_case("only-child"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::OnlyChild);
        if (pseudo_name.equals_ignoring_case("only-of-type"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::OnlyOfType);
        if (pseudo_name.equals_ignoring_case("root"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Root);
        if (pseudo_name.equals_ignoring_case("visited"sv))
            return make_pseudo_class_selector(Selector::SimpleSelector::PseudoClass::Type::Visited);

        // Single-colon syntax allowed for ::after, ::before, ::first-letter and ::first-line for compatibility.
        // https://www.w3.org/TR/selectors/#pseudo-element-syntax
        if (auto pseudo_element = pseudo_element_from_string(pseudo_name); pseudo_element.has_value()) {
            switch (pseudo_element.value()) {
            case Selector::PseudoElement::After:
            case Selector::PseudoElement::Before:
            case Selector::PseudoElement::FirstLetter:
            case Selector::PseudoElement::FirstLine:
                return Selector::SimpleSelector {
                    .type = Selector::SimpleSelector::Type::PseudoElement,
                    .value = pseudo_element.value()
                };
            default:
                break;
            }
        }

        dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-class: ':{}'", pseudo_name);
        return ParseError::SyntaxError;
    }

    if (pseudo_class_token.is_function()) {
        auto parse_nth_child_selector = [this](auto pseudo_class, Vector<ComponentValue> const& function_values, bool allow_of = false) -> ParseErrorOr<Selector::SimpleSelector> {
            auto tokens = TokenStream<ComponentValue>(function_values);
            auto nth_child_pattern = parse_a_n_plus_b_pattern(tokens);
            if (!nth_child_pattern.has_value()) {
                dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid An+B format for {}", pseudo_class_name(pseudo_class));
                return ParseError::SyntaxError;
            }

            tokens.skip_whitespace();
            if (!tokens.has_next_token()) {
                return Selector::SimpleSelector {
                    .type = Selector::SimpleSelector::Type::PseudoClass,
                    .value = Selector::SimpleSelector::PseudoClass {
                        .type = pseudo_class,
                        .nth_child_pattern = nth_child_pattern.release_value() }
                };
            }

            if (!allow_of)
                return ParseError::SyntaxError;

            // Parse the `of <selector-list>` syntax
            auto const& maybe_of = tokens.next_token();
            if (!(maybe_of.is(Token::Type::Ident) && maybe_of.token().ident().equals_ignoring_case("of"sv)))
                return ParseError::SyntaxError;

            tokens.skip_whitespace();
            auto selector_list = TRY(parse_a_selector_list(tokens, SelectorType::Standalone));

            tokens.skip_whitespace();
            if (tokens.has_next_token())
                return ParseError::SyntaxError;

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClass {
                    .type = pseudo_class,
                    .nth_child_pattern = nth_child_pattern.release_value(),
                    .argument_selector_list = move(selector_list) }
            };
        };

        auto const& pseudo_function = pseudo_class_token.function();
        if (pseudo_function.name().equals_ignoring_case("is"sv)
            || pseudo_function.name().equals_ignoring_case("where"sv)) {
            auto function_token_stream = TokenStream(pseudo_function.values());
            // NOTE: Because it's forgiving, even complete garbage will parse OK as an empty selector-list.
            auto argument_selector_list = MUST(parse_a_selector_list(function_token_stream, SelectorType::Standalone, SelectorParsingMode::Forgiving));

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClass {
                    .type = pseudo_function.name().equals_ignoring_case("is"sv)
                        ? Selector::SimpleSelector::PseudoClass::Type::Is
                        : Selector::SimpleSelector::PseudoClass::Type::Where,
                    .argument_selector_list = move(argument_selector_list) }
            };
        }
        if (pseudo_function.name().equals_ignoring_case("not"sv)) {
            auto function_token_stream = TokenStream(pseudo_function.values());
            auto not_selector = TRY(parse_a_selector_list(function_token_stream, SelectorType::Standalone));

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClass {
                    .type = Selector::SimpleSelector::PseudoClass::Type::Not,
                    .argument_selector_list = move(not_selector) }
            };
        }
        if (pseudo_function.name().equals_ignoring_case("lang"sv)) {
            if (pseudo_function.values().is_empty()) {
                dbgln_if(CSS_PARSER_DEBUG, "Empty :lang() selector");
                return ParseError::SyntaxError;
            }
            // FIXME: Support multiple, comma-separated, language ranges.
            Vector<FlyString> languages;
            languages.append(pseudo_function.values().first().token().to_string());
            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClass {
                    .type = Selector::SimpleSelector::PseudoClass::Type::Lang,
                    .languages = move(languages) }
            };
        }
        if (pseudo_function.name().equals_ignoring_case("nth-child"sv))
            return parse_nth_child_selector(Selector::SimpleSelector::PseudoClass::Type::NthChild, pseudo_function.values(), true);
        if (pseudo_function.name().equals_ignoring_case("nth-last-child"sv))
            return parse_nth_child_selector(Selector::SimpleSelector::PseudoClass::Type::NthLastChild, pseudo_function.values(), true);
        if (pseudo_function.name().equals_ignoring_case("nth-of-type"sv))
            return parse_nth_child_selector(Selector::SimpleSelector::PseudoClass::Type::NthOfType, pseudo_function.values(), false);
        if (pseudo_function.name().equals_ignoring_case("nth-last-of-type"sv))
            return parse_nth_child_selector(Selector::SimpleSelector::PseudoClass::Type::NthLastOfType, pseudo_function.values(), false);

        dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-class function: ':{}'()", pseudo_function.name());
        return ParseError::SyntaxError;
    }
    dbgln_if(CSS_PARSER_DEBUG, "Unexpected Block in pseudo-class name, expected a function or identifier. '{}'", pseudo_class_token.to_debug_string());
    return ParseError::SyntaxError;
}

Parser::ParseErrorOr<Optional<Selector::SimpleSelector>> Parser::parse_simple_selector(TokenStream<ComponentValue>& tokens)
{
    auto peek_token_ends_selector = [&]() -> bool {
        auto const& value = tokens.peek_token();
        return (value.is(Token::Type::EndOfFile) || value.is(Token::Type::Whitespace) || value.is(Token::Type::Comma));
    };

    if (peek_token_ends_selector())
        return Optional<Selector::SimpleSelector> {};

    auto const& first_value = tokens.next_token();

    if (first_value.is(Token::Type::Delim)) {
        u32 delim = first_value.token().delim();
        switch (delim) {
        case '*':
            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::Universal
            };
        case '.': {
            if (peek_token_ends_selector())
                return ParseError::SyntaxError;

            auto const& class_name_value = tokens.next_token();
            if (!class_name_value.is(Token::Type::Ident)) {
                dbgln_if(CSS_PARSER_DEBUG, "Expected an ident after '.', got: {}", class_name_value.to_debug_string());
                return ParseError::SyntaxError;
            }
            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::Class,
                .value = Selector::SimpleSelector::Name { class_name_value.token().ident() }
            };
        }
        case '>':
        case '+':
        case '~':
        case '|':
            // Whitespace is not required between the compound-selector and a combinator.
            // So, if we see a combinator, return that this compound-selector is done, instead of a syntax error.
            tokens.reconsume_current_input_token();
            return Optional<Selector::SimpleSelector> {};
        default:
            dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid simple selector!");
            return ParseError::SyntaxError;
        }
    }

    if (first_value.is(Token::Type::Hash)) {
        if (first_value.token().hash_type() != Token::HashType::Id) {
            dbgln_if(CSS_PARSER_DEBUG, "Selector contains hash token that is not an id: {}", first_value.to_debug_string());
            return ParseError::SyntaxError;
        }
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::Id,
            .value = Selector::SimpleSelector::Name { first_value.token().hash_value() }
        };
    }
    if (first_value.is(Token::Type::Ident)) {
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::TagName,
            .value = Selector::SimpleSelector::Name { first_value.token().ident() }
        };
    }
    if (first_value.is_block() && first_value.block().is_square())
        return TRY(parse_attribute_simple_selector(first_value));

    if (first_value.is(Token::Type::Colon))
        return TRY(parse_pseudo_simple_selector(tokens));

    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid simple selector!");
    return ParseError::SyntaxError;
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
NonnullRefPtr<MediaQuery> Parser::parse_media_query(TokenStream<ComponentValue>& tokens)
{
    // `<media-query> = <media-condition>
    //                | [ not | only ]? <media-type> [ and <media-condition-without-or> ]?`

    // `[ not | only ]?`, Returns whether to negate the query
    auto parse_initial_modifier = [](auto& tokens) -> Optional<bool> {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        auto& token = tokens.next_token();
        if (!token.is(Token::Type::Ident))
            return {};

        auto ident = token.token().ident();
        if (ident.equals_ignoring_case("not"sv)) {
            transaction.commit();
            return true;
        }
        if (ident.equals_ignoring_case("only"sv)) {
            transaction.commit();
            return false;
        }
        return {};
    };

    auto invalid_media_query = [&]() {
        // "A media query that does not match the grammar in the previous section must be replaced by `not all`
        // during parsing." - https://www.w3.org/TR/mediaqueries-5/#error-handling
        if constexpr (CSS_PARSER_DEBUG) {
            dbgln("Invalid media query:");
            tokens.dump_all_tokens();
        }
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
    if (auto maybe_and = tokens.next_token(); maybe_and.is(Token::Type::Ident) && maybe_and.token().ident().equals_ignoring_case("and"sv)) {
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
OwnPtr<MediaCondition> Parser::parse_media_condition(TokenStream<ComponentValue>& tokens, MediaCondition::AllowOr allow_or)
{
    // `<media-not> | <media-in-parens> [ <media-and>* | <media-or>* ]`
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();

    // `<media-not> = not <media-in-parens>`
    auto parse_media_not = [&](auto& tokens) -> OwnPtr<MediaCondition> {
        auto local_transaction = tokens.begin_transaction();
        tokens.skip_whitespace();

        auto& first_token = tokens.next_token();
        if (first_token.is(Token::Type::Ident) && first_token.token().ident().equals_ignoring_case("not"sv)) {
            if (auto child_condition = parse_media_condition(tokens, MediaCondition::AllowOr::Yes)) {
                local_transaction.commit();
                return MediaCondition::from_not(child_condition.release_nonnull());
            }
        }

        return {};
    };

    auto parse_media_with_combinator = [&](auto& tokens, StringView combinator) -> OwnPtr<MediaCondition> {
        auto local_transaction = tokens.begin_transaction();
        tokens.skip_whitespace();

        auto& first = tokens.next_token();
        if (first.is(Token::Type::Ident) && first.token().ident().equals_ignoring_case(combinator)) {
            tokens.skip_whitespace();
            if (auto media_in_parens = parse_media_in_parens(tokens)) {
                local_transaction.commit();
                return media_in_parens;
            }
        }

        return {};
    };

    // `<media-and> = and <media-in-parens>`
    auto parse_media_and = [&](auto& tokens) { return parse_media_with_combinator(tokens, "and"sv); };
    // `<media-or> = or <media-in-parens>`
    auto parse_media_or = [&](auto& tokens) { return parse_media_with_combinator(tokens, "or"sv); };

    // `<media-not>`
    if (auto maybe_media_not = parse_media_not(tokens)) {
        transaction.commit();
        return maybe_media_not.release_nonnull();
    }

    // `<media-in-parens> [ <media-and>* | <media-or>* ]`
    if (auto maybe_media_in_parens = parse_media_in_parens(tokens)) {
        tokens.skip_whitespace();
        // Only `<media-in-parens>`
        if (!tokens.has_next_token()) {
            transaction.commit();
            return maybe_media_in_parens.release_nonnull();
        }

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
                return {};
            }

            transaction.commit();
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
                    return {};
                }

                transaction.commit();
                return MediaCondition::from_or_list(move(child_conditions));
            }
        }
    }

    return {};
}

// `<media-feature>`, https://www.w3.org/TR/mediaqueries-4/#typedef-media-feature
Optional<MediaFeature> Parser::parse_media_feature(TokenStream<ComponentValue>& tokens)
{
    // `[ <mf-plain> | <mf-boolean> | <mf-range> ]`
    tokens.skip_whitespace();

    // `<mf-name> = <ident>`
    struct MediaFeatureName {
        enum Type {
            Normal,
            Min,
            Max
        } type;
        MediaFeatureID id;
    };
    auto parse_mf_name = [](auto& tokens, bool allow_min_max_prefix) -> Optional<MediaFeatureName> {
        auto transaction = tokens.begin_transaction();
        auto& token = tokens.next_token();
        if (token.is(Token::Type::Ident)) {
            auto name = token.token().ident();
            if (auto id = media_feature_id_from_string(name); id.has_value()) {
                transaction.commit();
                return MediaFeatureName { MediaFeatureName::Type::Normal, id.value() };
            }

            if (allow_min_max_prefix && (name.starts_with("min-"sv, CaseSensitivity::CaseInsensitive) || name.starts_with("max-"sv, CaseSensitivity::CaseInsensitive))) {
                auto adjusted_name = name.substring_view(4);
                if (auto id = media_feature_id_from_string(adjusted_name); id.has_value() && media_feature_type_is_range(id.value())) {
                    transaction.commit();
                    return MediaFeatureName {
                        name.starts_with("min-"sv, CaseSensitivity::CaseInsensitive) ? MediaFeatureName::Type::Min : MediaFeatureName::Type::Max,
                        id.value()
                    };
                }
            }
        }
        return {};
    };

    // `<mf-boolean> = <mf-name>`
    auto parse_mf_boolean = [&](auto& tokens) -> Optional<MediaFeature> {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();

        if (auto maybe_name = parse_mf_name(tokens, false); maybe_name.has_value()) {
            tokens.skip_whitespace();
            if (!tokens.has_next_token()) {
                transaction.commit();
                return MediaFeature::boolean(maybe_name->id);
            }
        }

        return {};
    };

    // `<mf-plain> = <mf-name> : <mf-value>`
    auto parse_mf_plain = [&](auto& tokens) -> Optional<MediaFeature> {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();

        if (auto maybe_name = parse_mf_name(tokens, true); maybe_name.has_value()) {
            tokens.skip_whitespace();
            if (tokens.next_token().is(Token::Type::Colon)) {
                tokens.skip_whitespace();
                if (auto maybe_value = parse_media_feature_value(maybe_name->id, tokens); maybe_value.has_value()) {
                    tokens.skip_whitespace();
                    if (!tokens.has_next_token()) {
                        transaction.commit();
                        switch (maybe_name->type) {
                        case MediaFeatureName::Type::Normal:
                            return MediaFeature::plain(maybe_name->id, maybe_value.release_value());
                        case MediaFeatureName::Type::Min:
                            return MediaFeature::min(maybe_name->id, maybe_value.release_value());
                        case MediaFeatureName::Type::Max:
                            return MediaFeature::max(maybe_name->id, maybe_value.release_value());
                        }
                        VERIFY_NOT_REACHED();
                    }
                }
            }
        }
        return {};
    };

    // `<mf-lt> = '<' '='?
    //  <mf-gt> = '>' '='?
    //  <mf-eq> = '='
    //  <mf-comparison> = <mf-lt> | <mf-gt> | <mf-eq>`
    auto parse_comparison = [](auto& tokens) -> Optional<MediaFeature::Comparison> {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();

        auto& first = tokens.next_token();
        if (first.is(Token::Type::Delim)) {
            auto first_delim = first.token().delim();
            if (first_delim == '=') {
                transaction.commit();
                return MediaFeature::Comparison::Equal;
            }
            if (first_delim == '<') {
                auto& second = tokens.peek_token();
                if (second.is(Token::Type::Delim) && second.token().delim() == '=') {
                    tokens.next_token();
                    transaction.commit();
                    return MediaFeature::Comparison::LessThanOrEqual;
                }
                transaction.commit();
                return MediaFeature::Comparison::LessThan;
            }
            if (first_delim == '>') {
                auto& second = tokens.peek_token();
                if (second.is(Token::Type::Delim) && second.token().delim() == '=') {
                    tokens.next_token();
                    transaction.commit();
                    return MediaFeature::Comparison::GreaterThanOrEqual;
                }
                transaction.commit();
                return MediaFeature::Comparison::GreaterThan;
            }
        }

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
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();

        // `<mf-name> <mf-comparison> <mf-value>`
        // NOTE: We have to check for <mf-name> first, since all <mf-name>s will also parse as <mf-value>.
        if (auto maybe_name = parse_mf_name(tokens, false); maybe_name.has_value() && media_feature_type_is_range(maybe_name->id)) {
            tokens.skip_whitespace();
            if (auto maybe_comparison = parse_comparison(tokens); maybe_comparison.has_value()) {
                tokens.skip_whitespace();
                if (auto maybe_value = parse_media_feature_value(maybe_name->id, tokens); maybe_value.has_value()) {
                    tokens.skip_whitespace();
                    if (!tokens.has_next_token() && !maybe_value->is_ident()) {
                        transaction.commit();
                        return MediaFeature::half_range(maybe_value.release_value(), flip(maybe_comparison.release_value()), maybe_name->id);
                    }
                }
            }
        }

        //  `<mf-value> <mf-comparison> <mf-name>
        // | <mf-value> <mf-lt> <mf-name> <mf-lt> <mf-value>
        // | <mf-value> <mf-gt> <mf-name> <mf-gt> <mf-value>`
        // NOTE: To parse the first value, we need to first find and parse the <mf-name> so we know what value types to parse.
        //       To allow for <mf-value> to be any number of tokens long, we scan forward until we find a comparison, and then
        //       treat the next non-whitespace token as the <mf-name>, which should be correct as long as they don't add a value
        //       type that can include a comparison in it. :^)
        Optional<MediaFeatureName> maybe_name;
        {
            // This transaction is never committed, we just use it to rewind automatically.
            auto temp_transaction = tokens.begin_transaction();
            while (tokens.has_next_token() && !maybe_name.has_value()) {
                if (auto maybe_comparison = parse_comparison(tokens); maybe_comparison.has_value()) {
                    // We found a comparison, so the next non-whitespace token should be the <mf-name>
                    tokens.skip_whitespace();
                    maybe_name = parse_mf_name(tokens, false);
                    break;
                }
                tokens.next_token();
                tokens.skip_whitespace();
            }
        }

        // Now, we can parse the range properly.
        if (maybe_name.has_value() && media_feature_type_is_range(maybe_name->id)) {
            if (auto maybe_left_value = parse_media_feature_value(maybe_name->id, tokens); maybe_left_value.has_value()) {
                tokens.skip_whitespace();
                if (auto maybe_left_comparison = parse_comparison(tokens); maybe_left_comparison.has_value()) {
                    tokens.skip_whitespace();
                    tokens.next_token(); // The <mf-name> which we already parsed above.
                    tokens.skip_whitespace();

                    if (!tokens.has_next_token()) {
                        transaction.commit();
                        return MediaFeature::half_range(maybe_left_value.release_value(), maybe_left_comparison.release_value(), maybe_name->id);
                    }

                    if (auto maybe_right_comparison = parse_comparison(tokens); maybe_right_comparison.has_value()) {
                        tokens.skip_whitespace();
                        if (auto maybe_right_value = parse_media_feature_value(maybe_name->id, tokens); maybe_right_value.has_value()) {
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
                                transaction.commit();
                                return MediaFeature::range(maybe_left_value.release_value(), left_comparison, maybe_name->id, right_comparison, maybe_right_value.release_value());
                            }
                        }
                    }
                }
            }
        }

        return {};
    };

    if (auto maybe_mf_boolean = parse_mf_boolean(tokens); maybe_mf_boolean.has_value())
        return maybe_mf_boolean.release_value();

    if (auto maybe_mf_plain = parse_mf_plain(tokens); maybe_mf_plain.has_value())
        return maybe_mf_plain.release_value();

    if (auto maybe_mf_range = parse_mf_range(tokens); maybe_mf_range.has_value())
        return maybe_mf_range.release_value();

    return {};
}

Optional<MediaQuery::MediaType> Parser::parse_media_type(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();
    auto const& token = tokens.next_token();

    if (!token.is(Token::Type::Ident))
        return {};

    auto ident = token.token().ident();
    if (auto media_type = media_type_from_string(ident); media_type.has_value()) {
        transaction.commit();
        return media_type.release_value();
    }

    return {};
}

// `<media-in-parens>`, https://www.w3.org/TR/mediaqueries-4/#typedef-media-in-parens
OwnPtr<MediaCondition> Parser::parse_media_in_parens(TokenStream<ComponentValue>& tokens)
{
    // `<media-in-parens> = ( <media-condition> ) | ( <media-feature> ) | <general-enclosed>`
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();

    // `( <media-condition> ) | ( <media-feature> )`
    auto const& first_token = tokens.peek_token();
    if (first_token.is_block() && first_token.block().is_paren()) {
        TokenStream inner_token_stream { first_token.block().values() };
        if (auto maybe_media_condition = parse_media_condition(inner_token_stream, MediaCondition::AllowOr::Yes)) {
            tokens.next_token();
            transaction.commit();
            return maybe_media_condition.release_nonnull();
        }
        if (auto maybe_media_feature = parse_media_feature(inner_token_stream); maybe_media_feature.has_value()) {
            tokens.next_token();
            transaction.commit();
            return MediaCondition::from_feature(maybe_media_feature.release_value());
        }
    }

    // `<general-enclosed>`
    // FIXME: We should only be taking this branch if the grammar doesn't match the above options.
    //        Currently we take it if the above fail to parse, which is different.
    //        eg, `@media (min-width: 76yaks)` is valid grammar, but does not parse because `yaks` isn't a unit.
    if (auto maybe_general_enclosed = parse_general_enclosed(tokens); maybe_general_enclosed.has_value()) {
        transaction.commit();
        return MediaCondition::from_general_enclosed(maybe_general_enclosed.release_value());
    }

    return {};
}

// `<mf-value>`, https://www.w3.org/TR/mediaqueries-4/#typedef-mf-value
Optional<MediaFeatureValue> Parser::parse_media_feature_value(MediaFeatureID media_feature, TokenStream<ComponentValue>& tokens)
{
    // Identifiers
    if (tokens.peek_token().is(Token::Type::Ident)) {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        auto ident = value_id_from_string(tokens.next_token().token().ident());
        if (ident != ValueID::Invalid && media_feature_accepts_identifier(media_feature, ident)) {
            transaction.commit();
            return MediaFeatureValue(ident);
        }
    }

    // One branch for each member of the MediaFeatureValueType enum:

    // Boolean (<mq-boolean> in the spec: a 1 or 0)
    if (media_feature_accepts_type(media_feature, MediaFeatureValueType::Boolean)) {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        auto const& first = tokens.next_token();
        if (first.is(Token::Type::Number) && first.token().number().is_integer()
            && (first.token().number_value() == 0 || first.token().number_value() == 1)) {
            transaction.commit();
            return MediaFeatureValue(first.token().number_value());
        }
    }

    // Integer
    if (media_feature_accepts_type(media_feature, MediaFeatureValueType::Integer)) {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        auto const& first = tokens.next_token();
        if (first.is(Token::Type::Number) && first.token().number().is_integer()) {
            transaction.commit();
            return MediaFeatureValue(first.token().number_value());
        }
    }

    // Length
    if (media_feature_accepts_type(media_feature, MediaFeatureValueType::Length)) {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        auto const& first = tokens.next_token();
        if (auto length = parse_length(first); length.has_value()) {
            transaction.commit();
            return MediaFeatureValue(length.release_value());
        }
    }

    // Ratio
    if (media_feature_accepts_type(media_feature, MediaFeatureValueType::Ratio)) {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        if (auto ratio = parse_ratio(tokens); ratio.has_value()) {
            transaction.commit();
            return MediaFeatureValue(ratio.release_value());
        }
    }

    // Resolution
    if (media_feature_accepts_type(media_feature, MediaFeatureValueType::Resolution)) {
        auto transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        auto const& first = tokens.next_token();
        if (auto resolution = parse_dimension(first); resolution.has_value() && resolution->is_resolution()) {
            transaction.commit();
            return MediaFeatureValue(resolution->resolution());
        }
    }

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
    TokenStream<ComponentValue> token_stream { component_values };
    auto maybe_condition = parse_supports_condition(token_stream);
    token_stream.skip_whitespace();
    if (maybe_condition && !token_stream.has_next_token())
        return Supports::create(maybe_condition.release_nonnull());

    return {};
}

OwnPtr<Supports::Condition> Parser::parse_supports_condition(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();

    auto const& peeked_token = tokens.peek_token();
    // `not <supports-in-parens>`
    if (peeked_token.is(Token::Type::Ident) && peeked_token.token().ident().equals_ignoring_case("not"sv)) {
        tokens.next_token();
        tokens.skip_whitespace();
        auto child = parse_supports_in_parens(tokens);
        if (!child.has_value())
            return {};

        transaction.commit();
        auto condition = make<Supports::Condition>();
        condition->type = Supports::Condition::Type::Not;
        condition->children.append(child.release_value());
        return condition;
    }

    // `  <supports-in-parens> [ and <supports-in-parens> ]*
    //  | <supports-in-parens> [ or <supports-in-parens> ]*`
    Vector<Supports::InParens> children;
    Optional<Supports::Condition::Type> condition_type {};
    auto as_condition_type = [](auto& token) -> Optional<Supports::Condition::Type> {
        if (!token.is(Token::Type::Ident))
            return {};
        auto ident = token.token().ident();
        if (ident.equals_ignoring_case("and"sv))
            return Supports::Condition::Type::And;
        if (ident.equals_ignoring_case("or"sv))
            return Supports::Condition::Type::Or;
        return {};
    };

    while (tokens.has_next_token()) {
        if (!children.is_empty()) {
            // Expect `and` or `or` here
            auto maybe_combination = as_condition_type(tokens.next_token());
            if (!maybe_combination.has_value())
                return {};
            if (!condition_type.has_value()) {
                condition_type = maybe_combination.value();
            } else if (maybe_combination != condition_type) {
                return {};
            }
        }

        tokens.skip_whitespace();

        if (auto in_parens = parse_supports_in_parens(tokens); in_parens.has_value()) {
            children.append(in_parens.release_value());
        } else {
            return {};
        }

        tokens.skip_whitespace();
    }

    if (children.is_empty())
        return {};

    transaction.commit();
    auto condition = make<Supports::Condition>();
    condition->type = condition_type.value_or(Supports::Condition::Type::Or);
    condition->children = move(children);
    return condition;
}

Optional<Supports::InParens> Parser::parse_supports_in_parens(TokenStream<ComponentValue>& tokens)
{
    // `( <supports-condition> )`
    auto const& first_token = tokens.peek_token();
    if (first_token.is_block() && first_token.block().is_paren()) {
        auto transaction = tokens.begin_transaction();
        tokens.next_token();
        tokens.skip_whitespace();

        TokenStream child_tokens { first_token.block().values() };
        if (auto condition = parse_supports_condition(child_tokens)) {
            if (child_tokens.has_next_token())
                return {};
            transaction.commit();
            return Supports::InParens {
                .value = { condition.release_nonnull() }
            };
        }
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

    return {};
}

Optional<Supports::Feature> Parser::parse_supports_feature(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();
    auto const& first_token = tokens.next_token();

    // `<supports-decl>`
    if (first_token.is_block() && first_token.block().is_paren()) {
        TokenStream block_tokens { first_token.block().values() };
        // FIXME: Parsing and then converting back to a string is weird.
        if (auto declaration = consume_a_declaration(block_tokens); declaration.has_value()) {
            transaction.commit();
            return Supports::Feature {
                Supports::Declaration { declaration->to_string() }
            };
        }
    }

    // `<supports-selector-fn>`
    if (first_token.is_function() && first_token.function().name().equals_ignoring_case("selector"sv)) {
        // FIXME: Parsing and then converting back to a string is weird.
        StringBuilder builder;
        for (auto const& item : first_token.function().values())
            builder.append(item.to_string());
        transaction.commit();
        return Supports::Feature {
            Supports::Selector { builder.to_string() }
        };
    }

    return {};
}

// https://www.w3.org/TR/mediaqueries-4/#typedef-general-enclosed
Optional<GeneralEnclosed> Parser::parse_general_enclosed(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();
    auto const& first_token = tokens.next_token();

    // `[ <function-token> <any-value>? ) ]`
    if (first_token.is_function()) {
        transaction.commit();
        return GeneralEnclosed { first_token.to_string() };
    }

    // `( <any-value>? )`
    if (first_token.is_block() && first_token.block().is_paren()) {
        transaction.commit();
        return GeneralEnclosed { first_token.to_string() };
    }

    return {};
}

// 5.4.1. Consume a list of rules
// https://www.w3.org/TR/css-syntax-3/#consume-list-of-rules
template<typename T>
NonnullRefPtrVector<Rule> Parser::consume_a_list_of_rules(TokenStream<T>& tokens, TopLevel top_level)
{
    // To consume a list of rules, given a top-level flag:

    // Create an initially empty list of rules.
    NonnullRefPtrVector<Rule> rules;

    // Repeatedly consume the next input token:
    for (;;) {
        auto& token = tokens.next_token();

        // <whitespace-token>
        if (token.is(Token::Type::Whitespace)) {
            // Do nothing.
            continue;
        }

        // <EOF-token>
        if (token.is(Token::Type::EndOfFile)) {
            // Return the list of rules.
            return rules;
        }

        // <CDO-token>
        // <CDC-token>
        if (token.is(Token::Type::CDO) || token.is(Token::Type::CDC)) {
            // If the top-level flag is set, do nothing.
            if (top_level == TopLevel::Yes)
                continue;

            // Otherwise, reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume a qualified rule. If anything is returned, append it to the list of rules.
            if (auto maybe_qualified = consume_a_qualified_rule(tokens))
                rules.append(maybe_qualified.release_nonnull());

            continue;
        }

        // <at-keyword-token>
        if (token.is(Token::Type::AtKeyword)) {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume an at-rule, and append the returned value to the list of rules.
            rules.append(consume_an_at_rule(tokens));

            continue;
        }

        // anything else
        {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume a qualified rule. If anything is returned, append it to the list of rules.
            if (auto maybe_qualified = consume_a_qualified_rule(tokens))
                rules.append(maybe_qualified.release_nonnull());

            continue;
        }
    }
}

// 5.4.2. Consume an at-rule
// https://www.w3.org/TR/css-syntax-3/#consume-at-rule
template<typename T>
NonnullRefPtr<Rule> Parser::consume_an_at_rule(TokenStream<T>& tokens)
{
    // To consume an at-rule:

    // Consume the next input token.
    auto& name_ident = tokens.next_token();
    VERIFY(name_ident.is(Token::Type::AtKeyword));

    // Create a new at-rule with its name set to the value of the current input token, its prelude initially set to an empty list, and its value initially set to nothing.
    // NOTE: We create the Rule fully initialized when we return it instead.
    FlyString at_rule_name = ((Token)name_ident).at_keyword();
    Vector<ComponentValue> prelude;
    RefPtr<Block> block;

    // Repeatedly consume the next input token:
    for (;;) {
        auto& token = tokens.next_token();

        // <semicolon-token>
        if (token.is(Token::Type::Semicolon)) {
            // Return the at-rule.
            return Rule::make_at_rule(move(at_rule_name), move(prelude), move(block));
        }

        // <EOF-token>
        if (token.is(Token::Type::EndOfFile)) {
            // This is a parse error. Return the at-rule.
            log_parse_error();
            return Rule::make_at_rule(move(at_rule_name), move(prelude), move(block));
        }

        // <{-token>
        if (token.is(Token::Type::OpenCurly)) {
            // Consume a simple block and assign it to the at-rule’s block. Return the at-rule.
            block = consume_a_simple_block(tokens);
            return Rule::make_at_rule(move(at_rule_name), move(prelude), move(block));
        }

        // simple block with an associated token of <{-token>
        if constexpr (IsSame<T, ComponentValue>) {
            ComponentValue const& component_value = token;
            if (component_value.is_block() && component_value.block().is_curly()) {
                // Assign the block to the at-rule’s block. Return the at-rule.
                block = component_value.block();
                return Rule::make_at_rule(move(at_rule_name), move(prelude), move(block));
            }
        }

        // anything else
        {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();
            // Consume a component value. Append the returned value to the at-rule’s prelude.
            prelude.append(consume_a_component_value(tokens));
        }
    }
}

// 5.4.3. Consume a qualified rule
// https://www.w3.org/TR/css-syntax-3/#consume-qualified-rule
template<typename T>
RefPtr<Rule> Parser::consume_a_qualified_rule(TokenStream<T>& tokens)
{
    // To consume a qualified rule:

    // Create a new qualified rule with its prelude initially set to an empty list, and its value initially set to nothing.
    // NOTE: We create the Rule fully initialized when we return it instead.
    Vector<ComponentValue> prelude;
    RefPtr<Block> block;

    // Repeatedly consume the next input token:
    for (;;) {
        auto& token = tokens.next_token();

        // <EOF-token>
        if (token.is(Token::Type::EndOfFile)) {
            // This is a parse error. Return nothing.
            log_parse_error();
            return {};
        }

        // <{-token>
        if (token.is(Token::Type::OpenCurly)) {
            // Consume a simple block and assign it to the qualified rule’s block. Return the qualified rule.
            block = consume_a_simple_block(tokens);
            return Rule::make_qualified_rule(move(prelude), move(block));
        }

        // simple block with an associated token of <{-token>
        if constexpr (IsSame<T, ComponentValue>) {
            ComponentValue const& component_value = token;
            if (component_value.is_block() && component_value.block().is_curly()) {
                // Assign the block to the qualified rule’s block. Return the qualified rule.
                block = component_value.block();
                return Rule::make_qualified_rule(move(prelude), move(block));
            }
        }

        // anything else
        {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume a component value. Append the returned value to the qualified rule’s prelude.
            prelude.append(consume_a_component_value(tokens));
        }
    }
}

// 5.4.4. Consume a style block’s contents
// https://www.w3.org/TR/css-syntax-3/#consume-a-style-blocks-contents
template<typename T>
Vector<DeclarationOrAtRule> Parser::consume_a_style_blocks_contents(TokenStream<T>& tokens)
{
    // To consume a style block’s contents:
    // Create an initially empty list of declarations decls, and an initially empty list of rules rules.
    Vector<DeclarationOrAtRule> declarations;
    Vector<DeclarationOrAtRule> rules;

    // Repeatedly consume the next input token:
    for (;;) {
        auto& token = tokens.next_token();

        // <whitespace-token>
        // <semicolon-token>
        if (token.is(Token::Type::Whitespace) || token.is(Token::Type::Semicolon)) {
            // Do nothing.
            continue;
        }

        // <EOF-token>
        if (token.is(Token::Type::EndOfFile)) {
            // Extend decls with rules, then return decls.
            declarations.extend(move(rules));
            return declarations;
        }

        // <at-keyword-token>
        if (token.is(Token::Type::AtKeyword)) {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume an at-rule, and append the result to rules.
            rules.empend(consume_an_at_rule(tokens));
            continue;
        }

        // <ident-token>
        if (token.is(Token::Type::Ident)) {
            // Initialize a temporary list initially filled with the current input token.
            Vector<ComponentValue> temporary_list;
            temporary_list.append(token);

            // As long as the next input token is anything other than a <semicolon-token> or <EOF-token>,
            // consume a component value and append it to the temporary list.
            for (;;) {
                auto& next_input_token = tokens.peek_token();
                if (next_input_token.is(Token::Type::Semicolon) || next_input_token.is(Token::Type::EndOfFile))
                    break;
                temporary_list.append(consume_a_component_value(tokens));
            }

            // Consume a declaration from the temporary list. If anything was returned, append it to decls.
            auto token_stream = TokenStream(temporary_list);
            if (auto maybe_declaration = consume_a_declaration(token_stream); maybe_declaration.has_value())
                declarations.empend(maybe_declaration.release_value());

            continue;
        }

        // <delim-token> with a value of "&" (U+0026 AMPERSAND)
        if (token.is(Token::Type::Delim) && token.token().delim() == '&') {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume a qualified rule. If anything was returned, append it to rules.
            if (auto qualified_rule = consume_a_qualified_rule(tokens))
                rules.empend(qualified_rule);

            continue;
        }

        // anything else
        {
            // This is a parse error.
            log_parse_error();

            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // As long as the next input token is anything other than a <semicolon-token> or <EOF-token>,
            // consume a component value and throw away the returned value.
            for (;;) {
                auto& peek = tokens.peek_token();
                if (peek.is(Token::Type::Semicolon) || peek.is(Token::Type::EndOfFile))
                    break;
                (void)consume_a_component_value(tokens);
            }
        }
    }
}

template<>
ComponentValue Parser::consume_a_component_value(TokenStream<ComponentValue>& tokens)
{
    // Note: This overload is called once tokens have already been converted into component values,
    //       so we do not need to do the work in the more general overload.
    return tokens.next_token();
}

// 5.4.7. Consume a component value
// https://www.w3.org/TR/css-syntax-3/#consume-component-value
template<typename T>
ComponentValue Parser::consume_a_component_value(TokenStream<T>& tokens)
{
    // To consume a component value:

    // Consume the next input token.
    auto& token = tokens.next_token();

    // If the current input token is a <{-token>, <[-token>, or <(-token>, consume a simple block and return it.
    if (token.is(Token::Type::OpenCurly) || token.is(Token::Type::OpenSquare) || token.is(Token::Type::OpenParen))
        return ComponentValue(consume_a_simple_block(tokens));

    // Otherwise, if the current input token is a <function-token>, consume a function and return it.
    if (token.is(Token::Type::Function))
        return ComponentValue(consume_a_function(tokens));

    // Otherwise, return the current input token.
    return ComponentValue(token);
}

// 5.4.8. Consume a simple block
// https://www.w3.org/TR/css-syntax-3/#consume-simple-block
template<typename T>
NonnullRefPtr<Block> Parser::consume_a_simple_block(TokenStream<T>& tokens)
{
    // Note: This algorithm assumes that the current input token has already been checked
    // to be an <{-token>, <[-token>, or <(-token>.

    // To consume a simple block:

    // The ending token is the mirror variant of the current input token.
    // (E.g. if it was called with <[-token>, the ending token is <]-token>.)
    auto ending_token = ((Token)tokens.current_token()).mirror_variant();

    // Create a simple block with its associated token set to the current input token
    // and with its value initially set to an empty list.
    // NOTE: We create the Block fully initialized when we return it instead.
    Token block_token = tokens.current_token();
    Vector<ComponentValue> block_values;

    // Repeatedly consume the next input token and process it as follows:
    for (;;) {
        auto& token = tokens.next_token();

        // ending token
        if (token.is(ending_token)) {
            // Return the block.
            return Block::create(move(block_token), move(block_values));
        }
        // <EOF-token>
        if (token.is(Token::Type::EndOfFile)) {
            // This is a parse error. Return the block.
            log_parse_error();
            return Block::create(move(block_token), move(block_values));
        }

        // anything else
        {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume a component value and append it to the value of the block.
            block_values.empend(consume_a_component_value(tokens));
        }
    }
}

// 5.4.9. Consume a function
// https://www.w3.org/TR/css-syntax-3/#consume-function
template<typename T>
NonnullRefPtr<Function> Parser::consume_a_function(TokenStream<T>& tokens)
{
    // Note: This algorithm assumes that the current input token has already been checked to be a <function-token>.
    auto name_ident = tokens.current_token();
    VERIFY(name_ident.is(Token::Type::Function));

    // To consume a function:

    // Create a function with its name equal to the value of the current input token
    // and with its value initially set to an empty list.
    // NOTE: We create the Function fully initialized when we return it instead.
    FlyString function_name = ((Token)name_ident).function();
    Vector<ComponentValue> function_values;

    // Repeatedly consume the next input token and process it as follows:
    for (;;) {
        auto& token = tokens.next_token();

        // <)-token>
        if (token.is(Token::Type::CloseParen)) {
            // Return the function.
            return Function::create(move(function_name), move(function_values));
        }

        // <EOF-token>
        if (token.is(Token::Type::EndOfFile)) {
            // This is a parse error. Return the function.
            log_parse_error();
            return Function::create(move(function_name), move(function_values));
        }

        // anything else
        {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume a component value and append the returned value to the function’s value.
            function_values.append(consume_a_component_value(tokens));
        }
    }
}

// 5.4.6. Consume a declaration
// https://www.w3.org/TR/css-syntax-3/#consume-declaration
template<typename T>
Optional<Declaration> Parser::consume_a_declaration(TokenStream<T>& tokens)
{
    // Note: This algorithm assumes that the next input token has already been checked to
    // be an <ident-token>.
    // NOTE: This is not true in our implementation! For convenience, we both skip whitespace
    //       and gracefully handle the first token not being an <ident-token>.

    // To consume a declaration:

    // Consume the next input token.
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();
    auto& token = tokens.next_token();

    // NOTE: Not to spec, handle the case where the input token *isn't* an <ident-token>.
    if (!token.is(Token::Type::Ident))
        return {};

    // Create a new declaration with its name set to the value of the current input token
    // and its value initially set to the empty list.
    // NOTE: We create a fully-initialized Declaration just before returning it instead.
    FlyString declaration_name = ((Token)token).ident();
    Vector<ComponentValue> declaration_values;
    Important declaration_important = Important::No;

    // 1. While the next input token is a <whitespace-token>, consume the next input token.
    tokens.skip_whitespace();

    // 2. If the next input token is anything other than a <colon-token>, this is a parse error.
    // Return nothing.
    auto& maybe_colon = tokens.peek_token();
    if (!maybe_colon.is(Token::Type::Colon)) {
        log_parse_error();
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
        declaration_values.append(consume_a_component_value(tokens));
    }

    // 5. If the last two non-<whitespace-token>s in the declaration’s value are a <delim-token>
    //    with the value "!" followed by an <ident-token> with a value that is an ASCII case-insensitive
    //    match for "important", remove them from the declaration’s value and set the declaration’s
    //    important flag to true.
    if (declaration_values.size() >= 2) {
        // Walk backwards from the end until we find "important"
        Optional<size_t> important_index;
        for (size_t i = declaration_values.size() - 1; i > 0; i--) {
            auto value = declaration_values[i];
            if (value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("important"sv)) {
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
                auto value = declaration_values[i];
                if (value.is(Token::Type::Delim) && value.token().delim() == '!') {
                    bang_index = i;
                    break;
                }
                if (value.is(Token::Type::Whitespace))
                    continue;
                break;
            }

            if (bang_index.has_value()) {
                declaration_values.remove(important_index.value());
                declaration_values.remove(bang_index.value());
                declaration_important = Important::Yes;
            }
        }
    }

    // 6. While the last token in the declaration’s value is a <whitespace-token>, remove that token.
    while (!declaration_values.is_empty()) {
        auto maybe_whitespace = declaration_values.last();
        if (!(maybe_whitespace.is(Token::Type::Whitespace))) {
            break;
        }
        declaration_values.take_last();
    }

    // 7. Return the declaration.
    transaction.commit();
    return Declaration { move(declaration_name), move(declaration_values), declaration_important };
}

// 5.4.5. Consume a list of declarations
// https://www.w3.org/TR/css-syntax-3/#consume-list-of-declarations
template<typename T>
Vector<DeclarationOrAtRule> Parser::consume_a_list_of_declarations(TokenStream<T>& tokens)
{
    // To consume a list of declarations:

    // Create an initially empty list of declarations.
    Vector<DeclarationOrAtRule> list_of_declarations;

    // Repeatedly consume the next input token:
    for (;;) {
        auto& token = tokens.next_token();

        // <whitespace-token>
        // <semicolon-token>
        if (token.is(Token::Type::Whitespace) || token.is(Token::Type::Semicolon)) {
            // Do nothing.
            continue;
        }

        // <EOF-token>
        if (token.is(Token::Type::EndOfFile)) {
            // Return the list of declarations.
            return list_of_declarations;
        }

        // <at-keyword-token>
        if (token.is(Token::Type::AtKeyword)) {
            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // Consume an at-rule. Append the returned rule to the list of declarations.
            list_of_declarations.empend(consume_an_at_rule(tokens));
            continue;
        }

        // <ident-token>
        if (token.is(Token::Type::Ident)) {
            // Initialize a temporary list initially filled with the current input token.
            Vector<ComponentValue> temporary_list;
            temporary_list.append(token);

            // As long as the next input token is anything other than a <semicolon-token> or <EOF-token>,
            // consume a component value and append it to the temporary list.
            for (;;) {
                auto& peek = tokens.peek_token();
                if (peek.is(Token::Type::Semicolon) || peek.is(Token::Type::EndOfFile))
                    break;
                temporary_list.append(consume_a_component_value(tokens));
            }

            // Consume a declaration from the temporary list. If anything was returned, append it to the list of declarations.
            auto token_stream = TokenStream(temporary_list);
            if (auto maybe_declaration = consume_a_declaration(token_stream); maybe_declaration.has_value())
                list_of_declarations.empend(maybe_declaration.value());

            continue;
        }

        // anything else
        {
            // This is a parse error.
            log_parse_error();

            // Reconsume the current input token.
            tokens.reconsume_current_input_token();

            // As long as the next input token is anything other than a <semicolon-token> or <EOF-token>,
            // consume a component value and throw away the returned value.
            for (;;) {
                auto& peek = tokens.peek_token();
                if (peek.is(Token::Type::Semicolon) || peek.is(Token::Type::EndOfFile))
                    break;
                dbgln_if(CSS_PARSER_DEBUG, "Discarding token: '{}'", peek.to_debug_string());
                (void)consume_a_component_value(tokens);
            }
        }
    }
}

CSSRule* Parser::parse_as_css_rule()
{
    auto maybe_rule = parse_a_rule(m_token_stream);
    if (maybe_rule)
        return convert_to_rule(maybe_rule.release_nonnull());
    return {};
}

// 5.3.5. Parse a rule
// https://www.w3.org/TR/css-syntax-3/#parse-rule
template<typename T>
RefPtr<Rule> Parser::parse_a_rule(TokenStream<T>& tokens)
{
    // To parse a rule from input:
    RefPtr<Rule> rule;

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. While the next input token from input is a <whitespace-token>, consume the next input token from input.
    tokens.skip_whitespace();

    // 3. If the next input token from input is an <EOF-token>, return a syntax error.
    auto& token = tokens.peek_token();
    if (token.is(Token::Type::EndOfFile)) {
        return {};
    }
    // Otherwise, if the next input token from input is an <at-keyword-token>, consume an at-rule from input, and let rule be the return value.
    else if (token.is(Token::Type::AtKeyword)) {
        rule = consume_an_at_rule(m_token_stream);
    }
    // Otherwise, consume a qualified rule from input and let rule be the return value. If nothing was returned, return a syntax error.
    else {
        auto qualified_rule = consume_a_qualified_rule(tokens);
        if (!qualified_rule)
            return {};

        rule = qualified_rule;
    }

    // 4. While the next input token from input is a <whitespace-token>, consume the next input token from input.
    tokens.skip_whitespace();

    // 5. If the next input token from input is an <EOF-token>, return rule. Otherwise, return a syntax error.
    if (tokens.peek_token().is(Token::Type::EndOfFile))
        return rule;
    return {};
}

// 5.3.4. Parse a list of rules
// https://www.w3.org/TR/css-syntax-3/#parse-list-of-rules
template<typename T>
NonnullRefPtrVector<Rule> Parser::parse_a_list_of_rules(TokenStream<T>& tokens)
{
    // To parse a list of rules from input:

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. Consume a list of rules from the input, with the top-level flag unset.
    auto list_of_rules = consume_a_list_of_rules(tokens, TopLevel::No);

    // 3. Return the returned list.
    return list_of_rules;
}

Optional<StyleProperty> Parser::parse_as_supports_condition()
{
    auto maybe_declaration = parse_a_declaration(m_token_stream);
    if (maybe_declaration.has_value())
        return convert_to_style_property(maybe_declaration.release_value());
    return {};
}

// 5.3.6. Parse a declaration
// https://www.w3.org/TR/css-syntax-3/#parse-a-declaration
template<typename T>
Optional<Declaration> Parser::parse_a_declaration(TokenStream<T>& tokens)
{
    // To parse a declaration from input:

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. While the next input token from input is a <whitespace-token>, consume the next input token.
    tokens.skip_whitespace();

    // 3. If the next input token from input is not an <ident-token>, return a syntax error.
    auto& token = tokens.peek_token();
    if (!token.is(Token::Type::Ident)) {
        return {};
    }

    // 4. Consume a declaration from input. If anything was returned, return it. Otherwise, return a syntax error.
    if (auto declaration = consume_a_declaration(tokens); declaration.has_value())
        return declaration.release_value();
    return {};
}

// 5.3.7. Parse a style block’s contents
// https://www.w3.org/TR/css-syntax-3/#parse-style-blocks-contents
template<typename T>
Vector<DeclarationOrAtRule> Parser::parse_a_style_blocks_contents(TokenStream<T>& tokens)
{
    // To parse a style block’s contents from input:

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. Consume a style block’s contents from input, and return the result.
    return consume_a_style_blocks_contents(tokens);
}

// 5.3.8. Parse a list of declarations
// https://www.w3.org/TR/css-syntax-3/#parse-list-of-declarations
template<typename T>
Vector<DeclarationOrAtRule> Parser::parse_a_list_of_declarations(TokenStream<T>& tokens)
{
    // To parse a list of declarations from input:

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. Consume a list of declarations from input, and return the result.
    return consume_a_list_of_declarations(tokens);
}

// 5.3.9. Parse a component value
// https://www.w3.org/TR/css-syntax-3/#parse-component-value
template<typename T>
Optional<ComponentValue> Parser::parse_a_component_value(TokenStream<T>& tokens)
{
    // To parse a component value from input:

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. While the next input token from input is a <whitespace-token>, consume the next input token from input.
    tokens.skip_whitespace();

    // 3. If the next input token from input is an <EOF-token>, return a syntax error.
    if (tokens.peek_token().is(Token::Type::EndOfFile))
        return {};

    // 4. Consume a component value from input and let value be the return value.
    auto value = consume_a_component_value(tokens);

    // 5. While the next input token from input is a <whitespace-token>, consume the next input token.
    tokens.skip_whitespace();

    // 6. If the next input token from input is an <EOF-token>, return value. Otherwise, return a syntax error.
    if (tokens.peek_token().is(Token::Type::EndOfFile))
        return value;
    return {};
}

// 5.3.10. Parse a list of component values
// https://www.w3.org/TR/css-syntax-3/#parse-list-of-component-values
template<typename T>
Vector<ComponentValue> Parser::parse_a_list_of_component_values(TokenStream<T>& tokens)
{
    // To parse a list of component values from input:

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. Repeatedly consume a component value from input until an <EOF-token> is returned, appending the returned values (except the final <EOF-token>) into a list. Return the list.
    Vector<ComponentValue> component_values;

    for (;;) {
        if (tokens.peek_token().is(Token::Type::EndOfFile)) {
            break;
        }

        component_values.append(consume_a_component_value(tokens));
    }

    return component_values;
}

// 5.3.11. Parse a comma-separated list of component values
// https://www.w3.org/TR/css-syntax-3/#parse-comma-separated-list-of-component-values
template<typename T>
Vector<Vector<ComponentValue>> Parser::parse_a_comma_separated_list_of_component_values(TokenStream<T>& tokens)
{
    // To parse a comma-separated list of component values from input:

    // 1. Normalize input, and set input to the result.
    // Note: This is done when initializing the Parser.

    // 2. Let list of cvls be an initially empty list of component value lists.
    Vector<Vector<ComponentValue>> list_of_component_value_lists;

    // 3. Repeatedly consume a component value from input until an <EOF-token> or <comma-token> is returned,
    //    appending the returned values (except the final <EOF-token> or <comma-token>) into a list.
    //    Append the list to list of cvls.
    //    If it was a <comma-token> that was returned, repeat this step.
    Vector<ComponentValue> current_list;
    for (;;) {
        auto component_value = consume_a_component_value(tokens);

        if (component_value.is(Token::Type::EndOfFile)) {
            list_of_component_value_lists.append(move(current_list));
            break;
        }
        if (component_value.is(Token::Type::Comma)) {
            list_of_component_value_lists.append(move(current_list));
            current_list = {};
            continue;
        }

        current_list.append(component_value);
    }

    // 4. Return list of cvls.
    return list_of_component_value_lists;
}

ElementInlineCSSStyleDeclaration* Parser::parse_as_style_attribute(DOM::Element& element)
{
    auto declarations_and_at_rules = parse_a_list_of_declarations(m_token_stream);
    auto [properties, custom_properties] = extract_properties(declarations_and_at_rules);
    return ElementInlineCSSStyleDeclaration::create(element, move(properties), move(custom_properties));
}

Optional<AK::URL> Parser::parse_url_function(ComponentValue const& component_value, AllowedDataUrlType allowed_data_url_type)
{
    // FIXME: Handle list of media queries. https://www.w3.org/TR/css-cascade-3/#conditional-import
    // FIXME: Handle data: urls (RFC2397)

    auto convert_string_to_url = [&](StringView& url_string) -> Optional<AK::URL> {
        if (url_string.starts_with("data:"sv, CaseSensitivity::CaseInsensitive)) {
            auto data_url = AK::URL(url_string);

            switch (allowed_data_url_type) {
            case AllowedDataUrlType::Image:
                if (data_url.data_mime_type().starts_with("image"sv, CaseSensitivity::CaseInsensitive))
                    return data_url;
                break;
            case AllowedDataUrlType::Font:
                if (data_url.data_mime_type().starts_with("font"sv, CaseSensitivity::CaseInsensitive))
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
    if (component_value.is_function() && component_value.function().name().equals_ignoring_case("url"sv)) {
        auto const& function_values = component_value.function().values();
        // FIXME: Handle url-modifiers. https://www.w3.org/TR/css-values-4/#url-modifiers
        for (size_t i = 0; i < function_values.size(); ++i) {
            auto const& value = function_values[i];
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

RefPtr<StyleValue> Parser::parse_linear_gradient_function(ComponentValue const& component_value)
{
    using GradientType = LinearGradientStyleValue::GradientType;
    using Repeating = LinearGradientStyleValue::Repeating;

    if (!component_value.is_function())
        return {};

    auto consume_if_starts_with = [](StringView str, StringView start, auto found_callback) {
        if (str.starts_with(start, CaseSensitivity::CaseInsensitive)) {
            found_callback();
            return str.substring_view(start.length());
        }
        return str;
    };

    Repeating repeating_gradient = Repeating::No;
    GradientType gradient_type { GradientType::Standard };

    auto function_name = component_value.function().name();

    function_name = consume_if_starts_with(function_name, "-webkit-"sv, [&] {
        gradient_type = GradientType::WebKit;
    });

    function_name = consume_if_starts_with(function_name, "repeating-"sv, [&] {
        repeating_gradient = Repeating::Yes;
    });

    if (!function_name.equals_ignoring_case("linear-gradient"sv))
        return {};

    // linear-gradient() = linear-gradient([ <angle> | to <side-or-corner> ]?, <color-stop-list>)

    TokenStream tokens { component_value.function().values() };
    tokens.skip_whitespace();

    if (!tokens.has_next_token())
        return {};

    bool has_direction_param = true;
    LinearGradientStyleValue::GradientDirection gradient_direction = gradient_type == GradientType::Standard
        ? SideOrCorner::Bottom
        : SideOrCorner::Top;

    auto to_side = [](StringView value) -> Optional<SideOrCorner> {
        if (value.equals_ignoring_case("top"sv))
            return SideOrCorner::Top;
        if (value.equals_ignoring_case("bottom"sv))
            return SideOrCorner::Bottom;
        if (value.equals_ignoring_case("left"sv))
            return SideOrCorner::Left;
        if (value.equals_ignoring_case("right"sv))
            return SideOrCorner::Right;
        return {};
    };

    auto is_to_side_or_corner = [&](auto const& token) {
        if (!token.is(Token::Type::Ident))
            return false;
        if (gradient_type == GradientType::WebKit)
            return to_side(token.token().ident()).has_value();
        return token.token().ident().equals_ignoring_case("to"sv);
    };

    auto const& first_param = tokens.peek_token();
    if (first_param.is(Token::Type::Dimension)) {
        // <angle>
        tokens.next_token();
        float angle_value = first_param.token().dimension_value();
        auto unit_string = first_param.token().dimension_unit();
        auto angle_type = Angle::unit_from_name(unit_string);

        if (!angle_type.has_value())
            return {};

        gradient_direction = Angle { angle_value, angle_type.release_value() };
    } else if (is_to_side_or_corner(first_param)) {
        // <side-or-corner> = [left | right] || [top | bottom]

        // Note: -webkit-linear-gradient does not include to the "to" prefix on the side or corner
        if (gradient_type == GradientType::Standard) {
            tokens.next_token();
            tokens.skip_whitespace();

            if (!tokens.has_next_token())
                return {};
        }

        // [left | right] || [top | bottom]
        auto const& first_side = tokens.next_token();
        if (!first_side.is(Token::Type::Ident))
            return {};

        auto side_a = to_side(first_side.token().ident());
        tokens.skip_whitespace();
        Optional<SideOrCorner> side_b;
        if (tokens.has_next_token() && tokens.peek_token().is(Token::Type::Ident))
            side_b = to_side(tokens.next_token().token().ident());

        if (side_a.has_value() && !side_b.has_value()) {
            gradient_direction = *side_a;
        } else if (side_a.has_value() && side_b.has_value()) {
            // Convert two sides to a corner
            if (to_underlying(*side_b) < to_underlying(*side_a))
                swap(side_a, side_b);
            if (side_a == SideOrCorner::Top && side_b == SideOrCorner::Left)
                gradient_direction = SideOrCorner::TopLeft;
            else if (side_a == SideOrCorner::Top && side_b == SideOrCorner::Right)
                gradient_direction = SideOrCorner::TopRight;
            else if (side_a == SideOrCorner::Bottom && side_b == SideOrCorner::Left)
                gradient_direction = SideOrCorner::BottomLeft;
            else if (side_a == SideOrCorner::Bottom && side_b == SideOrCorner::Right)
                gradient_direction = SideOrCorner::BottomRight;
            else
                return {};
        } else {
            return {};
        }
    } else {
        has_direction_param = false;
    }

    tokens.skip_whitespace();
    if (!tokens.has_next_token())
        return {};

    if (has_direction_param && !tokens.next_token().is(Token::Type::Comma))
        return {};

    // <color-stop-list> =
    //      <linear-color-stop> , [ <linear-color-hint>? , <linear-color-stop> ]#

    enum class ElementType {
        Garbage,
        ColorStop,
        ColorHint
    };

    auto parse_color_stop_list_element = [&](ColorStopListElement& element) -> ElementType {
        tokens.skip_whitespace();
        if (!tokens.has_next_token())
            return ElementType::Garbage;
        auto const& token = tokens.next_token();

        Gfx::Color color;
        Optional<LengthPercentage> position;
        Optional<LengthPercentage> second_position;
        auto dimension = parse_dimension(token);
        if (dimension.has_value() && dimension->is_length_percentage()) {
            // [<length-percentage> <color>] or [<length-percentage>]
            position = dimension->length_percentage();
            tokens.skip_whitespace();
            // <length-percentage>
            if (!tokens.has_next_token() || tokens.peek_token().is(Token::Type::Comma)) {
                element.transition_hint = GradientColorHint { *position };
                return ElementType::ColorHint;
            }
            // <length-percentage> <color>
            auto maybe_color = parse_color(tokens.next_token());
            if (!maybe_color.has_value())
                return ElementType::Garbage;
            color = *maybe_color;
        } else {
            // [<color> <length-percentage>?]
            auto maybe_color = parse_color(token);
            if (!maybe_color.has_value())
                return ElementType::Garbage;
            color = *maybe_color;
            tokens.skip_whitespace();
            // Allow up to [<color> <length-percentage> <length-percentage>] (double-position color stops)
            // Note: Double-position color stops only appear to be valid in this order.
            for (auto stop_position : Array { &position, &second_position }) {
                if (tokens.has_next_token() && !tokens.peek_token().is(Token::Type::Comma)) {
                    auto token = tokens.next_token();
                    auto dimension = parse_dimension(token);
                    if (!dimension.has_value() || !dimension->is_length_percentage())
                        return ElementType::Garbage;
                    *stop_position = dimension->length_percentage();
                    tokens.skip_whitespace();
                }
            }
        }

        element.color_stop = GradientColorStop { color, position, second_position };
        return ElementType::ColorStop;
    };

    ColorStopListElement first_element {};
    if (parse_color_stop_list_element(first_element) != ElementType::ColorStop)
        return {};

    if (!tokens.has_next_token())
        return {};

    Vector<ColorStopListElement> color_stops { first_element };
    while (tokens.has_next_token()) {
        ColorStopListElement list_element {};
        tokens.skip_whitespace();
        if (!tokens.next_token().is(Token::Type::Comma))
            return {};
        auto element_type = parse_color_stop_list_element(list_element);
        if (element_type == ElementType::ColorHint) {
            // <linear-color-hint>, <linear-color-stop>
            tokens.skip_whitespace();
            if (!tokens.next_token().is(Token::Type::Comma))
                return {};
            // Note: This fills in the color stop on the same list_element as the color hint (it does not overwrite it).
            if (parse_color_stop_list_element(list_element) != ElementType::ColorStop)
                return {};
        } else if (element_type == ElementType::ColorStop) {
            // <linear-color-stop>
        } else {
            return {};
        }
        color_stops.append(list_element);
    }

    return LinearGradientStyleValue::create(gradient_direction, move(color_stops), gradient_type, repeating_gradient);
}

CSSRule* Parser::convert_to_rule(NonnullRefPtr<Rule> rule)
{
    if (rule->is_at_rule()) {
        if (has_ignored_vendor_prefix(rule->at_rule_name()))
            return {};
        if (rule->at_rule_name().equals_ignoring_case("font-face"sv)) {
            if (!rule->block() || !rule->block()->is_curly()) {
                dbgln_if(CSS_PARSER_DEBUG, "@font-face rule is malformed.");
                return {};
            }
            TokenStream tokens { rule->block()->values() };
            return parse_font_face_rule(tokens);
        }
        if (rule->at_rule_name().equals_ignoring_case("import"sv) && !rule->prelude().is_empty()) {
            Optional<AK::URL> url;
            for (auto const& token : rule->prelude()) {
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
            dbgln_if(CSS_PARSER_DEBUG, "Unable to parse url from @import rule");
            return {};
        }
        if (rule->at_rule_name().equals_ignoring_case("media"sv)) {
            auto media_query_tokens = TokenStream { rule->prelude() };
            auto media_query_list = parse_a_media_query_list(media_query_tokens);
            if (media_query_list.is_empty() || !rule->block())
                return {};

            auto child_tokens = TokenStream { rule->block()->values() };
            auto parser_rules = parse_a_list_of_rules(child_tokens);
            JS::MarkedVector<CSSRule*> child_rules(m_context.realm().heap());
            for (auto& raw_rule : parser_rules) {
                if (auto* child_rule = convert_to_rule(raw_rule))
                    child_rules.append(child_rule);
            }
            auto* rule_list = CSSRuleList::create(m_context.realm(), child_rules);
            return CSSMediaRule::create(m_context.realm(), *MediaList::create(m_context.realm(), move(media_query_list)), *rule_list);
        }
        if (rule->at_rule_name().equals_ignoring_case("supports"sv)) {
            auto supports_tokens = TokenStream { rule->prelude() };
            auto supports = parse_a_supports(supports_tokens);
            if (!supports) {
                if constexpr (CSS_PARSER_DEBUG) {
                    dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @supports rule invalid; discarding.");
                    supports_tokens.dump_all_tokens();
                }
                return {};
            }

            if (!rule->block())
                return {};
            auto child_tokens = TokenStream { rule->block()->values() };
            auto parser_rules = parse_a_list_of_rules(child_tokens);
            JS::MarkedVector<CSSRule*> child_rules(m_context.realm().heap());
            for (auto& raw_rule : parser_rules) {
                if (auto* child_rule = convert_to_rule(raw_rule))
                    child_rules.append(child_rule);
            }

            auto* rule_list = CSSRuleList::create(m_context.realm(), child_rules);
            return CSSSupportsRule::create(m_context.realm(), supports.release_nonnull(), *rule_list);
        }

        // FIXME: More at rules!
        dbgln_if(CSS_PARSER_DEBUG, "Unrecognized CSS at-rule: @{}", rule->at_rule_name());
        return {};
    }

    auto prelude_stream = TokenStream(rule->prelude());
    auto selectors = parse_a_selector_list(prelude_stream, SelectorType::Standalone);

    if (selectors.is_error()) {
        if (selectors.error() != ParseError::IncludesIgnoredVendorPrefix) {
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

    if (!rule->block()->is_curly())
        return {};

    auto stream = TokenStream(rule->block()->values());
    auto declarations_and_at_rules = parse_a_style_blocks_contents(stream);

    auto* declaration = convert_to_style_declaration(declarations_and_at_rules);
    if (!declaration) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: style rule declaration invalid; discarding.");
        return {};
    }

    return CSSStyleRule::create(m_context.realm(), move(selectors.value()), *declaration);
}

auto Parser::extract_properties(Vector<DeclarationOrAtRule> const& declarations_and_at_rules) -> PropertiesAndCustomProperties
{
    PropertiesAndCustomProperties result;
    for (auto const& declaration_or_at_rule : declarations_and_at_rules) {
        if (declaration_or_at_rule.is_at_rule()) {
            dbgln_if(CSS_PARSER_DEBUG, "!!! CSS at-rule is not allowed here!");
            continue;
        }

        auto const& declaration = declaration_or_at_rule.declaration();

        if (auto maybe_property = convert_to_style_property(declaration); maybe_property.has_value()) {
            auto property = maybe_property.release_value();
            if (property.property_id == PropertyID::Custom) {
                result.custom_properties.set(property.custom_name, property);
            } else {
                result.properties.append(move(property));
            }
        }
    }
    return result;
}

PropertyOwningCSSStyleDeclaration* Parser::convert_to_style_declaration(Vector<DeclarationOrAtRule> const& declarations_and_at_rules)
{
    auto [properties, custom_properties] = extract_properties(declarations_and_at_rules);
    return PropertyOwningCSSStyleDeclaration::create(m_context.realm(), move(properties), move(custom_properties));
}

Optional<StyleProperty> Parser::convert_to_style_property(Declaration const& declaration)
{
    auto property_name = declaration.name();
    auto property_id = property_id_from_string(property_name);

    if (property_id == PropertyID::Invalid) {
        if (property_name.starts_with("--"sv)) {
            property_id = PropertyID::Custom;
        } else if (has_ignored_vendor_prefix(property_name)) {
            return {};
        } else if (!property_name.starts_with('-')) {
            dbgln_if(CSS_PARSER_DEBUG, "Unrecognized CSS property '{}'", property_name);
            return {};
        }
    }

    auto value_token_stream = TokenStream(declaration.values());
    auto value = parse_css_value(property_id, value_token_stream);
    if (value.is_error()) {
        if (value.error() != ParseError::IncludesIgnoredVendorPrefix) {
            dbgln_if(CSS_PARSER_DEBUG, "Unable to parse value for CSS property '{}'.", property_name);
            if constexpr (CSS_PARSER_DEBUG) {
                value_token_stream.dump_all_tokens();
            }
        }
        return {};
    }

    if (property_id == PropertyID::Custom)
        return StyleProperty { declaration.importance(), property_id, value.release_value(), declaration.name() };

    return StyleProperty { declaration.importance(), property_id, value.release_value(), {} };
}

RefPtr<StyleValue> Parser::parse_builtin_value(ComponentValue const& component_value)
{
    if (component_value.is(Token::Type::Ident)) {
        auto ident = component_value.token().ident();
        if (ident.equals_ignoring_case("inherit"sv))
            return InheritStyleValue::the();
        if (ident.equals_ignoring_case("initial"sv))
            return InitialStyleValue::the();
        if (ident.equals_ignoring_case("unset"sv))
            return UnsetStyleValue::the();
        // FIXME: Implement `revert` and `revert-layer` keywords, from Cascade4 and Cascade5 respectively
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_calculated_value(Vector<ComponentValue> const& component_values)
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

RefPtr<StyleValue> Parser::parse_dynamic_value(ComponentValue const& component_value)
{
    if (component_value.is_function()) {
        auto const& function = component_value.function();

        if (function.name().equals_ignoring_case("calc"sv))
            return parse_calculated_value(function.values());

        if (function.name().equals_ignoring_case("var"sv)) {
            // Declarations using `var()` should already be parsed as an UnresolvedStyleValue before this point.
            VERIFY_NOT_REACHED();
        }
    }

    return {};
}

Optional<Parser::Dimension> Parser::parse_dimension(ComponentValue const& component_value)
{
    if (component_value.is(Token::Type::Dimension)) {
        float numeric_value = component_value.token().dimension_value();
        auto unit_string = component_value.token().dimension_unit();

        if (auto length_type = Length::unit_from_name(unit_string); length_type.has_value())
            return Length { numeric_value, length_type.release_value() };

        if (auto angle_type = Angle::unit_from_name(unit_string); angle_type.has_value())
            return Angle { numeric_value, angle_type.release_value() };

        if (auto frequency_type = Frequency::unit_from_name(unit_string); frequency_type.has_value())
            return Frequency { numeric_value, frequency_type.release_value() };

        if (auto resolution_type = Resolution::unit_from_name(unit_string); resolution_type.has_value())
            return Resolution { numeric_value, resolution_type.release_value() };

        if (auto time_type = Time::unit_from_name(unit_string); time_type.has_value())
            return Time { numeric_value, time_type.release_value() };
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

Optional<Length> Parser::parse_length(ComponentValue const& component_value)
{
    auto dimension = parse_dimension(component_value);
    if (!dimension.has_value())
        return {};

    if (dimension->is_length())
        return dimension->length();

    // FIXME: auto isn't a length!
    if (component_value.is(Token::Type::Ident) && component_value.token().ident().equals_ignoring_case("auto"sv))
        return Length::make_auto();

    return {};
}

Optional<Ratio> Parser::parse_ratio(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();

    // `<ratio> = <number [0,∞]> [ / <number [0,∞]> ]?`
    // FIXME: I think either part is allowed to be calc(), which makes everything complicated.
    auto first_number = tokens.next_token();
    if (!first_number.is(Token::Type::Number) || first_number.token().number_value() < 0)
        return {};

    {
        auto two_value_transaction = tokens.begin_transaction();
        tokens.skip_whitespace();
        auto solidus = tokens.next_token();
        tokens.skip_whitespace();
        auto second_number = tokens.next_token();
        if (solidus.is(Token::Type::Delim) && solidus.token().delim() == '/'
            && second_number.is(Token::Type::Number) && second_number.token().number_value() > 0) {
            // Two-value ratio
            two_value_transaction.commit();
            transaction.commit();
            return Ratio { static_cast<float>(first_number.token().number_value()), static_cast<float>(second_number.token().number_value()) };
        }
    }

    // Single-value ratio
    transaction.commit();
    return Ratio { static_cast<float>(first_number.token().number_value()) };
}

// https://www.w3.org/TR/css-syntax-3/#urange-syntax
Optional<UnicodeRange> Parser::parse_unicode_range(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();
    tokens.skip_whitespace();

    // <urange> =
    //  u '+' <ident-token> '?'* |
    //  u <dimension-token> '?'* |
    //  u <number-token> '?'* |
    //  u <number-token> <dimension-token> |
    //  u <number-token> <number-token> |
    //  u '+' '?'+
    // (All with no whitespace in between tokens.)

    // NOTE: Parsing this is different from usual. We take these steps:
    // 1. Match the grammar above against the tokens, concatenating them into a string using their original representation.
    // 2. Then, parse that string according to the spec algorithm.
    // Step 2 is performed by calling the other parse_unicode_range() overload.

    auto is_question_mark = [](ComponentValue const& component_value) {
        return component_value.is(Token::Type::Delim) && component_value.token().delim() == '?';
    };

    auto is_ending_token = [](ComponentValue const& component_value) {
        return component_value.is(Token::Type::EndOfFile)
            || component_value.is(Token::Type::Comma)
            || component_value.is(Token::Type::Semicolon)
            || component_value.is(Token::Type::Whitespace);
    };

    auto representation_of = [](ComponentValue const& component_value) {
        // FIXME: This should use the "representation", that is, the original text that produced the token.
        //        See: https://www.w3.org/TR/css-syntax-3/#representation
        //        We don't have a way to get that, so instead, we're relying on Token::to_string(), and
        //        handling specific cases where that's not enough.
        // Integers like `+34` get serialized as `34`, so manually include the `+` sign.
        if (component_value.is(Token::Type::Number) && component_value.token().number().is_integer_with_explicit_sign()) {
            auto int_value = component_value.token().number().integer_value();
            return String::formatted("{:+}", int_value);
        }

        return component_value.to_string();
    };

    auto create_unicode_range = [&](StringView text, auto& local_transaction) -> Optional<UnicodeRange> {
        auto maybe_unicode_range = parse_unicode_range(text);
        if (maybe_unicode_range.has_value()) {
            local_transaction.commit();
            transaction.commit();
        }
        return maybe_unicode_range;
    };

    // All options start with 'u'/'U'.
    auto const& u = tokens.next_token();
    if (!(u.is(Token::Type::Ident) && u.token().ident().equals_ignoring_case("u"sv))) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> does not start with 'u'");
        return {};
    }

    auto const& second_token = tokens.next_token();

    //  u '+' <ident-token> '?'* |
    //  u '+' '?'+
    if (second_token.is(Token::Type::Delim) && second_token.token().delim() == '+') {
        auto local_transaction = tokens.begin_transaction();
        StringBuilder string_builder;
        string_builder.append(representation_of(second_token));

        auto const& third_token = tokens.next_token();
        if (third_token.is(Token::Type::Ident) || is_question_mark(third_token)) {
            string_builder.append(representation_of(third_token));
            while (is_question_mark(tokens.peek_token()))
                string_builder.append(representation_of(tokens.next_token()));
            if (is_ending_token(tokens.peek_token()))
                return create_unicode_range(string_builder.string_view(), local_transaction);
        }
    }

    //  u <dimension-token> '?'*
    if (second_token.is(Token::Type::Dimension)) {
        auto local_transaction = tokens.begin_transaction();
        StringBuilder string_builder;
        string_builder.append(representation_of(second_token));
        while (is_question_mark(tokens.peek_token()))
            string_builder.append(representation_of(tokens.next_token()));
        if (is_ending_token(tokens.peek_token()))
            return create_unicode_range(string_builder.string_view(), local_transaction);
    }

    //  u <number-token> '?'* |
    //  u <number-token> <dimension-token> |
    //  u <number-token> <number-token>
    if (second_token.is(Token::Type::Number)) {
        auto local_transaction = tokens.begin_transaction();
        StringBuilder string_builder;
        string_builder.append(representation_of(second_token));

        if (is_ending_token(tokens.peek_token()))
            return create_unicode_range(string_builder.string_view(), local_transaction);

        auto const& third_token = tokens.next_token();
        string_builder.append(representation_of(third_token));
        if (is_question_mark(third_token)) {
            while (is_question_mark(tokens.peek_token()))
                string_builder.append(representation_of(tokens.next_token()));
            if (is_ending_token(tokens.peek_token()))
                return create_unicode_range(string_builder.string_view(), local_transaction);
        } else if (third_token.is(Token::Type::Dimension)) {
            if (is_ending_token(tokens.peek_token()))
                return create_unicode_range(string_builder.string_view(), local_transaction);
        } else if (third_token.is(Token::Type::Number)) {
            if (is_ending_token(tokens.peek_token()))
                return create_unicode_range(string_builder.string_view(), local_transaction);
        }
    }

    if constexpr (CSS_PARSER_DEBUG) {
        dbgln("CSSParser: Tokens did not match <urange> grammar.");
        tokens.dump_all_tokens();
    }
    return {};
}

Optional<UnicodeRange> Parser::parse_unicode_range(StringView text)
{
    auto make_valid_unicode_range = [&](u32 start_value, u32 end_value) -> Optional<UnicodeRange> {
        // https://www.w3.org/TR/css-syntax-3/#maximum-allowed-code-point
        constexpr u32 maximum_allowed_code_point = 0x10FFFF;

        // To determine what codepoints the <urange> represents:
        // 1. If end value is greater than the maximum allowed code point,
        //    the <urange> is invalid and a syntax error.
        if (end_value > maximum_allowed_code_point) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: Invalid <urange>: end_value ({}) > maximum ({})", end_value, maximum_allowed_code_point);
            return {};
        }

        // 2. If start value is greater than end value, the <urange> is invalid and a syntax error.
        if (start_value > end_value) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: Invalid <urange>: start_value ({}) > end_value ({})", start_value, end_value);
            return {};
        }

        // 3. Otherwise, the <urange> represents a contiguous range of codepoints from start value to end value, inclusive.
        return UnicodeRange { start_value, end_value };
    };

    // 1. Skipping the first u token, concatenate the representations of all the tokens in the production together.
    //    Let this be text.
    // NOTE: The concatenation is already done by the caller.
    GenericLexer lexer { text };

    // 2. If the first character of text is U+002B PLUS SIGN, consume it.
    //    Otherwise, this is an invalid <urange>, and this algorithm must exit.
    if (lexer.next_is('+')) {
        lexer.consume();
    } else {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: Second character of <urange> was not '+'; got: '{}'", lexer.consume());
        return {};
    }

    // 3. Consume as many hex digits from text as possible.
    //    then consume as many U+003F QUESTION MARK (?) code points as possible.
    auto hex_digits = lexer.consume_while(is_ascii_hex_digit);
    auto question_marks = lexer.consume_while([](auto it) { return it == '?'; });
    //    If zero code points were consumed, or more than six code points were consumed,
    //    this is an invalid <urange>, and this algorithm must exit.
    size_t consumed_code_points = hex_digits.length() + question_marks.length();
    if (consumed_code_points == 0 || consumed_code_points > 6) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> start value had {} digits/?s, expected between 1 and 6.", consumed_code_points);
        return {};
    }
    StringView start_value_code_points { hex_digits.characters_without_null_termination(), consumed_code_points };

    //    If any U+003F QUESTION MARK (?) code points were consumed, then:
    if (question_marks.length() > 0) {
        // 1. If there are any code points left in text, this is an invalid <urange>,
        //    and this algorithm must exit.
        if (lexer.tell_remaining() != 0) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> invalid; had {} code points left over.", lexer.tell_remaining());
            return {};
        }

        // 2. Interpret the consumed code points as a hexadecimal number,
        //    with the U+003F QUESTION MARK (?) code points replaced by U+0030 DIGIT ZERO (0) code points.
        //    This is the start value.
        auto start_value_string = start_value_code_points.replace("?"sv, "0"sv, ReplaceMode::All);
        auto maybe_start_value = AK::StringUtils::convert_to_uint_from_hex<u32>(start_value_string);
        if (!maybe_start_value.has_value()) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> ?-converted start value did not parse as hex number.");
            return {};
        }
        u32 start_value = maybe_start_value.release_value();

        // 3. Interpret the consumed code points as a hexadecimal number again,
        //    with the U+003F QUESTION MARK (?) code points replaced by U+0046 LATIN CAPITAL LETTER F (F) code points.
        //    This is the end value.
        auto end_value_string = start_value_code_points.replace("?"sv, "F"sv, ReplaceMode::All);
        auto maybe_end_value = AK::StringUtils::convert_to_uint_from_hex<u32>(end_value_string);
        if (!maybe_end_value.has_value()) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> ?-converted end value did not parse as hex number.");
            return {};
        }
        u32 end_value = maybe_end_value.release_value();

        // 4. Exit this algorithm.
        return make_valid_unicode_range(start_value, end_value);
    }
    //   Otherwise, interpret the consumed code points as a hexadecimal number. This is the start value.
    auto maybe_start_value = AK::StringUtils::convert_to_uint_from_hex<u32>(start_value_code_points);
    if (!maybe_start_value.has_value()) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> start value did not parse as hex number.");
        return {};
    }
    u32 start_value = maybe_start_value.release_value();

    // 4. If there are no code points left in text, The end value is the same as the start value.
    //    Exit this algorithm.
    if (lexer.tell_remaining() == 0)
        return make_valid_unicode_range(start_value, start_value);

    // 5. If the next code point in text is U+002D HYPHEN-MINUS (-), consume it.
    if (lexer.next_is('-')) {
        lexer.consume();
    }
    //    Otherwise, this is an invalid <urange>, and this algorithm must exit.
    else {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> start and end values not separated by '-'.");
        return {};
    }

    // 6. Consume as many hex digits as possible from text.
    auto end_hex_digits = lexer.consume_while(is_ascii_hex_digit);

    //   If zero hex digits were consumed, or more than 6 hex digits were consumed,
    //   this is an invalid <urange>, and this algorithm must exit.
    if (end_hex_digits.length() == 0 || end_hex_digits.length() > 6) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> end value had {} digits, expected between 1 and 6.", end_hex_digits.length());
        return {};
    }

    //   If there are any code points left in text, this is an invalid <urange>, and this algorithm must exit.
    if (lexer.tell_remaining() != 0) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> invalid; had {} code points left over.", lexer.tell_remaining());
        return {};
    }

    // 7. Interpret the consumed code points as a hexadecimal number. This is the end value.
    auto maybe_end_value = AK::StringUtils::convert_to_uint_from_hex<u32>(end_hex_digits);
    if (!maybe_end_value.has_value()) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: <urange> end value did not parse as hex number.");
        return {};
    }
    u32 end_value = maybe_end_value.release_value();

    return make_valid_unicode_range(start_value, end_value);
}

RefPtr<StyleValue> Parser::parse_dimension_value(ComponentValue const& component_value)
{
    // Numbers with no units can be lengths, in two situations:
    // 1) We're in quirks mode, and it's an integer.
    // 2) It's a 0.
    // We handle case 1 here. Case 2 is handled by NumericStyleValue pretending to be a LengthStyleValue if it is 0.

    if (component_value.is(Token::Type::Number) && !(m_context.in_quirks_mode() && property_has_quirk(m_context.current_property_id(), Quirk::UnitlessLength)))
        return {};

    if (component_value.is(Token::Type::Ident) && component_value.token().ident().equals_ignoring_case("auto"sv))
        return LengthStyleValue::create(Length::make_auto());

    auto dimension = parse_dimension(component_value);
    if (!dimension.has_value())
        return {};

    if (dimension->is_angle())
        return AngleStyleValue::create(dimension->angle());
    if (dimension->is_frequency())
        return FrequencyStyleValue::create(dimension->frequency());
    if (dimension->is_length())
        return LengthStyleValue::create(dimension->length());
    if (dimension->is_percentage())
        return PercentageStyleValue::create(dimension->percentage());
    if (dimension->is_resolution())
        return ResolutionStyleValue::create(dimension->resolution());
    if (dimension->is_time())
        return TimeStyleValue::create(dimension->time());
    VERIFY_NOT_REACHED();
}

RefPtr<StyleValue> Parser::parse_numeric_value(ComponentValue const& component_value)
{
    if (component_value.is(Token::Type::Number)) {
        auto const& number = component_value.token();
        if (number.number().is_integer())
            return NumericStyleValue::create_integer(number.to_integer());
        return NumericStyleValue::create_float(number.number_value());
    }

    return {};
}

RefPtr<StyleValue> Parser::parse_identifier_value(ComponentValue const& component_value)
{
    if (component_value.is(Token::Type::Ident)) {
        auto value_id = value_id_from_string(component_value.token().ident());
        if (value_id != ValueID::Invalid)
            return IdentifierStyleValue::create(value_id);
    }

    return {};
}

Optional<Color> Parser::parse_rgb_or_hsl_color(StringView function_name, Vector<ComponentValue> const& component_values)
{
    Token params[4];
    bool legacy_syntax = false;
    auto tokens = TokenStream { component_values };

    tokens.skip_whitespace();
    auto const& component1 = tokens.next_token();

    if (!component1.is(Token::Type::Number)
        && !component1.is(Token::Type::Percentage)
        && !component1.is(Token::Type::Dimension))
        return {};
    params[0] = component1.token();

    tokens.skip_whitespace();
    if (tokens.peek_token().is(Token::Type::Comma)) {
        legacy_syntax = true;
        tokens.next_token();
    }

    tokens.skip_whitespace();
    auto const& component2 = tokens.next_token();
    if (!component2.is(Token::Type::Number) && !component2.is(Token::Type::Percentage))
        return {};
    params[1] = component2.token();

    tokens.skip_whitespace();
    if (legacy_syntax && !tokens.next_token().is(Token::Type::Comma))
        return {};

    tokens.skip_whitespace();
    auto const& component3 = tokens.next_token();
    if (!component3.is(Token::Type::Number) && !component3.is(Token::Type::Percentage))
        return {};
    params[2] = component3.token();

    tokens.skip_whitespace();
    auto const& alpha_separator = tokens.peek_token();
    bool has_comma = alpha_separator.is(Token::Type::Comma);
    bool has_slash = alpha_separator.is(Token::Type::Delim) && alpha_separator.token().delim() == '/';
    if (legacy_syntax ? has_comma : has_slash) {
        tokens.next_token();

        tokens.skip_whitespace();
        auto const& component4 = tokens.next_token();
        if (!component4.is(Token::Type::Number) && !component4.is(Token::Type::Percentage))
            return {};
        params[3] = component4.token();
    }

    tokens.skip_whitespace();
    if (tokens.has_next_token())
        return {};

    if (function_name.equals_ignoring_case("rgb"sv)
        || function_name.equals_ignoring_case("rgba"sv)) {

        // https://www.w3.org/TR/css-color-4/#rgb-functions

        u8 a_val = 255;
        if (params[3].is(Token::Type::Number))
            a_val = clamp(lroundf(params[3].number_value() * 255.0f), 0, 255);
        else if (params[3].is(Token::Type::Percentage))
            a_val = clamp(lroundf(params[3].percentage() * 2.55f), 0, 255);

        if (params[0].is(Token::Type::Number)
            && params[1].is(Token::Type::Number)
            && params[2].is(Token::Type::Number)) {

            u8 r_val = clamp(llroundf(params[0].number_value()), 0, 255);
            u8 g_val = clamp(llroundf(params[1].number_value()), 0, 255);
            u8 b_val = clamp(llroundf(params[2].number_value()), 0, 255);

            return Color(r_val, g_val, b_val, a_val);
        }

        if (params[0].is(Token::Type::Percentage)
            && params[1].is(Token::Type::Percentage)
            && params[2].is(Token::Type::Percentage)) {

            u8 r_val = lroundf(clamp(params[0].percentage() * 2.55f, 0, 255));
            u8 g_val = lroundf(clamp(params[1].percentage() * 2.55f, 0, 255));
            u8 b_val = lroundf(clamp(params[2].percentage() * 2.55f, 0, 255));

            return Color(r_val, g_val, b_val, a_val);
        }
    } else if (function_name.equals_ignoring_case("hsl"sv)
        || function_name.equals_ignoring_case("hsla"sv)) {

        // https://www.w3.org/TR/css-color-4/#the-hsl-notation

        float a_val = 1.0f;
        if (params[3].is(Token::Type::Number))
            a_val = params[3].number_value();
        else if (params[3].is(Token::Type::Percentage))
            a_val = params[3].percentage() / 100.0f;

        if (params[0].is(Token::Type::Dimension)
            && params[1].is(Token::Type::Percentage)
            && params[2].is(Token::Type::Percentage)) {

            float numeric_value = params[0].dimension_value();
            auto unit_string = params[0].dimension_unit();
            auto angle_type = Angle::unit_from_name(unit_string);

            if (!angle_type.has_value())
                return {};

            auto angle = Angle { numeric_value, angle_type.release_value() };

            float h_val = fmodf(angle.to_degrees(), 360.0f);
            float s_val = params[1].percentage() / 100.0f;
            float l_val = params[2].percentage() / 100.0f;

            return Color::from_hsla(h_val, s_val, l_val, a_val);
        }

        if (params[0].is(Token::Type::Number)
            && params[1].is(Token::Type::Percentage)
            && params[2].is(Token::Type::Percentage)) {

            float h_val = fmodf(params[0].number_value(), 360.0f);
            float s_val = params[1].percentage() / 100.0f;
            float l_val = params[2].percentage() / 100.0f;

            return Color::from_hsla(h_val, s_val, l_val, a_val);
        }
    }

    return {};
}

// https://www.w3.org/TR/CSS2/visufx.html#value-def-shape
RefPtr<StyleValue> Parser::parse_rect_value(ComponentValue const& component_value)
{
    if (!component_value.is_function())
        return {};
    auto const& function = component_value.function();
    if (!function.name().equals_ignoring_case("rect"sv))
        return {};

    Vector<Length, 4> params;
    auto tokens = TokenStream { function.values() };

    enum class CommaRequirement {
        Unknown,
        RequiresCommas,
        RequiresNoCommas
    };

    enum class Side {
        Top = 0,
        Right = 1,
        Bottom = 2,
        Left = 3
    };

    auto comma_requirement = CommaRequirement::Unknown;

    // In CSS 2.1, the only valid <shape> value is: rect(<top>, <right>, <bottom>, <left>) where
    // <top> and <bottom> specify offsets from the top border edge of the box, and <right>, and
    //  <left> specify offsets from the left border edge of the box.
    for (size_t side = 0; side < 4; side++) {
        tokens.skip_whitespace();

        // <top>, <right>, <bottom>, and <left> may either have a <length> value or 'auto'.
        // Negative lengths are permitted.
        auto current_token = tokens.next_token().token();
        if (current_token.is(Token::Type::Ident) && current_token.ident().equals_ignoring_case("auto"sv)) {
            params.append(Length::make_auto());
        } else {
            auto maybe_length = parse_length(current_token);
            if (!maybe_length.has_value())
                return {};
            params.append(maybe_length.value());
        }
        tokens.skip_whitespace();

        // The last side, should be no more tokens following it.
        if (static_cast<Side>(side) == Side::Left) {
            if (tokens.has_next_token())
                return {};
            break;
        }

        bool next_is_comma = tokens.peek_token().is(Token::Type::Comma);

        // Authors should separate offset values with commas. User agents must support separation
        // with commas, but may also support separation without commas (but not a combination),
        // because a previous revision of this specification was ambiguous in this respect.
        if (comma_requirement == CommaRequirement::Unknown)
            comma_requirement = next_is_comma ? CommaRequirement::RequiresCommas : CommaRequirement::RequiresNoCommas;

        if (comma_requirement == CommaRequirement::RequiresCommas) {
            if (next_is_comma)
                tokens.next_token();
            else
                return {};
        } else if (comma_requirement == CommaRequirement::RequiresNoCommas) {
            if (next_is_comma)
                return {};
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    return RectStyleValue::create(EdgeRect { params[0], params[1], params[2], params[3] });
}

Optional<Color> Parser::parse_color(ComponentValue const& component_value)
{
    // https://www.w3.org/TR/css-color-4/
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
        auto const& function = component_value.function();
        auto const& values = function.values();

        return parse_rgb_or_hsl_color(function.name(), values);
    }

    // https://quirks.spec.whatwg.org/#the-hashless-hex-color-quirk
    if (m_context.in_quirks_mode() && property_has_quirk(m_context.current_property_id(), Quirk::HashlessHexColor)) {
        // The value of a quirky color is obtained from the possible component values using the following algorithm,
        // aborting on the first step that returns a value:

        // 1. Let cv be the component value.
        auto const& cv = component_value;
        String serialization;
        // 2. If cv is a <number-token> or a <dimension-token>, follow these substeps:
        if (cv.is(Token::Type::Number) || cv.is(Token::Type::Dimension)) {
            // 1. If cv’s type flag is not "integer", return an error.
            //    This means that values that happen to use scientific notation, e.g., 5e5e5e, will fail to parse.
            if (!cv.token().number().is_integer())
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

RefPtr<StyleValue> Parser::parse_color_value(ComponentValue const& component_value)
{
    auto color = parse_color(component_value);
    if (color.has_value())
        return ColorStyleValue::create(color.value());

    return {};
}

RefPtr<StyleValue> Parser::parse_string_value(ComponentValue const& component_value)
{
    if (component_value.is(Token::Type::String))
        return StringStyleValue::create(component_value.token().string());

    return {};
}

RefPtr<StyleValue> Parser::parse_image_value(ComponentValue const& component_value)
{
    auto url = parse_url_function(component_value, AllowedDataUrlType::Image);
    if (url.has_value())
        return ImageStyleValue::create(url.value());
    // FIXME: Implement other kinds of gradient
    return parse_linear_gradient_function(component_value);
}

template<typename ParseFunction>
RefPtr<StyleValue> Parser::parse_comma_separated_value_list(Vector<ComponentValue> const& component_values, ParseFunction parse_one_value)
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

RefPtr<StyleValue> Parser::parse_simple_comma_separated_value_list(Vector<ComponentValue> const& component_values)
{
    return parse_comma_separated_value_list(component_values, [=, this](auto& tokens) -> RefPtr<StyleValue> {
        auto& token = tokens.next_token();
        if (auto value = parse_css_value(token); value && property_accepts_value(m_context.current_property_id(), *value))
            return value;
        tokens.reconsume_current_input_token();
        return nullptr;
    });
}

RefPtr<StyleValue> Parser::parse_background_value(Vector<ComponentValue> const& component_values)
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
        auto const& part = tokens.next_token();

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
                auto transaction = tokens.begin_transaction();
                auto& maybe_slash = tokens.next_token();
                if (maybe_slash.is(Token::Type::Delim) && maybe_slash.token().delim() == '/') {
                    if (auto maybe_background_size = parse_single_background_size_value(tokens)) {
                        transaction.commit();
                        background_size = maybe_background_size.release_nonnull();
                        continue;
                    }
                    return nullptr;
                }
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

RefPtr<StyleValue> Parser::parse_single_background_position_value(TokenStream<ComponentValue>& tokens)
{
    // NOTE: This *looks* like it parses a <position>, but it doesn't. From the spec:
    //      "Note: The background-position property also accepts a three-value syntax.
    //       This has been disallowed generically because it creates parsing ambiguities
    //       when combined with other length or percentage components in a property value."
    //           - https://www.w3.org/TR/css-values-4/#typedef-position
    //       So, we'll need a separate function to parse <position> later.

    auto transaction = tokens.begin_transaction();

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

        auto const& token = tokens.peek_token();
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
                return nullptr;
            }
            continue;
        }

        if (value->has_length()) {
            if (!horizontal.has_value()) {
                horizontal = EdgeOffset { PositionEdge::Left, value->to_length(), false, true };
            } else if (!vertical.has_value()) {
                vertical = EdgeOffset { PositionEdge::Top, value->to_length(), false, true };
            } else {
                return nullptr;
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
                return nullptr;
            }
            continue;
        }

        tokens.reconsume_current_input_token();
        break;
    }

    if (found_center) {
        if (horizontal.has_value() && vertical.has_value())
            return nullptr;
        if (!horizontal.has_value())
            horizontal = EdgeOffset { PositionEdge::Left, center_offset, true, false };
        if (!vertical.has_value())
            vertical = EdgeOffset { PositionEdge::Top, center_offset, true, false };
    }

    if (!horizontal.has_value() && !vertical.has_value())
        return nullptr;

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

    transaction.commit();
    return PositionStyleValue::create(
        horizontal->edge, horizontal->offset,
        vertical->edge, vertical->offset);
}

RefPtr<StyleValue> Parser::parse_single_background_repeat_value(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();

    auto is_directional_repeat = [](StyleValue const& value) -> bool {
        auto value_id = value.to_identifier();
        return value_id == ValueID::RepeatX || value_id == ValueID::RepeatY;
    };

    auto as_repeat = [](ValueID identifier) -> Optional<Repeat> {
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
            return {};
        }
    };

    auto const& token = tokens.next_token();
    auto maybe_x_value = parse_css_value(token);
    if (!maybe_x_value || !property_accepts_value(PropertyID::BackgroundRepeat, *maybe_x_value))
        return nullptr;
    auto x_value = maybe_x_value.release_nonnull();

    if (is_directional_repeat(*x_value)) {
        auto value_id = x_value->to_identifier();
        transaction.commit();
        return BackgroundRepeatStyleValue::create(
            value_id == ValueID::RepeatX ? Repeat::Repeat : Repeat::NoRepeat,
            value_id == ValueID::RepeatX ? Repeat::NoRepeat : Repeat::Repeat);
    }

    auto x_repeat = as_repeat(x_value->to_identifier());
    if (!x_repeat.has_value())
        return nullptr;

    // See if we have a second value for Y
    auto const& second_token = tokens.peek_token();
    auto maybe_y_value = parse_css_value(second_token);
    if (!maybe_y_value || !property_accepts_value(PropertyID::BackgroundRepeat, *maybe_y_value)) {
        // We don't have a second value, so use x for both
        transaction.commit();
        return BackgroundRepeatStyleValue::create(x_repeat.value(), x_repeat.value());
    }
    tokens.next_token();
    auto y_value = maybe_y_value.release_nonnull();
    if (is_directional_repeat(*y_value))
        return nullptr;

    auto y_repeat = as_repeat(y_value->to_identifier());
    if (!y_repeat.has_value())
        return nullptr;

    transaction.commit();
    return BackgroundRepeatStyleValue::create(x_repeat.value(), y_repeat.value());
}

RefPtr<StyleValue> Parser::parse_single_background_size_value(TokenStream<ComponentValue>& tokens)
{
    auto transaction = tokens.begin_transaction();

    auto get_length_percentage = [](StyleValue& style_value) -> Optional<LengthPercentage> {
        if (style_value.is_percentage())
            return LengthPercentage { style_value.as_percentage().percentage() };
        if (style_value.has_length())
            return LengthPercentage { style_value.to_length() };
        return {};
    };

    auto maybe_x_value = parse_css_value(tokens.next_token());
    if (!maybe_x_value || !property_accepts_value(PropertyID::BackgroundSize, *maybe_x_value))
        return nullptr;
    auto x_value = maybe_x_value.release_nonnull();

    if (x_value->to_identifier() == ValueID::Cover || x_value->to_identifier() == ValueID::Contain) {
        transaction.commit();
        return x_value;
    }

    auto maybe_y_value = parse_css_value(tokens.peek_token());
    if (!maybe_y_value || !property_accepts_value(PropertyID::BackgroundSize, *maybe_y_value)) {
        auto x_size = get_length_percentage(*x_value);
        if (!x_size.has_value())
            return nullptr;

        transaction.commit();
        return BackgroundSizeStyleValue::create(x_size.value(), x_size.value());
    }
    tokens.next_token();

    auto y_value = maybe_y_value.release_nonnull();
    auto x_size = get_length_percentage(*x_value);
    auto y_size = get_length_percentage(*y_value);

    if (!x_size.has_value() || !y_size.has_value())
        return nullptr;

    transaction.commit();
    return BackgroundSizeStyleValue::create(x_size.release_value(), y_size.release_value());
}

RefPtr<StyleValue> Parser::parse_border_value(Vector<ComponentValue> const& component_values)
{
    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> border_width;
    RefPtr<StyleValue> border_color;
    RefPtr<StyleValue> border_style;

    for (auto const& part : component_values) {
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

RefPtr<StyleValue> Parser::parse_border_radius_value(Vector<ComponentValue> const& component_values)
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

RefPtr<StyleValue> Parser::parse_border_radius_shorthand_value(Vector<ComponentValue> const& component_values)
{
    auto top_left = [&](Vector<LengthPercentage>& radii) { return radii[0]; };
    auto top_right = [&](Vector<LengthPercentage>& radii) {
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
    auto bottom_right = [&](Vector<LengthPercentage>& radii) {
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
    auto bottom_left = [&](Vector<LengthPercentage>& radii) {
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

    Vector<LengthPercentage> horizontal_radii;
    Vector<LengthPercentage> vertical_radii;
    bool reading_vertical = false;

    for (auto const& value : component_values) {
        if (value.is(Token::Type::Delim) && value.token().delim() == '/') {
            if (reading_vertical || horizontal_radii.is_empty())
                return nullptr;

            reading_vertical = true;
            continue;
        }

        auto maybe_dimension = parse_dimension(value);
        if (!maybe_dimension.has_value() || !maybe_dimension->is_length_percentage())
            return nullptr;
        if (reading_vertical) {
            vertical_radii.append(maybe_dimension->length_percentage());
        } else {
            horizontal_radii.append(maybe_dimension->length_percentage());
        }
    }

    if (horizontal_radii.size() > 4 || vertical_radii.size() > 4
        || horizontal_radii.is_empty()
        || (reading_vertical && vertical_radii.is_empty()))
        return nullptr;

    auto top_left_radius = BorderRadiusStyleValue::create(top_left(horizontal_radii),
        vertical_radii.is_empty() ? top_left(horizontal_radii) : top_left(vertical_radii));
    auto top_right_radius = BorderRadiusStyleValue::create(top_right(horizontal_radii),
        vertical_radii.is_empty() ? top_right(horizontal_radii) : top_right(vertical_radii));
    auto bottom_right_radius = BorderRadiusStyleValue::create(bottom_right(horizontal_radii),
        vertical_radii.is_empty() ? bottom_right(horizontal_radii) : bottom_right(vertical_radii));
    auto bottom_left_radius = BorderRadiusStyleValue::create(bottom_left(horizontal_radii),
        vertical_radii.is_empty() ? bottom_left(horizontal_radii) : bottom_left(vertical_radii));

    return BorderRadiusShorthandStyleValue::create(move(top_left_radius), move(top_right_radius), move(bottom_right_radius), move(bottom_left_radius));
}

RefPtr<StyleValue> Parser::parse_shadow_value(Vector<ComponentValue> const& component_values, AllowInsetKeyword allow_inset_keyword)
{
    // "none"
    if (component_values.size() == 1 && component_values.first().is(Token::Type::Ident)) {
        auto ident = parse_identifier_value(component_values.first());
        if (ident && ident->to_identifier() == ValueID::None)
            return ident;
    }

    return parse_comma_separated_value_list(component_values, [this, allow_inset_keyword](auto& tokens) {
        return parse_single_shadow_value(tokens, allow_inset_keyword);
    });
}

RefPtr<StyleValue> Parser::parse_single_shadow_value(TokenStream<ComponentValue>& tokens, AllowInsetKeyword allow_inset_keyword)
{
    auto transaction = tokens.begin_transaction();

    Optional<Color> color;
    Optional<Length> offset_x;
    Optional<Length> offset_y;
    Optional<Length> blur_radius;
    Optional<Length> spread_distance;
    Optional<ShadowPlacement> placement;

    while (tokens.has_next_token()) {
        auto const& token = tokens.peek_token();

        if (auto maybe_color = parse_color(token); maybe_color.has_value()) {
            if (color.has_value())
                return nullptr;
            color = maybe_color.release_value();
            tokens.next_token();
            continue;
        }

        if (auto maybe_offset_x = parse_length(token); maybe_offset_x.has_value()) {
            // horizontal offset
            if (offset_x.has_value())
                return nullptr;
            offset_x = maybe_offset_x.release_value();
            tokens.next_token();

            // vertical offset
            if (!tokens.has_next_token())
                return nullptr;
            auto maybe_offset_y = parse_length(tokens.peek_token());
            if (!maybe_offset_y.has_value())
                return nullptr;
            offset_y = maybe_offset_y.release_value();
            tokens.next_token();

            // blur radius (optional)
            if (!tokens.has_next_token())
                break;
            auto maybe_blur_radius = parse_length(tokens.peek_token());
            if (!maybe_blur_radius.has_value())
                continue;
            blur_radius = maybe_blur_radius.release_value();
            tokens.next_token();

            // spread distance (optional)
            if (!tokens.has_next_token())
                break;
            auto maybe_spread_distance = parse_length(tokens.peek_token());
            if (!maybe_spread_distance.has_value())
                continue;
            spread_distance = maybe_spread_distance.release_value();
            tokens.next_token();

            continue;
        }

        if (allow_inset_keyword == AllowInsetKeyword::Yes
            && token.is(Token::Type::Ident) && token.token().ident().equals_ignoring_case("inset"sv)) {
            if (placement.has_value())
                return nullptr;
            placement = ShadowPlacement::Inner;
            tokens.next_token();
            continue;
        }

        if (token.is(Token::Type::Comma))
            break;

        return nullptr;
    }

    // FIXME: If color is absent, default to `currentColor`
    if (!color.has_value())
        color = Color::NamedColor::Black;

    // x/y offsets are required
    if (!offset_x.has_value() || !offset_y.has_value())
        return nullptr;

    // Other lengths default to 0
    if (!blur_radius.has_value())
        blur_radius = Length::make_px(0);
    if (!spread_distance.has_value())
        spread_distance = Length::make_px(0);

    // Placement is outer by default
    if (!placement.has_value())
        placement = ShadowPlacement::Outer;

    transaction.commit();
    return ShadowStyleValue::create(color.release_value(), offset_x.release_value(), offset_y.release_value(), blur_radius.release_value(), spread_distance.release_value(), placement.release_value());
}

RefPtr<StyleValue> Parser::parse_content_value(Vector<ComponentValue> const& component_values)
{
    // FIXME: `content` accepts several kinds of function() type, which we don't handle in property_accepts_value() yet.

    auto is_single_value_identifier = [](ValueID identifier) -> bool {
        switch (identifier) {
        case ValueID::None:
        case ValueID::Normal:
            return true;
        default:
            return false;
        }
    };

    if (component_values.size() == 1) {
        if (auto identifier = parse_identifier_value(component_values.first())) {
            if (is_single_value_identifier(identifier->to_identifier()))
                return identifier;
        }
    }

    NonnullRefPtrVector<StyleValue> content_values;
    NonnullRefPtrVector<StyleValue> alt_text_values;
    bool in_alt_text = false;

    for (auto const& value : component_values) {
        if (value.is(Token::Type::Delim) && value.token().delim() == '/') {
            if (in_alt_text || content_values.is_empty())
                return {};
            in_alt_text = true;
            continue;
        }
        auto style_value = parse_css_value(value);
        if (style_value && property_accepts_value(PropertyID::Content, *style_value)) {
            if (is_single_value_identifier(style_value->to_identifier()))
                return {};

            if (in_alt_text) {
                alt_text_values.append(style_value.release_nonnull());
            } else {
                content_values.append(style_value.release_nonnull());
            }
            continue;
        }

        return {};
    }

    if (content_values.is_empty())
        return {};
    if (in_alt_text && alt_text_values.is_empty())
        return {};

    RefPtr<StyleValueList> alt_text;
    if (!alt_text_values.is_empty())
        alt_text = StyleValueList::create(move(alt_text_values), StyleValueList::Separator::Space);

    return ContentStyleValue::create(StyleValueList::create(move(content_values), StyleValueList::Separator::Space), move(alt_text));
}

RefPtr<StyleValue> Parser::parse_filter_value_list_value(Vector<ComponentValue> const& component_values)
{
    if (component_values.size() == 1 && component_values.first().is(Token::Type::Ident)) {
        auto ident = parse_identifier_value(component_values.first());
        if (ident && ident->to_identifier() == ValueID::None)
            return ident;
    }

    TokenStream tokens { component_values };

    // FIXME: <url>s are ignored for now
    // <filter-value-list> = [ <filter-function> | <url> ]+

    enum class FilterToken {
        // Color filters:
        Brightness,
        Contrast,
        Grayscale,
        Invert,
        Opacity,
        Saturate,
        Sepia,
        // Special filters:
        Blur,
        DropShadow,
        HueRotate
    };

    auto filter_token_to_operation = [&](auto filter) {
        VERIFY(to_underlying(filter) < to_underlying(FilterToken::Blur));
        return static_cast<Filter::Color::Operation>(filter);
    };

    auto parse_number_percentage = [&](auto& token) -> Optional<NumberPercentage> {
        if (token.is(Token::Type::Percentage))
            return NumberPercentage(Percentage(token.token().percentage()));
        if (token.is(Token::Type::Number))
            return NumberPercentage(Number(Number::Type::Number, token.token().number_value()));
        return {};
    };

    auto parse_filter_function_name = [&](auto name) -> Optional<FilterToken> {
        if (name.equals_ignoring_case("blur"sv))
            return FilterToken::Blur;
        if (name.equals_ignoring_case("brightness"sv))
            return FilterToken::Brightness;
        if (name.equals_ignoring_case("contrast"sv))
            return FilterToken::Contrast;
        if (name.equals_ignoring_case("drop-shadow"sv))
            return FilterToken::DropShadow;
        if (name.equals_ignoring_case("grayscale"sv))
            return FilterToken::Grayscale;
        if (name.equals_ignoring_case("hue-rotate"sv))
            return FilterToken::HueRotate;
        if (name.equals_ignoring_case("invert"sv))
            return FilterToken::Invert;
        if (name.equals_ignoring_case("opacity"sv))
            return FilterToken::Opacity;
        if (name.equals_ignoring_case("saturate"sv))
            return FilterToken::Saturate;
        if (name.equals_ignoring_case("sepia"sv))
            return FilterToken::Sepia;
        return {};
    };

    auto parse_filter_function = [&](auto filter_token, auto function_values) -> Optional<FilterFunction> {
        TokenStream tokens { function_values };
        tokens.skip_whitespace();

        auto if_no_more_tokens_return = [&](auto filter) -> Optional<FilterFunction> {
            tokens.skip_whitespace();
            if (tokens.has_next_token())
                return {};
            return filter;
        };

        if (filter_token == FilterToken::Blur) {
            // blur( <length>? )
            if (!tokens.has_next_token())
                return Filter::Blur {};
            auto blur_radius = parse_length(tokens.next_token());
            if (!blur_radius.has_value())
                return {};
            return if_no_more_tokens_return(Filter::Blur { *blur_radius });
        } else if (filter_token == FilterToken::DropShadow) {
            if (!tokens.has_next_token())
                return {};
            auto next_token = [&]() -> auto&
            {
                auto& token = tokens.next_token();
                tokens.skip_whitespace();
                return token;
            };
            // drop-shadow( [ <color>? && <length>{2,3} ] )
            // Note: The following code is a little awkward to allow the color to be before or after the lengths.
            auto& first_param = next_token();
            Optional<Length> maybe_radius = {};
            auto maybe_color = parse_color(first_param);
            auto x_offset = parse_length(maybe_color.has_value() ? next_token() : first_param);
            if (!x_offset.has_value() || !tokens.has_next_token()) {
                return {};
            }
            auto y_offset = parse_length(next_token());
            if (!y_offset.has_value()) {
                return {};
            }
            if (tokens.has_next_token()) {
                auto& token = next_token();
                maybe_radius = parse_length(token);
                if (!maybe_color.has_value() && (!maybe_radius.has_value() || tokens.has_next_token())) {
                    maybe_color = parse_color(!maybe_radius.has_value() ? token : next_token());
                    if (!maybe_color.has_value()) {
                        return {};
                    }
                } else if (!maybe_radius.has_value()) {
                    return {};
                }
            }
            return if_no_more_tokens_return(Filter::DropShadow { *x_offset, *y_offset, maybe_radius, maybe_color });
        } else if (filter_token == FilterToken::HueRotate) {
            // hue-rotate( [ <angle> | <zero> ]? )
            if (!tokens.has_next_token())
                return Filter::HueRotate {};
            auto& token = tokens.next_token();
            if (token.is(Token::Type::Number)) {
                // hue-rotate(0)
                auto number = token.token().number();
                if (number.is_integer() && number.integer_value() == 0)
                    return if_no_more_tokens_return(Filter::HueRotate { Filter::HueRotate::Zero {} });
                return {};
            }
            if (!token.is(Token::Type::Dimension))
                return {};
            float angle_value = token.token().dimension_value();
            auto angle_unit_name = token.token().dimension_unit();
            auto angle_unit = Angle::unit_from_name(angle_unit_name);
            if (!angle_unit.has_value())
                return {};
            Angle angle { angle_value, angle_unit.release_value() };
            return if_no_more_tokens_return(Filter::HueRotate { angle });
        } else {
            // Simple filters:
            // brightness( <number-percentage>? )
            // contrast( <number-percentage>? )
            // grayscale( <number-percentage>? )
            // invert( <number-percentage>? )
            // opacity( <number-percentage>? )
            // sepia( <number-percentage>? )
            // saturate( <number-percentage>? )
            if (!tokens.has_next_token())
                return Filter::Color { filter_token_to_operation(filter_token) };
            auto amount = parse_number_percentage(tokens.next_token());
            if (!amount.has_value())
                return {};
            return if_no_more_tokens_return(Filter::Color { filter_token_to_operation(filter_token), *amount });
        }
    };

    Vector<FilterFunction> filter_value_list {};

    while (tokens.has_next_token()) {
        tokens.skip_whitespace();
        if (!tokens.has_next_token())
            break;
        auto& token = tokens.next_token();
        if (!token.is_function())
            return {};
        auto filter_token = parse_filter_function_name(token.function().name());
        if (!filter_token.has_value())
            return {};
        auto filter_function = parse_filter_function(*filter_token, token.function().values());
        if (!filter_function.has_value())
            return {};
        filter_value_list.append(*filter_function);
    }

    if (filter_value_list.is_empty())
        return {};

    return FilterValueListStyleValue::create(move(filter_value_list));
}

RefPtr<StyleValue> Parser::parse_flex_value(Vector<ComponentValue> const& component_values)
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

RefPtr<StyleValue> Parser::parse_flex_flow_value(Vector<ComponentValue> const& component_values)
{
    if (component_values.size() > 2)
        return nullptr;

    RefPtr<StyleValue> flex_direction;
    RefPtr<StyleValue> flex_wrap;

    for (auto const& part : component_values) {
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

static bool is_generic_font_family(ValueID identifier)
{
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
}

RefPtr<StyleValue> Parser::parse_font_value(Vector<ComponentValue> const& component_values)
{
    RefPtr<StyleValue> font_style;
    RefPtr<StyleValue> font_weight;
    RefPtr<StyleValue> font_size;
    RefPtr<StyleValue> line_height;
    RefPtr<StyleValue> font_families;
    RefPtr<StyleValue> font_variant;
    // FIXME: Implement font-stretch.

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
        if (property_accepts_value(PropertyID::FontVariant, *value)) {
            if (font_variant)
                return nullptr;
            font_variant = value.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::FontSize, *value)) {
            if (font_size)
                return nullptr;
            font_size = value.release_nonnull();

            // Consume `/ line-height` if present
            if (i + 2 < component_values.size()) {
                auto const& maybe_solidus = component_values[i + 1];
                if (maybe_solidus.is(Token::Type::Delim) && maybe_solidus.token().delim() == '/') {
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

RefPtr<StyleValue> Parser::parse_font_family_value(Vector<ComponentValue> const& component_values, size_t start_index)
{
    auto is_comma_or_eof = [&](size_t i) -> bool {
        if (i < component_values.size()) {
            auto const& maybe_comma = component_values[i];
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
        auto const& part = component_values[i];

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

CSSRule* Parser::parse_font_face_rule(TokenStream<ComponentValue>& tokens)
{
    auto declarations_and_at_rules = parse_a_list_of_declarations(tokens);

    Optional<FlyString> font_family;
    Vector<FontFace::Source> src;
    Vector<UnicodeRange> unicode_range;

    for (auto& declaration_or_at_rule : declarations_and_at_rules) {
        if (declaration_or_at_rule.is_at_rule()) {
            dbgln_if(CSS_PARSER_DEBUG, "CSSParser: CSS at-rules are not allowed in @font-family; discarding.");
            continue;
        }

        auto const& declaration = declaration_or_at_rule.declaration();
        if (declaration.name().equals_ignoring_case("font-family"sv)) {
            // FIXME: This is very similar to, but different from, the logic in parse_font_family_value().
            //        Ideally they could share code.
            Vector<String> font_family_parts;
            bool had_syntax_error = false;
            for (size_t i = 0; i < declaration.values().size(); ++i) {
                auto const& part = declaration.values()[i];
                if (part.is(Token::Type::Whitespace))
                    continue;
                if (part.is(Token::Type::String)) {
                    if (!font_family_parts.is_empty()) {
                        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face font-family format invalid; discarding.");
                        had_syntax_error = true;
                        break;
                    }
                    font_family_parts.append(part.token().string());
                    continue;
                }
                if (part.is(Token::Type::Ident)) {
                    if (is_builtin(part.token().ident())) {
                        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face font-family format invalid; discarding.");
                        had_syntax_error = true;
                        break;
                    }
                    auto value_id = value_id_from_string(part.token().ident());
                    if (is_generic_font_family(value_id)) {
                        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face font-family format invalid; discarding.");
                        had_syntax_error = true;
                        break;
                    }
                    font_family_parts.append(part.token().ident());
                    continue;
                }

                dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face font-family format invalid; discarding.");
                had_syntax_error = true;
                break;
            }
            if (had_syntax_error || font_family_parts.is_empty())
                continue;

            font_family = String::join(' ', font_family_parts);
            continue;
        }
        if (declaration.name().equals_ignoring_case("src"sv)) {
            TokenStream token_stream { declaration.values() };
            Vector<FontFace::Source> supported_sources = parse_font_face_src(token_stream);
            if (!supported_sources.is_empty())
                src = move(supported_sources);
            continue;
        }
        if (declaration.name().equals_ignoring_case("unicode-range"sv)) {
            Vector<UnicodeRange> unicode_ranges;
            bool unicode_range_invalid = false;
            TokenStream all_tokens { declaration.values() };
            auto range_token_lists = parse_a_comma_separated_list_of_component_values(all_tokens);
            for (auto& range_tokens : range_token_lists) {
                TokenStream range_token_stream { range_tokens };
                auto maybe_unicode_range = parse_unicode_range(range_token_stream);
                if (!maybe_unicode_range.has_value()) {
                    dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face unicode-range format invalid; discarding.");
                    unicode_range_invalid = true;
                    break;
                }
                unicode_ranges.append(maybe_unicode_range.release_value());
            }

            if (unicode_range_invalid || unicode_ranges.is_empty())
                continue;

            unicode_range = move(unicode_ranges);
            continue;
        }

        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: Unrecognized descriptor '{}' in @font-family; discarding.", declaration.name());
    }

    if (!font_family.has_value()) {
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: Failed to parse @font-face: no font-family!");
        return {};
    }

    if (unicode_range.is_empty()) {
        unicode_range.empend(0x0u, 0x10FFFFu);
    }

    return CSSFontFaceRule::create(m_context.realm(), FontFace { font_family.release_value(), move(src), move(unicode_range) });
}

Vector<FontFace::Source> Parser::parse_font_face_src(TokenStream<ComponentValue>& component_values)
{
    // FIXME: Get this information from the system somehow?
    // Format-name table: https://www.w3.org/TR/css-fonts-4/#font-format-definitions
    auto font_format_is_supported = [](StringView name) {
        // The spec requires us to treat opentype and truetype as synonymous.
        if (name.is_one_of_ignoring_case("opentype"sv, "truetype"sv, "woff"sv))
            return true;
        return false;
    };

    Vector<FontFace::Source> supported_sources;

    auto list_of_source_token_lists = parse_a_comma_separated_list_of_component_values(component_values);
    for (auto const& source_token_list : list_of_source_token_lists) {
        TokenStream source_tokens { source_token_list };
        source_tokens.skip_whitespace();
        auto const& first = source_tokens.next_token();

        // <url> [ format(<font-format>)]?
        // FIXME: Implement optional tech() function from CSS-Fonts-4.
        if (auto maybe_url = parse_url_function(first, AllowedDataUrlType::Font); maybe_url.has_value()) {
            auto url = maybe_url.release_value();
            Optional<FlyString> format;

            source_tokens.skip_whitespace();
            if (!source_tokens.has_next_token()) {
                supported_sources.empend(move(url), format);
                continue;
            }

            auto maybe_function = source_tokens.next_token();
            if (!maybe_function.is_function()) {
                dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face src invalid (token after `url()` that isn't a function: {}); discarding.", maybe_function.to_debug_string());
                return {};
            }

            auto const& function = maybe_function.function();
            if (function.name().equals_ignoring_case("format"sv)) {
                TokenStream format_tokens { function.values() };
                format_tokens.skip_whitespace();
                auto const& format_name_token = format_tokens.next_token();
                StringView format_name;
                if (format_name_token.is(Token::Type::Ident)) {
                    format_name = format_name_token.token().ident();
                } else if (format_name_token.is(Token::Type::String)) {
                    format_name = format_name_token.token().string();
                } else {
                    dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face src invalid (`format()` parameter not an ident or string; is: {}); discarding.", format_name_token.to_debug_string());
                    return {};
                }

                if (!font_format_is_supported(format_name)) {
                    dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face src format({}) not supported; skipping.", format_name);
                    continue;
                }

                format = format_name;
            } else {
                dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face src invalid (unrecognized function token `{}`); discarding.", function.name());
                return {};
            }

            source_tokens.skip_whitespace();
            if (source_tokens.has_next_token()) {
                dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face src invalid (extra token `{}`); discarding.", source_tokens.peek_token().to_debug_string());
                return {};
            }

            supported_sources.empend(move(url), format);
            continue;
        }

        // FIXME: Implement `local()`.
        dbgln_if(CSS_PARSER_DEBUG, "CSSParser: @font-face src invalid (failed to parse url from: {}); discarding.", first.to_debug_string());
        return {};
    }

    return supported_sources;
}

RefPtr<StyleValue> Parser::parse_list_style_value(Vector<ComponentValue> const& component_values)
{
    if (component_values.size() > 3)
        return nullptr;

    RefPtr<StyleValue> list_position;
    RefPtr<StyleValue> list_image;
    RefPtr<StyleValue> list_type;
    int found_nones = 0;

    for (auto const& part : component_values) {
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

RefPtr<StyleValue> Parser::parse_overflow_value(Vector<ComponentValue> const& component_values)
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

RefPtr<StyleValue> Parser::parse_text_decoration_value(Vector<ComponentValue> const& component_values)
{
    RefPtr<StyleValue> decoration_line;
    RefPtr<StyleValue> decoration_thickness;
    RefPtr<StyleValue> decoration_style;
    RefPtr<StyleValue> decoration_color;

    auto tokens = TokenStream { component_values };

    while (tokens.has_next_token()) {
        auto const& part = tokens.next_token();
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
            tokens.reconsume_current_input_token();
            auto parsed_decoration_line = parse_text_decoration_line_value(tokens);
            if (!parsed_decoration_line)
                return nullptr;
            decoration_line = parsed_decoration_line.release_nonnull();
            continue;
        }
        if (property_accepts_value(PropertyID::TextDecorationThickness, *value)) {
            if (decoration_thickness)
                return nullptr;
            decoration_thickness = value.release_nonnull();
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
    if (!decoration_thickness)
        decoration_thickness = property_initial_value(PropertyID::TextDecorationThickness);
    if (!decoration_style)
        decoration_style = property_initial_value(PropertyID::TextDecorationStyle);
    if (!decoration_color)
        decoration_color = property_initial_value(PropertyID::TextDecorationColor);

    return TextDecorationStyleValue::create(decoration_line.release_nonnull(), decoration_thickness.release_nonnull(), decoration_style.release_nonnull(), decoration_color.release_nonnull());
}

RefPtr<StyleValue> Parser::parse_text_decoration_line_value(TokenStream<ComponentValue>& tokens)
{
    NonnullRefPtrVector<StyleValue> style_values;

    while (tokens.has_next_token()) {
        auto const& token = tokens.next_token();
        auto maybe_value = parse_css_value(token);
        if (!maybe_value || !property_accepts_value(PropertyID::TextDecorationLine, *maybe_value)) {
            tokens.reconsume_current_input_token();
            break;
        }
        auto value = maybe_value.release_nonnull();

        if (auto maybe_line = value_id_to_text_decoration_line(value->to_identifier()); maybe_line.has_value()) {
            auto line = maybe_line.release_value();
            if (line == TextDecorationLine::None) {
                if (!style_values.is_empty()) {
                    tokens.reconsume_current_input_token();
                    break;
                }
                return value;
            }
            if (style_values.contains_slow(value)) {
                tokens.reconsume_current_input_token();
                break;
            }
            style_values.append(move(value));
            continue;
        }

        tokens.reconsume_current_input_token();
        break;
    }

    if (style_values.is_empty())
        return nullptr;
    return StyleValueList::create(move(style_values), StyleValueList::Separator::Space);
}

RefPtr<StyleValue> Parser::parse_transform_value(Vector<ComponentValue> const& component_values)
{
    NonnullRefPtrVector<StyleValue> transformations;
    auto tokens = TokenStream { component_values };
    tokens.skip_whitespace();

    while (tokens.has_next_token()) {
        tokens.skip_whitespace();
        auto const& part = tokens.next_token();

        if (part.is(Token::Type::Ident) && part.token().ident().equals_ignoring_case("none"sv)) {
            if (!transformations.is_empty())
                return nullptr;
            tokens.skip_whitespace();
            if (tokens.has_next_token())
                return nullptr;
            return IdentifierStyleValue::create(ValueID::None);
        }

        if (!part.is_function())
            return nullptr;
        auto maybe_function = transform_function_from_string(part.function().name());
        if (!maybe_function.has_value())
            return nullptr;
        auto function = maybe_function.release_value();
        auto function_metadata = transform_function_metadata(function);

        NonnullRefPtrVector<StyleValue> values;
        auto argument_tokens = TokenStream { part.function().values() };
        argument_tokens.skip_whitespace();
        while (argument_tokens.has_next_token()) {
            auto const& value = argument_tokens.next_token();
            RefPtr<CalculatedStyleValue> maybe_calc_value;
            if (auto maybe_dynamic_value = parse_dynamic_value(value)) {
                // TODO: calc() is the only dynamic value we support for now, but more will come later.
                // FIXME: Actually, calc() should probably be parsed inside parse_dimension_value() etc,
                //        so that it affects every use instead of us having to manually implement it.
                VERIFY(maybe_dynamic_value->is_calculated());
                maybe_calc_value = maybe_dynamic_value->as_calculated();
            }

            switch (function_metadata.parameter_type) {
            case TransformFunctionParameterType::Angle: {
                // These are `<angle> | <zero>` in the spec, so we have to check for both kinds.
                if (maybe_calc_value && maybe_calc_value->resolves_to_angle()) {
                    values.append(AngleStyleValue::create(Angle::make_calculated(maybe_calc_value.release_nonnull())));
                } else if (value.is(Token::Type::Number) && value.token().number_value() == 0) {
                    values.append(AngleStyleValue::create(Angle::make_degrees(0)));
                } else {
                    auto dimension_value = parse_dimension_value(value);
                    if (!dimension_value || !dimension_value->is_angle())
                        return nullptr;
                    values.append(dimension_value.release_nonnull());
                }
                break;
            }
            case TransformFunctionParameterType::LengthPercentage: {
                if (maybe_calc_value && maybe_calc_value->resolves_to_length()) {
                    values.append(LengthStyleValue::create(Length::make_calculated(maybe_calc_value.release_nonnull())));
                } else {
                    auto dimension_value = parse_dimension_value(value);
                    if (!dimension_value)
                        return nullptr;

                    if (dimension_value->is_percentage() || dimension_value->is_length())
                        values.append(dimension_value.release_nonnull());
                    else
                        return nullptr;
                }
                break;
            }
            case TransformFunctionParameterType::Number: {
                if (maybe_calc_value && maybe_calc_value->resolves_to_number()) {
                    values.append(LengthStyleValue::create(Length::make_calculated(maybe_calc_value.release_nonnull())));
                } else {
                    auto number = parse_numeric_value(value);
                    if (!number)
                        return nullptr;
                    values.append(number.release_nonnull());
                }
                break;
            }
            }

            argument_tokens.skip_whitespace();
            if (argument_tokens.has_next_token()) {
                // Arguments must be separated by commas.
                if (!argument_tokens.next_token().is(Token::Type::Comma))
                    return nullptr;
                argument_tokens.skip_whitespace();

                // If there are no more parameters after the comma, this is invalid.
                if (!argument_tokens.has_next_token())
                    return nullptr;
            }
        }

        if (values.size() < function_metadata.min_parameters) {
            dbgln_if(CSS_PARSER_DEBUG, "Not enough arguments to {}. min: {}, given: {}", part.function().name(), function_metadata.min_parameters, values.size());
            return nullptr;
        }

        if (values.size() > function_metadata.max_parameters) {
            dbgln_if(CSS_PARSER_DEBUG, "Too many arguments to {}. max: {}, given: {}", part.function().name(), function_metadata.max_parameters, values.size());
            return nullptr;
        }

        transformations.append(TransformationStyleValue::create(function, move(values)));
    }
    return StyleValueList::create(move(transformations), StyleValueList::Separator::Space);
}

// https://www.w3.org/TR/css-transforms-1/#propdef-transform-origin
// FIXME: This only supports a 2D position
RefPtr<StyleValue> Parser::parse_transform_origin_value(Vector<ComponentValue> const& component_values)
{
    enum class Axis {
        None,
        X,
        Y,
    };

    struct AxisOffset {
        Axis axis;
        NonnullRefPtr<StyleValue> offset;
    };

    auto to_axis_offset = [](RefPtr<StyleValue> value) -> Optional<AxisOffset> {
        if (value->is_percentage())
            return AxisOffset { Axis::None, value->as_percentage() };
        if (value->is_length())
            return AxisOffset { Axis::None, value->as_length() };
        if (value->has_length())
            return AxisOffset { Axis::None, LengthStyleValue::create(value->to_length()) };
        if (value->is_identifier()) {
            switch (value->to_identifier()) {
            case ValueID::Top:
                return AxisOffset { Axis::Y, PercentageStyleValue::create(Percentage(0)) };
            case ValueID::Left:
                return AxisOffset { Axis::X, PercentageStyleValue::create(Percentage(0)) };
            case ValueID::Center:
                return AxisOffset { Axis::None, PercentageStyleValue::create(Percentage(50)) };
            case ValueID::Bottom:
                return AxisOffset { Axis::Y, PercentageStyleValue::create(Percentage(100)) };
            case ValueID::Right:
                return AxisOffset { Axis::X, PercentageStyleValue::create(Percentage(100)) };
            default:
                return {};
            }
        }
        return {};
    };

    auto make_list = [](NonnullRefPtr<StyleValue> const& x_value, NonnullRefPtr<StyleValue> const& y_value) -> NonnullRefPtr<StyleValueList> {
        NonnullRefPtrVector<StyleValue> values;
        values.append(x_value);
        values.append(y_value);
        return StyleValueList::create(move(values), StyleValueList::Separator::Space);
    };

    switch (component_values.size()) {
    case 1: {
        auto single_value = to_axis_offset(parse_css_value(component_values[0]));
        if (!single_value.has_value())
            return nullptr;
        // If only one value is specified, the second value is assumed to be center.
        // FIXME: If one or two values are specified, the third value is assumed to be 0px.
        switch (single_value->axis) {
        case Axis::None:
        case Axis::X:
            return make_list(single_value->offset, PercentageStyleValue::create(Percentage(50)));
        case Axis::Y:
            return make_list(PercentageStyleValue::create(Percentage(50)), single_value->offset);
        }
        VERIFY_NOT_REACHED();
    }
    case 2: {
        auto first_value = to_axis_offset(parse_css_value(component_values[0]));
        auto second_value = to_axis_offset(parse_css_value(component_values[1]));
        if (!first_value.has_value() || !second_value.has_value())
            return nullptr;

        RefPtr<StyleValue> x_value;
        RefPtr<StyleValue> y_value;

        if (first_value->axis == Axis::X) {
            x_value = first_value->offset;
        } else if (first_value->axis == Axis::Y) {
            y_value = first_value->offset;
        }

        if (second_value->axis == Axis::X) {
            if (x_value)
                return nullptr;
            x_value = second_value->offset;
            // Put the other in Y since its axis can't have been X
            y_value = first_value->offset;
        } else if (second_value->axis == Axis::Y) {
            if (y_value)
                return nullptr;
            y_value = second_value->offset;
            // Put the other in X since its axis can't have been Y
            x_value = first_value->offset;
        } else {
            if (x_value) {
                VERIFY(!y_value);
                y_value = second_value->offset;
            } else {
                VERIFY(!x_value);
                x_value = second_value->offset;
            }
        }
        // If two or more values are defined and either no value is a keyword, or the only used keyword is center,
        // then the first value represents the horizontal position (or offset) and the second represents the vertical position (or offset).
        // FIXME: A third value always represents the Z position (or offset) and must be of type <length>.
        if (first_value->axis == Axis::None && second_value->axis == Axis::None) {
            x_value = first_value->offset;
            y_value = second_value->offset;
        }
        return make_list(x_value.release_nonnull(), y_value.release_nonnull());
    }
    }

    return nullptr;
}

RefPtr<StyleValue> Parser::parse_as_css_value(PropertyID property_id)
{
    auto component_values = parse_a_list_of_component_values(m_token_stream);
    auto tokens = TokenStream(component_values);
    auto parsed_value = parse_css_value(property_id, tokens);
    if (parsed_value.is_error())
        return {};
    return parsed_value.release_value();
}

RefPtr<StyleValue> Parser::parse_grid_track_sizes(Vector<ComponentValue> const& component_values)
{
    Vector<CSS::GridTrackSize> params;
    for (auto const& component_value : component_values) {
        // FIXME: Incomplete as a GridTrackSize can be a function like minmax(min, max), etc.
        if (component_value.is_function()) {
            params.append(Length::make_auto());
            continue;
        }
        if (component_value.is_block()) {
            params.append(Length::make_auto());
            continue;
        }
        if (component_value.is(Token::Type::Ident) && component_value.token().ident().equals_ignoring_case("auto"sv)) {
            params.append(Length::make_auto());
            continue;
        }
        if (component_value.token().type() == Token::Type::Dimension) {
            float numeric_value = component_value.token().dimension_value();
            auto unit_string = component_value.token().dimension_unit();
            if (unit_string.equals_ignoring_case("fr"sv) && numeric_value) {
                params.append(GridTrackSize(numeric_value));
                continue;
            }
        }

        auto dimension = parse_dimension(component_value);
        if (!dimension.has_value())
            return GridTrackSizeStyleValue::create({});
        if (dimension->is_length())
            params.append(dimension->length());
        if (dimension->is_percentage())
            params.append(dimension->percentage());
    }
    return GridTrackSizeStyleValue::create(params);
}

RefPtr<StyleValue> Parser::parse_grid_track_placement(Vector<ComponentValue> const& component_values)
{
    auto tokens = TokenStream { component_values };
    auto current_token = tokens.next_token().token();

    if (!tokens.has_next_token()) {
        if (current_token.is(Token::Type::Ident) && current_token.ident().equals_ignoring_case("auto"sv))
            return GridTrackPlacementStyleValue::create(CSS::GridTrackPlacement());
        // https://drafts.csswg.org/css-grid/#grid-placement-span-int
        // If the <integer> is omitted, it defaults to 1.
        if (current_token.is(Token::Type::Ident) && current_token.ident().equals_ignoring_case("span"sv))
            return GridTrackPlacementStyleValue::create(CSS::GridTrackPlacement(1, true));
        if (current_token.is(Token::Type::Number) && current_token.number().is_integer())
            return GridTrackPlacementStyleValue::create(CSS::GridTrackPlacement(static_cast<int>(current_token.number_value())));
        return {};
    }

    auto is_span = false;
    if (current_token.is(Token::Type::Ident) && current_token.ident().equals_ignoring_case("span"sv)) {
        is_span = true;
        tokens.skip_whitespace();
        current_token = tokens.next_token().token();
    }

    if (current_token.is(Token::Type::Number) && current_token.number().is_integer() && !tokens.has_next_token()) {
        // https://drafts.csswg.org/css-grid/#grid-placement-span-int
        // Negative integers or zero are invalid.
        if (is_span && static_cast<int>(current_token.number_value()) < 1)
            return GridTrackPlacementStyleValue::create(CSS::GridTrackPlacement(1, is_span));
        return GridTrackPlacementStyleValue::create(CSS::GridTrackPlacement(static_cast<int>(current_token.number_value()), is_span));
    }
    return {};
}

RefPtr<StyleValue> Parser::parse_grid_track_placement_shorthand_value(Vector<ComponentValue> const& component_values)
{
    auto tokens = TokenStream { component_values };
    auto current_token = tokens.next_token().token();

    if (!tokens.has_next_token()) {
        if (current_token.is(Token::Type::Ident) && current_token.ident().equals_ignoring_case("auto"sv))
            return GridTrackPlacementShorthandStyleValue::create(CSS::GridTrackPlacement::make_auto());
        // https://drafts.csswg.org/css-grid/#grid-placement-span-int
        // If the <integer> is omitted, it defaults to 1.
        if (current_token.is(Token::Type::Ident) && current_token.ident().equals_ignoring_case("span"sv))
            return GridTrackPlacementShorthandStyleValue::create(CSS::GridTrackPlacement(1, true));
        if (current_token.is(Token::Type::Number) && current_token.number().is_integer())
            return GridTrackPlacementShorthandStyleValue::create(CSS::GridTrackPlacement(current_token.number_value()));
        return {};
    }

    auto calculate_grid_track_placement = [](auto& current_token, auto& tokens) -> CSS::GridTrackPlacement {
        auto is_span = false;
        if (current_token.is(Token::Type::Ident) && current_token.ident().equals_ignoring_case("span"sv)) {
            is_span = true;
            tokens.skip_whitespace();
            current_token = tokens.next_token().token();
        }
        if (current_token.is(Token::Type::Number) && current_token.number().is_integer()) {
            // https://drafts.csswg.org/css-grid/#grid-placement-span-int
            // Negative integers or zero are invalid.
            if (is_span && static_cast<int>(current_token.number_value()) < 1)
                return CSS::GridTrackPlacement(1, true);
            return CSS::GridTrackPlacement(static_cast<int>(current_token.number_value()), is_span);
        }
        // https://drafts.csswg.org/css-grid/#grid-placement-span-int
        // If the <integer> is omitted, it defaults to 1.
        if (is_span && current_token.is(Token::Type::Delim) && current_token.delim() == "/"sv)
            return CSS::GridTrackPlacement(1, true);
        return CSS::GridTrackPlacement();
    };

    auto first_grid_track_placement = calculate_grid_track_placement(current_token, tokens);
    if (!tokens.has_next_token())
        return GridTrackPlacementShorthandStyleValue::create(CSS::GridTrackPlacement(first_grid_track_placement));

    tokens.skip_whitespace();
    current_token = tokens.next_token().token();
    tokens.skip_whitespace();
    current_token = tokens.next_token().token();

    auto second_grid_track_placement = calculate_grid_track_placement(current_token, tokens);
    if (!tokens.has_next_token())
        return GridTrackPlacementShorthandStyleValue::create(GridTrackPlacementStyleValue::create(first_grid_track_placement), GridTrackPlacementStyleValue::create(second_grid_track_placement));
    return {};
}

Parser::ParseErrorOr<NonnullRefPtr<StyleValue>> Parser::parse_css_value(PropertyID property_id, TokenStream<ComponentValue>& tokens)
{
    auto function_contains_var_or_attr = [](Function const& function, auto&& recurse) -> bool {
        if (function.name().equals_ignoring_case("var"sv) || function.name().equals_ignoring_case("attr"sv))
            return true;
        for (auto const& token : function.values()) {
            if (token.is_function() && recurse(token.function(), recurse))
                return true;
        }
        return false;
    };
    auto block_contains_var_or_attr = [function_contains_var_or_attr](Block const& block, auto&& recurse) -> bool {
        for (auto const& token : block.values()) {
            if (token.is_function() && function_contains_var_or_attr(token.function(), function_contains_var_or_attr))
                return true;
            if (token.is_block() && recurse(token.block(), recurse))
                return true;
        }
        return false;
    };

    m_context.set_current_property_id(property_id);
    Vector<ComponentValue> component_values;
    bool contains_var_or_attr = false;

    while (tokens.has_next_token()) {
        auto const& token = tokens.next_token();

        if (token.is(Token::Type::Semicolon)) {
            tokens.reconsume_current_input_token();
            break;
        }

        if (property_id != PropertyID::Custom) {
            if (token.is(Token::Type::Whitespace))
                continue;

            if (token.is(Token::Type::Ident) && has_ignored_vendor_prefix(token.token().ident()))
                return ParseError::IncludesIgnoredVendorPrefix;
        }

        if (!contains_var_or_attr) {
            if (token.is_function() && function_contains_var_or_attr(token.function(), function_contains_var_or_attr))
                contains_var_or_attr = true;
            else if (token.is_block() && block_contains_var_or_attr(token.block(), block_contains_var_or_attr))
                contains_var_or_attr = true;
        }

        component_values.append(token);
    }

    if (property_id == PropertyID::Custom || contains_var_or_attr)
        return { UnresolvedStyleValue::create(move(component_values), contains_var_or_attr) };

    if (component_values.is_empty())
        return ParseError::SyntaxError;

    if (component_values.size() == 1) {
        if (auto parsed_value = parse_builtin_value(component_values.first()))
            return parsed_value.release_nonnull();
    }

    // Special-case property handling
    switch (property_id) {
    case PropertyID::BackdropFilter:
        if (auto parsed_value = parse_filter_value_list_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::Background:
        if (auto parsed_value = parse_background_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::BackgroundAttachment:
    case PropertyID::BackgroundClip:
    case PropertyID::BackgroundImage:
    case PropertyID::BackgroundOrigin:
        if (auto parsed_value = parse_simple_comma_separated_value_list(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::BackgroundPosition:
        if (auto parsed_value = parse_comma_separated_value_list(component_values, [this](auto& tokens) { return parse_single_background_position_value(tokens); }))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::BackgroundRepeat:
        if (auto parsed_value = parse_comma_separated_value_list(component_values, [this](auto& tokens) { return parse_single_background_repeat_value(tokens); }))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::BackgroundSize:
        if (auto parsed_value = parse_comma_separated_value_list(component_values, [this](auto& tokens) { return parse_single_background_size_value(tokens); }))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::Border:
    case PropertyID::BorderBottom:
    case PropertyID::BorderLeft:
    case PropertyID::BorderRight:
    case PropertyID::BorderTop:
        if (auto parsed_value = parse_border_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::BorderTopLeftRadius:
    case PropertyID::BorderTopRightRadius:
    case PropertyID::BorderBottomRightRadius:
    case PropertyID::BorderBottomLeftRadius:
        if (auto parsed_value = parse_border_radius_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::BorderRadius:
        if (auto parsed_value = parse_border_radius_shorthand_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::BoxShadow:
        if (auto parsed_value = parse_shadow_value(component_values, AllowInsetKeyword::Yes))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::Content:
        if (auto parsed_value = parse_content_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::Flex:
        if (auto parsed_value = parse_flex_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::FlexFlow:
        if (auto parsed_value = parse_flex_flow_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::Font:
        if (auto parsed_value = parse_font_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::FontFamily:
        if (auto parsed_value = parse_font_family_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridColumn:
        if (auto parsed_value = parse_grid_track_placement_shorthand_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridColumnEnd:
        if (auto parsed_value = parse_grid_track_placement(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridColumnStart:
        if (auto parsed_value = parse_grid_track_placement(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridRow:
        if (auto parsed_value = parse_grid_track_placement_shorthand_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridRowEnd:
        if (auto parsed_value = parse_grid_track_placement(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridRowStart:
        if (auto parsed_value = parse_grid_track_placement(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridTemplateColumns:
        if (auto parsed_value = parse_grid_track_sizes(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::GridTemplateRows:
        if (auto parsed_value = parse_grid_track_sizes(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::ListStyle:
        if (auto parsed_value = parse_list_style_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::Overflow:
        if (auto parsed_value = parse_overflow_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::TextDecoration:
        if (auto parsed_value = parse_text_decoration_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::TextDecorationLine: {
        TokenStream value_tokens { component_values };
        auto parsed_value = parse_text_decoration_line_value(value_tokens);
        if (parsed_value && !value_tokens.has_next_token())
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    }
    case PropertyID::TextShadow:
        if (auto parsed_value = parse_shadow_value(component_values, AllowInsetKeyword::No))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::Transform:
        if (auto parsed_value = parse_transform_value(component_values))
            return parsed_value.release_nonnull();
        return ParseError::SyntaxError;
    case PropertyID::TransformOrigin:
        if (auto parse_value = parse_transform_origin_value(component_values))
            return parse_value.release_nonnull();
        return ParseError ::SyntaxError;
    default:
        break;
    }

    if (component_values.size() == 1) {
        if (auto parsed_value = parse_css_value(component_values.first())) {
            if (property_accepts_value(property_id, *parsed_value))
                return parsed_value.release_nonnull();
        }
        return ParseError::SyntaxError;
    }

    // We have multiple values, so treat them as a StyleValueList.
    if (property_maximum_value_count(property_id) > 1) {
        NonnullRefPtrVector<StyleValue> parsed_values;
        for (auto& component_value : component_values) {
            auto parsed_value = parse_css_value(component_value);
            if (!parsed_value || !property_accepts_value(property_id, *parsed_value))
                return ParseError::SyntaxError;
            parsed_values.append(parsed_value.release_nonnull());
        }
        if (!parsed_values.is_empty() && parsed_values.size() <= property_maximum_value_count(property_id))
            return { StyleValueList::create(move(parsed_values), StyleValueList::Separator::Space) };
    }

    return ParseError::SyntaxError;
}

RefPtr<StyleValue> Parser::parse_css_value(ComponentValue const& component_value)
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

    if (auto rect = parse_rect_value(component_value))
        return rect;

    return {};
}

Optional<Selector::SimpleSelector::ANPlusBPattern> Parser::parse_a_n_plus_b_pattern(TokenStream<ComponentValue>& values)
{
    auto transaction = values.begin_transaction();
    auto syntax_error = [&]() -> Optional<Selector::SimpleSelector::ANPlusBPattern> {
        if constexpr (CSS_PARSER_DEBUG) {
            dbgln_if(CSS_PARSER_DEBUG, "Invalid An+B value:");
            values.dump_all_tokens();
        }
        return {};
    };

    auto is_n = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("n"sv);
    };
    auto is_ndash = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("n-"sv);
    };
    auto is_dashn = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("-n"sv);
    };
    auto is_dashndash = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_case("-n-"sv);
    };
    auto is_delim = [](ComponentValue const& value, u32 delim) -> bool {
        return value.is(Token::Type::Delim) && value.token().delim() == delim;
    };
    auto is_sign = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Delim) && (value.token().delim() == '+' || value.token().delim() == '-');
    };
    auto is_n_dimension = [](ComponentValue const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (!value.token().number().is_integer())
            return false;
        if (!value.token().dimension_unit().equals_ignoring_case("n"sv))
            return false;
        return true;
    };
    auto is_ndash_dimension = [](ComponentValue const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (!value.token().number().is_integer())
            return false;
        if (!value.token().dimension_unit().equals_ignoring_case("n-"sv))
            return false;
        return true;
    };
    auto is_ndashdigit_dimension = [](ComponentValue const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (!value.token().number().is_integer())
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
    auto is_ndashdigit_ident = [](ComponentValue const& value) -> bool {
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
    auto is_dashndashdigit_ident = [](ComponentValue const& value) -> bool {
        if (!value.is(Token::Type::Ident))
            return false;
        auto ident = value.token().ident();
        if (!ident.starts_with("-n-"sv, CaseSensitivity::CaseInsensitive))
            return false;
        if (ident.length() == 3)
            return false;
        for (size_t i = 3; i < ident.length(); ++i) {
            if (!is_ascii_digit(ident[i]))
                return false;
        }
        return true;
    };
    auto is_integer = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Number) && value.token().number().is_integer();
    };
    auto is_signed_integer = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Number) && value.token().number().is_integer_with_explicit_sign();
    };
    auto is_signless_integer = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Number) && !value.token().number().is_integer_with_explicit_sign();
    };

    // https://www.w3.org/TR/css-syntax-3/#the-anb-type
    // Unfortunately these can't be in the same order as in the spec.

    values.skip_whitespace();
    auto const& first_value = values.next_token();

    // odd | even
    if (first_value.is(Token::Type::Ident)) {
        auto ident = first_value.token().ident();
        if (ident.equals_ignoring_case("odd"sv)) {
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { 2, 1 };
        }
        if (ident.equals_ignoring_case("even"sv)) {
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { 2, 0 };
        }
    }
    // <integer>
    if (is_integer(first_value)) {
        int b = first_value.token().to_integer();
        transaction.commit();
        return Selector::SimpleSelector::ANPlusBPattern { 0, b };
    }
    // <n-dimension>
    // <n-dimension> <signed-integer>
    // <n-dimension> ['+' | '-'] <signless-integer>
    if (is_n_dimension(first_value)) {
        int a = first_value.token().dimension_value_int();
        values.skip_whitespace();

        // <n-dimension> <signed-integer>
        if (is_signed_integer(values.peek_token())) {
            int b = values.next_token().token().to_integer();
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { a, b };
        }

        // <n-dimension> ['+' | '-'] <signless-integer>
        {
            auto child_transaction = transaction.create_child();
            auto const& second_value = values.next_token();
            values.skip_whitespace();
            auto const& third_value = values.next_token();

            if (is_sign(second_value) && is_signless_integer(third_value)) {
                int b = third_value.token().to_integer() * (is_delim(second_value, '+') ? 1 : -1);
                child_transaction.commit();
                return Selector::SimpleSelector::ANPlusBPattern { a, b };
            }
        }

        // <n-dimension>
        transaction.commit();
        return Selector::SimpleSelector::ANPlusBPattern { a, 0 };
    }
    // <ndash-dimension> <signless-integer>
    if (is_ndash_dimension(first_value)) {
        values.skip_whitespace();
        auto const& second_value = values.next_token();
        if (is_signless_integer(second_value)) {
            int a = first_value.token().dimension_value_int();
            int b = -second_value.token().to_integer();
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { a, b };
        }

        return syntax_error();
    }
    // <ndashdigit-dimension>
    if (is_ndashdigit_dimension(first_value)) {
        auto const& dimension = first_value.token();
        int a = dimension.dimension_value_int();
        auto maybe_b = dimension.dimension_unit().substring_view(1).to_int();
        if (maybe_b.has_value()) {
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { a, maybe_b.value() };
        }

        return syntax_error();
    }
    // <dashndashdigit-ident>
    if (is_dashndashdigit_ident(first_value)) {
        auto maybe_b = first_value.token().ident().substring_view(2).to_int();
        if (maybe_b.has_value()) {
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { -1, maybe_b.value() };
        }

        return syntax_error();
    }
    // -n
    // -n <signed-integer>
    // -n ['+' | '-'] <signless-integer>
    if (is_dashn(first_value)) {
        values.skip_whitespace();

        // -n <signed-integer>
        if (is_signed_integer(values.peek_token())) {
            int b = values.next_token().token().to_integer();
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { -1, b };
        }

        // -n ['+' | '-'] <signless-integer>
        {
            auto child_transaction = transaction.create_child();
            auto const& second_value = values.next_token();
            values.skip_whitespace();
            auto const& third_value = values.next_token();

            if (is_sign(second_value) && is_signless_integer(third_value)) {
                int b = third_value.token().to_integer() * (is_delim(second_value, '+') ? 1 : -1);
                child_transaction.commit();
                return Selector::SimpleSelector::ANPlusBPattern { -1, b };
            }
        }

        // -n
        transaction.commit();
        return Selector::SimpleSelector::ANPlusBPattern { -1, 0 };
    }
    // -n- <signless-integer>
    if (is_dashndash(first_value)) {
        values.skip_whitespace();
        auto const& second_value = values.next_token();
        if (is_signless_integer(second_value)) {
            int b = -second_value.token().to_integer();
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { -1, b };
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
    if (!is_delim(first_value, '+')) {
        values.reconsume_current_input_token();
        // We do *not* skip whitespace here.
    }

    auto const& first_after_plus = values.next_token();
    // '+'?† n
    // '+'?† n <signed-integer>
    // '+'?† n ['+' | '-'] <signless-integer>
    if (is_n(first_after_plus)) {
        values.skip_whitespace();

        // '+'?† n <signed-integer>
        if (is_signed_integer(values.peek_token())) {
            int b = values.next_token().token().to_integer();
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { 1, b };
        }

        // '+'?† n ['+' | '-'] <signless-integer>
        {
            auto child_transaction = transaction.create_child();
            auto const& second_value = values.next_token();
            values.skip_whitespace();
            auto const& third_value = values.next_token();

            if (is_sign(second_value) && is_signless_integer(third_value)) {
                int b = third_value.token().to_integer() * (is_delim(second_value, '+') ? 1 : -1);
                child_transaction.commit();
                return Selector::SimpleSelector::ANPlusBPattern { 1, b };
            }
        }

        // '+'?† n
        transaction.commit();
        return Selector::SimpleSelector::ANPlusBPattern { 1, 0 };
    }

    // '+'?† n- <signless-integer>
    if (is_ndash(first_after_plus)) {
        values.skip_whitespace();
        auto const& second_value = values.next_token();
        if (is_signless_integer(second_value)) {
            int b = -second_value.token().to_integer();
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { 1, b };
        }

        return syntax_error();
    }

    // '+'?† <ndashdigit-ident>
    if (is_ndashdigit_ident(first_after_plus)) {
        auto maybe_b = first_after_plus.token().ident().substring_view(1).to_int();
        if (maybe_b.has_value()) {
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { 1, maybe_b.value() };
        }

        return syntax_error();
    }

    return syntax_error();
}

OwnPtr<CalculatedStyleValue::CalcSum> Parser::parse_calc_expression(Vector<ComponentValue> const& values)
{
    auto tokens = TokenStream(values);
    return parse_calc_sum(tokens);
}

Optional<CalculatedStyleValue::CalcValue> Parser::parse_calc_value(TokenStream<ComponentValue>& tokens)
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
        return CalculatedStyleValue::CalcValue { current_token.token().number() };

    if (current_token.is(Token::Type::Dimension) || current_token.is(Token::Type::Percentage)) {
        auto maybe_dimension = parse_dimension(current_token);
        if (!maybe_dimension.has_value())
            return {};
        auto& dimension = maybe_dimension.value();

        if (dimension.is_angle())
            return CalculatedStyleValue::CalcValue { dimension.angle() };
        if (dimension.is_frequency())
            return CalculatedStyleValue::CalcValue { dimension.frequency() };
        if (dimension.is_length())
            return CalculatedStyleValue::CalcValue { dimension.length() };
        if (dimension.is_percentage())
            return CalculatedStyleValue::CalcValue { dimension.percentage() };
        if (dimension.is_resolution()) {
            // Resolution is not allowed in calc()
            return {};
        }
        if (dimension.is_time())
            return CalculatedStyleValue::CalcValue { dimension.time() };
        VERIFY_NOT_REACHED();
    }

    return {};
}

OwnPtr<CalculatedStyleValue::CalcProductPartWithOperator> Parser::parse_calc_product_part_with_operator(TokenStream<ComponentValue>& tokens)
{
    // Note: The default value is not used or passed around.
    auto product_with_operator = make<CalculatedStyleValue::CalcProductPartWithOperator>(
        CalculatedStyleValue::ProductOperation::Multiply,
        CalculatedStyleValue::CalcNumberValue { Number {} });

    tokens.skip_whitespace();

    auto const& op_token = tokens.peek_token();
    if (!op_token.is(Token::Type::Delim))
        return nullptr;

    auto op = op_token.token().delim();
    if (op == '*') {
        tokens.next_token();
        tokens.skip_whitespace();
        product_with_operator->op = CalculatedStyleValue::ProductOperation::Multiply;
        auto parsed_calc_value = parse_calc_value(tokens);
        if (!parsed_calc_value.has_value())
            return nullptr;
        product_with_operator->value = { parsed_calc_value.release_value() };

    } else if (op == '/') {
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

OwnPtr<CalculatedStyleValue::CalcNumberProductPartWithOperator> Parser::parse_calc_number_product_part_with_operator(TokenStream<ComponentValue>& tokens)
{
    // Note: The default value is not used or passed around.
    auto number_product_with_operator = make<CalculatedStyleValue::CalcNumberProductPartWithOperator>(
        CalculatedStyleValue::ProductOperation::Multiply,
        CalculatedStyleValue::CalcNumberValue { Number {} });

    tokens.skip_whitespace();

    auto const& op_token = tokens.peek_token();
    if (!op_token.is(Token::Type::Delim))
        return nullptr;

    auto op = op_token.token().delim();
    if (op == '*') {
        tokens.next_token();
        tokens.skip_whitespace();
        number_product_with_operator->op = CalculatedStyleValue::ProductOperation::Multiply;
    } else if (op == '/') {
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

OwnPtr<CalculatedStyleValue::CalcNumberProduct> Parser::parse_calc_number_product(TokenStream<ComponentValue>& tokens)
{
    auto calc_number_product = make<CalculatedStyleValue::CalcNumberProduct>(
        CalculatedStyleValue::CalcNumberValue { Number {} },
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

OwnPtr<CalculatedStyleValue::CalcNumberSumPartWithOperator> Parser::parse_calc_number_sum_part_with_operator(TokenStream<ComponentValue>& tokens)
{
    if (!(tokens.peek_token().is(Token::Type::Delim)
            && (tokens.peek_token().token().delim() == '+' || tokens.peek_token().token().delim() == '-')
            && tokens.peek_token(1).is(Token::Type::Whitespace)))
        return nullptr;

    auto const& token = tokens.next_token();
    tokens.skip_whitespace();

    CalculatedStyleValue::SumOperation op;
    auto delim = token.token().delim();
    if (delim == '+')
        op = CalculatedStyleValue::SumOperation::Add;
    else if (delim == '-')
        op = CalculatedStyleValue::SumOperation::Subtract;
    else
        return nullptr;

    auto calc_number_product = parse_calc_number_product(tokens);
    if (!calc_number_product)
        return nullptr;
    return make<CalculatedStyleValue::CalcNumberSumPartWithOperator>(op, calc_number_product.release_nonnull());
}

OwnPtr<CalculatedStyleValue::CalcNumberSum> Parser::parse_calc_number_sum(TokenStream<ComponentValue>& tokens)
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

Optional<CalculatedStyleValue::CalcNumberValue> Parser::parse_calc_number_value(TokenStream<ComponentValue>& tokens)
{
    auto const& first = tokens.peek_token();
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

    return CalculatedStyleValue::CalcNumberValue { first.token().number() };
}

OwnPtr<CalculatedStyleValue::CalcProduct> Parser::parse_calc_product(TokenStream<ComponentValue>& tokens)
{
    auto calc_product = make<CalculatedStyleValue::CalcProduct>(
        CalculatedStyleValue::CalcValue { Number {} },
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

OwnPtr<CalculatedStyleValue::CalcSumPartWithOperator> Parser::parse_calc_sum_part_with_operator(TokenStream<ComponentValue>& tokens)
{
    // The following has to have the shape of <Whitespace><+ or -><Whitespace>
    // But the first whitespace gets eaten in parse_calc_product_part_with_operator().
    if (!(tokens.peek_token().is(Token::Type::Delim)
            && (tokens.peek_token().token().delim() == '+' || tokens.peek_token().token().delim() == '-')
            && tokens.peek_token(1).is(Token::Type::Whitespace)))
        return nullptr;

    auto const& token = tokens.next_token();
    tokens.skip_whitespace();

    CalculatedStyleValue::SumOperation op;
    auto delim = token.token().delim();
    if (delim == '+')
        op = CalculatedStyleValue::SumOperation::Add;
    else if (delim == '-')
        op = CalculatedStyleValue::SumOperation::Subtract;
    else
        return nullptr;

    auto calc_product = parse_calc_product(tokens);
    if (!calc_product)
        return nullptr;
    return make<CalculatedStyleValue::CalcSumPartWithOperator>(op, calc_product.release_nonnull());
};

OwnPtr<CalculatedStyleValue::CalcSum> Parser::parse_calc_sum(TokenStream<ComponentValue>& tokens)
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
    if (string.starts_with("--"sv))
        return false;
    if (string.starts_with("-libweb-"sv))
        return false;
    return true;
}

bool Parser::is_builtin(StringView name)
{
    return name.equals_ignoring_case("inherit"sv)
        || name.equals_ignoring_case("initial"sv)
        || name.equals_ignoring_case("unset"sv);
}

RefPtr<StyleValue> Parser::parse_css_value(Badge<StyleComputer>, ParsingContext const& context, PropertyID property_id, Vector<ComponentValue> const& tokens)
{
    if (tokens.is_empty() || property_id == CSS::PropertyID::Invalid || property_id == CSS::PropertyID::Custom)
        return {};

    Parser parser(context, ""sv);
    TokenStream<ComponentValue> token_stream { tokens };
    auto result = parser.parse_css_value(property_id, token_stream);
    if (result.is_error())
        return {};
    return result.release_value();
}

bool Parser::Dimension::is_angle() const
{
    return m_value.has<Angle>();
}

Angle Parser::Dimension::angle() const
{
    return m_value.get<Angle>();
}

bool Parser::Dimension::is_angle_percentage() const
{
    return is_angle() || is_percentage();
}

AnglePercentage Parser::Dimension::angle_percentage() const
{
    if (is_angle())
        return angle();
    if (is_percentage())
        return percentage();
    VERIFY_NOT_REACHED();
}

bool Parser::Dimension::is_frequency() const
{
    return m_value.has<Frequency>();
}

Frequency Parser::Dimension::frequency() const
{
    return m_value.get<Frequency>();
}

bool Parser::Dimension::is_frequency_percentage() const
{
    return is_frequency() || is_percentage();
}

FrequencyPercentage Parser::Dimension::frequency_percentage() const
{
    if (is_frequency())
        return frequency();
    if (is_percentage())
        return percentage();
    VERIFY_NOT_REACHED();
}

bool Parser::Dimension::is_length() const
{
    return m_value.has<Length>();
}

Length Parser::Dimension::length() const
{
    return m_value.get<Length>();
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

bool Parser::Dimension::is_percentage() const
{
    return m_value.has<Percentage>();
}

Percentage Parser::Dimension::percentage() const
{
    return m_value.get<Percentage>();
}

bool Parser::Dimension::is_resolution() const
{
    return m_value.has<Resolution>();
}

Resolution Parser::Dimension::resolution() const
{
    return m_value.get<Resolution>();
}

bool Parser::Dimension::is_time() const
{
    return m_value.has<Time>();
}

Time Parser::Dimension::time() const
{
    return m_value.get<Time>();
}

bool Parser::Dimension::is_time_percentage() const
{
    return is_time() || is_percentage();
}

TimePercentage Parser::Dimension::time_percentage() const
{
    if (is_time())
        return time();
    if (is_percentage())
        return percentage();
    VERIFY_NOT_REACHED();
}
}

namespace Web {

CSS::CSSStyleSheet* parse_css_stylesheet(CSS::Parser::ParsingContext const& context, StringView css, Optional<AK::URL> location)
{
    if (css.is_empty())
        return CSS::CSSStyleSheet::create(context.realm(), *CSS::CSSRuleList::create_empty(context.realm()), location);
    CSS::Parser::Parser parser(context, css);
    return parser.parse_as_css_stylesheet(location);
}

CSS::ElementInlineCSSStyleDeclaration* parse_css_style_attribute(CSS::Parser::ParsingContext const& context, StringView css, DOM::Element& element)
{
    if (css.is_empty())
        return CSS::ElementInlineCSSStyleDeclaration::create(element, {}, {});
    CSS::Parser::Parser parser(context, css);
    return parser.parse_as_style_attribute(element);
}

RefPtr<CSS::StyleValue> parse_css_value(CSS::Parser::ParsingContext const& context, StringView string, CSS::PropertyID property_id)
{
    if (string.is_empty())
        return {};
    CSS::Parser::Parser parser(context, string);
    return parser.parse_as_css_value(property_id);
}

CSS::CSSRule* parse_css_rule(CSS::Parser::ParsingContext const& context, StringView css_text)
{
    CSS::Parser::Parser parser(context, css_text);
    return parser.parse_as_css_rule();
}

Optional<CSS::SelectorList> parse_selector(CSS::Parser::ParsingContext const& context, StringView selector_text)
{
    CSS::Parser::Parser parser(context, selector_text);
    return parser.parse_as_selector();
}

RefPtr<CSS::MediaQuery> parse_media_query(CSS::Parser::ParsingContext const& context, StringView string)
{
    CSS::Parser::Parser parser(context, string);
    return parser.parse_as_media_query();
}

NonnullRefPtrVector<CSS::MediaQuery> parse_media_query_list(CSS::Parser::ParsingContext const& context, StringView string)
{
    CSS::Parser::Parser parser(context, string);
    return parser.parse_as_media_query_list();
}

RefPtr<CSS::Supports> parse_css_supports(CSS::Parser::ParsingContext const& context, StringView string)
{
    if (string.is_empty())
        return {};
    CSS::Parser::Parser parser(context, string);
    return parser.parse_as_supports();
}

}
