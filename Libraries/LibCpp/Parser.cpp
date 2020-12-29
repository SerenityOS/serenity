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

#define DEBUG_SPAM
#include <AK/ScopeLogger.h>

#if defined DEBUG_SPAM && !defined DEBUG_CPP_PARSER
#    define DEBUG_CPP_PARSER
#endif

namespace Cpp {

// all the parse_* methods and their comments are based on https://isocpp.org/files/papers/N4860.pdf

Token Parser::get_next_token_skip_comment_and_whitespaces()
{
    if (m_saved_token.has_value())
        return m_saved_token.release_value();

    Token tok = m_lexer.lex_one_token();

    while (tok.m_type == Token::Type::Comment || tok.m_type == Token::Type::Whitespace)
        tok = m_lexer.lex_one_token();
#ifdef DEBUG_CPP_PARSER
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

NonnullRefPtr<Variable> Parser::create_unnamed_var(NonnullRefPtr<Type>& type)
{
    StringBuilder sb;

    sb.appendf("_.%zu", m_unnamed_var_counter++);
    auto name = sb.build();
    return create_ast_node<Variable>(type, name);
}

/*
 * When parsing two operator with the same precedence, it will produce a tree with the branches inverted.
 * `i + j - k` will have a tree like `i + (j -k)`. This method transforms that tree into `(i + j) -k`.
 */
template<typename... Args>
BinaryExpression& Parser::maybe_correct_binop_tree(BinaryExpression& result, Expression& right, Args... args)
{
    if (right.is_binary_expression()) {
        auto& bi_right = reinterpret_cast<BinaryExpression&>(right);

        auto any_operator_match = [](auto const& expected, auto const&... rest) {
            return ((expected == rest) || ...);
        }(bi_right.binary_operation(), args...);

        if (any_operator_match) {
            auto right_left_node = bi_right.left();
            result.set_right(right_left_node);
            bi_right.set_left(result);
            return bi_right;
        }
    }
    return result;
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

// primary-expression:
//      - id-expression
NonnullRefPtr<Expression> Parser::parse_primary_expression()
{
    SCOPE_LOGGER();
    auto id = parse_id_expression();
    if (id.has_value()) {
        //TODO: should disapear when result will be bound to a variable
        auto return_type = create_ast_node<Variable>(create_ast_node<SignedIntType>(), id.value());
        return create_ast_node<IdentifierExpression>(move(return_type));
    } else {
        parse_error("expected identifier");
    }
}

// postfix-expression
//      - primary-expression
NonnullRefPtr<Expression> Parser::parse_postfix_expression()
{
    SCOPE_LOGGER();
    return parse_primary_expression();
}

// unary-expression:
//      - postfix-expression
NonnullRefPtr<Expression> Parser::parse_unary_expression()
{
    SCOPE_LOGGER();
    return parse_postfix_expression();
}

// cast-expression:
//      - unary-expression
NonnullRefPtr<Expression> Parser::parse_cast_expression()
{
    SCOPE_LOGGER();
    return parse_unary_expression();
}

// pm-expression:
//      - cast-expression
NonnullRefPtr<Expression> Parser::parse_pm_expression()
{
    SCOPE_LOGGER();
    return parse_cast_expression();
}

// multiplicative-expression:
//      - pm-expression
//      - multiplicative-expression * pm-expression
NonnullRefPtr<Expression> Parser::parse_multiplicative_expression()
{
    SCOPE_LOGGER();
    auto left = parse_pm_expression();
    if (peek().m_type == Token::Type::Asterisk) {
        consume();
        auto right = parse_multiplicative_expression();
        auto result_var = create_unnamed_var(left->result()->node_type());
        assert(left->result()->node_type()->kind() == right->result()->node_type()->kind());

        auto result = create_ast_node<BinaryExpression>(BinaryExpression::Kind::Multiplication, move(left), move(right), move(result_var));
        return maybe_correct_binop_tree(result, right, BinaryExpression::Kind::Multiplication);
    }
    return left;
}

// additive-expression:
//      - multiplicative-expression
//      - additive-expression + multiplicative-expression
//      - additive-expression - multiplicative-expression
NonnullRefPtr<Expression> Parser::parse_additive_expression()
{
    SCOPE_LOGGER();
    auto left = parse_multiplicative_expression();
    if (peek().m_type == Token::Type::Plus || peek().m_type == Token::Type::Minus) {
        auto operation = peek().m_type == Token::Type::Plus ? BinaryExpression::Kind::Addition : BinaryExpression::Kind::Subtraction;
        consume();
        auto right = parse_additive_expression();
        auto result_var = create_unnamed_var(left->result()->node_type());
        assert(left->result()->node_type()->kind() == right->result()->node_type()->kind());

        auto result = create_ast_node<BinaryExpression>(operation, move(left), move(right), move(result_var));

        return maybe_correct_binop_tree(result, right, BinaryExpression::Kind::Subtraction, BinaryExpression::Kind::Addition);
    }
    return left;
}

// shift-expression:
//      - additive-expression
NonnullRefPtr<Expression> Parser::parse_shift_expression()
{
    SCOPE_LOGGER();
    return parse_additive_expression();
}

// compare-expression:
//      - shift-expression
NonnullRefPtr<Expression> Parser::parse_compare_expression()
{
    SCOPE_LOGGER();
    return parse_shift_expression();
}

// relational-expression
//      - compare-expression
NonnullRefPtr<Expression> Parser::parse_relational_expression()
{
    SCOPE_LOGGER();
    return parse_compare_expression();
}

// equality-expression:
//      - relational-expression
NonnullRefPtr<Expression> Parser::parse_equality_expression()
{
    SCOPE_LOGGER();
    return parse_relational_expression();
}

// and-expression
//      - equality-expression
NonnullRefPtr<Expression> Parser::parse_and_expression()
{
    SCOPE_LOGGER();
    return parse_equality_expression();
}

// exclusive-or-expression
//      - and-expression
NonnullRefPtr<Expression> Parser::parse_exclusive_or_operation()
{
    SCOPE_LOGGER();
    return parse_and_expression();
}

// inclusive-or-expression:
//      - exclusive-or-expression
NonnullRefPtr<Expression> Parser::parse_inclusive_or_expression()
{
    SCOPE_LOGGER();
    return parse_exclusive_or_operation();
}

// logical-and-expression:
//      - inclusive-or-expression
NonnullRefPtr<Expression> Parser::parse_logical_and_expression()
{
    SCOPE_LOGGER();
    return parse_inclusive_or_expression();
}

// logical-or-expression:
//      - logical-and-expression
NonnullRefPtr<Expression> Parser::parse_logical_or_expression()
{
    SCOPE_LOGGER();
    return parse_logical_and_expression();
}

// assignment-expression:
//      - logical-or-expression
NonnullRefPtr<Expression> Parser::parse_assignment_expression()
{
    SCOPE_LOGGER();
    return parse_logical_or_expression();
}

// expression:
//      - assignment-expression
NonnullRefPtr<Expression> Parser::parse_expression()
{
    SCOPE_LOGGER();
    return parse_assignment_expression();
}

// expr-or-braced-init-list
//      - expression
NonnullRefPtr<Expression> Parser::parse_expr_or_braced_init_list()
{
    SCOPE_LOGGER();

    return parse_expression();
}

// parse_jump_statement
//      - return expr-or-braced-init-list ;
NonnullRefPtr<Statement> Parser::parse_jump_statement()
{
    SCOPE_LOGGER();
    assert(consume(Token::Type::Keyword).m_known_keyword == Token::KnownKeyword::Return);

    RefPtr expression = parse_expr_or_braced_init_list();

    expect(Token::Type::Semicolon);
    return create_ast_node<ReturnStatement>(move(expression));
}

// statement:
//      - jump-statement
NonnullRefPtr<Statement> Parser::parse_statement()
{
    SCOPE_LOGGER();
    return parse_jump_statement();
}

// statement-seq:
//      - statement
NonnullRefPtr<Statement> Parser::parse_statement_seq()
{
    SCOPE_LOGGER();
    return parse_statement();
}

// compound-statement:
//      - { [statement-seq] }
NonnullRefPtrVector<ASTNode> Parser::parse_compound_statement()
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<ASTNode> body;
    expect(Token::Type::LeftCurly);
    if (peek().m_type != Token::Type::RightCurly)
        body.append(parse_statement_seq());
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
        auto body = parse_function_body();
        return create_ast_node<Function>(move(return_type), declarator.value().name, declarator.value().parameters, move(body));
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
            functions.append(move(function));
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
        m_tu.functions().append(move(functions));
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
