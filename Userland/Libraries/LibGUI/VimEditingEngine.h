/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <LibGUI/EditingEngine.h>

namespace GUI {

class VimEditingEngine final : public EditingEngine {

public:
    virtual CursorWidth cursor_width() const override;

    virtual bool on_key(const KeyEvent& event) override;

private:
    enum VimMode {
        Normal,
        Insert,
        Visual
    };

    enum YankType {
        Line,
        Selection
    };

    VimMode m_vim_mode { VimMode::Normal };

    YankType m_yank_type {};
    String m_yank_buffer {};
    void yank(YankType);
    void yank(TextRange);
    void put(const GUI::KeyEvent&);

    TextPosition m_selection_start_position {};
    void update_selection_on_cursor_move();
    void clear_visual_mode_data();

    KeyCode m_previous_key {};
    void switch_to_normal_mode();
    void switch_to_insert_mode();
    void switch_to_visual_mode();
    void move_half_page_up(const KeyEvent& event);
    void move_half_page_down(const KeyEvent& event);

    bool on_key_in_insert_mode(const KeyEvent& event);
    bool on_key_in_normal_mode(const KeyEvent& event);
    bool on_key_in_visual_mode(const KeyEvent& event);
};

}
