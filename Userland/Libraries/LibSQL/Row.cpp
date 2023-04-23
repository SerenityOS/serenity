/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSQL/Meta.h>
#include <LibSQL/Row.h>

namespace SQL {

Row::Row(NonnullRefPtr<TableDef> table, Block::Index block_index)
    : Tuple(table->to_tuple_descriptor())
    , m_table(move(table))
{
    set_block_index(block_index);
}

void Row::deserialize(Serializer& serializer)
{
    Tuple::deserialize(serializer);
    m_next_block_index = serializer.deserialize<Block::Index>();
}

void Row::serialize(Serializer& serializer) const
{
    Tuple::serialize(serializer);
    serializer.serialize<Block::Index>(next_block_index());
}

}
