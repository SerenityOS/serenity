/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/RefPtr.h>
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
    Row();
    explicit Row(TupleDescriptor const&);
    explicit Row(RefPtr<TableDef>, u32 pointer = 0);
    Row(RefPtr<TableDef>, u32, Serializer&);
    Row(Row const&) = default;
    virtual ~Row() override = default;

    [[nodiscard]] u32 next_pointer() const { return m_next_pointer; }
    void next_pointer(u32 ptr) { m_next_pointer = ptr; }
    RefPtr<TableDef> table() const { return m_table; }
    [[nodiscard]] virtual size_t length() const override { return Tuple::length() + sizeof(u32); }
    virtual void serialize(Serializer&) const override;
    virtual void deserialize(Serializer&) override;

protected:
    void copy_from(Row const&);

private:
    RefPtr<TableDef> m_table;
    u32 m_next_pointer { 0 };
};

}
