/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/TextRange.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibUnicode/CharacterTypes.h>

namespace GUI {

class GlyphMapWidget final : public AbstractScrollableWidget {
    C_OBJECT(GlyphMapWidget)
public:
    virtual ~GlyphMapWidget() override = default;

    ErrorOr<void> initialize(Gfx::Font const*);

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

    void set_active_range(Unicode::CodePointRange);
    void set_active_glyph(int, ShouldResetSelection = ShouldResetSelection::Yes);

    void set_selection(int start, int size, Optional<u32> active_glyph = {});
    void restore_selection(int start, int size, int active_glyph);

    void scroll_to_glyph(int);
    void update_glyph(int);

    void set_highlight_modifications(bool);
    void set_show_system_emoji(bool);

    void set_glyph_modified(u32 glyph, bool modified);
    bool glyph_is_modified(u32 glyph);

    void select_previous_existing_glyph();
    void select_next_existing_glyph();

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }

    Function<void()> on_escape_pressed;
    Function<void(int)> on_active_glyph_changed;
    Function<void(int)> on_glyph_double_clicked;
    Function<void(ContextMenuEvent&)> on_context_menu_request;

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
    virtual void context_menu_event(ContextMenuEvent&) override;
    virtual void enter_event(Core::Event&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void automatic_scrolling_timer_did_fire() override;
    virtual Optional<UISize> calculated_min_size() const override;

    Gfx::IntRect get_outer_rect(int glyph) const;
    Optional<int> glyph_at_position(Gfx::IntPoint) const;
    int glyph_at_position_clamped(Gfx::IntPoint) const;

    void recalculate_content_size();

    RefPtr<Gfx::Font> m_original_font;
    int m_glyph_count { 0x110000 };
    int m_columns { 0 };
    int m_rows { 0 };
    int m_visible_rows { 0 };
    int m_horizontal_spacing { 4 };
    int m_vertical_spacing { 4 };
    Selection m_selection;
    int m_active_glyph { 0 };
    int m_tooltip_glyph { 0 };
    int m_visible_glyphs { 0 };
    bool m_in_drag_select { false };
    bool m_highlight_modifications { false };
    bool m_show_system_emoji { false };
    HashTable<u32> m_modified_glyphs;
    Unicode::CodePointRange m_active_range { 0x0000, 0x10FFFF };
    Gfx::IntPoint m_last_mousemove_position;
};

}
