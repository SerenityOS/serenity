/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS::Parser {

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

    Vector<NonnullRefPtr<Selector>> selectors;
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
template Parser::ParseErrorOr<SelectorList> Parser::parse_a_selector_list(TokenStream<ComponentValue>&, SelectorType, SelectorParsingMode);
template Parser::ParseErrorOr<SelectorList> Parser::parse_a_selector_list(TokenStream<Token>&, SelectorType, SelectorParsingMode);

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

            if (next.is_delim('|')) {
                tokens.next_token();
                return Selector::Combinator::Column;
            }
        }
        }
    }

    tokens.reconsume_current_input_token();
    return {};
}

Optional<Selector::SimpleSelector::QualifiedName> Parser::parse_selector_qualified_name(TokenStream<ComponentValue>& tokens, AllowWildcardName allow_wildcard_name)
{
    auto is_name = [](ComponentValue const& token) {
        return token.is_delim('*') || token.is(Token::Type::Ident);
    };
    auto get_name = [](ComponentValue const& token) {
        if (token.is_delim('*'))
            return FlyString::from_utf8("*"sv);
        return FlyString::from_utf8(token.token().ident());
    };

    // There are 3 possibilities here:
    // (Where <name> and <namespace> are either an <ident> or a `*` delim)
    // 1) `|<name>`
    // 2) `<namespace>|<name>`
    // 3) `<name>`
    // Whitespace is forbidden between any of these parts. https://www.w3.org/TR/selectors-4/#white-space

    auto transaction = tokens.begin_transaction();

    auto first_token = tokens.next_token();
    if (first_token.is_delim('|')) {
        // Case 1: `|<name>`
        if (is_name(tokens.peek_token())) {
            auto name_token = tokens.next_token();

            if (allow_wildcard_name == AllowWildcardName::No && name_token.is_delim('*'))
                return {};

            transaction.commit();
            return Selector::SimpleSelector::QualifiedName {
                .namespace_type = Selector::SimpleSelector::QualifiedName::NamespaceType::None,
                .name = get_name(name_token).release_value_but_fixme_should_propagate_errors(),
            };
        }
        return {};
    }

    if (!is_name(first_token))
        return {};

    if (tokens.peek_token().is_delim('|') && is_name(tokens.peek_token(1))) {
        // Case 2: `<namespace>|<name>`
        (void)tokens.next_token(); // `|`
        auto namespace_ = get_name(first_token).release_value_but_fixme_should_propagate_errors();
        auto name = get_name(tokens.next_token()).release_value_but_fixme_should_propagate_errors();

        if (allow_wildcard_name == AllowWildcardName::No && name == "*"sv)
            return {};

        auto namespace_type = namespace_ == "*"sv
            ? Selector::SimpleSelector::QualifiedName::NamespaceType::Any
            : Selector::SimpleSelector::QualifiedName::NamespaceType::Named;

        transaction.commit();
        return Selector::SimpleSelector::QualifiedName {
            .namespace_type = namespace_type,
            .namespace_ = namespace_,
            .name = name,
        };
    }

    // Case 3: `<name>`
    auto& name_token = first_token;
    if (allow_wildcard_name == AllowWildcardName::No && name_token.is_delim('*'))
        return {};

    transaction.commit();
    return Selector::SimpleSelector::QualifiedName {
        .namespace_type = Selector::SimpleSelector::QualifiedName::NamespaceType::Default,
        .name = get_name(name_token).release_value_but_fixme_should_propagate_errors(),
    };
}

Parser::ParseErrorOr<Selector::SimpleSelector> Parser::parse_attribute_simple_selector(ComponentValue const& first_value)
{
    auto attribute_tokens = TokenStream { first_value.block().values() };

    attribute_tokens.skip_whitespace();

    if (!attribute_tokens.has_next_token()) {
        dbgln_if(CSS_PARSER_DEBUG, "CSS attribute selector is empty!");
        return ParseError::SyntaxError;
    }

    auto maybe_qualified_name = parse_selector_qualified_name(attribute_tokens, AllowWildcardName::No);
    if (!maybe_qualified_name.has_value()) {
        dbgln_if(CSS_PARSER_DEBUG, "Expected qualified-name for attribute name, got: '{}'", attribute_tokens.peek_token().to_debug_string());
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
            .qualified_name = maybe_qualified_name.release_value(),
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
        if (!delim_second_part.is_delim('=')) {
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
    auto value_string_view = value_part.token().is(Token::Type::Ident) ? value_part.token().ident() : value_part.token().string();
    simple_selector.attribute().value = String::from_utf8(value_string_view).release_value_but_fixme_should_propagate_errors();

    attribute_tokens.skip_whitespace();
    // Handle case-sensitivity suffixes. https://www.w3.org/TR/selectors-4/#attribute-case
    if (attribute_tokens.has_next_token()) {
        auto const& case_sensitivity_part = attribute_tokens.next_token();
        if (case_sensitivity_part.is(Token::Type::Ident)) {
            auto case_sensitivity = case_sensitivity_part.token().ident();
            if (case_sensitivity.equals_ignoring_ascii_case("i"sv)) {
                simple_selector.attribute().case_type = Selector::SimpleSelector::Attribute::CaseType::CaseInsensitiveMatch;
            } else if (case_sensitivity.equals_ignoring_ascii_case("s"sv)) {
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
                .value = Selector::SimpleSelector::PseudoClassSelector { .type = pseudo_class }
            };
        };

        if (auto pseudo_class = pseudo_class_from_string(pseudo_name); pseudo_class.has_value()) {
            if (!pseudo_class_metadata(pseudo_class.value()).is_valid_as_identifier) {
                dbgln_if(CSS_PARSER_DEBUG, "Pseudo-class ':{}' is only valid as a function", pseudo_name);
                return ParseError::SyntaxError;
            }
            return make_pseudo_class_selector(pseudo_class.value());
        }

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
                    .value = Selector::SimpleSelector::PseudoClassSelector {
                        .type = pseudo_class,
                        .nth_child_pattern = nth_child_pattern.release_value() }
                };
            }

            if (!allow_of)
                return ParseError::SyntaxError;

            // Parse the `of <selector-list>` syntax
            auto const& maybe_of = tokens.next_token();
            if (!(maybe_of.is(Token::Type::Ident) && maybe_of.token().ident().equals_ignoring_ascii_case("of"sv)))
                return ParseError::SyntaxError;

            tokens.skip_whitespace();
            auto selector_list = TRY(parse_a_selector_list(tokens, SelectorType::Standalone));

            tokens.skip_whitespace();
            if (tokens.has_next_token())
                return ParseError::SyntaxError;

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClassSelector {
                    .type = pseudo_class,
                    .nth_child_pattern = nth_child_pattern.release_value(),
                    .argument_selector_list = move(selector_list) }
            };
        };

        auto const& pseudo_function = pseudo_class_token.function();
        auto maybe_pseudo_class = pseudo_class_from_string(pseudo_function.name());
        if (!maybe_pseudo_class.has_value()) {
            dbgln_if(CSS_PARSER_DEBUG, "Unrecognized pseudo-class function: ':{}'()", pseudo_function.name());
            return ParseError::SyntaxError;
        }
        auto pseudo_class = maybe_pseudo_class.value();
        auto metadata = pseudo_class_metadata(pseudo_class);

        if (!metadata.is_valid_as_function) {
            dbgln_if(CSS_PARSER_DEBUG, "Pseudo-class ':{}' is not valid as a function", pseudo_function.name());
            return ParseError::SyntaxError;
        }

        if (pseudo_function.values().is_empty()) {
            dbgln_if(CSS_PARSER_DEBUG, "Empty :{}() selector", pseudo_function.name());
            return ParseError::SyntaxError;
        }

        switch (metadata.parameter_type) {
        case PseudoClassMetadata::ParameterType::ANPlusB:
            return parse_nth_child_selector(pseudo_class, pseudo_function.values(), false);
        case PseudoClassMetadata::ParameterType::ANPlusBOf:
            return parse_nth_child_selector(pseudo_class, pseudo_function.values(), true);
        case PseudoClassMetadata::ParameterType::CompoundSelector: {
            auto function_token_stream = TokenStream(pseudo_function.values());
            auto compound_selector_or_error = parse_compound_selector(function_token_stream);
            if (compound_selector_or_error.is_error() || !compound_selector_or_error.value().has_value()) {
                dbgln_if(CSS_PARSER_DEBUG, "Failed to parse :{}() parameter as a compound selector", pseudo_function.name());
                return ParseError::SyntaxError;
            }

            Vector compound_selectors { compound_selector_or_error.release_value().release_value() };
            auto selector = Selector::create(move(compound_selectors));

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClassSelector {
                    .type = pseudo_class,
                    .argument_selector_list = { move(selector) } }
            };
        }
        case PseudoClassMetadata::ParameterType::ForgivingSelectorList: {
            auto function_token_stream = TokenStream(pseudo_function.values());
            // NOTE: Because it's forgiving, even complete garbage will parse OK as an empty selector-list.
            auto argument_selector_list = MUST(parse_a_selector_list(function_token_stream, SelectorType::Standalone, SelectorParsingMode::Forgiving));

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClassSelector {
                    .type = pseudo_class,
                    .argument_selector_list = move(argument_selector_list) }
            };
        }
        case PseudoClassMetadata::ParameterType::Ident: {
            auto function_token_stream = TokenStream(pseudo_function.values());
            function_token_stream.skip_whitespace();
            auto maybe_ident_token = function_token_stream.next_token();
            function_token_stream.skip_whitespace();
            if (!maybe_ident_token.is(Token::Type::Ident) || function_token_stream.has_next_token()) {
                dbgln_if(CSS_PARSER_DEBUG, "Failed to parse :{}() parameter as an ident: not an ident", pseudo_function.name());
                return ParseError::SyntaxError;
            }

            auto maybe_ident = value_id_from_string(maybe_ident_token.token().ident());
            if (!maybe_ident.has_value()) {
                dbgln_if(CSS_PARSER_DEBUG, "Failed to parse :{}() parameter as an ident: unrecognized ident", pseudo_function.name());
                return ParseError::SyntaxError;
            }

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClassSelector {
                    .type = pseudo_class,
                    .identifier = maybe_ident.value() }
            };
        }
        case PseudoClassMetadata::ParameterType::LanguageRanges: {
            Vector<FlyString> languages;
            auto function_token_stream = TokenStream(pseudo_function.values());
            auto language_token_lists = parse_a_comma_separated_list_of_component_values(function_token_stream);

            for (auto language_token_list : language_token_lists) {
                auto language_token_stream = TokenStream(language_token_list);
                language_token_stream.skip_whitespace();
                auto language_token = language_token_stream.next_token();
                if (!(language_token.is(Token::Type::Ident) || language_token.is(Token::Type::String))) {
                    dbgln_if(CSS_PARSER_DEBUG, "Invalid language range in :{}() - not a string/ident", pseudo_function.name());
                    return ParseError::SyntaxError;
                }

                auto language_string = language_token.is(Token::Type::String) ? language_token.token().string() : language_token.token().ident();
                languages.append(MUST(FlyString::from_utf8(language_string)));

                language_token_stream.skip_whitespace();
                if (language_token_stream.has_next_token()) {
                    dbgln_if(CSS_PARSER_DEBUG, "Invalid language range in :{}() - trailing tokens", pseudo_function.name());
                    return ParseError::SyntaxError;
                }
            }

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClassSelector {
                    .type = pseudo_class,
                    .languages = move(languages) }
            };
        }
        case PseudoClassMetadata::ParameterType::SelectorList: {
            auto function_token_stream = TokenStream(pseudo_function.values());
            auto not_selector = TRY(parse_a_selector_list(function_token_stream, SelectorType::Standalone));

            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::PseudoClass,
                .value = Selector::SimpleSelector::PseudoClassSelector {
                    .type = pseudo_class,
                    .argument_selector_list = move(not_selector) }
            };
        }
        case PseudoClassMetadata::ParameterType::None:
            // `None` means this is not a function-type pseudo-class, so this state should be impossible.
            VERIFY_NOT_REACHED();
        }
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

    // Handle universal and tag-name types together, since both can be namespaced
    if (auto qualified_name = parse_selector_qualified_name(tokens, AllowWildcardName::Yes); qualified_name.has_value()) {
        if (qualified_name->name.name == "*"sv) {
            return Selector::SimpleSelector {
                .type = Selector::SimpleSelector::Type::Universal,
                .value = qualified_name.release_value(),
            };
        }
        return Selector::SimpleSelector {
            .type = Selector::SimpleSelector::Type::TagName,
            .value = qualified_name.release_value(),
        };
    }

    auto const& first_value = tokens.next_token();

    if (first_value.is(Token::Type::Delim)) {
        u32 delim = first_value.token().delim();
        switch (delim) {
        case '*':
            // Handled already
            VERIFY_NOT_REACHED();
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
                .value = Selector::SimpleSelector::Name { FlyString::from_utf8(class_name_value.token().ident()).release_value_but_fixme_should_propagate_errors() }
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
            .value = Selector::SimpleSelector::Name { FlyString::from_utf8(first_value.token().hash_value()).release_value_but_fixme_should_propagate_errors() }
        };
    }

    if (first_value.is_block() && first_value.block().is_square())
        return TRY(parse_attribute_simple_selector(first_value));

    if (first_value.is(Token::Type::Colon))
        return TRY(parse_pseudo_simple_selector(tokens));

    dbgln_if(CSS_PARSER_DEBUG, "!!! Invalid simple selector!");
    return ParseError::SyntaxError;
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
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_ascii_case("n"sv);
    };
    auto is_ndash = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_ascii_case("n-"sv);
    };
    auto is_dashn = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_ascii_case("-n"sv);
    };
    auto is_dashndash = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Ident) && value.token().ident().equals_ignoring_ascii_case("-n-"sv);
    };
    auto is_sign = [](ComponentValue const& value) -> bool {
        return value.is(Token::Type::Delim) && (value.token().delim() == '+' || value.token().delim() == '-');
    };
    auto is_n_dimension = [](ComponentValue const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (!value.token().number().is_integer())
            return false;
        if (!value.token().dimension_unit().equals_ignoring_ascii_case("n"sv))
            return false;
        return true;
    };
    auto is_ndash_dimension = [](ComponentValue const& value) -> bool {
        if (!value.is(Token::Type::Dimension))
            return false;
        if (!value.token().number().is_integer())
            return false;
        if (!value.token().dimension_unit().equals_ignoring_ascii_case("n-"sv))
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
        if (!ident.starts_with_bytes("n-"sv, CaseSensitivity::CaseInsensitive))
            return false;
        for (size_t i = 2; i < ident.bytes_as_string_view().length(); ++i) {
            if (!is_ascii_digit(ident.bytes_as_string_view()[i]))
                return false;
        }
        return true;
    };
    auto is_dashndashdigit_ident = [](ComponentValue const& value) -> bool {
        if (!value.is(Token::Type::Ident))
            return false;
        auto ident = value.token().ident();
        if (!ident.starts_with_bytes("-n-"sv, CaseSensitivity::CaseInsensitive))
            return false;
        if (ident.bytes_as_string_view().length() == 3)
            return false;
        for (size_t i = 3; i < ident.bytes_as_string_view().length(); ++i) {
            if (!is_ascii_digit(ident.bytes_as_string_view()[i]))
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
        if (ident.equals_ignoring_ascii_case("odd"sv)) {
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { 2, 1 };
        }
        if (ident.equals_ignoring_ascii_case("even"sv)) {
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
                int b = third_value.token().to_integer() * (second_value.is_delim('+') ? 1 : -1);
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
        auto maybe_b = first_value.token().ident().bytes_as_string_view().substring_view(2).to_int();
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
                int b = third_value.token().to_integer() * (second_value.is_delim('+') ? 1 : -1);
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
    if (!first_value.is_delim('+')) {
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
                int b = third_value.token().to_integer() * (second_value.is_delim('+') ? 1 : -1);
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
        auto maybe_b = first_after_plus.token().ident().bytes_as_string_view().substring_view(1).to_int();
        if (maybe_b.has_value()) {
            transaction.commit();
            return Selector::SimpleSelector::ANPlusBPattern { 1, maybe_b.value() };
        }

        return syntax_error();
    }

    return syntax_error();
}

}
