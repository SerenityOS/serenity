/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AST/AST.h"
#include "Parser/ParseError.h"
#include "Parser/Token.h"

namespace JSSpecCompiler {

class TextParser {
public:
    struct DefinitionParseResult {
        StringView section_number;
        StringView function_name;
        Vector<StringView> arguments;
    };

    TextParser(Vector<Token>& tokens_, XML::Node const* node_)
        : m_tokens(tokens_)
        , m_node(node_)
    {
    }

    ParseErrorOr<DefinitionParseResult> parse_definition();
    ParseErrorOr<Tree> parse_step_without_substeps();
    ParseErrorOr<Tree> parse_step_with_substeps(Tree substeps);

private:
    struct IfConditionParseResult {
        bool is_if_branch;
        Optional<Tree> condition;
    };

    void retreat();
    [[nodiscard]] auto rollback_point();
    ParseErrorOr<Token const*> peek_token();
    ParseErrorOr<Token const*> consume_token();
    ParseErrorOr<Token const*> consume_token_with_one_of_types(std::initializer_list<TokenType> types);
    ParseErrorOr<Token const*> consume_token_with_type(TokenType type);
    ParseErrorOr<void> consume_word(StringView word);
    ParseErrorOr<void> consume_words(std::initializer_list<StringView> words);
    bool is_eof() const;
    ParseErrorOr<void> expect_eof() const;

    ParseErrorOr<Tree> parse_record_direct_list_initialization();
    ParseErrorOr<Tree> parse_expression();
    ParseErrorOr<Tree> parse_condition();
    ParseErrorOr<Tree> parse_return_statement();
    ParseErrorOr<Tree> parse_assert();
    ParseErrorOr<Tree> parse_assignment();
    ParseErrorOr<Tree> parse_simple_step_or_inline_if_branch();
    ParseErrorOr<IfConditionParseResult> parse_if_beginning();
    ParseErrorOr<Tree> parse_inline_if_else();
    ParseErrorOr<Tree> parse_if(Tree then_branch);
    ParseErrorOr<Tree> parse_else(Tree else_branch);

    Vector<Token> const& m_tokens;
    size_t m_next_token_index = 0;
    XML::Node const* m_node;
};

}
