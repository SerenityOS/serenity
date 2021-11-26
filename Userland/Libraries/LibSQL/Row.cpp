/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>
#include <LibSQL/Serializer.h>
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

Row::Row(RefPtr<TableDef> table, u32 pointer)
    : Tuple(table->to_tuple_descriptor())
    , m_table(table)
{
    set_pointer(pointer);
}

Row::Row(RefPtr<TableDef> table, u32 pointer, Serializer& serializer)
    : Row(move(table), pointer)
{
    Row::deserialize(serializer);
}

void Row::deserialize(Serializer& serializer)
{
    Tuple::deserialize(serializer);
    m_next_pointer = serializer.deserialize<u32>();
}

void Row::serialize(Serializer& serializer) const
{
    Tuple::serialize(serializer);
    serializer.serialize<u32>(next_pointer());
}

void Row::copy_from(Row const& other)
{
    Tuple::copy_from(other);
    m_next_pointer = other.next_pointer();
}

}
