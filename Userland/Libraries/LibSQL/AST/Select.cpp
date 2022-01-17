/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/ResultSet.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

RefPtr<SQLResult> Select::execute(ExecutionContext& context) const
{
    NonnullRefPtrVector<ResultColumn> columns;
    for (auto& table_descriptor : table_or_subquery_list()) {
        if (!table_descriptor.is_table())
            return SQLResult::construct(SQLCommand::Select, SQLErrorCode::NotYetImplemented, "Sub-selects are not yet implemented");
        auto table_def_or_error = context.database->get_table(table_descriptor.schema_name(), table_descriptor.table_name());
        if (table_def_or_error.is_error())
            return SQLResult::construct(SQLCommand::Select, SQLErrorCode::InternalError, table_def_or_error.error());
        auto table = table_def_or_error.value();
        if (!table) {
            return SQLResult::construct(SQLCommand::Select, SQLErrorCode::TableDoesNotExist, table_descriptor.table_name());
        }

        if (result_column_list().size() == 1 && result_column_list()[0].type() == ResultType::All) {
            for (auto& col : table->columns()) {
                columns.append(
                    create_ast_node<ResultColumn>(
                        create_ast_node<ColumnNameExpression>(table->parent()->name(), table->name(), col.name()),
                        ""));
            }
        }
    }

    VERIFY(!result_column_list().is_empty());
    if (result_column_list().size() != 1 || result_column_list()[0].type() != ResultType::All) {
        for (auto& col : result_column_list()) {
            if (col.type() == ResultType::All)
                // FIXME can have '*' for example in conjunction with computed columns
                return SQLResult::construct(SQL::SQLCommand::Select, SQLErrorCode::SyntaxError, "*");
            columns.append(col);
        }
    }

    context.result = SQLResult::construct();
    AK::NonnullRefPtr<TupleDescriptor> descriptor = AK::adopt_ref(*new TupleDescriptor);
    Tuple tuple(descriptor);
    Vector<Tuple> rows;
    descriptor->empend("__unity__");
    tuple.append(Value(SQLType::Boolean, true));
    rows.append(tuple);

    for (auto& table_descriptor : table_or_subquery_list()) {
        if (!table_descriptor.is_table())
            return SQLResult::construct(SQLCommand::Select, SQLErrorCode::NotYetImplemented, "Sub-selects are not yet implemented");
        auto table_def_or_error = context.database->get_table(table_descriptor.schema_name(), table_descriptor.table_name());
        if (table_def_or_error.is_error())
            return SQLResult::construct(SQLCommand::Select, SQLErrorCode::InternalError, table_def_or_error.error());
        auto table = table_def_or_error.value();
        if (table->num_columns() == 0)
            continue;
        auto old_descriptor_size = descriptor->size();
        descriptor->extend(table->to_tuple_descriptor());
        for (auto cartesian_row = rows.first(); cartesian_row.size() == old_descriptor_size; cartesian_row = rows.first()) {
            rows.remove(0);
            auto table_rows_or_error = context.database->select_all(*table);
            if (table_rows_or_error.is_error())
                return SQLResult::construct(SQLCommand::Select, SQLErrorCode::InternalError, table_rows_or_error.error());
            for (auto& table_row : table_rows_or_error.value()) {
                auto new_row = cartesian_row;
                new_row.extend(table_row);
                rows.append(new_row);
            }
        }
    }

    bool has_ordering { false };
    AK::NonnullRefPtr<TupleDescriptor> sort_descriptor = AK::adopt_ref(*new TupleDescriptor);
    for (auto& term : m_ordering_term_list) {
        sort_descriptor->append(TupleElementDescriptor { .order = term.order() });
        has_ordering = true;
    }
    Tuple sort_key(sort_descriptor);

    for (auto& row : rows) {
        context.current_row = &row;
        if (where_clause()) {
            auto where_result = where_clause()->evaluate(context);
            if (context.result->has_error())
                return context.result;
            if (!where_result)
                continue;
        }
        tuple.clear();
        for (auto& col : columns) {
            auto value = col.expression()->evaluate(context);
            if (context.result->has_error())
                return context.result;
            tuple.append(value);
        }

        if (has_ordering) {
            sort_key.clear();
            for (auto& term : m_ordering_term_list) {
                auto value = term.expression()->evaluate(context);
                if (context.result->has_error())
                    return context.result;
                sort_key.append(value);
            }
        }
        context.result->insert(tuple, sort_key);
    }

    if (m_limit_clause != nullptr) {
        size_t limit_value = NumericLimits<size_t>::max();
        auto limit = m_limit_clause->limit_expression()->evaluate(context);
        if (!limit.is_null()) {
            auto limit_value_maybe = limit.to_u32();
            if (!limit_value_maybe.has_value()) {
                return SQLResult::construct(SQLCommand::Select, SQLErrorCode::SyntaxError, "LIMIT clause must evaluate to an integer value");
            }
            limit_value = limit_value_maybe.value();
        }
        size_t offset_value = 0;
        if (m_limit_clause->offset_expression() != nullptr) {
            auto offset = m_limit_clause->offset_expression()->evaluate(context);
            if (!offset.is_null()) {
                auto offset_value_maybe = offset.to_u32();
                if (!offset_value_maybe.has_value()) {
                    return SQLResult::construct(SQLCommand::Select, SQLErrorCode::SyntaxError, "OFFSET clause must evaluate to an integer value");
                }
                offset_value = offset_value_maybe.value();
            }
        }
        context.result->limit(offset_value, limit_value);
    }

    return context.result;
}

}
