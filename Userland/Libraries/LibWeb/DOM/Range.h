/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

class Range final
    : public RefCounted<Range>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::RangeWrapper;

    static NonnullRefPtr<Range> create(Document&);
    static NonnullRefPtr<Range> create(Window&);
    static NonnullRefPtr<Range> create(Node& start_container, size_t start_offset, Node& end_container, size_t end_offset);
    static NonnullRefPtr<Range> create_with_global_object(Bindings::WindowObject&);

    // FIXME: There are a ton of methods missing here.

    Node* start_container() { return m_start_container; }
    Node const* start_container() const { return m_start_container; }
    unsigned start_offset() const { return m_start_offset; }

    Node* end_container() { return m_end_container; }
    Node const* end_container() const { return m_end_container; }
    unsigned end_offset() const { return m_end_offset; }

    bool collapsed() const
    {
        return start_container() == end_container() && start_offset() == end_offset();
    }

    void set_start(Node& container, unsigned offset)
    {
        m_start_container = container;
        m_start_offset = offset;
    }

    void set_end(Node& container, unsigned offset)
    {
        m_end_container = container;
        m_end_offset = offset;
    }

    NonnullRefPtr<Range> inverted() const;
    NonnullRefPtr<Range> normalized() const;
    NonnullRefPtr<Range> clone_range() const;

private:
    explicit Range(Document&);

    Range(Node& start_container, size_t start_offset, Node& end_container, size_t end_offset);

    NonnullRefPtr<Node> m_start_container;
    unsigned m_start_offset;

    NonnullRefPtr<Node> m_end_container;
    unsigned m_end_offset;
};

}
