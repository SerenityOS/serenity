/*
 * Copyright (c) 2021, Mahmoud Mandour <ma.mandourr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/ResultSet.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

ResultOr<ResultSet> DescribeTable::execute(ExecutionContext& context) const
{
    auto const& schema_name = m_qualified_table_name->schema_name();
    auto const& table_name = m_qualified_table_name->table_name();
    auto table_def = TRY(context.database->get_table(schema_name, table_name));

    auto describe_table_def = MUST(context.database->get_table("master"sv, "internal_describe_table"sv));
    auto descriptor = describe_table_def->to_tuple_descriptor();

    ResultSet result { SQLCommand::Describe };
    TRY(result.try_ensure_capacity(table_def->columns().size()));

    for (auto& column : table_def->columns()) {
        Tuple tuple(descriptor);
        tuple[0] = column->name();
        tuple[1] = SQLType_name(column->type());

        result.insert_row(tuple, Tuple {});
    }

    return result;
}

}
