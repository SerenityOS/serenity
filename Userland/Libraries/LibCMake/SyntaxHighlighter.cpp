/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SyntaxHighlighter.h"
#include <LibCMake/Lexer.h>
#include <LibCMake/Token.h>

namespace CMake {

static Gfx::TextAttributes style_for_token_type(Gfx::Palette const& palette, Token::Type type)
{
    switch (type) {
    case Token::Type::BracketComment:
    case Token::Type::LineComment:
        return { palette.syntax_comment() };
    case Token::Type::Identifier:
        return { palette.syntax_function() };
    case Token::Type::ControlKeyword:
        return { palette.syntax_control_keyword() };
    case Token::Type::OpenParen:
    case Token::Type::CloseParen:
        return { palette.syntax_punctuation() };
    case Token::Type::BracketArgument:
        return { palette.syntax_parameter() };
    case Token::Type::QuotedArgument:
        return { palette.syntax_string() };
    case Token::Type::UnquotedArgument:
        return { palette.syntax_parameter() };
    case Token::Type::Garbage:
        return { palette.red(), {}, false, Gfx::TextAttributes::UnderlineStyle::Wavy, palette.red() };
    case Token::Type::VariableReference:
        // This is a bit arbitrary, since we don't have a color specifically for this.
        return { palette.syntax_preprocessor_value() };
    default:
        return { palette.base_text() };
    }
}

bool SyntaxHighlighter::is_identifier(u64 token_type) const
{
    auto cmake_token = static_cast<Token::Type>(token_type);
    return cmake_token == Token::Type::Identifier;
}

void SyntaxHighlighter::rehighlight(Gfx::Palette const& palette)
{
    auto text = m_client->get_text();
    auto tokens = Lexer::lex(text).release_value_but_fixme_should_propagate_errors();
    auto& document = m_client->get_document();

    struct OpenBlock {
        Token token;
        int open_paren_count { 0 };
        Optional<Token> ending_paren {};
    };
    Vector<OpenBlock> open_blocks;
    Vector<Syntax::TextDocumentFoldingRegion> folding_regions;
    Vector<Syntax::TextDocumentSpan> spans;
    auto highlight_span = [&](Token::Type type, Position const& start, Position const& end) {
        Syntax::TextDocumentSpan span;
        span.range.set_start({ start.line, start.column });
        span.range.set_end({ end.line, end.column });
        if (!span.range.is_valid())
            return;

        span.attributes = style_for_token_type(palette, type);
        span.is_skippable = false;
        span.data = static_cast<u64>(type);
        spans.append(move(span));
    };

    auto create_region_from_block_type = [&](auto control_keywords, Token const& end_token) {
        if (open_blocks.is_empty())
            return;

        // Find the most recent open block with a matching keyword.
        Optional<size_t> found_index;
        OpenBlock open_block;
        for (int i = open_blocks.size() - 1; i >= 0; i--) {
            for (auto value : control_keywords) {
                if (open_blocks[i].token.control_keyword == value) {
                    found_index = i;
                    open_block = open_blocks[i];
                    break;
                }
            }
            if (found_index.has_value())
                break;
        }

        if (found_index.has_value()) {
            // Remove the found token and all after it.
            open_blocks.shrink(found_index.value());

            // Create a region.
            Syntax::TextDocumentFoldingRegion region;
            if (open_block.ending_paren.has_value()) {
                region.range.set_start({ open_block.ending_paren->end.line, open_block.ending_paren->end.column });
            } else {
                // The opening command is invalid, it does not have a closing paren.
                // So, we just start the region at the end of the line where the command identifier was. (eg, `if`)
                region.range.set_start({ open_block.token.end.line, document.line(open_block.token.end.line).last_non_whitespace_column().value() });
            }
            region.range.set_end({ end_token.start.line, end_token.start.column });
            folding_regions.append(move(region));
        }
    };

    for (auto const& token : tokens) {
        if (token.type == Token::Type::QuotedArgument || token.type == Token::Type::UnquotedArgument) {
            // Alternately highlight the regular/variable-reference parts.
            // 0-length ranges are caught in highlight_span() so we don't have to worry about them.
            Position previous_position = token.start;
            for (auto const& reference : token.variable_references) {
                highlight_span(token.type, previous_position, reference.start);
                highlight_span(Token::Type::VariableReference, reference.start, reference.end);
                previous_position = reference.end;
            }
            highlight_span(token.type, previous_position, token.end);
            continue;
        }

        highlight_span(token.type, token.start, token.end);

        if (!open_blocks.is_empty() && !open_blocks.last().ending_paren.has_value()) {
            auto& open_block = open_blocks.last();
            if (token.type == Token::Type::OpenParen) {
                open_block.open_paren_count++;
            } else if (token.type == Token::Type::CloseParen) {
                open_block.open_paren_count--;
                if (open_block.open_paren_count == 0)
                    open_block.ending_paren = token;
            }
        }

        // Create folding regions from control-keyword blocks.
        if (token.type == Token::Type::ControlKeyword) {
            switch (token.control_keyword.value()) {
            case ControlKeywordType::If:
                open_blocks.empend(token);
                break;
            case ControlKeywordType::ElseIf:
            case ControlKeywordType::Else:
                create_region_from_block_type(Array { ControlKeywordType::If, ControlKeywordType::ElseIf }, token);
                open_blocks.empend(token);
                break;
            case ControlKeywordType::EndIf:
                create_region_from_block_type(Array { ControlKeywordType::If, ControlKeywordType::ElseIf, ControlKeywordType::Else }, token);
                break;
            case ControlKeywordType::ForEach:
                open_blocks.empend(token);
                break;
            case ControlKeywordType::EndForEach:
                create_region_from_block_type(Array { ControlKeywordType::ForEach }, token);
                break;
            case ControlKeywordType::While:
                open_blocks.empend(token);
                break;
            case ControlKeywordType::EndWhile:
                create_region_from_block_type(Array { ControlKeywordType::While }, token);
                break;
            case ControlKeywordType::Macro:
                open_blocks.empend(token);
                break;
            case ControlKeywordType::EndMacro:
                create_region_from_block_type(Array { ControlKeywordType::Macro }, token);
                break;
            case ControlKeywordType::Function:
                open_blocks.empend(token);
                break;
            case ControlKeywordType::EndFunction:
                create_region_from_block_type(Array { ControlKeywordType::Function }, token);
                break;
            case ControlKeywordType::Block:
                open_blocks.empend(token);
                break;
            case ControlKeywordType::EndBlock:
                create_region_from_block_type(Array { ControlKeywordType::Block }, token);
                break;
            default:
                break;
            }
        }
    }
    m_client->do_set_spans(move(spans));
    m_client->do_set_folding_regions(move(folding_regions));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

Vector<SyntaxHighlighter::MatchingTokenPair> SyntaxHighlighter::matching_token_pairs_impl() const
{
    static Vector<MatchingTokenPair> pairs;
    if (pairs.is_empty()) {
        pairs.append({ static_cast<u64>(Token::Type::OpenParen), static_cast<u64>(Token::Type::CloseParen) });
    }
    return pairs;
}

bool SyntaxHighlighter::token_types_equal(u64 token1, u64 token2) const
{
    return static_cast<Token::Type>(token1) == static_cast<Token::Type>(token2);
}

}
