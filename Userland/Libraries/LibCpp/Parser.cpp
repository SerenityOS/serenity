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
#include "AST.h"
#include <AK/Debug.h>
#include <AK/ScopeGuard.h>
#include <AK/ScopeLogger.h>
#include <LibCpp/Lexer.h>

namespace Cpp {

Parser::Parser(const StringView& program, const String& filename, Preprocessor::Definitions&& definitions)
    : m_definitions(move(definitions))
    , m_filename(filename)
{
    initialize_program_tokens(program);
#if CPP_DEBUG
    dbgln("Tokens:");
    for (auto& token : m_tokens) {
        StringView text;
        if (token.start().line != token.end().line || token.start().column > token.end().column)
            text = {};
        else
            text = text_of_token(token);
        dbgln("{}  {}:{}-{}:{} ({})", token.to_string(), token.start().line, token.start().column, token.end().line, token.end().column, text);
    }
#endif
}

void Parser::initialize_program_tokens(const StringView& program)
{
    Lexer lexer(program);
    for (auto& token : lexer.lex()) {
        if (token.type() == Token::Type::Whitespace)
            continue;
        if (token.type() == Token::Type::Identifier) {
            if (auto defined_value = m_definitions.find(text_of_token(token)); defined_value != m_definitions.end()) {
                add_tokens_for_preprocessor(token, defined_value->value);
                m_replaced_preprocessor_tokens.append({ token, defined_value->value });
                continue;
            }
        }
        m_tokens.append(move(token));
    }
}

NonnullRefPtr<TranslationUnit> Parser::parse()
{
    SCOPE_LOGGER();
    if (m_tokens.is_empty())
        return create_root_ast_node({}, {});
    auto unit = create_root_ast_node(m_tokens.first().start(), m_tokens.last().end());
    unit->m_declarations = parse_declarations_in_translation_unit(*unit);
    return unit;
}

NonnullRefPtrVector<Declaration> Parser::parse_declarations_in_translation_unit(ASTNode& parent)
{
    NonnullRefPtrVector<Declaration> declarations;
    while (!eof()) {
        auto declaration = parse_single_declaration_in_translation_unit(parent);
        if (declaration) {
            declarations.append(declaration.release_nonnull());
        } else {
            error("unexpected token");
            consume();
        }
    }
    return declarations;
}

RefPtr<Declaration> Parser::parse_single_declaration_in_translation_unit(ASTNode& parent)
{
    while (!eof()) {
        if (match_comment()) {
            consume(Token::Type::Comment);
            continue;
        }

        if (match_preprocessor()) {
            consume_preprocessor();
            continue;
        }

        auto declaration = match_declaration_in_translation_unit();
        if (declaration.has_value()) {
            return parse_declaration(parent, declaration.value());
        }
        return {};
    }
    return {};
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
    case DeclarationType::Namespace:
        return parse_namespace_declaration(parent);
    default:
        error("unexpected declaration type");
        return create_ast_node<InvalidDeclaration>(parent, position(), position());
    }
}

NonnullRefPtr<FunctionDeclaration> Parser::parse_function_declaration(ASTNode& parent)
{
    auto func = create_ast_node<FunctionDeclaration>(parent, position(), {});

    func->m_qualifiers = parse_function_qualifiers();
    func->m_return_type = parse_type(*func);

    auto function_name = consume(Token::Type::Identifier);
    func->m_name = text_of_token(function_name);

    consume(Token::Type::LeftParen);
    auto parameters = parse_parameter_list(*func);
    if (parameters.has_value())
        func->m_parameters = move(parameters.value());

    consume(Token::Type::RightParen);

    RefPtr<FunctionDefinition> body;
    Position func_end {};
    if (peek(Token::Type::LeftCurly).has_value()) {
        body = parse_function_definition(*func);
        func_end = body->end();
    } else {
        func_end = position();
        if (match_attribute_specification())
            consume_attribute_specification(); // we don't use the value of __attribute__
        consume(Token::Type::Semicolon);
    }

    func->m_definition = move(body);
    func->set_end(func_end);
    return func;
}

NonnullRefPtr<FunctionDefinition> Parser::parse_function_definition(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto func = create_ast_node<FunctionDefinition>(parent, position(), {});
    consume(Token::Type::LeftCurly);
    while (!eof() && peek().type() != Token::Type::RightCurly) {
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
    ArmedScopeGuard consume_semicolon([this]() {
        consume(Token::Type::Semicolon);
    });

    if (match_block_statement()) {
        consume_semicolon.disarm();
        return parse_block_statement(parent);
    }
    if (match_comment()) {
        consume_semicolon.disarm();
        return parse_comment(parent);
    }
    if (match_variable_declaration()) {
        return parse_variable_declaration(parent, false);
    }
    if (match_expression()) {
        return parse_expression(parent);
    }
    if (match_keyword("return")) {
        return parse_return_statement(parent);
    }
    if (match_keyword("for")) {
        consume_semicolon.disarm();
        return parse_for_statement(parent);
    }
    if (match_keyword("if")) {
        consume_semicolon.disarm();
        return parse_if_statement(parent);
    } else {
        error("unexpected statement type");
        consume_semicolon.disarm();
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
    while (!eof() && peek().type() != Token::Type::RightCurly) {
        block_statement->m_statements.append(parse_statement(*block_statement));
    }
    consume(Token::Type::RightCurly);
    block_statement->set_end(position());
    return block_statement;
}

Parser::TemplatizedMatchResult Parser::match_type()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    parse_type_qualifiers();
    if (match_keyword("struct")) {
        consume(Token::Type::Keyword); // Consume struct prefix
    }

    if (!match_name())
        return TemplatizedMatchResult::NoMatch;
    parse_name(*m_root_node);

    if (peek(Token::Type::Less).has_value()) {
        if (match_template_arguments()) {
            return TemplatizedMatchResult::Templatized;
        }
        return TemplatizedMatchResult::NoMatch;
    }

    return TemplatizedMatchResult::Regular;
}

bool Parser::match_template_arguments()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    if (!peek(Token::Type::Less).has_value())
        return false;
    consume();

    while (!eof() && peek().type() != Token::Type::Greater) {
        if (match_type() == TemplatizedMatchResult::NoMatch)
            return false;
        parse_type(*m_root_node);
    }

    return peek().type() == Token::Type::Greater;
}

NonnullRefPtrVector<Type> Parser::parse_template_arguments(ASTNode& parent)
{
    SCOPE_LOGGER();

    consume(Token::Type::Less);

    NonnullRefPtrVector<Type> template_arguments;
    while (!eof() && peek().type() != Token::Type::Greater) {
        template_arguments.append(parse_type(parent));
    }

    consume(Token::Type::Greater);

    return template_arguments;
}

bool Parser::match_variable_declaration()
{
    SCOPE_LOGGER();
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    if (match_type() == TemplatizedMatchResult::NoMatch) {
        return false;
    }

    VERIFY(m_root_node);
    parse_type(*m_root_node);

    // Identifier
    if (!peek(Token::Type::Identifier).has_value()) {
        return false;
    }
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

NonnullRefPtr<VariableDeclaration> Parser::parse_variable_declaration(ASTNode& parent, bool expect_semicolon)
{
    SCOPE_LOGGER();
    auto var = create_ast_node<VariableDeclaration>(parent, position(), {});
    if (!match_variable_declaration()) {
        error("unexpected token for variable type");
        var->set_end(position());
        return var;
    }
    var->m_type = parse_type(var);
    auto identifier_token = consume(Token::Type::Identifier);
    RefPtr<Expression> initial_value;

    if (match(Token::Type::Equals)) {
        consume(Token::Type::Equals);
        initial_value = parse_expression(var);
    }

    if (expect_semicolon)
        consume(Token::Type::Semicolon);

    var->set_end(position());
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
        || type == Token::Type::EqualsEquals
        || type == Token::Type::AndAnd
        || type == Token::Type::PipePipe
        || type == Token::Type::ExclamationMarkEquals
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

    if (match_cpp_cast_expression())
        return parse_cpp_cast_expression(parent);

    if (match_name()) {
        if (match_function_call() != TemplatizedMatchResult::NoMatch)
            return parse_function_call(parent);
        return parse_name(parent);
    }

    error("could not parse primary expression");
    auto token = consume();
    return create_ast_node<InvalidExpression>(parent, token.start(), token.end());
}

bool Parser::match_literal()
{
    switch (peek().type()) {
    case Token::Type::Integer:
        return true;
    case Token::Type::SingleQuotedString:
        return true;
    case Token::Type::DoubleQuotedString:
        return true;
    case Token::Type::Float:
        return true;
    case Token::Type::Keyword: {
        return match_boolean_literal() || peek().text() == "nullptr";
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
        || type == Token::Type::Minus
        || type == Token::Type::And;
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
    case Token::Type::And:
        op = UnaryOp::Address;
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
        return create_ast_node<NumericLiteral>(parent, token.start(), token.end(), text_of_token(token));
    }
    case Token::Type::SingleQuotedString:
        [[fallthrough]];
    case Token::Type::DoubleQuotedString:
        return parse_string_literal(parent);
    case Token::Type::Keyword: {
        if (match_boolean_literal())
            return parse_boolean_literal(parent);
        if (peek().text() == "nullptr") {
            auto token = consume();
            return create_ast_node<NullPointerLiteral>(parent, token.start(), token.end());
        }
        [[fallthrough]];
    }
    default: {
        error("could not parse literal");
        auto token = consume();
        return create_ast_node<InvalidExpression>(parent, token.start(), token.end());
    }
    }
}

NonnullRefPtr<Expression> Parser::parse_secondary_expression(ASTNode& parent, NonnullRefPtr<Expression> lhs)
{
    SCOPE_LOGGER();
    switch (peek().type()) {
    case Token::Type::Plus:
        return parse_binary_expression(parent, lhs, BinaryOp::Addition);
    case Token::Type::Less:
        return parse_binary_expression(parent, lhs, BinaryOp::LessThan);
    case Token::Type::EqualsEquals:
        return parse_binary_expression(parent, lhs, BinaryOp::EqualsEquals);
    case Token::Type::ExclamationMarkEquals:
        return parse_binary_expression(parent, lhs, BinaryOp::NotEqual);
    case Token::Type::And:
        return parse_binary_expression(parent, lhs, BinaryOp::BitwiseAnd);
    case Token::Type::AndAnd:
        return parse_binary_expression(parent, lhs, BinaryOp::LogicalAnd);
    case Token::Type::Pipe:
        return parse_binary_expression(parent, lhs, BinaryOp::BitwiseOr);
    case Token::Type::PipePipe:
        return parse_binary_expression(parent, lhs, BinaryOp::LogicalOr);
    case Token::Type::Equals:
        return parse_assignment_expression(parent, lhs, AssignmentOp::Assignment);
    case Token::Type::Dot: {
        consume();
        auto exp = create_ast_node<MemberExpression>(parent, lhs->start(), {});
        lhs->set_parent(*exp);
        exp->m_object = move(lhs);
        exp->m_property = parse_expression(*exp);
        exp->set_end(position());
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
    if (match_namespace_declaration())
        return DeclarationType::Namespace;
    if (match_variable_declaration())
        return DeclarationType::Variable;
    return {};
}

bool Parser::match_enum_declaration()
{
    return match_keyword("enum");
}

bool Parser::match_struct_declaration()
{
    return match_keyword("struct");
}

bool Parser::match_namespace_declaration()
{
    return match_keyword("namespace");
}

bool Parser::match_function_declaration()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    parse_function_qualifiers();

    if (match_type() == TemplatizedMatchResult::NoMatch)
        return false;
    VERIFY(m_root_node);
    parse_type(*m_root_node);

    if (!peek(Token::Type::Identifier).has_value())
        return false;
    consume();

    if (!peek(Token::Type::LeftParen).has_value())
        return false;
    consume();

    while (consume().type() != Token::Type::RightParen && !eof()) { };

    if (peek(Token::Type::Semicolon).has_value() || peek(Token::Type::LeftCurly).has_value())
        return true;

    if (match_attribute_specification()) {
        consume_attribute_specification();
        return peek(Token::Type::Semicolon).has_value();
    }

    return false;
}

Optional<NonnullRefPtrVector<Parameter>> Parser::parse_parameter_list(ASTNode& parent)
{
    SCOPE_LOGGER();
    NonnullRefPtrVector<Parameter> parameters;
    while (peek().type() != Token::Type::RightParen && !eof()) {
        if (match_ellipsis()) {
            auto last_dot = consume();
            while (peek().type() == Token::Type::Dot)
                last_dot = consume();
            auto param = create_ast_node<Parameter>(parent, position(), last_dot.end(), StringView {});
            param->m_is_ellipsis = true;
            parameters.append(move(param));
        } else {
            auto type = parse_type(parent);

            auto name_identifier = peek(Token::Type::Identifier);
            if (name_identifier.has_value())
                consume(Token::Type::Identifier);

            StringView name;
            if (name_identifier.has_value())
                name = text_of_token(name_identifier.value());

            auto param = create_ast_node<Parameter>(parent, type->start(), name_identifier.has_value() ? name_identifier.value().end() : type->end(), name);

            param->m_type = move(type);
            parameters.append(move(param));
        }

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
    return peek().type() == type;
}

Token Parser::consume()
{
    if (eof()) {
        error("C++ Parser: out of tokens");
        return { Token::Type::EOF_TOKEN, position(), position(), {} };
    }
    return m_tokens[m_state.token_index++];
}

Token Parser::peek(size_t offset) const
{
    if (m_state.token_index + offset >= m_tokens.size())
        return { Token::Type::EOF_TOKEN, position(), position(), {} };
    return m_tokens[m_state.token_index + offset];
}

Optional<Token> Parser::peek(Token::Type type) const
{
    auto token = peek();
    if (token.type() == type)
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

StringView Parser::text_of_token(const Cpp::Token& token) const
{
    return token.text();
}

String Parser::text_of_node(const ASTNode& node) const
{
    return text_in_range(node.start(), node.end());
}

String Parser::text_in_range(Position start, Position end) const
{
    auto start_token_index = index_of_token_at(start);
    auto end_node_index = index_of_token_at(end);
    VERIFY(start_token_index.has_value());
    VERIFY(end_node_index.has_value());
    StringBuilder text;
    for (size_t i = start_token_index.value(); i <= end_node_index.value(); ++i) {
        text.append(m_tokens[i].text());
    }
    return text.build();
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
            m_tokens[m_state.token_index].start().line,
            m_tokens[m_state.token_index].start().column);
    }

    m_state.errors.append(formatted_message);
}

bool Parser::match_expression()
{
    auto token_type = peek().type();
    return match_literal()
        || token_type == Token::Type::Identifier
        || match_unary_expression()
        || match_cpp_cast_expression();
}

bool Parser::eof() const
{
    return m_state.token_index >= m_tokens.size();
}

Position Parser::position() const
{
    if (eof())
        return m_tokens.last().end();
    return peek().start();
}

RefPtr<ASTNode> Parser::eof_node() const
{
    VERIFY(m_tokens.size());
    return node_at(m_tokens.last().end());
}

RefPtr<ASTNode> Parser::node_at(Position pos) const
{
    auto index = index_of_node_at(pos);
    if (!index.has_value())
        return nullptr;
    return m_nodes[index.value()];
}

Optional<size_t> Parser::index_of_node_at(Position pos) const
{
    VERIFY(!m_tokens.is_empty());
    Optional<size_t> match_node_index;

    auto node_span = [](const ASTNode& node) {
        VERIFY(node.end().line >= node.start().line);
        VERIFY((node.end().line > node.start().line) || (node.end().column >= node.start().column));
        return Position { node.end().line - node.start().line, node.start().line != node.end().line ? 0 : node.end().column - node.start().column };
    };

    for (size_t node_index = 0; node_index < m_nodes.size(); ++node_index) {
        auto& node = m_nodes[node_index];
        if (node.start() > pos || node.end() < pos)
            continue;

        if (!match_node_index.has_value() || (node_span(node) < node_span(m_nodes[match_node_index.value()])))
            match_node_index = node_index;
    }
    return match_node_index;
}

Optional<Token> Parser::token_at(Position pos) const
{
    auto index = index_of_token_at(pos);
    if (!index.has_value())
        return {};
    return m_tokens[index.value()];
}

Optional<size_t> Parser::index_of_token_at(Position pos) const
{
    for (size_t token_index = 0; token_index < m_tokens.size(); ++token_index) {
        auto token = m_tokens[token_index];
        if (token.start() > pos || token.end() < pos)
            continue;
        return token_index;
    }
    return {};
}

void Parser::print_tokens() const
{
    for (auto& token : m_tokens) {
        dbgln("{}", token.to_string());
    }
}

Parser::TemplatizedMatchResult Parser::match_function_call()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };
    if (!match_name())
        return TemplatizedMatchResult::NoMatch;
    parse_name(*m_root_node);

    bool is_templatized = false;
    if (match_template_arguments()) {
        is_templatized = true;
        parse_template_arguments(*m_root_node);
    }

    if (!match(Token::Type::LeftParen))
        return TemplatizedMatchResult::NoMatch;

    return is_templatized ? TemplatizedMatchResult::Templatized : TemplatizedMatchResult::Regular;
}

NonnullRefPtr<FunctionCall> Parser::parse_function_call(ASTNode& parent)
{
    SCOPE_LOGGER();

    auto match_result = match_type();
    if (match_result == TemplatizedMatchResult::NoMatch) {
        error("expected type");
        return create_ast_node<FunctionCall>(parent, position(), position());
    }

    bool is_templatized = match_result == TemplatizedMatchResult::Templatized;

    RefPtr<FunctionCall> call;
    if (is_templatized) {
        call = create_ast_node<TemplatizedFunctionCall>(parent, position(), {});
    } else {
        call = create_ast_node<FunctionCall>(parent, position(), {});
    }

    call->m_name = parse_name(*call);
    if (is_templatized) {
        static_cast<TemplatizedFunctionCall&>(*call).m_template_arguments = parse_template_arguments(*call);
    }

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

    return call.release_nonnull();
}

NonnullRefPtr<StringLiteral> Parser::parse_string_literal(ASTNode& parent)
{
    SCOPE_LOGGER();
    Optional<size_t> start_token_index;
    Optional<size_t> end_token_index;
    while (!eof()) {
        auto token = peek();
        if (token.type() != Token::Type::DoubleQuotedString && token.type() != Token::Type::SingleQuotedString && token.type() != Token::Type::EscapeSequence) {
            VERIFY(start_token_index.has_value());
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

    VERIFY(start_token_index.has_value());
    VERIFY(end_token_index.has_value());

    Token start_token = m_tokens[start_token_index.value()];
    Token end_token = m_tokens[end_token_index.value()];

    auto text = text_in_range(start_token.start(), end_token.end());
    auto string_literal = create_ast_node<StringLiteral>(parent, start_token.start(), end_token.end());
    string_literal->m_value = text;
    return string_literal;
}

NonnullRefPtr<ReturnStatement> Parser::parse_return_statement(ASTNode& parent)
{
    SCOPE_LOGGER();
    auto return_statement = create_ast_node<ReturnStatement>(parent, position(), {});
    consume(Token::Type::Keyword);
    if (!peek(Token::Type::Semicolon).has_value()) {
        auto expression = parse_expression(*return_statement);
        return_statement->m_value = expression;
    }
    return_statement->set_end(position());
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
    while (!eof() && peek().type() != Token::Type::RightCurly) {
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

    while (!eof() && peek().type() != Token::Type::RightCurly) {
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
    member_decl->m_type = parse_type(*member_decl);

    auto identifier_token = consume(Token::Type::Identifier);
    member_decl->m_name = text_of_token(identifier_token);

    RefPtr<Expression> initial_value;
    if (match(Token::Type::LeftCurly)) {
        consume(Token::Type::LeftCurly);
        initial_value = parse_expression(*member_decl);
        consume(Token::Type::RightCurly);
    }
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

    auto match_result = match_type();
    if (match_result == TemplatizedMatchResult::NoMatch) {
        auto token = consume();
        return create_ast_node<Type>(parent, token.start(), token.end());
    }
    bool is_templatized = match_result == TemplatizedMatchResult::Templatized;

    RefPtr<Type> type;
    if (is_templatized) {
        type = create_ast_node<TemplatizedType>(parent, position(), {});
    } else {
        type = create_ast_node<Type>(parent, position(), {});
    }

    auto qualifiers = parse_type_qualifiers();
    type->m_qualifiers = move(qualifiers);

    if (match_keyword("struct")) {
        consume(Token::Type::Keyword); // Consume struct prefix
    }

    if (!match_name()) {
        type->set_end(position());
        error(String::formatted("expected name instead of: {}", peek().text()));
        return type.release_nonnull();
    }
    type->m_name = parse_name(*type);

    if (is_templatized) {
        static_cast<TemplatizedType&>(*type).m_template_arguments = parse_template_arguments(*type);
    }

    while (!eof() && peek().type() == Token::Type::Asterisk) {
        type->set_end(position());
        auto asterisk = consume();
        auto ptr = create_ast_node<Pointer>(parent, asterisk.start(), asterisk.end());
        type->set_parent(*ptr);
        ptr->m_pointee = type;
        type = ptr;
    }

    type->set_end(position());
    return type.release_nonnull();
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
Vector<StringView> Parser::parse_type_qualifiers()
{
    SCOPE_LOGGER();
    Vector<StringView> qualifiers;
    while (!eof()) {
        auto token = peek();
        if (token.type() != Token::Type::Keyword)
            break;
        auto text = text_of_token(token);
        if (text == "static" || text == "const") {
            qualifiers.append(text);
            consume();
        } else {
            break;
        }
    }
    return qualifiers;
}

Vector<StringView> Parser::parse_function_qualifiers()
{
    SCOPE_LOGGER();
    Vector<StringView> qualifiers;
    while (!eof()) {
        auto token = peek();
        if (token.type() != Token::Type::Keyword)
            break;
        auto text = text_of_token(token);
        if (text == "static" || text == "inline") {
            qualifiers.append(text);
            consume();
        } else {
            break;
        }
    }
    return qualifiers;
}

bool Parser::match_attribute_specification()
{
    return text_of_token(peek()) == "__attribute__";
}
void Parser::consume_attribute_specification()
{
    consume(); // __attribute__
    consume(Token::Type::LeftParen);
    size_t left_count = 1;
    while (!eof()) {
        auto token = consume();
        if (token.type() == Token::Type::LeftParen) {
            ++left_count;
        }
        if (token.type() == Token::Type::RightParen) {
            --left_count;
        }
        if (left_count == 0)
            return;
    }
}

bool Parser::match_ellipsis()
{
    if (m_state.token_index > m_tokens.size() - 3)
        return false;
    return peek().type() == Token::Type::Dot && peek().type() == Token::Type::Dot && peek().type() == Token::Type::Dot;
}
void Parser::add_tokens_for_preprocessor(Token& replaced_token, Preprocessor::DefinedValue& definition)
{
    if (!definition.value.has_value())
        return;
    Lexer lexer(definition.value.value());
    for (auto token : lexer.lex()) {
        if (token.type() == Token::Type::Whitespace)
            continue;
        token.set_start(replaced_token.start());
        token.set_end(replaced_token.end());
        m_tokens.append(move(token));
    }
}

NonnullRefPtr<NamespaceDeclaration> Parser::parse_namespace_declaration(ASTNode& parent, bool is_nested_namespace)
{
    auto namespace_decl = create_ast_node<NamespaceDeclaration>(parent, position(), {});

    if (!is_nested_namespace)
        consume(Token::Type::Keyword);

    auto name_token = consume(Token::Type::Identifier);
    namespace_decl->m_name = name_token.text();

    if (peek().type() == Token::Type::ColonColon) {
        consume(Token::Type::ColonColon);
        namespace_decl->m_declarations.append(parse_namespace_declaration(*namespace_decl, true));
        namespace_decl->set_end(position());
        return namespace_decl;
    }

    consume(Token::Type::LeftCurly);
    while (!eof() && peek().type() != Token::Type::RightCurly) {
        auto declaration = parse_single_declaration_in_translation_unit(*namespace_decl);
        if (declaration) {
            namespace_decl->m_declarations.append(declaration.release_nonnull());
        } else {
            error("unexpected token");
            consume();
        }
    }
    consume(Token::Type::RightCurly);
    namespace_decl->set_end(position());
    return namespace_decl;
}

bool Parser::match_name()
{
    auto type = peek().type();
    return type == Token::Type::Identifier || type == Token::Type::KnownType;
}

NonnullRefPtr<Name> Parser::parse_name(ASTNode& parent)
{
    auto name_node = create_ast_node<Name>(parent, position(), {});
    while (!eof() && (peek().type() == Token::Type::Identifier || peek().type() == Token::Type::KnownType)) {
        auto token = consume();
        name_node->m_scope.append(create_ast_node<Identifier>(*name_node, token.start(), token.end(), token.text()));
        if (peek().type() == Token::Type::ColonColon)
            consume();
        else
            break;
    }

    VERIFY(!name_node->m_scope.is_empty());
    name_node->m_name = name_node->m_scope.take_last();
    name_node->set_end(position());
    return name_node;
}

bool Parser::match_cpp_cast_expression()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    auto token = consume();
    if (token.type() != Token::Type::Keyword)
        return false;

    auto text = token.text();
    if (text == "static_cast" || text == "reinterpret_cast" || text == "dynamic_cast" || text == "const_cast")
        return true;
    return false;
}

NonnullRefPtr<CppCastExpression> Parser::parse_cpp_cast_expression(ASTNode& parent)
{
    auto cast_expression = create_ast_node<CppCastExpression>(parent, position(), {});

    cast_expression->m_cast_type = consume(Token::Type::Keyword).text();

    consume(Token::Type::Less);
    cast_expression->m_type = parse_type(*cast_expression);
    consume(Token::Type::Greater);

    consume(Token::Type::LeftParen);
    cast_expression->m_expression = parse_expression(*cast_expression);
    consume(Token::Type::RightParen);

    cast_expression->set_end(position());

    return cast_expression;
}

}
