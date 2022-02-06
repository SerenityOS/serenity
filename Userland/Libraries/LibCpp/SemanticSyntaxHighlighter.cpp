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
    Vector<GUI::AutocompleteProvider::TokenInfo> new_tokens_info;
    auto text = m_client->get_text();
    {
        Threading::MutexLocker locker(m_lock);
        Cpp::Lexer lexer(text);
        lexer.set_ignore_whitespace(true);
        auto current_tokens = lexer.lex();

        StringBuilder current_tokens_as_lines;
        StringBuilder previous_tokens_as_lines;

        for (auto& token : current_tokens)
            current_tokens_as_lines.appendff("{}\n", token.type_as_string());

        for (Cpp::Token const& token : m_saved_tokens)
            previous_tokens_as_lines.appendff("{}\n", token.type_as_string());

        auto previous = previous_tokens_as_lines.build();
        auto current = current_tokens_as_lines.build();

        // FIXME: Computing the diff on the entire document's tokens is quite inefficient.
        //        An improvement over this could be only including the tokens that are in edited text ranges in the diff.
        auto diff_hunks = Diff::from_text(previous.view(), current.view());
        for (auto& token : current_tokens) {
            new_tokens_info.append(GUI::AutocompleteProvider::TokenInfo { GUI::AutocompleteProvider::TokenInfo::SemanticType::Unknown,
                token.start().line, token.start().column, token.end().line, token.end().column });
        }
        size_t previous_token_index = 0;
        size_t current_token_index = 0;
        for (auto const& hunk : diff_hunks) {
            for (; previous_token_index < hunk.original_start_line; ++previous_token_index) {
                new_tokens_info[current_token_index].type = m_tokens_info[previous_token_index].type;
                ++current_token_index;
            }
            for (size_t i = 0; i < hunk.added_lines.size(); ++i) {
                ++current_token_index;
            }
            for (size_t i = 0; i < hunk.removed_lines.size(); ++i) {
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

static Syntax::TextStyle style_for_token_type(Gfx::Palette const& palette, GUI::AutocompleteProvider::TokenInfo::SemanticType type)
{
    switch (type) {
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Unknown:
        return { palette.base_text(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Keyword:
        return { palette.syntax_keyword(), true };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Type:
        return { palette.syntax_type(), true };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Identifier:
        return { palette.syntax_identifier(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::String:
        return { palette.syntax_string(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Number:
        return { palette.syntax_number(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::IncludePath:
        return { palette.syntax_preprocessor_value(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::PreprocessorStatement:
        return { palette.syntax_preprocessor_statement(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Comment:
        return { palette.syntax_comment(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Function:
        return { palette.syntax_function(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Variable:
        return { palette.syntax_variable(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::CustomType:
        return { palette.syntax_custom_type(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Namespace:
        return { palette.syntax_namespace(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Member:
        return { palette.syntax_member(), false };
    case GUI::AutocompleteProvider::TokenInfo::SemanticType::Parameter:
        return { palette.syntax_parameter(), false };
    default:
        VERIFY_NOT_REACHED();
        return { palette.base_text(), false };
    }
}
void SemanticSyntaxHighlighter::update_spans(Vector<GUI::AutocompleteProvider::TokenInfo> const& tokens_info, Gfx::Palette const& pallete)
{
    Vector<GUI::TextDocumentSpan> spans;
    for (auto& token : tokens_info) {
        // FIXME: The +1 for the token end column is a quick hack due to not wanting to modify the lexer (which is also used by the parser). Maybe there's a better way to do this.
        GUI::TextDocumentSpan span;
        span.range.set_start({ token.start_line, token.start_column });
        span.range.set_end({ token.end_line, token.end_column + 1 });
        auto style = style_for_token_type(pallete, token.type);
        span.attributes.color = style.color;
        span.attributes.bold = style.bold;
        span.is_skippable = token.type == GUI::AutocompleteProvider::TokenInfo::SemanticType::Whitespace;
        span.data = static_cast<u64>(token.type);
        spans.append(span);
    }
    m_client->do_set_spans(move(spans));

    m_has_brace_buddies = false;
    highlight_matching_token_pair();

    m_client->do_update();
}

void SemanticSyntaxHighlighter::update_tokens_info(Vector<GUI::AutocompleteProvider::TokenInfo> tokens_info)
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
    using GUI::AutocompleteProvider;
    auto type = static_cast<AutocompleteProvider::TokenInfo::SemanticType>(token_type);

    return type == AutocompleteProvider::TokenInfo::SemanticType::Identifier
        || type == AutocompleteProvider::TokenInfo::SemanticType::Function
        || type == AutocompleteProvider::TokenInfo::SemanticType::Variable
        || type == AutocompleteProvider::TokenInfo::SemanticType::CustomType
        || type == AutocompleteProvider::TokenInfo::SemanticType::Namespace
        || type == AutocompleteProvider::TokenInfo::SemanticType::Member
        || type == AutocompleteProvider::TokenInfo::SemanticType::Parameter;
}

bool SemanticSyntaxHighlighter::is_navigatable(u64 token_type) const
{
    return static_cast<GUI::AutocompleteProvider::TokenInfo::SemanticType>(token_type) == GUI::AutocompleteProvider::TokenInfo::SemanticType::IncludePath;
}

}
