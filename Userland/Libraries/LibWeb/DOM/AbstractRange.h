/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::DOM {

class AbstractRange
    : public RefCounted<AbstractRange>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::AbstractRangeWrapper;

    virtual ~AbstractRange() override = default;

    Node* start_container() { return m_start_container; }
    const Node* start_container() const { return m_start_container; }
    unsigned start_offset() const { return m_start_offset; }

    Node* end_container() { return m_end_container; }
    const Node* end_container() const { return m_end_container; }
    unsigned end_offset() const { return m_end_offset; }

    // https://dom.spec.whatwg.org/#range-collapsed
    bool collapsed() const
    {
        // A range is collapsed if its start node is its end node and its start offset is its end offset.
        return start_container() == end_container() && start_offset() == end_offset();
    }

protected:
    AbstractRange(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset)
        : m_start_container(start_container)
        , m_start_offset(start_offset)
        , m_end_container(end_container)
        , m_end_offset(end_offset)
    {
    }

    NonnullRefPtr<Node> m_start_container;
    u32 m_start_offset;

    NonnullRefPtr<Node> m_end_container;
    u32 m_end_offset;
};

}
