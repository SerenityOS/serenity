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
    IndexNode* as_index_node() { return dynamic_cast<IndexNode*>(this); }

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
    Index(Serializer&, NonnullRefPtr<TupleDescriptor> const&, bool unique, u32 pointer);
    Index(Serializer&, NonnullRefPtr<TupleDescriptor> const&, u32 pointer);

    [[nodiscard]] Serializer& serializer() { return m_serializer; }
    void set_pointer(u32 pointer) { m_pointer = pointer; }
    u32 new_record_pointer() { return m_serializer.new_record_pointer(); }
    //    ByteBuffer read_block(u32);

private:
    Serializer m_serializer;
    NonnullRefPtr<TupleDescriptor> m_descriptor;
    bool m_unique { false };
    u32 m_pointer { 0 };
};

}
