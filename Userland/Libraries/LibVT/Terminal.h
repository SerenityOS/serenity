/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/API/KeyCode.h>
#include <LibVT/EscapeSequenceParser.h>
#include <LibVT/Line.h>
#include <LibVT/Position.h>

namespace VT {

class TerminalClient {
public:
    virtual ~TerminalClient() { }

    virtual void beep() = 0;
    virtual void set_window_title(const StringView&) = 0;
    virtual void set_window_progress(int value, int max) = 0;
    virtual void terminal_did_resize(u16 columns, u16 rows) = 0;
    virtual void terminal_history_changed() = 0;
    virtual void emit(const u8*, size_t) = 0;
};

class Terminal : public EscapeSequenceExecutor {
public:
    explicit Terminal(TerminalClient&);
    ~Terminal();

    bool m_need_full_flush { false };

    void invalidate_cursor();
    void on_input(u8);

    void clear();
    void clear_including_history();

    void set_size(u16 columns, u16 rows);
    u16 columns() const { return m_columns; }
    u16 rows() const { return m_rows; }

    u16 cursor_column() const { return m_cursor_column; }
    u16 cursor_row() const { return m_cursor_row; }

    size_t line_count() const
    {
        return m_history.size() + m_lines.size();
    }

    Line& line(size_t index)
    {
        if (index < m_history.size())
            return m_history[(m_history_start + index) % m_history.size()];
        return m_lines[index - m_history.size()];
    }
    const Line& line(size_t index) const
    {
        return const_cast<Terminal*>(this)->line(index);
    }

    Line& visible_line(size_t index)
    {
        return m_lines[index];
    }
    const Line& visible_line(size_t index) const
    {
        return m_lines[index];
    }

    size_t max_history_size() const { return m_max_history_lines; }
    void set_max_history_size(size_t value)
    {
        if (value == 0) {
            m_max_history_lines = 0;
            m_history_start = 0;
            m_history.clear();
            m_client.terminal_history_changed();
            return;
        }

        if (m_max_history_lines > value) {
            NonnullOwnPtrVector<Line> new_history;
            new_history.ensure_capacity(value);
            auto existing_line_count = min(m_history.size(), value);
            for (size_t i = m_history.size() - existing_line_count; i < m_history.size(); ++i) {
                auto j = (m_history_start + i) % m_history.size();
                new_history.unchecked_append(move(static_cast<Vector<NonnullOwnPtr<Line>>&>(m_history).at(j)));
            }
            m_history = move(new_history);
            m_history_start = 0;
            m_client.terminal_history_changed();
        }
        m_max_history_lines = value;
    }
    size_t history_size() const { return m_history.size(); }

    void inject_string(const StringView&);
    void handle_key_press(KeyCode, u32, u8 flags);

    Attribute attribute_at(const Position&) const;

private:
    // ^EscapeSequenceExecutor
    virtual void emit_code_point(u32) override;
    virtual void execute_control_code(u8) override;
    virtual void execute_escape_sequence(Intermediates intermediates, bool ignore, u8 last_byte) override;
    virtual void execute_csi_sequence(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte) override;
    virtual void execute_osc_sequence(OscParameters parameters, u8 last_byte) override;
    virtual void dcs_hook(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte) override;
    virtual void receive_dcs_char(u8 byte) override;
    virtual void execute_dcs_sequence() override;

    void scroll_up();
    void scroll_down();
    void newline();
    void carriage_return();

    void set_cursor(unsigned row, unsigned column);
    void put_character_at(unsigned row, unsigned column, u32 ch);
    void set_window_title(const String&);

    void unimplemented_control_code(u8);
    void unimplemented_escape_sequence(Intermediates, u8 last_byte);
    void unimplemented_csi_sequence(Parameters, Intermediates, u8 last_byte);
    void unimplemented_osc_sequence(OscParameters, u8 last_byte);

    void emit_string(const StringView&);

    void alter_mode(bool should_set, bool question_param, Parameters);

    // CUU – Cursor Up
    void CUU(Parameters);

    // CUD – Cursor Down
    void CUD(Parameters);

    // CUF – Cursor Forward
    void CUF(Parameters);

    // CUB – Cursor Backward
    void CUB(Parameters);

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
    void SCORC(Parameters);

    // DECSTBM – Set Top and Bottom Margins ("Scrolling Region")
    void DECSTBM(Parameters);

    // RM – Reset Mode
    void RM(Parameters);

    // SM – Set Mode
    void SM(Parameters);

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

    // DSR - Device Status Reports
    void DSR(Parameters);

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

    // VPA - Vertical Line Position Absolute
    void VPA(Parameters);

    // ECH - Erase Character
    void ECH(Parameters);

    // FIXME: Find the right names for these.
    void XTERM_WM(Parameters);

    TerminalClient& m_client;

    EscapeSequenceParser m_parser;

    size_t m_history_start = 0;
    NonnullOwnPtrVector<Line> m_history;
    void add_line_to_history(NonnullOwnPtr<Line>&& line)
    {
        if (max_history_size() == 0)
            return;

        if (m_history.size() < max_history_size()) {
            VERIFY(m_history_start == 0);
            m_history.append(move(line));
            return;
        }
        m_history.ptr_at(m_history_start) = move(line);
        m_history_start = (m_history_start + 1) % m_history.size();
    }

    NonnullOwnPtrVector<Line> m_lines;

    size_t m_scroll_region_top { 0 };
    size_t m_scroll_region_bottom { 0 };

    u16 m_columns { 1 };
    u16 m_rows { 1 };

    u16 m_cursor_row { 0 };
    u16 m_cursor_column { 0 };
    u16 m_saved_cursor_row { 0 };
    u16 m_saved_cursor_column { 0 };
    bool m_swallow_current { false };
    bool m_stomp { false };

    Attribute m_current_attribute;
    Attribute m_saved_attribute;

    u32 m_next_href_id { 0 };

    Vector<bool> m_horizontal_tabs;
    u32 m_last_code_point { 0 };
    size_t m_max_history_lines { 1024 };
};

}
