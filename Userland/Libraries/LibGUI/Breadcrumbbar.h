/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class Breadcrumbbar : public GUI::Widget {
    C_OBJECT(Breadcrumbbar);

public:
    virtual ~Breadcrumbbar() override;

    void clear_segments();
    void append_segment(String text, const Gfx::Bitmap* icon = nullptr, String data = {}, String tooltip = {});

    size_t segment_count() const { return m_segments.size(); }
    String segment_data(size_t index) const { return m_segments[index].data; }

    void set_selected_segment(Optional<size_t> index);
    Optional<size_t> selected_segment() const { return m_selected_segment; }

    Function<void(size_t index)> on_segment_click;
    Function<void(size_t index, DropEvent&)> on_segment_drop;
    Function<void(size_t index, DragEvent&)> on_segment_drag_enter;
    Function<void(MouseEvent& event)> on_doubleclick;

private:
    Breadcrumbbar();

    struct Segment {
        RefPtr<const Gfx::Bitmap> icon;
        String text;
        String data;
        WeakPtr<GUI::Button> button;
    };

    Vector<Segment> m_segments;
    Optional<size_t> m_selected_segment;

    virtual void doubleclick_event(GUI::MouseEvent&) override;
};

}
