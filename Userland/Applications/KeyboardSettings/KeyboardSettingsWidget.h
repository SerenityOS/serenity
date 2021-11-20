/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/SettingsWindow.h>

class KeyboardSettingsWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT(KeyboardSettingsWidget)
public:
    virtual ~KeyboardSettingsWidget() override = default;

    virtual void apply_settings() override;

private:
    KeyboardSettingsWidget();

    Vector<String> m_character_map_files;

    RefPtr<GUI::ComboBox> m_character_map_file_combo;
    RefPtr<GUI::CheckBox> m_num_lock_checkbox;
};
