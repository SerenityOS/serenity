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
    JS_DECLARE_ALLOCATOR(ViewportPaintable);

public:
    static JS::NonnullGCPtr<ViewportPaintable> create(Layout::Viewport const&);
    virtual ~ViewportPaintable() override;

    void paint_all_phases(PaintContext&);
    void build_stacking_context_tree_if_needed();

    HashMap<JS::GCPtr<PaintableBox const>, RefPtr<ScrollFrame>> scroll_state;
    void assign_scroll_frames();
    void refresh_scroll_state();

    HashMap<JS::GCPtr<PaintableBox const>, RefPtr<ClipFrame>> clip_state;
    void assign_clip_frames();
    void refresh_clip_state();

    void resolve_paint_only_properties();

    JS::GCPtr<Selection::Selection> selection() const;
    void recompute_selection_states();

private:
    void build_stacking_context_tree();

    explicit ViewportPaintable(Layout::Viewport const&);

    virtual void visit_edges(Visitor&) override;
};

}
