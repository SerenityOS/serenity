/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Heap.h>
#include <LibSQL/Index.h>
#include <LibSQL/TupleDescriptor.h>

namespace SQL {

Index::Index(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, bool unique, Block::Index block_index)
    : m_serializer(serializer)
    , m_descriptor(descriptor)
    , m_unique(unique)
    , m_block_index(block_index)
{
}

Index::Index(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, Block::Index block_index)
    : m_serializer(serializer)
    , m_descriptor(descriptor)
    , m_block_index(block_index)
{
}

}
