/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Layout/Node.h>

namespace Web::Layout {

class StackingContext {
public:
    StackingContext(Box&, StackingContext* parent);

    StackingContext* parent() { return m_parent; }
    const StackingContext* parent() const { return m_parent; }

    enum class StackingContextPaintPhase {
        BackgroundAndBorders,
        Floats,
        Foreground,
        FocusAndOverlay,
    };

    void paint_descendants(PaintContext&, Node&, StackingContextPaintPhase);
    void paint(PaintContext&);
    HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const;

    void dump(int indent = 0) const;

private:
    Box& m_box;
    StackingContext* const m_parent { nullptr };
    Vector<StackingContext*> m_children;

    void paint_internal(PaintContext&);
};

}
