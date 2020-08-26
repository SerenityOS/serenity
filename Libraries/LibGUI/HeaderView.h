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
#include <LibGfx/Orientation.h>

namespace GUI {

class HeaderView final : public Widget {
    C_OBJECT(HeaderView);

public:
    virtual ~HeaderView() override;

    Gfx::Orientation orientation() const { return m_orientation; }

    Model* model();
    const Model* model() const;

    void set_section_size(int section, int size);
    int section_size(int section) const;

    Gfx::TextAlignment section_alignment(int section) const;
    void set_section_alignment(int section, Gfx::TextAlignment);

    bool is_section_visible(int section) const;
    void set_section_visible(int section, bool);

    int section_count() const;
    Gfx::IntRect section_rect(int section) const;

private:
    HeaderView(AbstractTableView&, Gfx::Orientation);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void leave_event(Core::Event&) override;

    int horizontal_padding() const { return 5; }

    Gfx::IntRect section_resize_grabbable_rect(int) const;

    void paint_horizontal(Painter&);
    void paint_vertical(Painter&);

    Menu& ensure_context_menu();
    RefPtr<Menu> m_context_menu;

    AbstractTableView& m_table_view;

    Gfx::Orientation m_orientation { Gfx::Orientation::Horizontal };

    struct SectionData {
        int size { 0 };
        bool has_initialized_size { false };
        bool visibility { true };
        RefPtr<Action> visibility_action;
        Gfx::TextAlignment alignment { Gfx::TextAlignment::CenterLeft };
    };
    SectionData& section_data(int section) const;

    void set_hovered_section(int);

    mutable Vector<SectionData> m_section_data;

    bool m_in_section_resize { false };
    Gfx::IntPoint m_section_resize_origin;
    int m_section_resize_original_width { 0 };
    int m_resizing_section { -1 };
    int m_pressed_section { -1 };
    bool m_pressed_section_is_pressed { false };
    int m_hovered_section { -1 };
};

}
