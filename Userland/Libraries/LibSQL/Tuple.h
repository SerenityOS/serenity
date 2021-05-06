/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>
#include <LibSQL/StorageForward.h>
#include <LibSQL/Value.h>

namespace SQL {

class Tuple {
public:
    Tuple();
    explicit Tuple(TableDef const&);
    Tuple(TableDef const&, u32, ByteBuffer &);
    Tuple(Tuple const&);
    ~Tuple() = default;

    Tuple& operator = (Tuple const&);
    explicit operator String() const;
    bool operator==(Tuple const &) const;
    bool operator!=(Tuple const &) const;

    [[nodiscard]] bool has(String) const;
    Value const& operator [](size_t) const;
    Value& operator [](size_t);
    Value const& operator [](String) const;
    Value& operator [](String);

    [[nodiscard]] RefPtr<TableDef> table() const { return m_table; }
    [[nodiscard]] u32 pointer() const { return m_pointer; }
    void pointer(u32 ptr) { m_pointer = ptr; }
    [[nodiscard]] u32 next_pointer() const { return m_next_pointer; }
    void next_pointer(u32 ptr) { m_next_pointer = ptr; }

    [[nodiscard]] size_t size() const;
    [[nodiscard]] size_t length() const { return m_values.size(); }
    void serialize(ByteBuffer&) const;
    bool match(Key const &) const;

private:
    [[nodiscard]] int index_of(String) const;
    void copy_from(Tuple const&);

    RefPtr<TableDef> m_table;
    Vector<Value> m_values;
    u32 m_pointer { 0 };
    u32 m_next_pointer { 0 };
};

}
