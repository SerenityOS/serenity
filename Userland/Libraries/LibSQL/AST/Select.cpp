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
        auto table = context.database->get_table(table_or_subquery_list()[0].schema_name(), table_or_subquery_list()[0].table_name());
        if (!table) {
            return SQLResult::construct(SQL::SQLCommand::Select, SQL::SQLErrorCode::TableDoesNotExist, table_or_subquery_list()[0].table_name());
        }

        NonnullRefPtrVector<ResultColumn> columns;
        if (result_column_list().size() == 1 && result_column_list()[0].type() == ResultType::All) {
            for (auto& col : table->columns()) {
                columns.append(
                    create_ast_node<ResultColumn>(
                        create_ast_node<ColumnNameExpression>(table->parent()->name(), table->name(), col.name()),
                        ""));
            }
        } else {
            for (auto& col : result_column_list()) {
                columns.append(col);
            }
        }
        context.result = SQLResult::construct();
        AK::NonnullRefPtr<TupleDescriptor> descriptor = AK::adopt_ref(*new TupleDescriptor);
        Tuple tuple(descriptor);
        for (auto& row : context.database->select_all(*table)) {
            context.current_row = &row;
            if (where_clause()) {
                auto where_result = where_clause()->evaluate(context);
                if (context.result->has_error())
                    return context.result;
                if (!where_result)
                    continue;
            }
            tuple.clear();
            for (auto& col : columns) {
                auto value = col.expression()->evaluate(context);
                if (context.result->has_error())
                    return context.result;
                tuple.append(value);
            }
            context.result->append(tuple);
        }
        return context.result;
    }
    return SQLResult::construct();
}

}
