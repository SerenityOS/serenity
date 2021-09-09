/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Window.h>

namespace Web::DOM {

NonnullRefPtr<Range> Range::create(Window& window)
{
    return Range::create(window.associated_document());
}

NonnullRefPtr<Range> Range::create(Document& document)
{
    return adopt_ref(*new Range(document));
}

NonnullRefPtr<Range> Range::create(Node& start_container, size_t start_offset, Node& end_container, size_t end_offset)
{
    return adopt_ref(*new Range(start_container, start_offset, end_container, end_offset));
}
NonnullRefPtr<Range> Range::create_with_global_object(Bindings::WindowObject& window)
{
    return Range::create(window.impl());
}

Range::Range(Document& document)
    : Range(document, 0, document, 0)
{
}

Range::Range(Node& start_container, size_t start_offset, Node& end_container, size_t end_offset)
    : m_start_container(start_container)
    , m_start_offset(start_offset)
    , m_end_container(end_container)
    , m_end_offset(end_offset)
{
}

NonnullRefPtr<Range> Range::clone_range() const
{
    return adopt_ref(*new Range(const_cast<Node&>(*m_start_container), m_start_offset, const_cast<Node&>(*m_end_container), m_end_offset));
}

NonnullRefPtr<Range> Range::inverted() const
{
    return adopt_ref(*new Range(const_cast<Node&>(*m_end_container), m_end_offset, const_cast<Node&>(*m_start_container), m_start_offset));
}

NonnullRefPtr<Range> Range::normalized() const
{
    if (m_start_container.ptr() == m_end_container.ptr()) {
        if (m_start_offset <= m_end_offset)
            return clone_range();

        return inverted();
    }

    if (m_start_container->is_before(m_end_container))
        return clone_range();

    return inverted();
}

}
