/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumericLimits.h>
#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

ResultOr<ResultSet> Select::execute(ExecutionContext& context) const
{
    NonnullRefPtrVector<ResultColumn> columns;

    auto const& result_column_list = this->result_column_list();
    VERIFY(!result_column_list.is_empty());

    for (auto& table_descriptor : table_or_subquery_list()) {
        if (!table_descriptor.is_table())
            return Result { SQLCommand::Select, SQLErrorCode::NotYetImplemented, "Sub-selects are not yet implemented"sv };

        auto table_def = TRY(context.database->get_table(table_descriptor.schema_name(), table_descriptor.table_name()));
        if (!table_def)
            return Result { SQLCommand::Select, SQLErrorCode::TableDoesNotExist, table_descriptor.table_name() };

        if (result_column_list.size() == 1 && result_column_list[0].type() == ResultType::All) {
            for (auto& col : table_def->columns()) {
                columns.append(
                    create_ast_node<ResultColumn>(
                        create_ast_node<ColumnNameExpression>(table_def->parent()->name(), table_def->name(), col.name()),
                        ""));
            }
        }
    }

    if (result_column_list.size() != 1 || result_column_list[0].type() != ResultType::All) {
        for (auto& col : result_column_list) {
            if (col.type() == ResultType::All) {
                // FIXME can have '*' for example in conjunction with computed columns
                return Result { SQLCommand::Select, SQLErrorCode::SyntaxError, "*"sv };
            }

            columns.append(col);
        }
    }

    context.result = Result { SQLCommand::Select };
    ResultSet result { SQLCommand::Select };

    auto descriptor = adopt_ref(*new TupleDescriptor);
    Tuple tuple(descriptor);
    Vector<Tuple> rows;
    descriptor->empend("__unity__"sv);
    tuple.append(Value(SQLType::Boolean, true));
    rows.append(tuple);

    for (auto& table_descriptor : table_or_subquery_list()) {
        if (!table_descriptor.is_table())
            return Result { SQLCommand::Select, SQLErrorCode::NotYetImplemented, "Sub-selects are not yet implemented"sv };

        auto table_def = TRY(context.database->get_table(table_descriptor.schema_name(), table_descriptor.table_name()));
        if (table_def->num_columns() == 0)
            continue;

        auto old_descriptor_size = descriptor->size();
        descriptor->extend(table_def->to_tuple_descriptor());

        while (!rows.is_empty() && (rows.first().size() == old_descriptor_size)) {
            auto cartesian_row = rows.take_first();
            auto table_rows = TRY(context.database->select_all(*table_def));

            for (auto& table_row : table_rows) {
                auto new_row = cartesian_row;
                new_row.extend(table_row);
                rows.append(new_row);
            }
        }
    }

    bool has_ordering { false };
    auto sort_descriptor = adopt_ref(*new TupleDescriptor);
    for (auto& term : m_ordering_term_list) {
        sort_descriptor->append(TupleElementDescriptor { .order = term.order() });
        has_ordering = true;
    }
    Tuple sort_key(sort_descriptor);

    for (auto& row : rows) {
        context.current_row = &row;

        if (where_clause()) {
            auto where_result = where_clause()->evaluate(context);
            if (context.result->is_error())
                return context.result.release_value();
            if (!where_result)
                continue;
        }

        tuple.clear();

        for (auto& col : columns) {
            auto value = col.expression()->evaluate(context);
            if (context.result->is_error())
                return context.result.release_value();
            tuple.append(value);
        }

        if (has_ordering) {
            sort_key.clear();
            for (auto& term : m_ordering_term_list) {
                auto value = term.expression()->evaluate(context);
                if (context.result->is_error())
                    return context.result.release_value();
                sort_key.append(value);
            }
        }

        result.insert_row(tuple, sort_key);
    }

    if (m_limit_clause != nullptr) {
        size_t limit_value = NumericLimits<size_t>::max();
        size_t offset_value = 0;

        auto limit = m_limit_clause->limit_expression()->evaluate(context);
        if (!limit.is_null()) {
            auto limit_value_maybe = limit.to_u32();
            if (!limit_value_maybe.has_value())
                return Result { SQLCommand::Select, SQLErrorCode::SyntaxError, "LIMIT clause must evaluate to an integer value"sv };

            limit_value = limit_value_maybe.value();
        }

        if (m_limit_clause->offset_expression() != nullptr) {
            auto offset = m_limit_clause->offset_expression()->evaluate(context);
            if (!offset.is_null()) {
                auto offset_value_maybe = offset.to_u32();
                if (!offset_value_maybe.has_value())
                    return Result { SQLCommand::Select, SQLErrorCode::SyntaxError, "OFFSET clause must evaluate to an integer value"sv };

                offset_value = offset_value_maybe.value();
            }
        }

        result.limit(offset_value, limit_value);
    }

    return result;
}

}
