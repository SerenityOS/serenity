/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Font/BitmapFont.h>

namespace FontEditor {

class GlyphEditorWidget final : public GUI::Frame {
    C_OBJECT(GlyphEditorWidget)
public:
    enum Mode {
        Paint,
        Move
    };

    virtual ~GlyphEditorWidget() override = default;

    int glyph() const { return m_glyph; }
    void set_glyph(int);
    bool is_glyph_empty();

    void rotate_90(Gfx::RotationDirection);
    void flip(Gfx::Orientation);

    int preferred_width() const;
    int preferred_height() const;

    void initialize(Gfx::BitmapFont*);

    int scale() const { return m_scale; }
    void set_scale(int scale);

    Mode mode() const { return m_mode; }
    void set_mode(Mode mode) { m_mode = mode; }

    Function<void(int)> on_glyph_altered;
    Function<void(StringView action_text)> on_undo_event;

private:
    GlyphEditorWidget() = default;
    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void enter_event(Core::Event&) override;

    void draw_at_mouse(GUI::MouseEvent const&);
    void move_at_mouse(GUI::MouseEvent const&);

    RefPtr<Gfx::BitmapFont> m_font;
    int m_glyph { 0 };
    int m_scale { 10 };
    int m_scaled_offset_x { 0 };
    int m_scaled_offset_y { 0 };
    u8 m_movable_bits[Gfx::GlyphBitmap::max_width() * 3][Gfx::GlyphBitmap::max_height() * 3] {};
    Mode m_mode { Paint };
    bool m_is_clicking_valid_cell { false };
    bool m_is_altering_glyph { false };
};

}
