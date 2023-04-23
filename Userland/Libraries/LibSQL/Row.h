/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Tuple.h>
#include <LibSQL/Value.h>

namespace SQL {

/**
 * A Tuple is an element of a sequential-access persistence data structure
 * like a flat table. Like a key it has a definition for all its parts,
 * but unlike a key this definition is not optional.
 *
 * FIXME Tuples should logically belong to a TupleStore object, but right now
 * they stand by themselves; they contain a row's worth of data and a pointer
 * to the next Tuple.
 */
class Row : public Tuple {
public:
    explicit Row(NonnullRefPtr<TableDef>, Block::Index block_index = 0);
    virtual ~Row() override = default;

    [[nodiscard]] Block::Index next_block_index() const { return m_next_block_index; }
    void set_next_block_index(Block::Index index) { m_next_block_index = index; }

    TableDef const& table() const { return *m_table; }
    TableDef& table() { return *m_table; }

    [[nodiscard]] virtual size_t length() const override { return Tuple::length() + sizeof(Block::Index); }
    virtual void serialize(Serializer&) const override;
    virtual void deserialize(Serializer&) override;

private:
    NonnullRefPtr<TableDef> m_table;
    Block::Index m_next_block_index { 0 };
};

}
