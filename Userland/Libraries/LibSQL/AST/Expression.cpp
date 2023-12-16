/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibRegex/Regex.h>
#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>

namespace SQL::AST {

static constexpr auto s_posix_basic_metacharacters = ".^$*[]+\\"sv;

ResultOr<Value> NumericLiteral::evaluate(ExecutionContext&) const
{
    return Value { value() };
}

ResultOr<Value> StringLiteral::evaluate(ExecutionContext&) const
{
    return Value { value() };
}

ResultOr<Value> BooleanLiteral::evaluate(ExecutionContext&) const
{
    return Value { value() };
}

ResultOr<Value> NullLiteral::evaluate(ExecutionContext&) const
{
    return Value {};
}

ResultOr<Value> Placeholder::evaluate(ExecutionContext& context) const
{
    if (parameter_index() >= context.placeholder_values.size())
        return Result { SQLCommand::Unknown, SQLErrorCode::InvalidNumberOfPlaceholderValues };
    return context.placeholder_values[parameter_index()];
}

ResultOr<Value> NestedExpression::evaluate(ExecutionContext& context) const
{
    return expression()->evaluate(context);
}

ResultOr<Value> ChainedExpression::evaluate(ExecutionContext& context) const
{
    Vector<Value> values;
    TRY(values.try_ensure_capacity(expressions().size()));

    for (auto& expression : expressions())
        values.unchecked_append(TRY(expression->evaluate(context)));

    return Value::create_tuple(move(values));
}

ResultOr<Value> BinaryOperatorExpression::evaluate(ExecutionContext& context) const
{
    Value lhs_value = TRY(lhs()->evaluate(context));
    Value rhs_value = TRY(rhs()->evaluate(context));

    switch (type()) {
    case BinaryOperator::Concatenate: {
        if (lhs_value.type() != SQLType::Text)
            return Result { SQLCommand::Unknown, SQLErrorCode::BooleanOperatorTypeMismatch, BinaryOperator_name(type()) };

        AK::StringBuilder builder;
        builder.append(lhs_value.to_byte_string());
        builder.append(rhs_value.to_byte_string());
        return Value(builder.to_byte_string());
    }
    case BinaryOperator::Multiplication:
        return lhs_value.multiply(rhs_value);
    case BinaryOperator::Division:
        return lhs_value.divide(rhs_value);
    case BinaryOperator::Modulo:
        return lhs_value.modulo(rhs_value);
    case BinaryOperator::Plus:
        return lhs_value.add(rhs_value);
    case BinaryOperator::Minus:
        return lhs_value.subtract(rhs_value);
    case BinaryOperator::ShiftLeft:
        return lhs_value.shift_left(rhs_value);
    case BinaryOperator::ShiftRight:
        return lhs_value.shift_right(rhs_value);
    case BinaryOperator::BitwiseAnd:
        return lhs_value.bitwise_and(rhs_value);
    case BinaryOperator::BitwiseOr:
        return lhs_value.bitwise_or(rhs_value);
    case BinaryOperator::LessThan:
        return Value(lhs_value.compare(rhs_value) < 0);
    case BinaryOperator::LessThanEquals:
        return Value(lhs_value.compare(rhs_value) <= 0);
    case BinaryOperator::GreaterThan:
        return Value(lhs_value.compare(rhs_value) > 0);
    case BinaryOperator::GreaterThanEquals:
        return Value(lhs_value.compare(rhs_value) >= 0);
    case BinaryOperator::Equals:
        return Value(lhs_value.compare(rhs_value) == 0);
    case BinaryOperator::NotEquals:
        return Value(lhs_value.compare(rhs_value) != 0);
    case BinaryOperator::And: {
        auto lhs_bool_maybe = lhs_value.to_bool();
        auto rhs_bool_maybe = rhs_value.to_bool();
        if (!lhs_bool_maybe.has_value() || !rhs_bool_maybe.has_value())
            return Result { SQLCommand::Unknown, SQLErrorCode::BooleanOperatorTypeMismatch, BinaryOperator_name(type()) };

        return Value(lhs_bool_maybe.release_value() && rhs_bool_maybe.release_value());
    }
    case BinaryOperator::Or: {
        auto lhs_bool_maybe = lhs_value.to_bool();
        auto rhs_bool_maybe = rhs_value.to_bool();
        if (!lhs_bool_maybe.has_value() || !rhs_bool_maybe.has_value())
            return Result { SQLCommand::Unknown, SQLErrorCode::BooleanOperatorTypeMismatch, BinaryOperator_name(type()) };

        return Value(lhs_bool_maybe.release_value() || rhs_bool_maybe.release_value());
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

ResultOr<Value> UnaryOperatorExpression::evaluate(ExecutionContext& context) const
{
    Value expression_value = TRY(NestedExpression::evaluate(context));

    switch (type()) {
    case UnaryOperator::Plus:
        if (expression_value.type() == SQLType::Integer || expression_value.type() == SQLType::Float)
            return expression_value;
        return Result { SQLCommand::Unknown, SQLErrorCode::NumericOperatorTypeMismatch, UnaryOperator_name(type()) };
    case UnaryOperator::Minus:
        return expression_value.negate();
    case UnaryOperator::Not:
        if (expression_value.type() == SQLType::Boolean) {
            expression_value = !expression_value.to_bool().value();
            return expression_value;
        }
        return Result { SQLCommand::Unknown, SQLErrorCode::BooleanOperatorTypeMismatch, UnaryOperator_name(type()) };
    case UnaryOperator::BitwiseNot:
        return expression_value.bitwise_not();
    default:
        VERIFY_NOT_REACHED();
    }
}

ResultOr<Value> ColumnNameExpression::evaluate(ExecutionContext& context) const
{
    if (!context.current_row)
        return Result { SQLCommand::Unknown, SQLErrorCode::SyntaxError, column_name() };

    auto& descriptor = *context.current_row->descriptor();
    VERIFY(context.current_row->size() == descriptor.size());
    Optional<size_t> index_in_row;
    for (auto ix = 0u; ix < context.current_row->size(); ix++) {
        auto& column_descriptor = descriptor[ix];
        if (!table_name().is_empty() && column_descriptor.table != table_name())
            continue;
        if (column_descriptor.name == column_name()) {
            if (index_in_row.has_value())
                return Result { SQLCommand::Unknown, SQLErrorCode::AmbiguousColumnName, column_name() };

            index_in_row = ix;
        }
    }
    if (index_in_row.has_value())
        return (*context.current_row)[index_in_row.value()];

    return Result { SQLCommand::Unknown, SQLErrorCode::ColumnDoesNotExist, column_name() };
}

ResultOr<Value> MatchExpression::evaluate(ExecutionContext& context) const
{
    switch (type()) {
    case MatchOperator::Like: {
        Value lhs_value = TRY(lhs()->evaluate(context));
        Value rhs_value = TRY(rhs()->evaluate(context));

        char escape_char = '\0';
        if (escape()) {
            auto escape_str = TRY(escape()->evaluate(context)).to_byte_string();
            if (escape_str.length() != 1)
                return Result { SQLCommand::Unknown, SQLErrorCode::SyntaxError, "ESCAPE should be a single character" };
            escape_char = escape_str[0];
        }

        // Compile the pattern into a simple regex.
        // https://sqlite.org/lang_expr.html#the_like_glob_regexp_and_match_operators
        bool escaped = false;
        AK::StringBuilder builder;
        builder.append('^');
        for (auto c : rhs_value.to_byte_string()) {
            if (escape() && c == escape_char && !escaped) {
                escaped = true;
            } else if (s_posix_basic_metacharacters.contains(c)) {
                escaped = false;
                builder.append('\\');
                builder.append(c);
            } else if (c == '_' && !escaped) {
                builder.append('.');
            } else if (c == '%' && !escaped) {
                builder.append(".*"sv);
            } else {
                escaped = false;
                builder.append(c);
            }
        }
        builder.append('$');

        // FIXME: We should probably cache this regex.
        auto regex = Regex<PosixBasic>(builder.to_byte_string());
        auto result = regex.match(lhs_value.to_byte_string(), PosixFlags::Insensitive | PosixFlags::Unicode);
        return Value(invert_expression() ? !result.success : result.success);
    }
    case MatchOperator::Regexp: {
        Value lhs_value = TRY(lhs()->evaluate(context));
        Value rhs_value = TRY(rhs()->evaluate(context));

        auto regex = Regex<PosixExtended>(rhs_value.to_byte_string());
        auto err = regex.parser_result.error;
        if (err != regex::Error::NoError) {
            StringBuilder builder;
            builder.append("Regular expression: "sv);
            builder.append(get_error_string(err));

            return Result { SQLCommand::Unknown, SQLErrorCode::SyntaxError, builder.to_byte_string() };
        }

        auto result = regex.match(lhs_value.to_byte_string(), PosixFlags::Insensitive | PosixFlags::Unicode);
        return Value(invert_expression() ? !result.success : result.success);
    }
    case MatchOperator::Glob:
        return Result { SQLCommand::Unknown, SQLErrorCode::NotYetImplemented, "GLOB expression is not yet implemented"sv };
    case MatchOperator::Match:
        return Result { SQLCommand::Unknown, SQLErrorCode::NotYetImplemented, "MATCH expression is not yet implemented"sv };
    default:
        VERIFY_NOT_REACHED();
    }
}

}
