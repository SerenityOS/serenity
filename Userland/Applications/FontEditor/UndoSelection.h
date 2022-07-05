/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Command.h>
#include <LibGUI/UndoStack.h>
#include <LibGfx/Font/BitmapFont.h>

class UndoSelection : public RefCounted<UndoSelection> {
public:
    explicit UndoSelection(int const start, int const size, u32 const active_glyph, Gfx::BitmapFont const& font)
        : m_start(start)
        , m_size(size)
        , m_active_glyph(active_glyph)
        , m_font(font)
    {
    }
    ErrorOr<NonnullRefPtr<UndoSelection>> save_state()
    {
        auto state = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) UndoSelection(m_start, m_size, m_active_glyph, *m_font)));
        size_t bytes_per_glyph = Gfx::GlyphBitmap::bytes_per_row() * font().glyph_height();
        auto* rows = font().rows() + m_start * bytes_per_glyph;
        auto* widths = font().widths() + m_start;
        TRY(state->m_data.try_append(&rows[0], bytes_per_glyph * m_size));
        TRY(state->m_data.try_append(&widths[0], m_size));
        return state;
    }
    void restore_state(UndoSelection const& state)
    {
        size_t bytes_per_glyph = Gfx::GlyphBitmap::bytes_per_row() * font().glyph_height();
        auto* rows = font().rows() + state.m_start * bytes_per_glyph;
        auto* widths = font().widths() + state.m_start;
        memcpy(rows, &state.m_data[0], bytes_per_glyph * state.m_size);
        memcpy(widths, &state.m_data[bytes_per_glyph * state.m_size], state.m_size);
        m_restored_active_glyph = state.m_active_glyph;
        m_restored_start = state.m_start;
        m_restored_size = state.m_size;
    }
    void set_start(int start) { m_start = start; }
    void set_size(int size) { m_size = size; }
    void set_active_glyph(u32 code_point) { m_active_glyph = code_point; }
    Gfx::BitmapFont& font() { return *m_font; }
    u32 restored_active_glyph() const { return m_restored_active_glyph; }
    int restored_start() const { return m_restored_start; }
    int restored_size() const { return m_restored_size; }

private:
    int m_start { 0 };
    int m_size { 0 };
    u32 m_active_glyph { 0 };
    int m_restored_start { 0 };
    int m_restored_size { 0 };
    u32 m_restored_active_glyph { 0 };
    RefPtr<Gfx::BitmapFont> m_font;
    ByteBuffer m_data;
};

class SelectionUndoCommand : public GUI::Command {
public:
    SelectionUndoCommand(UndoSelection& selection, NonnullRefPtr<UndoSelection> undo_state)
        : m_undo_state(undo_state)
        , m_undo_selection(selection)
    {
    }
    virtual void undo() override
    {
        if (!m_redo_state) {
            if (auto maybe_state = m_undo_state->save_state(); !maybe_state.is_error()) {
                auto state = maybe_state.release_value();
                m_redo_state = move(state);
            } else {
                warnln("Failed to save redo state: {}", maybe_state.error());
            }
        }
        m_undo_selection.restore_state(*m_undo_state);
    }
    virtual void redo() override
    {
        if (m_redo_state)
            m_undo_selection.restore_state(*m_redo_state);
        else
            warnln("Failed to restore state");
    }

private:
    NonnullRefPtr<UndoSelection> m_undo_state;
    RefPtr<UndoSelection> m_redo_state;
    UndoSelection& m_undo_selection;
};
