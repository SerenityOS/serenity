/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Key.h>
#include <LibSQL/Meta.h>

namespace SQL {

Key::Key(TupleDescriptor const& descriptor)
    : Tuple(descriptor)
{
}

Key::Key(RefPtr<IndexDef> index)
    : Tuple(index->to_tuple_descriptor())
    , m_index(index)
{
}

Key::Key(TupleDescriptor const& descriptor, ByteBuffer& buffer, size_t& offset)
    : Tuple(descriptor, buffer, offset)
{
}

Key::Key(RefPtr<IndexDef> index, ByteBuffer& buffer, size_t& offset)
    : Key(index->to_tuple_descriptor())
{
    deserialize(buffer, offset);
}

}
