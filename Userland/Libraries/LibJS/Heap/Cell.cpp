/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

ThrowCompletionOr<void> JS::Cell::initialize(JS::Realm&)
{
    return {};
}

void JS::Cell::Visitor::visit(JS::Value value)
{
    if (value.is_cell())
        visit_impl(value.as_cell());
}

}
