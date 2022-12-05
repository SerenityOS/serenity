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

ResultOr<ResultSet> Delete::execute(ExecutionContext& context) const
{
    auto const& schema_name = m_qualified_table_name->schema_name();
    auto const& table_name = m_qualified_table_name->table_name();
    auto table_def = TRY(context.database->get_table(schema_name, table_name));

    ResultSet result { SQLCommand::Delete };

    for (auto& table_row : TRY(context.database->select_all(*table_def))) {
        context.current_row = &table_row;

        if (auto const& where_clause = this->where_clause()) {
            auto where_result = TRY(where_clause->evaluate(context)).to_bool();
            if (!where_result.has_value() || !where_result.value())
                continue;
        }

        TRY(context.database->remove(table_row));

        // FIXME: Implement the RETURNING clause.
        result.insert_row(table_row, {});
    }

    return result;
}

}
