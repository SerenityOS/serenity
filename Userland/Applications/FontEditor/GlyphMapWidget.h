/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGUI/TextRange.h>
#include <LibGfx/BitmapFont.h>

class GlyphMapWidget final : public GUI::AbstractScrollableWidget {
    C_OBJECT(GlyphMapWidget)
public:
    virtual ~GlyphMapWidget() override;

    void initialize(Gfx::BitmapFont&);

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

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }

    Gfx::BitmapFont& font() { return *m_font; }
    Gfx::BitmapFont const& font() const { return *m_font; }

    Function<void(int)> on_active_glyph_changed;

private:
    GlyphMapWidget();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    Gfx::IntRect get_outer_rect(int glyph) const;

    void cut_glyph(int glyph);
    void copy_glyph(int glyph);
    void paste_glyph(int glyph);
    void delete_glyph(int glyph);

    RefPtr<Gfx::BitmapFont> m_font;
    int m_glyph_count { 0x110000 };
    int m_columns { 0 };
    int m_rows { 0 };
    int m_horizontal_spacing { 2 };
    int m_vertical_spacing { 2 };
    Selection m_selection;
    int m_active_glyph { 0 };
    int m_visible_glyphs { 0 };
};
