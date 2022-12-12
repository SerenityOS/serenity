/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/ListView.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextEditor.h>

class KeyboardSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(KeyboardSettingsWidget)
public:
    virtual ~KeyboardSettingsWidget() override;

    virtual void apply_settings() override;

    void window_activated(bool is_active_window);

private:
    KeyboardSettingsWidget();

    void set_keymaps(Vector<DeprecatedString> const& keymaps, DeprecatedString const& active_keymap);

    void write_caps_lock_to_ctrl_sys_variable(bool);
    ErrorOr<bool> read_caps_lock_to_ctrl_sys_variable();

    Vector<DeprecatedString> m_initial_keymap_list;

    DeprecatedString m_initial_active_keymap;

    RefPtr<GUI::ListView> m_selected_keymaps_listview;
    RefPtr<GUI::Label> m_active_keymap_label;
    RefPtr<GUI::CheckBox> m_num_lock_checkbox;
    RefPtr<GUI::CheckBox> m_caps_lock_checkbox;
    RefPtr<GUI::Button> m_activate_keymap_button;
    RefPtr<GUI::Button> m_add_keymap_button;
    RefPtr<GUI::Button> m_remove_keymap_button;
    RefPtr<GUI::TextEditor> m_test_typing_area;
    RefPtr<GUI::Button> m_clear_test_typing_area_button;

    Function<void()> m_activate_keymap_event;
};
