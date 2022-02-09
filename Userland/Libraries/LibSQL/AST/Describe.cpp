/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

Result DescribeTable::execute(ExecutionContext& context) const
{
    auto schema_name = m_qualified_table_name->schema_name();
    auto table_name = m_qualified_table_name->table_name();

    auto table_def = TRY(context.database->get_table(schema_name, table_name));
    if (!table_def) {
        if (schema_name.is_empty())
            schema_name = "default"sv;
        return { SQLCommand::Describe, SQLErrorCode::TableDoesNotExist, String::formatted("{}.{}", schema_name, table_name) };
    }

    auto describe_table_def = MUST(context.database->get_table("master"sv, "internal_describe_table"sv));
    auto descriptor = describe_table_def->to_tuple_descriptor();

    Result result { SQLCommand::Describe };

    for (auto& column : table_def->columns()) {
        Tuple tuple(descriptor);
        tuple[0] = column.name();
        tuple[1] = SQLType_name(column.type());
        result.insert(tuple, Tuple {});
    }

    return result;
}

}
