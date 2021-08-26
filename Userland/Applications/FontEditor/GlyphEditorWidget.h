/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Frame.h>
#include <LibGfx/BitmapFont.h>

static constexpr int s_max_width = 32;
static constexpr int s_max_height = 36;

class GlyphEditorWidget final : public GUI::Frame {
    C_OBJECT(GlyphEditorWidget)
public:
    enum Mode {
        Paint,
        Move
    };

    virtual ~GlyphEditorWidget() override;

    void initialize(Gfx::BitmapFont&);

    int glyph() const { return m_glyph; }
    void set_glyph(int);

    void cut_glyph();
    void copy_glyph();
    void paste_glyph();
    void delete_glyph();
    bool is_glyph_empty();

    int preferred_width() const;
    int preferred_height() const;

    Gfx::BitmapFont& font() { return *m_font; }
    const Gfx::BitmapFont& font() const { return *m_font; }

    int scale() const { return m_scale; }
    void set_scale(int scale);

    Mode mode() const { return m_mode; }
    void set_mode(Mode mode) { m_mode = mode; }

    Function<void(int)> on_glyph_altered;
    Function<void()> on_undo_event;

private:
    GlyphEditorWidget() {};
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void enter_event(Core::Event&) override;

    void draw_at_mouse(const GUI::MouseEvent&);
    void move_at_mouse(const GUI::MouseEvent&);

    RefPtr<Gfx::BitmapFont> m_font;
    int m_glyph { 0 };
    int m_scale { 10 };
    u8 m_movable_bits[s_max_width * 3][s_max_height * 3] = {};
    Mode m_mode { Paint };
    bool m_is_clicking_valid_cell { false };
};
