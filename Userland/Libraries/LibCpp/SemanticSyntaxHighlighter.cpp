/*
 * Copyright (c) 2022, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SemanticSyntaxHighlighter.h"
#include "Lexer.h"
#include <LibDiff/Generator.h>
#include <LibGUI/AutocompleteProvider.h>
#include <LibGfx/Palette.h>

namespace Cpp {

void SemanticSyntaxHighlighter::rehighlight(Palette const& palette)
{
    Vector<CodeComprehension::TokenInfo> new_tokens_info;
    auto text = m_client->get_text();
    {
        Threading::MutexLocker locker(m_lock);
        Cpp::Lexer lexer(text);
        lexer.set_ignore_whitespace(true);
        auto current_tokens = lexer.lex();

        // Identify folding regions
        Vector<Token> folding_region_start_tokens;
        Vector<GUI::TextDocumentFoldingRegion> folding_regions;
        for (auto& token : current_tokens) {
            if (token.type() == Token::Type::LeftCurly) {
                folding_region_start_tokens.append(token);
            } else if (token.type() == Token::Type::RightCurly) {
                if (!folding_region_start_tokens.is_empty()) {
                    auto start_token = folding_region_start_tokens.take_last();
                    GUI::TextDocumentFoldingRegion folding_region;
                    folding_region.range.set_start({ start_token.end().line, start_token.end().column });
                    folding_region.range.set_end({ token.start().line, token.start().column });
                    folding_regions.append(move(folding_region));
                }
            }
        }
        m_client->do_set_folding_regions(move(folding_regions));

        StringBuilder current_tokens_as_lines;
        StringBuilder previous_tokens_as_lines;

        for (auto& token : current_tokens)
            current_tokens_as_lines.appendff("{}\n", token.type_as_byte_string());

        for (Cpp::Token const& token : m_saved_tokens)
            previous_tokens_as_lines.appendff("{}\n", token.type_as_byte_string());

        auto previous = previous_tokens_as_lines.to_byte_string();
        auto current = current_tokens_as_lines.to_byte_string();

        // FIXME: Computing the diff on the entire document's tokens is quite inefficient.
        //        An improvement over this could be only including the tokens that are in edited text ranges in the diff.
        auto diff_hunks = Diff::from_text(previous.view(), current.view()).release_value_but_fixme_should_propagate_errors();
        for (auto& token : current_tokens) {
            new_tokens_info.append(CodeComprehension::TokenInfo { CodeComprehension::TokenInfo::SemanticType::Unknown,
                token.start().line, token.start().column, token.end().line, token.end().column });
        }
        size_t previous_token_index = 0;
        size_t current_token_index = 0;
        for (auto const& hunk : diff_hunks) {
            for (; previous_token_index < hunk.location.old_range.start_line; ++previous_token_index) {
                new_tokens_info[current_token_index].type = m_tokens_info[previous_token_index].type;
                ++current_token_index;
            }
            for (size_t i = 0; i < hunk.location.new_range.number_of_lines; ++i) {
                ++current_token_index;
            }
            for (size_t i = 0; i < hunk.location.old_range.number_of_lines; ++i) {
                ++previous_token_index;
            }
        }
        while (current_token_index < new_tokens_info.size() && previous_token_index < m_tokens_info.size()) {
            new_tokens_info[current_token_index].type = m_tokens_info[previous_token_index].type;

            ++previous_token_index;
            ++current_token_index;
        }
    }

    update_spans(new_tokens_info, palette);
}

static Gfx::TextAttributes style_for_token_type(Gfx::Palette const& palette, CodeComprehension::TokenInfo::SemanticType type)
{
    switch (type) {
    case CodeComprehension::TokenInfo::SemanticType::Unknown:
        return { palette.base_text() };
    case CodeComprehension::TokenInfo::SemanticType::Keyword:
        return { palette.syntax_keyword(), {}, true };
    case CodeComprehension::TokenInfo::SemanticType::Type:
        return { palette.syntax_type(), {}, true };
    case CodeComprehension::TokenInfo::SemanticType::Identifier:
        return { palette.syntax_identifier() };
    case CodeComprehension::TokenInfo::SemanticType::String:
        return { palette.syntax_string() };
    case CodeComprehension::TokenInfo::SemanticType::Number:
        return { palette.syntax_number() };
    case CodeComprehension::TokenInfo::SemanticType::IncludePath:
        return { palette.syntax_preprocessor_value() };
    case CodeComprehension::TokenInfo::SemanticType::PreprocessorStatement:
        return { palette.syntax_preprocessor_statement() };
    case CodeComprehension::TokenInfo::SemanticType::Comment:
        return { palette.syntax_comment() };
    case CodeComprehension::TokenInfo::SemanticType::Function:
        return { palette.syntax_function() };
    case CodeComprehension::TokenInfo::SemanticType::Variable:
        return { palette.syntax_variable() };
    case CodeComprehension::TokenInfo::SemanticType::CustomType:
        return { palette.syntax_custom_type() };
    case CodeComprehension::TokenInfo::SemanticType::Namespace:
        return { palette.syntax_namespace() };
    case CodeComprehension::TokenInfo::SemanticType::Member:
        return { palette.syntax_member() };
    case CodeComprehension::TokenInfo::SemanticType::Parameter:
        return { palette.syntax_parameter() };
    case CodeComprehension::TokenInfo::SemanticType::PreprocessorMacro:
        return { palette.syntax_preprocessor_value() };
    default:
        VERIFY_NOT_REACHED();
        return { palette.base_text() };
    }
}
void SemanticSyntaxHighlighter::update_spans(Vector<CodeComprehension::TokenInfo> const& tokens_info, Gfx::Palette const& palette)
{
    Vector<GUI::TextDocumentSpan> spans;
    for (auto& token : tokens_info) {
        // FIXME: The +1 for the token end column is a quick hack due to not wanting to modify the lexer (which is also used by the parser). Maybe there's a better way to do this.
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.start_line, token.start_column });
        span.range.set_end({ token.end_line, token.end_column + 1 });
        span.attributes = style_for_token_type(palette, token.type);
        span.is_skippable = token.type == CodeComprehension::TokenInfo::SemanticType::Whitespace;
        span.data = static_cast<u64>(token.type);
        spans.append(span);
    }
    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

void SemanticSyntaxHighlighter::update_tokens_info(Vector<CodeComprehension::TokenInfo> tokens_info)
{
    {
        Threading::MutexLocker locker(m_lock);
        m_tokens_info = move(tokens_info);

        m_saved_tokens_text = m_client->get_text();
        Cpp::Lexer lexer(m_saved_tokens_text);
        lexer.set_ignore_whitespace(true);
        m_saved_tokens = lexer.lex();
    }
}

bool SemanticSyntaxHighlighter::is_identifier(u64 token_type) const
{
    auto type = static_cast<CodeComprehension::TokenInfo::SemanticType>(token_type);

    return type == CodeComprehension::TokenInfo::SemanticType::Identifier
        || type == CodeComprehension::TokenInfo::SemanticType::Function
        || type == CodeComprehension::TokenInfo::SemanticType::Variable
        || type == CodeComprehension::TokenInfo::SemanticType::CustomType
        || type == CodeComprehension::TokenInfo::SemanticType::Namespace
        || type == CodeComprehension::TokenInfo::SemanticType::Member
        || type == CodeComprehension::TokenInfo::SemanticType::Parameter
        || type == CodeComprehension::TokenInfo::SemanticType::PreprocessorMacro;
}

bool SemanticSyntaxHighlighter::is_navigatable(u64 token_type) const
{
    return static_cast<CodeComprehension::TokenInfo::SemanticType>(token_type) == CodeComprehension::TokenInfo::SemanticType::IncludePath;
}

}
