/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/BorderPainting.h>

namespace Web::Layout {

struct LineBoxFragmentCoordinate {
    size_t line_box_index { 0 };
    size_t fragment_index { 0 };
};

class Box : public NodeWithStyleAndBoxModelMetrics {
public:
    OwnPtr<Painting::Box> m_paint_box;

    bool is_out_of_flow(FormattingContext const&) const;

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;
    virtual void set_needs_display() override;

    bool is_body() const;

    virtual void paint(PaintContext&, Painting::PaintPhase) override;
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

    virtual void before_children_paint(PaintContext&, Painting::PaintPhase) override;
    virtual void after_children_paint(PaintContext&, Painting::PaintPhase) override;

    virtual ~Box() override;

    virtual void did_set_rect() { }

protected:
    Box(DOM::Document&, DOM::Node*, NonnullRefPtr<CSS::StyleProperties>);
    Box(DOM::Document&, DOM::Node*, CSS::ComputedValues);

private:
    virtual bool is_box() const final { return true; }
};

template<>
inline bool Node::fast_is<Box>() const { return is_box(); }

}
