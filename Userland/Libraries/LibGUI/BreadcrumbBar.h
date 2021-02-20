/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class BreadcrumbBar : public GUI::Widget {
    C_OBJECT(BreadcrumbBar);

public:
    virtual ~BreadcrumbBar() override;

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
    BreadcrumbBar();

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
