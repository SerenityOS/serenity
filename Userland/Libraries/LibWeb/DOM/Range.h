/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/AbstractRange.h>

namespace Web::DOM {

class Range final : public AbstractRange {
public:
    using WrapperType = Bindings::RangeWrapper;

    virtual ~Range() override;

    static NonnullRefPtr<Range> create(Document&);
    static NonnullRefPtr<Range> create(Window&);
    static NonnullRefPtr<Range> create(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset);
    static NonnullRefPtr<Range> create_with_global_object(Bindings::WindowObject&);

    // FIXME: There are a ton of methods missing here.

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

    NonnullRefPtr<Node> common_ancestor_container() const;

private:
    explicit Range(Document&);

    Range(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset);
};

}
