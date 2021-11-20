/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyboardSettingsWidget.h"
#include <AK/JsonObject.h>
#include <AK/QuickSort.h>
#include <Applications/KeyboardSettings/KeyboardWidgetGML.h>
#include <LibConfig/Client.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibKeyboard/CharacterMap.h>
#include <spawn.h>

KeyboardSettingsWidget::KeyboardSettingsWidget()
{
    load_from_gml(keyboard_widget_gml);

    auto proc_keymap = Core::File::construct("/proc/keymap");
    if (!proc_keymap->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();

    auto json = JsonValue::from_string(proc_keymap->read_all()).release_value_but_fixme_should_propagate_errors();
    auto const& keymap_object = json.as_object();
    VERIFY(keymap_object.has("keymap"));
    String current_keymap = keymap_object.get("keymap").to_string();
    dbgln("KeyboardSettings thinks the current keymap is: {}", current_keymap);

    Core::DirIterator iterator("/res/keymaps/", Core::DirIterator::Flags::SkipDots);
    if (iterator.has_error()) {
        GUI::MessageBox::show(nullptr, String::formatted("Error on reading mapping file list: {}", iterator.error_string()), "Keyboard settings", GUI::MessageBox::Type::Error);
        GUI::Application::the()->quit(-1);
    }

    while (iterator.has_next()) {
        auto name = iterator.next_path();
        m_character_map_files.append(name.replace(".json", ""));
    }
    quick_sort(m_character_map_files);

    size_t initial_keymap_index = SIZE_MAX;
    for (size_t i = 0; i < m_character_map_files.size(); ++i) {
        if (m_character_map_files[i].equals_ignoring_case(current_keymap))
            initial_keymap_index = i;
    }
    VERIFY(initial_keymap_index < m_character_map_files.size());

    m_character_map_file_combo = find_descendant_of_type_named<GUI::ComboBox>("character_map_file_combo");
    m_character_map_file_combo->set_only_allow_values_from_model(true);
    m_character_map_file_combo->set_model(*GUI::ItemListModel<String>::create(m_character_map_files));
    m_character_map_file_combo->set_selected_index(initial_keymap_index);

    m_num_lock_checkbox = find_descendant_of_type_named<GUI::CheckBox>("num_lock_checkbox");
    m_num_lock_checkbox->set_checked(Config::read_bool("KeyboardSettings", "StartupEnable", "NumLock", true));
}

void KeyboardSettingsWidget::apply_settings()
{
    String character_map_file = m_character_map_file_combo->text();
    if (character_map_file.is_empty()) {
        GUI::MessageBox::show(window(), "Please select character mapping file.", "Keyboard settings", GUI::MessageBox::Type::Error);
        return;
    }
    pid_t child_pid;
    const char* argv[] = { "/bin/keymap", character_map_file.characters(), nullptr };
    if ((errno = posix_spawn(&child_pid, "/bin/keymap", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        exit(1);
    }

    Config::write_bool("KeyboardSettings", "StartupEnable", "NumLock", m_num_lock_checkbox->is_checked());
}
