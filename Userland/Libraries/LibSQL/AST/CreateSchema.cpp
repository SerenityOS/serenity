/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>

namespace SQL::AST {

RefPtr<SQLResult> CreateSchema::execute(ExecutionContext& context) const
{
    auto schema_def_or_error = context.database->get_schema(m_schema_name);
    if (schema_def_or_error.is_error())
        return SQLResult::construct(SQLCommand::Create, SQLErrorCode::InternalError, schema_def_or_error.error());
    auto schema_def = schema_def_or_error.release_value();
    if (schema_def) {
        if (m_is_error_if_schema_exists) {
            return SQLResult::construct(SQLCommand::Create, SQLErrorCode::SchemaExists, m_schema_name);
        }
        return SQLResult::construct(SQLCommand::Create);
    }

    schema_def = SchemaDef::construct(m_schema_name);
    if (auto maybe_error = context.database->add_schema(*schema_def); maybe_error.is_error())
        return SQLResult::construct(SQLCommand::Create, SQLErrorCode::InternalError, maybe_error.error());
    return SQLResult::construct(SQLCommand::Create, 0, 1);
}

}
