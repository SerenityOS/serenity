/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Command.h>
#include <LibGUI/GlyphMapWidget.h>
#include <LibGUI/UndoStack.h>
#include <LibGfx/Font/BitmapFont.h>

class UndoSelection : public RefCounted<UndoSelection> {
public:
    explicit UndoSelection(int const start, int const size, u32 const active_glyph, Gfx::BitmapFont& font, NonnullRefPtr<GUI::GlyphMapWidget> glyph_map_widget)
        : m_start(start)
        , m_size(size)
        , m_active_glyph(active_glyph)
        , m_font(font)
        , m_glyph_map_widget(move(glyph_map_widget))
    {
    }
    ErrorOr<NonnullRefPtr<UndoSelection>> save_state()
    {
        auto state = TRY(try_make_ref_counted<UndoSelection>(m_start, m_size, m_active_glyph, *m_font, m_glyph_map_widget));
        size_t bytes_per_glyph = Gfx::GlyphBitmap::bytes_per_row() * font().glyph_height();
        auto rows = font().rows().slice(m_start * bytes_per_glyph, m_size * bytes_per_glyph);
        auto widths = font().widths().slice(m_start, m_size);
        TRY(state->m_data.try_append(&rows[0], bytes_per_glyph * m_size));
        TRY(state->m_data.try_append(widths));

        TRY(state->m_restored_modified_state.try_ensure_capacity(m_size));
        for (int glyph = m_start; glyph < m_start + m_size; ++glyph)
            TRY(state->m_restored_modified_state.try_append(m_glyph_map_widget->glyph_is_modified(glyph)));

        return state;
    }
    void restore_state(UndoSelection const& state)
    {
        size_t bytes_per_glyph = Gfx::GlyphBitmap::bytes_per_row() * font().glyph_height();
        auto rows = font().rows().slice(state.m_start * bytes_per_glyph, state.m_size * bytes_per_glyph);
        auto widths = font().widths().slice(state.m_start, state.m_size);
        memcpy(rows.data(), &state.m_data[0], bytes_per_glyph * state.m_size);
        memcpy(widths.data(), &state.m_data[bytes_per_glyph * state.m_size], state.m_size);

        for (int i = 0; i < state.m_size; ++i)
            m_glyph_map_widget->set_glyph_modified(state.m_start + i, state.m_restored_modified_state[i]);

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
    Vector<bool> m_restored_modified_state;
    RefPtr<Gfx::BitmapFont> m_font;
    NonnullRefPtr<GUI::GlyphMapWidget> m_glyph_map_widget;
    ByteBuffer m_data;
};

class SelectionUndoCommand : public GUI::Command {
public:
    SelectionUndoCommand(UndoSelection& selection, NonnullRefPtr<UndoSelection> undo_state, String action_text)
        : m_undo_state(undo_state)
        , m_undo_selection(selection)
        , m_action_text(move(action_text))
    {
    }
    virtual void undo() override
    {
        if (!m_redo_state) {
            if (auto maybe_state = m_undo_state->save_state(); !maybe_state.is_error())
                m_redo_state = move(maybe_state.value());
            else
                warnln("Saving redo state failed: {}", maybe_state.error());
        }
        m_undo_selection.restore_state(*m_undo_state);
    }
    virtual void redo() override
    {
        if (m_redo_state)
            m_undo_selection.restore_state(*m_redo_state);
        else
            warnln("Restoring state failed");
    }
    virtual ByteString action_text() const override { return m_action_text.to_byte_string(); }

private:
    NonnullRefPtr<UndoSelection> m_undo_state;
    RefPtr<UndoSelection> m_redo_state;
    UndoSelection& m_undo_selection;
    String m_action_text;
};
