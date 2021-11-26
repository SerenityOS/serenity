/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TextEditor.h>

class KeyboardSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(KeyboardSettingsWidget)
public:
    virtual ~KeyboardSettingsWidget() override;

    virtual void apply_settings() override;

private:
    KeyboardSettingsWidget();

    void set_keymap(String const& keymap_filename);

    String m_current_applied_keymap;
    Vector<String> m_character_map_files;

    RefPtr<GUI::ComboBox> m_character_map_file_combo;
    RefPtr<GUI::TextEditor> m_test_typing_area;
    RefPtr<GUI::Button> m_clear_test_typing_area_button;
    RefPtr<GUI::CheckBox> m_num_lock_checkbox;
};
