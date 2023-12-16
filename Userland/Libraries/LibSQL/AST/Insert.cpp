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

ResultOr<ResultSet> Insert::execute(ExecutionContext& context) const
{
    auto table_def = TRY(context.database->get_table(m_schema_name, m_table_name));

    Row row(table_def);
    for (auto& column : m_column_names) {
        if (!row.has(column))
            return Result { SQLCommand::Insert, SQLErrorCode::ColumnDoesNotExist, column };
    }

    ResultSet result { SQLCommand::Insert };
    TRY(result.try_ensure_capacity(m_chained_expressions.size()));

    for (auto& row_expr : m_chained_expressions) {
        for (auto& column_def : table_def->columns()) {
            if (!m_column_names.contains_slow(column_def->name()))
                row[column_def->name()] = column_def->default_value();
        }

        auto row_value = TRY(row_expr->evaluate(context));
        VERIFY(row_value.type() == SQLType::Tuple);

        auto values = row_value.to_vector().release_value();

        if (m_column_names.is_empty() && values.size() != row.size())
            return Result { SQLCommand::Insert, SQLErrorCode::InvalidNumberOfValues, ByteString::empty() };

        for (auto ix = 0u; ix < values.size(); ix++) {
            auto& tuple_descriptor = *row.descriptor();
            // In case of having column names, this must succeed since we checked for every column name for existence in the table.
            auto element_index = m_column_names.is_empty() ? ix : tuple_descriptor.find_if([&](auto element) { return element.name == m_column_names[ix]; }).index();
            auto element_type = tuple_descriptor[element_index].type;

            if (!values[ix].is_type_compatible_with(element_type))
                return Result { SQLCommand::Insert, SQLErrorCode::InvalidValueType, table_def->columns()[element_index]->name() };

            row[element_index] = move(values[ix]);
        }

        TRY(context.database->insert(row));
        result.insert_row(row, {});
    }

    return result;
}

}
