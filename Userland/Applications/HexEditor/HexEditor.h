/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SearchResultsModel.h"
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/StdLibExtras.h>
#include <LibCore/Timer.h>
#include <LibGUI/AbstractScrollableWidget.h>
#include <LibGfx/Font.h>
#include <LibGfx/TextAlignment.h>

class HexEditor : public GUI::AbstractScrollableWidget {
    C_OBJECT(HexEditor)
public:
    enum EditMode {
        Hex,
        Text
    };

    virtual ~HexEditor() override;

    bool is_readonly() const { return m_readonly; }
    void set_readonly(bool);

    size_t buffer_size() const { return m_buffer.size(); }
    void set_buffer(const ByteBuffer&);
    void fill_selection(u8 fill_byte);
    bool write_to_file(const String& path);
    bool write_to_file(int fd);

    void select_all();
    bool has_selection() const { return !((m_selection_end < m_selection_start) || m_buffer.is_empty()); }
    size_t selection_size();
    size_t selection_start_offset() const { return m_selection_start; }
    bool copy_selected_text_to_clipboard();
    bool copy_selected_hex_to_clipboard();
    bool copy_selected_hex_to_clipboard_as_c_code();

    size_t bytes_per_row() const { return m_bytes_per_row; }
    void set_bytes_per_row(size_t);

    void set_position(size_t position);
    void highlight(size_t start, size_t end);
    Optional<size_t> find(ByteBuffer& needle, size_t start = 0);
    Optional<size_t> find_and_highlight(ByteBuffer& needle, size_t start = 0);
    Vector<Match> find_all(ByteBuffer& needle, size_t start = 0);
    Vector<Match> find_all_strings(size_t min_length = 4);
    Function<void(size_t, EditMode, size_t, size_t)> on_status_change; // position, edit mode, selection start, selection end
    Function<void()> on_change;

protected:
    HexEditor();

    virtual void paint_event(GUI::PaintEvent&) override;
    virtual void mousedown_event(GUI::MouseEvent&) override;
    virtual void mouseup_event(GUI::MouseEvent&) override;
    virtual void mousemove_event(GUI::MouseEvent&) override;
    virtual void keydown_event(GUI::KeyEvent&) override;

private:
    bool m_readonly { false };
    size_t m_line_spacing { 4 };
    size_t m_content_length { 0 };
    size_t m_bytes_per_row { 16 };
    ByteBuffer m_buffer;
    bool m_in_drag_select { false };
    size_t m_selection_start { 0 };
    size_t m_selection_end { 0 };
    HashMap<size_t, u8> m_tracked_changes;
    size_t m_position { 0 };
    bool m_cursor_at_low_nibble { false };
    EditMode m_edit_mode { Hex };
    NonnullRefPtr<Core::Timer> m_blink_timer;
    bool m_cursor_blink_active { false };

    static constexpr int m_address_bar_width = 90;
    static constexpr int m_padding = 5;

    void scroll_position_into_view(size_t position);

    size_t total_rows() const { return ceil_div(m_content_length, m_bytes_per_row); }
    size_t line_height() const { return font().glyph_height() + m_line_spacing; }
    size_t character_width() const { return font().glyph_width('W'); }
    size_t cell_width() const { return character_width() * 3; }
    size_t offset_margin_width() const { return 80; }

    void hex_mode_keydown_event(GUI::KeyEvent&);
    void text_mode_keydown_event(GUI::KeyEvent&);

    void set_content_length(size_t); // I might make this public if I add fetching data on demand.
    void update_status();
    void did_change();

    void reset_cursor_blink_state();
};
