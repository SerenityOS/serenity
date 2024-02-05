/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/Vector.h>
#include <Kernel/API/KeyCode.h>
#include <LibVT/CharacterSet.h>
#include <LibVT/EscapeSequenceParser.h>
#include <LibVT/Position.h>

#ifndef KERNEL
#    include <AK/ByteString.h>
#    include <AK/HashTable.h>
#    include <LibVT/Attribute.h>
#    include <LibVT/Line.h>
#else
namespace Kernel {
class VirtualConsole;
}
#    include <LibVT/Attribute.h>
#endif

namespace VT {

enum class CursorShape {
    None,
    Block,
    Underline,
    Bar,
};

enum CursorKeysMode {
    Application,
    Cursor,
};

class TerminalClient {
public:
    virtual ~TerminalClient() = default;

    virtual void beep() = 0;
    virtual void set_window_title(StringView) = 0;
    virtual void set_window_progress(int value, int max) = 0;
    virtual void terminal_did_resize(u16 columns, u16 rows) = 0;
    virtual void terminal_history_changed(int delta) = 0;
    virtual void terminal_did_perform_possibly_partial_clear() = 0;
    virtual void emit(u8 const*, size_t) = 0;
    virtual void set_cursor_shape(CursorShape) = 0;
    virtual void set_cursor_blinking(bool) = 0;
};

class Terminal : public EscapeSequenceExecutor {
public:
#ifndef KERNEL
    explicit Terminal(TerminalClient&);
#else
    explicit Terminal(Kernel::VirtualConsole&);
#endif

    virtual ~Terminal()
    {
    }

    bool m_need_full_flush { false };

#ifndef KERNEL
    void invalidate_cursor();
#else
    virtual void invalidate_cursor() = 0;
#endif

    void on_input(u8);

    void set_cursor(unsigned row, unsigned column, bool skip_debug = false);

    void clear_including_history()
    {
        clear_history();
        clear();
    }

#ifndef KERNEL
    void mark_cursor();
    OrderedHashTable<Mark> const& marks() const { return m_valid_marks; }

    void clear();
    void clear_history();
    void clear_to_mark(Mark);
#else
    virtual void clear() = 0;
    virtual void clear_history() = 0;
#endif

#ifndef KERNEL
    void set_size(u16 columns, u16 rows);
#else
    virtual void set_size(u16 columns, u16 rows) = 0;
#endif

    u16 columns() const
    {
        return m_columns;
    }
    u16 rows() const { return m_rows; }

    u16 cursor_column() const { return m_current_state.cursor.column; }
    u16 cursor_row() const { return m_current_state.cursor.row; }

#ifndef KERNEL
    size_t line_count() const
    {
        if (m_use_alternate_screen_buffer)
            return m_alternate_screen_buffer.size();
        else
            return m_history.size() + m_normal_screen_buffer.size();
    }

    Line& line(size_t index)
    {
        if (m_use_alternate_screen_buffer) {
            return *m_alternate_screen_buffer[index];
        } else {
            if (index < m_history.size())
                return *m_history[(m_history_start + index) % m_history.size()];
            return *m_normal_screen_buffer[index - m_history.size()];
        }
    }
    Line const& line(size_t index) const
    {
        return const_cast<Terminal*>(this)->line(index);
    }

    Line& visible_line(size_t index)
    {
        return *active_buffer()[index];
    }

    Line const& visible_line(size_t index) const
    {
        return *active_buffer()[index];
    }

    size_t max_history_size() const { return m_max_history_lines; }
    void set_max_history_size(size_t value)
    {
        if (value == 0) {
            auto previous_size = m_history.size();
            m_max_history_lines = 0;
            m_history_start = 0;
            m_history.clear();
            m_client.terminal_history_changed(-previous_size);
            return;
        }

        if (m_max_history_lines > value) {
            Vector<NonnullOwnPtr<Line>> new_history;
            new_history.ensure_capacity(value);
            auto existing_line_count = min(m_history.size(), value);
            for (size_t i = m_history.size() - existing_line_count; i < m_history.size(); ++i) {
                auto j = (m_history_start + i) % m_history.size();
                new_history.unchecked_append(move(static_cast<Vector<NonnullOwnPtr<Line>>&>(m_history).at(j)));
            }
            m_history = move(new_history);
            m_history_start = 0;
            m_client.terminal_history_changed(value - existing_line_count);
        }
        m_max_history_lines = value;
    }
    size_t history_size() const { return m_use_alternate_screen_buffer ? 0 : m_history.size(); }
#endif

    void inject_string(StringView);
    void handle_key_press(KeyCode, u32, u8 flags);

#ifndef KERNEL
    Attribute attribute_at(Position const&) const;
#endif

    bool needs_bracketed_paste() const
    {
        return m_needs_bracketed_paste;
    }

    bool is_within_scroll_region(u16 line) const
    {
        return line >= m_scroll_region_top && line <= m_scroll_region_bottom;
    }

protected:
    // ^EscapeSequenceExecutor
    virtual void emit_code_point(u32) override;
    virtual void execute_control_code(u8) override;
    virtual void execute_escape_sequence(Intermediates intermediates, bool ignore, u8 last_byte) override;
    virtual void execute_csi_sequence(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte) override;
    virtual void execute_osc_sequence(OscParameters parameters, u8 last_byte) override;
    virtual void dcs_hook(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte) override;
    virtual void receive_dcs_char(u8 byte) override;
    virtual void execute_dcs_sequence() override;

    struct BufferState {
        Attribute attribute;
        CursorPosition cursor;
    };

    void carriage_return();
    inline void scroll_up(size_t count = 1);
    inline void scroll_down(size_t count = 1);
    void linefeed();
#ifndef KERNEL
    void scroll_up(u16 region_top, u16 region_bottom, size_t count);
    void scroll_down(u16 region_top, u16 region_bottom, size_t count);
    void scroll_left(u16 row, u16 column, size_t count);
    void scroll_right(u16 row, u16 column, size_t count);
    void put_character_at(unsigned row, unsigned column, u32 ch);
    void clear_in_line(u16 row, u16 first_column, u16 last_column);
#else
    virtual void scroll_up(u16 region_top, u16 region_bottom, size_t count) = 0;
    virtual void scroll_down(u16 region_top, u16 region_bottom, size_t count) = 0;
    virtual void scroll_left(u16 row, u16 column, size_t count) = 0;
    virtual void scroll_right(u16 row, u16 column, size_t count) = 0;
    virtual void put_character_at(unsigned row, unsigned column, u32 ch) = 0;
    virtual void clear_in_line(u16 row, u16 first_column, u16 last_column) = 0;
#endif

    void unimplemented_control_code(u8);
    void unimplemented_escape_sequence(Intermediates, u8 last_byte);
    void unimplemented_csi_sequence(Parameters, Intermediates, u8 last_byte);
    void unimplemented_osc_sequence(OscParameters, u8 last_byte);

    void emit_string(StringView);

    void alter_ansi_mode(bool should_set, Parameters);
    void alter_private_mode(bool should_set, Parameters);

    // CUU – Cursor Up
    void CUU(Parameters);

    // CUD – Cursor Down
    void CUD(Parameters);

    // CUF – Cursor Forward
    void CUF(Parameters);

    // CUB – Cursor Backward
    void CUB(Parameters);

    // CNL - Cursor Next Line
    void CNL(Parameters);

    // CPL - Cursor Previous Line
    void CPL(Parameters);

    // CUP - Cursor Position
    void CUP(Parameters);

    // ED - Erase in Display
    void ED(Parameters);

    // EL - Erase in Line
    void EL(Parameters);

    // SGR – Select Graphic Rendition
    void SGR(Parameters);

    // Save Current Cursor Position
    void SCOSC();

    // Restore Saved Cursor Position
    void SCORC();

    // Save Cursor (and other attributes)
    void DECSC();

    // Restore Cursor (and other attributes)
    void DECRC();

    // DECSTBM – Set Top and Bottom Margins ("Scrolling Region")
    void DECSTBM(Parameters);

    // RM – Reset Mode
    void RM(Parameters);

    // DECRST - DEC Private Mode Reset
    void DECRST(Parameters);

    // SM – Set Mode
    void SM(Parameters);

    // DECSET - Dec Private Mode Set
    void DECSET(Parameters);

    // DA - Device Attributes
    void DA(Parameters);

    // HVP – Horizontal and Vertical Position
    void HVP(Parameters);

    // NEL - Next Line
    void NEL();

    // IND - Index (move down)
    void IND();

    // RI - Reverse Index (move up)
    void RI();

    // DECBI - Back Index
    void DECBI();

    // DECFI - Forward Index
    void DECFI();

    // DSR - Device Status Reports
    void DSR(Parameters);

    // DECSCUSR - Set Cursor Style
    void DECSCUSR(Parameters);

    // ICH - Insert Character
    void ICH(Parameters);

    // SU - Scroll Up (called "Pan Down" in VT510)
    void SU(Parameters);

    // SD - Scroll Down (called "Pan Up" in VT510)
    void SD(Parameters);

    // IL - Insert Line
    void IL(Parameters);

    // DCH - Delete Character
    void DCH(Parameters);

    // DL - Delete Line
    void DL(Parameters);

    // CHA - Cursor Horizontal Absolute
    void CHA(Parameters);

    // REP - Repeat
    void REP(Parameters);

    // VPA - Line Position Absolute
    void VPA(Parameters);

    // VPR - Line Position Relative
    void VPR(Parameters);

    // HPA - Character Position Absolute
    void HPA(Parameters);

    // HPR - Character Position Relative
    void HPR(Parameters);

    // ECH - Erase Character
    void ECH(Parameters);

    // FIXME: Find the right names for these.
    void XTERM_WM(Parameters);

    // DECIC - Insert Column
    void DECIC(Parameters);

    // DECDC - Delete Column
    void DECDC(Parameters);

    // DECPNM - Set numeric keypad mode
    void DECKPNM();

    // DECPAM - Set application keypad mode
    void DECKPAM();

#ifndef KERNEL
    TerminalClient& m_client;
#else
    Kernel::VirtualConsole& m_client;
#endif

    EscapeSequenceParser m_parser;
#ifndef KERNEL
    size_t m_history_start = 0;
    Vector<NonnullOwnPtr<Line>> m_history;
    void add_line_to_history(NonnullOwnPtr<Line>&& line)
    {
        if (max_history_size() == 0)
            return;

        // If m_history can expand, add the new line to the end of the list.
        // If there is an overflow wrap, the end is at the index before the start.
        if (m_history.size() < max_history_size()) {
            if (m_history_start == 0)
                m_history.append(move(line));
            else
                m_history.insert(m_history_start - 1, move(line));

            return;
        }
        m_history[m_history_start] = move(line);
        m_history_start = (m_history_start + 1) % m_history.size();
    }

    Vector<NonnullOwnPtr<Line>>& active_buffer() { return m_use_alternate_screen_buffer ? m_alternate_screen_buffer : m_normal_screen_buffer; }
    Vector<NonnullOwnPtr<Line>> const& active_buffer() const { return m_use_alternate_screen_buffer ? m_alternate_screen_buffer : m_normal_screen_buffer; }
    Vector<NonnullOwnPtr<Line>> m_normal_screen_buffer;
    Vector<NonnullOwnPtr<Line>> m_alternate_screen_buffer;
#endif

    bool m_use_alternate_screen_buffer { false };

    size_t m_scroll_region_top { 0 };
    size_t m_scroll_region_bottom { 0 };

    u16 m_columns { 1 };
    u16 m_rows { 1 };

    BufferState m_current_state;
    BufferState m_normal_saved_state;
    BufferState m_alternate_saved_state;

    // Separate from *_saved_state: some escape sequences only save/restore the cursor position,
    // while others impact the text attributes and other state too.
    CursorPosition m_saved_cursor_position;

    bool m_swallow_current { false };
    bool m_stomp { false };
    bool m_in_application_keypad_mode { false };

    CursorShape m_cursor_shape { VT::CursorShape::Block };
    CursorShape m_saved_cursor_shape { VT::CursorShape::Block };
    bool m_cursor_is_blinking_set { true };

    bool m_needs_bracketed_paste { false };

    Attribute m_current_attribute;
    Attribute m_saved_attribute;

#ifndef KERNEL
    ByteString m_current_window_title;
    Vector<ByteString> m_title_stack;
    OrderedHashTable<Mark> m_valid_marks;
#endif

#ifndef KERNEL
    u32 m_next_href_id { 0 };
#endif

    Vector<bool> m_horizontal_tabs;
    u32 m_last_code_point { 0 };
    size_t m_max_history_lines { 1024 };

    Optional<u16> m_column_before_carriage_return;
    bool m_controls_are_logically_generated { false };
    CursorKeysMode m_cursor_keys_mode { Cursor };

    CharacterSetTranslator m_character_set_translator {};
    size_t m_active_working_set_index { 0 };
    CharacterSet m_working_sets[4] { Iso_8859_1 };
};

}
