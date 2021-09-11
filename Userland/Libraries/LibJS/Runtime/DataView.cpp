/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/DataView.h>

namespace JS {

DataView* DataView::create(GlobalObject& global_object, ArrayBuffer* viewed_buffer, size_t byte_length, size_t byte_offset)
{
    return global_object.heap().allocate<DataView>(global_object, viewed_buffer, byte_length, byte_offset, *global_object.data_view_prototype());
}

DataView::DataView(ArrayBuffer* viewed_buffer, size_t byte_length, size_t byte_offset, Object& prototype)
    : Object(prototype)
    , m_viewed_array_buffer(viewed_buffer)
    , m_byte_length(byte_length)
    , m_byte_offset(byte_offset)
{
}

DataView::~DataView()
{
}

void DataView::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

}
