/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "AutoComplete.h"
#include <AK/HashTable.h>
#include <LibCpp/Lexer.h>

// #define DEBUG_AUTOCOMPLETE

namespace LanguageServers::Cpp {

Vector<AutoCompleteResponse> AutoComplete::get_suggestions(const String& code, GUI::TextPosition autocomplete_position)
{
    auto lines = code.split('\n', true);
    Lexer lexer(code);
    auto tokens = lexer.lex();

    auto index_of_target_token = token_in_position(tokens, autocomplete_position);
    if (!index_of_target_token.has_value())
        return {};

    auto suggestions = identifier_prefixes(lines, tokens, index_of_target_token.value());

#ifdef DEBUG_AUTOCOMPLETE
    for (auto& suggestion : suggestions) {
        dbgln("suggestion: {}", suggestion.completion);
    }
#endif

    return suggestions;
}

String AutoComplete::text_of_token(const Vector<String> lines, const Cpp::Token& token)
{
    return lines[token.m_start.line].substring(token.m_start.column, token.m_end.column - token.m_start.column + 1);
}

Optional<size_t> AutoComplete::token_in_position(const Vector<Cpp::Token>& tokens, GUI::TextPosition position)
{
    for (size_t token_index = 0; token_index < tokens.size(); ++token_index) {
        auto& token = tokens[token_index];
        if (token.m_start.line != position.line())
            continue;
        if (token.m_start.column > position.column() || token.m_end.column < position.column())
            continue;
        return token_index;
    }
    return {};
}

Vector<AutoCompleteResponse> AutoComplete::identifier_prefixes(const Vector<String> lines, const Vector<Cpp::Token>& tokens, size_t target_token_index)
{
    auto partial_input = text_of_token(lines, tokens[target_token_index]);
    Vector<AutoCompleteResponse> suggestions;

    HashTable<String> suggestions_lookup; // To avoid duplicate results

    for (size_t i = 0; i < target_token_index; ++i) {
        auto& token = tokens[i];
        if (token.m_type != Cpp::Token::Type::Identifier)
            continue;
        auto text = text_of_token(lines, token);
        if (text.starts_with(partial_input) && !suggestions_lookup.contains(text)) {
            suggestions_lookup.set(text);
            suggestions.append({ text, partial_input.length(), HackStudio::CompletionKind::Identifier });
        }
    }
    return suggestions;
}

}
