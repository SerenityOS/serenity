/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>

namespace JS {

class ScopePusher {
public:
    enum Type {
        Var = 1,
        Let = 2,
        Function = 3,
    };

    ScopePusher(Parser& parser, unsigned mask)
        : m_parser(parser)
        , m_mask(mask)
    {
        if (m_mask & Var)
            m_parser.m_parser_state.m_var_scopes.append(NonnullRefPtrVector<VariableDeclaration>());
        if (m_mask & Let)
            m_parser.m_parser_state.m_let_scopes.append(NonnullRefPtrVector<VariableDeclaration>());
        if (m_mask & Function)
            m_parser.m_parser_state.m_function_scopes.append(NonnullRefPtrVector<FunctionDeclaration>());
    }

    ~ScopePusher()
    {
        if (m_mask & Var)
            m_parser.m_parser_state.m_var_scopes.take_last();
        if (m_mask & Let)
            m_parser.m_parser_state.m_let_scopes.take_last();
        if (m_mask & Function)
            m_parser.m_parser_state.m_function_scopes.take_last();
    }

    Parser& m_parser;
    unsigned m_mask { 0 };
};

class OperatorPrecedenceTable {
public:
    constexpr OperatorPrecedenceTable()
        : m_token_precedence()
    {
        for (size_t i = 0; i < array_size(m_operator_precedence); ++i) {
            auto& op = m_operator_precedence[i];
            m_token_precedence[static_cast<size_t>(op.token)] = op.precedence;
        }
    }

    constexpr int get(TokenType token) const
    {
        int p = m_token_precedence[static_cast<size_t>(token)];
        if (p == 0) {
            fprintf(stderr, "Internal Error: No precedence for operator %s\n", Token::name(token));
            ASSERT_NOT_REACHED();
            return -1;
        }

        return p;
    }

private:
    int m_token_precedence[cs_num_of_js_tokens];

    struct OperatorPrecedence {
        TokenType token;
        int precedence;
    };

    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Operator_Precedence
    static constexpr const OperatorPrecedence m_operator_precedence[] = {
        { TokenType::Period, 20 },
        { TokenType::BracketOpen, 20 },
        { TokenType::ParenOpen, 20 },
        { TokenType::QuestionMarkPeriod, 20 },

        { TokenType::New, 19 },

        { TokenType::PlusPlus, 18 },
        { TokenType::MinusMinus, 18 },

        { TokenType::ExclamationMark, 17 },
        { TokenType::Tilde, 17 },
        { TokenType::Typeof, 17 },
        { TokenType::Void, 17 },
        { TokenType::Delete, 17 },
        { TokenType::Await, 17 },

        { TokenType::DoubleAsterisk, 16 },

        { TokenType::Asterisk, 15 },
        { TokenType::Slash, 15 },
        { TokenType::Percent, 15 },

        { TokenType::Plus, 14 },
        { TokenType::Minus, 14 },

        { TokenType::ShiftLeft, 13 },
        { TokenType::ShiftRight, 13 },
        { TokenType::UnsignedShiftRight, 13 },

        { TokenType::LessThan, 12 },
        { TokenType::LessThanEquals, 12 },
        { TokenType::GreaterThan, 12 },
        { TokenType::GreaterThanEquals, 12 },
        { TokenType::In, 12 },
        { TokenType::Instanceof, 12 },

        { TokenType::EqualsEquals, 11 },
        { TokenType::ExclamationMarkEquals, 11 },
        { TokenType::EqualsEqualsEquals, 11 },
        { TokenType::ExclamationMarkEqualsEquals, 11 },

        { TokenType::Ampersand, 10 },

        { TokenType::Caret, 9 },

        { TokenType::Pipe, 8 },

        { TokenType::DoubleQuestionMark, 7 },

        { TokenType::DoubleAmpersand, 6 },

        { TokenType::DoublePipe, 5 },

        { TokenType::QuestionMark, 4 },

        { TokenType::Equals, 3 },
        { TokenType::PlusEquals, 3 },
        { TokenType::MinusEquals, 3 },
        { TokenType::DoubleAsteriskEquals, 3 },
        { TokenType::AsteriskEquals, 3 },
        { TokenType::SlashEquals, 3 },
        { TokenType::PercentEquals, 3 },
        { TokenType::ShiftLeftEquals, 3 },
        { TokenType::ShiftRightEquals, 3 },
        { TokenType::UnsignedShiftRightEquals, 3 },
        { TokenType::AmpersandEquals, 3 },
        { TokenType::CaretEquals, 3 },
        { TokenType::PipeEquals, 3 },
        { TokenType::DoubleAmpersandEquals, 3 },
        { TokenType::DoublePipeEquals, 3 },
        { TokenType::DoubleQuestionMarkEquals, 3 },

        { TokenType::Yield, 2 },

        { TokenType::Comma, 1 },
    };
};

constexpr OperatorPrecedenceTable g_operator_precedence;

Parser::ParserState::ParserState(Lexer lexer)
    : m_lexer(move(lexer))
    , m_current_token(m_lexer.next())
{
}

Parser::Parser(Lexer lexer)
    : m_parser_state(move(lexer))
{
}

Associativity Parser::operator_associativity(TokenType type) const
{
    switch (type) {
    case TokenType::Period:
    case TokenType::BracketOpen:
    case TokenType::ParenOpen:
    case TokenType::QuestionMarkPeriod:
    case TokenType::Asterisk:
    case TokenType::Slash:
    case TokenType::Percent:
    case TokenType::Plus:
    case TokenType::Minus:
    case TokenType::ShiftLeft:
    case TokenType::ShiftRight:
    case TokenType::UnsignedShiftRight:
    case TokenType::LessThan:
    case TokenType::LessThanEquals:
    case TokenType::GreaterThan:
    case TokenType::GreaterThanEquals:
    case TokenType::In:
    case TokenType::Instanceof:
    case TokenType::EqualsEquals:
    case TokenType::ExclamationMarkEquals:
    case TokenType::EqualsEqualsEquals:
    case TokenType::ExclamationMarkEqualsEquals:
    case TokenType::Typeof:
    case TokenType::Void:
    case TokenType::Delete:
    case TokenType::Ampersand:
    case TokenType::Caret:
    case TokenType::Pipe:
    case TokenType::DoubleQuestionMark:
    case TokenType::DoubleAmpersand:
    case TokenType::DoublePipe:
    case TokenType::Comma:
        return Associativity::Left;
    default:
        return Associativity::Right;
    }
}

NonnullRefPtr<Program> Parser::parse_program()
{
    ScopePusher scope(*this, ScopePusher::Var | ScopePusher::Let | ScopePusher::Function);
    auto program = adopt(*new Program);

    bool first = true;
    m_parser_state.m_use_strict_directive = UseStrictDirectiveState::Looking;
    while (!done()) {
        if (match_statement()) {
            program->append(parse_statement());
            if (first) {
                if (m_parser_state.m_use_strict_directive == UseStrictDirectiveState::Found) {
                    program->set_strict_mode();
                    m_parser_state.m_strict_mode = true;
                }
                first = false;
                m_parser_state.m_use_strict_directive = UseStrictDirectiveState::None;
            }
        } else {
            expected("statement");
            consume();
        }
    }
    if (m_parser_state.m_var_scopes.size() == 1) {
        program->add_variables(m_parser_state.m_var_scopes.last());
        program->add_variables(m_parser_state.m_let_scopes.last());
        program->add_functions(m_parser_state.m_function_scopes.last());
    } else {
        syntax_error("Unclosed scope");
    }
    return program;
}

NonnullRefPtr<Statement> Parser::parse_statement()
{
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Class:
        return parse_class_declaration();
    case TokenType::Function: {
        auto declaration = parse_function_node<FunctionDeclaration>();
        m_parser_state.m_function_scopes.last().append(declaration);
        return declaration;
    }
    case TokenType::CurlyOpen:
        return parse_block_statement();
    case TokenType::Return:
        return parse_return_statement();
    case TokenType::Var:
    case TokenType::Let:
    case TokenType::Const:
        return parse_variable_declaration();
    case TokenType::For:
        return parse_for_statement();
    case TokenType::If:
        return parse_if_statement();
    case TokenType::Throw:
        return parse_throw_statement();
    case TokenType::Try:
        return parse_try_statement();
    case TokenType::Break:
        return parse_break_statement();
    case TokenType::Continue:
        return parse_continue_statement();
    case TokenType::Switch:
        return parse_switch_statement();
    case TokenType::Do:
        return parse_do_while_statement();
    case TokenType::While:
        return parse_while_statement();
    case TokenType::Debugger:
        return parse_debugger_statement();
    case TokenType::Semicolon:
        consume();
        return create_ast_node<EmptyStatement>();
    default:
        if (match(TokenType::Identifier)) {
            auto result = try_parse_labelled_statement();
            if (!result.is_null())
                return result.release_nonnull();
        }
        if (match_expression()) {
            auto expr = parse_expression(0);
            consume_or_insert_semicolon();
            return create_ast_node<ExpressionStatement>(move(expr));
        }
        expected("statement (missing switch case)");
        consume();
        return create_ast_node<ErrorStatement>();
    }
}

RefPtr<FunctionExpression> Parser::try_parse_arrow_function_expression(bool expect_parens)
{
    save_state();
    m_parser_state.m_var_scopes.append(NonnullRefPtrVector<VariableDeclaration>());

    ArmedScopeGuard state_rollback_guard = [&] {
        m_parser_state.m_var_scopes.take_last();
        load_state();
    };

    Vector<FunctionNode::Parameter> parameters;
    bool parse_failed = false;
    bool has_rest_parameter = false;
    i32 function_length = -1;
    while (true) {
        if (match(TokenType::Comma)) {
            if (has_rest_parameter || !expect_parens) {
                parse_failed = true;
                break;
            }
            consume(TokenType::Comma);
        } else if (match(TokenType::Identifier)) {
            auto parameter_name = consume(TokenType::Identifier).value();
            RefPtr<Expression> default_value;
            if (expect_parens && match(TokenType::Equals)) {
                consume(TokenType::Equals);
                function_length = parameters.size();
                default_value = parse_expression(2);
            }
            parameters.append({ parameter_name, default_value });
        } else if (match(TokenType::TripleDot)) {
            consume();
            if (has_rest_parameter) {
                parse_failed = true;
                break;
            }
            has_rest_parameter = true;
            function_length = parameters.size();
            auto parameter_name = consume(TokenType::Identifier).value();
            parameters.append({ parameter_name, nullptr, true });
        } else if (match(TokenType::ParenClose)) {
            if (expect_parens) {
                consume(TokenType::ParenClose);
                if (match(TokenType::Arrow)) {
                    consume(TokenType::Arrow);
                } else {
                    parse_failed = true;
                }
                break;
            }
            parse_failed = true;
            break;
        } else if (match(TokenType::Arrow)) {
            if (!expect_parens) {
                consume(TokenType::Arrow);
                break;
            }
            parse_failed = true;
            break;
        } else {
            parse_failed = true;
            break;
        }
    }

    if (parse_failed)
        return nullptr;

    if (function_length == -1)
        function_length = parameters.size();

    auto old_labels_in_scope = move(m_parser_state.m_labels_in_scope);
    ScopeGuard guard([&]() {
        m_parser_state.m_labels_in_scope = move(old_labels_in_scope);
    });

    bool is_strict = false;

    auto function_body_result = [&]() -> RefPtr<BlockStatement> {
        TemporaryChange change(m_parser_state.m_in_function_context, true);
        if (match(TokenType::CurlyOpen)) {
            // Parse a function body with statements
            return parse_block_statement(is_strict);
        }
        if (match_expression()) {
            // Parse a function body which returns a single expression

            // FIXME: We synthesize a block with a return statement
            // for arrow function bodies which are a single expression.
            // Esprima generates a single "ArrowFunctionExpression"
            // with a "body" property.
            auto return_expression = parse_expression(2);
            auto return_block = create_ast_node<BlockStatement>();
            return_block->append<ReturnStatement>(move(return_expression));
            return return_block;
        }
        // Invalid arrow function body
        return nullptr;
    }();

    if (!function_body_result.is_null()) {
        state_rollback_guard.disarm();
        auto body = function_body_result.release_nonnull();
        return create_ast_node<FunctionExpression>("", move(body), move(parameters), function_length, m_parser_state.m_var_scopes.take_last(), is_strict, true);
    }

    return nullptr;
}

RefPtr<Statement> Parser::try_parse_labelled_statement()
{
    save_state();
    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    auto identifier = consume(TokenType::Identifier).value();
    if (!match(TokenType::Colon))
        return {};
    consume(TokenType::Colon);

    if (!match_statement())
        return {};
    m_parser_state.m_labels_in_scope.set(identifier);
    auto statement = parse_statement();
    m_parser_state.m_labels_in_scope.remove(identifier);

    statement->set_label(identifier);
    state_rollback_guard.disarm();
    return statement;
}

NonnullRefPtr<ClassDeclaration> Parser::parse_class_declaration()
{
    return create_ast_node<ClassDeclaration>(parse_class_expression(true));
}

NonnullRefPtr<ClassExpression> Parser::parse_class_expression(bool expect_class_name)
{
    // Classes are always in strict mode.
    TemporaryChange strict_mode_rollback(m_parser_state.m_strict_mode, true);

    consume(TokenType::Class);

    NonnullRefPtrVector<ClassMethod> methods;
    RefPtr<Expression> super_class;
    RefPtr<FunctionExpression> constructor;

    String class_name = expect_class_name || match(TokenType::Identifier) ? consume(TokenType::Identifier).value().to_string() : "";

    if (match(TokenType::Extends)) {
        consume();
        super_class = parse_primary_expression();
    }

    consume(TokenType::CurlyOpen);

    while (!done() && !match(TokenType::CurlyClose)) {
        RefPtr<Expression> property_key;
        bool is_static = false;
        bool is_constructor = false;
        auto method_kind = ClassMethod::Kind::Method;

        if (match(TokenType::Semicolon)) {
            consume();
            continue;
        }

        if (match_property_key()) {
            StringView name;
            if (match(TokenType::Identifier) && m_parser_state.m_current_token.value() == "static") {
                consume();
                is_static = true;
            }

            if (match(TokenType::Identifier)) {
                auto identifier_name = m_parser_state.m_current_token.value();

                if (identifier_name == "get") {
                    method_kind = ClassMethod::Kind::Getter;
                    consume();
                } else if (identifier_name == "set") {
                    method_kind = ClassMethod::Kind::Setter;
                    consume();
                }
            }

            if (match_property_key()) {
                switch (m_parser_state.m_current_token.type()) {
                case TokenType::Identifier:
                    name = consume().value();
                    property_key = create_ast_node<StringLiteral>(name);
                    break;
                case TokenType::StringLiteral: {
                    auto string_literal = parse_string_literal(consume());
                    name = string_literal->value();
                    property_key = move(string_literal);
                    break;
                }
                default:
                    property_key = parse_property_key();
                    break;
                }

            } else {
                expected("property key");
            }

            // Constructor may be a StringLiteral or an Identifier.
            if (!is_static && name == "constructor") {
                if (method_kind != ClassMethod::Kind::Method)
                    syntax_error("Class constructor may not be an accessor");
                if (!constructor.is_null())
                    syntax_error("Classes may not have more than one constructor");

                is_constructor = true;
            }
        }

        if (match(TokenType::ParenOpen)) {
            auto function = parse_function_node<FunctionExpression>(false, true, !super_class.is_null());
            auto arg_count = function->parameters().size();

            if (method_kind == ClassMethod::Kind::Getter && arg_count != 0) {
                syntax_error("Class getter method must have no arguments");
            } else if (method_kind == ClassMethod::Kind::Setter && arg_count != 1) {
                syntax_error("Class setter method must have one argument");
            }

            if (is_constructor) {
                constructor = move(function);
            } else if (!property_key.is_null()) {
                methods.append(create_ast_node<ClassMethod>(property_key.release_nonnull(), move(function), method_kind, is_static));
            } else {
                syntax_error("No key for class method");
            }
        } else {
            expected("ParenOpen");
            consume();
        }
    }

    consume(TokenType::CurlyClose);

    if (constructor.is_null()) {
        auto constructor_body = create_ast_node<BlockStatement>();
        if (!super_class.is_null()) {
            // Set constructor to the result of parsing the source text
            // constructor(... args){ super (...args);}
            auto super_call = create_ast_node<CallExpression>(create_ast_node<SuperExpression>(), Vector { CallExpression::Argument { create_ast_node<Identifier>("args"), true } });
            constructor_body->append(create_ast_node<ExpressionStatement>(move(super_call)));
            constructor_body->add_variables(m_parser_state.m_var_scopes.last());

            constructor = create_ast_node<FunctionExpression>(class_name, move(constructor_body), Vector { FunctionNode::Parameter { "args", nullptr, true } }, 0, NonnullRefPtrVector<VariableDeclaration>(), true);
        } else {
            constructor = create_ast_node<FunctionExpression>(class_name, move(constructor_body), Vector<FunctionNode::Parameter> {}, 0, NonnullRefPtrVector<VariableDeclaration>(), true);
        }
    }

    return create_ast_node<ClassExpression>(move(class_name), move(constructor), move(super_class), move(methods));
}

NonnullRefPtr<Expression> Parser::parse_primary_expression()
{
    if (match_unary_prefixed_expression())
        return parse_unary_prefixed_expression();

    switch (m_parser_state.m_current_token.type()) {
    case TokenType::ParenOpen: {
        consume(TokenType::ParenOpen);
        if (match(TokenType::ParenClose) || match(TokenType::Identifier) || match(TokenType::TripleDot)) {
            auto arrow_function_result = try_parse_arrow_function_expression(true);
            if (!arrow_function_result.is_null()) {
                return arrow_function_result.release_nonnull();
            }
        }
        auto expression = parse_expression(0);
        consume(TokenType::ParenClose);
        return expression;
    }
    case TokenType::This:
        consume();
        return create_ast_node<ThisExpression>();
    case TokenType::Class:
        return parse_class_expression(false);
    case TokenType::Super:
        consume();
        if (!m_parser_state.m_allow_super_property_lookup)
            syntax_error("'super' keyword unexpected here");
        return create_ast_node<SuperExpression>();
    case TokenType::Identifier: {
        auto arrow_function_result = try_parse_arrow_function_expression(false);
        if (!arrow_function_result.is_null()) {
            return arrow_function_result.release_nonnull();
        }
        return create_ast_node<Identifier>(consume().value());
    }
    case TokenType::NumericLiteral:
        return create_ast_node<NumericLiteral>(consume().double_value());
    case TokenType::BigIntLiteral:
        return create_ast_node<BigIntLiteral>(consume().value());
    case TokenType::BoolLiteral:
        return create_ast_node<BooleanLiteral>(consume().bool_value());
    case TokenType::StringLiteral:
        return parse_string_literal(consume());
    case TokenType::NullLiteral:
        consume();
        return create_ast_node<NullLiteral>();
    case TokenType::CurlyOpen:
        return parse_object_expression();
    case TokenType::Function:
        return parse_function_node<FunctionExpression>();
    case TokenType::BracketOpen:
        return parse_array_expression();
    case TokenType::RegexLiteral:
        return parse_regexp_literal();
    case TokenType::TemplateLiteralStart:
        return parse_template_literal(false);
    case TokenType::New:
        return parse_new_expression();
    default:
        expected("primary expression (missing switch case)");
        consume();
        return create_ast_node<ErrorExpression>();
    }
}

NonnullRefPtr<RegExpLiteral> Parser::parse_regexp_literal()
{
    auto content = consume().value();
    auto flags = match(TokenType::RegexFlags) ? consume().value() : "";
    return create_ast_node<RegExpLiteral>(content.substring_view(1, content.length() - 2), flags);
}

NonnullRefPtr<Expression> Parser::parse_unary_prefixed_expression()
{
    auto precedence = g_operator_precedence.get(m_parser_state.m_current_token.type());
    auto associativity = operator_associativity(m_parser_state.m_current_token.type());
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::PlusPlus: {
        consume();
        auto rhs_start_line = m_parser_state.m_current_token.line_number();
        auto rhs_start_column = m_parser_state.m_current_token.line_column();
        auto rhs = parse_expression(precedence, associativity);
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for ++foo()
        if (!rhs->is_identifier() && !rhs->is_member_expression())
            syntax_error(String::formatted("Right-hand side of prefix increment operator must be identifier or member expression, got {}", rhs->class_name()), rhs_start_line, rhs_start_column);
        return create_ast_node<UpdateExpression>(UpdateOp::Increment, move(rhs), true);
    }
    case TokenType::MinusMinus: {
        consume();
        auto rhs_start_line = m_parser_state.m_current_token.line_number();
        auto rhs_start_column = m_parser_state.m_current_token.line_column();
        auto rhs = parse_expression(precedence, associativity);
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for --foo()
        if (!rhs->is_identifier() && !rhs->is_member_expression())
            syntax_error(String::formatted("Right-hand side of prefix decrement operator must be identifier or member expression, got {}", rhs->class_name()), rhs_start_line, rhs_start_column);
        return create_ast_node<UpdateExpression>(UpdateOp::Decrement, move(rhs), true);
    }
    case TokenType::ExclamationMark:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Not, parse_expression(precedence, associativity));
    case TokenType::Tilde:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::BitwiseNot, parse_expression(precedence, associativity));
    case TokenType::Plus:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Plus, parse_expression(precedence, associativity));
    case TokenType::Minus:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Minus, parse_expression(precedence, associativity));
    case TokenType::Typeof:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Typeof, parse_expression(precedence, associativity));
    case TokenType::Void:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Void, parse_expression(precedence, associativity));
    case TokenType::Delete:
        consume();
        return create_ast_node<UnaryExpression>(UnaryOp::Delete, parse_expression(precedence, associativity));
    default:
        expected("primary expression (missing switch case)");
        consume();
        return create_ast_node<ErrorExpression>();
    }
}

NonnullRefPtr<Expression> Parser::parse_property_key()
{
    if (match(TokenType::StringLiteral)) {
        return parse_string_literal(consume());
    } else if (match(TokenType::NumericLiteral)) {
        return create_ast_node<StringLiteral>(consume(TokenType::NumericLiteral).value());
    } else if (match(TokenType::BigIntLiteral)) {
        auto value = consume(TokenType::BigIntLiteral).value();
        return create_ast_node<StringLiteral>(value.substring_view(0, value.length() - 1));
    } else if (match(TokenType::BracketOpen)) {
        consume(TokenType::BracketOpen);
        auto result = parse_expression(0);
        consume(TokenType::BracketClose);
        return result;
    } else {
        if (!match_identifier_name())
            expected("IdentifierName");
        return create_ast_node<StringLiteral>(consume().value());
    }
}

NonnullRefPtr<ObjectExpression> Parser::parse_object_expression()
{
    consume(TokenType::CurlyOpen);

    NonnullRefPtrVector<ObjectProperty> properties;
    ObjectProperty::Type property_type;

    auto skip_to_next_property = [&] {
        while (!done() && !match(TokenType::Comma) && !match(TokenType::CurlyOpen))
            consume();
    };

    while (!done() && !match(TokenType::CurlyClose)) {
        property_type = ObjectProperty::Type::KeyValue;
        RefPtr<Expression> property_name;
        RefPtr<Expression> property_value;

        if (match(TokenType::TripleDot)) {
            consume();
            property_name = parse_expression(4);
            properties.append(create_ast_node<ObjectProperty>(*property_name, nullptr, ObjectProperty::Type::Spread, false));
            if (!match(TokenType::Comma))
                break;
            consume(TokenType::Comma);
            continue;
        }

        if (match(TokenType::Identifier)) {
            auto identifier = consume().value();
            if (identifier == "get" && match_property_key()) {
                property_type = ObjectProperty::Type::Getter;
                property_name = parse_property_key();
            } else if (identifier == "set" && match_property_key()) {
                property_type = ObjectProperty::Type::Setter;
                property_name = parse_property_key();
            } else {
                property_name = create_ast_node<StringLiteral>(identifier);
                property_value = create_ast_node<Identifier>(identifier);
            }
        } else {
            property_name = parse_property_key();
        }

        if (property_type == ObjectProperty::Type::Getter || property_type == ObjectProperty::Type::Setter) {
            if (!match(TokenType::ParenOpen)) {
                syntax_error(
                    "Expected '(' for object getter or setter property",
                    m_parser_state.m_current_token.line_number(),
                    m_parser_state.m_current_token.line_column());
                skip_to_next_property();
                continue;
            }
        }

        if (match(TokenType::ParenOpen)) {
            ASSERT(property_name);
            auto function = parse_function_node<FunctionExpression>(false, true);
            auto arg_count = function->parameters().size();

            if (property_type == ObjectProperty::Type::Getter && arg_count != 0) {
                syntax_error(
                    "Object getter property must have no arguments",
                    m_parser_state.m_current_token.line_number(),
                    m_parser_state.m_current_token.line_column());
                skip_to_next_property();
                continue;
            }
            if (property_type == ObjectProperty::Type::Setter && arg_count != 1) {
                syntax_error(
                    "Object setter property must have one argument",
                    m_parser_state.m_current_token.line_number(),
                    m_parser_state.m_current_token.line_column());
                skip_to_next_property();
                continue;
            }

            properties.append(create_ast_node<ObjectProperty>(*property_name, function, property_type, true));
        } else if (match(TokenType::Colon)) {
            if (!property_name) {
                syntax_error("Expected a property name");
                skip_to_next_property();
                continue;
            }
            consume();
            properties.append(create_ast_node<ObjectProperty>(*property_name, parse_expression(2), property_type, false));
        } else if (property_name && property_value) {
            properties.append(create_ast_node<ObjectProperty>(*property_name, *property_value, property_type, false));
        } else {
            syntax_error("Expected a property");
            skip_to_next_property();
            continue;
        }

        if (!match(TokenType::Comma))
            break;
        consume(TokenType::Comma);
    }

    consume(TokenType::CurlyClose);
    return create_ast_node<ObjectExpression>(properties);
}

NonnullRefPtr<ArrayExpression> Parser::parse_array_expression()
{
    consume(TokenType::BracketOpen);

    Vector<RefPtr<Expression>> elements;
    while (match_expression() || match(TokenType::TripleDot) || match(TokenType::Comma)) {
        RefPtr<Expression> expression;

        if (match(TokenType::TripleDot)) {
            consume(TokenType::TripleDot);
            expression = create_ast_node<SpreadExpression>(parse_expression(2));
        } else if (match_expression()) {
            expression = parse_expression(2);
        }

        elements.append(expression);
        if (!match(TokenType::Comma))
            break;
        consume(TokenType::Comma);
    }

    consume(TokenType::BracketClose);
    return create_ast_node<ArrayExpression>(move(elements));
}

NonnullRefPtr<StringLiteral> Parser::parse_string_literal(Token token)
{
    auto status = Token::StringValueStatus::Ok;
    auto string = token.string_value(status);
    if (status != Token::StringValueStatus::Ok) {
        String message;
        if (status == Token::StringValueStatus::MalformedHexEscape || status == Token::StringValueStatus::MalformedUnicodeEscape) {
            auto type = status == Token::StringValueStatus::MalformedUnicodeEscape ? "unicode" : "hexadecimal";
            message = String::formatted("Malformed {} escape sequence", type);
        } else if (status == Token::StringValueStatus::UnicodeEscapeOverflow) {
            message = "Unicode code_point must not be greater than 0x10ffff in escape sequence";
        }

        syntax_error(
            message,
            m_parser_state.m_current_token.line_number(),
            m_parser_state.m_current_token.line_column());
    }

    if (m_parser_state.m_use_strict_directive == UseStrictDirectiveState::Looking) {
        if (string == "use strict" && token.type() != TokenType::TemplateLiteralString) {
            m_parser_state.m_use_strict_directive = UseStrictDirectiveState::Found;
        } else {
            m_parser_state.m_use_strict_directive = UseStrictDirectiveState::None;
        }
    }

    return create_ast_node<StringLiteral>(string);
}

NonnullRefPtr<TemplateLiteral> Parser::parse_template_literal(bool is_tagged)
{
    consume(TokenType::TemplateLiteralStart);

    NonnullRefPtrVector<Expression> expressions;
    NonnullRefPtrVector<Expression> raw_strings;

    auto append_empty_string = [&expressions, &raw_strings, is_tagged]() {
        auto string_literal = create_ast_node<StringLiteral>("");
        expressions.append(string_literal);
        if (is_tagged)
            raw_strings.append(string_literal);
    };

    if (!match(TokenType::TemplateLiteralString))
        append_empty_string();

    while (!done() && !match(TokenType::TemplateLiteralEnd) && !match(TokenType::UnterminatedTemplateLiteral)) {
        if (match(TokenType::TemplateLiteralString)) {
            auto token = consume();
            expressions.append(parse_string_literal(token));
            if (is_tagged)
                raw_strings.append(create_ast_node<StringLiteral>(token.value()));
        } else if (match(TokenType::TemplateLiteralExprStart)) {
            consume(TokenType::TemplateLiteralExprStart);
            if (match(TokenType::TemplateLiteralExprEnd)) {
                syntax_error("Empty template literal expression block");
                return create_ast_node<TemplateLiteral>(expressions);
            }

            expressions.append(parse_expression(0));
            if (match(TokenType::UnterminatedTemplateLiteral)) {
                syntax_error("Unterminated template literal");
                return create_ast_node<TemplateLiteral>(expressions);
            }
            consume(TokenType::TemplateLiteralExprEnd);

            if (!match(TokenType::TemplateLiteralString))
                append_empty_string();
        } else {
            expected("Template literal string or expression");
            break;
        }
    }

    if (match(TokenType::UnterminatedTemplateLiteral)) {
        syntax_error("Unterminated template literal");
    } else {
        consume(TokenType::TemplateLiteralEnd);
    }

    if (is_tagged)
        return create_ast_node<TemplateLiteral>(expressions, raw_strings);
    return create_ast_node<TemplateLiteral>(expressions);
}

NonnullRefPtr<Expression> Parser::parse_expression(int min_precedence, Associativity associativity, Vector<TokenType> forbidden)
{
    auto expression = parse_primary_expression();
    while (match(TokenType::TemplateLiteralStart)) {
        auto template_literal = parse_template_literal(true);
        expression = create_ast_node<TaggedTemplateLiteral>(move(expression), move(template_literal));
    }
    while (match_secondary_expression(forbidden)) {
        int new_precedence = g_operator_precedence.get(m_parser_state.m_current_token.type());
        if (new_precedence < min_precedence)
            break;
        if (new_precedence == min_precedence && associativity == Associativity::Left)
            break;

        Associativity new_associativity = operator_associativity(m_parser_state.m_current_token.type());
        expression = parse_secondary_expression(move(expression), new_precedence, new_associativity);
        while (match(TokenType::TemplateLiteralStart)) {
            auto template_literal = parse_template_literal(true);
            expression = create_ast_node<TaggedTemplateLiteral>(move(expression), move(template_literal));
        }
    }
    if (match(TokenType::Comma) && min_precedence <= 1) {
        NonnullRefPtrVector<Expression> expressions;
        expressions.append(expression);
        while (match(TokenType::Comma)) {
            consume();
            expressions.append(parse_expression(2));
        }
        expression = create_ast_node<SequenceExpression>(move(expressions));
    }
    return expression;
}

NonnullRefPtr<Expression> Parser::parse_secondary_expression(NonnullRefPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Plus:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Addition, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PlusEquals:
        return parse_assignment_expression(AssignmentOp::AdditionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Minus:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Subtraction, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::MinusEquals:
        return parse_assignment_expression(AssignmentOp::SubtractionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Asterisk:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Multiplication, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AsteriskEquals:
        return parse_assignment_expression(AssignmentOp::MultiplicationAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Slash:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Division, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::SlashEquals:
        return parse_assignment_expression(AssignmentOp::DivisionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Percent:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Modulo, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PercentEquals:
        return parse_assignment_expression(AssignmentOp::ModuloAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoubleAsterisk:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::Exponentiation, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleAsteriskEquals:
        return parse_assignment_expression(AssignmentOp::ExponentiationAssignment, move(lhs), min_precedence, associativity);
    case TokenType::GreaterThan:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::GreaterThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::GreaterThanEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::GreaterThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThan:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::LessThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThanEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::LessThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::TypedEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::TypedInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::AbstractEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEquals:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::AbstractInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::In:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::In, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Instanceof:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::InstanceOf, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Ampersand:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::BitwiseAnd, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AmpersandEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseAndAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Pipe:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::BitwiseOr, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PipeEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseOrAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Caret:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::BitwiseXor, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::CaretEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseXorAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ShiftLeft:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::LeftShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ShiftLeftEquals:
        return parse_assignment_expression(AssignmentOp::LeftShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ShiftRight:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::RightShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ShiftRightEquals:
        return parse_assignment_expression(AssignmentOp::RightShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::UnsignedShiftRight:
        consume();
        return create_ast_node<BinaryExpression>(BinaryOp::UnsignedRightShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::UnsignedShiftRightEquals:
        return parse_assignment_expression(AssignmentOp::UnsignedRightShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ParenOpen:
        return parse_call_expression(move(lhs));
    case TokenType::Equals:
        return parse_assignment_expression(AssignmentOp::Assignment, move(lhs), min_precedence, associativity);
    case TokenType::Period:
        consume();
        if (!match_identifier_name())
            expected("IdentifierName");
        return create_ast_node<MemberExpression>(move(lhs), create_ast_node<Identifier>(consume().value()));
    case TokenType::BracketOpen: {
        consume(TokenType::BracketOpen);
        auto expression = create_ast_node<MemberExpression>(move(lhs), parse_expression(0), true);
        consume(TokenType::BracketClose);
        return expression;
    }
    case TokenType::PlusPlus:
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for foo()++
        if (!lhs->is_identifier() && !lhs->is_member_expression())
            syntax_error(String::formatted("Left-hand side of postfix increment operator must be identifier or member expression, got {}", lhs->class_name()));
        consume();
        return create_ast_node<UpdateExpression>(UpdateOp::Increment, move(lhs));
    case TokenType::MinusMinus:
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for foo()--
        if (!lhs->is_identifier() && !lhs->is_member_expression())
            syntax_error(String::formatted("Left-hand side of postfix increment operator must be identifier or member expression, got {}", lhs->class_name()));
        consume();
        return create_ast_node<UpdateExpression>(UpdateOp::Decrement, move(lhs));
    case TokenType::DoubleAmpersand:
        consume();
        return create_ast_node<LogicalExpression>(LogicalOp::And, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleAmpersandEquals:
        return parse_assignment_expression(AssignmentOp::AndAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoublePipe:
        consume();
        return create_ast_node<LogicalExpression>(LogicalOp::Or, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoublePipeEquals:
        return parse_assignment_expression(AssignmentOp::OrAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoubleQuestionMark:
        consume();
        return create_ast_node<LogicalExpression>(LogicalOp::NullishCoalescing, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleQuestionMarkEquals:
        return parse_assignment_expression(AssignmentOp::NullishAssignment, move(lhs), min_precedence, associativity);
    case TokenType::QuestionMark:
        return parse_conditional_expression(move(lhs));
    default:
        expected("secondary expression (missing switch case)");
        consume();
        return create_ast_node<ErrorExpression>();
    }
}

NonnullRefPtr<AssignmentExpression> Parser::parse_assignment_expression(AssignmentOp assignment_op, NonnullRefPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    ASSERT(match(TokenType::Equals)
        || match(TokenType::PlusEquals)
        || match(TokenType::MinusEquals)
        || match(TokenType::AsteriskEquals)
        || match(TokenType::SlashEquals)
        || match(TokenType::PercentEquals)
        || match(TokenType::DoubleAsteriskEquals)
        || match(TokenType::AmpersandEquals)
        || match(TokenType::PipeEquals)
        || match(TokenType::CaretEquals)
        || match(TokenType::ShiftLeftEquals)
        || match(TokenType::ShiftRightEquals)
        || match(TokenType::UnsignedShiftRightEquals)
        || match(TokenType::DoubleAmpersandEquals)
        || match(TokenType::DoublePipeEquals)
        || match(TokenType::DoubleQuestionMarkEquals));
    consume();
    if (!lhs->is_identifier() && !lhs->is_member_expression() && !lhs->is_call_expression()) {
        syntax_error("Invalid left-hand side in assignment");
    } else if (m_parser_state.m_strict_mode && lhs->is_identifier()) {
        auto name = static_cast<const Identifier&>(*lhs).string();
        if (name == "eval" || name == "arguments")
            syntax_error(String::formatted("'{}' cannot be assigned to in strict mode code", name));
    } else if (m_parser_state.m_strict_mode && lhs->is_call_expression()) {
        syntax_error("Cannot assign to function call");
    }
    return create_ast_node<AssignmentExpression>(assignment_op, move(lhs), parse_expression(min_precedence, associativity));
}

NonnullRefPtr<CallExpression> Parser::parse_call_expression(NonnullRefPtr<Expression> lhs)
{
    if (!m_parser_state.m_allow_super_constructor_call && lhs->is_super_expression())
        syntax_error("'super' keyword unexpected here");

    consume(TokenType::ParenOpen);

    Vector<CallExpression::Argument> arguments;

    while (match_expression() || match(TokenType::TripleDot)) {
        if (match(TokenType::TripleDot)) {
            consume();
            arguments.append({ parse_expression(2), true });
        } else {
            arguments.append({ parse_expression(2), false });
        }
        if (!match(TokenType::Comma))
            break;
        consume();
    }

    consume(TokenType::ParenClose);

    return create_ast_node<CallExpression>(move(lhs), move(arguments));
}

NonnullRefPtr<NewExpression> Parser::parse_new_expression()
{
    consume(TokenType::New);

    auto callee = parse_expression(g_operator_precedence.get(TokenType::New), Associativity::Right, { TokenType::ParenOpen });

    Vector<CallExpression::Argument> arguments;

    if (match(TokenType::ParenOpen)) {
        consume(TokenType::ParenOpen);
        while (match_expression() || match(TokenType::TripleDot)) {
            if (match(TokenType::TripleDot)) {
                consume();
                arguments.append({ parse_expression(2), true });
            } else {
                arguments.append({ parse_expression(2), false });
            }
            if (!match(TokenType::Comma))
                break;
            consume();
        }
        consume(TokenType::ParenClose);
    }

    return create_ast_node<NewExpression>(move(callee), move(arguments));
}

NonnullRefPtr<ReturnStatement> Parser::parse_return_statement()
{
    if (!m_parser_state.m_in_function_context)
        syntax_error("'return' not allowed outside of a function");

    consume(TokenType::Return);

    // Automatic semicolon insertion: terminate statement when return is followed by newline
    if (m_parser_state.m_current_token.trivia().contains('\n'))
        return create_ast_node<ReturnStatement>(nullptr);

    if (match_expression()) {
        auto expression = parse_expression(0);
        consume_or_insert_semicolon();
        return create_ast_node<ReturnStatement>(move(expression));
    }

    consume_or_insert_semicolon();
    return create_ast_node<ReturnStatement>(nullptr);
}

NonnullRefPtr<BlockStatement> Parser::parse_block_statement()
{
    bool dummy = false;
    return parse_block_statement(dummy);
}

NonnullRefPtr<BlockStatement> Parser::parse_block_statement(bool& is_strict)
{
    ScopePusher scope(*this, ScopePusher::Let);
    auto block = create_ast_node<BlockStatement>();
    consume(TokenType::CurlyOpen);

    bool first = true;
    bool initial_strict_mode_state = m_parser_state.m_strict_mode;
    if (initial_strict_mode_state) {
        m_parser_state.m_use_strict_directive = UseStrictDirectiveState::None;
        is_strict = true;
    } else {
        m_parser_state.m_use_strict_directive = UseStrictDirectiveState::Looking;
    }

    while (!done() && !match(TokenType::CurlyClose)) {
        if (match(TokenType::Semicolon)) {
            consume();
        } else if (match_statement()) {
            block->append(parse_statement());

            if (first && !initial_strict_mode_state) {
                if (m_parser_state.m_use_strict_directive == UseStrictDirectiveState::Found) {
                    is_strict = true;
                    m_parser_state.m_strict_mode = true;
                }
                m_parser_state.m_use_strict_directive = UseStrictDirectiveState::None;
            }
        } else {
            expected("statement");
            consume();
        }

        first = false;
    }
    m_parser_state.m_strict_mode = initial_strict_mode_state;
    consume(TokenType::CurlyClose);
    block->add_variables(m_parser_state.m_let_scopes.last());
    block->add_functions(m_parser_state.m_function_scopes.last());
    return block;
}

template<typename FunctionNodeType>
NonnullRefPtr<FunctionNodeType> Parser::parse_function_node(bool check_for_function_and_name, bool allow_super_property_lookup, bool allow_super_constructor_call)
{
    TemporaryChange super_property_access_rollback(m_parser_state.m_allow_super_property_lookup, allow_super_property_lookup);
    TemporaryChange super_constructor_call_rollback(m_parser_state.m_allow_super_constructor_call, allow_super_constructor_call);

    ScopePusher scope(*this, ScopePusher::Var | ScopePusher::Function);

    if (check_for_function_and_name)
        consume(TokenType::Function);

    String name;
    if (check_for_function_and_name) {
        if (FunctionNodeType::must_have_name()) {
            name = consume(TokenType::Identifier).value();
        } else {
            if (match(TokenType::Identifier))
                name = consume(TokenType::Identifier).value();
        }
    }
    consume(TokenType::ParenOpen);
    Vector<FunctionNode::Parameter> parameters;
    i32 function_length = -1;
    while (match(TokenType::Identifier) || match(TokenType::TripleDot)) {
        if (match(TokenType::TripleDot)) {
            consume();
            auto parameter_name = consume(TokenType::Identifier).value();
            function_length = parameters.size();
            parameters.append({ parameter_name, nullptr, true });
            break;
        }
        auto parameter_name = consume(TokenType::Identifier).value();
        RefPtr<Expression> default_value;
        if (match(TokenType::Equals)) {
            consume(TokenType::Equals);
            function_length = parameters.size();
            default_value = parse_expression(2);
        }
        parameters.append({ parameter_name, default_value });
        if (match(TokenType::ParenClose))
            break;
        consume(TokenType::Comma);
    }
    consume(TokenType::ParenClose);

    if (function_length == -1)
        function_length = parameters.size();

    TemporaryChange change(m_parser_state.m_in_function_context, true);
    auto old_labels_in_scope = move(m_parser_state.m_labels_in_scope);
    ScopeGuard guard([&]() {
        m_parser_state.m_labels_in_scope = move(old_labels_in_scope);
    });

    bool is_strict = false;
    auto body = parse_block_statement(is_strict);
    body->add_variables(m_parser_state.m_var_scopes.last());
    body->add_functions(m_parser_state.m_function_scopes.last());
    return create_ast_node<FunctionNodeType>(name, move(body), move(parameters), function_length, NonnullRefPtrVector<VariableDeclaration>(), is_strict);
}

NonnullRefPtr<VariableDeclaration> Parser::parse_variable_declaration(bool with_semicolon)
{
    DeclarationKind declaration_kind;

    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Var:
        declaration_kind = DeclarationKind::Var;
        consume(TokenType::Var);
        break;
    case TokenType::Let:
        declaration_kind = DeclarationKind::Let;
        consume(TokenType::Let);
        break;
    case TokenType::Const:
        declaration_kind = DeclarationKind::Const;
        consume(TokenType::Const);
        break;
    default:
        ASSERT_NOT_REACHED();
    }

    NonnullRefPtrVector<VariableDeclarator> declarations;
    for (;;) {
        auto id = consume(TokenType::Identifier).value();
        RefPtr<Expression> init;
        if (match(TokenType::Equals)) {
            consume();
            init = parse_expression(2);
        }
        declarations.append(create_ast_node<VariableDeclarator>(create_ast_node<Identifier>(move(id)), move(init)));
        if (match(TokenType::Comma)) {
            consume();
            continue;
        }
        break;
    }
    if (with_semicolon)
        consume_or_insert_semicolon();

    auto declaration = create_ast_node<VariableDeclaration>(declaration_kind, move(declarations));
    if (declaration_kind == DeclarationKind::Var)
        m_parser_state.m_var_scopes.last().append(declaration);
    else
        m_parser_state.m_let_scopes.last().append(declaration);
    return declaration;
}

NonnullRefPtr<ThrowStatement> Parser::parse_throw_statement()
{
    consume(TokenType::Throw);

    // Automatic semicolon insertion: terminate statement when throw is followed by newline
    if (m_parser_state.m_current_token.trivia().contains('\n')) {
        syntax_error("No line break is allowed between 'throw' and its expression");
        return create_ast_node<ThrowStatement>(create_ast_node<ErrorExpression>());
    }

    auto expression = parse_expression(0);
    consume_or_insert_semicolon();
    return create_ast_node<ThrowStatement>(move(expression));
}

NonnullRefPtr<BreakStatement> Parser::parse_break_statement()
{
    consume(TokenType::Break);
    FlyString target_label;
    if (match(TokenType::Semicolon)) {
        consume();
    } else {
        if (match(TokenType::Identifier) && !m_parser_state.m_current_token.trivia().contains('\n')) {
            target_label = consume().value();
            if (!m_parser_state.m_labels_in_scope.contains(target_label))
                syntax_error(String::formatted("Label '{}' not found", target_label));
        }
        consume_or_insert_semicolon();
    }

    if (target_label.is_null() && !m_parser_state.m_in_break_context)
        syntax_error("Unlabeled 'break' not allowed outside of a loop or switch statement");

    return create_ast_node<BreakStatement>(target_label);
}

NonnullRefPtr<ContinueStatement> Parser::parse_continue_statement()
{
    if (!m_parser_state.m_in_continue_context)
        syntax_error("'continue' not allow outside of a loop");

    consume(TokenType::Continue);
    FlyString target_label;
    if (match(TokenType::Semicolon)) {
        consume();
        return create_ast_node<ContinueStatement>(target_label);
    }
    if (match(TokenType::Identifier) && !m_parser_state.m_current_token.trivia().contains('\n')) {
        target_label = consume().value();
        if (!m_parser_state.m_labels_in_scope.contains(target_label))
            syntax_error(String::formatted("Label '{}' not found", target_label));
    }
    consume_or_insert_semicolon();
    return create_ast_node<ContinueStatement>(target_label);
}

NonnullRefPtr<ConditionalExpression> Parser::parse_conditional_expression(NonnullRefPtr<Expression> test)
{
    consume(TokenType::QuestionMark);
    auto consequent = parse_expression(2);
    consume(TokenType::Colon);
    auto alternate = parse_expression(2);
    return create_ast_node<ConditionalExpression>(move(test), move(consequent), move(alternate));
}

NonnullRefPtr<TryStatement> Parser::parse_try_statement()
{
    consume(TokenType::Try);

    auto block = parse_block_statement();

    RefPtr<CatchClause> handler;
    if (match(TokenType::Catch))
        handler = parse_catch_clause();

    RefPtr<BlockStatement> finalizer;
    if (match(TokenType::Finally)) {
        consume();
        finalizer = parse_block_statement();
    }

    return create_ast_node<TryStatement>(move(block), move(handler), move(finalizer));
}

NonnullRefPtr<DoWhileStatement> Parser::parse_do_while_statement()
{
    consume(TokenType::Do);

    auto body = [&]() -> NonnullRefPtr<Statement> {
        TemporaryChange break_change(m_parser_state.m_in_break_context, true);
        TemporaryChange continue_change(m_parser_state.m_in_continue_context, true);
        return parse_statement();
    }();

    consume(TokenType::While);
    consume(TokenType::ParenOpen);

    auto test = parse_expression(0);

    consume(TokenType::ParenClose);
    consume_or_insert_semicolon();

    return create_ast_node<DoWhileStatement>(move(test), move(body));
}

NonnullRefPtr<WhileStatement> Parser::parse_while_statement()
{
    consume(TokenType::While);
    consume(TokenType::ParenOpen);

    auto test = parse_expression(0);

    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_parser_state.m_in_break_context, true);
    TemporaryChange continue_change(m_parser_state.m_in_continue_context, true);
    auto body = parse_statement();

    return create_ast_node<WhileStatement>(move(test), move(body));
}

NonnullRefPtr<SwitchStatement> Parser::parse_switch_statement()
{
    consume(TokenType::Switch);

    consume(TokenType::ParenOpen);
    auto determinant = parse_expression(0);
    consume(TokenType::ParenClose);

    consume(TokenType::CurlyOpen);

    NonnullRefPtrVector<SwitchCase> cases;

    while (match(TokenType::Case) || match(TokenType::Default))
        cases.append(parse_switch_case());

    consume(TokenType::CurlyClose);

    return create_ast_node<SwitchStatement>(move(determinant), move(cases));
}

NonnullRefPtr<SwitchCase> Parser::parse_switch_case()
{
    RefPtr<Expression> test;

    if (consume().type() == TokenType::Case) {
        test = parse_expression(0);
    }

    consume(TokenType::Colon);

    NonnullRefPtrVector<Statement> consequent;
    TemporaryChange break_change(m_parser_state.m_in_break_context, true);
    while (match_statement())
        consequent.append(parse_statement());

    return create_ast_node<SwitchCase>(move(test), move(consequent));
}

NonnullRefPtr<CatchClause> Parser::parse_catch_clause()
{
    consume(TokenType::Catch);

    String parameter;
    if (match(TokenType::ParenOpen)) {
        consume();
        parameter = consume(TokenType::Identifier).value();
        consume(TokenType::ParenClose);
    }

    auto body = parse_block_statement();
    return create_ast_node<CatchClause>(parameter, move(body));
}

NonnullRefPtr<IfStatement> Parser::parse_if_statement()
{
    consume(TokenType::If);
    consume(TokenType::ParenOpen);
    auto predicate = parse_expression(0);
    consume(TokenType::ParenClose);
    auto consequent = parse_statement();
    RefPtr<Statement> alternate;
    if (match(TokenType::Else)) {
        consume(TokenType::Else);
        alternate = parse_statement();
    }
    return create_ast_node<IfStatement>(move(predicate), move(consequent), move(alternate));
}

NonnullRefPtr<Statement> Parser::parse_for_statement()
{
    auto match_for_in_of = [&]() {
        return match(TokenType::In) || (match(TokenType::Identifier) && m_parser_state.m_current_token.value() == "of");
    };

    consume(TokenType::For);

    consume(TokenType::ParenOpen);

    bool in_scope = false;
    RefPtr<ASTNode> init;
    if (!match(TokenType::Semicolon)) {
        if (match_expression()) {
            init = parse_expression(0, Associativity::Right, { TokenType::In });
            if (match_for_in_of())
                return parse_for_in_of_statement(*init);
        } else if (match_variable_declaration()) {
            if (!match(TokenType::Var)) {
                m_parser_state.m_let_scopes.append(NonnullRefPtrVector<VariableDeclaration>());
                in_scope = true;
            }
            init = parse_variable_declaration(false);
            if (match_for_in_of())
                return parse_for_in_of_statement(*init);
        } else {
            syntax_error("Unexpected token in for loop");
        }
    }
    consume(TokenType::Semicolon);

    RefPtr<Expression> test;
    if (!match(TokenType::Semicolon))
        test = parse_expression(0);

    consume(TokenType::Semicolon);

    RefPtr<Expression> update;
    if (!match(TokenType::ParenClose))
        update = parse_expression(0);

    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_parser_state.m_in_break_context, true);
    TemporaryChange continue_change(m_parser_state.m_in_continue_context, true);
    auto body = parse_statement();

    if (in_scope) {
        m_parser_state.m_let_scopes.take_last();
    }

    return create_ast_node<ForStatement>(move(init), move(test), move(update), move(body));
}

NonnullRefPtr<Statement> Parser::parse_for_in_of_statement(NonnullRefPtr<ASTNode> lhs)
{
    if (lhs->is_variable_declaration()) {
        auto declarations = static_cast<VariableDeclaration*>(lhs.ptr())->declarations();
        if (declarations.size() > 1) {
            syntax_error("multiple declarations not allowed in for..in/of");
            lhs = create_ast_node<ErrorExpression>();
        }
        if (declarations.first().init() != nullptr) {
            syntax_error("variable initializer not allowed in for..in/of");
            lhs = create_ast_node<ErrorExpression>();
        }
    }
    auto in_or_of = consume();
    auto rhs = parse_expression(0);
    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_parser_state.m_in_break_context, true);
    TemporaryChange continue_change(m_parser_state.m_in_continue_context, true);
    auto body = parse_statement();
    if (in_or_of.type() == TokenType::In)
        return create_ast_node<ForInStatement>(move(lhs), move(rhs), move(body));
    return create_ast_node<ForOfStatement>(move(lhs), move(rhs), move(body));
}

NonnullRefPtr<DebuggerStatement> Parser::parse_debugger_statement()
{
    consume(TokenType::Debugger);
    consume_or_insert_semicolon();
    return create_ast_node<DebuggerStatement>();
}

bool Parser::match(TokenType type) const
{
    return m_parser_state.m_current_token.type() == type;
}

bool Parser::match_variable_declaration() const
{
    switch (m_parser_state.m_current_token.type()) {
    case TokenType::Var:
    case TokenType::Let:
    case TokenType::Const:
        return true;
    default:
        return false;
    }
}

bool Parser::match_expression() const
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::BoolLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::BigIntLiteral
        || type == TokenType::StringLiteral
        || type == TokenType::TemplateLiteralStart
        || type == TokenType::NullLiteral
        || type == TokenType::Identifier
        || type == TokenType::New
        || type == TokenType::CurlyOpen
        || type == TokenType::BracketOpen
        || type == TokenType::ParenOpen
        || type == TokenType::Function
        || type == TokenType::This
        || type == TokenType::Super
        || type == TokenType::RegexLiteral
        || match_unary_prefixed_expression();
}

bool Parser::match_unary_prefixed_expression() const
{
    auto type = m_parser_state.m_current_token.type();
    return type == TokenType::PlusPlus
        || type == TokenType::MinusMinus
        || type == TokenType::ExclamationMark
        || type == TokenType::Tilde
        || type == TokenType::Plus
        || type == TokenType::Minus
        || type == TokenType::Typeof
        || type == TokenType::Void
        || type == TokenType::Delete;
}

bool Parser::match_secondary_expression(Vector<TokenType> forbidden) const
{
    auto type = m_parser_state.m_current_token.type();
    if (forbidden.contains_slow(type))
        return false;
    return type == TokenType::Plus
        || type == TokenType::PlusEquals
        || type == TokenType::Minus
        || type == TokenType::MinusEquals
        || type == TokenType::Asterisk
        || type == TokenType::AsteriskEquals
        || type == TokenType::Slash
        || type == TokenType::SlashEquals
        || type == TokenType::Percent
        || type == TokenType::PercentEquals
        || type == TokenType::DoubleAsterisk
        || type == TokenType::DoubleAsteriskEquals
        || type == TokenType::Equals
        || type == TokenType::EqualsEqualsEquals
        || type == TokenType::ExclamationMarkEqualsEquals
        || type == TokenType::EqualsEquals
        || type == TokenType::ExclamationMarkEquals
        || type == TokenType::GreaterThan
        || type == TokenType::GreaterThanEquals
        || type == TokenType::LessThan
        || type == TokenType::LessThanEquals
        || type == TokenType::ParenOpen
        || type == TokenType::Period
        || type == TokenType::BracketOpen
        || type == TokenType::PlusPlus
        || type == TokenType::MinusMinus
        || type == TokenType::In
        || type == TokenType::Instanceof
        || type == TokenType::QuestionMark
        || type == TokenType::Ampersand
        || type == TokenType::AmpersandEquals
        || type == TokenType::Pipe
        || type == TokenType::PipeEquals
        || type == TokenType::Caret
        || type == TokenType::CaretEquals
        || type == TokenType::ShiftLeft
        || type == TokenType::ShiftLeftEquals
        || type == TokenType::ShiftRight
        || type == TokenType::ShiftRightEquals
        || type == TokenType::UnsignedShiftRight
        || type == TokenType::UnsignedShiftRightEquals
        || type == TokenType::DoubleAmpersand
        || type == TokenType::DoubleAmpersandEquals
        || type == TokenType::DoublePipe
        || type == TokenType::DoublePipeEquals
        || type == TokenType::DoubleQuestionMark
        || type == TokenType::DoubleQuestionMarkEquals;
}

bool Parser::match_statement() const
{
    auto type = m_parser_state.m_current_token.type();
    return match_expression()
        || type == TokenType::Function
        || type == TokenType::Return
        || type == TokenType::Let
        || type == TokenType::Class
        || type == TokenType::Do
        || type == TokenType::If
        || type == TokenType::Throw
        || type == TokenType::Try
        || type == TokenType::While
        || type == TokenType::For
        || type == TokenType::Const
        || type == TokenType::CurlyOpen
        || type == TokenType::Switch
        || type == TokenType::Break
        || type == TokenType::Continue
        || type == TokenType::Var
        || type == TokenType::Debugger
        || type == TokenType::Semicolon;
}

bool Parser::match_identifier_name() const
{
    return m_parser_state.m_current_token.is_identifier_name();
}

bool Parser::match_property_key() const
{
    auto type = m_parser_state.m_current_token.type();
    return match_identifier_name()
        || type == TokenType::BracketOpen
        || type == TokenType::StringLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::BigIntLiteral;
}

bool Parser::done() const
{
    return match(TokenType::Eof);
}

Token Parser::consume()
{
    auto old_token = m_parser_state.m_current_token;
    m_parser_state.m_current_token = m_parser_state.m_lexer.next();
    return old_token;
}

void Parser::consume_or_insert_semicolon()
{
    // Semicolon was found and will be consumed
    if (match(TokenType::Semicolon)) {
        consume();
        return;
    }
    // Insert semicolon if...
    // ...token is preceded by one or more newlines
    if (m_parser_state.m_current_token.trivia().contains('\n'))
        return;
    // ...token is a closing curly brace
    if (match(TokenType::CurlyClose))
        return;
    // ...token is eof
    if (match(TokenType::Eof))
        return;

    // No rule for semicolon insertion applies -> syntax error
    expected("Semicolon");
}

Token Parser::consume(TokenType expected_type)
{
    if (!match(expected_type)) {
        expected(Token::name(expected_type));
    }
    return consume();
}

void Parser::expected(const char* what)
{
    syntax_error(String::formatted("Unexpected token {}. Expected {}", m_parser_state.m_current_token.name(), what));
}

void Parser::syntax_error(const String& message, size_t line, size_t column)
{
    if (line == 0 || column == 0) {
        line = m_parser_state.m_current_token.line_number();
        column = m_parser_state.m_current_token.line_column();
    }
    m_parser_state.m_errors.append({ message, line, column });
}

void Parser::save_state()
{
    m_saved_state.append(m_parser_state);
}

void Parser::load_state()
{
    ASSERT(!m_saved_state.is_empty());
    m_parser_state = m_saved_state.take_last();
}

}
