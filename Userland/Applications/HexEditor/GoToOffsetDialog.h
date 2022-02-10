/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Result.h>
#include <AK/Vector.h>
#include <LibGUI/Dialog.h>

class GoToOffsetDialog : public GUI::Dialog {
    C_OBJECT(GoToOffsetDialog);

public:
    static int show(GUI::Window* parent_window, int& history_offset, int& out_offset, int selection_offset, int end);

private:
    GoToOffsetDialog();
    virtual ~GoToOffsetDialog() override = default;
    void update_statusbar();
    int process_input();
    int calculate_new_offset(int offset);
    int m_selection_offset { 0 };
    int m_buffer_size { 0 };
    Vector<String> m_offset_type;
    Vector<String> m_offset_from;

    RefPtr<GUI::TextEditor> m_text_editor;
    RefPtr<GUI::Button> m_go_button;
    RefPtr<GUI::ComboBox> m_offset_type_box;
    RefPtr<GUI::ComboBox> m_offset_from_box;
    RefPtr<GUI::Statusbar> m_statusbar;
};
