/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL {

Row::Row(RefPtr<TableDef> table, u32 pointer)
    : Tuple(table->to_tuple_descriptor())
    , m_table(table)
{
    set_pointer(pointer);
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

}
