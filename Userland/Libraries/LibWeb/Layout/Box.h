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
#include <LibWeb/Layout/LineBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

class Box : public NodeWithStyleAndBoxModelMetrics {
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

    Gfx::FloatRect padded_rect() const
    {
        Gfx::FloatRect rect;
        rect.set_x(absolute_x() - box_model().padding.left);
        rect.set_width(width() + box_model().padding.left + box_model().padding.right);
        rect.set_y(absolute_y() - box_model().padding.top);
        rect.set_height(height() + box_model().padding.top + box_model().padding.bottom);
        return rect;
    }

    Gfx::FloatRect bordered_rect() const
    {
        auto padded_rect = this->padded_rect();
        Gfx::FloatRect rect;
        rect.set_x(padded_rect.x() - box_model().border.left);
        rect.set_width(padded_rect.width() + box_model().border.left + box_model().border.right);
        rect.set_y(padded_rect.y() - box_model().border.top);
        rect.set_height(padded_rect.height() + box_model().border.top + box_model().border.bottom);
        return rect;
    }

    float margin_box_width() const
    {
        auto margin_box = box_model().margin_box();
        return width() + margin_box.left + margin_box.right;
    }

    float margin_box_height() const
    {
        auto margin_box = box_model().margin_box();
        return height() + margin_box.top + margin_box.bottom;
    }

    float border_box_width() const
    {
        auto border_box = box_model().border_box();
        return width() + border_box.left + border_box.right;
    }

    float border_box_height() const
    {
        auto border_box = box_model().border_box();
        return height() + border_box.top + border_box.bottom;
    }

    Gfx::FloatRect content_box_as_relative_rect() const
    {
        return { m_offset, m_size };
    }

    Gfx::FloatRect margin_box_as_relative_rect() const
    {
        auto rect = content_box_as_relative_rect();
        auto margin_box = box_model().margin_box();
        rect.set_x(rect.x() - margin_box.left);
        rect.set_width(rect.width() + margin_box.left + margin_box.right);
        rect.set_y(rect.y() - margin_box.top);
        rect.set_height(rect.height() + margin_box.top + margin_box.bottom);
        return rect;
    }

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

    Vector<LineBox>& line_boxes() { return m_line_boxes; }
    const Vector<LineBox>& line_boxes() const { return m_line_boxes; }

    LineBox& ensure_last_line_box();
    LineBox& add_line_box();

    virtual float width_of_logical_containing_block() const;

protected:
    Box(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> style)
        : NodeWithStyleAndBoxModelMetrics(document, node, move(style))
    {
    }

    Box(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
        : NodeWithStyleAndBoxModelMetrics(document, node, move(computed_values))
    {
    }

    virtual void did_set_rect() { }

    Vector<LineBox> m_line_boxes;

private:
    virtual bool is_box() const final { return true; }

    Gfx::FloatPoint m_offset;
    Gfx::FloatSize m_size;

    // Some boxes hang off of line box fragments. (inline-block, inline-table, replaced, etc)
    WeakPtr<LineBoxFragment> m_containing_line_box_fragment;

    OwnPtr<StackingContext> m_stacking_context;
};

template<>
inline bool Node::fast_is<Box>() const { return is_box(); }

}
