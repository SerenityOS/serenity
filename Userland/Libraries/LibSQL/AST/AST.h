/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
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
    virtual ~ASTNode() { }

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
    TypeName(String name, NonnullRefPtrVector<SignedNumber> signed_numbers)
        : m_name(move(name))
        , m_signed_numbers(move(signed_numbers))
    {
        VERIFY(m_signed_numbers.size() <= 2);
    }

    const String& name() const { return m_name; }
    const NonnullRefPtrVector<SignedNumber>& signed_numbers() const { return m_signed_numbers; }

private:
    String m_name;
    NonnullRefPtrVector<SignedNumber> m_signed_numbers;
};

class ColumnDefinition : public ASTNode {
public:
    ColumnDefinition(String name, NonnullRefPtr<TypeName> type_name)
        : m_name(move(name))
        , m_type_name(move(type_name))
    {
    }

    const String& name() const { return m_name; }
    const NonnullRefPtr<TypeName>& type_name() const { return m_type_name; }

private:
    String m_name;
    NonnullRefPtr<TypeName> m_type_name;
};

class CommonTableExpression : public ASTNode {
public:
    CommonTableExpression(String table_name, Vector<String> column_names, NonnullRefPtr<Select> select_statement)
        : m_table_name(move(table_name))
        , m_column_names(move(column_names))
        , m_select_statement(move(select_statement))
    {
    }

    const String& table_name() const { return m_table_name; }
    const Vector<String>& column_names() const { return m_column_names; }
    const NonnullRefPtr<Select>& select_statement() const { return m_select_statement; }

private:
    String m_table_name;
    Vector<String> m_column_names;
    NonnullRefPtr<Select> m_select_statement;
};

class CommonTableExpressionList : public ASTNode {
public:
    CommonTableExpressionList(bool recursive, NonnullRefPtrVector<CommonTableExpression> common_table_expressions)
        : m_recursive(recursive)
        , m_common_table_expressions(move(common_table_expressions))
    {
        VERIFY(!m_common_table_expressions.is_empty());
    }

    bool recursive() const { return m_recursive; }
    const NonnullRefPtrVector<CommonTableExpression>& common_table_expressions() const { return m_common_table_expressions; }

private:
    bool m_recursive;
    NonnullRefPtrVector<CommonTableExpression> m_common_table_expressions;
};

class QualifiedTableName : public ASTNode {
public:
    QualifiedTableName(String schema_name, String table_name, String alias)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
    {
    }

    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }
    const String& alias() const { return m_alias; }

private:
    String m_schema_name;
    String m_table_name;
    String m_alias;
};

class ReturningClause : public ASTNode {
public:
    struct ColumnClause {
        NonnullRefPtr<Expression> expression;
        String column_alias;
    };

    ReturningClause() = default;

    explicit ReturningClause(Vector<ColumnClause> columns)
        : m_columns(move(columns))
    {
    }

    bool return_all_columns() const { return m_columns.is_empty(); };
    const Vector<ColumnClause>& columns() const { return m_columns; }

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

    explicit ResultColumn(String table_name)
        : m_type(ResultType::Table)
        , m_table_name(move(table_name))
    {
    }

    ResultColumn(NonnullRefPtr<Expression> expression, String column_alias)
        : m_type(ResultType::Expression)
        , m_expression(move(expression))
        , m_column_alias(move(column_alias))
    {
    }

    ResultType type() const { return m_type; }

    bool select_from_table() const { return !m_table_name.is_null(); }
    const String& table_name() const { return m_table_name; }

    bool select_from_expression() const { return !m_expression.is_null(); }
    const RefPtr<Expression>& expression() const { return m_expression; }
    const String& column_alias() const { return m_column_alias; }

private:
    ResultType m_type { ResultType::All };

    String m_table_name {};

    RefPtr<Expression> m_expression {};
    String m_column_alias {};
};

class GroupByClause : public ASTNode {
public:
    GroupByClause(NonnullRefPtrVector<Expression> group_by_list, RefPtr<Expression> having_clause)
        : m_group_by_list(move(group_by_list))
        , m_having_clause(move(having_clause))
    {
        VERIFY(!m_group_by_list.is_empty());
    }

    const NonnullRefPtrVector<Expression>& group_by_list() const { return m_group_by_list; }
    const RefPtr<Expression>& having_clause() const { return m_having_clause; }

private:
    NonnullRefPtrVector<Expression> m_group_by_list;
    RefPtr<Expression> m_having_clause;
};

class TableOrSubquery : public ASTNode {
public:
    TableOrSubquery() = default;

    TableOrSubquery(String schema_name, String table_name, String table_alias)
        : m_is_table(true)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_table_alias(move(table_alias))
    {
    }

    explicit TableOrSubquery(NonnullRefPtrVector<TableOrSubquery> subqueries)
        : m_is_subquery(!subqueries.is_empty())
        , m_subqueries(move(subqueries))
    {
    }

    bool is_table() const { return m_is_table; }
    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }
    const String& table_alias() const { return m_table_alias; }

    bool is_subquery() const { return m_is_subquery; }
    const NonnullRefPtrVector<TableOrSubquery>& subqueries() const { return m_subqueries; }

private:
    bool m_is_table { false };
    String m_schema_name {};
    String m_table_name {};
    String m_table_alias {};

    bool m_is_subquery { false };
    NonnullRefPtrVector<TableOrSubquery> m_subqueries {};
};

class OrderingTerm : public ASTNode {
public:
    OrderingTerm(NonnullRefPtr<Expression> expression, String collation_name, Order order, Nulls nulls)
        : m_expression(move(expression))
        , m_collation_name(move(collation_name))
        , m_order(order)
        , m_nulls(nulls)
    {
    }

    const NonnullRefPtr<Expression>& expression() const { return m_expression; }
    const String& collation_name() const { return m_collation_name; }
    Order order() const { return m_order; }
    Nulls nulls() const { return m_nulls; }

private:
    NonnullRefPtr<Expression> m_expression;
    String m_collation_name;
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

    const NonnullRefPtr<Expression>& limit_expression() const { return m_limit_expression; }
    const RefPtr<Expression>& offset_expression() const { return m_offset_expression; }

private:
    NonnullRefPtr<Expression> m_limit_expression;
    RefPtr<Expression> m_offset_expression;
};

//==================================================================================================
// Expressions
//==================================================================================================

struct ExecutionContext {
    NonnullRefPtr<Database> database;
    Optional<Result> result;
    class Statement const* statement;
    Tuple* current_row { nullptr };
};

class Expression : public ASTNode {
public:
    virtual Value evaluate(ExecutionContext&) const;
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
    virtual Value evaluate(ExecutionContext&) const override;

private:
    double m_value;
};

class StringLiteral : public Expression {
public:
    explicit StringLiteral(String value)
        : m_value(move(value))
    {
    }

    const String& value() const { return m_value; }
    virtual Value evaluate(ExecutionContext&) const override;

private:
    String m_value;
};

class BlobLiteral : public Expression {
public:
    explicit BlobLiteral(String value)
        : m_value(move(value))
    {
    }

    const String& value() const { return m_value; }

private:
    String m_value;
};

class NullLiteral : public Expression {
public:
    virtual Value evaluate(ExecutionContext&) const override;
};

class NestedExpression : public Expression {
public:
    const NonnullRefPtr<Expression>& expression() const { return m_expression; }
    virtual Value evaluate(ExecutionContext&) const override;

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
    const NonnullRefPtr<Expression>& lhs() const { return m_lhs; }
    const NonnullRefPtr<Expression>& rhs() const { return m_rhs; }

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
    ColumnNameExpression(String schema_name, String table_name, String column_name)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_column_name(move(column_name))
    {
    }

    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }
    const String& column_name() const { return m_column_name; }
    virtual Value evaluate(ExecutionContext&) const override;

private:
    String m_schema_name;
    String m_table_name;
    String m_column_name;
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
    virtual Value evaluate(ExecutionContext&) const override;

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
    virtual Value evaluate(ExecutionContext&) const override;

private:
    BinaryOperator m_type;
};

class ChainedExpression : public Expression {
public:
    explicit ChainedExpression(NonnullRefPtrVector<Expression> expressions)
        : m_expressions(move(expressions))
    {
    }

    const NonnullRefPtrVector<Expression>& expressions() const { return m_expressions; }
    virtual Value evaluate(ExecutionContext&) const override;

private:
    NonnullRefPtrVector<Expression> m_expressions;
};

class CastExpression : public NestedExpression {
public:
    CastExpression(NonnullRefPtr<Expression> expression, NonnullRefPtr<TypeName> type_name)
        : NestedExpression(move(expression))
        , m_type_name(move(type_name))
    {
    }

    const NonnullRefPtr<TypeName>& type_name() const { return m_type_name; }

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

    const RefPtr<Expression>& case_expression() const { return m_case_expression; }
    const Vector<WhenThenClause>& when_then_clauses() const { return m_when_then_clauses; }
    const RefPtr<Expression>& else_expression() const { return m_else_expression; }

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

    const NonnullRefPtr<Select>& select_statement() const { return m_select_statement; }
    bool invert_expression() const { return m_invert_expression; }

private:
    NonnullRefPtr<Select> m_select_statement;
    bool m_invert_expression;
};

class CollateExpression : public NestedExpression {
public:
    CollateExpression(NonnullRefPtr<Expression> expression, String collation_name)
        : NestedExpression(move(expression))
        , m_collation_name(move(collation_name))
    {
    }

    const String& collation_name() const { return m_collation_name; }

private:
    String m_collation_name;
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
    const RefPtr<Expression>& escape() const { return m_escape; }
    virtual Value evaluate(ExecutionContext&) const override;

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

    const NonnullRefPtr<Expression>& expression() const { return m_expression; }

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

    const NonnullRefPtr<Select>& select_statement() const { return m_select_statement; }

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

    const NonnullRefPtr<ChainedExpression>& expression_chain() const { return m_expression_chain; }

private:
    NonnullRefPtr<ChainedExpression> m_expression_chain;
};

class InTableExpression : public InvertibleNestedExpression {
public:
    InTableExpression(NonnullRefPtr<Expression> expression, String schema_name, String table_name, bool invert_expression)
        : InvertibleNestedExpression(move(expression), invert_expression)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
    {
    }

    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }

private:
    String m_schema_name;
    String m_table_name;
};

//==================================================================================================
// Statements
//==================================================================================================

class Statement : public ASTNode {
public:
    ResultOr<ResultSet> execute(AK::NonnullRefPtr<Database> database) const;

    virtual ResultOr<ResultSet> execute(ExecutionContext&) const
    {
        return Result { SQLCommand::Unknown, SQLErrorCode::NotYetImplemented };
    }
};

class ErrorStatement final : public Statement {
};

class CreateSchema : public Statement {
public:
    CreateSchema(String schema_name, bool is_error_if_schema_exists)
        : m_schema_name(move(schema_name))
        , m_is_error_if_schema_exists(is_error_if_schema_exists)
    {
    }

    const String& schema_name() const { return m_schema_name; }
    bool is_error_if_schema_exists() const { return m_is_error_if_schema_exists; }

    ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    String m_schema_name;
    bool m_is_error_if_schema_exists;
};

class CreateTable : public Statement {
public:
    CreateTable(String schema_name, String table_name, RefPtr<Select> select_statement, bool is_temporary, bool is_error_if_table_exists)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_select_statement(move(select_statement))
        , m_is_temporary(is_temporary)
        , m_is_error_if_table_exists(is_error_if_table_exists)
    {
    }

    CreateTable(String schema_name, String table_name, NonnullRefPtrVector<ColumnDefinition> columns, bool is_temporary, bool is_error_if_table_exists)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_columns(move(columns))
        , m_is_temporary(is_temporary)
        , m_is_error_if_table_exists(is_error_if_table_exists)
    {
    }

    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }

    bool has_selection() const { return !m_select_statement.is_null(); }
    const RefPtr<Select>& select_statement() const { return m_select_statement; }

    bool has_columns() const { return !m_columns.is_empty(); }
    const NonnullRefPtrVector<ColumnDefinition>& columns() const { return m_columns; }

    bool is_temporary() const { return m_is_temporary; }
    bool is_error_if_table_exists() const { return m_is_error_if_table_exists; }

    ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    String m_schema_name;
    String m_table_name;
    RefPtr<Select> m_select_statement;
    NonnullRefPtrVector<ColumnDefinition> m_columns;
    bool m_is_temporary;
    bool m_is_error_if_table_exists;
};

class AlterTable : public Statement {
public:
    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }

protected:
    AlterTable(String schema_name, String table_name)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
    {
    }

private:
    String m_schema_name;
    String m_table_name;
};

class RenameTable : public AlterTable {
public:
    RenameTable(String schema_name, String table_name, String new_table_name)
        : AlterTable(move(schema_name), move(table_name))
        , m_new_table_name(move(new_table_name))
    {
    }

    const String& new_table_name() const { return m_new_table_name; }

private:
    String m_new_table_name;
};

class RenameColumn : public AlterTable {
public:
    RenameColumn(String schema_name, String table_name, String column_name, String new_column_name)
        : AlterTable(move(schema_name), move(table_name))
        , m_column_name(move(column_name))
        , m_new_column_name(move(new_column_name))
    {
    }

    const String& column_name() const { return m_column_name; }
    const String& new_column_name() const { return m_new_column_name; }

private:
    String m_column_name;
    String m_new_column_name;
};

class AddColumn : public AlterTable {
public:
    AddColumn(String schema_name, String table_name, NonnullRefPtr<ColumnDefinition> column)
        : AlterTable(move(schema_name), move(table_name))
        , m_column(move(column))
    {
    }

    const NonnullRefPtr<ColumnDefinition>& column() const { return m_column; }

private:
    NonnullRefPtr<ColumnDefinition> m_column;
};

class DropColumn : public AlterTable {
public:
    DropColumn(String schema_name, String table_name, String column_name)
        : AlterTable(move(schema_name), move(table_name))
        , m_column_name(move(column_name))
    {
    }

    const String& column_name() const { return m_column_name; }

private:
    String m_column_name;
};

class DropTable : public Statement {
public:
    DropTable(String schema_name, String table_name, bool is_error_if_table_does_not_exist)
        : m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_is_error_if_table_does_not_exist(is_error_if_table_does_not_exist)
    {
    }

    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }
    bool is_error_if_table_does_not_exist() const { return m_is_error_if_table_does_not_exist; }

private:
    String m_schema_name;
    String m_table_name;
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
    Insert(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, String schema_name, String table_name, String alias, Vector<String> column_names, NonnullRefPtrVector<ChainedExpression> chained_expressions)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
        , m_column_names(move(column_names))
        , m_chained_expressions(move(chained_expressions))
    {
    }

    Insert(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, String schema_name, String table_name, String alias, Vector<String> column_names, RefPtr<Select> select_statement)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
        , m_column_names(move(column_names))
        , m_select_statement(move(select_statement))
    {
    }

    Insert(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, String schema_name, String table_name, String alias, Vector<String> column_names)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_schema_name(move(schema_name))
        , m_table_name(move(table_name))
        , m_alias(move(alias))
        , m_column_names(move(column_names))
    {
    }

    const RefPtr<CommonTableExpressionList>& common_table_expression_list() const { return m_common_table_expression_list; }
    ConflictResolution conflict_resolution() const { return m_conflict_resolution; }
    const String& schema_name() const { return m_schema_name; }
    const String& table_name() const { return m_table_name; }
    const String& alias() const { return m_alias; }
    const Vector<String>& column_names() const { return m_column_names; }

    bool default_values() const { return !has_expressions() && !has_selection(); };

    bool has_expressions() const { return !m_chained_expressions.is_empty(); }
    const NonnullRefPtrVector<ChainedExpression>& chained_expressions() const { return m_chained_expressions; }

    bool has_selection() const { return !m_select_statement.is_null(); }
    const RefPtr<Select>& select_statement() const { return m_select_statement; }

    virtual ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    ConflictResolution m_conflict_resolution;
    String m_schema_name;
    String m_table_name;
    String m_alias;
    Vector<String> m_column_names;
    NonnullRefPtrVector<ChainedExpression> m_chained_expressions;
    RefPtr<Select> m_select_statement;
};

class Update : public Statement {
public:
    struct UpdateColumns {
        Vector<String> column_names;
        NonnullRefPtr<Expression> expression;
    };

    Update(RefPtr<CommonTableExpressionList> common_table_expression_list, ConflictResolution conflict_resolution, NonnullRefPtr<QualifiedTableName> qualified_table_name, Vector<UpdateColumns> update_columns, NonnullRefPtrVector<TableOrSubquery> table_or_subquery_list, RefPtr<Expression> where_clause, RefPtr<ReturningClause> returning_clause)
        : m_common_table_expression_list(move(common_table_expression_list))
        , m_conflict_resolution(conflict_resolution)
        , m_qualified_table_name(move(qualified_table_name))
        , m_update_columns(move(update_columns))
        , m_table_or_subquery_list(move(table_or_subquery_list))
        , m_where_clause(move(where_clause))
        , m_returning_clause(move(returning_clause))
    {
    }

    const RefPtr<CommonTableExpressionList>& common_table_expression_list() const { return m_common_table_expression_list; }
    ConflictResolution conflict_resolution() const { return m_conflict_resolution; }
    const NonnullRefPtr<QualifiedTableName>& qualified_table_name() const { return m_qualified_table_name; }
    const Vector<UpdateColumns>& update_columns() const { return m_update_columns; }
    const NonnullRefPtrVector<TableOrSubquery>& table_or_subquery_list() const { return m_table_or_subquery_list; }
    const RefPtr<Expression>& where_clause() const { return m_where_clause; }
    const RefPtr<ReturningClause>& returning_clause() const { return m_returning_clause; }

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    ConflictResolution m_conflict_resolution;
    NonnullRefPtr<QualifiedTableName> m_qualified_table_name;
    Vector<UpdateColumns> m_update_columns;
    NonnullRefPtrVector<TableOrSubquery> m_table_or_subquery_list;
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

    const RefPtr<CommonTableExpressionList>& common_table_expression_list() const { return m_common_table_expression_list; }
    const NonnullRefPtr<QualifiedTableName>& qualified_table_name() const { return m_qualified_table_name; }
    const RefPtr<Expression>& where_clause() const { return m_where_clause; }
    const RefPtr<ReturningClause>& returning_clause() const { return m_returning_clause; }

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    NonnullRefPtr<QualifiedTableName> m_qualified_table_name;
    RefPtr<Expression> m_where_clause;
    RefPtr<ReturningClause> m_returning_clause;
};

class Select : public Statement {
public:
    Select(RefPtr<CommonTableExpressionList> common_table_expression_list, bool select_all, NonnullRefPtrVector<ResultColumn> result_column_list, NonnullRefPtrVector<TableOrSubquery> table_or_subquery_list, RefPtr<Expression> where_clause, RefPtr<GroupByClause> group_by_clause, NonnullRefPtrVector<OrderingTerm> ordering_term_list, RefPtr<LimitClause> limit_clause)
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

    const RefPtr<CommonTableExpressionList>& common_table_expression_list() const { return m_common_table_expression_list; }
    bool select_all() const { return m_select_all; }
    const NonnullRefPtrVector<ResultColumn>& result_column_list() const { return m_result_column_list; }
    const NonnullRefPtrVector<TableOrSubquery>& table_or_subquery_list() const { return m_table_or_subquery_list; }
    const RefPtr<Expression>& where_clause() const { return m_where_clause; }
    const RefPtr<GroupByClause>& group_by_clause() const { return m_group_by_clause; }
    const NonnullRefPtrVector<OrderingTerm>& ordering_term_list() const { return m_ordering_term_list; }
    const RefPtr<LimitClause>& limit_clause() const { return m_limit_clause; }
    ResultOr<ResultSet> execute(ExecutionContext&) const override;

private:
    RefPtr<CommonTableExpressionList> m_common_table_expression_list;
    bool m_select_all;
    NonnullRefPtrVector<ResultColumn> m_result_column_list;
    NonnullRefPtrVector<TableOrSubquery> m_table_or_subquery_list;
    RefPtr<Expression> m_where_clause;
    RefPtr<GroupByClause> m_group_by_clause;
    NonnullRefPtrVector<OrderingTerm> m_ordering_term_list;
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
