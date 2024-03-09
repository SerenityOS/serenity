/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AST/AST.h"
#include "Function.h"
#include "Parser/Token.h"

namespace JSSpecCompiler {

struct ClauseHeader {
    enum class ObjectType {
        Constructor,
        Prototype,
        Instance,
    };

    struct PropertiesList {
        QualifiedName name;
        ObjectType object_type;
    };

    StringView section_number;
    Variant<AK::Empty, AbstractOperationDeclaration, AccessorDeclaration, MethodDeclaration, PropertiesList> header;
};

struct TextParseError { };

struct FailedTextParseDiagnostic {
    Location location;
    String message;
};

template<typename T>
using TextParseErrorOr = ErrorOr<T, TextParseError>;

class TextParser {
public:
    enum class ClauseHasAoidAttribute {
        No,
        Yes,
    };

    TextParser(SpecificationParsingContext& ctx, Vector<Token> const& tokens, XML::Node const* node)
        : m_ctx(ctx)
        , m_tokens(tokens)
        , m_node(node)
    {
    }

    TextParseErrorOr<ClauseHeader> parse_clause_header(ClauseHasAoidAttribute clause_has_aoid_attribute);
    TextParseErrorOr<NullableTree> parse_step_without_substeps();
    TextParseErrorOr<Tree> parse_step_with_substeps(Tree substeps);

    FailedTextParseDiagnostic get_diagnostic() const;

private:
    struct IfConditionParseResult {
        bool is_if_branch;
        NullableTree condition;
    };

    struct CustomMessage {
        StringView message;
    };

    void save_error(Variant<TokenType, StringView, CustomMessage>&& expected);

    void retreat();
    [[nodiscard]] auto rollback_point();
    Optional<Token> peek_token();
    Optional<Token> consume_token();
    TextParseErrorOr<Token> consume_token_with_one_of_types(std::initializer_list<TokenType> types);
    TextParseErrorOr<Token> consume_token_with_type(TokenType type);
    TextParseErrorOr<void> consume_token(TokenType type, StringView data);
    TextParseErrorOr<void> consume_word(StringView word);
    TextParseErrorOr<void> consume_words(std::initializer_list<StringView> words);
    bool is_eof() const;
    TextParseErrorOr<void> expect_eof();

    TextParseErrorOr<Tree> parse_record_direct_list_initialization();
    TextParseErrorOr<Vector<Tree>> parse_function_arguments();
    TextParseErrorOr<Tree> parse_list_initialization();
    TextParseErrorOr<Tree> parse_the_this_value();
    TextParseErrorOr<Tree> parse_value();
    TextParseErrorOr<Tree> parse_expression();
    TextParseErrorOr<Tree> parse_condition();
    TextParseErrorOr<Tree> parse_return_statement();
    TextParseErrorOr<Tree> parse_assert();
    TextParseErrorOr<Tree> parse_assignment();
    TextParseErrorOr<Tree> parse_perform();
    TextParseErrorOr<Tree> parse_simple_step_or_inline_if_branch();
    TextParseErrorOr<IfConditionParseResult> parse_if_beginning();
    TextParseErrorOr<Tree> parse_inline_if_else();
    TextParseErrorOr<Tree> parse_if(Tree then_branch);
    TextParseErrorOr<Tree> parse_else(Tree else_branch);

    TextParseErrorOr<QualifiedName> parse_qualified_name();
    TextParseErrorOr<Vector<FunctionArgument>> parse_function_arguments_in_declaration();
    TextParseErrorOr<AbstractOperationDeclaration> parse_abstract_operation_declaration();
    TextParseErrorOr<MethodDeclaration> parse_method_declaration();
    TextParseErrorOr<AccessorDeclaration> parse_accessor_declaration();
    TextParseErrorOr<ClauseHeader::PropertiesList> parse_properties_list_declaration();

    SpecificationParsingContext& m_ctx;
    Vector<Token> const& m_tokens;
    size_t m_next_token_index = 0;
    XML::Node const* m_node;

    size_t m_max_parsed_tokens = 0;
    Vector<Variant<TokenType, StringView, CustomMessage>, 8> m_suitable_continuations;
};

}
