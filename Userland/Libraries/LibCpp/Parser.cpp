/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#ifdef CPP_DEBUG
#    define DEBUG_SPAM
#endif

#include "Parser.h"
#include "AK/LogStream.h"
#include "AST.h"
#include <AK/Debug.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopeLogger.h>
#include <LibCpp/Lexer.h>

namespace Cpp {

Parser::Parser(const StringView& program, const String& filename)
    : m_program(program)
    , m_lines(m_program.split_view("\n", true))
    , m_filename(filename)
{
    Lexer lexer(m_program);
    for (auto& token : lexer.lex()) {
        if (token.m_type == Token::Type::Whitespace)
            continue;
        m_tokens.append(move(token));
    }
#if CPP_DEBUG
    dbgln("Program:");
    dbgln("{}", m_program);
    dbgln("Tokens:");
    for (auto& token : m_tokens) {
        dbgln("{} ({}:{}-{}:{})", token.to_string(), token.start().line, token.start().column, token.end().line, token.end().column);
    }
#endif
}

NonnullRefPtr<TranslationUnit> Parser::parse()
{
    SCOPE_LOGGER();
    auto unit = create_root_ast_node(m_tokens.first().m_start, m_tokens.last().m_end);
    while (!done()) {
        if (match_comment()) {
            consume(Token::Type::Comment);
            continue;
        }

        if (match_preprocessor()) {
            consume_preprocessor();
            continue;
        }

        auto declaration = match_declaration();
        if (declaration.has_value()) {
            unit->append(parse_declaration(*unit, declaration.value()));
            continue;
        }

        error("unexpected token");
        consume();
    }
    return unit;
}

Optional<Parser::DeclarationType> Parser::match_declaration()
{
    switch (m_state.context) {
    case Context::InTranslationUnit:
        return match_declaration_in_translation_unit();
    case Context::InFunctionDefinition:
        return match_declaration_in_function_definition();
    default:
        error("unexpected context");
        return {};
    }
}

NonnullRefPtr<Declaration> Parser::parse_declaration(ASTNode& parent, DeclarationType declaration_type)
{
    switch (declaration_type) {
    case DeclarationType::Function:
        return parse_function_declaration(parent);
    case DeclarationType::Variable:
        return parse_variable_declaration(parent);
    case DeclarationType::Enum:
        return parse_enum_declaration(parent);
    case DeclarationType::Struct:
        return parse_struct_or_class_declaration(parent, StructOrClassDeclaration::Type::Struct);
    default:
        error("unexpected declaration type");
        return create_ast_node<InvalidDeclaration>(parent, position(), position());
    }
}

NonnullRefPtr<FunctionDeclaration> Parser::parse_function_declaration(ASTNode& parent)
{
    auto func = create_ast_node<FunctionDeclaration>(parent, position(), {});

    auto return_type_token = consume(Token::Type::KnownType);
    auto function_name = consume(Token::Type::Identifier);
    consume(Token::Type::LeftParen);
    auto parameters = parse_parameter_list(*func);
    consume(Token::Type::RightParen);

    RefPtr<FunctionDefinition> body;
    Position func_end {};
    if (peek(Token::Type::LeftCurly).has_value()) {
        body = parse_function_definition(*func);
        func_end = body->end();
    } else {
        func_end = position();
        consume(Token::Type::Semicolon);
    }

    func->m_name = text_of_token(function_name);
    func->m_return_type = create_ast_node<Type>(*func, return_type_token.m_start, return_type_token.m_end, text_of_token(return_type_token));
    if (parameters.has_value())
        func->m_parameters = move(parameters.value());
    func->m_definition = move(body);
    func->set_end(func_end);
    return func;
}

NonnullRefPtr<FunctionDefinition> Parser::parse_function_definition(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto func = create_ast_node<FunctionDefinition>(parent, position(), {});
    consume(Token::Type::LeftCurly);
    while (!eof() && peek().m_type != Token::Type::RightCurly) {
        func->statements().append(parse_statement(func));
    }
    func->set_end(position());
    if (!eof())
        consume(Token::Type::RightCurly);
    return func;
}

NonnullRefPtr<Statement> Parser::parse_statement(ASTNode& parent)
{
    SCOPE_LOGGER();
    ArmedScopeGuard consume_semicolumn([this]() {
        consume(Token::Type::Semicolon);
    });

    if (match_block_statement()) {
        consume_semicolumn.disarm();
        return parse_block_statement(parent);
    }
    if (match_comment()) {
        consume_semicolumn.disarm();
        return parse_comment(parent);
    }
    if (match_variable_declaration()) {
        return parse_variable_declaration(parent);
    }
    if (match_expression()) {
        return parse_expression(parent);
    }
    if (match_keyword("return")) {
        return parse_return_statement(parent);
    }
    if (match_keyword("for")) {
        consume_semicolumn.disarm();
        return parse_for_statement(parent);
    }
    if (match_keyword("if")) {
        consume_semicolumn.disarm();
        return parse_if_statement(parent);
    } else {
        error("unexpected statement type");
        consume_semicolumn.disarm();
        consume();
        return create_ast_node<InvalidStatement>(parent, position(), position());
    }
}

NonnullRefPtr<Comment> Parser::parse_comment(ASTNode& parent)
{
    auto comment = create_ast_node<Comment>(parent, position(), {});
    consume(Token::Type::Comment);
    comment->set_end(position());
    return comment;
}

bool Parser::match_block_statement()
{
    return peek().type() == Token::Type::LeftCurly;
}

NonnullRefPtr<BlockStatement> Parser::parse_block_statement(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto block_statement = create_ast_node<BlockStatement>(parent, position(), {});
    consume(Token::Type::LeftCurly);
    while (peek().type() != Token::Type::RightCurly) {
        block_statement->m_statements.append(parse_statement(*block_statement));
    }
    consume(Token::Type::RightCurly);
    block_statement->set_end(position());
    return block_statement;
}

bool Parser::match_variable_declaration()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    // Type
    if (!peek(Token::Type::KnownType).has_value() && !peek(Token::Type::Identifier).has_value())
        return false;
    consume();

    // Identifier
    if (!peek(Token::Type::Identifier).has_value())
        return false;
    consume();

    if (match(Token::Type::Equals)) {
        consume(Token::Type::Equals);
        if (!match_expression()) {
            error("initial value of variable is not an expression");
            return false;
        }
        return true;
    }

    return match(Token::Type::Semicolon);
}

NonnullRefPtr<VariableDeclaration> Parser::parse_variable_declaration(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto var = create_ast_node<VariableDeclaration>(parent, position(), {});
    auto type_token = consume();
    if (type_token.type() != Token::Type::KnownType && type_token.type() != Token::Type::Identifier) {
        error("unexpected token for variable type");
        var->set_end(type_token.end());
        return var;
    }
    auto identifier_token = consume(Token::Type::Identifier);
    RefPtr<Expression> initial_value;

    if (match(Token::Type::Equals)) {
        consume(Token::Type::Equals);
        initial_value = parse_expression(var);
    }

    var->set_end(position());
    var->m_type = create_ast_node<Type>(var, type_token.m_start, type_token.m_end, text_of_token(type_token));
    var->m_name = text_of_token(identifier_token);
    var->m_initial_value = move(initial_value);

    return var;
}

NonnullRefPtr<Expression> Parser::parse_expression(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto expression = parse_primary_expression(parent);
    // TODO: remove eof() logic, should still work without it
    if (eof() || match(Token::Type::Semicolon)) {
        return expression;
    }

    NonnullRefPtrVector<Expression> secondary_expressions;

    while (match_secondary_expression()) {
        // FIXME: Handle operator precedence
        expression = parse_secondary_expression(parent, expression);
        secondary_expressions.append(expression);
    }

    for (size_t i = 0; secondary_expressions.size() != 0 && i < secondary_expressions.size() - 1; ++i) {
        secondary_expressions[i].set_parent(secondary_expressions[i + 1]);
    }

    return expression;
}

bool Parser::match_secondary_expression()
{
    auto type = peek().type();
    return type == Token::Type::Plus
        || type == Token::Type::PlusEquals
        || type == Token::Type::Minus
        || type == Token::Type::MinusEquals
        || type == Token::Type::Asterisk
        || type == Token::Type::AsteriskEquals
        || type == Token::Type::Percent
        || type == Token::Type::PercentEquals
        || type == Token::Type::Equals
        || type == Token::Type::Greater
        || type == Token::Type::Greater
        || type == Token::Type::Less
        || type == Token::Type::LessEquals
        || type == Token::Type::Dot
        || type == Token::Type::PlusPlus
        || type == Token::Type::MinusMinus
        || type == Token::Type::And
        || type == Token::Type::AndEquals
        || type == Token::Type::Pipe
        || type == Token::Type::PipeEquals
        || type == Token::Type::Caret
        || type == Token::Type::CaretEquals
        || type == Token::Type::LessLess
        || type == Token::Type::LessLessEquals
        || type == Token::Type::GreaterGreater
        || type == Token::Type::GreaterGreaterEquals
        || type == Token::Type::AndAnd
        || type == Token::Type::PipePipe;
}

NonnullRefPtr<Expression> Parser::parse_primary_expression(ASTNode& parent)
{
    SCOPE_LOGGER();
    // TODO: remove eof() logic, should still work without it
    if (eof()) {
        auto node = create_ast_node<Identifier>(parent, position(), position());
        return node;
    }

    if (match_unary_expression())
        return parse_unary_expression(parent);

    if (match_literal()) {
        return parse_literal(parent);
    }
    switch (peek().type()) {
    case Token::Type::Identifier: {
        if (match_function_call())
            return parse_function_call(parent);
        auto token = consume();
        return create_ast_node<Identifier>(parent, token.m_start, token.m_end, text_of_token(token));
    }
    default: {
        error("could not parse primary expression");
        auto token = consume();
        return create_ast_node<InvalidExpression>(parent, token.m_start, token.m_end);
    }
    }
}

bool Parser::match_literal()
{
    switch (peek().type()) {
    case Token::Type::Integer:
        return true;
    case Token::Type::DoubleQuotedString:
        return true;
    case Token::Type::Keyword: {
        return match_boolean_literal();
    }
    default:
        return false;
    }
}

bool Parser::match_unary_expression()
{
    auto type = peek().type();
    return type == Token::Type::PlusPlus
        || type == Token::Type::MinusMinus
        || type == Token::Type::ExclamationMark
        || type == Token::Type::Tilde
        || type == Token::Type::Plus
        || type == Token::Type::Minus;
}

NonnullRefPtr<UnaryExpression> Parser::parse_unary_expression(ASTNode& parent)
{
    auto unary_exp = create_ast_node<UnaryExpression>(parent, position(), {});
    auto op_token = consume();
    UnaryOp op { UnaryOp::Invalid };
    switch (op_token.type()) {
    case Token::Type::Minus:
        op = UnaryOp::Minus;
        break;
    case Token::Type::Plus:
        op = UnaryOp::Plus;
        break;
    case Token::Type::ExclamationMark:
        op = UnaryOp::Not;
        break;
    case Token::Type::Tilde:
        op = UnaryOp::BitwiseNot;
        break;
    case Token::Type::PlusPlus:
        op = UnaryOp::PlusPlus;
        break;
    default:
        break;
    }
    unary_exp->m_op = op;
    auto lhs = parse_expression(*unary_exp);
    unary_exp->m_lhs = lhs;
    unary_exp->set_end(lhs->end());
    return unary_exp;
}

NonnullRefPtr<Expression> Parser::parse_literal(ASTNode& parent)
{
    switch (peek().type()) {
    case Token::Type::Integer: {
        auto token = consume();
        return create_ast_node<NumericLiteral>(parent, token.m_start, token.m_end, text_of_token(token));
    }
    case Token::Type::DoubleQuotedString: {
        return parse_string_literal(parent);
    }
    case Token::Type::Keyword: {
        if (match_boolean_literal())
            return parse_boolean_literal(parent);
        [[fallthrough]];
    }
    default: {
        error("could not parse literal");
        auto token = consume();
        return create_ast_node<InvalidExpression>(parent, token.m_start, token.m_end);
    }
    }
}

NonnullRefPtr<Expression> Parser::parse_secondary_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs)
{
    SCOPE_LOGGER();
    switch (peek().m_type) {
    case Token::Type::Plus:
        return parse_binary_expression(parent, lhs, BinaryOp::Addition);
    case Token::Type::Less:
        return parse_binary_expression(parent, lhs, BinaryOp::LessThan);
    case Token::Type::Equals:
        return parse_assignment_expression(parent, lhs, AssignmentOp::Assignment);
    case Token::Type::Dot: {
        consume();
        auto exp = create_ast_node<MemberExpression>(parent, lhs->start(), {});
        lhs->set_parent(*exp);
        exp->m_object = move(lhs);
        auto property_token = consume(Token::Type::Identifier);
        exp->m_property = create_ast_node<Identifier>(*exp, property_token.start(), property_token.end(), text_of_token(property_token));
        exp->set_end(property_token.end());
        return exp;
    }
    default: {
        error(String::formatted("unexpected operator for expression. operator: {}", peek().to_string()));
        auto token = consume();
        return create_ast_node<InvalidExpression>(parent, token.start(), token.end());
    }
    }
}

NonnullRefPtr<BinaryExpression> Parser::parse_binary_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs, BinaryOp op)
{
    consume(); // Operator
    auto exp = create_ast_node<BinaryExpression>(parent, lhs->start(), {});
    lhs->set_parent(*exp);
    exp->m_op = op;
    exp->m_lhs = move(lhs);
    auto rhs = parse_expression(exp);
    exp->set_end(rhs->end());
    exp->m_rhs = move(rhs);
    return exp;
}

NonnullRefPtr<AssignmentExpression> Parser::parse_assignment_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs, AssignmentOp op)
{
    consume(); // Operator
    auto exp = create_ast_node<AssignmentExpression>(parent, lhs->start(), {});
    lhs->set_parent(*exp);
    exp->m_op = op;
    exp->m_lhs = move(lhs);
    auto rhs = parse_expression(exp);
    exp->set_end(rhs->end());
    exp->m_rhs = move(rhs);
    return exp;
}

Optional<Parser::DeclarationType> Parser::match_declaration_in_translation_unit()
{
    if (match_function_declaration())
        return DeclarationType::Function;
    if (match_enum_declaration())
        return DeclarationType::Enum;
    if (match_struct_declaration())
        return DeclarationType::Struct;
    return {};
}

bool Parser::match_enum_declaration()
{
    return peek().type() == Token::Type::Keyword && text_of_token(peek()) == "enum";
}

bool Parser::match_struct_declaration()
{
    return peek().type() == Token::Type::Keyword && text_of_token(peek()) == "struct";
}

bool Parser::match_function_declaration()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    if (!peek(Token::Type::KnownType).has_value())
        return false;
    consume();

    if (!peek(Token::Type::Identifier).has_value())
        return false;
    consume();

    if (!peek(Token::Type::LeftParen).has_value())
        return false;
    consume();

    while (consume().m_type != Token::Type::RightParen && !eof()) { };

    if (peek(Token::Type::Semicolon).has_value() || peek(Token::Type::LeftCurly).has_value())
        return true;

    return false;
}

Optional<NonnullRefPtrVector<Parameter>> Parser::parse_parameter_list(ASTNode& parent)
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<Parameter> parameters;
    while (peek().m_type != Token::Type::RightParen && !eof()) {
        auto type = parse_type(parent);

        auto name_identifier = peek(Token::Type::Identifier);
        if (name_identifier.has_value())
            consume(Token::Type::Identifier);

        StringView name;
        if (name_identifier.has_value())
            name = text_of_token(name_identifier.value());

        auto param = create_ast_node<Parameter>(parent, type->start(), name_identifier.has_value() ? name_identifier.value().m_end : type->end(), name);

        param->m_type = move(type);
        parameters.append(move(param));
        if (peek(Token::Type::Comma).has_value())
            consume(Token::Type::Comma);
    }
    return parameters;
}

bool Parser::match_comment()
{
    return match(Token::Type::Comment);
}

bool Parser::match_whitespace()
{
    return match(Token::Type::Whitespace);
}

bool Parser::match_preprocessor()
{
    return match(Token::Type::PreprocessorStatement) || match(Token::Type::IncludeStatement);
}

void Parser::consume_preprocessor()
{
    SCOPE_LOGGER();
    switch (peek().type()) {
    case Token::Type::PreprocessorStatement:
        consume();
        break;
    case Token::Type::IncludeStatement:
        consume();
        consume(Token::Type::IncludePath);
        break;
    default:
        error("unexpected token while parsing preprocessor statement");
        consume();
    }
}

Optional<Token> Parser::consume_whitespace()
{
    SCOPE_LOGGER();
    return consume(Token::Type::Whitespace);
}

Token Parser::consume(Token::Type type)
{
    auto token = consume();
    if (token.type() != type)
        error(String::formatted("expected {} at {}:{}, found: {}", Token::type_to_string(type), token.start().line, token.start().column, Token::type_to_string(token.type())));
    return token;
}

bool Parser::match(Token::Type type)
{
    return peek().m_type == type;
}

Token Parser::consume()
{
    if (eof()) {
        error("C++ Parser: out of tokens");
        return { Token::Type::EOF_TOKEN, position(), position() };
    }
    return m_tokens[m_state.token_index++];
}

Token Parser::peek() const
{
    if (eof()) {
        return { Token::Type::EOF_TOKEN, position(), position() };
    }
    return m_tokens[m_state.token_index];
}

Optional<Token> Parser::peek(Token::Type type) const
{
    auto token = peek();
    if (token.m_type == type)
        return token;
    return {};
}

void Parser::save_state()
{
    m_saved_states.append(m_state);
}

void Parser::load_state()
{
    m_state = m_saved_states.take_last();
}

Optional<Parser::DeclarationType> Parser::match_declaration_in_function_definition()
{
    ASSERT_NOT_REACHED();
}

bool Parser::done()
{
    return m_state.token_index == m_tokens.size();
}

StringView Parser::text_of_token(const Cpp::Token& token) const
{
    ASSERT(token.m_start.line == token.m_end.line);
    ASSERT(token.m_start.column <= token.m_end.column);
    return m_lines[token.m_start.line].substring_view(token.m_start.column, token.m_end.column - token.m_start.column + 1);
}

StringView Parser::text_of_node(const ASTNode& node) const
{
    return text_of_range(node.start(), node.end());
}

StringView Parser::text_of_range(Position start, Position end) const
{
    if (start.line == end.line) {
        ASSERT(start.column <= end.column);
        return m_lines[start.line].substring_view(start.column, end.column - start.column + 1);
    }

    auto index_of_position([this](auto position) {
        size_t start_index = 0;
        for (size_t line = 0; line < position.line; ++line) {
            start_index += m_lines[line].length() + 1;
        }
        start_index += position.column;
        return start_index;
    });
    auto start_index = index_of_position(start);
    auto end_index = index_of_position(end);
    ASSERT(end_index >= start_index);
    return m_program.substring_view(start_index, end_index - start_index);
}

void Parser::error(StringView message)
{
    SCOPE_LOGGER();
    if (message.is_null() || message.is_empty())
        message = "<empty>";
    String formatted_message;
    if (m_state.token_index >= m_tokens.size()) {
        formatted_message = String::formatted("C++ Parsed error on EOF.{}", message);
    } else {
        formatted_message = String::formatted("C++ Parser error: {}. token: {} ({}:{})",
            message,
            m_state.token_index < m_tokens.size() ? text_of_token(m_tokens[m_state.token_index]) : "EOF",
            m_tokens[m_state.token_index].m_start.line,
            m_tokens[m_state.token_index].m_start.column);
    }
    m_errors.append(formatted_message);
    dbgln<CPP_DEBUG>("{}", formatted_message);
}

bool Parser::match_expression()
{
    auto token_type = peek().m_type;
    return token_type == Token::Type::Integer
        || token_type == Token::Type::Float
        || token_type == Token::Type::Identifier
        || token_type == Token::Type::DoubleQuotedString
        || match_unary_expression();
}

bool Parser::eof() const
{
    return m_state.token_index >= m_tokens.size();
}

Position Parser::position() const
{
    if (eof())
        return m_tokens.last().m_end;
    return peek().m_start;
}

RefPtr<ASTNode> Parser::eof_node() const
{
    ASSERT(m_tokens.size());
    return node_at(m_tokens.last().m_end);
}

RefPtr<ASTNode> Parser::node_at(Position pos) const
{
    ASSERT(!m_tokens.is_empty());
    RefPtr<ASTNode> match_node;
    for (auto& node : m_nodes) {
        if (node.start() > pos || node.end() < pos)
            continue;
        if (!match_node)
            match_node = node;
        else if (node_span_size(node) < node_span_size(*match_node))
            match_node = node;
    }
    return match_node;
}

Optional<Token> Parser::token_at(Position pos) const
{
    for (auto& token : m_tokens) {
        if (token.start() > pos || token.end() < pos)
            continue;
        return token;
    }
    return {};
}

size_t Parser::node_span_size(const ASTNode& node) const
{
    if (node.start().line == node.end().line)
        return node.end().column - node.start().column;

    size_t span_size = m_lines[node.start().line].length() - node.start().column;
    for (size_t line = node.start().line + 1; line < node.end().line; ++line) {
        span_size += m_lines[line].length();
    }
    return span_size + m_lines[node.end().line].length() - node.end().column;
}

void Parser::print_tokens() const
{
    for (auto& token : m_tokens) {
        dbgln("{}", token.to_string());
    }
}

bool Parser::match_function_call()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };
    if (!match(Token::Type::Identifier))
        return false;
    consume();
    return match(Token::Type::LeftParen);
}

NonnullRefPtr<FunctionCall> Parser::parse_function_call(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto call = create_ast_node<FunctionCall>(parent, position(), {});
    auto name_identifier = consume(Token::Type::Identifier);
    call->m_name = text_of_token(name_identifier);

    NonnullRefPtrVector<Expression> args;
    consume(Token::Type::LeftParen);
    while (peek().type() != Token::Type::RightParen && !eof()) {
        args.append(parse_expression(*call));
        if (peek().type() == Token::Type::Comma)
            consume(Token::Type::Comma);
    }
    consume(Token::Type::RightParen);
    call->m_arguments = move(args);
    call->set_end(position());
    return call;
}

NonnullRefPtr<StringLiteral> Parser::parse_string_literal(ASTNode& parent)
{
    SCOPE_LOGGER();
    Optional<size_t> start_token_index;
    Optional<size_t> end_token_index;
    while (!eof()) {
        auto token = peek();
        if (token.type() != Token::Type::DoubleQuotedString && token.type() != Token::Type::EscapeSequence) {
            ASSERT(start_token_index.has_value());
            end_token_index = m_state.token_index - 1;
            break;
        }
        if (!start_token_index.has_value())
            start_token_index = m_state.token_index;
        consume();
    }

    // String was not terminated
    if (!end_token_index.has_value()) {
        end_token_index = m_tokens.size() - 1;
    }

    ASSERT(start_token_index.has_value());
    ASSERT(end_token_index.has_value());

    Token start_token = m_tokens[start_token_index.value()];
    Token end_token = m_tokens[end_token_index.value()];

    auto text = text_of_range(start_token.start(), end_token.end());
    auto string_literal = create_ast_node<StringLiteral>(parent, start_token.start(), end_token.end());
    string_literal->m_value = text;
    return string_literal;
}

NonnullRefPtr<ReturnStatement> Parser::parse_return_statement(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto return_statement = create_ast_node<ReturnStatement>(parent, position(), {});
    consume(Token::Type::Keyword);
    auto expression = parse_expression(*return_statement);
    return_statement->m_value = expression;
    return_statement->set_end(expression->end());
    return return_statement;
}

NonnullRefPtr<EnumDeclaration> Parser::parse_enum_declaration(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto enum_decl = create_ast_node<EnumDeclaration>(parent, position(), {});
    consume_keyword("enum");
    auto name_token = consume(Token::Type::Identifier);
    enum_decl->m_name = text_of_token(name_token);
    consume(Token::Type::LeftCurly);
    while (peek().type() != Token::Type::RightCurly && !eof()) {
        enum_decl->m_entries.append(text_of_token(consume(Token::Type::Identifier)));
        if (peek().type() != Token::Type::Comma) {
            break;
        }
        consume(Token::Type::Comma);
    }
    consume(Token::Type::RightCurly);
    consume(Token::Type::Semicolon);
    enum_decl->set_end(position());
    return enum_decl;
}

Token Parser::consume_keyword(const String& keyword)
{
    auto token = consume();
    if (token.type() != Token::Type::Keyword) {
        error(String::formatted("unexpected token: {}, expected Keyword", token.to_string()));
        return token;
    }
    if (text_of_token(token) != keyword) {
        error(String::formatted("unexpected keyword: {}, expected {}", text_of_token(token), keyword));
        return token;
    }
    return token;
}

bool Parser::match_keyword(const String& keyword)
{
    auto token = peek();
    if (token.type() != Token::Type::Keyword) {
        return false;
    }
    if (text_of_token(token) != keyword) {
        return false;
    }
    return true;
}

NonnullRefPtr<StructOrClassDeclaration> Parser::parse_struct_or_class_declaration(ASTNode& parent, StructOrClassDeclaration::Type type)
{
    SCOPE_LOGGER();
    auto decl = create_ast_node<StructOrClassDeclaration>(parent, position(), {}, type);
    switch (type) {
    case StructOrClassDeclaration::Type::Struct:
        consume_keyword("struct");
        break;
    case StructOrClassDeclaration::Type::Class:
        consume_keyword("class");
        break;
    }
    auto name_token = consume(Token::Type::Identifier);
    decl->m_name = text_of_token(name_token);

    consume(Token::Type::LeftCurly);

    while (peek().type() != Token::Type::RightCurly && !eof()) {
        decl->m_members.append(parse_member_declaration(*decl));
    }

    consume(Token::Type::RightCurly);
    consume(Token::Type::Semicolon);
    decl->set_end(position());
    return decl;
}

NonnullRefPtr<MemberDeclaration> Parser::parse_member_declaration(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto member_decl = create_ast_node<MemberDeclaration>(parent, position(), {});
    auto type_token = consume();
    auto identifier_token = consume(Token::Type::Identifier);
    RefPtr<Expression> initial_value;

    if (match(Token::Type::LeftCurly)) {
        consume(Token::Type::LeftCurly);
        initial_value = parse_expression(*member_decl);
        consume(Token::Type::RightCurly);
    }

    member_decl->m_type = create_ast_node<Type>(*member_decl, type_token.m_start, type_token.m_end, text_of_token(type_token));
    member_decl->m_name = text_of_token(identifier_token);
    member_decl->m_initial_value = move(initial_value);
    consume(Token::Type::Semicolon);
    member_decl->set_end(position());

    return member_decl;
}

NonnullRefPtr<BooleanLiteral> Parser::parse_boolean_literal(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto token = consume(Token::Type::Keyword);
    auto text = text_of_token(token);
    // text == "true" || text == "false";
    bool value = (text == "true");
    return create_ast_node<BooleanLiteral>(parent, token.start(), token.end(), value);
}

bool Parser::match_boolean_literal()
{
    auto token = peek();
    if (token.type() != Token::Type::Keyword)
        return false;
    auto text = text_of_token(token);
    return text == "true" || text == "false";
}

NonnullRefPtr<Type> Parser::parse_type(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto token = consume();
    auto type = create_ast_node<Type>(parent, token.start(), token.end(), text_of_token(token));
    if (token.type() != Token::Type::KnownType && token.type() != Token::Type::Identifier) {
        error(String::formatted("unexpected token for type: {}", token.to_string()));
        return type;
    }
    while (peek().type() == Token::Type::Asterisk) {
        auto asterisk = consume();
        auto ptr = create_ast_node<Pointer>(type, asterisk.start(), asterisk.end());
        ptr->m_pointee = type;
        type = ptr;
    }
    return type;
}

NonnullRefPtr<ForStatement> Parser::parse_for_statement(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto for_statement = create_ast_node<ForStatement>(parent, position(), {});
    consume(Token::Type::Keyword);
    consume(Token::Type::LeftParen);
    for_statement->m_init = parse_variable_declaration(*for_statement);
    consume(Token::Type::Semicolon);
    for_statement->m_test = parse_expression(*for_statement);
    consume(Token::Type::Semicolon);
    for_statement->m_update = parse_expression(*for_statement);
    consume(Token::Type::RightParen);
    for_statement->m_body = parse_statement(*for_statement);
    for_statement->set_end(for_statement->m_body->end());
    return for_statement;
}

NonnullRefPtr<IfStatement> Parser::parse_if_statement(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto if_statement = create_ast_node<IfStatement>(parent, position(), {});
    consume(Token::Type::Keyword);
    consume(Token::Type::LeftParen);
    if_statement->m_predicate = parse_expression(*if_statement);
    consume(Token::Type::RightParen);
    if_statement->m_then = parse_statement(*if_statement);
    if (match_keyword("else")) {
        consume(Token::Type::Keyword);
        if_statement->m_else = parse_statement(*if_statement);
        if_statement->set_end(if_statement->m_else->end());
    } else {
        if_statement->set_end(if_statement->m_then->end());
    }
    return if_statement;
}
}
