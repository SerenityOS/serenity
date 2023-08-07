/*
 * Copyright (c) 2021, Jan de Visser <jan@de-visser.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibSQL/Forward.h>
#include <LibSQL/Meta.h>
#include <LibSQL/Serializer.h>

namespace SQL {

class IndexNode {
public:
    virtual ~IndexNode() = default;
    [[nodiscard]] Block::Index block_index() const { return m_block_index; }
    IndexNode* as_index_node() { return dynamic_cast<IndexNode*>(this); }

protected:
    explicit IndexNode(Block::Index block_index)
        : m_block_index(block_index)
    {
    }

    void set_block_index(Block::Index block_index) { m_block_index = block_index; }

private:
    Block::Index m_block_index;
};

class Index : public RefCounted<Index> {
public:
    virtual ~Index() = default;

    NonnullRefPtr<TupleDescriptor> descriptor() const { return m_descriptor; }
    [[nodiscard]] bool duplicates_allowed() const { return !m_unique; }
    [[nodiscard]] bool unique() const { return m_unique; }
    [[nodiscard]] Block::Index block_index() const { return m_block_index; }

protected:
    Index(Serializer&, NonnullRefPtr<TupleDescriptor> const&, bool unique, Block::Index block_index);
    Index(Serializer&, NonnullRefPtr<TupleDescriptor> const&, Block::Index block_index);

    [[nodiscard]] Serializer& serializer() { return m_serializer; }
    void set_block_index(Block::Index block_index) { m_block_index = block_index; }
    u32 request_new_block_index() { return m_serializer.request_new_block_index(); }

private:
    Serializer m_serializer;
    NonnullRefPtr<TupleDescriptor> m_descriptor;
    bool m_unique { false };
    Block::Index m_block_index { 0 };
};

}
