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

#include <AK/Function.h>
#include <AK/StdLibExtras.h>
#include <LibGUI/Frame.h>

class GlyphMapWidget final : public GUI::Frame {
    C_OBJECT(GlyphMapWidget)
public:
    virtual ~GlyphMapWidget() override;

    u8 selected_glyph() const { return m_selected_glyph; }
    void set_selected_glyph(u8);

    int rows() const { return ceil_div(m_glyph_count, m_columns); }
    int columns() const { return m_columns; }

    int preferred_width() const;
    int preferred_height() const;

    Gfx::Font& font() { return *m_font; }
    const Gfx::Font& font() const { return *m_font; }

    void update_glyph(u8);

    Function<void(u8)> on_glyph_selected;

private:
    explicit GlyphMapWidget(Gfx::Font&);
    virtual bool accepts_focus() const override { return true; }
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

    Gfx::Rect get_outer_rect(u8 glyph) const;

    RefPtr<Gfx::Font> m_font;
    int m_glyph_count { 256 };
    int m_columns { 32 };
    int m_horizontal_spacing { 2 };
    int m_vertical_spacing { 2 };
    u8 m_selected_glyph { 0 };
};
