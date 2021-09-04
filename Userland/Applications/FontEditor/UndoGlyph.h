/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Command.h>
#include <LibGUI/UndoStack.h>
#include <LibGfx/BitmapFont.h>

class UndoGlyph : public RefCounted<UndoGlyph> {
public:
    explicit UndoGlyph(const size_t code_point, const Gfx::BitmapFont& font)
        : m_code_point(code_point)
        , m_font(font)
    {
    }
    RefPtr<UndoGlyph> save_state() const
    {
        auto state = adopt_ref(*new UndoGlyph(m_code_point, *m_font));
        auto glyph = font().glyph(m_code_point).glyph_bitmap();
        for (int x = 0; x < glyph.width(); x++)
            for (int y = 0; y < glyph.height(); y++)
                state->m_bits[x][y] = glyph.bit_at(x, y);
        return state;
    }
    void restore_state(UndoGlyph const& state) const
    {
        auto bitmap = font().glyph(state.m_code_point).glyph_bitmap();
        for (int x = 0; x < bitmap.width(); x++)
            for (int y = 0; y < bitmap.height(); y++)
                bitmap.set_bit_at(x, y, state.m_bits[x][y]);
    }
    void set_code_point(size_t point) { m_code_point = point; }
    void set_font(Gfx::BitmapFont& font) { m_font = font; }
    const Gfx::BitmapFont& font() const { return *m_font; }

private:
    size_t m_code_point;
    RefPtr<Gfx::BitmapFont> m_font;
    u8 m_bits[32][36] = {};
};

class GlyphUndoCommand : public GUI::Command {
public:
    GlyphUndoCommand(UndoGlyph& glyph)
        : m_undo_state(glyph.save_state())
        , m_undo_glyph(glyph)
    {
    }
    virtual void undo() override
    {
        if (!m_redo_state)
            m_redo_state = m_undo_state->save_state();
        m_undo_glyph.restore_state(*m_undo_state);
    }
    virtual void redo() override
    {
        m_undo_glyph.restore_state(*m_redo_state);
    }

private:
    RefPtr<UndoGlyph> m_undo_state;
    RefPtr<UndoGlyph> m_redo_state;
    UndoGlyph& m_undo_glyph;
};
