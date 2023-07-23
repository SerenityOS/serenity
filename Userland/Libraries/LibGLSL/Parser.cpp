/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023, Volodymyr V. <vvmposeydon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include "AST.h"
#include <LibGLSL/Lexer.h>

namespace GLSL {

Parser::Parser(Vector<Token> tokens, String const& filename)
    : m_filename(move(filename))
    , m_tokens(move(tokens))
{
}

ErrorOr<NonnullRefPtr<TranslationUnit>> Parser::parse()
{
    if (m_tokens.is_empty())
        return create_root_ast_node({}, {});
    auto unit = create_root_ast_node(m_tokens.first().start(), m_tokens.last().end());
    unit->set_declarations(TRY(parse_declarations_in_translation_unit(*unit)));
    return unit;
}

bool Parser::eof() const
{
    return m_state.token_index >= m_tokens.size();
}

void Parser::print_tokens() const
{
    for (auto& token : m_tokens) {
        outln("{}", token.to_string());
    }
}

ErrorOr<Optional<Parser::DeclarationType>> Parser::match_declaration_in_translation_unit()
{
    if (TRY(match_variable_declaration()))
        return DeclarationType::Variable;
    if (TRY(match_function_declaration()))
        return DeclarationType::Function;
    if (TRY(match_struct_declaration()))
        return DeclarationType::Struct;
    return Optional<DeclarationType>();
}

ErrorOr<bool> Parser::match_struct_declaration()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    if (!match_keyword("struct"sv))
        return false;
    TRY(consume(Token::Type::Keyword));

    if (!match(Token::Type::Identifier))
        return false;
    TRY(consume(Token::Type::Identifier));

    return match(Token::Type::LeftCurly);
}

ErrorOr<bool> Parser::match_function_declaration()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    if (!TRY(match_type()))
        return false;

    VERIFY(m_root_node);
    (void)parse_type(get_dummy_node());

    if (!TRY(match_name()))
        return false;

    (void)parse_name(get_dummy_node());

    if (!peek(Token::Type::LeftParen).has_value())
        return false;
    TRY(consume());

    while (TRY(consume()).type() != Token::Type::RightParen && !eof())
        ;

    if (peek(Token::Type::Semicolon).has_value() || peek(Token::Type::LeftCurly).has_value())
        return true;

    return false;
}

ErrorOr<bool> Parser::match_variable_declaration()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    if (!TRY(match_type())) {
        return false;
    }

    VERIFY(m_root_node);
    (void)parse_type(get_dummy_node());

    // Identifier
    if (!TRY(match_name()))
        return false;

    (void)TRY(parse_name(get_dummy_node()));

    while (!eof() && (peek().type() == Token::Type::LeftBracket)) {
        TRY(consume(Token::Type::LeftBracket));

        if (match(Token::Type::Integer)) {
            TRY(consume(Token::Type::Integer));
        }
        if (!match(Token::Type::RightBracket)) {
            TRY(error("No closing right bracket"sv));
            return false;
        }
        TRY(consume(Token::Type::RightBracket));
    }

    if (match(Token::Type::Equals)) {
        TRY(consume(Token::Type::Equals));
        if (!TRY(match_expression())) {
            TRY(error("initial value of variable is not an expression"sv));
            return false;
        }
        return true;
    }

    return match(Token::Type::Semicolon);
}

ErrorOr<bool> Parser::match_block_statement()
{
    return peek().type() == Token::Type::LeftCurly;
}

ErrorOr<bool> Parser::match_expression()
{
    return TRY(match_name())
        || match_unary_op()
        || match(Token::Type::LeftParen)
        || TRY(match_boolean_literal())
        || TRY(match_numeric_literal())
        || TRY(match_string_literal());
}

ErrorOr<bool> Parser::match_name()
{
    auto type = peek().type();
    return type == Token::Type::Identifier || type == Token::Type::KnownType;
}

ErrorOr<bool> Parser::match_string_literal()
{
    return match(Token::Type::DoubleQuotedString) || match(Token::Type::SingleQuotedString);
}

ErrorOr<bool> Parser::match_numeric_literal()
{
    return match(Token::Type::Float) || match(Token::Type::Integer);
}

ErrorOr<bool> Parser::match_boolean_literal()
{
    auto token = peek();
    if (token.type() != Token::Type::Keyword)
        return false;
    auto text = token.text();
    return text == "true" || text == "false";
}

ErrorOr<bool> Parser::match_type()
{
    save_state();
    ScopeGuard state_guard = [this] { load_state(); };

    if (match_storage_qualifier())
        TRY(consume_storage_qualifier());

    if (!TRY(match_name()))
        return false;

    return true;
}

ErrorOr<Vector<NonnullRefPtr<Declaration const>>> Parser::parse_declarations_in_translation_unit(ASTNode const& parent)
{
    Vector<NonnullRefPtr<Declaration const>> declarations;
    while (!eof()) {
        auto declaration = TRY(parse_single_declaration_in_translation_unit(parent));
        if (declaration) {
            declarations.append(declaration.release_nonnull());
        } else {
            TRY(error("unexpected token"sv));
            TRY(consume());
        }
    }
    return declarations;
}

ErrorOr<RefPtr<Declaration const>> Parser::parse_single_declaration_in_translation_unit(ASTNode const& parent)
{
    while (!eof()) {
        if (match_preprocessor()) {
            TRY(consume_preprocessor());
            continue;
        }

        auto declaration = TRY(match_declaration_in_translation_unit());
        if (declaration.has_value()) {
            return parse_declaration(parent, declaration.value());
        }
        return nullptr;
    }
    return nullptr;
}

ErrorOr<NonnullRefPtr<Declaration const>> Parser::parse_declaration(ASTNode const& parent, DeclarationType declaration_type)
{
    switch (declaration_type) {
    case DeclarationType::Function:
        return parse_function_declaration(parent);
    case DeclarationType::Variable:
        return parse_variable_declaration(parent);
    case DeclarationType::Struct:
        return parse_struct_declaration(parent);
    default:
        TRY(error("unexpected declaration type"sv));
        return create_ast_node<InvalidDeclaration>(parent, position(), position());
    }
}

ErrorOr<NonnullRefPtr<StructDeclaration const>> Parser::parse_struct_declaration(ASTNode const& parent)
{
    TRY(consume_keyword("struct"sv));

    auto decl = create_ast_node<StructDeclaration>(parent, position(), {});
    decl->set_name(TRY(parse_name(*decl)));

    TRY(consume(Token::Type::LeftCurly));
    while (!eof() && peek().type() != Token::Type::RightCurly) {
        decl->set_members(TRY(parse_struct_members(*decl)));
    }
    TRY(consume(Token::Type::RightCurly));

    TRY(consume(Token::Type::Semicolon));
    decl->set_end(position());

    return decl;
}

ErrorOr<Vector<NonnullRefPtr<Declaration const>>> Parser::parse_struct_members(StructDeclaration& parent)
{
    Vector<NonnullRefPtr<Declaration const>> members;
    while (!eof() && peek().type() != Token::Type::RightCurly) {
        auto member = TRY(parse_variable_declaration(parent));
        members.append(move(member));
    }
    return members;
}

ErrorOr<NonnullRefPtr<FunctionDeclaration const>> Parser::parse_function_declaration(ASTNode const& parent)
{
    auto func = create_ast_node<FunctionDeclaration>(parent, position(), {});

    func->set_return_type(TRY(parse_type(*func)));
    func->set_name(TRY(parse_name(*func)));

    TRY(consume(Token::Type::LeftParen));
    func->set_parameters(TRY(parse_parameter_list(*func)));
    TRY(consume(Token::Type::RightParen));

    RefPtr<FunctionDefinition const> body;
    Position func_end {};
    if (peek(Token::Type::LeftCurly).has_value()) {
        body = TRY(parse_function_definition(*func));
        func_end = body->end();
    } else {
        func_end = position();
        TRY(consume(Token::Type::Semicolon));
    }

    func->set_definition(move(body));
    func->set_end(func_end);
    return func;
}

ErrorOr<Vector<NonnullRefPtr<Parameter const>>> Parser::parse_parameter_list(ASTNode const& parent)
{
    Vector<NonnullRefPtr<Parameter const>> parameters;
    while (peek().type() != Token::Type::RightParen && !eof()) {
        auto type = TRY(parse_type(parent));

        RefPtr<Name const> name;
        if (TRY(match_name()))
            name = TRY(parse_name(parent));

        auto param = create_ast_node<Parameter>(parent, type->start(), !name.is_null() ? name->end() : type->end(), name);
        const_cast<Type&>(*type).set_parent(*param.ptr());

        param->set_type(move(type));
        parameters.append(move(param));

        if (peek(Token::Type::Comma).has_value())
            TRY(consume(Token::Type::Comma));
    }
    return parameters;
}

ErrorOr<NonnullRefPtr<FunctionDefinition const>> Parser::parse_function_definition(ASTNode const& parent)
{
    auto func = create_ast_node<FunctionDefinition>(parent, position(), {});

    TRY(consume(Token::Type::LeftCurly));
    while (!eof() && peek().type() != Token::Type::RightCurly) {
        func->add_statement(TRY(parse_statement(func)));
    }
    func->set_end(position());
    TRY(consume(Token::Type::RightCurly));

    return func;
}

ErrorOr<NonnullRefPtr<VariableDeclaration const>> Parser::parse_variable_declaration(ASTNode const& parent, bool expect_semicolon)
{
    auto var = create_ast_node<VariableDeclaration>(parent, position(), {});
    if (!TRY(match_variable_declaration())) {
        TRY(error("unexpected token for variable type"sv));
        var->set_end(position());
        return var;
    }
    var->set_type(TRY(parse_type(var)));
    auto name = TRY(parse_name(*var, true));
    RefPtr<Expression const> initial_value;

    if (match(Token::Type::Equals)) {
        TRY(consume(Token::Type::Equals));
        initial_value = TRY(parse_expression(var));
    }

    if (expect_semicolon)
        TRY(consume(Token::Type::Semicolon));

    var->set_end(position());
    var->set_name(name);
    var->set_initial_value(move(initial_value));

    return var;
}

ErrorOr<NonnullRefPtr<Statement const>> Parser::parse_statement(ASTNode const& parent)
{
    bool should_consume_semicolon = true;
    RefPtr<Statement const> result;

    if (TRY(match_block_statement())) {
        should_consume_semicolon = false;
        result = TRY(parse_block_statement(parent));
    } else if (TRY(match_variable_declaration())) {
        result = TRY(parse_variable_declaration(parent, false));
    } else if (TRY(match_expression())) {
        result = TRY(parse_expression(parent));
    } else if (match_keyword("return"sv)) {
        result = TRY(parse_return_statement(parent));
    } else if (match_keyword("discard"sv)) {
        auto start = position();
        TRY(consume());
        result = create_ast_node<DiscardStatement>(parent, start, position());
    } else if (match_keyword("for"sv)) {
        should_consume_semicolon = false;
        result = TRY(parse_for_statement(parent));
    } else if (match_keyword("if"sv)) {
        should_consume_semicolon = false;
        result = TRY(parse_if_statement(parent));
    } else {
        TRY(error("unexpected statement type"sv));
        should_consume_semicolon = false;
        TRY(consume());
        return create_ast_node<InvalidStatement>(parent, position(), position());
    }

    if (should_consume_semicolon)
        TRY(consume(Token::Type::Semicolon));
    return result.release_nonnull();
}

ErrorOr<NonnullRefPtr<BlockStatement const>> Parser::parse_block_statement(ASTNode const& parent)
{
    auto block_statement = create_ast_node<BlockStatement>(parent, position(), {});

    TRY(consume(Token::Type::LeftCurly));
    while (!eof() && peek().type() != Token::Type::RightCurly) {
        block_statement->add_statement(TRY(parse_statement(*block_statement)));
    }
    TRY(consume(Token::Type::RightCurly));

    block_statement->set_end(position());
    return block_statement;
}

ErrorOr<NonnullRefPtr<IfStatement const>> Parser::parse_if_statement(ASTNode const& parent)
{
    auto if_statement = create_ast_node<IfStatement>(parent, position(), {});
    TRY(consume_keyword("if"sv));
    TRY(consume(Token::Type::LeftParen));
    if_statement->set_predicate(TRY(parse_expression(*if_statement)));
    TRY(consume(Token::Type::RightParen));
    if_statement->set_then_statement(TRY(parse_statement(*if_statement)));
    if (match_keyword("else"sv)) {
        TRY(consume(Token::Type::Keyword));
        if_statement->set_else_statement(TRY(parse_statement(*if_statement)));
        if_statement->set_end(if_statement->else_statement()->end());
    } else {
        if_statement->set_end(if_statement->then_statement()->end());
    }
    return if_statement;
}

ErrorOr<NonnullRefPtr<ForStatement const>> Parser::parse_for_statement(ASTNode const& parent)
{
    auto for_statement = create_ast_node<ForStatement>(parent, position(), {});
    TRY(consume_keyword("for"sv));
    TRY(consume(Token::Type::LeftParen));
    if (peek().type() != Token::Type::Semicolon)
        for_statement->set_init(TRY(parse_variable_declaration(*for_statement, false)));
    TRY(consume(Token::Type::Semicolon));

    if (peek().type() != Token::Type::Semicolon)
        for_statement->set_test(TRY(parse_expression(*for_statement)));
    TRY(consume(Token::Type::Semicolon));

    if (peek().type() != Token::Type::RightParen)
        for_statement->set_update(TRY(parse_expression(*for_statement)));
    TRY(consume(Token::Type::RightParen));

    for_statement->set_body(TRY(parse_statement(*for_statement)));

    for_statement->set_end(for_statement->body()->end());
    return for_statement;
}

ErrorOr<NonnullRefPtr<ReturnStatement const>> Parser::parse_return_statement(ASTNode const& parent)
{
    auto return_statement = create_ast_node<ReturnStatement>(parent, position(), {});
    TRY(consume_keyword("return"sv));
    if (!peek(Token::Type::Semicolon).has_value()) {
        return_statement->set_value(TRY(parse_expression(*return_statement)));
    }
    return_statement->set_end(position());
    return return_statement;
}

HashMap<BinaryOp, int> s_operator_precedence = {
    { BinaryOp::Assignment, 1 },
    { BinaryOp::AdditionAssignment, 1 },
    { BinaryOp::SubtractionAssignment, 1 },
    { BinaryOp::MultiplicationAssignment, 1 },
    { BinaryOp::DivisionAssignment, 1 },
    { BinaryOp::ModuloAssignment, 1 },
    { BinaryOp::AndAssignment, 1 },
    { BinaryOp::XorAssignment, 1 },
    { BinaryOp::OrAssignment, 1 },
    { BinaryOp::LeftShiftAssignment, 1 },
    { BinaryOp::RightShiftAssignment, 1 },
    { BinaryOp::LogicalOr, 2 },
    { BinaryOp::LogicalXor, 3 },
    { BinaryOp::LogicalAnd, 4 },
    { BinaryOp::BitwiseOr, 5 },
    { BinaryOp::BitwiseXor, 6 },
    { BinaryOp::BitwiseAnd, 7 },
    { BinaryOp::EqualsEquals, 8 },
    { BinaryOp::NotEqual, 8 },
    { BinaryOp::LessThan, 9 },
    { BinaryOp::LessThanEquals, 9 },
    { BinaryOp::GreaterThan, 9 },
    { BinaryOp::GreaterThanEquals, 9 },
    { BinaryOp::LeftShift, 10 },
    { BinaryOp::RightShift, 10 },
    { BinaryOp::Addition, 11 },
    { BinaryOp::Subtraction, 11 },
    { BinaryOp::Multiplication, 12 },
    { BinaryOp::Division, 12 },
    { BinaryOp::Modulo, 12 },
};

HashMap<BinaryOp, Parser::Associativity> Parser::s_operator_associativity = {
    { BinaryOp::Assignment, Associativity::RightToLeft },
    { BinaryOp::AdditionAssignment, Associativity::RightToLeft },
    { BinaryOp::SubtractionAssignment, Associativity::RightToLeft },
    { BinaryOp::MultiplicationAssignment, Associativity::RightToLeft },
    { BinaryOp::DivisionAssignment, Associativity::RightToLeft },
    { BinaryOp::ModuloAssignment, Associativity::RightToLeft },
    { BinaryOp::AndAssignment, Associativity::RightToLeft },
    { BinaryOp::XorAssignment, Associativity::RightToLeft },
    { BinaryOp::OrAssignment, Associativity::RightToLeft },
    { BinaryOp::LeftShiftAssignment, Associativity::RightToLeft },
    { BinaryOp::RightShiftAssignment, Associativity::RightToLeft },
    { BinaryOp::LogicalOr, Associativity::LeftToRight },
    { BinaryOp::LogicalXor, Associativity::LeftToRight },
    { BinaryOp::LogicalAnd, Associativity::LeftToRight },
    { BinaryOp::BitwiseOr, Associativity::LeftToRight },
    { BinaryOp::BitwiseXor, Associativity::LeftToRight },
    { BinaryOp::BitwiseAnd, Associativity::LeftToRight },
    { BinaryOp::EqualsEquals, Associativity::LeftToRight },
    { BinaryOp::NotEqual, Associativity::LeftToRight },
    { BinaryOp::LessThan, Associativity::LeftToRight },
    { BinaryOp::LessThanEquals, Associativity::LeftToRight },
    { BinaryOp::GreaterThan, Associativity::LeftToRight },
    { BinaryOp::GreaterThanEquals, Associativity::LeftToRight },
    { BinaryOp::LeftShift, Associativity::LeftToRight },
    { BinaryOp::RightShift, Associativity::LeftToRight },
    { BinaryOp::Addition, Associativity::LeftToRight },
    { BinaryOp::Subtraction, Associativity::LeftToRight },
    { BinaryOp::Multiplication, Associativity::LeftToRight },
    { BinaryOp::Division, Associativity::LeftToRight },
    { BinaryOp::Modulo, Associativity::LeftToRight },
};

ErrorOr<NonnullRefPtr<Expression const>> Parser::parse_expression(ASTNode const& parent, int min_precedence, Associativity associativity)
{
    auto start_pos = position();

    auto lhs = TRY(parse_unary_expression(get_dummy_node()));

    while (match_binary_op()) {
        auto op = TRY(peek_binary_op());
        auto maybe_op_precedence = s_operator_precedence.get(op);
        VERIFY(maybe_op_precedence.has_value());

        auto op_precedence = maybe_op_precedence.value();
        if (op_precedence < min_precedence || (op_precedence == min_precedence && associativity == Associativity::LeftToRight))
            break;
        TRY(consume());

        auto maybe_op_associativity = s_operator_associativity.get(op);
        VERIFY(maybe_op_associativity.has_value());
        auto op_associativity = maybe_op_associativity.value();

        auto expr = create_ast_node<BinaryExpression>(parent, start_pos, {});
        const_cast<Expression&>(*lhs).set_parent(expr);
        expr->set_lhs(move(lhs));
        expr->set_op(op);
        expr->set_rhs(TRY(parse_expression(expr, op_precedence, op_associativity)));

        expr->set_end(position());

        lhs = move(expr);
    }

    return lhs;
}

// NOTE: this function should parse everything with precedence of prefix increment and above, e.g. ++/--/!/~, function call, member expressions and expressions in parentheses
ErrorOr<NonnullRefPtr<Expression const>> Parser::parse_unary_expression(const GLSL::ASTNode& parent)
{
    if (match(Token::Type::LeftParen)) {
        TRY(consume(Token::Type::LeftParen));
        auto expr = TRY(parse_expression(parent));
        TRY(consume(Token::Type::RightParen));

        return expr;
    }

    if (TRY(match_boolean_literal()))
        return parse_boolean_literal(parent);

    if (TRY(match_numeric_literal()))
        return parse_numeric_literal(parent);

    if (TRY(match_string_literal()))
        return parse_string_literal(parent);

    if (TRY(match_name())) {
        NonnullRefPtr<Expression const> lhs = TRY(parse_name(parent));

        while (true) {
            if (match(Token::Type::LeftParen)) {
                TRY(consume(Token::Type::LeftParen));
                auto expr = create_ast_node<FunctionCall>(parent, lhs->start(), {});
                auto args = TRY(parse_function_call_args(expr));

                const_cast<Expression&>(*lhs).set_parent(expr);
                expr->set_callee(move(lhs));
                expr->set_arguments(move(args));

                TRY(consume(Token::Type::RightParen));
                expr->set_end(position());

                lhs = move(expr);
            } else if (match(Token::Type::Dot)) {
                TRY(consume(Token::Type::Dot));

                auto expr = create_ast_node<MemberExpression>(parent, lhs->start(), {});
                auto rhs = TRY(parse_name(expr));

                const_cast<Expression&>(*lhs).set_parent(expr);
                expr->set_object(move(lhs));
                expr->set_property(move(rhs));
                expr->set_end(position());

                lhs = move(expr);
            } else if (match(Token::Type::LeftBracket)) {
                TRY(consume(Token::Type::LeftBracket));

                auto expr = create_ast_node<ArrayElementExpression>(parent, lhs->start(), {});
                auto index = TRY(parse_expression(expr));

                TRY(consume(Token::Type::RightBracket));

                const_cast<Expression&>(*lhs).set_parent(expr);
                expr->set_array(move(lhs));
                expr->set_index(move(index));
                expr->set_end(position());

                lhs = move(expr);
            } else if (match(Token::Type::PlusPlus) || match(Token::Type::MinusMinus)) {
                auto op = TRY(consume_unary_op());

                auto expr = create_ast_node<UnaryExpression>(parent, lhs->start(), position());

                const_cast<Expression&>(*lhs).set_parent(expr);
                expr->set_lhs(move(lhs));
                expr->set_op(op);
                expr->set_is_postfix(true);

                lhs = move(expr);
            } else {
                break;
            }
        }

        return lhs;
    }

    if (match_unary_op()) {
        auto expr = create_ast_node<UnaryExpression>(parent, position(), {});
        auto op = TRY(consume_unary_op());

        auto lhs = TRY(parse_unary_expression(expr));
        expr->set_lhs(move(lhs));
        expr->set_op(op);
        expr->set_end(position());

        return expr;
    }

    TRY(error(TRY(String::formatted("unable to parse unary expression starting with {}", TRY(peek().type_as_string())))));
    return create_ast_node<InvalidExpression>(parent, position(), position());
}

ErrorOr<Vector<NonnullRefPtr<Expression const>>> Parser::parse_function_call_args(ASTNode const& parent)
{
    Vector<NonnullRefPtr<Expression const>> result;
    while (!match(Token::Type::RightParen)) {
        auto arg = TRY(parse_expression(parent));
        result.append(move(arg));

        if (!match(Token::Type::RightParen))
            TRY(consume(Token::Type::Comma));
    }
    return result;
}

ErrorOr<NonnullRefPtr<Expression const>> Parser::parse_boolean_literal(ASTNode const& parent)
{
    auto token = TRY(consume(Token::Type::Keyword));
    auto text = token.text();
    bool value = (text == "true");
    return create_ast_node<BooleanLiteral>(parent, token.start(), token.end(), value);
}

ErrorOr<NonnullRefPtr<Expression const>> Parser::parse_numeric_literal(GLSL::ASTNode const& parent)
{
    auto token = TRY(consume());
    auto text = token.text();
    return create_ast_node<NumericLiteral>(parent, token.start(), token.end(), text);
}

ErrorOr<NonnullRefPtr<Expression const>> Parser::parse_string_literal(ASTNode const& parent)
{
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
        TRY(consume());
    }

    // String was not terminated
    if (!end_token_index.has_value()) {
        end_token_index = m_tokens.size() - 1;
    }

    VERIFY(start_token_index.has_value());
    VERIFY(end_token_index.has_value());

    Token start_token = m_tokens[start_token_index.value()];
    Token end_token = m_tokens[end_token_index.value()];

    auto text = TRY(text_in_range(start_token.start(), end_token.end()));
    auto string_literal = create_ast_node<StringLiteral>(parent, start_token.start(), end_token.end());
    string_literal->set_value(move(text));
    return string_literal;
}

ErrorOr<NonnullRefPtr<Name const>> Parser::parse_name(ASTNode const& parent, bool allow_sized_name)
{
    NonnullRefPtr<Name> name_node = create_ast_node<Name>(parent, position(), {});

    if (peek().type() == Token::Type::Identifier || peek().type() == Token::Type::KnownType) {
        auto token = TRY(consume());
        name_node->set_name(token.text());
        name_node->set_end(position());
    } else {
        TRY(error("expected keyword or identifier while trying to parse name"sv));
        name_node->set_end(position());
        return name_node;
    }

    if (peek().type() == Token::Type::LeftBracket && allow_sized_name) {
        NonnullRefPtr<SizedName> sized_name = create_ast_node<SizedName>(parent, name_node->start(), {});
        sized_name->set_name(name_node->name());

        while (peek().type() == Token::Type::LeftBracket) {
            TRY(consume(Token::Type::LeftBracket));

            StringView size = "0"sv;
            if (peek().type() == Token::Type::Integer)
                size = TRY(consume(Token::Type::Integer)).text();
            sized_name->append_dimension(size);

            TRY(consume(Token::Type::RightBracket));
        }
        name_node->set_end(position());
        name_node = sized_name;
    }

    name_node->set_end(previous_token_end());
    return name_node;
}

ErrorOr<NonnullRefPtr<Type const>> Parser::parse_type(ASTNode const& parent)
{
    auto type = create_ast_node<Type>(parent, position(), {});

    Vector<StorageTypeQualifier> storage_qualifiers;
    while (match_storage_qualifier()) {
        storage_qualifiers.append(TRY(consume_storage_qualifier()));
    }
    type->set_storage_qualifiers(move(storage_qualifiers));

    if (match_keyword("struct"sv)) {
        TRY(consume(Token::Type::Keyword)); // Consume struct prefix
    }

    if (!TRY(match_name())) {
        type->set_end(position());
        TRY(error(TRY(String::formatted("expected name instead of: {}", peek().text()))));
        return type;
    }
    type->set_name(TRY(parse_name(*type)));

    type->set_end(previous_token_end());

    return type;
}

bool Parser::match_unary_op()
{
    return match(Token::Type::Plus)
        || match(Token::Type::Minus)
        || match(Token::Type::PlusPlus)
        || match(Token::Type::MinusMinus)
        || match(Token::Type::ExclamationMark)
        || match(Token::Type::Tilde);
}

ErrorOr<UnaryOp> Parser::consume_unary_op()
{
    switch (TRY(consume()).type()) {
    case Token::Type::Plus:
        return UnaryOp::Plus;
    case Token::Type::Minus:
        return UnaryOp::Minus;
    case Token::Type::PlusPlus:
        return UnaryOp::PlusPlus;
    case Token::Type::MinusMinus:
        return UnaryOp::MinusMinus;
    case Token::Type::ExclamationMark:
        return UnaryOp::Not;
    case Token::Type::Tilde:
        return UnaryOp::BitwiseNot;
    default:
        VERIFY_NOT_REACHED();
    }
}

bool Parser::match_binary_op()
{
    return match(Token::Type::Plus)
        || match(Token::Type::Minus)
        || match(Token::Type::Asterisk)
        || match(Token::Type::Slash)
        || match(Token::Type::Percent)
        || match(Token::Type::And)
        || match(Token::Type::Pipe)
        || match(Token::Type::Caret)
        || match(Token::Type::AndAnd)
        || match(Token::Type::PipePipe)
        || match(Token::Type::CaretCaret)
        || match(Token::Type::LessLess)
        || match(Token::Type::GreaterGreater)
        || match(Token::Type::Less)
        || match(Token::Type::LessEquals)
        || match(Token::Type::Greater)
        || match(Token::Type::GreaterEquals)
        || match(Token::Type::EqualsEquals)
        || match(Token::Type::ExclamationMarkEquals)
        || match(Token::Type::Equals)
        || match(Token::Type::PlusEquals)
        || match(Token::Type::MinusEquals)
        || match(Token::Type::AsteriskEquals)
        || match(Token::Type::SlashEquals)
        || match(Token::Type::PercentEquals)
        || match(Token::Type::LessLessEquals)
        || match(Token::Type::GreaterGreaterEquals)
        || match(Token::Type::AndEquals)
        || match(Token::Type::PipeEquals)
        || match(Token::Type::CaretEquals);
}

ErrorOr<BinaryOp> Parser::peek_binary_op()
{
    switch (peek().type()) {
    case Token::Type::Plus:
        return BinaryOp::Addition;
    case Token::Type::Minus:
        return BinaryOp::Subtraction;
    case Token::Type::Asterisk:
        return BinaryOp::Multiplication;
    case Token::Type::Slash:
        return BinaryOp::Division;
    case Token::Type::Percent:
        return BinaryOp::Modulo;
    case Token::Type::And:
        return BinaryOp::BitwiseAnd;
    case Token::Type::Pipe:
        return BinaryOp::BitwiseOr;
    case Token::Type::Caret:
        return BinaryOp::BitwiseXor;
    case Token::Type::AndAnd:
        return BinaryOp::LogicalAnd;
    case Token::Type::PipePipe:
        return BinaryOp::LogicalOr;
    case Token::Type::CaretCaret:
        return BinaryOp::LogicalXor;
    case Token::Type::LessLess:
        return BinaryOp::LeftShift;
    case Token::Type::GreaterGreater:
        return BinaryOp::RightShift;
    case Token::Type::Less:
        return BinaryOp::LessThan;
    case Token::Type::LessEquals:
        return BinaryOp::LessThanEquals;
    case Token::Type::Greater:
        return BinaryOp::GreaterThan;
    case Token::Type::GreaterEquals:
        return BinaryOp::GreaterThanEquals;
    case Token::Type::EqualsEquals:
        return BinaryOp::EqualsEquals;
    case Token::Type::ExclamationMarkEquals:
        return BinaryOp::NotEqual;
    case Token::Type::Equals:
        return BinaryOp::Assignment;
    case Token::Type::PlusEquals:
        return BinaryOp::AdditionAssignment;
    case Token::Type::MinusEquals:
        return BinaryOp::SubtractionAssignment;
    case Token::Type::AsteriskEquals:
        return BinaryOp::MultiplicationAssignment;
    case Token::Type::SlashEquals:
        return BinaryOp::DivisionAssignment;
    case Token::Type::PercentEquals:
        return BinaryOp::ModuloAssignment;
    case Token::Type::LessLessEquals:
        return BinaryOp::LeftShiftAssignment;
    case Token::Type::GreaterGreaterEquals:
        return BinaryOp::RightShiftAssignment;
    case Token::Type::AndEquals:
        return BinaryOp::AndAssignment;
    case Token::Type::PipeEquals:
        return BinaryOp::OrAssignment;
    case Token::Type::CaretEquals:
        return BinaryOp::XorAssignment;
    default:
        VERIFY_NOT_REACHED();
    }
}

bool Parser::match_storage_qualifier()
{
    return match_keyword("const"sv)
        || match_keyword("in"sv)
        || match_keyword("out"sv)
        || match_keyword("inout"sv)
        || match_keyword("centroid"sv)
        || match_keyword("patch"sv)
        || match_keyword("sample"sv)
        || match_keyword("uniform"sv)
        || match_keyword("buffer"sv)
        || match_keyword("shared"sv)
        || match_keyword("coherent"sv)
        || match_keyword("volatile"sv)
        || match_keyword("restrict"sv)
        || match_keyword("readonly"sv)
        || match_keyword("writeonly"sv)
        || match_keyword("subroutine"sv);
}

ErrorOr<StorageTypeQualifier> Parser::consume_storage_qualifier()
{
    VERIFY(peek().type() == Token::Type::Keyword);
    auto keyword = MUST(consume()).text();
    if (keyword == "buffer")
        return StorageTypeQualifier::Buffer;
    if (keyword == "centroid")
        return StorageTypeQualifier::Centroid;
    if (keyword == "coherent")
        return StorageTypeQualifier::Coherent;
    if (keyword == "const")
        return StorageTypeQualifier::Const;
    if (keyword == "in")
        return StorageTypeQualifier::In;
    if (keyword == "inout")
        return StorageTypeQualifier::Inout;
    if (keyword == "out")
        return StorageTypeQualifier::Out;
    if (keyword == "patch")
        return StorageTypeQualifier::Patch;
    if (keyword == "readonly")
        return StorageTypeQualifier::Readonly;
    if (keyword == "restrict")
        return StorageTypeQualifier::Restrict;
    if (keyword == "sample")
        return StorageTypeQualifier::Sample;
    if (keyword == "shared")
        return StorageTypeQualifier::Shared;
    if (keyword == "subroutine")
        return StorageTypeQualifier::Subroutine;
    if (keyword == "uniform")
        return StorageTypeQualifier::Uniform;
    if (keyword == "volatile")
        return StorageTypeQualifier::Volatile;
    if (keyword == "writeonly")
        return StorageTypeQualifier::Writeonly;
    VERIFY_NOT_REACHED();
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

bool Parser::match(Token::Type type)
{
    return peek().type() == type;
}

bool Parser::match_keyword(StringView keyword)
{
    auto token = peek();
    if (token.type() != Token::Type::Keyword) {
        return false;
    }
    if (token.text() != keyword) {
        return false;
    }
    return true;
}

bool Parser::match_preprocessor()
{
    return match(Token::Type::PreprocessorStatement) || match(Token::Type::IncludeStatement);
}

ErrorOr<Token> Parser::consume()
{
    if (eof()) {
        TRY(error("GLSL Parser: out of tokens"sv));
        return Token { Token::Type::EOF_TOKEN, position(), position(), {} };
    }
    return m_tokens[m_state.token_index++];
}

ErrorOr<Token> Parser::consume(Token::Type type)
{
    auto token = TRY(consume());
    if (token.type() != type)
        TRY(error(TRY(String::formatted("expected {} at {}:{}, found: {}", Token::type_to_string(type), token.start().line, token.start().column, Token::type_to_string(token.type())))));
    return token;
}

ErrorOr<Token> Parser::consume_keyword(StringView keyword)
{
    auto token = TRY(consume());
    if (token.type() != Token::Type::Keyword) {
        TRY(error(TRY(String::formatted("unexpected token: {}, expected Keyword", TRY(token.to_string())))));
        return token;
    }
    if (token.text() != keyword) {
        TRY(error(TRY(String::formatted("unexpected keyword: {}, expected {}", token.text(), keyword))));
        return token;
    }
    return token;
}

ErrorOr<void> Parser::consume_preprocessor()
{
    switch (peek().type()) {
    case Token::Type::PreprocessorStatement:
        TRY(consume());
        break;
    case Token::Type::IncludeStatement:
        TRY(consume());
        TRY(consume(Token::Type::IncludePath));
        break;
    default:
        TRY(error("unexpected token while parsing preprocessor statement"sv));
        TRY(consume());
    }
    return {};
}

Position Parser::position() const
{
    if (m_tokens.is_empty())
        return {};

    if (eof())
        return m_tokens.last().end();

    return peek().start();
}

Position Parser::previous_token_end() const
{
    if (m_state.token_index < 1)
        return {};
    return m_tokens[m_state.token_index - 1].end();
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

Vector<Token> Parser::tokens_in_range(Position start, Position end) const
{
    auto start_token_index = index_of_token_at(start);
    auto end_node_index = index_of_token_at(end);
    VERIFY(start_token_index.has_value());
    VERIFY(end_node_index.has_value());

    Vector<Token> tokens;
    for (size_t i = start_token_index.value(); i <= end_node_index.value(); ++i) {
        tokens.append(m_tokens[i]);
    }
    return tokens;
}

ErrorOr<String> Parser::text_in_range(Position start, Position end) const
{
    StringBuilder builder;
    for (auto token : tokens_in_range(start, end)) {
        builder.append(token.text());
    }
    return builder.to_string();
}

ErrorOr<void> Parser::error(StringView message)
{
    if (!m_saved_states.is_empty())
        return {};

    if (message.is_null() || message.is_empty())
        message = "<empty>"sv;
    String formatted_message;
    if (m_state.token_index >= m_tokens.size()) {
        formatted_message = TRY(String::formatted("GLSL Parsed error on EOF.{}", message));
    } else {
        formatted_message = TRY(String::formatted("GLSL Parser error: {}. token: {} ({}:{})",
            message,
            m_state.token_index < m_tokens.size() ? m_tokens[m_state.token_index].text() : "EOF"sv,
            m_tokens[m_state.token_index].start().line,
            m_tokens[m_state.token_index].start().column));
    }

    m_errors.append(formatted_message);
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

}
