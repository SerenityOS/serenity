/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Heap.h>
#include <LibSQL/Index.h>
#include <LibSQL/Meta.h>

namespace SQL {

Index::Index(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, bool unique, u32 pointer)
    : m_serializer(serializer)
    , m_descriptor(descriptor)
    , m_unique(unique)
    , m_pointer(pointer)
{
}

Index::Index(Serializer& serializer, NonnullRefPtr<TupleDescriptor> const& descriptor, u32 pointer)
    : m_serializer(serializer)
    , m_descriptor(descriptor)
    , m_pointer(pointer)
{
}

}
