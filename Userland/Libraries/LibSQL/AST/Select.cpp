/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/AST/AST.h>
#include <LibSQL/Database.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL::AST {

RefPtr<SQLResult> Select::execute(ExecutionContext& context) const
{
    if (table_or_subquery_list().size() == 1 && table_or_subquery_list()[0].is_table()) {
        if (result_column_list().size() == 1 && result_column_list()[0].type() == ResultType::All) {
            auto table = context.database->get_table(table_or_subquery_list()[0].schema_name(), table_or_subquery_list()[0].table_name());
            if (!table) {
                return SQLResult::construct(SQL::SQLCommand::Select, SQL::SQLErrorCode::TableDoesNotExist, table_or_subquery_list()[0].table_name());
            }
            NonnullRefPtr<TupleDescriptor> descriptor = table->to_tuple_descriptor();
            context.result = SQLResult::construct();
            for (auto& row : context.database->select_all(*table)) {
                Tuple tuple(descriptor);
                for (auto ix = 0u; ix < descriptor->size(); ix++) {
                    tuple[ix] = row[ix];
                }
                context.result->append(tuple);
            }
            return context.result;
        }
    }
    return SQLResult::construct();
}

}
