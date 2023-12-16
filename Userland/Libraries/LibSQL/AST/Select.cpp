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

static ByteString result_column_name(ResultColumn const& column, size_t column_index)
{
    auto fallback_column_name = [column_index]() {
        return ByteString::formatted("Column{}", column_index);
    };

    if (auto const& alias = column.column_alias(); !alias.is_empty())
        return alias;

    if (column.select_from_expression()) {
        if (is<ColumnNameExpression>(*column.expression())) {
            auto const& column_name_expression = verify_cast<ColumnNameExpression>(*column.expression());
            return column_name_expression.column_name();
        }

        // FIXME: Generate column names from other result column expressions.
        return fallback_column_name();
    }

    VERIFY(column.select_from_table());

    // FIXME: Generate column names from select-from-table result columns.
    return fallback_column_name();
}

ResultOr<ResultSet> Select::execute(ExecutionContext& context) const
{
    Vector<NonnullRefPtr<ResultColumn const>> columns;
    Vector<ByteString> column_names;

    auto const& result_column_list = this->result_column_list();
    VERIFY(!result_column_list.is_empty());

    for (auto& table_descriptor : table_or_subquery_list()) {
        if (!table_descriptor->is_table())
            return Result { SQLCommand::Select, SQLErrorCode::NotYetImplemented, "Sub-selects are not yet implemented"sv };

        auto table_def = TRY(context.database->get_table(table_descriptor->schema_name(), table_descriptor->table_name()));

        if (result_column_list.size() == 1 && result_column_list[0]->type() == ResultType::All) {
            TRY(columns.try_ensure_capacity(columns.size() + table_def->columns().size()));
            TRY(column_names.try_ensure_capacity(column_names.size() + table_def->columns().size()));

            for (auto& col : table_def->columns()) {
                columns.unchecked_append(
                    create_ast_node<ResultColumn>(
                        create_ast_node<ColumnNameExpression>(table_def->parent()->name(), table_def->name(), col->name()),
                        ""));

                column_names.unchecked_append(col->name());
            }
        }
    }

    if (result_column_list.size() != 1 || result_column_list[0]->type() != ResultType::All) {
        TRY(columns.try_ensure_capacity(result_column_list.size()));
        TRY(column_names.try_ensure_capacity(result_column_list.size()));

        for (size_t i = 0; i < result_column_list.size(); ++i) {
            auto const& col = result_column_list[i];

            if (col->type() == ResultType::All) {
                // FIXME can have '*' for example in conjunction with computed columns
                return Result { SQLCommand::Select, SQLErrorCode::SyntaxError, "*"sv };
            }

            columns.unchecked_append(col);
            column_names.unchecked_append(result_column_name(col, i));
        }
    }

    ResultSet result { SQLCommand::Select, move(column_names) };

    auto descriptor = adopt_ref(*new TupleDescriptor);
    Tuple tuple(descriptor);
    Vector<Tuple> rows;
    descriptor->empend("__unity__"sv);
    tuple.append(Value { true });
    rows.append(tuple);

    for (auto& table_descriptor : table_or_subquery_list()) {
        if (!table_descriptor->is_table())
            return Result { SQLCommand::Select, SQLErrorCode::NotYetImplemented, "Sub-selects are not yet implemented"sv };

        auto table_def = TRY(context.database->get_table(table_descriptor->schema_name(), table_descriptor->table_name()));
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
        sort_descriptor->append(TupleElementDescriptor { .order = term->order() });
        has_ordering = true;
    }
    Tuple sort_key(sort_descriptor);

    for (auto& row : rows) {
        context.current_row = &row;

        if (where_clause()) {
            auto where_result = TRY(where_clause()->evaluate(context)).to_bool();
            if (!where_result.has_value() || !where_result.value())
                continue;
        }

        tuple.clear();

        for (auto& col : columns) {
            auto value = TRY(col->expression()->evaluate(context));
            tuple.append(value);
        }

        if (has_ordering) {
            sort_key.clear();
            for (auto& term : m_ordering_term_list) {
                auto value = TRY(term->expression()->evaluate(context));
                sort_key.append(value);
            }
        }

        result.insert_row(tuple, sort_key);
    }

    if (m_limit_clause != nullptr) {
        size_t limit_value = NumericLimits<size_t>::max();
        size_t offset_value = 0;

        auto limit = TRY(m_limit_clause->limit_expression()->evaluate(context));
        if (!limit.is_null()) {
            auto limit_value_maybe = limit.to_int<size_t>();
            if (!limit_value_maybe.has_value())
                return Result { SQLCommand::Select, SQLErrorCode::SyntaxError, "LIMIT clause must evaluate to an integer value"sv };

            limit_value = limit_value_maybe.value();
        }

        if (m_limit_clause->offset_expression() != nullptr) {
            auto offset = TRY(m_limit_clause->offset_expression()->evaluate(context));
            if (!offset.is_null()) {
                auto offset_value_maybe = offset.to_int<size_t>();
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
