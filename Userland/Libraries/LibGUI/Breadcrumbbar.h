/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class Breadcrumbbar : public GUI::Widget {
    C_OBJECT(Breadcrumbbar);

public:
    virtual ~Breadcrumbbar() override = default;

    void clear_segments();
    void append_segment(ByteString text, Gfx::Bitmap const* icon = nullptr, ByteString data = {}, String tooltip = {});
    void remove_end_segments(size_t segment_index);
    void relayout();

    size_t segment_count() const { return m_segments.size(); }
    ByteString segment_data(size_t index) const { return m_segments[index].data; }
    Optional<size_t> find_segment_with_data(ByteString const& data);

    void set_selected_segment(Optional<size_t> index);
    Optional<size_t> selected_segment() const { return m_selected_segment; }

    bool has_parent_segment() const { return m_selected_segment.has_value() && m_selected_segment.value() > 0; }
    bool has_child_segment() const { return m_selected_segment.has_value() && m_selected_segment.value() < m_segments.size() - 1; }

    Function<void(Optional<size_t> index)> on_segment_change;
    Function<void(size_t index)> on_segment_click;
    Function<void(size_t index, DropEvent&)> on_segment_drop;
    Function<void(size_t index, DragEvent&)> on_segment_drag_enter;
    Function<void(unsigned modifiers)> on_doubleclick;

protected:
    virtual void did_change_font() override;
    Breadcrumbbar();

private:
    virtual void resize_event(ResizeEvent&) override;

    struct Segment {
        RefPtr<Gfx::Bitmap const> icon;
        ByteString text;
        ByteString data;
        int width { 0 };
        int shrunken_width { 0 };
        WeakPtr<GUI::Button> button;
    };

    Vector<Segment> m_segments;
    Optional<size_t> m_selected_segment;

    virtual void doubleclick_event(GUI::MouseEvent&) override;
};

}
