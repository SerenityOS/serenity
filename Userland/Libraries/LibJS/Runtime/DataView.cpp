/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/DataView.h>

namespace JS {

NonnullGCPtr<DataView> DataView::create(Realm& realm, ArrayBuffer* viewed_buffer, size_t byte_length, size_t byte_offset)
{
    return realm.heap().allocate<DataView>(realm, viewed_buffer, byte_length, byte_offset, realm.intrinsics().data_view_prototype());
}

DataView::DataView(ArrayBuffer* viewed_buffer, size_t byte_length, size_t byte_offset, Object& prototype)
    : Object(ConstructWithPrototypeTag::Tag, prototype)
    , m_viewed_array_buffer(viewed_buffer)
    , m_byte_length(byte_length)
    , m_byte_offset(byte_offset)
{
}

void DataView::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_viewed_array_buffer);
}

}
