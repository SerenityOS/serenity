/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/Paintable.h>
#include <LibWeb/Painting/ShadowPainting.h>

namespace Web::Painting {

class PaintableBox : public Paintable {
public:
    static NonnullRefPtr<PaintableBox> create(Layout::Box const&);
    virtual ~PaintableBox();

    virtual void paint(PaintContext&, PaintPhase) const override;

    bool is_visible() const { return layout_box().is_visible(); }

    Layout::Box& layout_box() { return static_cast<Layout::Box&>(Paintable::layout_node()); }
    Layout::Box const& layout_box() const { return static_cast<Layout::Box const&>(Paintable::layout_node()); }

    auto const& box_model() const { return layout_box().box_model(); }

    struct OverflowData {
        Gfx::FloatRect scrollable_overflow_rect;
        Gfx::FloatPoint scroll_offset;
    };

    Gfx::FloatRect absolute_rect() const;
    Gfx::FloatPoint effective_offset() const;

    void set_offset(Gfx::FloatPoint const&);
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

    Gfx::FloatRect absolute_paint_rect() const;

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

    bool has_overflow() const { return m_overflow_data.has_value(); }

    Optional<Gfx::FloatRect> scrollable_overflow_rect() const
    {
        if (!m_overflow_data.has_value())
            return {};
        return m_overflow_data->scrollable_overflow_rect;
    }

    void set_overflow_data(Optional<OverflowData> data) { m_overflow_data = move(data); }
    void set_containing_line_box_fragment(Optional<Layout::LineBoxFragmentCoordinate>);

    StackingContext* stacking_context() { return m_stacking_context; }
    StackingContext const* stacking_context() const { return m_stacking_context; }
    void set_stacking_context(NonnullOwnPtr<Painting::StackingContext>);
    StackingContext* enclosing_stacking_context();

    DOM::Node const* dom_node() const { return layout_box().dom_node(); }
    DOM::Node* dom_node() { return layout_box().dom_node(); }

    DOM::Document const& document() const { return layout_box().document(); }
    DOM::Document& document() { return layout_box().document(); }

    virtual void before_children_paint(PaintContext&, PaintPhase, ShouldClipOverflow) const override;
    virtual void after_children_paint(PaintContext&, PaintPhase, ShouldClipOverflow) const override;

    virtual Optional<HitTestResult> hit_test(Gfx::FloatPoint const&, HitTestType) const override;

    void invalidate_stacking_context();

    bool is_out_of_view(PaintContext&) const;

protected:
    explicit PaintableBox(Layout::Box const&);

    virtual void paint_border(PaintContext&) const;
    virtual void paint_backdrop_filter(PaintContext&) const;
    virtual void paint_background(PaintContext&) const;
    virtual void paint_box_shadow(PaintContext&) const;

    virtual Gfx::FloatRect compute_absolute_rect() const;
    virtual Gfx::FloatRect compute_absolute_paint_rect() const;

    enum class ShrinkRadiiForBorders {
        Yes,
        No
    };

    Painting::BorderRadiiData normalized_border_radii_data(ShrinkRadiiForBorders shrink = ShrinkRadiiForBorders::No) const;

    Vector<ShadowData> resolve_box_shadow_data() const;

private:
    Optional<OverflowData> m_overflow_data;

    Gfx::FloatPoint m_offset;
    Gfx::FloatSize m_content_size;

    // Some boxes hang off of line box fragments. (inline-block, inline-table, replaced, etc)
    Optional<Layout::LineBoxFragmentCoordinate> m_containing_line_box_fragment;

    OwnPtr<Painting::StackingContext> m_stacking_context;

    Optional<Gfx::FloatRect> mutable m_absolute_rect;
    Optional<Gfx::FloatRect> mutable m_absolute_paint_rect;

    mutable bool m_clipping_overflow { false };
    Optional<BorderRadiusCornerClipper> mutable m_overflow_corner_radius_clipper;
};

class PaintableWithLines : public PaintableBox {
public:
    static NonnullRefPtr<PaintableWithLines> create(Layout::BlockContainer const& block_container)
    {
        return adopt_ref(*new PaintableWithLines(block_container));
    }
    virtual ~PaintableWithLines() override;

    Layout::BlockContainer const& layout_box() const;
    Layout::BlockContainer& layout_box();

    Vector<Layout::LineBox> const& line_boxes() const { return m_line_boxes; }
    void set_line_boxes(Vector<Layout::LineBox>&& line_boxes) { m_line_boxes = move(line_boxes); }

    template<typename Callback>
    void for_each_fragment(Callback callback) const
    {
        for (auto& line_box : line_boxes()) {
            for (auto& fragment : line_box.fragments()) {
                if (callback(fragment) == IterationDecision::Break)
                    return;
            }
        }
    }

    virtual void paint(PaintContext&, PaintPhase) const override;
    virtual bool wants_mouse_events() const override { return false; }
    virtual bool handle_mousewheel(Badge<EventHandler>, Gfx::IntPoint const&, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y) override;

    virtual Optional<HitTestResult> hit_test(Gfx::FloatPoint const&, HitTestType) const override;

protected:
    PaintableWithLines(Layout::BlockContainer const&);

private:
    Vector<Layout::LineBox> m_line_boxes;
};

}
