/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/ScrollableWidget.h>
#include <LibGfx/BitmapFont.h>

class GlyphMapWidget final : public GUI::ScrollableWidget {
    C_OBJECT(GlyphMapWidget)
public:
    virtual ~GlyphMapWidget() override;

    void initialize(Gfx::BitmapFont&);

    int selected_glyph() const { return m_selected_glyph; }
    void set_selected_glyph(int);

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }

    Gfx::BitmapFont& font() { return *m_font; }
    const Gfx::BitmapFont& font() const { return *m_font; }

    void update_glyph(int);

    Function<void(int)> on_glyph_selected;

private:
    GlyphMapWidget();
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void resize_event(GUI::ResizeEvent&) override;

    Gfx::IntRect get_outer_rect(int glyph) const;
    void scroll_to_glyph(int glyph);

    RefPtr<Gfx::BitmapFont> m_font;
    int m_glyph_count { 384 };
    int m_columns { 32 };
    int m_rows { 12 };
    int m_horizontal_spacing { 2 };
    int m_vertical_spacing { 2 };
    int m_selected_glyph { 0 };
};
