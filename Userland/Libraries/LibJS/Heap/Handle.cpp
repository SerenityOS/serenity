/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

HandleImpl::HandleImpl(Cell* cell)
    : m_cell(cell)
{
    m_cell->heap().did_create_handle({}, *this);
}

HandleImpl::~HandleImpl()
{
    m_cell->heap().did_destroy_handle({}, *this);
}

}
