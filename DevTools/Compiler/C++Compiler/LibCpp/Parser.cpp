/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
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

#include "Parser.h"
#include "AST.h"
#include "Option.h"
#include <LibCore/File.h>

#define DEBUG_LOG_SPAM
#include <AK/ScopeLogger.h>

#if defined DEBUG_LOG_SPAM && !defined DEBUG_CXX_PARSER
#    define DEBUG_CXX_PARSER
#endif

namespace Cpp {

Token Parser::get_next_token_skip_comment_and_whitespaces()
{
    if (m_saved_token.has_value())
        return m_saved_token.release_value();

    Token tok = m_lexer.lex_one_token();

    while (tok.m_type == Token::Type::Comment || tok.m_type == Token::Type::Whitespace)
        tok = m_lexer.lex_one_token();
#ifdef DEBUG_CXX_PARSER
    dbgln("got token: {}", tok.to_string());
#endif
    return tok;
}

Token Parser::peek()
{
    if (!m_saved_token.has_value())
        m_saved_token = get_next_token_skip_comment_and_whitespaces();
    return m_saved_token.value();
}

void Parser::consume()
{
    assert(m_saved_token.has_value());
    m_saved_token.clear();
}

void Parser::expect(Token::Type expected_type)
{
    (void)consume(expected_type);
}

Token Parser::consume(Token::Type expected_type)
{
    auto tok = peek();

    if (tok.m_type != expected_type) {
        Token expected_tok;
        expected_tok.m_type = expected_type;
        fprintf(stderr, "expected %s: got %s\n", expected_tok.to_string(), tok.to_string());
        exit(-1);
    }
    consume();
    return tok;
}

void Parser::parse_error(StringView message)
{
    fprintf(stderr, "%s\n", message.to_string().characters());
    exit(-1);
}

// unqualified-id:
//      - identifier
Optional<String> Parser::parse_unqualified_id()
{
    SCOPE_LOGGER();
    const Token identifier = peek();

    if (identifier.m_type == Token::Type::Identifier) {
        consume();
        return identifier.m_identifier;
    }

    return {};
}

// id-expression:
//      - unqualified-id
Optional<String> Parser::parse_id_expression()
{
    SCOPE_LOGGER();
    return parse_unqualified_id();
}

// declarator-id:
//      - id-expression
Optional<String> Parser::parse_declarator_id()
{
    SCOPE_LOGGER();
    return parse_id_expression();
}

// noptr-declarator:
//      - declarator-id
Optional<String> Parser::parse_noptr_declarator()
{
    SCOPE_LOGGER();
    return parse_declarator_id();
}

// simple-type-specifier:
//      - void
//      - int
void Parser::parse_simple_type_specifier(TypeSpecifier& type_specifier)
{
    SCOPE_LOGGER();
    const auto type = consume(Token::Type::KnownType);

    switch (type.m_known_type) {
    case Token::KnownType::Void:
        type_specifier.is_void = true;
        break;
    case Token::KnownType::Int:
        type_specifier.is_int = true;
        break;
    }
}

// type-specifier:
//      - simple-type-specifier
void Parser::parse_type_specifier(TypeSpecifier& type_specifier)
{
    SCOPE_LOGGER();
    parse_simple_type_specifier(type_specifier);
}

// defining-type-specifier:
//      - type-specifier
Parser::TypeSpecifier Parser::parse_defining_type_specifier()
{
    SCOPE_LOGGER();
    TypeSpecifier type_specifier;
    parse_type_specifier(type_specifier);
    return type_specifier;
}

// decl-specifier:
//      - defining-type-specifier
Parser::TypeSpecifier Parser::parse_decl_specifier()
{
    SCOPE_LOGGER();
    return parse_defining_type_specifier();
}

// decl-specifier-seq:
//      - decl-specifier
NonnullRefPtr<Type> Parser::parse_decl_specifier_seq()
{
    SCOPE_LOGGER();
    auto type = parse_decl_specifier();

    if (type.is_void)
        return create_ast_node<VoidType>();
    else if (type.is_int)
        return create_ast_node<SignedIntType>();
    else
        TODO();
}

// parameter-declaration:
//      - decl-specifier-seq declarator
//      - decl-specifier-seq [abstract-declarator]
NonnullRefPtr<Variable> Parser::parse_parameter_declaration()
{
    SCOPE_LOGGER();
    auto specifiers = parse_decl_specifier_seq();
    auto declarator = parse_declarator();
    String name = declarator.value_or({}).name;

    return create_ast_node<Variable>(specifiers, name);
}

// parameter-declaration-list:
//      - parameter-declaration
//      - parameter-declaration-list, parameter-declaration
NonnullRefPtrVector<Variable> Parser::parse_parameter_declaration_list()
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<Variable> params;
    params.append(parse_parameter_declaration());
    while (peek().m_type == Token::Type::Comma) {
        consume();
        params.append(parse_parameter_declaration());
    }
    return params;
}

// parameter-declaration-clause:
//      - parameter-declaration-list
NonnullRefPtrVector<Variable> Parser::parse_parameter_declaration_clause()
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<Variable> params;

    params.append(parse_parameter_declaration_list());
    return params;
}

// parameters-and-qualifiers:
//      - ( parameter-declaration-clause )
NonnullRefPtrVector<Variable> Parser::parse_parameters_and_qualifiers()
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<Variable> params;
    expect(Token::Type::LeftParen);
    if (peek().m_type != Token::Type::RightParen)
        params = parse_parameter_declaration_clause();
    expect(Token::Type::RightParen);
    return params;
}

// declarator:
//      - noptr-declarator parameters-and-qualifiers
Optional<Parser::Declarator> Parser::parse_declarator()
{
    SCOPE_LOGGER();
    auto name = parse_noptr_declarator();
    if (name.has_value()) {
        if (peek().m_type != Token::Type::LeftParen)
            return { { name.value(), {} } };
        else
            return { { name.value(), parse_parameters_and_qualifiers() } };
    }
    return {};
}

// compound-statement:
//      - { [statement-seq] }
NonnullRefPtrVector<ASTNode> Parser::parse_compound_statement()
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<ASTNode> body;
    expect(Token::Type::LeftCurly);
    expect(Token::Type::RightCurly);
    return body;
}

// function-body:
//      - compound-statement
NonnullRefPtrVector<ASTNode> Parser::parse_function_body()
{
    SCOPE_LOGGER();
    return parse_compound_statement();
}

// function-definition:
//      - decl-specifier-seq declarator function-body
NonnullRefPtr<Function> Parser::parse_function_definition()
{
    SCOPE_LOGGER();
    auto return_type = parse_decl_specifier_seq();
    auto declarator = parse_declarator();

    if (declarator.has_value()) {
        parse_function_body();
        return create_ast_node<Function>(return_type, declarator.value().name, declarator.value().parameters);
    }
    parse_error("expected identifier");
}

// declaration:
//      - function-definition
NonnullRefPtr<Function> Parser::parse_declaration()
{
    SCOPE_LOGGER();
    return parse_function_definition();
}

// declaration-seq:
//      - declaration+
NonnullRefPtrVector<Function> Parser::parse_declaration_sequence()
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<Function> functions;
    while (true) {
        const auto tok = peek();
        if (tok.m_type != Token::Type::EndOfFile) {
            auto function = parse_declaration();
            functions.append(function);
        } else {
            return functions;
        }
    }
}

// translation-unit:
//      - [declaration-seq]
Cpp::TranslationUnit Parser::parse_translation_unit()
{
    SCOPE_LOGGER();

    const auto tok = peek();
    if (tok.m_type != Token::Type::EndOfFile) {
        auto functions = parse_declaration_sequence();
        m_tu.functions().append(functions);
    }

    return m_tu;
}

TranslationUnit Parser::parse(const Cpp::Option& options)
{
    Parser parser(options.input_file);

    return parser.parse_translation_unit();
}

ByteBuffer Parser::get_input_file_content(const String& filename)
{
    //TODO: maybe give the filename to the lexer.
    auto file = Core::File::open(filename, Core::IODevice::ReadOnly);
    assert(!file.is_error());
    return file.value()->read_all();
}

Parser::Parser(const String& filename)
    : m_file_content(get_input_file_content(filename))
    , m_lexer(m_file_content)
{
}

}