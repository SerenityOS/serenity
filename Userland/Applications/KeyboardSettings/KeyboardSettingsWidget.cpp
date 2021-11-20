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
#include <LibGUI/Label.h>
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
    m_current_applied_keymap = keymap_object.get("keymap").to_string();
    dbgln("KeyboardSettings thinks the current keymap is: {}", m_current_applied_keymap);

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
        if (m_character_map_files[i].equals_ignoring_case(m_current_applied_keymap))
            initial_keymap_index = i;
    }
    VERIFY(initial_keymap_index < m_character_map_files.size());

    auto& character_map_image_label = *find_descendant_of_type_named<GUI::Label>("character_map_image_label");
    character_map_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/app-keyboard-mapper.png").release_value_but_fixme_should_propagate_errors());

    m_character_map_file_combo = find_descendant_of_type_named<GUI::ComboBox>("character_map_file_combo");
    m_character_map_file_combo->set_only_allow_values_from_model(true);
    m_character_map_file_combo->set_model(*GUI::ItemListModel<String>::create(m_character_map_files));
    m_character_map_file_combo->set_selected_index(initial_keymap_index);
    // This is a bit of a hack. We set the keymap to the selected one, so that it applies in the testing box below.
    // But, we also keep track of the current "applied" keymap, and then revert to that when we exit.
    // Ideally, we'd only use the selected keymap for the testing box without making a global system change.
    m_character_map_file_combo->on_change = [this](auto& keymap, auto) {
        set_keymap(keymap);
    };

    m_test_typing_area = find_descendant_of_type_named<GUI::TextEditor>("test_typing_area");
    m_clear_test_typing_area_button = find_descendant_of_type_named<GUI::Button>("button_clear_test_typing_area");
    m_clear_test_typing_area_button->on_click = [this](auto) {
        m_test_typing_area->clear();
        m_test_typing_area->set_focus(true);
    };

    auto& num_lock_image_label = *find_descendant_of_type_named<GUI::Label>("num_lock_image_label");
    num_lock_image_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/app-calculator.png").release_value_but_fixme_should_propagate_errors());
    m_num_lock_checkbox = find_descendant_of_type_named<GUI::CheckBox>("num_lock_checkbox");
    m_num_lock_checkbox->set_checked(Config::read_bool("KeyboardSettings", "StartupEnable", "NumLock", true));
}

KeyboardSettingsWidget::~KeyboardSettingsWidget()
{
    set_keymap(m_current_applied_keymap);
}

void KeyboardSettingsWidget::apply_settings()
{
    String character_map_file = m_character_map_file_combo->text();
    if (character_map_file.is_empty()) {
        GUI::MessageBox::show(window(), "Please select character mapping file.", "Keyboard settings", GUI::MessageBox::Type::Error);
        return;
    }
    m_current_applied_keymap = character_map_file;
    set_keymap(character_map_file);
    Config::write_bool("KeyboardSettings", "StartupEnable", "NumLock", m_num_lock_checkbox->is_checked());
}

void KeyboardSettingsWidget::set_keymap(String const& keymap_filename)
{
    pid_t child_pid;
    const char* argv[] = { "/bin/keymap", keymap_filename.characters(), nullptr };
    if ((errno = posix_spawn(&child_pid, "/bin/keymap", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        exit(1);
    }
}
