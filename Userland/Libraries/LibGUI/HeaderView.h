/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    Model const* model() const;

    void set_section_size(int section, int size);
    int section_size(int section) const;

    void set_default_section_size(int section, int size);
    int default_section_size(int section) const;
    bool is_default_section_size_initialized(int section) const;

    Gfx::TextAlignment section_alignment(int section) const;
    void set_section_alignment(int section, Gfx::TextAlignment);

    bool is_section_visible(int section) const;
    void set_section_visible(int section, bool);

    void set_section_selectable(int section, bool);

    int section_count() const;
    Gfx::IntRect section_rect(int section) const;

    Function<void(int section)> on_resize_doubleclick;

    static constexpr auto const sorting_arrow_offset = 3;
    static constexpr auto const sorting_arrow_width = 6;

    static constexpr auto const ascending_arrow_coordinates = Array {
        Gfx::IntPoint { 4, 2 },
        Gfx::IntPoint { 1, 5 },
        Gfx::IntPoint { 7, 5 },
    };

    static constexpr auto const descending_arrow_coordinates = Array {
        Gfx::IntPoint { 1, 3 },
        Gfx::IntPoint { 7, 3 },
        Gfx::IntPoint { 4, 6 },
    };

private:
    HeaderView(AbstractTableView&, Gfx::Orientation);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void leave_event(Core::Event&) override;

    struct VisibleSectionRange {
        int start_offset { 0 };
        int start { 0 };
        int end { 0 };
    };
    VisibleSectionRange visible_section_range() const;

    Gfx::IntRect section_resize_grabbable_rect(int) const;

    void paint_horizontal(Painter&);
    void paint_vertical(Painter&);

    Menu& ensure_context_menu();
    RefPtr<Menu> m_context_menu;

    AbstractTableView& m_table_view;

    Gfx::Orientation m_orientation { Gfx::Orientation::Horizontal };

    struct SectionData {
        int size { 0 };
        int default_size { 0 };
        bool has_initialized_size { false };
        bool has_initialized_default_size { false };
        bool visibility { true };
        bool selectable { true };
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
