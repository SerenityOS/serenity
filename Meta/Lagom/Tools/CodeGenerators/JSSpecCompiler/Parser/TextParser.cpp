/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>

#include "Parser/SpecificationParsing.h"
#include "Parser/TextParser.h"

namespace JSSpecCompiler {

void TextParser::save_error(Variant<TokenType, StringView, CustomMessage>&& expected)
{
    if (expected.has<TokenType>() && expected.get<TokenType>() == TokenType::Invalid)
        return;
    if (m_max_parsed_tokens > m_next_token_index)
        return;
    if (m_max_parsed_tokens < m_next_token_index)
        m_suitable_continuations.clear();
    m_max_parsed_tokens = m_next_token_index;
    m_suitable_continuations.append(move(expected));
}

void TextParser::retreat()
{
    --m_next_token_index;
}

auto TextParser::rollback_point()
{
    return ArmedScopeGuard {
        [this, index = this->m_next_token_index] {
            m_next_token_index = index;
        }
    };
}

Optional<Token> TextParser::peek_token()
{
    if (m_next_token_index == m_tokens.size())
        return {};
    return m_tokens[m_next_token_index];
}

Optional<Token> TextParser::consume_token()
{
    auto result = peek_token();
    if (result.has_value())
        ++m_next_token_index;
    return result;
}

TextParseErrorOr<Token> TextParser::consume_token_with_one_of_types(std::initializer_list<TokenType> types)
{
    auto token = peek_token();
    if (token.has_value()) {
        for (TokenType type : types) {
            if (token->type == type) {
                (void)consume_token();
                return *token;
            } else {
                save_error(type);
            }
        }
    } else {
        for (TokenType type : types)
            save_error(type);
    }

    return TextParseError {};
}

TextParseErrorOr<Token> TextParser::consume_token_with_type(TokenType type)
{
    return consume_token_with_one_of_types({ type });
}

TextParseErrorOr<void> TextParser::consume_token(TokenType type, StringView data)
{
    auto token = consume_token();
    if (!token.has_value() || token->type != type || !token->data.equals_ignoring_ascii_case(data)) {
        retreat();
        save_error(data);
        return TextParseError {};
    }
    return {};
}

TextParseErrorOr<void> TextParser::consume_word(StringView word)
{
    auto token = consume_token();
    if (!token.has_value() || token->type != TokenType::Word || !token->data.equals_ignoring_ascii_case(word)) {
        retreat();
        save_error(word);
        return TextParseError {};
    }
    return {};
}

TextParseErrorOr<void> TextParser::consume_words(std::initializer_list<StringView> words)
{
    for (auto word : words)
        TRY(consume_word(word));
    return {};
}

bool TextParser::is_eof() const
{
    return m_next_token_index == m_tokens.size();
}

TextParseErrorOr<void> TextParser::expect_eof()
{
    if (!is_eof()) {
        save_error(CustomMessage { "EOF"sv });
        return TextParseError {};
    }
    return {};
}

// <record_initialization> :== (the)? <record_name> { (<name>: <value>,)* }
TextParseErrorOr<Tree> TextParser::parse_record_direct_list_initialization()
{
    auto rollback = rollback_point();

    (void)consume_word("the"sv);

    auto identifier = TRY(consume_token_with_type(TokenType::Identifier));
    TRY(consume_token_with_type(TokenType::BraceOpen));
    Vector<RecordDirectListInitialization::Argument> arguments;
    while (true) {
        auto name = TRY(consume_token_with_one_of_types({ TokenType::Identifier, TokenType::BraceClose }));

        if (name.is_bracket()) {
            break;
        } else {
            TRY(consume_token_with_type(TokenType::Colon));
            auto value = TRY(parse_expression());
            (void)consume_token_with_type(TokenType::Comma);
            arguments.append({ make_ref_counted<UnresolvedReference>(name.data), value });
        }
    }

    rollback.disarm();
    return make_ref_counted<RecordDirectListInitialization>(
        make_ref_counted<UnresolvedReference>(identifier.data), move(arguments));
}

// <function_arguments> :== '(' (<expr> (, <expr>)* )? ')'
TextParseErrorOr<Vector<Tree>> TextParser::parse_function_arguments()
{
    auto rollback = rollback_point();

    TRY(consume_token_with_type(TokenType::ParenOpen));

    if (!consume_token_with_type(TokenType::ParenClose).is_error()) {
        rollback.disarm();
        return Vector<Tree> {};
    }

    Vector<Tree> arguments;
    while (true) {
        arguments.append(TRY(parse_expression()));

        auto token = TRY(consume_token_with_one_of_types({ TokenType::ParenClose, TokenType::Comma }));
        if (token.type == TokenType::ParenClose)
            break;
    }
    rollback.disarm();
    return arguments;
}

// <list_initialization> :== « (<expr> (, <expr>)*)? »
TextParseErrorOr<Tree> TextParser::parse_list_initialization()
{
    auto rollback = rollback_point();

    TRY(consume_token_with_type(TokenType::ListStart));

    if (!consume_token_with_type(TokenType::ListEnd).is_error()) {
        rollback.disarm();
        return make_ref_counted<List>(Vector<Tree> {});
    }

    Vector<Tree> elements;
    while (true) {
        elements.append(TRY(parse_expression()));

        auto token = TRY(consume_token_with_one_of_types({ TokenType::ListEnd, TokenType::Comma }));
        if (token.type == TokenType::ListEnd)
            break;
    }
    rollback.disarm();
    return make_ref_counted<List>(move(elements));
}

TextParseErrorOr<Tree> TextParser::parse_the_this_value()
{
    auto rollback = rollback_point();

    TRY(consume_word("the"sv));
    TRY(consume_token(TokenType::WellKnownValue, "this"sv));
    TRY(consume_word("value"sv));

    rollback.disarm();
    return make_ref_counted<WellKnownNode>(WellKnownNode::Type::This);
}

// <value> :== <identifier> | <well_known_value> | <enumerator> | <number> | <string> | <list_initialization> | <record_initialization>
TextParseErrorOr<Tree> TextParser::parse_value()
{
    if (auto identifier = consume_token_with_type(TokenType::Identifier); !identifier.is_error())
        return make_ref_counted<UnresolvedReference>(identifier.release_value().data);

    if (auto well_known_value = consume_token_with_type(TokenType::WellKnownValue); !well_known_value.is_error()) {
        static constexpr struct {
            StringView name;
            WellKnownNode::Type type;
        } translations[] = {
            { "false"sv, WellKnownNode::Type::False },
            { "NewTarget"sv, WellKnownNode::Type::NewTarget },
            { "null"sv, WellKnownNode::Type::Null },
            { "this"sv, WellKnownNode::Type::This },
            { "true"sv, WellKnownNode::Type::True },
            { "undefined"sv, WellKnownNode::Type::Undefined },
        };
        for (auto [name, type] : translations)
            if (well_known_value.value().data == name)
                return make_ref_counted<WellKnownNode>(type);
        VERIFY_NOT_REACHED();
    }

    if (auto enumerator = consume_token_with_type(TokenType::Enumerator); !enumerator.is_error())
        return m_ctx.translation_unit()->get_node_for_enumerator_value(enumerator.value().data);

    if (auto number = consume_token_with_type(TokenType::Number); !number.is_error())
        return make_ref_counted<MathematicalConstant>(MUST(Crypto::BigFraction::from_string(number.value().data)));

    if (auto string = consume_token_with_type(TokenType::String); !string.is_error())
        return make_ref_counted<StringLiteral>(string.value().data);

    if (auto list_initialization = parse_list_initialization(); !list_initialization.is_error())
        return list_initialization.release_value();

    if (auto record_initialization = parse_record_direct_list_initialization(); !record_initialization.is_error())
        return record_initialization.release_value();

    if (auto the_this_value = parse_the_this_value(); !the_this_value.is_error())
        return the_this_value.release_value();

    return TextParseError {};
}

// <expr>
TextParseErrorOr<Tree> TextParser::parse_expression()
{
    auto rollback = rollback_point();

#define THROW_PARSE_ERROR_IF(expr)                                                                           \
    do {                                                                                                     \
        if (expr) {                                                                                          \
            save_error(CustomMessage { "valid expression continuation (not valid because " #expr ")"##sv }); \
            return TextParseError {};                                                                        \
        }                                                                                                    \
    } while (false)
#define THROW_PARSE_ERROR THROW_PARSE_ERROR_IF(true)

    Vector<Variant<Tree, Token>> stack;

    auto merge_stack = [&](i32 precedence) {
        if (!stack.last().has<Tree>())
            return;

        while (stack.size() >= 2) {
            auto const& maybe_operator = stack[stack.size() - 2];
            if (!maybe_operator.has<Token>())
                break;
            auto last_operator = maybe_operator.get<Token>();

            auto right = stack.last().get<Tree>();

            if (last_operator.is_unary_operator()) {
                auto operation = make_ref_counted<UnaryOperation>(last_operator.as_unary_operator(), right);
                stack.shrink(stack.size() - 2);
                stack.empend(operation);
            } else if (last_operator.is_binary_operator() && last_operator.precedence() < precedence) {
                auto left = stack[stack.size() - 3].get<Tree>();
                auto operation = make_ref_counted<BinaryOperation>(last_operator.as_binary_operator(), left, right);
                stack.shrink(stack.size() - 3);
                stack.empend(operation);
            } else {
                break;
            }
        }
    };

    auto merge_pre_merged = [&] {
        if (stack.size() < 3)
            return;

        auto const& maybe_left = stack[stack.size() - 3];
        auto const& maybe_operator = stack[stack.size() - 2];
        auto const& maybe_right = stack.last();

        if (!maybe_left.has<Tree>() || !maybe_operator.has<Token>() || !maybe_right.has<Tree>())
            return;

        auto last_operator = maybe_operator.get<Token>();
        if (!last_operator.is_pre_merged_binary_operator())
            return;

        auto expression = make_ref_counted<BinaryOperation>(last_operator.as_binary_operator(), maybe_left.get<Tree>(), maybe_right.get<Tree>());

        stack.shrink(stack.size() - 3);
        stack.empend(expression);
    };

    i32 bracket_balance = 0;

    while (true) {
        auto token_or_error = peek_token();
        if (!token_or_error.has_value())
            break;
        auto token = token_or_error.release_value();
        bool is_consumed = false;

        enum {
            NoneType,
            ExpressionType,
            PreMergedBinaryOperatorType,
            UnaryOperatorType,
            BinaryOperatorType,
            BracketType,
        } last_element_type;

        if (stack.is_empty())
            last_element_type = NoneType;
        else if (stack.last().has<Tree>())
            last_element_type = ExpressionType;
        else if (stack.last().get<Token>().is_pre_merged_binary_operator())
            last_element_type = PreMergedBinaryOperatorType;
        else if (stack.last().get<Token>().is_unary_operator())
            last_element_type = UnaryOperatorType;
        else if (stack.last().get<Token>().is_binary_operator())
            last_element_type = BinaryOperatorType;
        else if (stack.last().get<Token>().is_bracket())
            last_element_type = BracketType;
        else
            VERIFY_NOT_REACHED();

        if (token.is_ambiguous_operator()) {
            if (token.type == TokenType::AmbiguousMinus)
                token.type = last_element_type == ExpressionType ? TokenType::BinaryMinus : TokenType::UnaryMinus;
            else
                VERIFY_NOT_REACHED();
        }

        bracket_balance += token.is_opening_bracket();
        bracket_balance -= token.is_closing_bracket();

        if (bracket_balance < 0)
            break;

        if (token.type == TokenType::ParenOpen) {
            if (last_element_type == ExpressionType) {
                // This is a function call.
                auto arguments = TRY(parse_function_arguments());
                is_consumed = true;
                stack.append(Tree { make_ref_counted<FunctionCall>(stack.take_last().get<Tree>(), move(arguments)) });
                --bracket_balance;
            } else {
                // This is just an opening '(' in expression.
                stack.append(token);
            }
        } else if (token.is_pre_merged_binary_operator()) {
            THROW_PARSE_ERROR_IF(last_element_type != ExpressionType);
            stack.append(token);
        } else if (token.is_unary_operator()) {
            THROW_PARSE_ERROR_IF(last_element_type == PreMergedBinaryOperatorType);
            stack.append(token);
        } else if (token.is_binary_operator() || token.is_closing_bracket()) {
            if (bracket_balance == 0 && token.type == TokenType::Comma)
                break;

            THROW_PARSE_ERROR_IF(last_element_type != ExpressionType);

            merge_stack(token.precedence());
            if (token.is_closing_bracket()) {
                THROW_PARSE_ERROR_IF(stack.size() == 1);
                THROW_PARSE_ERROR_IF(!stack[stack.size() - 2].get<Token>().matches_with(token));
                stack.remove(stack.size() - 2);
                merge_pre_merged();
            } else {
                stack.append(token);
            }
        } else {
            if (auto expression = parse_value(); !expression.is_error()) {
                is_consumed = true;
                THROW_PARSE_ERROR_IF(last_element_type == ExpressionType);
                stack.append(expression.release_value());
                merge_pre_merged();
            } else {
                break;
            }
        }

        if (!is_consumed)
            VERIFY(consume_token().has_value());
    }

    THROW_PARSE_ERROR_IF(stack.is_empty());
    merge_stack(closing_bracket_precedence);
    THROW_PARSE_ERROR_IF(stack.size() != 1 || !stack[0].has<Tree>());

    rollback.disarm();
    return stack[0].get<Tree>();
#undef THROW_PARSE_ERROR
#undef THROW_PARSE_ERROR_IF
}

// <condition> :== <expr> | (<expr> is <expr> (or <expr>)?)
TextParseErrorOr<Tree> TextParser::parse_condition()
{
    auto rollback = rollback_point();
    auto expression = TRY(parse_expression());

    if (!consume_token_with_type(TokenType::Is).is_error()) {
        Vector compare_values { TRY(parse_expression()) };
        if (!consume_word("or"sv).is_error())
            compare_values.append(TRY(parse_expression()));

        rollback.disarm();
        return make_ref_counted<IsOneOfOperation>(expression, move(compare_values));
    }

    rollback.disarm();
    return expression;
}

// return <expr>
TextParseErrorOr<Tree> TextParser::parse_return_statement()
{
    auto rollback = rollback_point();

    TRY(consume_word("return"sv));
    auto return_value = TRY(parse_expression());

    rollback.disarm();
    return make_ref_counted<ReturnNode>(return_value);
}

// assert: <condition>
TextParseErrorOr<Tree> TextParser::parse_assert()
{
    auto rollback = rollback_point();

    TRY(consume_token(TokenType::Identifier, "assert"sv));
    TRY(consume_token_with_type(TokenType::Colon));
    auto condition = TRY(parse_condition());

    rollback.disarm();
    return make_ref_counted<AssertExpression>(condition);
}

// (let <expr> be <expr>) | (set <expr> to <expr>)
TextParseErrorOr<Tree> TextParser::parse_assignment()
{
    auto rollback = rollback_point();

    bool is_let = !consume_word("let"sv).is_error();
    if (!is_let)
        TRY(consume_word("set"sv));
    auto lvalue = TRY(parse_expression());
    TRY(consume_word(is_let ? "be"sv : "to"sv));
    auto rvalue = TRY(parse_expression());

    rollback.disarm();
    auto op = is_let ? BinaryOperator::Declaration : BinaryOperator::Assignment;
    return make_ref_counted<BinaryOperation>(op, lvalue, rvalue);
}

// perform <expr>
TextParseErrorOr<Tree> TextParser::parse_perform()
{
    auto rollback = rollback_point();

    TRY(consume_word("perform"sv));
    auto value = TRY(parse_expression());

    rollback.disarm();
    return value;
}

// <simple_step>
TextParseErrorOr<Tree> TextParser::parse_simple_step_or_inline_if_branch()
{
    auto rollback = rollback_point();

    // Return <expr>.$
    if (auto result = parse_return_statement(); !result.is_error()) {
        TRY(consume_token_with_type(TokenType::Dot));
        TRY(expect_eof());
        rollback.disarm();
        return result.release_value();
    }

    // Assert: <expr>.$
    if (auto result = parse_assert(); !result.is_error()) {
        TRY(consume_token_with_type(TokenType::Dot));
        TRY(expect_eof());
        rollback.disarm();
        return result.release_value();
    }

    // Let <expr> be <expr>.$
    // Set <expr> to <expr>.$
    if (auto result = parse_assignment(); !result.is_error()) {
        TRY(consume_token_with_type(TokenType::Dot));
        TRY(expect_eof());
        rollback.disarm();
        return result.release_value();
    }

    // Perform <expr>.$
    if (auto result = parse_perform(); !result.is_error()) {
        TRY(consume_token_with_type(TokenType::Dot));
        TRY(expect_eof());
        rollback.disarm();
        return result.release_value();
    }

    return TextParseError {};
}

// <if_condition> :== (If <condition>) | (Else) | (Else if <condition>),
TextParseErrorOr<TextParser::IfConditionParseResult> TextParser::parse_if_beginning()
{
    auto rollback = rollback_point();

    bool is_if_branch = !consume_word("if"sv).is_error();
    NullableTree condition = nullptr;
    if (is_if_branch) {
        condition = TRY(parse_condition());
    } else {
        TRY(consume_word("else"sv));
        if (!consume_word("if"sv).is_error())
            condition = TRY(parse_condition());
    }
    TRY(consume_token_with_type(TokenType::Comma));

    rollback.disarm();
    return IfConditionParseResult { is_if_branch, condition };
}

// <inline_if> :== <if_condition> <simple_step>.$
TextParseErrorOr<Tree> TextParser::parse_inline_if_else()
{
    auto rollback = rollback_point();

    auto [is_if_branch, condition] = TRY(parse_if_beginning());
    auto then_branch = TRY(parse_simple_step_or_inline_if_branch());

    rollback.disarm();
    if (is_if_branch)
        return make_ref_counted<IfBranch>(condition.release_nonnull(), then_branch);
    return make_ref_counted<ElseIfBranch>(condition, then_branch);
}

// <if> :== <if_condition> then$ <substeps>
TextParseErrorOr<Tree> TextParser::parse_if(Tree then_branch)
{
    auto rollback = rollback_point();

    auto [is_if_branch, condition] = TRY(parse_if_beginning());
    TRY(consume_word("then"sv));
    TRY(expect_eof());

    rollback.disarm();
    if (is_if_branch)
        return make_ref_counted<IfBranch>(*condition, then_branch);
    else
        return make_ref_counted<ElseIfBranch>(condition, then_branch);
}

// <else> :== Else,$ <substeps>
TextParseErrorOr<Tree> TextParser::parse_else(Tree else_branch)
{
    auto rollback = rollback_point();

    TRY(consume_word("else"sv));
    TRY(consume_token_with_type(TokenType::Comma));
    TRY(expect_eof());

    rollback.disarm();
    return make_ref_counted<ElseIfBranch>(nullptr, else_branch);
}

// <simple_step> | <inline_if>
TextParseErrorOr<NullableTree> TextParser::parse_step_without_substeps()
{
    auto rollback = rollback_point();

    // NOTE: ...
    if (auto result = consume_word("NOTE:"sv); !result.is_error()) {
        rollback.disarm();
        return nullptr;
    }

    // <simple_step>
    if (auto result = parse_simple_step_or_inline_if_branch(); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    // <inline_if>
    if (auto result = parse_inline_if_else(); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    return TextParseError {};
}

// <if> | <else>
TextParseErrorOr<Tree> TextParser::parse_step_with_substeps(Tree substeps)
{
    auto rollback = rollback_point();

    // <if>
    if (auto result = parse_if(substeps); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    // <else>
    if (auto result = parse_else(substeps); !result.is_error()) {
        rollback.disarm();
        return result.release_value();
    }

    return TextParseError {};
}

// <qualified_name> :== <word> (. <word>)*
TextParseErrorOr<QualifiedName> TextParser::parse_qualified_name()
{
    Vector<StringView> qualified_name;
    qualified_name.append(TRY(consume_token_with_type(TokenType::Word)).data);
    while (true) {
        auto token_or_error = consume_token_with_type(TokenType::MemberAccess);
        if (token_or_error.is_error())
            return QualifiedName { qualified_name };
        qualified_name.append(TRY(consume_token_with_type(TokenType::Word)).data);
    }
}

// <function_arguments> :== '(' (<word> (, <word>)*)? ')'
TextParseErrorOr<Vector<FunctionArgument>> TextParser::parse_function_arguments_in_declaration()
{
    TRY(consume_token_with_type(TokenType::ParenOpen));

    Vector<FunctionArgument> arguments;
    size_t optional_arguments_group = 0;

    while (true) {
        Token token = TRY(consume_token_with_one_of_types({
            TokenType::SquareBracketOpen,
            arguments.is_empty() ? TokenType::Identifier : TokenType::Comma,
            !optional_arguments_group ? TokenType::ParenClose : TokenType::Invalid,
            optional_arguments_group ? TokenType::SquareBracketClose : TokenType::Invalid,
        }));

        StringView identifier;

        if (token.type == TokenType::SquareBracketClose) {
            VERIFY(optional_arguments_group != 0);
            for (size_t i = 1; i < optional_arguments_group; ++i)
                TRY(consume_token_with_type(TokenType::SquareBracketClose));
            TRY(consume_token_with_type(TokenType::ParenClose));
            break;
        } else if (token.type == TokenType::ParenClose) {
            VERIFY(optional_arguments_group == 0);
            break;
        } else if (token.type == TokenType::SquareBracketOpen) {
            ++optional_arguments_group;
            if (!arguments.is_empty())
                TRY(consume_token_with_type(TokenType::Comma));
            identifier = TRY(consume_token_with_type(TokenType::Identifier)).data;
        } else if (token.type == TokenType::Comma) {
            identifier = TRY(consume_token_with_type(TokenType::Identifier)).data;
        } else {
            VERIFY(token.type == TokenType::Identifier);
            identifier = token.data;
        }

        arguments.append({ identifier, optional_arguments_group });
    }

    return arguments;
}

// <ao_declaration> :== <word> <function_arguments> $
TextParseErrorOr<AbstractOperationDeclaration> TextParser::parse_abstract_operation_declaration()
{
    auto rollback = rollback_point();

    auto name = TRY(consume_token_with_type(TokenType::Word)).data;

    AbstractOperationDeclaration function_definition;
    function_definition.name = MUST(FlyString::from_utf8(name));
    function_definition.arguments = TRY(parse_function_arguments_in_declaration());
    TRY(expect_eof());

    rollback.disarm();
    return function_definition;
}

// <accessor_declaration> :== get <qualified_name> $
TextParseErrorOr<AccessorDeclaration> TextParser::parse_accessor_declaration()
{
    auto rollback = rollback_point();

    TRY(consume_word("get"sv));
    AccessorDeclaration accessor;
    accessor.name = TRY(parse_qualified_name());
    TRY(expect_eof());

    rollback.disarm();
    return accessor;
}

// <properties_list_declaration> :== | Properties of the <qualified_name> Prototype Object $
//                                   | Properties of the <qualified_name> Constructor $
//                                   | Properties of <qualified_name> Instances $
//                                   | The <qualified_name> Constructor $
TextParseErrorOr<ClauseHeader::PropertiesList> TextParser::parse_properties_list_declaration()
{
    auto rollback = rollback_point();

    ClauseHeader::PropertiesList properties_list;

    if (!consume_word("The"sv).is_error()) {
        properties_list.name = TRY(parse_qualified_name());
        properties_list.object_type = ClauseHeader::ObjectType::Constructor;
        TRY(consume_word("Constructor"sv));
    } else {
        TRY(consume_words({ "Properties"sv, "of"sv }));

        bool has_the = !consume_word("the"sv).is_error();

        properties_list.name = TRY(parse_qualified_name());

        if (!has_the) {
            TRY(consume_word("Instances"sv));
            properties_list.object_type = ClauseHeader::ObjectType::Instance;
        } else {
            if (consume_word("Prototype"sv).is_error()) {
                TRY(consume_word("Constructor"sv));
                properties_list.object_type = ClauseHeader::ObjectType::Constructor;
            } else {
                TRY(consume_word("Object"sv));
                properties_list.object_type = ClauseHeader::ObjectType::Prototype;
            }
        }
    }

    TRY(expect_eof());

    rollback.disarm();
    return properties_list;
}

TextParseErrorOr<MethodDeclaration> TextParser::parse_method_declaration()
{
    auto rollback = rollback_point();

    MethodDeclaration method;
    method.name = TRY(parse_qualified_name());
    method.arguments = TRY(parse_function_arguments_in_declaration());
    TRY(expect_eof());

    rollback.disarm();
    return method;
}

// <clause_header> :== <section_number> <ao_declaration> | <accessor_declaration>
TextParseErrorOr<ClauseHeader> TextParser::parse_clause_header(ClauseHasAoidAttribute clause_has_aoid_attribute)
{
    ClauseHeader result;

    auto section_number_token = TRY(consume_token_with_type(TokenType::SectionNumber));
    result.section_number = section_number_token.data;

    if (clause_has_aoid_attribute == ClauseHasAoidAttribute::Yes) {
        if (auto ao_declaration = parse_abstract_operation_declaration(); !ao_declaration.is_error()) {
            result.header = ao_declaration.release_value();
            return result;
        }
    } else {
        if (auto accessor = parse_accessor_declaration(); !accessor.is_error()) {
            result.header = accessor.release_value();
            return result;
        } else if (auto method = parse_method_declaration(); !method.is_error()) {
            result.header = method.release_value();
            return result;
        } else if (auto properties_list = parse_properties_list_declaration(); !properties_list.is_error()) {
            result.header = properties_list.release_value();
            return result;
        }
    }
    return TextParseError {};
}

FailedTextParseDiagnostic TextParser::get_diagnostic() const
{
    StringBuilder message;

    message.append("unexpected "sv);

    if (m_max_parsed_tokens == m_tokens.size()) {
        message.append("EOF"sv);
    } else {
        auto token = m_tokens[m_max_parsed_tokens];
        if (token.type == TokenType::Word)
            message.appendff("'{}'", token.data);
        else if (token.type == TokenType::Identifier)
            message.appendff("identifier '{}'", token.data);
        else
            message.append(token.name_for_diagnostic());
    }

    message.appendff(", expected ");

    size_t size = m_suitable_continuations.size();
    VERIFY(size > 0);

    for (size_t i = 0; i < size; ++i) {
        m_suitable_continuations[i].visit(
            [&](TokenType type) { message.append(token_info[to_underlying(type)].name_for_diagnostic); },
            [&](StringView word) { message.appendff("'{}'", word); },
            [&](CustomMessage continuation) { message.append(continuation.message); });

        if (i + 1 != size) {
            if (size == 2)
                message.append(" or "sv);
            else if (i + 2 == size)
                message.append(", or "sv);
            else
                message.append(", "sv);
        }
    }

    Location location = Location::global_scope();

    if (m_max_parsed_tokens < m_tokens.size()) {
        location = m_tokens[m_max_parsed_tokens].location;
    } else {
        // FIXME: Would be nice to point to the closing tag not the opening one. This is also the
        //        only place where we use m_location.
        location = m_ctx.location_from_xml_offset(m_node->offset);
    }

    return { location, MUST(message.to_string()) };
}

}
