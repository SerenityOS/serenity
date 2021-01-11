/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    void move_to_previous_empty_lines_block();
    void move_to_next_empty_lines_block();

    bool on_key_in_insert_mode(const KeyEvent& event);
    bool on_key_in_normal_mode(const KeyEvent& event);
    bool on_key_in_visual_mode(const KeyEvent& event);
};

}
