/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>

namespace SQL::AST {

Value Expression::evaluate(ExecutionContext&) const
{
    return Value::null();
}

Value NumericLiteral::evaluate(ExecutionContext&) const
{
    Value ret(SQLType::Float);
    ret = value();
    return ret;
}

Value StringLiteral::evaluate(ExecutionContext&) const
{
    Value ret(SQLType::Text);
    ret = value();
    return ret;
}

Value NullLiteral::evaluate(ExecutionContext&) const
{
    return Value::null();
}

Value NestedExpression::evaluate(ExecutionContext& context) const
{
    return expression()->evaluate(context);
}

Value ChainedExpression::evaluate(ExecutionContext& context) const
{
    Value ret(SQLType::Tuple);
    Vector<Value> values;
    for (auto& expression : expressions()) {
        values.append(expression.evaluate(context));
    }
    ret = values;
    return ret;
}

Value UnaryOperatorExpression::evaluate(ExecutionContext& context) const
{
    Value expression_value = NestedExpression::evaluate(context);
    switch (type()) {
    case UnaryOperator::Plus:
        if (expression_value.type() == SQLType::Integer || expression_value.type() == SQLType::Float)
            return expression_value;
        // TODO: Error handling.
        VERIFY_NOT_REACHED();
    case UnaryOperator::Minus:
        if (expression_value.type() == SQLType::Integer) {
            expression_value = -int(expression_value);
            return expression_value;
        }
        if (expression_value.type() == SQLType::Float) {
            expression_value = -double(expression_value);
            return expression_value;
        }
        // TODO: Error handling.
        VERIFY_NOT_REACHED();
    case UnaryOperator::Not:
        if (expression_value.type() == SQLType::Boolean) {
            expression_value = !bool(expression_value);
            return expression_value;
        }
        // TODO: Error handling.
        VERIFY_NOT_REACHED();
    case UnaryOperator::BitwiseNot:
        if (expression_value.type() == SQLType::Integer) {
            expression_value = ~u32(expression_value);
            return expression_value;
        }
        // TODO: Error handling.
        VERIFY_NOT_REACHED();
    }
    VERIFY_NOT_REACHED();
}

Value ColumnNameExpression::evaluate(ExecutionContext& context) const
{
    auto& descriptor = *context.current_row->descriptor();
    VERIFY(context.current_row->size() == descriptor.size());
    for (auto ix = 0u; ix < context.current_row->size(); ix++) {
        auto& column_descriptor = descriptor[ix];
        if (column_descriptor.name == column_name())
            return { (*context.current_row)[ix] };
    }
    // TODO: Error handling.
    VERIFY_NOT_REACHED();
}

}
