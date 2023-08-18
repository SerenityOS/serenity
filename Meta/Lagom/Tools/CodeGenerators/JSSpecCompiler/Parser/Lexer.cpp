/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <AK/NonnullOwnPtr.h>

#include "Parser/Lexer.h"
#include "Parser/XMLUtils.h"

namespace JSSpecCompiler {

namespace {
Optional<Token> consume_number(GenericLexer& lexer, XML::Node const* node)
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
    return { Token { TokenType::Number, lexer.consume(length), node } };
}

bool can_end_word_token(char c)
{
    return is_ascii_space(c) || ".,"sv.contains(c);
}
}

ParseErrorOr<void> tokenize_string(XML::Node const* node, StringView view, Vector<Token>& tokens)
{
#define CONSUME_IF_NEXT(view, type)                                      \
    if (lexer.next_is(view##sv)) {                                       \
        size_t length = __builtin_strlen(view);                          \
        tokens.append({ TokenType::type, lexer.consume(length), node }); \
        continue;                                                        \
    }

    GenericLexer lexer(view);
    while (!lexer.is_eof()) {
        lexer.ignore_while(is_ascii_space);

        if (auto result = consume_number(lexer, node); result.has_value()) {
            tokens.append(result.release_value());
            continue;
        }

        CONSUME_IF_NEXT("(", ParenOpen);
        CONSUME_IF_NEXT(")", ParenClose);
        CONSUME_IF_NEXT("{", BraceOpen);
        CONSUME_IF_NEXT("}", BraceClose);
        CONSUME_IF_NEXT(",", Comma);
        CONSUME_IF_NEXT(". ", Dot);
        CONSUME_IF_NEXT(".\n", Dot);
        CONSUME_IF_NEXT(":", Colon);
        CONSUME_IF_NEXT(".", MemberAccess);
        CONSUME_IF_NEXT("<", Less);
        CONSUME_IF_NEXT(">", Greater);
        CONSUME_IF_NEXT("is not equal to", NotEquals);
        CONSUME_IF_NEXT("≠", NotEquals);
        CONSUME_IF_NEXT("is equal to", Equals);
        CONSUME_IF_NEXT("=", Equals);
        CONSUME_IF_NEXT("+", Plus);
        CONSUME_IF_NEXT("-", AmbiguousMinus);
        CONSUME_IF_NEXT("×", Multiplication);
        CONSUME_IF_NEXT("/", Division);
        CONSUME_IF_NEXT("!", ExclamationMark);
        CONSUME_IF_NEXT("is", Is);

        StringView word = lexer.consume_until(can_end_word_token);
        if (word.length())
            tokens.append({ TokenType::Word, word, node });
    }
    return {};

#undef CONSUME_IF_NEXT
}

ParseErrorOr<TokenizeTreeResult> tokenize_tree(XML::Node const* node, bool allow_substeps)
{
    TokenizeTreeResult result;
    auto& tokens = result.tokens;

    for (auto const& child : node->as_element().children) {
        TRY(child->content.visit(
            [&](XML::Node::Element const& element) -> ParseErrorOr<void> {
                if (result.substeps != nullptr)
                    return ParseError::create("Substeps list must be the last non-empty child"sv, child);

                if (element.name == tag_var) {
                    tokens.append({ TokenType::Identifier, TRY(get_text_contents(child)), child });
                    return {};
                }

                if (element.name == tag_span) {
                    auto element_class = TRY(get_attribute_by_name(child, attribute_class));
                    if (element_class != class_secnum)
                        return ParseError::create(String::formatted("Expected 'secnum' as a class name of <span>, but found '{}'", element_class), child);
                    tokens.append({ TokenType::SectionNumber, TRY(get_text_contents(child)), child });
                    return {};
                }

                if (element.name == tag_emu_val) {
                    auto contents = TRY(get_text_contents(child));
                    if (contents.length() >= 2 && contents.starts_with('"') && contents.ends_with('"'))
                        tokens.append({ TokenType::String, contents.substring_view(1, contents.length() - 2), child });
                    else if (contents == "undefined")
                        tokens.append({ TokenType::Undefined, contents, child });
                    else
                        tokens.append({ TokenType::Identifier, contents, child });
                    return {};
                }

                if (element.name == tag_emu_xref) {
                    auto contents = TRY(get_text_contents(TRY(get_only_child(child, "a"sv))));
                    tokens.append({ TokenType::Identifier, contents, child });
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
                return tokenize_string(child, view, tokens);
            },
            move(ignore_comments)));
    }
    return result;
}

}
