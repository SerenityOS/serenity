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
    void append_segment(String text, Gfx::Bitmap const* icon = nullptr, String data = {}, String tooltip = {});
    void remove_end_segments(size_t segment_index);
    void relayout();

    size_t segment_count() const { return m_segments.size(); }
    String segment_data(size_t index) const { return m_segments[index].data; }
    Optional<size_t> find_segment_with_data(String const& data);

    void set_selected_segment(Optional<size_t> index);
    Optional<size_t> selected_segment() const { return m_selected_segment; }

    Function<void(size_t index)> on_segment_click;
    Function<void(size_t index, DropEvent&)> on_segment_drop;
    Function<void(size_t index, DragEvent&)> on_segment_drag_enter;
    Function<void(MouseEvent& event)> on_doubleclick;

private:
    Breadcrumbbar();

    virtual void resize_event(ResizeEvent&) override;

    struct Segment {
        RefPtr<const Gfx::Bitmap> icon;
        String text;
        String data;
        int width { 0 };
        int shrunken_width { 0 };
        WeakPtr<GUI::Button> button;
    };

    Vector<Segment> m_segments;
    Optional<size_t> m_selected_segment;

    virtual void doubleclick_event(GUI::MouseEvent&) override;
};

}
