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

#include <AK/OwnPtr.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web {

class LayoutBox : public LayoutNodeWithStyleAndBoxModelMetrics {
public:
    const Gfx::FloatRect absolute_rect() const;

    Gfx::FloatPoint effective_offset() const;

    void set_offset(const Gfx::FloatPoint& offset);
    void set_offset(float x, float y) { set_offset({ x, y }); }

    const Gfx::FloatSize& size() const { return m_size; }
    void set_size(const Gfx::FloatSize&);
    void set_size(float width, float height) { set_size({ width, height }); }

    void set_width(float width) { set_size(width, height()); }
    void set_height(float height) { set_size(width(), height); }
    float width() const { return m_size.width(); }
    float height() const { return m_size.height(); }

    float absolute_x() const { return absolute_rect().x(); }
    float absolute_y() const { return absolute_rect().y(); }
    Gfx::FloatPoint absolute_position() const { return absolute_rect().location(); }

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;
    virtual void set_needs_display() override;

    bool is_body() const;

    void set_containing_line_box_fragment(LineBoxFragment&);

    bool establishes_stacking_context() const;
    StackingContext* stacking_context() { return m_stacking_context; }
    const StackingContext* stacking_context() const { return m_stacking_context; }
    void set_stacking_context(NonnullOwnPtr<StackingContext> context) { m_stacking_context = move(context); }
    StackingContext* enclosing_stacking_context();

    virtual void paint(PaintContext&, PaintPhase) override;

protected:
    LayoutBox(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
        : LayoutNodeWithStyleAndBoxModelMetrics(document, node, move(style))
    {
    }

    virtual void did_set_rect() { }

private:
    virtual bool is_box() const final { return true; }

    enum class Edge {
        Top,
        Right,
        Bottom,
        Left,
    };
    void paint_border(PaintContext&, Edge, const Gfx::FloatRect&, CSS::PropertyID style_property_id, const BorderData&);

    Gfx::FloatPoint m_offset;
    Gfx::FloatSize m_size;

    // Some boxes hang off of line box fragments. (inline-block, inline-table, replaced, etc)
    WeakPtr<LineBoxFragment> m_containing_line_box_fragment;

    OwnPtr<StackingContext> m_stacking_context;
};

}

AK_BEGIN_TYPE_TRAITS(Web::LayoutBox)
static bool is_type(const Web::LayoutNode& layout_node) { return layout_node.is_box(); }
AK_END_TYPE_TRAITS()
