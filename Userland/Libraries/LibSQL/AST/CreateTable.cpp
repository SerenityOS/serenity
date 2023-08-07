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
    auto schema_def = TRY(context.database->get_schema(m_schema_name));
    auto table_def = TRY(TableDef::create(schema_def, m_table_name));

    for (auto const& column : m_columns) {
        SQLType type;

        if (column->type_name()->name().is_one_of("VARCHAR"sv, "TEXT"sv))
            type = SQLType::Text;
        else if (column->type_name()->name().is_one_of("INT"sv, "INTEGER"sv))
            type = SQLType::Integer;
        else if (column->type_name()->name().is_one_of("FLOAT"sv, "NUMBER"sv))
            type = SQLType::Float;
        else if (column->type_name()->name().is_one_of("BOOL"sv, "BOOLEAN"sv))
            type = SQLType::Boolean;
        else
            return Result { SQLCommand::Create, SQLErrorCode::InvalidType, column->type_name()->name() };

        table_def->append_column(column->name(), type);
    }

    if (auto result = context.database->add_table(*table_def); result.is_error()) {
        if (result.error().error() != SQLErrorCode::TableExists || m_is_error_if_table_exists)
            return result.release_error();
    }

    return ResultSet { SQLCommand::Create };
}

}
