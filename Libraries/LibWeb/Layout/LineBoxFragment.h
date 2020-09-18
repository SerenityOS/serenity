/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Weakable.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Forward.h>

namespace Web {

class LineBoxFragment : public Weakable<LineBoxFragment> {
    friend class LineBox;

public:
    LineBoxFragment(const LayoutNode& layout_node, int start, int length, const Gfx::FloatPoint& offset, const Gfx::FloatSize& size)
        : m_layout_node(layout_node)
        , m_start(start)
        , m_length(length)
        , m_offset(offset)
        , m_size(size)
    {
    }

    const LayoutNode& layout_node() const { return m_layout_node; }
    int start() const { return m_start; }
    int length() const { return m_length; }
    const Gfx::FloatRect absolute_rect() const;

    const Gfx::FloatPoint& offset() const { return m_offset; }
    void set_offset(const Gfx::FloatPoint& offset) { m_offset = offset; }

    const Gfx::FloatSize& size() const { return m_size; }
    void set_width(float width) { m_size.set_width(width); }
    float width() const { return m_size.width(); }
    float height() const { return m_size.height(); }

    float absolute_x() const { return absolute_rect().x(); }

    void paint(PaintContext&);

    bool ends_in_whitespace() const;
    bool is_justifiable_whitespace() const;
    StringView text() const;

    int text_index_at(float x) const;

    Gfx::FloatRect selection_rect(const Gfx::Font&) const;

private:
    const LayoutNode& m_layout_node;
    int m_start { 0 };
    int m_length { 0 };
    Gfx::FloatPoint m_offset;
    Gfx::FloatSize m_size;
};

}
