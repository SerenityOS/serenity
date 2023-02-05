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

ResultOr<ResultSet> Statement::execute(AK::NonnullRefPtr<Database> database, ReadonlySpan<Value> placeholder_values) const
{
    ExecutionContext context { move(database), this, placeholder_values, nullptr };
    auto result = TRY(execute(context));

    // FIXME: When transactional sessions are supported, don't auto-commit modifications.
    TRY(context.database->commit());

    return result;
}

}
