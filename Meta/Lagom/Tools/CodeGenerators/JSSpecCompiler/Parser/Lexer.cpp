/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <LibXML/Parser/Parser.h>

#include "Parser/Lexer.h"
#include "Parser/SpecParser.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

namespace {
Optional<Token> consume_number(XML::LineTrackingLexer& lexer, XML::Node const* node, Location& location)
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
    return { Token { TokenType::Number, lexer.consume(length), node, move(location) } };
}

bool can_end_word_token(char c)
{
    return is_ascii_space(c) || ".,"sv.contains(c);
}
}

ParseErrorOr<void> tokenize_string(SpecificationParsingContext& ctx, XML::Node const* node, StringView view, Vector<Token>& tokens)
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
        { "."sv, TokenType::MemberAccess },
        { "×"sv, TokenType::Multiplication },
        { "is not equal to"sv, TokenType::NotEquals },
        { "≠"sv, TokenType::NotEquals },
        { ")"sv, TokenType::ParenClose },
        { "("sv, TokenType::ParenOpen },
        { "+"sv, TokenType::Plus },
    };

    XML::LineTrackingLexer lexer(view, node->offset);

    while (!lexer.is_eof()) {
        lexer.ignore_while(is_ascii_space);

        // FIXME: This is incorrect since we count text offset after XML reference resolution. To do
        //        this properly, we need support from XML::Parser.
        Location token_location = ctx.location_from_xml_offset(lexer.offset_for(lexer.tell()));

        if (auto result = consume_number(lexer, node, token_location); result.has_value()) {
            tokens.append(result.release_value());
            continue;
        }

        bool matched = false;
        for (auto const& [text_to_match, token_type] : choices) {
            if (lexer.consume_specific(text_to_match)) {
                tokens.append({ token_type, ""sv, node, move(token_location) });
                matched = true;
                break;
            }
        }
        if (matched)
            continue;

        StringView word = lexer.consume_until(can_end_word_token);
        if (word.length())
            tokens.append({ TokenType::Word, word, node, move(token_location) });
    }
    return {};
}

ParseErrorOr<TokenizeTreeResult> tokenize_tree(SpecificationParsingContext& ctx, XML::Node const* node, bool allow_substeps)
{
    TokenizeTreeResult result;
    auto& tokens = result.tokens;

    for (auto const& child : node->as_element().children) {
        TRY(child->content.visit(
            [&](XML::Node::Element const& element) -> ParseErrorOr<void> {
                if (result.substeps != nullptr)
                    return ParseError::create("Substeps list must be the last non-empty child"sv, child);

                Location child_location = ctx.location_from_xml_offset(child->offset);

                if (element.name == tag_var) {
                    tokens.append({ TokenType::Identifier, TRY(get_text_contents(child)), child, move(child_location) });
                    return {};
                }

                if (element.name == tag_span) {
                    auto element_class = TRY(deprecated_get_attribute_by_name(child, attribute_class));
                    if (element_class != class_secnum)
                        return ParseError::create(String::formatted("Expected 'secnum' as a class name of <span>, but found '{}'", element_class), child);
                    tokens.append({ TokenType::SectionNumber, TRY(get_text_contents(child)), child, move(child_location) });
                    return {};
                }

                if (element.name == tag_emu_val) {
                    auto contents = TRY(get_text_contents(child));
                    if (contents.length() >= 2 && contents.starts_with('"') && contents.ends_with('"'))
                        tokens.append({ TokenType::String, contents.substring_view(1, contents.length() - 2), child, move(child_location) });
                    else if (contents == "undefined")
                        tokens.append({ TokenType::Undefined, contents, child, move(child_location) });
                    else
                        tokens.append({ TokenType::Identifier, contents, child, move(child_location) });
                    return {};
                }

                if (element.name == tag_emu_xref) {
                    auto contents = TRY(get_text_contents(TRY(get_only_child(child, "a"sv))));
                    tokens.append({ TokenType::Identifier, contents, child, move(child_location) });
                    return {};
                }

                if (element.name == tag_ol) {
                    if (!allow_substeps)
                        return ParseError::create("Found nested list but substeps are not allowed"sv, child);
                    result.substeps = child;
                    return {};
                }

                return ParseError::create(String::formatted("Unexpected child element with tag {}", element.name), child);
            },
            [&](XML::Node::Text const& text) -> ParseErrorOr<void> {
                auto view = text.builder.string_view();
                if (result.substeps && !contains_empty_text(child))
                    return ParseError::create("Substeps list must be the last non-empty child"sv, child);
                return tokenize_string(ctx, child, view, tokens);
            },
            move(ignore_comments)));
    }

    if (tokens.size() && tokens.last().type == TokenType::MemberAccess)
        tokens.last().type = TokenType::Dot;

    return result;
}

}
