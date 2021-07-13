/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Meta.h>

namespace SQL {

class IndexNode {
public:
    virtual ~IndexNode() = default;
    [[nodiscard]] u32 pointer() const { return m_pointer; }
    virtual void serialize(ByteBuffer&) const = 0;
    virtual IndexNode* as_index_node() = 0;

protected:
    explicit IndexNode(u32 pointer)
        : m_pointer(pointer)
    {
    }

    void set_pointer(u32 pointer) { m_pointer = pointer; }

private:
    u32 m_pointer;
};

class Index : public Core::Object {
    C_OBJECT_ABSTRACT(Index);

public:
    ~Index() override = default;

    NonnullRefPtr<TupleDescriptor> descriptor() const { return m_descriptor; }
    [[nodiscard]] bool duplicates_allowed() const { return !m_unique; }
    [[nodiscard]] bool unique() const { return m_unique; }
    [[nodiscard]] u32 pointer() const { return m_pointer; }

protected:
    Index(Heap& heap, NonnullRefPtr<TupleDescriptor> const&, bool unique, u32 pointer);
    Index(Heap& heap, NonnullRefPtr<TupleDescriptor> const&, u32 pointer);

    [[nodiscard]] Heap const& heap() const { return m_heap; }
    [[nodiscard]] Heap& heap() { return m_heap; }
    void set_pointer(u32 pointer) { m_pointer = pointer; }
    u32 new_record_pointer() { return m_heap.new_record_pointer(); }
    ByteBuffer read_block(u32);
    void add_to_write_ahead_log(IndexNode*);

private:
    Heap& m_heap;
    NonnullRefPtr<TupleDescriptor> m_descriptor;
    bool m_unique { false };
    u32 m_pointer { 0 };
};

}
