/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/TextRange.h>
#include <LibGfx/BitmapFont.h>

namespace GUI {

class GlyphMapWidget final : public AbstractScrollableWidget {
    C_OBJECT(GlyphMapWidget)
public:
    virtual ~GlyphMapWidget() override;

    class Selection {
    public:
        Selection() = default;
        Selection(int start, int size)
            : m_start(start)
            , m_size(size)
        {
        }

        int size() const { return m_size; }
        void set_size(int i) { m_size = i; }
        int start() const { return m_start; }
        void set_start(int i) { m_start = i; }

        Selection normalized() const;
        bool contains(int) const;
        void resize_by(int i);
        void extend_to(int);

    private:
        int m_start { 0 };
        int m_size { 1 };
    };

    Selection selection() const { return m_selection; }
    int active_glyph() const { return m_active_glyph; }

    enum class ShouldResetSelection {
        Yes,
        No
    };

    void set_active_glyph(int, ShouldResetSelection = ShouldResetSelection::Yes);
    void clear_selection() { m_selection.set_size(0); }
    void scroll_to_glyph(int);
    void update_glyph(int);

    void select_previous_existing_glyph();
    void select_next_existing_glyph();

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }

    Function<void(int)> on_active_glyph_changed;
    Function<void(int)> on_glyph_double_clicked;

private:
    GlyphMapWidget();
    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void keydown_event(KeyEvent&) override;
    virtual void resize_event(ResizeEvent&) override;
    virtual void did_change_font() override;

    Gfx::IntRect get_outer_rect(int glyph) const;
    Optional<int> glyph_at_position(Gfx::IntPoint) const;

    void recalculate_content_size();

    int m_glyph_count { 0x110000 };
    int m_columns { 0 };
    int m_rows { 0 };
    int m_horizontal_spacing { 2 };
    int m_vertical_spacing { 2 };
    Selection m_selection;
    int m_active_glyph { 0 };
    int m_visible_glyphs { 0 };
    bool m_in_drag_select { false };
};

}
