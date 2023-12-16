/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibSQL/AST/Token.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Result.h>
#include <LibSQL/ResultSet.h>
#include <LibSQL/Type.h>

namespace SQL::AST {

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(Args&&... args)
{
    return adopt_ref(*new T(forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() = default;

protected:
    ASTNode() = default;
};

//==================================================================================================
// Language types
//==================================================================================================

class SignedNumber final : public ASTNode {
public:
    explicit SignedNumber(double value)
        : m_value(value)
    {
    }

    double value() const { return m_value; }

private:
    double m_value;
};

class TypeName : public ASTNode {
public:
    TypeName(ByteString name, Vector<NonnullRefPtr<SignedNumber>> signed_numbers)
        : m_name(move(name))
        , m_signed_numbers(move(signed_numbers))
    {
        VERIFY(m_signed_numbers.size() <= 2);
    }

    ByteString const& name() const { return m_name; }
    Vector<NonnullRefPtr<SignedNumber>> const& signed_numbers() const { return m_signed_numbers; }

private:
    ByteString m_name;
    Vector<NonnullRefPtr<SignedNumber>> m_signed_numbers;
};

class ColumnDefinition : public ASTNode {
public:
    ColumnDefinition(ByteString name, NonnullRefPtr<TypeName> type_name)
        : m_name(move(name))
        , m_type_name(move(type_name))
    {
    }

    ByteString const& name() const { return m_name; }
    NonnullRefPtr<TypeName> const& type_name() const { return m_type_name; }

private:
    ByteString m_name;
    NonnullRefPtr<TypeName> m_type_name;
};

class CommonTableExpression : public ASTNode {
public:
    CommonTableExpression(ByteString table_name, Vector<ByteString> column_names, NonnullRefPtr<Select> select_statement)
        : m_table_name(move(table_name))
        , m_column_names(move(column_names))
        , m_select_statement(move(select_statement))
    {
    }

    ByteString const& table_name() const { return m_table_name; }
    Vector<ByteString> const& column_names() const { return m_column_names; }
    NonnullRefPtr<Select> const& select_statement() const { return m_select_statement; }

private:
    ByteString m_table_name;
    Vector<ByteString> m_column_names;
    NonnullRefPtr<Select> m_select_statement;
};

class CommonTableExpressionList : public ASTNode {
public:
    CommonTableExpressionList(bool recursive, Vector<NonnullRefPtr<CommonTableExpression>> common_table_expressions)
        : m_recursive(recursive)
        , m_common_table_expressions(move(common_table_expressions))
    {
        VERIFY(!m_common_table_expressions.is_empty());
    }

    bool recursive() const { return m_recursive; }
    Vector<NonnullRefPtr<CommonTableExpression>> const& common_table_expressions() const { return m_common_table_expressions; }

private:
    bool m_recursive;
    Vector<NonnullRefPtr<CommonTableExpression>> m_common_table_expressions;
};

class QualifiedTableName : public ASTNode {
public:
    QualifiedTableName(ByteString schema_name, ByteString table_name, ByteString alias)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
    {
    }

    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }
    ByteString const& alias() const { return m_alias; }

private:
    ByteString m_schema_name;
    ByteString m_table_name;
    ByteString m_alias;
};

class ReturningClause : public ASTNode {
public:
    struct ColumnClause {
        NonnullRefPtr<Expression> expression;
        ByteString column_alias;
    };

    ReturningClause() = default;

    explicit ReturningClause(Vector<ColumnClause> columns)
        : m_columns(move(columns))
    {
    }

    bool return_all_columns() const { return m_columns.is_empty(); }
    Vector<ColumnClause> const& columns() const { return m_columns; }

private:
    Vector<ColumnClause> m_columns;
};

enum class ResultType {
    All,
    Table,
    Expression,
};

class ResultColumn : public ASTNode {
public:
    ResultColumn() = default;

    explicit ResultColumn(ByteString table_name)
        : m_type(ResultType::Table)
        , m_table_name(move(table_name))
    {
    }

    ResultColumn(NonnullRefPtr<Expression> expression, ByteString column_alias)
        : m_type(ResultType::Expression)
        , m_expression(move(expression))
        , m_column_alias(move(column_alias))
    {
    }

    ResultType type() const { return m_type; }

    bool select_from_table() const { return m_table_name.has_value(); }
    Optional<ByteString> const& table_name() const { return m_table_name; }

    bool select_from_expression() const { return !m_expression.is_null(); }
    RefPtr<Expression> const& expression() const { return m_expression; }
    ByteString const& column_alias() const { return m_column_alias; }

private:
    ResultType m_type { ResultType::All };

    Optional<ByteString> m_table_name {};

    RefPtr<Expression> m_expression {};
    ByteString m_column_alias {};
};

class GroupByClause : public ASTNode {
public:
    GroupByClause(Vector<NonnullRefPtr<Expression>> group_by_list, RefPtr<Expression> having_clause)
        : m_group_by_list(move(group_by_list))
        , m_having_clause(move(having_clause))
    {
        VERIFY(!m_group_by_list.is_empty());
    }

    Vector<NonnullRefPtr<Expression>> const& group_by_list() const { return m_group_by_list; }
    RefPtr<Expression> const& having_clause() const { return m_having_clause; }

private:
    Vector<NonnullRefPtr<Expression>> m_group_by_list;
    RefPtr<Expression> m_having_clause;
};

class TableOrSubquery : public ASTNode {
public:
    TableOrSubquery() = default;

    TableOrSubquery(ByteString schema_name, ByteString table_name, ByteString table_alias)
        : m_is_table(true)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_table_alias(move(table_alias))
    {
    }

    explicit TableOrSubquery(Vector<NonnullRefPtr<TableOrSubquery>> subqueries)
        : m_is_subquery(!subqueries.is_empty())
        , m_subqueries(move(subqueries))
    {
    }

    bool is_table() const { return m_is_table; }
    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }
    ByteString const& table_alias() const { return m_table_alias; }

    bool is_subquery() const { return m_is_subquery; }
    Vector<NonnullRefPtr<TableOrSubquery>> const& subqueries() const { return m_subqueries; }

private:
    bool m_is_table { false };
    ByteString m_schema_name {};
    ByteString m_table_name {};
    ByteString m_table_alias {};

    bool m_is_subquery { false };
    Vector<NonnullRefPtr<TableOrSubquery>> m_subqueries {};
};

class OrderingTerm : public ASTNode {
public:
    OrderingTerm(NonnullRefPtr<Expression> expression, ByteString collation_name, Order order, Nulls nulls)
        : m_expression(move(expression))
        , m_collation_name(move(collation_name))
        , m_order(order)
        , m_nulls(nulls)
    {
    }

    NonnullRefPtr<Expression> const& expression() const { return m_expression; }
    ByteString const& collation_name() const { return m_collation_name; }
    Order order() const { return m_order; }
    Nulls nulls() const { return m_nulls; }

private:
    NonnullRefPtr<Expression> m_expression;
    ByteString m_collation_name;
    Order m_order;
    Nulls m_nulls;
};

class LimitClause : public ASTNode {
public:
    LimitClause(NonnullRefPtr<Expression> limit_expression, RefPtr<Expression> offset_expression)
        : m_limit_expression(move(limit_expression))
        , m_offset_expression(move(offset_expression))
    {
    }

    NonnullRefPtr<Expression> const& limit_expression() const { return m_limit_expression; }
    RefPtr<Expression> const& offset_expression() const { return m_offset_expression; }

private:
    NonnullRefPtr<Expression> m_limit_expression;
    RefPtr<Expression> m_offset_expression;
};

//==================================================================================================
// Expressions
//==================================================================================================

struct ExecutionContext {
    NonnullRefPtr<Database> database;
    Statement const* statement { nullptr };
    ReadonlySpan<Value> placeholder_values {};
    Tuple* current_row { nullptr };
};

class Expression : public ASTNode {
public:
    virtual ResultOr<Value> evaluate(ExecutionContext&) const
    {
        return Result { SQLCommand::Unknown, SQLErrorCode::NotYetImplemented };
    }
};

class ErrorExpression final : public Expression {
};

class NumericLiteral : public Expression {
public:
    explicit NumericLiteral(double value)
        : m_value(value)
    {
    }

    double value() const { return m_value; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    double m_value;
};

class StringLiteral : public Expression {
public:
    explicit StringLiteral(ByteString value)
        : m_value(move(value))
    {
    }

    ByteString const& value() const { return m_value; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    ByteString m_value;
};

class BlobLiteral : public Expression {
public:
    explicit BlobLiteral(ByteString value)
        : m_value(move(value))
    {
    }

    ByteString const& value() const { return m_value; }

private:
    ByteString m_value;
};

class BooleanLiteral : public Expression {
public:
    explicit BooleanLiteral(bool value)
        : m_value(value)
    {
    }

    bool value() const { return m_value; }

    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    bool m_value { false };
};

class NullLiteral : public Expression {
public:
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;
};

class Placeholder : public Expression {
public:
    explicit Placeholder(size_t parameter_index)
        : m_parameter_index(parameter_index)
    {
    }

    size_t parameter_index() const { return m_parameter_index; }

    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    size_t m_parameter_index { 0 };
};

class NestedExpression : public Expression {
public:
    NonnullRefPtr<Expression> const& expression() const { return m_expression; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

protected:
    explicit NestedExpression(NonnullRefPtr<Expression> expression)
        : m_expression(move(expression))
    {
    }

private:
    NonnullRefPtr<Expression> m_expression;
};

class NestedDoubleExpression : public Expression {
public:
    NonnullRefPtr<Expression> const& lhs() const { return m_lhs; }
    NonnullRefPtr<Expression> const& rhs() const { return m_rhs; }

protected:
    NestedDoubleExpression(NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

private:
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

class InvertibleNestedExpression : public NestedExpression {
public:
    bool invert_expression() const { return m_invert_expression; }

protected:
    InvertibleNestedExpression(NonnullRefPtr<Expression> expression, bool invert_expression)
        : NestedExpression(move(expression))
        , m_invert_expression(invert_expression)
    {
    }

private:
    bool m_invert_expression;
};

class InvertibleNestedDoubleExpression : public NestedDoubleExpression {
public:
    bool invert_expression() const { return m_invert_expression; }

protected:
    InvertibleNestedDoubleExpression(NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs, bool invert_expression)
        : NestedDoubleExpression(move(lhs), move(rhs))
        , m_invert_expression(invert_expression)
    {
    }

private:
    bool m_invert_expression;
};

class ColumnNameExpression : public Expression {
public:
    ColumnNameExpression(ByteString schema_name, ByteString table_name, ByteString column_name)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_column_name(move(column_name))
    {
    }

    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }
    ByteString const& column_name() const { return m_column_name; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    ByteString m_schema_name;
    ByteString m_table_name;
    ByteString m_column_name;
};

#define __enum_UnaryOperator(S) \
    S(Minus, "-")               \
    S(Plus, "+")                \
    S(BitwiseNot, "~")          \
    S(Not, "NOT")

enum class UnaryOperator {
#undef __UnaryOperator
#define __UnaryOperator(code, name) code,
    __enum_UnaryOperator(__UnaryOperator)
#undef __UnaryOperator
};

constexpr char const* UnaryOperator_name(UnaryOperator op)
{
    switch (op) {
#undef __UnaryOperator
#define __UnaryOperator(code, name) \
    case UnaryOperator::code:       \
        return name;
        __enum_UnaryOperator(__UnaryOperator)
#undef __UnaryOperator
            default : VERIFY_NOT_REACHED();
    }
}

class UnaryOperatorExpression : public NestedExpression {
public:
    UnaryOperatorExpression(UnaryOperator type, NonnullRefPtr<Expression> expression)
        : NestedExpression(move(expression))
        , m_type(type)
    {
    }

    UnaryOperator type() const { return m_type; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    UnaryOperator m_type;
};

// Note: These are in order of highest-to-lowest operator precedence.
#define __enum_BinaryOperator(S) \
    S(Concatenate, "||")         \
    S(Multiplication, "*")       \
    S(Division, "/")             \
    S(Modulo, "%")               \
    S(Plus, "+")                 \
    S(Minus, "-")                \
    S(ShiftLeft, "<<")           \
    S(ShiftRight, ">>")          \
    S(BitwiseAnd, "&")           \
    S(BitwiseOr, "|")            \
    S(LessThan, "<")             \
    S(LessThanEquals, "<=")      \
    S(GreaterThan, ">")          \
    S(GreaterThanEquals, ">=")   \
    S(Equals, "=")               \
    S(NotEquals, "!=")           \
    S(And, "and")                \
    S(Or, "or")

enum class BinaryOperator {
#undef __BinaryOperator
#define __BinaryOperator(code, name) code,
    __enum_BinaryOperator(__BinaryOperator)
#undef __BinaryOperator
};

constexpr char const* BinaryOperator_name(BinaryOperator op)
{
    switch (op) {
#undef __BinaryOperator
#define __BinaryOperator(code, name) \
    case BinaryOperator::code:       \
        return name;
        __enum_BinaryOperator(__BinaryOperator)
#undef __BinaryOperator
            default : VERIFY_NOT_REACHED();
    }
}

class BinaryOperatorExpression : public NestedDoubleExpression {
public:
    BinaryOperatorExpression(BinaryOperator type, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : NestedDoubleExpression(move(lhs), move(rhs))
        , m_type(type)
    {
    }

    BinaryOperator type() const { return m_type; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    BinaryOperator m_type;
};

class ChainedExpression : public Expression {
public:
    explicit ChainedExpression(Vector<NonnullRefPtr<Expression>> expressions)
        : m_expressions(move(expressions))
    {
    }

    Vector<NonnullRefPtr<Expression>> const& expressions() const { return m_expressions; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    Vector<NonnullRefPtr<Expression>> m_expressions;
};

class CastExpression : public NestedExpression {
public:
    CastExpression(NonnullRefPtr<Expression> expression, NonnullRefPtr<TypeName> type_name)
        : NestedExpression(move(expression))
        , m_type_name(move(type_name))
    {
    }

    NonnullRefPtr<TypeName> const& type_name() const { return m_type_name; }

private:
    NonnullRefPtr<TypeName> m_type_name;
};

class CaseExpression : public Expression {
public:
    struct WhenThenClause {
        NonnullRefPtr<Expression> when;
        NonnullRefPtr<Expression> then;
    };

    CaseExpression(RefPtr<Expression> case_expression, Vector<WhenThenClause> when_then_clauses, RefPtr<Expression> else_expression)
        : m_case_expression(case_expression)
        , m_when_then_clauses(when_then_clauses)
        , m_else_expression(else_expression)
    {
        VERIFY(!m_when_then_clauses.is_empty());
    }

    RefPtr<Expression> const& case_expression() const { return m_case_expression; }
    Vector<WhenThenClause> const& when_then_clauses() const { return m_when_then_clauses; }
    RefPtr<Expression> const& else_expression() const { return m_else_expression; }

private:
    RefPtr<Expression> m_case_expression;
    Vector<WhenThenClause> m_when_then_clauses;
    RefPtr<Expression> m_else_expression;
};

class ExistsExpression : public Expression {
public:
    ExistsExpression(NonnullRefPtr<Select> select_statement, bool invert_expression)
        : m_select_statement(move(select_statement))
        , m_invert_expression(invert_expression)
    {
    }

    NonnullRefPtr<Select> const& select_statement() const { return m_select_statement; }
    bool invert_expression() const { return m_invert_expression; }

private:
    NonnullRefPtr<Select> m_select_statement;
    bool m_invert_expression;
};

class CollateExpression : public NestedExpression {
public:
    CollateExpression(NonnullRefPtr<Expression> expression, ByteString collation_name)
        : NestedExpression(move(expression))
        , m_collation_name(move(collation_name))
    {
    }

    ByteString const& collation_name() const { return m_collation_name; }

private:
    ByteString m_collation_name;
};

enum class MatchOperator {
    Like,
    Glob,
    Match,
    Regexp,
};

class MatchExpression : public InvertibleNestedDoubleExpression {
public:
    MatchExpression(MatchOperator type, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs, RefPtr<Expression> escape, bool invert_expression)
        : InvertibleNestedDoubleExpression(move(lhs), move(rhs), invert_expression)
        , m_type(type)
        , m_escape(move(escape))
    {
    }

    MatchOperator type() const { return m_type; }
    RefPtr<Expression> const& escape() const { return m_escape; }
    virtual ResultOr<Value> evaluate(ExecutionContext&) const override;

private:
    MatchOperator m_type;
    RefPtr<Expression> m_escape;
};

class NullExpression : public InvertibleNestedExpression {
public:
    NullExpression(NonnullRefPtr<Expression> expression, bool invert_expression)
        : InvertibleNestedExpression(move(expression), invert_expression)
    {
    }
};

class IsExpression : public InvertibleNestedDoubleExpression {
public:
    IsExpression(NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs, bool invert_expression)
        : InvertibleNestedDoubleExpression(move(lhs), move(rhs), invert_expression)
    {
    }
};

class BetweenExpression : public InvertibleNestedDoubleExpression {
public:
    BetweenExpression(NonnullRefPtr<Expression> expression, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs, bool invert_expression)
        : InvertibleNestedDoubleExpression(move(lhs), move(rhs), invert_expression)
        , m_expression(move(expression))
    {
    }

    NonnullRefPtr<Expression> const& expression() const { return m_expression; }

private:
    NonnullRefPtr<Expression> m_expression;
};

class InSelectionExpression : public InvertibleNestedExpression {
public:
    InSelectionExpression(NonnullRefPtr<Expression> expression, NonnullRefPtr<Select> select_statement, bool invert_expression)
        : InvertibleNestedExpression(move(expression), invert_expression)
        , m_select_statement(move(select_statement))
    {
    }

    NonnullRefPtr<Select> const& select_statement() const { return m_select_statement; }

private:
    NonnullRefPtr<Select> m_select_statement;
};

class InChainedExpression : public InvertibleNestedExpression {
public:
    InChainedExpression(NonnullRefPtr<Expression> expression, NonnullRefPtr<ChainedExpression> expression_chain, bool invert_expression)
        : InvertibleNestedExpression(move(expression), invert_expression)
        , m_expression_chain(move(expression_chain))
    {
    }

    NonnullRefPtr<ChainedExpression> const& expression_chain() const { return m_expression_chain; }

private:
    NonnullRefPtr<ChainedExpression> m_expression_chain;
};

class InTableExpression : public InvertibleNestedExpression {
public:
    InTableExpression(NonnullRefPtr<Expression> expression, ByteString schema_name, ByteString table_name, bool invert_expression)
        : InvertibleNestedExpression(move(expression), invert_expression)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
    {
    }

    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }

private:
    ByteString m_schema_name;
    ByteString m_table_name;
};

//==================================================================================================
// Statements
//==================================================================================================

class Statement : public ASTNode {
public:
    ResultOr<ResultSet> execute(AK::NonnullRefPtr<Database> database, ReadonlySpan<Value> placeholder_values = {}) const;

    virtual ResultOr<ResultSet> execute(ExecutionContext&) const
    {
        return Result { SQLCommand::Unknown, SQLErrorCode::NotYetImplemented };
    }
};

class ErrorStatement final : public Statement {
};

class CreateSchema : public Statement {
public:
    CreateSchema(ByteString schema_name, bool is_error_if_schema_exists)
        : m_schema_name(move(schema_name))
        , m_is_error_if_schema_exists(is_error_if_schema_exists)
    {
    }

    ByteString const& schema_name() const { return m_schema_name; }
    bool is_error_if_schema_exists() const { return m_is_error_if_schema_exists; }

    ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    ByteString m_schema_name;
    bool m_is_error_if_schema_exists;
};

class CreateTable : public Statement {
public:
    CreateTable(ByteString schema_name, ByteString table_name, RefPtr<Select> select_statement, bool is_temporary, bool is_error_if_table_exists)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_select_statement(move(select_statement))
        , m_is_temporary(is_temporary)
        , m_is_error_if_table_exists(is_error_if_table_exists)
    {
    }

    CreateTable(ByteString schema_name, ByteString table_name, Vector<NonnullRefPtr<ColumnDefinition>> columns, bool is_temporary, bool is_error_if_table_exists)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_columns(move(columns))
        , m_is_temporary(is_temporary)
        , m_is_error_if_table_exists(is_error_if_table_exists)
    {
    }

    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }

    bool has_selection() const { return !m_select_statement.is_null(); }
    RefPtr<Select> const& select_statement() const { return m_select_statement; }

    bool has_columns() const { return !m_columns.is_empty(); }
    Vector<NonnullRefPtr<ColumnDefinition>> const& columns() const { return m_columns; }

    bool is_temporary() const { return m_is_temporary; }
    bool is_error_if_table_exists() const { return m_is_error_if_table_exists; }

    ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    ByteString m_schema_name;
    ByteString m_table_name;
    RefPtr<Select> m_select_statement;
    Vector<NonnullRefPtr<ColumnDefinition>> m_columns;
    bool m_is_temporary;
    bool m_is_error_if_table_exists;
};

class AlterTable : public Statement {
public:
    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }

protected:
    AlterTable(ByteString schema_name, ByteString table_name)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
    {
    }

private:
    ByteString m_schema_name;
    ByteString m_table_name;
};

class RenameTable : public AlterTable {
public:
    RenameTable(ByteString schema_name, ByteString table_name, ByteString new_table_name)
        : AlterTable(move(schema_name), move(table_name))
        , m_new_table_name(move(new_table_name))
    {
    }

    ByteString const& new_table_name() const { return m_new_table_name; }

private:
    ByteString m_new_table_name;
};

class RenameColumn : public AlterTable {
public:
    RenameColumn(ByteString schema_name, ByteString table_name, ByteString column_name, ByteString new_column_name)
        : AlterTable(move(schema_name), move(table_name))
        , m_column_name(move(column_name))
        , m_new_column_name(move(new_column_name))
    {
    }

    ByteString const& column_name() const { return m_column_name; }
    ByteString const& new_column_name() const { return m_new_column_name; }

private:
    ByteString m_column_name;
    ByteString m_new_column_name;
};

class AddColumn : public AlterTable {
public:
    AddColumn(ByteString schema_name, ByteString table_name, NonnullRefPtr<ColumnDefinition> column)
        : AlterTable(move(schema_name), move(table_name))
        , m_column(move(column))
    {
    }

    NonnullRefPtr<ColumnDefinition> const& column() const { return m_column; }

private:
    NonnullRefPtr<ColumnDefinition> m_column;
};

class DropColumn : public AlterTable {
public:
    DropColumn(ByteString schema_name, ByteString table_name, ByteString column_name)
        : AlterTable(move(schema_name), move(table_name))
        , m_column_name(move(column_name))
    {
    }

    ByteString const& column_name() const { return m_column_name; }

private:
    ByteString m_column_name;
};

class DropTable : public Statement {
public:
    DropTable(ByteString schema_name, ByteString table_name, bool is_error_if_table_does_not_exist)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_is_error_if_table_does_not_exist(is_error_if_table_does_not_exist)
    {
    }

    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }
    bool is_error_if_table_does_not_exist() const { return m_is_error_if_table_does_not_exist; }

private:
    ByteString m_schema_name;
    ByteString m_table_name;
    bool m_is_error_if_table_does_not_exist;
};

enum class ConflictResolution {
    Abort,
    Fail,
    Ignore,
    Replace,
    Rollback,
};

class Insert : public Statement {
public:
    Insert(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, ByteString schema_name, ByteString table_name, ByteString alias, Vector<ByteString> column_names, Vector<NonnullRefPtr<ChainedExpression>> chained_expressions)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
        , m_column_names(move(column_names))
        , m_chained_expressions(move(chained_expressions))
    {
    }

    Insert(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, ByteString schema_name, ByteString table_name, ByteString alias, Vector<ByteString> column_names, RefPtr<Select> select_statement)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
        , m_column_names(move(column_names))
        , m_select_statement(move(select_statement))
    {
    }

    Insert(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, ByteString schema_name, ByteString table_name, ByteString alias, Vector<ByteString> column_names)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
        , m_column_names(move(column_names))
    {
    }

    RefPtr<CommonTableExpressionList> const& common_table_expression_list() const { return m_common_table_expression_list; }
    ConflictResolution conflict_resolution() const { return m_conflict_resolution; }
    ByteString const& schema_name() const { return m_schema_name; }
    ByteString const& table_name() const { return m_table_name; }
    ByteString const& alias() const { return m_alias; }
    Vector<ByteString> const& column_names() const { return m_column_names; }

    bool default_values() const { return !has_expressions() && !has_selection(); }

    bool has_expressions() const { return !m_chained_expressions.is_empty(); }
    Vector<NonnullRefPtr<ChainedExpression>> const& chained_expressions() const { return m_chained_expressions; }

    bool has_selection() const { return !m_select_statement.is_null(); }
    RefPtr<Select> const& select_statement() const { return m_select_statement; }

    virtual ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    ConflictResolution m_conflict_resolution;
    ByteString m_schema_name;
    ByteString m_table_name;
    ByteString m_alias;
    Vector<ByteString> m_column_names;
    Vector<NonnullRefPtr<ChainedExpression>> m_chained_expressions;
    RefPtr<Select> m_select_statement;
};

class Update : public Statement {
public:
    struct UpdateColumns {
        Vector<ByteString> column_names;
        NonnullRefPtr<Expression> expression;
    };

    Update(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, NonnullRefPtr<QualifiedTableName> qualified_table_name, Vector<UpdateColumns> update_columns, Vector<NonnullRefPtr<TableOrSubquery>> table_or_subquery_list, RefPtr<Expression> where_clause, RefPtr<ReturningClause> returning_clause)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_qualified_table_name(move(qualified_table_name))
        , m_update_columns(move(update_columns))
        , m_table_or_subquery_list(move(table_or_subquery_list))
        , m_where_clause(move(where_clause))
        , m_returning_clause(move(returning_clause))
    {
    }

    RefPtr<CommonTableExpressionList> const& common_table_expression_list() const { return m_common_table_expression_list; }
    ConflictResolution conflict_resolution() const { return m_conflict_resolution; }
    NonnullRefPtr<QualifiedTableName> const& qualified_table_name() const { return m_qualified_table_name; }
    Vector<UpdateColumns> const& update_columns() const { return m_update_columns; }
    Vector<NonnullRefPtr<TableOrSubquery>> const& table_or_subquery_list() const { return m_table_or_subquery_list; }
    RefPtr<Expression> const& where_clause() const { return m_where_clause; }
    RefPtr<ReturningClause> const& returning_clause() const { return m_returning_clause; }

    virtual ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    ConflictResolution m_conflict_resolution;
    NonnullRefPtr<QualifiedTableName> m_qualified_table_name;
    Vector<UpdateColumns> m_update_columns;
    Vector<NonnullRefPtr<TableOrSubquery>> m_table_or_subquery_list;
    RefPtr<Expression> m_where_clause;
    RefPtr<ReturningClause> m_returning_clause;
};

class Delete : public Statement {
public:
    Delete(RefPtr<CommonTableExpressionList> common_table_expression_list, NonnullRefPtr<QualifiedTableName> qualified_table_name, RefPtr<Expression> where_clause, RefPtr<ReturningClause> returning_clause)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_qualified_table_name(move(qualified_table_name))
        , m_where_clause(move(where_clause))
        , m_returning_clause(move(returning_clause))
    {
    }

    RefPtr<CommonTableExpressionList> const& common_table_expression_list() const { return m_common_table_expression_list; }
    NonnullRefPtr<QualifiedTableName> const& qualified_table_name() const { return m_qualified_table_name; }
    RefPtr<Expression> const& where_clause() const { return m_where_clause; }
    RefPtr<ReturningClause> const& returning_clause() const { return m_returning_clause; }

    virtual ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    NonnullRefPtr<QualifiedTableName> m_qualified_table_name;
    RefPtr<Expression> m_where_clause;
    RefPtr<ReturningClause> m_returning_clause;
};

class Select : public Statement {
public:
    Select(RefPtr<CommonTableExpressionList> common_table_expression_list, bool select_all, Vector<NonnullRefPtr<ResultColumn>> result_column_list, Vector<NonnullRefPtr<TableOrSubquery>> table_or_subquery_list, RefPtr<Expression> where_clause, RefPtr<GroupByClause> group_by_clause, Vector<NonnullRefPtr<OrderingTerm>> ordering_term_list, RefPtr<LimitClause> limit_clause)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_select_all(move(select_all))
        , m_result_column_list(move(result_column_list))
        , m_table_or_subquery_list(move(table_or_subquery_list))
        , m_where_clause(move(where_clause))
        , m_group_by_clause(move(group_by_clause))
        , m_ordering_term_list(move(ordering_term_list))
        , m_limit_clause(move(limit_clause))
    {
    }

    RefPtr<CommonTableExpressionList> const& common_table_expression_list() const { return m_common_table_expression_list; }
    bool select_all() const { return m_select_all; }
    Vector<NonnullRefPtr<ResultColumn>> const& result_column_list() const { return m_result_column_list; }
    Vector<NonnullRefPtr<TableOrSubquery>> const& table_or_subquery_list() const { return m_table_or_subquery_list; }
    RefPtr<Expression> const& where_clause() const { return m_where_clause; }
    RefPtr<GroupByClause> const& group_by_clause() const { return m_group_by_clause; }
    Vector<NonnullRefPtr<OrderingTerm>> const& ordering_term_list() const { return m_ordering_term_list; }
    RefPtr<LimitClause> const& limit_clause() const { return m_limit_clause; }
    ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    bool m_select_all;
    Vector<NonnullRefPtr<ResultColumn>> m_result_column_list;
    Vector<NonnullRefPtr<TableOrSubquery>> m_table_or_subquery_list;
    RefPtr<Expression> m_where_clause;
    RefPtr<GroupByClause> m_group_by_clause;
    Vector<NonnullRefPtr<OrderingTerm>> m_ordering_term_list;
    RefPtr<LimitClause> m_limit_clause;
};

class DescribeTable : public Statement {
public:
    DescribeTable(NonnullRefPtr<QualifiedTableName> qualified_table_name)
        : m_qualified_table_name(move(qualified_table_name))
    {
    }

    NonnullRefPtr<QualifiedTableName> qualified_table_name() const { return m_qualified_table_name; }
    ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    NonnullRefPtr<QualifiedTableName> m_qualified_table_name;
};

}
