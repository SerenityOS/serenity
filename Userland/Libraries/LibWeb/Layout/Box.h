/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/StackingContext.h>

namespace Web::Layout {

class Box : public NodeWithStyleAndBoxModelMetrics {
public:
    struct OverflowData {
        Gfx::FloatRect scrollable_overflow_rect;
        Gfx::FloatPoint scroll_offset;
    };

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
        auto absolute_rect = this->absolute_rect();
        Gfx::FloatRect rect;
        rect.set_x(absolute_rect.x() - box_model().padding.left);
        rect.set_width(width() + box_model().padding.left + box_model().padding.right);
        rect.set_y(absolute_rect.y() - box_model().padding.top);
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

    Gfx::FloatRect border_box_as_relative_rect() const
    {
        auto rect = content_box_as_relative_rect();
        auto border_box = box_model().border_box();
        rect.set_x(rect.x() - border_box.left);
        rect.set_width(rect.width() + border_box.left + border_box.right);
        rect.set_y(rect.y() - border_box.top);
        rect.set_height(rect.height() + border_box.top + border_box.bottom);
        return rect;
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

    bool is_out_of_flow(FormattingContext const&) const;

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;
    virtual void set_needs_display() override;

    bool is_body() const;

    void set_containing_line_box_fragment(LineBoxFragment&);

    StackingContext* stacking_context() { return m_stacking_context; }
    const StackingContext* stacking_context() const { return m_stacking_context; }
    void set_stacking_context(NonnullOwnPtr<StackingContext> context) { m_stacking_context = move(context); }
    StackingContext* enclosing_stacking_context();

    virtual void paint(PaintContext&, PaintPhase) override;
    virtual void paint_border(PaintContext& context);
    virtual void paint_box_shadow(PaintContext& context);
    virtual void paint_background(PaintContext& context);

    Painting::BorderRadiusData normalized_border_radius_data();

    virtual Optional<float> intrinsic_width() const { return {}; }
    virtual Optional<float> intrinsic_height() const { return {}; }
    virtual Optional<float> intrinsic_aspect_ratio() const { return {}; }

    bool has_intrinsic_width() const { return intrinsic_width().has_value(); }
    bool has_intrinsic_height() const { return intrinsic_height().has_value(); }
    bool has_intrinsic_aspect_ratio() const { return intrinsic_aspect_ratio().has_value(); }

    bool has_overflow() const { return m_overflow_data; }

    Optional<Gfx::FloatRect> scrollable_overflow_rect() const
    {
        if (!m_overflow_data)
            return {};
        return m_overflow_data->scrollable_overflow_rect;
    }

    OverflowData& ensure_overflow_data()
    {
        if (!m_overflow_data)
            m_overflow_data = make<OverflowData>();
        return *m_overflow_data;
    }

    void clear_overflow_data() { m_overflow_data = nullptr; }

    virtual void before_children_paint(PaintContext&, PaintPhase) override;
    virtual void after_children_paint(PaintContext&, PaintPhase) override;

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

private:
    virtual bool is_box() const final { return true; }

    Gfx::FloatPoint m_offset;
    Gfx::FloatSize m_size;

    // Some boxes hang off of line box fragments. (inline-block, inline-table, replaced, etc)
    WeakPtr<LineBoxFragment> m_containing_line_box_fragment;

    OwnPtr<StackingContext> m_stacking_context;

    OwnPtr<OverflowData> m_overflow_data;
};

template<>
inline bool Node::fast_is<Box>() const { return is_box(); }

}
