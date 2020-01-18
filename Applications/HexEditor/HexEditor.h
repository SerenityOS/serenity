/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/StdLibExtras.h>
#include <LibDraw/TextAlignment.h>
#include <LibGUI/GScrollableWidget.h>

class HexEditor : public GScrollableWidget {
    C_OBJECT(HexEditor)
public:
    enum EditMode {
        Hex,
        Text
    };

    virtual ~HexEditor() override;

    bool is_readonly() const { return m_readonly; }
    void set_readonly(bool);

    void set_buffer(const ByteBuffer&);
    void fill_selection(u8 fill_byte);
    bool write_to_file(const StringView& path);

    bool has_selection() const { return !(m_selection_start == -1 || m_selection_end == -1 || (m_selection_end - m_selection_start) < 0 || m_buffer.is_empty()); }
    bool copy_selected_text_to_clipboard();
    bool copy_selected_hex_to_clipboard();
    bool copy_selected_hex_to_clipboard_as_c_code();

    int bytes_per_row() const { return m_bytes_per_row; }
    void set_bytes_per_row(int);

    void set_position(int position);

    Function<void(int, EditMode, int, int)> on_status_change; // position, edit mode, selection start, selection end
    Function<void()> on_change;

protected:
    HexEditor(GWidget* parent);

    virtual void paint_event(GPaintEvent&) override;
    virtual void mousedown_event(GMouseEvent&) override;
    virtual void mouseup_event(GMouseEvent&) override;
    virtual void mousemove_event(GMouseEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual bool accepts_focus() const override { return true; }
    virtual void leave_event(CEvent&) override;

private:
    bool m_readonly { false };
    int m_line_spacing { 4 };
    int m_content_length { 0 };
    int m_bytes_per_row { 16 };
    ByteBuffer m_buffer;
    bool m_in_drag_select { false };
    int m_selection_start { -1 };
    int m_selection_end { -1 };
    int m_hover_pos { -1 };
    HashMap<int, u8> m_tracked_changes;
    int m_position { 0 };
    int m_byte_position { 0 }; // 0 or 1
    EditMode m_edit_mode { Hex };

    void scroll_position_into_view(int position);

    int total_rows() const { return ceil_div(m_content_length, m_bytes_per_row); }
    int line_height() const { return font().glyph_height() + m_line_spacing; }
    int character_width() const { return font().glyph_width('W'); }
    int offset_margin_width() const { return 80; }

    void hex_mode_keydown_event(GKeyEvent&);
    void text_mode_keydown_event(GKeyEvent&);

    void set_content_length(int); // I might make this public if I add fetching data on demand.
    void update_status();
    void did_change();
};