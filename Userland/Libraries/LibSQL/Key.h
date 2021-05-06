/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Meta.h"
#include <LibCore/Object.h>
#include <LibSQL/StorageForward.h>
#include <LibSQL/Value.h>

namespace SQL {

class Key {
public:
    Key();
    explicit Key(IndexDef const&);
    Key(IndexDef const&, ByteBuffer &, size_t &);
    Key(Key const&);
    ~Key() = default;

    Key & operator = (Key const&);
    explicit operator String() const;

    bool operator < (Key const&) const;
    bool operator <= (Key const&) const;
    bool operator == (Key const&) const;
    bool operator != (Key const&) const;
    bool operator > (Key const&) const;
    bool operator >= (Key const&) const;

    [[nodiscard]] bool is_null() const;
    [[nodiscard]] bool has(String) const;
    Value const& operator [](size_t) const;
    Value& operator [](size_t);
    Value const& operator [](String) const;
    Value& operator [](String);
    void append(Value const&);
    Key& operator += (Value const&);
    [[nodiscard]] bool is_compatible(Key const&) const;

    [[nodiscard]] u32 pointer() const { return m_pointer; }
    void pointer(u32 ptr) { m_pointer = ptr; }

    [[nodiscard]] size_t size() const;
    [[nodiscard]] size_t length() const { return m_part_definitions.size(); }
    [[nodiscard]] NonnullRefPtrVector<KeyPartDef> parts() const { return m_part_definitions; }
    void serialize(ByteBuffer&) const;
    [[nodiscard]] int compare(Key const&) const;
    [[nodiscard]] int match(Key const&) const;

private:
    NonnullRefPtrVector<KeyPartDef> m_part_definitions;
    Vector<Value> m_key_data;
    u32 m_pointer { 0 };

    [[nodiscard]] int index_of(String) const;
    void copy_from(Key const&);
};

}
