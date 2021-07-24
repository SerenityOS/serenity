/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/HashTable.h>
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>
#include <AK/TemporaryChange.h>
#include <LibJS/Runtime/RegExpObject.h>
#include <LibRegex/Regex.h>

namespace JS {

static bool statement_is_use_strict_directive(NonnullRefPtr<Statement> statement)
{
    if (!is<ExpressionStatement>(*statement))
        return false;
    auto& expression_statement = static_cast<ExpressionStatement&>(*statement);
    auto& expression = expression_statement.expression();
    if (!is<StringLiteral>(expression))
        return false;
    return static_cast<const StringLiteral&>(expression).is_use_strict_directive();
}

class ScopePusher {
public:
    enum Type {
        Var = 1,
        Let = 2,
    };

    ScopePusher(Parser& parser, unsigned mask, Parser::Scope::Type scope_type)
        : m_parser(parser)
        , m_mask(mask)
    {
        if (m_mask & Var)
            m_parser.m_state.var_scopes.append(NonnullRefPtrVector<VariableDeclaration>());
        if (m_mask & Let)
            m_parser.m_state.let_scopes.append(NonnullRefPtrVector<VariableDeclaration>());

        m_parser.m_state.current_scope = create<Parser::Scope>(scope_type, m_parser.m_state.current_scope);
    }

    ~ScopePusher()
    {
        if (m_mask & Var)
            m_parser.m_state.var_scopes.take_last();
        if (m_mask & Let)
            m_parser.m_state.let_scopes.take_last();

        auto& popped = m_parser.m_state.current_scope;
        // Manual clear required to resolve circular references
        popped->hoisted_function_declarations.clear();

        m_parser.m_state.current_scope = popped->parent;
    }

    void add_to_scope_node(NonnullRefPtr<ScopeNode> scope_node)
    {
        if (m_mask & Var)
            scope_node->add_variables(m_parser.m_state.var_scopes.last());
        if (m_mask & Let)
            scope_node->add_variables(m_parser.m_state.let_scopes.last());

        auto& scope = m_parser.m_state.current_scope;
        scope_node->add_functions(scope->function_declarations);

        for (auto& hoistable_function : scope->hoisted_function_declarations) {
            if (is_hoistable(hoistable_function)) {
                scope_node->add_hoisted_function(hoistable_function.declaration);
            }
        }
    }

    static bool is_hoistable(Parser::Scope::HoistableDeclaration& declaration)
    {
        auto& name = declaration.declaration->name();
        // See if we find any conflicting lexical declaration on the way up
        for (RefPtr<Parser::Scope> scope = declaration.scope; !scope.is_null(); scope = scope->parent) {
            if (scope->lexical_declarations.contains(name)) {
                return false;
            }
        }
        return true;
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
            warnln("Internal Error: No precedence for operator {}", Token::name(token));
            VERIFY_NOT_REACHED();
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

Parser::ParserState::ParserState(Lexer l, Program::Type program_type)
    : lexer(move(l))
    , current_token(TokenType::Invalid, {}, {}, {}, {}, 0, 0, 0)
{
    if (program_type == Program::Type::Module)
        lexer.disallow_html_comments();
    current_token = lexer.next();
}

Parser::Scope::Scope(Parser::Scope::Type type, RefPtr<Parser::Scope> parent_scope)
    : type(type)
    , parent(move(parent_scope))
{
}

RefPtr<Parser::Scope> Parser::Scope::get_current_function_scope()
{
    if (this->type == Parser::Scope::Function) {
        return *this;
    }
    auto result = this->parent;
    while (result->type != Parser::Scope::Function) {
        result = result->parent;
    }
    return result;
}

Parser::Parser(Lexer lexer, Program::Type program_type)
    : m_state(move(lexer), program_type)
    , m_program_type(program_type)
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

NonnullRefPtr<Program> Parser::parse_program(bool starts_in_strict_mode)
{
    auto rule_start = push_start();
    ScopePusher scope(*this, ScopePusher::Var | ScopePusher::Let, Scope::Function);
    auto program = adopt_ref(*new Program({ m_filename, rule_start.position(), position() }, m_program_type));
    if (starts_in_strict_mode || m_program_type == Program::Type::Module) {
        program->set_strict_mode();
        m_state.strict_mode = true;
    }

    bool parsing_directives = true;
    while (!done()) {
        if (match_declaration()) {
            program->append(parse_declaration());
            parsing_directives = false;
        } else if (match_statement()) {
            auto statement = parse_statement(AllowLabelledFunction::Yes);
            program->append(statement);
            if (statement_is_use_strict_directive(statement)) {
                if (parsing_directives) {
                    program->set_strict_mode();
                    m_state.strict_mode = true;
                }
                if (m_state.string_legacy_octal_escape_sequence_in_scope)
                    syntax_error("Octal escape sequence in string literal not allowed in strict mode");
            }

            if (parsing_directives && is<ExpressionStatement>(*statement)) {
                auto& expression_statement = static_cast<ExpressionStatement&>(*statement);
                auto& expression = expression_statement.expression();
                parsing_directives = is<StringLiteral>(expression);
            } else {
                parsing_directives = false;
            }

        } else if (match_export_or_import()) {
            VERIFY(m_state.current_token.type() == TokenType::Export || m_state.current_token.type() == TokenType::Import);
            if (m_state.current_token.type() == TokenType::Export)
                program->append_export(parse_export_statement(*program));
            else
                program->append_import(parse_import_statement(*program));

            parsing_directives = false;
        } else {
            expected("statement or declaration");
            consume();
            parsing_directives = false;
        }
    }
    if (m_state.var_scopes.size() == 1) {
        scope.add_to_scope_node(program);
    } else {
        syntax_error("Unclosed lexical_environment");
    }
    program->source_range().end = position();
    return program;
}

NonnullRefPtr<Declaration> Parser::parse_declaration()
{
    auto rule_start = push_start();
    switch (m_state.current_token.type()) {
    case TokenType::Class:
        return parse_class_declaration();
    case TokenType::Function: {
        auto declaration = parse_function_node<FunctionDeclaration>();
        m_state.current_scope->function_declarations.append(declaration);
        auto hoisting_target = m_state.current_scope->get_current_function_scope();
        hoisting_target->hoisted_function_declarations.append({ declaration, *m_state.current_scope });
        return declaration;
    }
    case TokenType::Let:
    case TokenType::Const:
        return parse_variable_declaration();
    default:
        expected("declaration");
        consume();
        return create_ast_node<ErrorDeclaration>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

NonnullRefPtr<Statement> Parser::parse_statement(AllowLabelledFunction allow_labelled_function)
{
    auto rule_start = push_start();
    switch (m_state.current_token.type()) {
    case TokenType::CurlyOpen:
        return parse_block_statement();
    case TokenType::Return:
        return parse_return_statement();
    case TokenType::Var:
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
    case TokenType::With:
        if (m_state.strict_mode)
            syntax_error("'with' statement not allowed in strict mode");
        return parse_with_statement();
    case TokenType::Debugger:
        return parse_debugger_statement();
    case TokenType::Semicolon:
        consume();
        return create_ast_node<EmptyStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
    default:
        if (match_identifier_name()) {
            auto result = try_parse_labelled_statement(allow_labelled_function);
            if (!result.is_null())
                return result.release_nonnull();
        }
        if (match_expression()) {
            if (match(TokenType::Function))
                syntax_error("Function declaration not allowed in single-statement context");
            auto expr = parse_expression(0);
            consume_or_insert_semicolon();
            return create_ast_node<ExpressionStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expr));
        }
        expected("statement");
        consume();
        return create_ast_node<ErrorStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

static constexpr AK::Array<StringView, 9> strict_reserved_words = { "implements", "interface", "let", "package", "private", "protected", "public", "static", "yield" };

static bool is_strict_reserved_word(StringView str)
{
    return any_of(strict_reserved_words, [&str](StringView const& word) {
        return word == str;
    });
}

RefPtr<FunctionExpression> Parser::try_parse_arrow_function_expression(bool expect_parens)
{
    save_state();
    auto rule_start = push_start();

    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    Vector<FunctionNode::Parameter> parameters;
    i32 function_length = -1;
    if (expect_parens) {
        // We have parens around the function parameters and can re-use the same parsing
        // logic used for regular functions: multiple parameters, default values, rest
        // parameter, maybe a trailing comma. If we have a new syntax error afterwards we
        // check if it's about a wrong token (something like duplicate parameter name must
        // not abort), know parsing failed and rollback the parser state.
        auto previous_syntax_errors = m_state.errors.size();
        parameters = parse_formal_parameters(function_length, FunctionNodeParseOptions::IsArrowFunction);
        if (m_state.errors.size() > previous_syntax_errors && m_state.errors[previous_syntax_errors].message.starts_with("Unexpected token"))
            return nullptr;
        if (!match(TokenType::ParenClose))
            return nullptr;
        consume();
    } else {
        // No parens - this must be an identifier followed by arrow. That's it.
        if (!match_identifier() && !match(TokenType::Yield) && !match(TokenType::Await))
            return nullptr;
        auto token = consume_identifier_reference();
        if (m_state.strict_mode && token.value().is_one_of("arguments"sv, "eval"sv))
            syntax_error("BindingIdentifier may not be 'arguments' or 'eval' in strict mode");
        parameters.append({ FlyString { token.value() }, {} });
    }
    // If there's a newline between the closing paren and arrow it's not a valid arrow function,
    // ASI should kick in instead (it'll then fail with "Unexpected token Arrow")
    if (m_state.current_token.trivia_contains_line_terminator())
        return nullptr;
    if (!match(TokenType::Arrow))
        return nullptr;
    consume();

    if (function_length == -1)
        function_length = parameters.size();

    m_state.function_parameters.append(parameters);

    auto old_labels_in_scope = move(m_state.labels_in_scope);
    ScopeGuard guard([&]() {
        m_state.labels_in_scope = move(old_labels_in_scope);
    });

    bool is_strict = false;

    auto function_body_result = [&]() -> RefPtr<BlockStatement> {
        TemporaryChange change(m_state.in_arrow_function_context, true);
        if (match(TokenType::CurlyOpen)) {
            // Parse a function body with statements
            ScopePusher scope(*this, ScopePusher::Var, Scope::Function);
            bool has_binding = any_of(parameters, [](FunctionNode::Parameter const& parameter) {
                return parameter.binding.has<NonnullRefPtr<BindingPattern>>();
            });

            auto body = parse_block_statement(is_strict, has_binding);
            scope.add_to_scope_node(body);
            return body;
        }
        if (match_expression()) {
            // Parse a function body which returns a single expression

            // FIXME: We synthesize a block with a return statement
            // for arrow function bodies which are a single expression.
            // Esprima generates a single "ArrowFunctionExpression"
            // with a "body" property.
            auto return_expression = parse_expression(2);
            auto return_block = create_ast_node<BlockStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
            return_block->append<ReturnStatement>({ m_filename, rule_start.position(), position() }, move(return_expression));
            return return_block;
        }
        // Invalid arrow function body
        return nullptr;
    }();

    m_state.function_parameters.take_last();

    if (function_body_result.is_null())
        return nullptr;

    state_rollback_guard.disarm();
    discard_saved_state();
    auto body = function_body_result.release_nonnull();

    if (is_strict) {
        for (auto& parameter : parameters) {
            parameter.binding.visit(
                [&](FlyString const& name) {
                    check_identifier_name_for_assignment_validity(name, true);
                },
                [&](auto const&) {});
        }
    }

    return create_ast_node<FunctionExpression>(
        { m_state.current_token.filename(), rule_start.position(), position() }, "", move(body),
        move(parameters), function_length, FunctionKind::Regular, is_strict, true);
}

RefPtr<Statement> Parser::try_parse_labelled_statement(AllowLabelledFunction allow_function)
{
    save_state();
    auto rule_start = push_start();
    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    if (match(TokenType::Yield) && (m_state.strict_mode || m_state.in_generator_function_context)) {
        syntax_error("'yield' label not allowed in this context");
        return {};
    }

    auto identifier = consume_identifier_reference().value();
    if (!match(TokenType::Colon))
        return {};
    consume(TokenType::Colon);

    if (!match_statement())
        return {};

    if (match(TokenType::Function) && (allow_function == AllowLabelledFunction::No || m_state.strict_mode)) {
        syntax_error("Not allowed to declare a function here");
        return {};
    }

    if (m_state.labels_in_scope.contains(identifier))
        syntax_error(String::formatted("Label '{}' has already been declared", identifier));
    m_state.labels_in_scope.set(identifier);

    RefPtr<Statement> labelled_statement;

    if (match(TokenType::Function)) {
        auto function_declaration = parse_function_node<FunctionDeclaration>();
        m_state.current_scope->function_declarations.append(function_declaration);
        auto hoisting_target = m_state.current_scope->get_current_function_scope();
        hoisting_target->hoisted_function_declarations.append({ function_declaration, *m_state.current_scope });
        if (function_declaration->kind() == FunctionKind::Generator)
            syntax_error("Generator functions cannot be defined in labelled statements");

        labelled_statement = move(function_declaration);
    } else {
        labelled_statement = parse_statement();
    }

    m_state.labels_in_scope.remove(identifier);

    labelled_statement->set_label(identifier);
    state_rollback_guard.disarm();
    discard_saved_state();
    return labelled_statement.release_nonnull();
}

RefPtr<MetaProperty> Parser::try_parse_new_target_expression()
{
    save_state();
    auto rule_start = push_start();
    ArmedScopeGuard state_rollback_guard = [&] {
        load_state();
    };

    consume(TokenType::New);
    if (!match(TokenType::Period))
        return {};
    consume();
    if (!match(TokenType::Identifier))
        return {};
    if (consume().value() != "target")
        return {};

    state_rollback_guard.disarm();
    discard_saved_state();
    return create_ast_node<MetaProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, MetaProperty::Type::NewTarget);
}

NonnullRefPtr<ClassDeclaration> Parser::parse_class_declaration()
{
    auto rule_start = push_start();
    return create_ast_node<ClassDeclaration>({ m_state.current_token.filename(), rule_start.position(), position() }, parse_class_expression(true));
}

NonnullRefPtr<ClassExpression> Parser::parse_class_expression(bool expect_class_name)
{
    auto rule_start = push_start();
    // Classes are always in strict mode.
    TemporaryChange strict_mode_rollback(m_state.strict_mode, true);

    consume(TokenType::Class);

    NonnullRefPtrVector<ClassMethod> methods;
    RefPtr<Expression> super_class;
    RefPtr<FunctionExpression> constructor;

    String class_name = expect_class_name || match_identifier() || match(TokenType::Yield) || match(TokenType::Await)
        ? consume_identifier_reference().value().to_string()
        : "";

    check_identifier_name_for_assignment_validity(class_name, true);
    if (match(TokenType::Extends)) {
        consume();
        auto [expression, should_continue_parsing] = parse_primary_expression();

        // Basically a (much) simplified parse_secondary_expression().
        for (;;) {
            if (match(TokenType::TemplateLiteralStart)) {
                auto template_literal = parse_template_literal(true);
                expression = create_ast_node<TaggedTemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(template_literal));
                continue;
            }
            if (match(TokenType::BracketOpen) || match(TokenType::Period) || match(TokenType::ParenOpen)) {
                auto precedence = g_operator_precedence.get(m_state.current_token.type());
                expression = parse_secondary_expression(move(expression), precedence);
                continue;
            }
            break;
        }

        super_class = move(expression);
        (void)should_continue_parsing;
    }

    consume(TokenType::CurlyOpen);

    while (!done() && !match(TokenType::CurlyClose)) {
        RefPtr<Expression> property_key;
        bool is_static = false;
        bool is_constructor = false;
        bool is_generator = false;
        auto method_kind = ClassMethod::Kind::Method;

        if (match(TokenType::Semicolon)) {
            consume();
            continue;
        }

        if (match(TokenType::Asterisk)) {
            consume();
            is_generator = true;
        }

        if (match_property_key()) {
            StringView name;
            if (!is_generator && m_state.current_token.value() == "static"sv) {
                if (match(TokenType::Identifier)) {
                    consume();
                    is_static = true;
                    if (match(TokenType::Asterisk)) {
                        consume();
                        is_generator = true;
                    }
                }
            }

            if (match(TokenType::Identifier)) {
                auto identifier_name = m_state.current_token.value();

                if (identifier_name == "get") {
                    method_kind = ClassMethod::Kind::Getter;
                    consume();
                } else if (identifier_name == "set") {
                    method_kind = ClassMethod::Kind::Setter;
                    consume();
                }
            }

            if (match_property_key()) {
                switch (m_state.current_token.type()) {
                case TokenType::Identifier:
                    name = consume().value();
                    property_key = create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, name);
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

                //https://tc39.es/ecma262/#sec-class-definitions-static-semantics-early-errors
                // ClassElement : static MethodDefinition
                //   It is a Syntax Error if PropName of MethodDefinition is "prototype".
                if (is_static && name == "prototype"sv)
                    syntax_error("Classes may not have a static property named 'prototype'");

            } else {
                expected("property key");
            }

            // Constructor may be a StringLiteral or an Identifier.
            if (!is_static && name == "constructor") {
                if (method_kind != ClassMethod::Kind::Method)
                    syntax_error("Class constructor may not be an accessor");
                if (!constructor.is_null())
                    syntax_error("Classes may not have more than one constructor");
                if (is_generator)
                    syntax_error("Class constructor may not be a generator");

                is_constructor = true;
            }
        }

        if (match(TokenType::ParenOpen)) {
            u8 parse_options = FunctionNodeParseOptions::AllowSuperPropertyLookup;
            if (!super_class.is_null())
                parse_options |= FunctionNodeParseOptions::AllowSuperConstructorCall;
            if (method_kind == ClassMethod::Kind::Getter)
                parse_options |= FunctionNodeParseOptions::IsGetterFunction;
            if (method_kind == ClassMethod::Kind::Setter)
                parse_options |= FunctionNodeParseOptions::IsSetterFunction;
            if (is_generator)
                parse_options |= FunctionNodeParseOptions::IsGeneratorFunction;
            auto function = parse_function_node<FunctionExpression>(parse_options);
            if (is_constructor) {
                constructor = move(function);
            } else if (!property_key.is_null()) {
                methods.append(create_ast_node<ClassMethod>({ m_state.current_token.filename(), rule_start.position(), position() }, property_key.release_nonnull(), move(function), method_kind, is_static));
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
        auto constructor_body = create_ast_node<BlockStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
        if (!super_class.is_null()) {
            // Set constructor to the result of parsing the source text
            // constructor(... args){ super (...args);}
            auto super_call = create_ast_node<SuperCall>(
                { m_state.current_token.filename(), rule_start.position(), position() },
                Vector { CallExpression::Argument { create_ast_node<Identifier>({ m_state.current_token.filename(), rule_start.position(), position() }, "args"), true } });
            constructor_body->append(create_ast_node<ExpressionStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(super_call)));
            constructor_body->add_variables(m_state.var_scopes.last());

            constructor = create_ast_node<FunctionExpression>(
                { m_state.current_token.filename(), rule_start.position(), position() }, class_name, move(constructor_body),
                Vector { FunctionNode::Parameter { FlyString { "args" }, nullptr, true } }, 0, FunctionKind::Regular, true);
        } else {
            constructor = create_ast_node<FunctionExpression>(
                { m_state.current_token.filename(), rule_start.position(), position() }, class_name, move(constructor_body),
                Vector<FunctionNode::Parameter> {}, 0, FunctionKind::Regular, true);
        }
    }

    return create_ast_node<ClassExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(class_name), move(constructor), move(super_class), move(methods));
}

Parser::PrimaryExpressionParseResult Parser::parse_primary_expression()
{
    auto rule_start = push_start();
    if (match_unary_prefixed_expression())
        return { parse_unary_prefixed_expression() };

    switch (m_state.current_token.type()) {
    case TokenType::ParenOpen: {
        auto paren_position = position();
        consume(TokenType::ParenOpen);
        if ((match(TokenType::ParenClose) || match_identifier() || match(TokenType::TripleDot) || match(TokenType::CurlyOpen) || match(TokenType::BracketOpen))
            && !try_parse_arrow_function_expression_failed_at_position(paren_position)) {

            auto arrow_function_result = try_parse_arrow_function_expression(true);
            if (!arrow_function_result.is_null())
                return { arrow_function_result.release_nonnull() };

            set_try_parse_arrow_function_expression_failed_at_position(paren_position, true);
        }
        auto expression = parse_expression(0);
        consume(TokenType::ParenClose);
        if (is<FunctionExpression>(*expression)) {
            static_cast<FunctionExpression&>(*expression).set_cannot_auto_rename();
        }
        return { move(expression) };
    }
    case TokenType::This:
        consume();
        return { create_ast_node<ThisExpression>({ m_state.current_token.filename(), rule_start.position(), position() }) };
    case TokenType::Class:
        return { parse_class_expression(false) };
    case TokenType::Super:
        consume();
        if (!m_state.allow_super_property_lookup)
            syntax_error("'super' keyword unexpected here");
        return { create_ast_node<SuperExpression>({ m_state.current_token.filename(), rule_start.position(), position() }) };
    case TokenType::Identifier: {
    read_as_identifier:;
        if (!try_parse_arrow_function_expression_failed_at_position(position())) {
            auto arrow_function_result = try_parse_arrow_function_expression(false);
            if (!arrow_function_result.is_null())
                return { arrow_function_result.release_nonnull() };

            set_try_parse_arrow_function_expression_failed_at_position(position(), true);
        }
        auto string = consume().value();
        // This could be 'eval' or 'arguments' and thus needs a custom check (`eval[1] = true`)
        if (m_state.strict_mode && (string == "let" || is_strict_reserved_word(string)))
            syntax_error(String::formatted("Identifier must not be a reserved word in strict mode ('{}')", string));
        return { create_ast_node<Identifier>({ m_state.current_token.filename(), rule_start.position(), position() }, string) };
    }
    case TokenType::NumericLiteral:
        return { create_ast_node<NumericLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume_and_validate_numeric_literal().double_value()) };
    case TokenType::BigIntLiteral:
        return { create_ast_node<BigIntLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value()) };
    case TokenType::BoolLiteral:
        return { create_ast_node<BooleanLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().bool_value()) };
    case TokenType::StringLiteral:
        return { parse_string_literal(consume()) };
    case TokenType::NullLiteral:
        consume();
        return { create_ast_node<NullLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }) };
    case TokenType::CurlyOpen:
        return { parse_object_expression() };
    case TokenType::Function:
        return { parse_function_node<FunctionExpression>() };
    case TokenType::BracketOpen:
        return { parse_array_expression() };
    case TokenType::RegexLiteral:
        return { parse_regexp_literal() };
    case TokenType::TemplateLiteralStart:
        return { parse_template_literal(false) };
    case TokenType::New: {
        auto new_start = position();
        auto new_target_result = try_parse_new_target_expression();
        if (!new_target_result.is_null()) {
            if (!m_state.in_function_context)
                syntax_error("'new.target' not allowed outside of a function", new_start);
            return { new_target_result.release_nonnull() };
        }
        return { parse_new_expression() };
    }
    case TokenType::Yield:
        if (!m_state.in_generator_function_context)
            goto read_as_identifier;
        return { parse_yield_expression(), false };
    default:
        if (match_identifier_name())
            goto read_as_identifier;
        expected("primary expression");
        consume();
        return { create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() }) };
    }
}

NonnullRefPtr<RegExpLiteral> Parser::parse_regexp_literal()
{
    auto rule_start = push_start();
    auto pattern = consume().value();
    // Remove leading and trailing slash.
    pattern = pattern.substring_view(1, pattern.length() - 2);

    auto flags = String::empty();
    auto parsed_flags = RegExpObject::default_flags;

    if (match(TokenType::RegexFlags)) {
        auto flags_start = position();
        flags = consume().value();

        auto parsed_flags_or_error = regex_flags_from_string(flags);
        if (parsed_flags_or_error.is_error())
            syntax_error(parsed_flags_or_error.release_error(), flags_start);
        else
            parsed_flags = parsed_flags_or_error.release_value();
    }

    auto parsed_pattern = parse_regex_pattern(pattern, parsed_flags.has_flag_set(ECMAScriptFlags::Unicode));
    auto parsed_regex = Regex<ECMA262>::parse_pattern(parsed_pattern, parsed_flags);

    if (parsed_regex.error != regex::Error::NoError)
        syntax_error(String::formatted("RegExp compile error: {}", Regex<ECMA262>(parsed_regex, parsed_pattern, parsed_flags).error_string()), rule_start.position());

    SourceRange range { m_state.current_token.filename(), rule_start.position(), position() };
    return create_ast_node<RegExpLiteral>(move(range), move(parsed_regex), move(parsed_pattern), move(parsed_flags), pattern.to_string(), move(flags));
}

NonnullRefPtr<Expression> Parser::parse_unary_prefixed_expression()
{
    auto rule_start = push_start();
    auto precedence = g_operator_precedence.get(m_state.current_token.type());
    auto associativity = operator_associativity(m_state.current_token.type());
    switch (m_state.current_token.type()) {
    case TokenType::PlusPlus: {
        consume();
        auto rhs_start = position();
        auto rhs = parse_expression(precedence, associativity);
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for ++foo()
        if (!is<Identifier>(*rhs) && !is<MemberExpression>(*rhs))
            syntax_error(String::formatted("Right-hand side of prefix increment operator must be identifier or member expression, got {}", rhs->class_name()), rhs_start);

        if (m_state.strict_mode && is<Identifier>(*rhs)) {
            auto& identifier = static_cast<Identifier&>(*rhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }

        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Increment, move(rhs), true);
    }
    case TokenType::MinusMinus: {
        consume();
        auto rhs_start = position();
        auto rhs = parse_expression(precedence, associativity);
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for --foo()
        if (!is<Identifier>(*rhs) && !is<MemberExpression>(*rhs))
            syntax_error(String::formatted("Right-hand side of prefix decrement operator must be identifier or member expression, got {}", rhs->class_name()), rhs_start);

        if (m_state.strict_mode && is<Identifier>(*rhs)) {
            auto& identifier = static_cast<Identifier&>(*rhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }

        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Decrement, move(rhs), true);
    }
    case TokenType::ExclamationMark:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Not, parse_expression(precedence, associativity));
    case TokenType::Tilde:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::BitwiseNot, parse_expression(precedence, associativity));
    case TokenType::Plus:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Plus, parse_expression(precedence, associativity));
    case TokenType::Minus:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Minus, parse_expression(precedence, associativity));
    case TokenType::Typeof:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Typeof, parse_expression(precedence, associativity));
    case TokenType::Void:
        consume();
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Void, parse_expression(precedence, associativity));
    case TokenType::Delete: {
        consume();
        auto rhs_start = position();
        auto rhs = parse_expression(precedence, associativity);
        if (is<Identifier>(*rhs) && m_state.strict_mode) {
            syntax_error("Delete of an unqualified identifier in strict mode.", rhs_start);
        }
        return create_ast_node<UnaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UnaryOp::Delete, move(rhs));
    }
    default:
        expected("primary expression");
        consume();
        return create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

NonnullRefPtr<Expression> Parser::parse_property_key()
{
    auto rule_start = push_start();
    if (match(TokenType::StringLiteral)) {
        return parse_string_literal(consume());
    } else if (match(TokenType::NumericLiteral)) {
        return create_ast_node<NumericLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().double_value());
    } else if (match(TokenType::BigIntLiteral)) {
        return create_ast_node<BigIntLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value());
    } else if (match(TokenType::BracketOpen)) {
        consume(TokenType::BracketOpen);
        auto result = parse_expression(2);
        consume(TokenType::BracketClose);
        return result;
    } else {
        if (!match_identifier_name())
            expected("IdentifierName");
        return create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value());
    }
}

NonnullRefPtr<ObjectExpression> Parser::parse_object_expression()
{
    auto rule_start = push_start();
    consume(TokenType::CurlyOpen);

    NonnullRefPtrVector<ObjectProperty> properties;
    ObjectProperty::Type property_type;
    Optional<SourceRange> invalid_object_literal_property_range;

    auto skip_to_next_property = [&] {
        while (!done() && !match(TokenType::Comma) && !match(TokenType::CurlyOpen))
            consume();
    };

    // It is a Syntax Error if PropertyNameList of PropertyDefinitionList contains any duplicate
    // entries for "__proto__" and at least two of those entries were obtained from productions  of
    // the form PropertyDefinition : PropertyName : AssignmentExpression .
    bool has_direct_proto_property = false;

    while (!done() && !match(TokenType::CurlyClose)) {
        property_type = ObjectProperty::Type::KeyValue;
        RefPtr<Expression> property_name;
        RefPtr<Expression> property_value;
        FunctionKind function_kind { FunctionKind::Regular };

        if (match(TokenType::TripleDot)) {
            consume();
            property_name = parse_expression(4);
            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, nullptr, ObjectProperty::Type::Spread, false));
            if (!match(TokenType::Comma))
                break;
            consume(TokenType::Comma);
            continue;
        }

        auto type = m_state.current_token.type();

        if (match(TokenType::Asterisk)) {
            consume();
            property_type = ObjectProperty::Type::KeyValue;
            property_name = parse_property_key();
            function_kind = FunctionKind ::Generator;
        } else if (match(TokenType::Identifier)) {
            auto identifier = consume().value();
            if (identifier == "get" && match_property_key()) {
                property_type = ObjectProperty::Type::Getter;
                property_name = parse_property_key();
            } else if (identifier == "set" && match_property_key()) {
                property_type = ObjectProperty::Type::Setter;
                property_name = parse_property_key();
            } else {
                property_name = create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, identifier);
                property_value = create_ast_node<Identifier>({ m_state.current_token.filename(), rule_start.position(), position() }, identifier);
            }
        } else {
            property_name = parse_property_key();
        }

        bool is_proto = (type == TokenType::StringLiteral || type == TokenType::Identifier) && is<StringLiteral>(*property_name) && static_cast<StringLiteral const&>(*property_name).value() == "__proto__";

        if (property_type == ObjectProperty::Type::Getter || property_type == ObjectProperty::Type::Setter) {
            if (!match(TokenType::ParenOpen)) {
                expected("'(' for object getter or setter property");
                skip_to_next_property();
                continue;
            }
        }
        if (match(TokenType::Equals)) {
            // Not a valid object literal, but a valid assignment target
            consume();
            // Parse the expression and throw it away
            auto expression = parse_expression(2);
            if (!invalid_object_literal_property_range.has_value())
                invalid_object_literal_property_range = expression->source_range();
        } else if (match(TokenType::ParenOpen)) {
            VERIFY(property_name);
            u8 parse_options = FunctionNodeParseOptions::AllowSuperPropertyLookup;
            if (property_type == ObjectProperty::Type::Getter)
                parse_options |= FunctionNodeParseOptions::IsGetterFunction;
            if (property_type == ObjectProperty::Type::Setter)
                parse_options |= FunctionNodeParseOptions::IsSetterFunction;
            if (function_kind == FunctionKind::Generator)
                parse_options |= FunctionNodeParseOptions::IsGeneratorFunction;
            auto function = parse_function_node<FunctionExpression>(parse_options);
            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, function, property_type, true));
        } else if (match(TokenType::Colon)) {
            if (!property_name) {
                expected("a property name");
                skip_to_next_property();
                continue;
            }
            consume();
            if (is_proto) {
                if (has_direct_proto_property)
                    syntax_error("Property name '__proto__' must not appear more than once in object literal");
                has_direct_proto_property = true;
            }
            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, parse_expression(2), property_type, false));
        } else if (property_name && property_value) {
            properties.append(create_ast_node<ObjectProperty>({ m_state.current_token.filename(), rule_start.position(), position() }, *property_name, *property_value, property_type, false));
        } else {
            expected("a property");
            skip_to_next_property();
            continue;
        }

        if (!match(TokenType::Comma))
            break;
        consume(TokenType::Comma);
    }

    consume(TokenType::CurlyClose);
    return create_ast_node<ObjectExpression>(
        { m_state.current_token.filename(), rule_start.position(), position() },
        move(properties),
        move(invalid_object_literal_property_range));
}

NonnullRefPtr<ArrayExpression> Parser::parse_array_expression()
{
    auto rule_start = push_start();
    consume(TokenType::BracketOpen);

    Vector<RefPtr<Expression>> elements;
    while (match_expression() || match(TokenType::TripleDot) || match(TokenType::Comma)) {
        RefPtr<Expression> expression;

        if (match(TokenType::TripleDot)) {
            consume(TokenType::TripleDot);
            expression = create_ast_node<SpreadExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, parse_expression(2));
        } else if (match_expression()) {
            expression = parse_expression(2);
        }

        elements.append(expression);
        if (!match(TokenType::Comma))
            break;
        consume(TokenType::Comma);
    }

    consume(TokenType::BracketClose);
    return create_ast_node<ArrayExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(elements));
}

NonnullRefPtr<StringLiteral> Parser::parse_string_literal(const Token& token, bool in_template_literal)
{
    auto rule_start = push_start();
    auto status = Token::StringValueStatus::Ok;
    auto string = token.string_value(status);
    if (status != Token::StringValueStatus::Ok) {
        String message;
        if (status == Token::StringValueStatus::LegacyOctalEscapeSequence) {
            m_state.string_legacy_octal_escape_sequence_in_scope = true;
            if (in_template_literal)
                message = "Octal escape sequence not allowed in template literal";
            else if (m_state.strict_mode)
                message = "Octal escape sequence in string literal not allowed in strict mode";
        } else if (status == Token::StringValueStatus::MalformedHexEscape || status == Token::StringValueStatus::MalformedUnicodeEscape) {
            auto type = status == Token::StringValueStatus::MalformedUnicodeEscape ? "unicode" : "hexadecimal";
            message = String::formatted("Malformed {} escape sequence", type);
        } else if (status == Token::StringValueStatus::UnicodeEscapeOverflow) {
            message = "Unicode code_point must not be greater than 0x10ffff in escape sequence";
        } else {
            VERIFY_NOT_REACHED();
        }

        if (!message.is_empty())
            syntax_error(message, Position { token.line_number(), token.line_column() });
    }

    auto is_use_strict_directive = !in_template_literal && (token.value() == "'use strict'" || token.value() == "\"use strict\"");

    return create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, string, is_use_strict_directive);
}

NonnullRefPtr<TemplateLiteral> Parser::parse_template_literal(bool is_tagged)
{
    auto rule_start = push_start();
    consume(TokenType::TemplateLiteralStart);

    NonnullRefPtrVector<Expression> expressions;
    NonnullRefPtrVector<Expression> raw_strings;

    auto append_empty_string = [this, &rule_start, &expressions, &raw_strings, is_tagged]() {
        auto string_literal = create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, "");
        expressions.append(string_literal);
        if (is_tagged)
            raw_strings.append(string_literal);
    };

    if (!match(TokenType::TemplateLiteralString))
        append_empty_string();

    while (!done() && !match(TokenType::TemplateLiteralEnd) && !match(TokenType::UnterminatedTemplateLiteral)) {
        if (match(TokenType::TemplateLiteralString)) {
            auto token = consume();
            expressions.append(parse_string_literal(token, true));
            if (is_tagged)
                raw_strings.append(create_ast_node<StringLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, token.value()));
        } else if (match(TokenType::TemplateLiteralExprStart)) {
            consume(TokenType::TemplateLiteralExprStart);
            if (match(TokenType::TemplateLiteralExprEnd)) {
                syntax_error("Empty template literal expression block");
                return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions);
            }

            expressions.append(parse_expression(0));
            if (match(TokenType::UnterminatedTemplateLiteral)) {
                syntax_error("Unterminated template literal");
                return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions);
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
        return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions, raw_strings);
    return create_ast_node<TemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, expressions);
}

NonnullRefPtr<Expression> Parser::parse_expression(int min_precedence, Associativity associativity, const Vector<TokenType>& forbidden)
{
    auto rule_start = push_start();
    auto [expression, should_continue_parsing] = parse_primary_expression();
    auto check_for_invalid_object_property = [&](auto& expression) {
        if (is<ObjectExpression>(*expression)) {
            if (auto range = static_cast<ObjectExpression&>(*expression).invalid_property_range(); range.has_value())
                syntax_error("Invalid property in object literal", range->start);
        }
    };
    while (match(TokenType::TemplateLiteralStart)) {
        auto template_literal = parse_template_literal(true);
        expression = create_ast_node<TaggedTemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(template_literal));
    }
    if (should_continue_parsing) {
        while (match_secondary_expression(forbidden)) {
            int new_precedence = g_operator_precedence.get(m_state.current_token.type());
            if (new_precedence < min_precedence)
                break;
            if (new_precedence == min_precedence && associativity == Associativity::Left)
                break;
            check_for_invalid_object_property(expression);

            Associativity new_associativity = operator_associativity(m_state.current_token.type());
            expression = parse_secondary_expression(move(expression), new_precedence, new_associativity);
            while (match(TokenType::TemplateLiteralStart)) {
                auto template_literal = parse_template_literal(true);
                expression = create_ast_node<TaggedTemplateLiteral>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(template_literal));
            }
        }
    }

    check_for_invalid_object_property(expression);

    if (match(TokenType::Comma) && min_precedence <= 1) {
        NonnullRefPtrVector<Expression> expressions;
        expressions.append(expression);
        while (match(TokenType::Comma)) {
            consume();
            expressions.append(parse_expression(2));
        }
        expression = create_ast_node<SequenceExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expressions));
    }
    return expression;
}

NonnullRefPtr<Expression> Parser::parse_secondary_expression(NonnullRefPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    auto rule_start = push_start();
    switch (m_state.current_token.type()) {
    case TokenType::Plus:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Addition, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PlusEquals:
        return parse_assignment_expression(AssignmentOp::AdditionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Minus:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Subtraction, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::MinusEquals:
        return parse_assignment_expression(AssignmentOp::SubtractionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Asterisk:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Multiplication, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AsteriskEquals:
        return parse_assignment_expression(AssignmentOp::MultiplicationAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Slash:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Division, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::SlashEquals:
        return parse_assignment_expression(AssignmentOp::DivisionAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Percent:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Modulo, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PercentEquals:
        return parse_assignment_expression(AssignmentOp::ModuloAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoubleAsterisk:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::Exponentiation, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleAsteriskEquals:
        return parse_assignment_expression(AssignmentOp::ExponentiationAssignment, move(lhs), min_precedence, associativity);
    case TokenType::GreaterThan:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::GreaterThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::GreaterThanEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::GreaterThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThan:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LessThan, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::LessThanEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LessThanEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::TypedEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::TypedInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::EqualsEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::AbstractEquals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ExclamationMarkEquals:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::AbstractInequals, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::In:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::In, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Instanceof:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::InstanceOf, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::Ampersand:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::BitwiseAnd, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::AmpersandEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseAndAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Pipe:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::BitwiseOr, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::PipeEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseOrAssignment, move(lhs), min_precedence, associativity);
    case TokenType::Caret:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::BitwiseXor, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::CaretEquals:
        return parse_assignment_expression(AssignmentOp::BitwiseXorAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ShiftLeft:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::LeftShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ShiftLeftEquals:
        return parse_assignment_expression(AssignmentOp::LeftShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::ShiftRight:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::RightShift, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::ShiftRightEquals:
        return parse_assignment_expression(AssignmentOp::RightShiftAssignment, move(lhs), min_precedence, associativity);
    case TokenType::UnsignedShiftRight:
        consume();
        return create_ast_node<BinaryExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, BinaryOp::UnsignedRightShift, move(lhs), parse_expression(min_precedence, associativity));
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
        return create_ast_node<MemberExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), create_ast_node<Identifier>({ m_state.current_token.filename(), rule_start.position(), position() }, consume().value()));
    case TokenType::BracketOpen: {
        consume(TokenType::BracketOpen);
        auto expression = create_ast_node<MemberExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), parse_expression(0), true);
        consume(TokenType::BracketClose);
        return expression;
    }
    case TokenType::PlusPlus:
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for foo()++
        if (!is<Identifier>(*lhs) && !is<MemberExpression>(*lhs))
            syntax_error(String::formatted("Left-hand side of postfix increment operator must be identifier or member expression, got {}", lhs->class_name()));

        if (m_state.strict_mode && is<Identifier>(*lhs)) {
            auto& identifier = static_cast<Identifier&>(*lhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }

        consume();
        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Increment, move(lhs));
    case TokenType::MinusMinus:
        // FIXME: Apparently for functions this should also not be enforced on a parser level,
        // other engines throw ReferenceError for foo()--
        if (!is<Identifier>(*lhs) && !is<MemberExpression>(*lhs))
            syntax_error(String::formatted("Left-hand side of postfix increment operator must be identifier or member expression, got {}", lhs->class_name()));

        if (m_state.strict_mode && is<Identifier>(*lhs)) {
            auto& identifier = static_cast<Identifier&>(*lhs);
            auto& name = identifier.string();
            check_identifier_name_for_assignment_validity(name);
        }
        consume();
        return create_ast_node<UpdateExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, UpdateOp::Decrement, move(lhs));
    case TokenType::DoubleAmpersand:
        consume();
        return create_ast_node<LogicalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, LogicalOp::And, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleAmpersandEquals:
        return parse_assignment_expression(AssignmentOp::AndAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoublePipe:
        consume();
        return create_ast_node<LogicalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, LogicalOp::Or, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoublePipeEquals:
        return parse_assignment_expression(AssignmentOp::OrAssignment, move(lhs), min_precedence, associativity);
    case TokenType::DoubleQuestionMark:
        consume();
        return create_ast_node<LogicalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, LogicalOp::NullishCoalescing, move(lhs), parse_expression(min_precedence, associativity));
    case TokenType::DoubleQuestionMarkEquals:
        return parse_assignment_expression(AssignmentOp::NullishAssignment, move(lhs), min_precedence, associativity);
    case TokenType::QuestionMark:
        return parse_conditional_expression(move(lhs));
    default:
        expected("secondary expression");
        consume();
        return create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() });
    }
}

NonnullRefPtr<AssignmentExpression> Parser::parse_assignment_expression(AssignmentOp assignment_op, NonnullRefPtr<Expression> lhs, int min_precedence, Associativity associativity)
{
    auto rule_start = push_start();
    VERIFY(match(TokenType::Equals)
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

    if (assignment_op == AssignmentOp::Assignment) {
        auto synthesize_binding_pattern = [this](Expression const& expression) -> RefPtr<BindingPattern> {
            // Clear any syntax error that has occurred in the range that 'expression' spans.
            m_state.errors.remove_all_matching([range = expression.source_range()](auto const& error) {
                return error.position.has_value() && range.contains(*error.position);
            });
            // Make a parser and parse the source for this expression as a binding pattern.
            auto source = m_state.lexer.source().substring_view(expression.source_range().start.offset - 2, expression.source_range().end.offset - expression.source_range().start.offset);
            Lexer lexer { source, m_state.lexer.filename(), expression.source_range().start.line, expression.source_range().start.column };
            Parser parser { lexer };

            parser.m_state.strict_mode = m_state.strict_mode;
            parser.m_state.allow_super_property_lookup = m_state.allow_super_property_lookup;
            parser.m_state.allow_super_constructor_call = m_state.allow_super_constructor_call;
            parser.m_state.in_function_context = m_state.in_function_context;
            parser.m_state.in_generator_function_context = m_state.in_generator_function_context;
            parser.m_state.in_arrow_function_context = m_state.in_arrow_function_context;
            parser.m_state.in_break_context = m_state.in_break_context;
            parser.m_state.in_continue_context = m_state.in_continue_context;
            parser.m_state.string_legacy_octal_escape_sequence_in_scope = m_state.string_legacy_octal_escape_sequence_in_scope;

            auto result = parser.parse_binding_pattern();
            if (parser.has_errors())
                m_state.errors.extend(parser.errors());
            return result;
        };
        if (is<ArrayExpression>(*lhs) || is<ObjectExpression>(*lhs)) {
            auto binding_pattern = synthesize_binding_pattern(*lhs);
            if (binding_pattern) {
                auto rhs = parse_expression(min_precedence, associativity);
                return create_ast_node<AssignmentExpression>(
                    { m_state.current_token.filename(), rule_start.position(), position() },
                    assignment_op,
                    binding_pattern.release_nonnull(),
                    move(rhs));
            }
        }
    }
    if (!is<Identifier>(*lhs) && !is<MemberExpression>(*lhs) && !is<CallExpression>(*lhs)) {
        syntax_error("Invalid left-hand side in assignment");
    } else if (m_state.strict_mode && is<Identifier>(*lhs)) {
        auto name = static_cast<const Identifier&>(*lhs).string();
        check_identifier_name_for_assignment_validity(name);
    } else if (m_state.strict_mode && is<CallExpression>(*lhs)) {
        syntax_error("Cannot assign to function call");
    }
    auto rhs = parse_expression(min_precedence, associativity);
    if (assignment_op == AssignmentOp::Assignment && is<FunctionExpression>(*rhs)) {
        auto ident = lhs;
        if (is<MemberExpression>(*lhs)) {
            ident = static_cast<MemberExpression&>(*lhs).property();
        }
        if (is<Identifier>(*ident))
            static_cast<FunctionExpression&>(*rhs).set_name_if_possible(static_cast<Identifier&>(*ident).string());
    }
    return create_ast_node<AssignmentExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, assignment_op, move(lhs), move(rhs));
}

NonnullRefPtr<Identifier> Parser::parse_identifier()
{
    auto identifier_start = position();
    auto token = consume_identifier();
    return create_ast_node<Identifier>(
        { m_state.current_token.filename(), identifier_start, position() },
        token.value());
}

NonnullRefPtr<CallExpression> Parser::parse_call_expression(NonnullRefPtr<Expression> lhs)
{
    auto rule_start = push_start();
    if (!m_state.allow_super_constructor_call && is<SuperExpression>(*lhs))
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

    if (is<SuperExpression>(*lhs))
        return create_ast_node<SuperCall>({ m_state.current_token.filename(), rule_start.position(), position() }, move(arguments));

    return create_ast_node<CallExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), move(arguments));
}

NonnullRefPtr<NewExpression> Parser::parse_new_expression()
{
    auto rule_start = push_start();
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

    return create_ast_node<NewExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(callee), move(arguments));
}

NonnullRefPtr<YieldExpression> Parser::parse_yield_expression()
{
    auto rule_start = push_start();
    consume(TokenType::Yield);
    RefPtr<Expression> argument;
    bool yield_from = false;

    if (!m_state.current_token.trivia_contains_line_terminator()) {
        if (match(TokenType::Asterisk)) {
            consume();
            yield_from = true;
        }

        if (yield_from || match_expression())
            argument = parse_expression(0);
    }

    return create_ast_node<YieldExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(argument), yield_from);
}

NonnullRefPtr<ReturnStatement> Parser::parse_return_statement()
{
    auto rule_start = push_start();
    if (!m_state.in_function_context && !m_state.in_arrow_function_context)
        syntax_error("'return' not allowed outside of a function");

    consume(TokenType::Return);

    // Automatic semicolon insertion: terminate statement when return is followed by newline
    if (m_state.current_token.trivia_contains_line_terminator())
        return create_ast_node<ReturnStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, nullptr);

    if (match_expression()) {
        auto expression = parse_expression(0);
        consume_or_insert_semicolon();
        return create_ast_node<ReturnStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression));
    }

    consume_or_insert_semicolon();
    return create_ast_node<ReturnStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, nullptr);
}

NonnullRefPtr<BlockStatement> Parser::parse_block_statement()
{
    auto rule_start = push_start();
    bool dummy = false;
    return parse_block_statement(dummy);
}

NonnullRefPtr<BlockStatement> Parser::parse_block_statement(bool& is_strict, bool error_on_binding)
{
    auto rule_start = push_start();
    ScopePusher scope(*this, ScopePusher::Let, Parser::Scope::Block);
    auto block = create_ast_node<BlockStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
    consume(TokenType::CurlyOpen);

    bool initial_strict_mode_state = m_state.strict_mode;
    if (initial_strict_mode_state)
        is_strict = true;

    bool parsing_directives = true;
    while (!done() && !match(TokenType::CurlyClose)) {
        if (match_declaration()) {
            block->append(parse_declaration());
            parsing_directives = false;
        } else if (match_statement()) {
            auto statement = parse_statement(AllowLabelledFunction::Yes);
            block->append(statement);
            if (statement_is_use_strict_directive(statement)) {
                if (parsing_directives) {
                    if (!initial_strict_mode_state) {
                        is_strict = true;
                        m_state.strict_mode = true;
                    }
                }
                if (m_state.string_legacy_octal_escape_sequence_in_scope)
                    syntax_error("Octal escape sequence in string literal not allowed in strict mode");

                if (error_on_binding) {
                    syntax_error("Illegal 'use strict' directive in function with non-simple parameter list");
                }
            }

            if (parsing_directives && is<ExpressionStatement>(*statement)) {
                auto& expression_statement = static_cast<ExpressionStatement&>(*statement);
                auto& expression = expression_statement.expression();
                parsing_directives = is<StringLiteral>(expression);
            } else {
                parsing_directives = false;
            }
        } else {
            expected("statement or declaration");
            consume();
            parsing_directives = false;
        }
    }
    m_state.strict_mode = initial_strict_mode_state;
    m_state.string_legacy_octal_escape_sequence_in_scope = false;
    consume(TokenType::CurlyClose);
    scope.add_to_scope_node(block);
    return block;
}

template<typename FunctionNodeType>
NonnullRefPtr<FunctionNodeType> Parser::parse_function_node(u8 parse_options)
{
    auto rule_start = push_start();
    VERIFY(!(parse_options & FunctionNodeParseOptions::IsGetterFunction && parse_options & FunctionNodeParseOptions::IsSetterFunction));

    TemporaryChange super_property_access_rollback(m_state.allow_super_property_lookup, !!(parse_options & FunctionNodeParseOptions::AllowSuperPropertyLookup));
    TemporaryChange super_constructor_call_rollback(m_state.allow_super_constructor_call, !!(parse_options & FunctionNodeParseOptions::AllowSuperConstructorCall));

    ScopePusher scope(*this, ScopePusher::Var, Parser::Scope::Function);

    constexpr auto is_function_expression = IsSame<FunctionNodeType, FunctionExpression>;
    auto is_generator = (parse_options & FunctionNodeParseOptions::IsGeneratorFunction) != 0;
    String name;
    if (parse_options & FunctionNodeParseOptions::CheckForFunctionAndName) {
        consume(TokenType::Function);
        if (!is_generator) {
            is_generator = match(TokenType::Asterisk);
            if (is_generator) {
                consume(TokenType::Asterisk);
                parse_options = parse_options | FunctionNodeParseOptions::IsGeneratorFunction;
            }
        }

        if (FunctionNodeType::must_have_name() || match_identifier())
            name = consume_identifier().value();
        else if (is_function_expression && (match(TokenType::Yield) || match(TokenType::Await)))
            name = consume().value();

        check_identifier_name_for_assignment_validity(name);
    }
    consume(TokenType::ParenOpen);
    i32 function_length = -1;
    auto parameters = parse_formal_parameters(function_length, parse_options);
    consume(TokenType::ParenClose);

    if (function_length == -1)
        function_length = parameters.size();

    TemporaryChange change(m_state.in_function_context, true);
    TemporaryChange generator_change(m_state.in_generator_function_context, m_state.in_generator_function_context || is_generator);
    auto old_labels_in_scope = move(m_state.labels_in_scope);
    ScopeGuard guard([&]() {
        m_state.labels_in_scope = move(old_labels_in_scope);
    });

    m_state.function_parameters.append(parameters);

    bool has_binding = any_of(parameters, [](FunctionNode::Parameter const& parameter) {
        return parameter.binding.has<NonnullRefPtr<BindingPattern>>();
    });

    bool is_strict = false;
    auto body = parse_block_statement(is_strict, has_binding);

    // If the function contains 'use strict' we need to check the parameters (again).
    if (is_strict) {
        Vector<StringView> parameter_names;
        for (auto& parameter : parameters) {
            parameter.binding.visit(
                [&](FlyString const& parameter_name) {
                    check_identifier_name_for_assignment_validity(parameter_name, true);
                    for (auto& previous_name : parameter_names) {
                        if (previous_name == parameter_name) {
                            syntax_error(String::formatted("Duplicate parameter '{}' not allowed in strict mode", parameter_name));
                        }
                    }

                    parameter_names.append(parameter_name);
                },
                [&](NonnullRefPtr<BindingPattern> const& binding) {
                    binding->for_each_bound_name([&](auto& bound_name) {
                        for (auto& previous_name : parameter_names) {
                            if (previous_name == bound_name) {
                                syntax_error(String::formatted("Duplicate parameter '{}' not allowed in strict mode", bound_name));
                                break;
                            }
                        }
                        parameter_names.append(bound_name);
                    });
                });
        }
        check_identifier_name_for_assignment_validity(name, true);
    }

    m_state.function_parameters.take_last();

    scope.add_to_scope_node(body);

    return create_ast_node<FunctionNodeType>(
        { m_state.current_token.filename(), rule_start.position(), position() },
        name, move(body), move(parameters), function_length,
        is_generator ? FunctionKind::Generator : FunctionKind::Regular, is_strict);
}

Vector<FunctionNode::Parameter> Parser::parse_formal_parameters(int& function_length, u8 parse_options)
{
    auto rule_start = push_start();
    bool has_default_parameter = false;
    bool has_rest_parameter = false;

    Vector<FunctionNode::Parameter> parameters;

    auto consume_identifier_or_binding_pattern = [&]() -> Variant<FlyString, NonnullRefPtr<BindingPattern>> {
        if (auto pattern = parse_binding_pattern())
            return pattern.release_nonnull();

        auto token = consume_identifier();
        auto parameter_name = token.value();

        check_identifier_name_for_assignment_validity(parameter_name);

        for (auto& parameter : parameters) {
            bool has_same_name = parameter.binding.visit(
                [&](FlyString const& name) {
                    return name == parameter_name;
                },
                [&](NonnullRefPtr<BindingPattern> const& bindings) {
                    bool found_duplicate = false;
                    bindings->for_each_bound_name([&](auto& bound_name) {
                        if (bound_name == parameter_name)
                            found_duplicate = true;
                    });
                    return found_duplicate;
                });

            if (!has_same_name)
                continue;

            String message;
            if (parse_options & FunctionNodeParseOptions::IsArrowFunction)
                message = String::formatted("Duplicate parameter '{}' not allowed in arrow function", parameter_name);
            else if (m_state.strict_mode)
                message = String::formatted("Duplicate parameter '{}' not allowed in strict mode", parameter_name);
            else if (has_default_parameter || match(TokenType::Equals))
                message = String::formatted("Duplicate parameter '{}' not allowed in function with default parameter", parameter_name);
            else if (has_rest_parameter)
                message = String::formatted("Duplicate parameter '{}' not allowed in function with rest parameter", parameter_name);
            if (!message.is_empty())
                syntax_error(message, Position { token.line_number(), token.line_column() });
            break;
        }
        return FlyString { token.value() };
    };

    while (match(TokenType::CurlyOpen) || match(TokenType::BracketOpen) || match_identifier() || match(TokenType::TripleDot)) {
        if (parse_options & FunctionNodeParseOptions::IsGetterFunction)
            syntax_error("Getter function must have no arguments");
        if (parse_options & FunctionNodeParseOptions::IsSetterFunction && (parameters.size() >= 1 || match(TokenType::TripleDot)))
            syntax_error("Setter function must have one argument");
        auto is_rest = false;
        if (match(TokenType::TripleDot)) {
            consume();
            has_rest_parameter = true;
            function_length = parameters.size();
            is_rest = true;
        }
        auto parameter = consume_identifier_or_binding_pattern();
        RefPtr<Expression> default_value;
        if (match(TokenType::Equals)) {
            consume();
            TemporaryChange change(m_state.in_function_context, true);
            has_default_parameter = true;
            function_length = parameters.size();
            default_value = parse_expression(2);

            bool is_generator = parse_options & FunctionNodeParseOptions::IsGeneratorFunction;
            if ((is_generator || m_state.strict_mode) && default_value && default_value->fast_is<Identifier>() && static_cast<Identifier&>(*default_value).string() == "yield"sv)
                syntax_error("Generator function parameter initializer cannot contain a reference to an identifier named \"yield\"");
        }
        parameters.append({ move(parameter), default_value, is_rest });
        if (match(TokenType::ParenClose))
            break;
        consume(TokenType::Comma);
        if (is_rest)
            break;
    }
    if (parse_options & FunctionNodeParseOptions::IsSetterFunction && parameters.is_empty())
        syntax_error("Setter function must have one argument");
    return parameters;
}

static constexpr AK::Array<StringView, 36> s_reserved_words = { "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof", "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void", "while", "with" };
RefPtr<BindingPattern> Parser::parse_binding_pattern()
{
    auto rule_start = push_start();

    TokenType closing_token;
    bool is_object = true;

    if (match(TokenType::BracketOpen)) {
        consume();
        closing_token = TokenType::BracketClose;
        is_object = false;
    } else if (match(TokenType::CurlyOpen)) {
        consume();
        closing_token = TokenType::CurlyClose;
    } else {
        return {};
    }

    Vector<BindingPattern::BindingEntry> entries;

    while (!match(closing_token)) {
        if (!is_object && match(TokenType::Comma)) {
            consume();
            entries.append(BindingPattern::BindingEntry {});
            continue;
        }

        auto is_rest = false;

        if (match(TokenType::TripleDot)) {
            consume();
            is_rest = true;
        }

        decltype(BindingPattern::BindingEntry::name) name = Empty {};
        decltype(BindingPattern::BindingEntry::alias) alias = Empty {};
        RefPtr<Expression> initializer = {};

        if (is_object) {
            if (match_identifier_name()) {
                name = create_ast_node<Identifier>(
                    { m_state.current_token.filename(), rule_start.position(), position() },
                    consume().value());
            } else if (match(TokenType::BracketOpen)) {
                consume();
                name = parse_expression(0);
                consume(TokenType::BracketClose);
            } else {
                expected("identifier or computed property name");
                return {};
            }

            if (!is_rest && match(TokenType::Colon)) {
                consume();
                if (match(TokenType::CurlyOpen) || match(TokenType::BracketOpen)) {
                    auto binding_pattern = parse_binding_pattern();
                    if (!binding_pattern)
                        return {};
                    alias = binding_pattern.release_nonnull();
                } else if (match_identifier_name()) {
                    alias = create_ast_node<Identifier>(
                        { m_state.current_token.filename(), rule_start.position(), position() },
                        consume().value());
                } else {
                    expected("identifier or binding pattern");
                    return {};
                }
            }
        } else {
            if (match_identifier_name()) {
                // BindingElement must always have an Empty name field
                alias = create_ast_node<Identifier>(
                    { m_state.current_token.filename(), rule_start.position(), position() },
                    consume().value());
            } else if (match(TokenType::BracketOpen) || match(TokenType::CurlyOpen)) {
                auto pattern = parse_binding_pattern();
                if (!pattern) {
                    expected("binding pattern");
                    return {};
                }
                alias = pattern.release_nonnull();
            } else {
                expected("identifier or binding pattern");
                return {};
            }
        }

        if (match(TokenType::Equals)) {
            if (is_rest) {
                syntax_error("Unexpected initializer after rest element");
                return {};
            }

            consume();

            initializer = parse_expression(2);
            if (!initializer) {
                expected("initialization expression");
                return {};
            }
        }

        entries.append(BindingPattern::BindingEntry { move(name), move(alias), move(initializer), is_rest });

        if (match(TokenType::Comma)) {
            if (is_rest) {
                syntax_error("Rest element may not be followed by a comma");
                return {};
            }
            consume();
        }
    }

    while (!is_object && match(TokenType::Comma))
        consume();

    consume(closing_token);

    auto kind = is_object ? BindingPattern::Kind::Object : BindingPattern::Kind::Array;
    auto pattern = adopt_ref(*new BindingPattern);
    pattern->entries = move(entries);
    pattern->kind = kind;
    pattern->for_each_bound_name([this](auto& name) { check_identifier_name_for_assignment_validity(name); });

    return pattern;
}

NonnullRefPtr<VariableDeclaration> Parser::parse_variable_declaration(bool for_loop_variable_declaration)
{
    auto rule_start = push_start();
    DeclarationKind declaration_kind;

    switch (m_state.current_token.type()) {
    case TokenType::Var:
        declaration_kind = DeclarationKind::Var;
        break;
    case TokenType::Let:
        declaration_kind = DeclarationKind::Let;
        break;
    case TokenType::Const:
        declaration_kind = DeclarationKind::Const;
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    consume();

    NonnullRefPtrVector<VariableDeclarator> declarations;
    for (;;) {
        Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>, Empty> target { Empty() };
        if (match_identifier()) {
            auto identifier_start = push_start();
            auto name = consume_identifier().value();
            target = create_ast_node<Identifier>(
                { m_state.current_token.filename(), rule_start.position(), position() },
                name);
            check_identifier_name_for_assignment_validity(name);
            if ((declaration_kind == DeclarationKind::Let || declaration_kind == DeclarationKind::Const) && name == "let"sv)
                syntax_error("Lexical binding may not be called 'let'");

            // Check we do not have duplicates
            auto check_declarations = [&](VariableDeclarator const& declarator) {
                declarator.target().visit([&](NonnullRefPtr<Identifier> const& identifier) {
                    if (identifier->string() == name)
                        syntax_error(String::formatted("Identifier '{}' has already been declared", name), identifier_start.position()); },
                    [&](auto const&) {});
            };

            // In any previous let scope
            if (!m_state.let_scopes.is_empty()) {
                for (auto& decls : m_state.let_scopes.last()) {
                    for (auto& decl : decls.declarations()) {
                        check_declarations(decl);
                    }
                }
            }

            // or this declaration
            if (declaration_kind == DeclarationKind::Let || declaration_kind == DeclarationKind::Const) {
                // FIXME: We should check the var_scopes here as well however this has edges cases with for loops.
                //        See duplicated-variable-declarations.js.

                for (auto& declaration : declarations) {
                    check_declarations(declaration);
                }
            }
        } else if (auto pattern = parse_binding_pattern()) {
            target = pattern.release_nonnull();

            if ((declaration_kind == DeclarationKind::Let || declaration_kind == DeclarationKind::Const)) {
                target.get<NonnullRefPtr<BindingPattern>>()->for_each_bound_name([this](auto& name) {
                    if (name == "let"sv)
                        syntax_error("Lexical binding may not be called 'let'");
                });
            }
        } else if (!m_state.in_generator_function_context && match(TokenType::Yield)) {
            if (m_state.strict_mode)
                syntax_error("Identifier must not be a reserved word in strict mode ('yield')");

            target = create_ast_node<Identifier>(
                { m_state.current_token.filename(), rule_start.position(), position() },
                consume().value());
        }

        if (target.has<Empty>()) {
            expected("identifier or a binding pattern");
            if (match(TokenType::Comma)) {
                consume();
                continue;
            }
            break;
        }

        RefPtr<Expression> init;
        if (match(TokenType::Equals)) {
            consume();
            init = parse_expression(2);
        } else if (!for_loop_variable_declaration && declaration_kind == DeclarationKind::Const) {
            syntax_error("Missing initializer in 'const' variable declaration");
        } else if (!for_loop_variable_declaration && target.has<NonnullRefPtr<BindingPattern>>()) {
            syntax_error("Missing initializer in destructuring assignment");
        }

        if (init && is<FunctionExpression>(*init) && target.has<NonnullRefPtr<Identifier>>()) {
            static_cast<FunctionExpression&>(*init).set_name_if_possible(target.get<NonnullRefPtr<Identifier>>()->string());
        }

        declarations.append(create_ast_node<VariableDeclarator>(
            { m_state.current_token.filename(), rule_start.position(), position() },
            move(target).downcast<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>>(),
            move(init)));

        if (match(TokenType::Comma)) {
            consume();
            continue;
        }
        break;
    }
    if (!for_loop_variable_declaration)
        consume_or_insert_semicolon();

    auto declaration = create_ast_node<VariableDeclaration>({ m_state.current_token.filename(), rule_start.position(), position() }, declaration_kind, move(declarations));
    if (declaration_kind == DeclarationKind::Var) {
        m_state.var_scopes.last().append(declaration);
    } else {
        m_state.let_scopes.last().append(declaration);

        for (auto& declarator : declaration->declarations()) {
            declarator.target().visit(
                [&](const NonnullRefPtr<Identifier>& id) {
                    m_state.current_scope->lexical_declarations.set(id->string());
                },
                [&](const NonnullRefPtr<BindingPattern>& binding) {
                    binding->for_each_bound_name([&](const auto& name) {
                        m_state.current_scope->lexical_declarations.set(name);
                    });
                });
        }
    }
    return declaration;
}

NonnullRefPtr<ThrowStatement> Parser::parse_throw_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Throw);

    // Automatic semicolon insertion: terminate statement when throw is followed by newline
    if (m_state.current_token.trivia_contains_line_terminator()) {
        syntax_error("No line break is allowed between 'throw' and its expression");
        return create_ast_node<ThrowStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, create_ast_node<ErrorExpression>({ m_state.current_token.filename(), rule_start.position(), position() }));
    }

    auto expression = parse_expression(0);
    consume_or_insert_semicolon();
    return create_ast_node<ThrowStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression));
}

NonnullRefPtr<BreakStatement> Parser::parse_break_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Break);
    FlyString target_label;
    if (match(TokenType::Semicolon)) {
        consume();
    } else {
        if (match(TokenType::Identifier) && !m_state.current_token.trivia_contains_line_terminator()) {
            target_label = consume().value();
            if (!m_state.labels_in_scope.contains(target_label))
                syntax_error(String::formatted("Label '{}' not found", target_label));
        }
        consume_or_insert_semicolon();
    }

    if (target_label.is_null() && !m_state.in_break_context)
        syntax_error("Unlabeled 'break' not allowed outside of a loop or switch statement");

    return create_ast_node<BreakStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, target_label);
}

NonnullRefPtr<ContinueStatement> Parser::parse_continue_statement()
{
    auto rule_start = push_start();
    if (!m_state.in_continue_context)
        syntax_error("'continue' not allow outside of a loop");

    consume(TokenType::Continue);
    FlyString target_label;
    if (match(TokenType::Semicolon)) {
        consume();
        return create_ast_node<ContinueStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, target_label);
    }
    if (match(TokenType::Identifier) && !m_state.current_token.trivia_contains_line_terminator()) {
        target_label = consume().value();
        if (!m_state.labels_in_scope.contains(target_label))
            syntax_error(String::formatted("Label '{}' not found", target_label));
    }
    consume_or_insert_semicolon();
    return create_ast_node<ContinueStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, target_label);
}

NonnullRefPtr<ConditionalExpression> Parser::parse_conditional_expression(NonnullRefPtr<Expression> test)
{
    auto rule_start = push_start();
    consume(TokenType::QuestionMark);
    auto consequent = parse_expression(2);
    consume(TokenType::Colon);
    auto alternate = parse_expression(2);
    return create_ast_node<ConditionalExpression>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test), move(consequent), move(alternate));
}

NonnullRefPtr<TryStatement> Parser::parse_try_statement()
{
    auto rule_start = push_start();
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

    if (!handler && !finalizer)
        syntax_error("try statement must have a 'catch' or 'finally' clause");

    return create_ast_node<TryStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(block), move(handler), move(finalizer));
}

NonnullRefPtr<DoWhileStatement> Parser::parse_do_while_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Do);

    auto body = [&]() -> NonnullRefPtr<Statement> {
        TemporaryChange break_change(m_state.in_break_context, true);
        TemporaryChange continue_change(m_state.in_continue_context, true);
        return parse_statement();
    }();

    consume(TokenType::While);
    consume(TokenType::ParenOpen);

    auto test = parse_expression(0);

    consume(TokenType::ParenClose);

    // Since ES 2015 a missing semicolon is inserted here, despite the regular ASI rules not applying
    if (match(TokenType::Semicolon))
        consume();

    return create_ast_node<DoWhileStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test), move(body));
}

NonnullRefPtr<WhileStatement> Parser::parse_while_statement()
{
    auto rule_start = push_start();
    consume(TokenType::While);
    consume(TokenType::ParenOpen);

    auto test = parse_expression(0);

    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_state.in_break_context, true);
    TemporaryChange continue_change(m_state.in_continue_context, true);
    auto body = parse_statement();

    return create_ast_node<WhileStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test), move(body));
}

NonnullRefPtr<SwitchStatement> Parser::parse_switch_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Switch);

    consume(TokenType::ParenOpen);
    auto determinant = parse_expression(0);
    consume(TokenType::ParenClose);

    consume(TokenType::CurlyOpen);

    NonnullRefPtrVector<SwitchCase> cases;

    auto has_default = false;
    while (match(TokenType::Case) || match(TokenType::Default)) {
        if (match(TokenType::Default)) {
            if (has_default)
                syntax_error("Multiple 'default' clauses in switch statement");
            has_default = true;
        }
        cases.append(parse_switch_case());
    }

    consume(TokenType::CurlyClose);

    return create_ast_node<SwitchStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(determinant), move(cases));
}

NonnullRefPtr<WithStatement> Parser::parse_with_statement()
{
    auto rule_start = push_start();
    consume(TokenType::With);
    consume(TokenType::ParenOpen);

    auto object = parse_expression(0);

    consume(TokenType::ParenClose);

    auto body = parse_statement();
    return create_ast_node<WithStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(object), move(body));
}

NonnullRefPtr<SwitchCase> Parser::parse_switch_case()
{
    auto rule_start = push_start();
    RefPtr<Expression> test;

    if (consume().type() == TokenType::Case) {
        test = parse_expression(0);
    }

    consume(TokenType::Colon);

    NonnullRefPtrVector<Statement> consequent;
    TemporaryChange break_change(m_state.in_break_context, true);
    for (;;) {
        if (match_declaration())
            consequent.append(parse_declaration());
        else if (match_statement())
            consequent.append(parse_statement());
        else
            break;
    }

    return create_ast_node<SwitchCase>({ m_state.current_token.filename(), rule_start.position(), position() }, move(test), move(consequent));
}

NonnullRefPtr<CatchClause> Parser::parse_catch_clause()
{
    auto rule_start = push_start();
    consume(TokenType::Catch);

    FlyString parameter;
    RefPtr<BindingPattern> pattern_parameter;
    auto should_expect_parameter = false;
    if (match(TokenType::ParenOpen)) {
        should_expect_parameter = true;
        consume();
        if (match_identifier_name())
            parameter = consume().value();
        else
            pattern_parameter = parse_binding_pattern();
        consume(TokenType::ParenClose);
    }

    if (should_expect_parameter && parameter.is_empty() && !pattern_parameter)
        expected("an identifier or a binding pattern");

    if (pattern_parameter)
        pattern_parameter->for_each_bound_name([this](auto& name) { check_identifier_name_for_assignment_validity(name); });

    if (!parameter.is_empty())
        check_identifier_name_for_assignment_validity(parameter);

    auto body = parse_block_statement();
    if (pattern_parameter) {
        return create_ast_node<CatchClause>(
            { m_state.current_token.filename(), rule_start.position(), position() },
            pattern_parameter.release_nonnull(),
            move(body));
    }

    return create_ast_node<CatchClause>(
        { m_state.current_token.filename(), rule_start.position(), position() },
        move(parameter),
        move(body));
}

NonnullRefPtr<IfStatement> Parser::parse_if_statement()
{
    auto rule_start = push_start();
    auto parse_function_declaration_as_block_statement = [&] {
        // https://tc39.es/ecma262/#sec-functiondeclarations-in-ifstatement-statement-clauses
        // Code matching this production is processed as if each matching occurrence of
        // FunctionDeclaration[?Yield, ?Await, ~Default] was the sole StatementListItem
        // of a BlockStatement occupying that position in the source code.
        ScopePusher scope(*this, ScopePusher::Let, Parser::Scope::Block);
        auto block = create_ast_node<BlockStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
        block->append(parse_declaration());
        scope.add_to_scope_node(block);
        return block;
    };

    consume(TokenType::If);
    consume(TokenType::ParenOpen);
    auto predicate = parse_expression(0);
    consume(TokenType::ParenClose);

    RefPtr<Statement> consequent;
    if (!m_state.strict_mode && match(TokenType::Function))
        consequent = parse_function_declaration_as_block_statement();
    else
        consequent = parse_statement();

    RefPtr<Statement> alternate;
    if (match(TokenType::Else)) {
        consume();
        if (!m_state.strict_mode && match(TokenType::Function))
            alternate = parse_function_declaration_as_block_statement();
        else
            alternate = parse_statement();
    }
    return create_ast_node<IfStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(predicate), move(*consequent), move(alternate));
}

NonnullRefPtr<Statement> Parser::parse_for_statement()
{
    auto rule_start = push_start();
    auto match_for_in_of = [&]() {
        return match(TokenType::In) || (match(TokenType::Identifier) && m_state.current_token.value() == "of");
    };

    consume(TokenType::For);

    consume(TokenType::ParenOpen);

    bool in_scope = false;
    ScopeGuard guard([&]() {
        if (in_scope)
            m_state.let_scopes.take_last();
    });

    RefPtr<ASTNode> init;
    if (!match(TokenType::Semicolon)) {
        if (match_variable_declaration()) {
            if (!match(TokenType::Var)) {
                m_state.let_scopes.append(NonnullRefPtrVector<VariableDeclaration>());
                in_scope = true;
            }
            init = parse_variable_declaration(true);
            if (match_for_in_of())
                return parse_for_in_of_statement(*init);
            if (static_cast<VariableDeclaration&>(*init).declaration_kind() == DeclarationKind::Const) {
                for (auto& declaration : static_cast<VariableDeclaration&>(*init).declarations()) {
                    if (!declaration.init())
                        syntax_error("Missing initializer in 'const' variable declaration");
                }
            }
        } else if (match_expression()) {
            init = parse_expression(0, Associativity::Right, { TokenType::In });
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

    TemporaryChange break_change(m_state.in_break_context, true);
    TemporaryChange continue_change(m_state.in_continue_context, true);
    auto body = parse_statement();

    return create_ast_node<ForStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(init), move(test), move(update), move(body));
}

NonnullRefPtr<Statement> Parser::parse_for_in_of_statement(NonnullRefPtr<ASTNode> lhs)
{
    auto rule_start = push_start();
    if (is<VariableDeclaration>(*lhs)) {
        auto declarations = static_cast<VariableDeclaration&>(*lhs).declarations();
        if (declarations.size() > 1)
            syntax_error("multiple declarations not allowed in for..in/of");
        if (declarations.size() < 1)
            syntax_error("need exactly one variable declaration in for..in/of");
        else if (declarations.first().init() != nullptr)
            syntax_error("variable initializer not allowed in for..in/of");
    }
    auto in_or_of = consume();
    auto rhs = parse_expression(0);
    consume(TokenType::ParenClose);

    TemporaryChange break_change(m_state.in_break_context, true);
    TemporaryChange continue_change(m_state.in_continue_context, true);
    auto body = parse_statement();
    if (in_or_of.type() == TokenType::In)
        return create_ast_node<ForInStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), move(rhs), move(body));
    return create_ast_node<ForOfStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(lhs), move(rhs), move(body));
}

NonnullRefPtr<DebuggerStatement> Parser::parse_debugger_statement()
{
    auto rule_start = push_start();
    consume(TokenType::Debugger);
    consume_or_insert_semicolon();
    return create_ast_node<DebuggerStatement>({ m_state.current_token.filename(), rule_start.position(), position() });
}

bool Parser::match(TokenType type) const
{
    return m_state.current_token.type() == type;
}

bool Parser::match_expression() const
{
    auto type = m_state.current_token.type();
    return type == TokenType::BoolLiteral
        || type == TokenType::NumericLiteral
        || type == TokenType::BigIntLiteral
        || type == TokenType::StringLiteral
        || type == TokenType::TemplateLiteralStart
        || type == TokenType::NullLiteral
        || match_identifier()
        || type == TokenType::New
        || type == TokenType::CurlyOpen
        || type == TokenType::BracketOpen
        || type == TokenType::ParenOpen
        || type == TokenType::Function
        || type == TokenType::This
        || type == TokenType::Super
        || type == TokenType::RegexLiteral
        || type == TokenType::Yield
        || match_unary_prefixed_expression();
}

bool Parser::match_unary_prefixed_expression() const
{
    auto type = m_state.current_token.type();
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

bool Parser::match_secondary_expression(const Vector<TokenType>& forbidden) const
{
    auto type = m_state.current_token.type();
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
    auto type = m_state.current_token.type();
    return match_expression()
        || type == TokenType::Return
        || type == TokenType::Yield
        || type == TokenType::Do
        || type == TokenType::If
        || type == TokenType::Throw
        || type == TokenType::Try
        || type == TokenType::While
        || type == TokenType::With
        || type == TokenType::For
        || type == TokenType::CurlyOpen
        || type == TokenType::Switch
        || type == TokenType::Break
        || type == TokenType::Continue
        || type == TokenType::Var
        || type == TokenType::Debugger
        || type == TokenType::Semicolon;
}

bool Parser::match_export_or_import() const
{
    auto type = m_state.current_token.type();
    return type == TokenType::Export
        || type == TokenType::Import;
}

bool Parser::match_declaration() const
{
    auto type = m_state.current_token.type();
    return type == TokenType::Function
        || type == TokenType::Class
        || type == TokenType::Const
        || type == TokenType::Let;
}

bool Parser::match_variable_declaration() const
{
    auto type = m_state.current_token.type();
    return type == TokenType::Var
        || type == TokenType::Let
        || type == TokenType::Const;
}

bool Parser::match_identifier() const
{
    return m_state.current_token.type() == TokenType::Identifier
        || m_state.current_token.type() == TokenType::Let; // See note in Parser::parse_identifier().
}

bool Parser::match_identifier_name() const
{
    return m_state.current_token.is_identifier_name();
}

bool Parser::match_property_key() const
{
    auto type = m_state.current_token.type();
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
    auto old_token = m_state.current_token;
    m_state.current_token = m_state.lexer.next();
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
    if (m_state.current_token.trivia_contains_line_terminator())
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

Token Parser::consume_identifier()
{
    if (match(TokenType::Identifier))
        return consume(TokenType::Identifier);

    // Note that 'let' is not a reserved keyword, but our lexer considers it such
    // As it's pretty nice to have that (for syntax highlighting and such), we'll
    // special-case it here instead.
    if (match(TokenType::Let)) {
        if (m_state.strict_mode)
            syntax_error("'let' is not allowed as an identifier in strict mode");
        return consume();
    }

    expected("Identifier");
    return consume();
}

// https://tc39.es/ecma262/#prod-IdentifierReference
Token Parser::consume_identifier_reference()
{
    if (match(TokenType::Identifier))
        return consume(TokenType::Identifier);

    // See note in Parser::parse_identifier().
    if (match(TokenType::Let)) {
        if (m_state.strict_mode)
            syntax_error("'let' is not allowed as an identifier in strict mode");
        return consume();
    }

    if (match(TokenType::Yield)) {
        if (m_state.strict_mode)
            syntax_error("Identifier reference may not be 'yield' in strict mode");
        return consume();
    }

    if (match(TokenType::Await)) {
        syntax_error("Identifier reference may not be 'await'");
        return consume();
    }

    expected(Token::name(TokenType::Identifier));
    return consume();
}

Token Parser::consume(TokenType expected_type)
{
    if (!match(expected_type)) {
        expected(Token::name(expected_type));
    }
    auto token = consume();
    if (expected_type == TokenType::Identifier) {
        if (m_state.strict_mode && is_strict_reserved_word(token.value()))
            syntax_error(String::formatted("Identifier must not be a reserved word in strict mode ('{}')", token.value()));
    }
    return token;
}

Token Parser::consume_and_validate_numeric_literal()
{
    auto is_unprefixed_octal_number = [](const StringView& value) {
        return value.length() > 1 && value[0] == '0' && is_ascii_digit(value[1]);
    };
    auto literal_start = position();
    auto token = consume(TokenType::NumericLiteral);
    if (m_state.strict_mode && is_unprefixed_octal_number(token.value()))
        syntax_error("Unprefixed octal number not allowed in strict mode", literal_start);
    if (match_identifier_name() && m_state.current_token.trivia().is_empty())
        syntax_error("Numeric literal must not be immediately followed by identifier");
    return token;
}

void Parser::expected(const char* what)
{
    auto message = m_state.current_token.message();
    if (message.is_empty())
        message = String::formatted("Unexpected token {}. Expected {}", m_state.current_token.name(), what);
    syntax_error(message);
}

Position Parser::position() const
{
    return {
        m_state.current_token.line_number(),
        m_state.current_token.line_column(),
        m_state.current_token.offset(),
    };
}

bool Parser::try_parse_arrow_function_expression_failed_at_position(const Position& position) const
{
    auto it = m_token_memoizations.find(position);
    if (it == m_token_memoizations.end())
        return false;

    return (*it).value.try_parse_arrow_function_expression_failed;
}

void Parser::set_try_parse_arrow_function_expression_failed_at_position(const Position& position, bool failed)
{
    m_token_memoizations.set(position, { failed });
}

void Parser::syntax_error(const String& message, Optional<Position> position)
{
    if (!position.has_value())
        position = this->position();
    m_state.errors.append({ message, position });
}

void Parser::save_state()
{
    m_saved_state.append(m_state);
}

void Parser::load_state()
{
    VERIFY(!m_saved_state.is_empty());
    m_state = m_saved_state.take_last();
}

void Parser::discard_saved_state()
{
    m_saved_state.take_last();
}

void Parser::check_identifier_name_for_assignment_validity(StringView name, bool force_strict)
{
    // FIXME: this is now called from multiple places maybe the error message should be dynamic?
    if (any_of(s_reserved_words, [&](auto& value) { return name == value; })) {
        syntax_error("Binding pattern target may not be a reserved word");
    } else if (m_state.strict_mode || force_strict) {
        if (name.is_one_of("arguments"sv, "eval"sv))
            syntax_error("Binding pattern target may not be called 'arguments' or 'eval' in strict mode");
        else if (is_strict_reserved_word(name))
            syntax_error("Binding pattern target may not be called 'yield' in strict mode");
    }
}

NonnullRefPtr<ImportStatement> Parser::parse_import_statement(Program& program)
{
    auto rule_start = push_start();
    if (program.type() != Program::Type::Module)
        syntax_error("Cannot use import statement outside a module");

    consume(TokenType::Import);

    if (match(TokenType::StringLiteral)) {
        auto module_name = consume(TokenType::StringLiteral).value();
        return create_ast_node<ImportStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, module_name);
    }

    auto match_imported_binding = [&] {
        return match_identifier() || match(TokenType::Yield) || match(TokenType::Await);
    };

    auto match_as = [&] {
        return match(TokenType::Identifier) && m_state.current_token.value() == "as"sv;
    };

    bool continue_parsing = true;

    struct ImportWithLocation {
        ImportStatement::ImportEntry entry;
        Position position;
    };

    Vector<ImportWithLocation> entries_with_location;

    if (match_imported_binding()) {
        auto id_position = position();
        auto bound_name = consume().value();
        entries_with_location.append({ { "default", bound_name }, id_position });

        if (match(TokenType::Comma)) {
            consume(TokenType::Comma);
        } else {
            continue_parsing = false;
        }
    }

    if (!continue_parsing) {
        // skip the rest
    } else if (match(TokenType::Asterisk)) {
        consume(TokenType::Asterisk);

        if (!match_as())
            syntax_error(String::formatted("Unexpected token: {}", m_state.current_token.name()));

        consume(TokenType::Identifier);

        if (match_imported_binding()) {
            auto namespace_position = position();
            auto namespace_name = consume().value();
            entries_with_location.append({ { "*", namespace_name }, namespace_position });
        } else {
            syntax_error(String::formatted("Unexpected token: {}", m_state.current_token.name()));
        }

    } else if (match(TokenType::CurlyOpen)) {
        consume(TokenType::CurlyOpen);

        while (!done() && !match(TokenType::CurlyClose)) {
            if (match_identifier_name()) {
                auto require_as = !match_imported_binding();
                auto name_position = position();
                auto name = consume().value();

                if (match_as()) {
                    consume(TokenType::Identifier);

                    auto alias_position = position();
                    auto alias = consume_identifier().value();
                    check_identifier_name_for_assignment_validity(alias);

                    entries_with_location.append({ { name, alias }, alias_position });
                } else if (require_as) {
                    syntax_error(String::formatted("Unexpected reserved word '{}'", name));
                } else {
                    check_identifier_name_for_assignment_validity(name);

                    entries_with_location.append({ { name, name }, name_position });
                }
            } else {
                expected("identifier");
                break;
            }

            if (!match(TokenType::Comma))
                break;

            consume(TokenType::Comma);
        }

        consume(TokenType::CurlyClose);
    } else {
        expected("import clauses");
    }

    auto from_statement = consume(TokenType::Identifier).value();
    if (from_statement != "from"sv)
        syntax_error(String::formatted("Expected 'from' got {}", from_statement));

    auto module_name = consume(TokenType::StringLiteral).value();

    Vector<ImportStatement::ImportEntry> entries;
    entries.ensure_capacity(entries_with_location.size());

    for (auto& entry : entries_with_location) {
        for (auto& import_statement : program.imports()) {
            if (import_statement.has_bound_name(entry.entry.local_name))
                syntax_error(String::formatted("Identifier '{}' already declared", entry.entry.local_name), entry.position);
        }

        for (auto& new_entry : entries) {
            if (new_entry.local_name == entry.entry.local_name)
                syntax_error(String::formatted("Identifier '{}' already declared", entry.entry.local_name), entry.position);
        }

        entries.append(move(entry.entry));
    }

    return create_ast_node<ImportStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, module_name, move(entries));
}

NonnullRefPtr<ExportStatement> Parser::parse_export_statement(Program& program)
{
    auto rule_start = push_start();
    if (program.type() != Program::Type::Module)
        syntax_error("Cannot use export statement outside a module");

    auto match_as = [&] {
        return match(TokenType::Identifier) && m_state.current_token.value() == "as"sv;
    };

    auto match_from = [&] {
        return match(TokenType::Identifier) && m_state.current_token.value() == "from"sv;
    };

    consume(TokenType::Export);

    struct EntryAndLocation {
        ExportStatement::ExportEntry entry;
        Position position;

        void to_module_request(String from_module)
        {
            entry.kind = ExportStatement::ExportEntry::Kind::ModuleRequest;
            entry.module_request = from_module;
        }
    };

    Vector<EntryAndLocation> entries_with_location;

    RefPtr<ASTNode> expression = {};

    if (match(TokenType::Default)) {
        auto default_position = position();
        consume(TokenType::Default);

        String local_name;

        if (match(TokenType::Class)) {
            auto class_expression = parse_class_expression(false);
            local_name = class_expression->name();
            expression = move(class_expression);
        } else if (match(TokenType::Function)) {
            auto func_expr = parse_function_node<FunctionExpression>();
            local_name = func_expr->name();
            expression = move(func_expr);
            // TODO: Allow async function
        } else if (match_expression()) {
            expression = parse_expression(2);
            consume_or_insert_semicolon();
            local_name = "*default*";
        } else {
            expected("Declaration or assignment expression");
        }

        entries_with_location.append({ { "default", local_name }, default_position });
    } else {
        enum FromSpecifier {
            NotAllowed,
            Optional,
            Required
        } check_for_from { NotAllowed };

        if (match(TokenType::Asterisk)) {
            auto asterisk_position = position();
            consume(TokenType::Asterisk);

            if (match_as()) {
                consume(TokenType::Identifier);
                if (match_identifier_name()) {
                    auto namespace_position = position();
                    auto exported_name = consume().value();
                    entries_with_location.append({ { exported_name, "*" }, namespace_position });
                } else {
                    expected("identifier");
                }
            } else {
                entries_with_location.append({ { {}, "*" }, asterisk_position });
            }
            check_for_from = Required;
        } else if (match_declaration()) {
            auto decl_position = position();
            auto declaration = parse_declaration();
            if (is<FunctionDeclaration>(*declaration)) {
                auto& func = static_cast<FunctionDeclaration&>(*declaration);
                entries_with_location.append({ { func.name(), func.name() }, func.source_range().start });
            } else if (is<ClassDeclaration>(*declaration)) {
                auto& class_declaration = static_cast<ClassDeclaration&>(*declaration);
                entries_with_location.append({ { class_declaration.class_name(), class_declaration.class_name() }, class_declaration.source_range().start });
            } else {
                VERIFY(is<VariableDeclaration>(*declaration));
                auto& variables = static_cast<VariableDeclaration&>(*declaration);
                for (auto& decl : variables.declarations()) {
                    decl.target().visit(
                        [&](NonnullRefPtr<Identifier> const& identifier) {
                            entries_with_location.append({ { identifier->string(), identifier->string() }, identifier->source_range().start });
                        },
                        [&](NonnullRefPtr<BindingPattern> const& binding) {
                            binding->for_each_bound_name([&](auto& name) {
                                entries_with_location.append({ { name, name }, decl_position });
                            });
                        });
                }
            }
            expression = declaration;
        } else if (match(TokenType::Var)) {
            auto variable_position = position();
            auto variable_declaration = parse_variable_declaration();
            for (auto& decl : variable_declaration->declarations()) {
                decl.target().visit(
                    [&](NonnullRefPtr<Identifier> const& identifier) {
                        entries_with_location.append({ { identifier->string(), identifier->string() }, identifier->source_range().start });
                    },
                    [&](NonnullRefPtr<BindingPattern> const& binding) {
                        binding->for_each_bound_name([&](auto& name) {
                            entries_with_location.append({ { name, name }, variable_position });
                        });
                    });
            }
            expression = variable_declaration;
        } else if (match(TokenType::CurlyOpen)) {
            consume(TokenType::CurlyOpen);

            while (!done() && !match(TokenType::CurlyClose)) {
                if (match_identifier_name()) {
                    auto identifier_position = position();
                    auto identifier = consume().value();

                    if (match_as()) {
                        consume(TokenType::Identifier);
                        if (match_identifier_name()) {
                            auto export_name = consume().value();
                            entries_with_location.append({ { export_name, identifier }, identifier_position });
                        } else {
                            expected("identifier name");
                        }
                    } else {
                        entries_with_location.append({ { identifier, identifier }, identifier_position });
                    }
                } else {
                    expected("identifier");
                    break;
                }

                if (!match(TokenType::Comma))
                    break;

                consume(TokenType::Comma);
            }

            consume(TokenType::CurlyClose);
            check_for_from = Optional;
        } else {
            syntax_error("Unexpected token 'export'", rule_start.position());
        }

        if (check_for_from != NotAllowed && match_from()) {
            consume(TokenType::Identifier);
            if (match(TokenType::StringLiteral)) {
                auto from_specifier = consume().value();
                for (auto& entry : entries_with_location)
                    entry.to_module_request(from_specifier);
            } else {
                expected("ModuleSpecifier");
            }
        } else if (check_for_from == Required) {
            expected("from");
        }

        if (check_for_from != NotAllowed)
            consume_or_insert_semicolon();
    }

    Vector<ExportStatement::ExportEntry> entries;
    entries.ensure_capacity(entries_with_location.size());

    for (auto& entry : entries_with_location) {
        for (auto& export_statement : program.exports()) {
            if (export_statement.has_export(entry.entry.export_name))
                syntax_error(String::formatted("Duplicate export with name: '{}'", entry.entry.export_name), entry.position);
        }

        for (auto& new_entry : entries) {
            if (new_entry.export_name == entry.entry.export_name)
                syntax_error(String::formatted("Duplicate export with name: '{}'", entry.entry.export_name), entry.position);
        }

        entries.append(move(entry.entry));
    }

    return create_ast_node<ExportStatement>({ m_state.current_token.filename(), rule_start.position(), position() }, move(expression), move(entries));
}

}
