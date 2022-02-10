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

ResultOr<ResultSet> Statement::execute(AK::NonnullRefPtr<Database> database) const
{
    ExecutionContext context { move(database), this, nullptr };
    return execute(context);
}

}
