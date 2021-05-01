/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ScrollableWidget.h>
#include <LibGfx/BitmapFont.h>

class GlyphMapWidget final : public GUI::ScrollableWidget {
    C_OBJECT(GlyphMapWidget)
public:
    virtual ~GlyphMapWidget() override;

    void initialize(Gfx::BitmapFont&);

    int selected_glyph() const { return m_selected_glyph; }
    void set_selected_glyph(int);
    void scroll_to_glyph(int glyph);

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }

    Gfx::BitmapFont& font() { return *m_font; }
    const Gfx::BitmapFont& font() const { return *m_font; }

    void update_glyph(int);
    void reprobe_font();

    Function<void(int)> on_glyph_selected;

private:
    GlyphMapWidget();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    Gfx::IntRect get_outer_rect(int glyph) const;

    RefPtr<Gfx::BitmapFont> m_font;
    int m_glyph_count { 384 };
    int m_columns { 32 };
    int m_rows { 12 };
    int m_horizontal_spacing { 2 };
    int m_vertical_spacing { 2 };
    int m_selected_glyph { 0 };
};
