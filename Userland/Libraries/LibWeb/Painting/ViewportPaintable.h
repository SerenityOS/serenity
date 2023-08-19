/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class ViewportPaintable final : public PaintableWithLines {
    JS_CELL(ViewportPaintable, PaintableWithLines);

public:
    static JS::NonnullGCPtr<ViewportPaintable> create(Layout::Viewport const&);
    virtual ~ViewportPaintable() override;

    void paint_all_phases(PaintContext&);
    void build_stacking_context_tree_if_needed();

private:
    void build_stacking_context_tree();

    explicit ViewportPaintable(Layout::Viewport const&);
};

}
