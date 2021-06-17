/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Serialize.h>
#include <LibSQL/Tuple.h>

namespace SQL {

Row::Row()
    : Tuple()
{
}

Row::Row(TupleDescriptor const& descriptor)
    : Tuple(descriptor)
{
}

Row::Row(RefPtr<TableDef> table)
    : Tuple(table->to_tuple_descriptor())
    , m_table(table)
{
}

Row::Row(RefPtr<TableDef> table, u32 pointer, ByteBuffer& buffer)
    : Tuple(table->to_tuple_descriptor())
{
    // FIXME Sanitize constructor situation in Tuple so this can be better
    size_t offset = 0;
    deserialize(buffer, offset);
    deserialize_from<u32>(buffer, offset, m_next_pointer);
    set_pointer(pointer);
}

void Row::serialize(ByteBuffer& buffer) const
{
    Tuple::serialize(buffer);
    serialize_to<u32>(buffer, next_pointer());
}

void Row::copy_from(Row const& other)
{
    Tuple::copy_from(other);
    m_next_pointer = other.next_pointer();
}

}
