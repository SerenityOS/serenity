/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>

namespace SQL::AST {

ResultOr<ResultSet> CreateSchema::execute(ExecutionContext& context) const
{
    auto schema_def = TRY(SchemaDef::create(m_schema_name));

    if (auto result = context.database->add_schema(*schema_def); result.is_error()) {
        if (result.error().error() != SQLErrorCode::SchemaExists || m_is_error_if_schema_exists)
            return result.release_error();
    }

    return ResultSet { SQLCommand::Create };
}

}
