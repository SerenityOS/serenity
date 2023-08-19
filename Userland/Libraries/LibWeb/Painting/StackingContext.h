/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Matrix4x4.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class StackingContext {
public:
    StackingContext(PaintableBox&, StackingContext* parent, size_t index_in_tree_order);

    StackingContext* parent() { return m_parent; }
    StackingContext const* parent() const { return m_parent; }

    PaintableBox const& paintable_box() const { return *m_paintable_box; }

    enum class StackingContextPaintPhase {
        BackgroundAndBorders,
        Floats,
        BackgroundAndBordersForInlineLevelAndReplaced,
        Foreground,
        FocusAndOverlay,
    };

    void paint_descendants(PaintContext&, Paintable const&, StackingContextPaintPhase) const;
    void paint(PaintContext&) const;
    Optional<HitTestResult> hit_test(CSSPixelPoint, HitTestType) const;

    Gfx::FloatMatrix4x4 const& transform_matrix() const { return m_transform; }
    Gfx::AffineTransform affine_transform_matrix() const;

    void dump(int indent = 0) const;

    void sort();

private:
    JS::NonnullGCPtr<PaintableBox> m_paintable_box;
    Gfx::FloatMatrix4x4 m_transform;
    Gfx::FloatPoint m_transform_origin;
    StackingContext* const m_parent { nullptr };
    Vector<StackingContext*> m_children;
    size_t m_index_in_tree_order { 0 };

    void paint_child(PaintContext&, StackingContext const&) const;
    void paint_internal(PaintContext&) const;
    Gfx::FloatMatrix4x4 get_transformation_matrix(CSS::Transformation const& transformation) const;
    Gfx::FloatMatrix4x4 combine_transformations(Vector<CSS::Transformation> const& transformations) const;
    Gfx::FloatPoint transform_origin() const { return m_transform_origin; }
    Gfx::FloatPoint compute_transform_origin() const;
};

}
