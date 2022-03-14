/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class StackingContext {
public:
    StackingContext(Layout::Box&, StackingContext* parent);

    StackingContext* parent() { return m_parent; }
    const StackingContext* parent() const { return m_parent; }

    enum class StackingContextPaintPhase {
        BackgroundAndBorders,
        Floats,
        BackgroundAndBordersForInlineLevelAndReplaced,
        Foreground,
        FocusAndOverlay,
    };

    void paint_descendants(PaintContext&, Layout::Node&, StackingContextPaintPhase) const;
    void paint(PaintContext&) const;
    HitTestResult hit_test(Gfx::IntPoint const&, HitTestType) const;

    void dump(int indent = 0) const;

    void sort();

private:
    Layout::Box& m_box;
    StackingContext* const m_parent { nullptr };
    Vector<StackingContext*> m_children;

    void paint_internal(PaintContext&) const;
};

}
