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

RefPtr<SQLResult> DescribeTable::execute(ExecutionContext& context) const
{
    auto schema_name = m_qualified_table_name->schema_name();
    auto table_name = m_qualified_table_name->table_name();

    auto table_def_or_error = context.database->get_table(schema_name, table_name);
    auto table_def = table_def_or_error.release_value();
    if (!table_def) {
        if (schema_name.is_null() || schema_name.is_empty())
            schema_name = "default";
        return SQLResult::construct(SQLCommand::Describe, SQLErrorCode::TableDoesNotExist, String::formatted("{}.{}", schema_name, table_name));
    }

    auto describe_table_def = context.database->get_table("master", "internal_describe_table").value();
    NonnullRefPtr<TupleDescriptor> descriptor = describe_table_def->to_tuple_descriptor();

    context.result = SQLResult::construct();

    for (auto& column : table_def->columns()) {
        Tuple tuple(descriptor);
        tuple[0] = column.name();
        tuple[1] = SQLType_name(column.type());
        context.result->insert(tuple, Tuple {});
    }

    return context.result;
}

}
