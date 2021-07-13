/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Heap.h>
#include <LibSQL/Index.h>
#include <LibSQL/Meta.h>

namespace SQL {

Index::Index(Heap& heap, NonnullRefPtr<TupleDescriptor> const& descriptor, bool unique, u32 pointer)
    : m_heap(heap)
    , m_descriptor(descriptor)
    , m_unique(unique)
    , m_pointer(pointer)
{
}

Index::Index(Heap& heap, NonnullRefPtr<TupleDescriptor> const& descriptor, u32 pointer)
    : m_heap(heap)
    , m_descriptor(descriptor)
    , m_pointer(pointer)
{
}

ByteBuffer Index::read_block(u32 block)
{
    auto ret = m_heap.read_block(block);
    if (ret.is_error()) {
        warnln("Error reading block {}: {}", block, ret.error());
        VERIFY_NOT_REACHED();
    }
    return ret.value();
}

void Index::add_to_write_ahead_log(IndexNode* node)
{
    VERIFY(node->pointer());
    ByteBuffer buffer;
    node->serialize(buffer);
    m_heap.add_to_wal(node->pointer(), buffer);
}

}
