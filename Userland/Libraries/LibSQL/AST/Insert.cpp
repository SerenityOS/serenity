/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

RefPtr<SQLResult> Insert::execute(ExecutionContext& context) const
{
    auto table_def = context.database->get_table(m_schema_name, m_table_name);
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

    for (auto& row_expr : m_chained_expressions) {
        for (auto& column_def : table_def->columns()) {
            if (!m_column_names.contains_slow(column_def.name())) {
                row[column_def.name()] = column_def.default_value();
            }
        }
        auto row_value = row_expr.evaluate(context);
        VERIFY(row_value.type() == SQLType::Tuple);
        auto values = row_value.to_vector().value();
        for (auto ix = 0u; ix < values.size(); ix++) {
            auto& column_name = m_column_names[ix];
            row[column_name] = values[ix];
        }
        context.database->insert(row);
    }
    return SQLResult::construct(SQLCommand::Insert, 0, m_chained_expressions.size(), 0);
}

}
