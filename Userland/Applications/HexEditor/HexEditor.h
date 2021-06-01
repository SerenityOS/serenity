/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

    int buffer_size() const { return m_buffer.size(); }
    void set_buffer(const ByteBuffer&);
    void fill_selection(u8 fill_byte);
    bool write_to_file(const String& path);

    void select_all();
    bool has_selection() const { return !(m_selection_start == -1 || m_selection_end == -1 || (m_selection_end - m_selection_start) < 0 || m_buffer.is_empty()); }
    size_t selection_size();
    int selection_start_offset() const { return m_selection_start; }
    bool copy_selected_text_to_clipboard();
    bool copy_selected_hex_to_clipboard();
    bool copy_selected_hex_to_clipboard_as_c_code();

    int bytes_per_row() const { return m_bytes_per_row; }
    void set_bytes_per_row(int);

    void set_position(int position);
    void highlight(int start, int end);
    int find(ByteBuffer& needle, int start = 0);
    int find_and_highlight(ByteBuffer& needle, int start = 0);
    Vector<Match> find_all(ByteBuffer& needle, int start = 0);
    Vector<Match> find_all_strings(size_t min_length = 4);
    Function<void(int, EditMode, int, int)> on_status_change; // position, edit mode, selection start, selection end
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
    int m_line_spacing { 4 };
    int m_content_length { 0 };
    int m_bytes_per_row { 16 };
    ByteBuffer m_buffer;
    bool m_in_drag_select { false };
    int m_selection_start { 0 };
    int m_selection_end { 0 };
    HashMap<int, u8> m_tracked_changes;
    int m_position { 0 };
    int m_byte_position { 0 }; // 0 or 1
    EditMode m_edit_mode { Hex };

    void scroll_position_into_view(int position);

    int total_rows() const { return ceil_div(m_content_length, m_bytes_per_row); }
    int line_height() const { return font().glyph_height() + m_line_spacing; }
    int character_width() const { return font().glyph_width('W'); }
    int offset_margin_width() const { return 80; }

    void hex_mode_keydown_event(GUI::KeyEvent&);
    void text_mode_keydown_event(GUI::KeyEvent&);

    void set_content_length(int); // I might make this public if I add fetching data on demand.
    void update_status();
    void did_change();
};
