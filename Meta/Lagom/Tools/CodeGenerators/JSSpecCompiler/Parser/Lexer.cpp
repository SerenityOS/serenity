/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibXML/Parser/Parser.h>

#include "Parser/Lexer.h"
#include "Parser/SpecificationParsing.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

namespace {
Optional<Token> consume_number(LineTrackingLexer& lexer, Location& location)
{
    u64 start = lexer.tell();

    if (lexer.next_is('-'))
        lexer.consume(1);

    if (!lexer.next_is(is_ascii_digit)) {
        lexer.retreat(lexer.tell() - start);
        return {};
    }

    lexer.consume_while(is_ascii_digit);

    if (lexer.next_is('.')) {
        lexer.consume(1);
        if (lexer.consume_while(is_ascii_digit).length() == 0)
            lexer.retreat(1);
    }

    auto length = lexer.tell() - start;
    lexer.retreat(length);
    return { Token { TokenType::Number, lexer.consume(length), move(location) } };
}

bool can_end_word_token(char c)
{
    return is_ascii_space(c) || ".,"sv.contains(c);
}

void tokenize_string(SpecificationParsingContext& ctx, XML::Node const* node, StringView view, Vector<Token>& tokens)
{
    static constexpr struct {
        StringView text_to_match;
        TokenType token_type;
    } choices[] = {
        { "-"sv, TokenType::AmbiguousMinus },
        { "}"sv, TokenType::BraceClose },
        { "{"sv, TokenType::BraceOpen },
        { ":"sv, TokenType::Colon },
        { ","sv, TokenType::Comma },
        { "/"sv, TokenType::Division },
        { ". "sv, TokenType::Dot },
        { ".\n"sv, TokenType::Dot },
        { "="sv, TokenType::Equals },
        { "is equal to"sv, TokenType::Equals },
        { "!"sv, TokenType::ExclamationMark },
        { ">"sv, TokenType::Greater },
        { "is"sv, TokenType::Is },
        { "<"sv, TokenType::Less },
        { "»"sv, TokenType::ListEnd },
        { "«"sv, TokenType::ListStart },
        { "."sv, TokenType::MemberAccess },
        { "×"sv, TokenType::Multiplication },
        { "is not equal to"sv, TokenType::NotEquals },
        { "≠"sv, TokenType::NotEquals },
        { ")"sv, TokenType::ParenClose },
        { "("sv, TokenType::ParenOpen },
        { "+"sv, TokenType::Plus },
        { "?"sv, TokenType::QuestionMark },
        { "]"sv, TokenType::SquareBracketClose },
        { "["sv, TokenType::SquareBracketOpen },
        { "NewTarget"sv, TokenType::WellKnownValue },
    };

    LineTrackingLexer lexer(view, node->offset);

    while (!lexer.is_eof()) {
        lexer.ignore_while(is_ascii_space);

        // FIXME: This is incorrect since we count text offset after XML reference resolution. To do
        //        this properly, we need support from XML::Parser.
        Location token_location = ctx.location_from_xml_offset(lexer.position_for(lexer.tell()));

        if (auto result = consume_number(lexer, token_location); result.has_value()) {
            tokens.append(result.release_value());
            continue;
        }

        bool matched = false;
        for (auto const& [text_to_match, token_type] : choices) {
            if (lexer.consume_specific(text_to_match)) {
                tokens.append({ token_type, text_to_match, move(token_location) });
                matched = true;
                break;
            }
        }
        if (matched)
            continue;

        StringView word = lexer.consume_until(can_end_word_token);
        if (word.length())
            tokens.append({ TokenType::Word, word, move(token_location) });
    }
}

enum class TreeType {
    AlgorithmStep,
    NestedExpression,
    Header,
};

struct TokenizerState {
    Vector<Token> tokens;
    XML::Node const* substeps = nullptr;
    bool has_errors = false;
};

void tokenize_tree(SpecificationParsingContext& ctx, TokenizerState& state, XML::Node const* node, TreeType tree_type)
{
    // FIXME: Use structured binding once macOS Lagom CI updates to Clang >= 16.
    auto& tokens = state.tokens;
    auto& substeps = state.substeps;
    auto& has_errors = state.has_errors;

    for (auto const& child : node->as_element().children) {
        if (has_errors)
            break;

        child->content.visit(
            [&](XML::Node::Element const& element) -> void {
                Location child_location = ctx.location_from_xml_offset(child->offset);
                auto report_error = [&]<typename... Parameters>(AK::CheckedFormatString<Parameters...>&& fmt, Parameters const&... parameters) {
                    ctx.diag().error(child_location, move(fmt), parameters...);
                    has_errors = true;
                };

                if (substeps) {
                    report_error("substeps list must be the last child of algorithm step");
                    return;
                }

                if (element.name == tag_var) {
                    auto variable_name = get_text_contents(child);
                    if (!variable_name.has_value())
                        report_error("malformed <var> subtree, expected single text child node");

                    tokens.append({ TokenType::Identifier, variable_name.value_or(""sv), move(child_location) });
                    return;
                }

                if (element.name == tag_emu_val) {
                    auto maybe_contents = get_text_contents(child);
                    if (!maybe_contents.has_value())
                        report_error("malformed <emu-val> subtree, expected single text child node");

                    auto contents = maybe_contents.value_or(""sv);

                    if (contents.length() >= 2 && contents.starts_with('"') && contents.ends_with('"'))
                        tokens.append({ TokenType::String, contents.substring_view(1, contents.length() - 2), move(child_location) });
                    else if (contents.is_one_of("undefined", "null", "this", "true", "false"))
                        tokens.append({ TokenType::WellKnownValue, contents, move(child_location) });
                    else
                        tokens.append({ TokenType::Identifier, contents, move(child_location) });
                    return;
                }

                if (element.name == tag_emu_xref) {
                    auto identifier = get_single_child_with_tag(child, "a"sv).map([](XML::Node const* node) {
                        return get_text_contents(node).value_or(""sv);
                    });
                    if (!identifier.has_value() || identifier.value().is_empty())
                        report_error("malformed <emu-xref> subtree, expected <a> with nested single text node");

                    tokens.append({ TokenType::Identifier, identifier.value_or(""sv), move(child_location) });
                    return;
                }

                if (element.name == tag_sup) {
                    tokens.append({ TokenType::Superscript, ""sv, move(child_location) });
                    tokens.append({ TokenType::ParenOpen, ""sv, move(child_location) });
                    tokenize_tree(ctx, state, child, TreeType::NestedExpression);
                    tokens.append({ TokenType::ParenClose, ""sv, move(child_location) });
                    return;
                }

                if (element.name == tag_emu_const) {
                    auto maybe_contents = get_text_contents(child);
                    if (!maybe_contents.has_value())
                        report_error("malformed <emu-const> subtree, expected single text child node");

                    tokens.append({ TokenType::Enumerator, maybe_contents.value_or(""sv), move(child_location) });
                    return;
                }

                if (tree_type == TreeType::Header && element.name == tag_span) {
                    auto element_class = get_attribute_by_name(child, attribute_class);
                    if (element_class != class_secnum)
                        report_error("expected <span> to have class='secnum' attribute");

                    auto section_number = get_text_contents(child);
                    if (!section_number.has_value())
                        report_error("malformed section number span subtree, expected single text child node");

                    tokens.append({ TokenType::SectionNumber, section_number.value_or(""sv), move(child_location) });
                    return;
                }

                if (tree_type == TreeType::AlgorithmStep && element.name == tag_ol) {
                    substeps = child;
                    return;
                }

                report_error("<{}> should not be a child of algorithm step", element.name);
            },
            [&](XML::Node::Text const& text) {
                auto view = text.builder.string_view();
                if (substeps != nullptr && !contains_empty_text(child)) {
                    ctx.diag().error(ctx.location_from_xml_offset(child->offset),
                        "substeps list must be the last child of algorithm step");
                } else {
                    tokenize_string(ctx, child, view, tokens);
                }
            },
            [&](auto const&) {});
    }

    if (tree_type == TreeType::AlgorithmStep && tokens.size() && tokens.last().type == TokenType::MemberAccess)
        tokens.last().type = TokenType::Dot;
}
}

StepTokenizationResult tokenize_step(SpecificationParsingContext& ctx, XML::Node const* node)
{
    TokenizerState state;
    tokenize_tree(ctx, state, node, TreeType::AlgorithmStep);
    return {
        .tokens = state.has_errors ? OptionalNone {} : Optional<Vector<Token>> { move(state.tokens) },
        .substeps = state.substeps,
    };
}

Optional<Vector<Token>> tokenize_header(SpecificationParsingContext& ctx, XML::Node const* node)
{
    TokenizerState state;
    tokenize_tree(ctx, state, node, TreeType::Header);
    return state.has_errors ? OptionalNone {} : Optional<Vector<Token>> { state.tokens };
}

}
