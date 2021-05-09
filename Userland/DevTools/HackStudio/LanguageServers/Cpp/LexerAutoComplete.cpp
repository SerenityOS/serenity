/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LexerAutoComplete.h"
#include <AK/Debug.h>
#include <AK/HashTable.h>
#include <LibCpp/Lexer.h>

namespace LanguageServers::Cpp {

LexerAutoComplete::LexerAutoComplete(ClientConnection& connection, const FileDB& filedb)
    : AutoCompleteEngine(connection, filedb)
{
}

Vector<GUI::AutocompleteProvider::Entry> LexerAutoComplete::get_suggestions(const String& file, const GUI::TextPosition& autocomplete_position)
{
    auto document = filedb().get(file);
    if (!document) {
        dbgln("didn't find document for {}", file);
        return {};
    }
    auto code = document->text();
    auto lines = code.split('\n', true);
    Cpp::Lexer lexer(code);
    auto tokens = lexer.lex();

    auto index_of_target_token = token_in_position(tokens, autocomplete_position);
    if (!index_of_target_token.has_value())
        return {};

    auto suggestions = identifier_prefixes(lines, tokens, index_of_target_token.value());

    if constexpr (AUTOCOMPLETE_DEBUG) {
        for (auto& suggestion : suggestions) {
            dbgln("suggestion: {}", suggestion.completion);
        }
    }

    return suggestions;
}

StringView LexerAutoComplete::text_of_token(const Vector<String>& lines, const Cpp::Token& token)
{
    VERIFY(token.start().line == token.end().line);
    VERIFY(token.start().column <= token.end().column);
    return lines[token.start().line].substring_view(token.start().column, token.end().column - token.start().column + 1);
}

Optional<size_t> LexerAutoComplete::token_in_position(const Vector<Cpp::Token>& tokens, const GUI::TextPosition& position)
{
    for (size_t token_index = 0; token_index < tokens.size(); ++token_index) {
        auto& token = tokens[token_index];
        if (token.start().line != token.end().line)
            continue;
        if (token.start().line != position.line())
            continue;
        if (token.start().column + 1 > position.column() || token.end().column + 1 < position.column())
            continue;
        return token_index;
    }
    return {};
}

Vector<GUI::AutocompleteProvider::Entry> LexerAutoComplete::identifier_prefixes(const Vector<String>& lines, const Vector<Cpp::Token>& tokens, size_t target_token_index)
{
    auto partial_input = text_of_token(lines, tokens[target_token_index]);
    Vector<GUI::AutocompleteProvider::Entry> suggestions;

    HashTable<String> suggestions_lookup; // To avoid duplicate results

    for (size_t i = 0; i < target_token_index; ++i) {
        auto& token = tokens[i];
        if (token.type() != Cpp::Token::Type::Identifier)
            continue;
        auto text = text_of_token(lines, token);
        if (text.starts_with(partial_input) && suggestions_lookup.set(text) == AK::HashSetResult::InsertedNewEntry) {
            suggestions.append({ text, partial_input.length(), GUI::AutocompleteProvider::CompletionKind::Identifier });
        }
    }
    return suggestions;
}

}
