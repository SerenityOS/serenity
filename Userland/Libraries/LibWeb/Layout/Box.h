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

    Gfx::FloatSize const& content_size() const { return m_content_size; }
    void set_content_size(Gfx::FloatSize const&);
    void set_content_size(float width, float height) { set_content_size({ width, height }); }

    void set_content_width(float width) { set_content_size(width, content_height()); }
    void set_content_height(float height) { set_content_size(content_width(), height); }
    float content_width() const { return m_content_size.width(); }
    float content_height() const { return m_content_size.height(); }

    Gfx::FloatRect absolute_padding_box_rect() const
    {
        auto absolute_rect = this->absolute_rect();
        Gfx::FloatRect rect;
        rect.set_x(absolute_rect.x() - box_model().padding.left);
        rect.set_width(content_width() + box_model().padding.left + box_model().padding.right);
        rect.set_y(absolute_rect.y() - box_model().padding.top);
        rect.set_height(content_height() + box_model().padding.top + box_model().padding.bottom);
        return rect;
    }

    Gfx::FloatRect absolute_border_box_rect() const
    {
        auto padded_rect = this->absolute_padding_box_rect();
        Gfx::FloatRect rect;
        rect.set_x(padded_rect.x() - box_model().border.left);
        rect.set_width(padded_rect.width() + box_model().border.left + box_model().border.right);
        rect.set_y(padded_rect.y() - box_model().border.top);
        rect.set_height(padded_rect.height() + box_model().border.top + box_model().border.bottom);
        return rect;
    }

    float border_box_width() const
    {
        auto border_box = box_model().border_box();
        return content_width() + border_box.left + border_box.right;
    }

    float border_box_height() const
    {
        auto border_box = box_model().border_box();
        return content_height() + border_box.top + border_box.bottom;
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

    void set_overflow_data(OwnPtr<OverflowData> data) { m_overflow_data = move(data); }

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
    Gfx::FloatSize m_content_size;

    // Some boxes hang off of line box fragments. (inline-block, inline-table, replaced, etc)
    WeakPtr<LineBoxFragment> m_containing_line_box_fragment;

    OwnPtr<StackingContext> m_stacking_context;

    OwnPtr<OverflowData> m_overflow_data;
};

template<>
inline bool Node::fast_is<Box>() const { return is_box(); }

}
