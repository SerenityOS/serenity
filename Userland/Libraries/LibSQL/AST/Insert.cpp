/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

static bool does_value_data_type_match(SQLType expected, SQLType actual)
{
    if (actual == SQLType::Null) {
        return false;
    }

    if (expected == SQLType::Integer) {
        return actual == SQLType::Integer || actual == SQLType::Float;
    }

    return expected == actual;
}

RefPtr<SQLResult> Insert::execute(ExecutionContext& context) const
{
    auto table_def_or_error = context.database->get_table(m_schema_name, m_table_name);
    if (table_def_or_error.is_error())
        return SQLResult::construct(SQLCommand::Insert, SQLErrorCode::InternalError, table_def_or_error.release_error());
    auto table_def = table_def_or_error.release_value();
    if (!table_def) {
        auto schema_name = m_schema_name;
        if (schema_name.is_null() || schema_name.is_empty())
            schema_name = "default";
        return SQLResult::construct(SQLCommand::Insert, SQLErrorCode::TableDoesNotExist, String::formatted("{}.{}", schema_name, m_table_name));
    }

    Row row(table_def);
    for (auto& column : m_column_names) {
        if (!row.has(column)) {
            return SQLResult::construct(SQLCommand::Insert, SQLErrorCode::ColumnDoesNotExist, column);
        }
    }

    Vector<Row> inserted_rows;
    inserted_rows.ensure_capacity(m_chained_expressions.size());
    context.result = SQLResult::construct();
    for (auto& row_expr : m_chained_expressions) {
        for (auto& column_def : table_def->columns()) {
            if (!m_column_names.contains_slow(column_def.name())) {
                row[column_def.name()] = column_def.default_value();
            }
        }
        auto row_value = row_expr.evaluate(context);
        if (context.result->has_error())
            return context.result;
        VERIFY(row_value.type() == SQLType::Tuple);
        auto values = row_value.to_vector().value();

        if (m_column_names.size() == 0 && values.size() != row.size()) {
            return SQLResult::construct(SQLCommand::Insert, SQLErrorCode::InvalidNumberOfValues, "");
        }

        for (auto ix = 0u; ix < values.size(); ix++) {
            auto input_value_type = values[ix].type();
            auto& tuple_descriptor = *row.descriptor();
            // In case of having column names, this must succeed since we checked for every column name for existence in the table.
            auto element_index = (m_column_names.size() == 0) ? ix : tuple_descriptor.find_if([&](auto element) { return element.name == m_column_names[ix]; }).index();
            auto element_type = tuple_descriptor[element_index].type;

            if (!does_value_data_type_match(element_type, input_value_type)) {
                return SQLResult::construct(SQLCommand::Insert, SQLErrorCode::InvalidValueType, table_def->columns()[element_index].name());
            }

            row[element_index] = values[ix];
        }
        inserted_rows.append(row);
    }

    for (auto& inserted_row : inserted_rows) {
        if (auto maybe_error = context.database->insert(inserted_row); maybe_error.is_error())
            return SQLResult::construct(SQLCommand::Insert, SQLErrorCode::InternalError, maybe_error.release_error());
    }

    return SQLResult::construct(SQLCommand::Insert, 0, m_chained_expressions.size(), 0);
}

}
