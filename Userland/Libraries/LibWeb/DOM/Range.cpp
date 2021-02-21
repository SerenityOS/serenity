/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/Range.h>
#include <LibWeb/DOM/Window.h>

namespace Web::DOM {

NonnullRefPtr<Range> Range::create(Window& window)
{
    return Range::create(window.document());
}

NonnullRefPtr<Range> Range::create(Document& document)
{
    return adopt(*new Range(document));
}

NonnullRefPtr<Range> Range::create(Node& start_container, size_t start_offset, Node& end_container, size_t end_offset)
{
    return adopt(*new Range(start_container, start_offset, end_container, end_offset));
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
    return adopt(*new Range(const_cast<Node&>(*m_start_container), m_start_offset, const_cast<Node&>(*m_end_container), m_end_offset));
}

NonnullRefPtr<Range> Range::inverted() const
{
    return adopt(*new Range(const_cast<Node&>(*m_end_container), m_end_offset, const_cast<Node&>(*m_start_container), m_start_offset));
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
