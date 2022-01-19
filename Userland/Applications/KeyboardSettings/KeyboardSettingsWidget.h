/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ListView.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/WindowManagerServerConnection.h>

class KeyboardSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(KeyboardSettingsWidget)
public:
    virtual ~KeyboardSettingsWidget() override;

    virtual void apply_settings() override;

    void window_activated(bool is_active_window);

private:
    KeyboardSettingsWidget();

    void set_keymaps(Vector<String> const& keymaps);

    Vector<String> m_initial_keymap_list;

    String m_current_applied_keymap;

    RefPtr<GUI::ListView> m_selected_keymaps_listview;
    RefPtr<GUI::CheckBox> m_num_lock_checkbox;
    RefPtr<GUI::Button> m_add_keymap_button;
    RefPtr<GUI::Button> m_remove_keymap_button;
    RefPtr<GUI::TextEditor> m_test_typing_area;
    RefPtr<GUI::Button> m_clear_test_typing_area_button;
};
