/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

ResultOr<ResultSet> Update::execute(ExecutionContext& context) const
{
    auto const& schema_name = m_qualified_table_name->schema_name();
    auto const& table_name = m_qualified_table_name->table_name();
    auto table_def = TRY(context.database->get_table(schema_name, table_name));

    Vector<Row> matched_rows;

    for (auto& table_row : TRY(context.database->select_all(*table_def))) {
        context.current_row = &table_row;

        if (auto const& where_clause = this->where_clause()) {
            auto where_result = TRY(where_clause->evaluate(context)).to_bool();
            if (!where_result.has_value() || !where_result.value())
                continue;
        }

        TRY(matched_rows.try_append(move(table_row)));
    }

    ResultSet result { SQLCommand::Update };

    for (auto& update_column : m_update_columns) {
        auto row_value = TRY(update_column.expression->evaluate(context));

        for (auto& table_row : matched_rows) {
            auto& row_descriptor = *table_row.descriptor();

            for (auto const& column_name : update_column.column_names) {
                if (!table_row.has(column_name))
                    return Result { SQLCommand::Update, SQLErrorCode::ColumnDoesNotExist, column_name };

                auto column_index = row_descriptor.find_if([&](auto element) { return element.name == column_name; }).index();
                auto column_type = row_descriptor[column_index].type;

                if (!row_value.is_type_compatible_with(column_type))
                    return Result { SQLCommand::Update, SQLErrorCode::InvalidValueType, column_name };

                table_row[column_index] = row_value;
            }

            TRY(context.database->update(table_row));
            result.insert_row(table_row, {});
        }
    }

    return result;
}

}
