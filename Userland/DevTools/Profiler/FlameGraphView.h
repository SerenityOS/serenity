/*
 * Copyright (c) 2021, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Profile.h"
#include <AK/Function.h>
#include <AK/Optional.h>
#include <LibGUI/Model.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollableContainerWidget.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Color.h>

namespace Profiler {

class FlameGraphView final : public GUI::AbstractScrollableWidget
    , GUI::ModelClient {
    C_OBJECT(FlameGraphView);

public:
    virtual ~FlameGraphView() override = default;

    Function<void()> on_hover_change;

    GUI::ModelIndex hovered_index() const;

protected:
    virtual void model_did_update(unsigned flags) override;

    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;

    virtual void resize_event(GUI::ResizeEvent&) override;
    virtual void paint_event(GUI::PaintEvent&) override;

private:
    explicit FlameGraphView(GUI::Model&, int text_column, int width_column);

    struct StackBar {
        GUI::ModelIndex const index;
        Gfx::IntRect rect;
        bool selected;
    };

    String bar_label(StackBar const&) const;
    void layout_bars();
    void layout_children(GUI::ModelIndex& parent, int depth, int left, int right, Vector<GUI::ModelIndex>& selected);

    GUI::Model& m_model;
    int m_text_column { -1 };
    int m_width_column { -1 };
    Vector<Gfx::Color> m_colors;
    Vector<StackBar> m_bars;
    StackBar* m_hovered_bar {};
    Vector<GUI::ModelIndex> m_selected_indexes;
    Gfx::IntSize m_old_available_size {};
};

}
