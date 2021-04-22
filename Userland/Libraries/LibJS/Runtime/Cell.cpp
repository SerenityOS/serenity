/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Heap/HeapBlock.h>
#include <LibJS/Runtime/Cell.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

void Cell::Visitor::visit(Cell* cell)
{
    if (cell)
        visit_impl(cell);
}

void Cell::Visitor::visit(Value value)
{
    if (value.is_cell())
        visit_impl(value.as_cell());
}

}
