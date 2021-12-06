/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>

namespace SQL::AST {

RefPtr<SQLResult> CreateTable::execute(ExecutionContext& context) const
{
    auto schema_name = (!m_schema_name.is_null() && !m_schema_name.is_empty()) ? m_schema_name : "default";
    auto schema_def_or_error = context.database->get_schema(schema_name);
    if (schema_def_or_error.is_error())
        return SQLResult::construct(SQLCommand::Create, SQLErrorCode::InternalError, schema_def_or_error.error());
    auto schema_def = schema_def_or_error.release_value();
    if (!schema_def)
        return SQLResult::construct(SQLCommand::Create, SQLErrorCode::SchemaDoesNotExist, m_schema_name);
    auto table_def_or_error = context.database->get_table(schema_name, m_table_name);
    if (table_def_or_error.is_error())
        return SQLResult::construct(SQLCommand::Create, SQLErrorCode::InternalError, table_def_or_error.error());
    auto table_def = table_def_or_error.release_value();
    if (table_def) {
        if (m_is_error_if_table_exists) {
            return SQLResult::construct(SQLCommand::Create, SQLErrorCode::TableExists, m_table_name);
        } else {
            return SQLResult::construct(SQLCommand::Create);
        }
    }
    table_def = TableDef::construct(schema_def, m_table_name);
    for (auto& column : m_columns) {
        SQLType type;
        if (column.type_name()->name() == "VARCHAR" || column.type_name()->name() == "TEXT") {
            type = SQLType::Text;
        } else if (column.type_name()->name() == "INT" || column.type_name()->name() == "INTEGER") {
            type = SQLType::Integer;
        } else if (column.type_name()->name() == "FLOAT" || column.type_name()->name() == "NUMBER") {
            type = SQLType::Float;
        } else {
            return SQLResult::construct(SQLCommand::Create, SQLErrorCode::InvalidType, column.type_name()->name());
        }
        table_def->append_column(column.name(), type);
    }
    if (auto maybe_error = context.database->add_table(*table_def); maybe_error.is_error())
        return SQLResult::construct(SQLCommand::Create, SQLErrorCode::InternalError, maybe_error.release_error());
    return SQLResult::construct(SQLCommand::Create, 0, 1);
}

}
