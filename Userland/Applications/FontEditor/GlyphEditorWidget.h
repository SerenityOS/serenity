/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Frame.h>
#include <LibGfx/BitmapFont.h>

class GlyphEditorWidget final : public GUI::Frame {
    C_OBJECT(GlyphEditorWidget)
public:
    virtual ~GlyphEditorWidget() override;

    void initialize(Gfx::BitmapFont&);

    int glyph() const { return m_glyph; }
    void set_glyph(int);

    void cut_glyph();
    void copy_glyph();
    void paste_glyph();
    void delete_glyph();

    int preferred_width() const;
    int preferred_height() const;

    Gfx::BitmapFont& font() { return *m_font; }
    const Gfx::BitmapFont& font() const { return *m_font; }

    int scale() const { return m_scale; }
    void set_scale(int scale);

    Function<void(int)> on_glyph_altered;

private:
    GlyphEditorWidget() {};
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;

    void draw_at_mouse(const GUI::MouseEvent&);

    RefPtr<Gfx::BitmapFont> m_font;
    int m_glyph { 0 };
    int m_scale { 10 };
};
