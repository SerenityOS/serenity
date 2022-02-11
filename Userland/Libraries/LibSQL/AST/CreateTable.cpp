/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>

namespace SQL::AST {

ResultOr<ResultSet> CreateTable::execute(ExecutionContext& context) const
{
    auto schema_name = m_schema_name.is_empty() ? String { "default"sv } : m_schema_name;

    auto schema_def = TRY(context.database->get_schema(schema_name));
    if (!schema_def)
        return Result { SQLCommand::Create, SQLErrorCode::SchemaDoesNotExist, schema_name };

    auto table_def = TRY(context.database->get_table(schema_name, m_table_name));
    if (table_def) {
        if (m_is_error_if_table_exists)
            return Result { SQLCommand::Create, SQLErrorCode::TableExists, m_table_name };
        return ResultSet { SQLCommand::Create };
    }

    table_def = TableDef::construct(schema_def, m_table_name);

    for (auto& column : m_columns) {
        SQLType type;

        if (column.type_name()->name().is_one_of("VARCHAR"sv, "TEXT"sv))
            type = SQLType::Text;
        else if (column.type_name()->name().is_one_of("INT"sv, "INTEGER"sv))
            type = SQLType::Integer;
        else if (column.type_name()->name().is_one_of("FLOAT"sv, "NUMBER"sv))
            type = SQLType::Float;
        else
            return Result { SQLCommand::Create, SQLErrorCode::InvalidType, column.type_name()->name() };

        table_def->append_column(column.name(), type);
    }

    TRY(context.database->add_table(*table_def));
    return ResultSet { SQLCommand::Create };
}

}
