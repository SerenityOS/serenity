/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021, Rasmus Nylander <RasmusNylander.SerenityOS@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeyboardMapperWidget.h"
#include "KeyPositions.h"
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RadioButton.h>
#include <LibKeyboard/CharacterMap.h>
#include <LibKeyboard/CharacterMapFile.h>

KeyboardMapperWidget::KeyboardMapperWidget()
{
    create_frame();
}

bool KeyboardMapperWidget::request_close()
{
    if (!window()->is_modified())
        return true;
    auto result = GUI::MessageBox::ask_about_unsaved_changes(window(), m_filename);
    if (result == GUI::MessageBox::ExecResult::Yes) {
        if (auto error_or = save(); error_or.is_error())
            show_error_to_user(error_or.release_error());

        if (!window()->is_modified())
            return true;
    }
    return result == GUI::MessageBox::ExecResult::No;
}

void KeyboardMapperWidget::create_frame()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>(4);

    auto& main_widget = add<GUI::Widget>();
    main_widget.set_relative_rect(0, 0, 200, 200);

    m_keys.resize(KEY_COUNT);

    for (unsigned i = 0; i < KEY_COUNT; i++) {
        Gfx::IntRect rect = { keys[i].x, keys[i].y, keys[i].width, keys[i].height };

        auto& tmp_button = main_widget.add<KeyButton>();
        tmp_button.set_relative_rect(rect);
        tmp_button.set_text(String::from_byte_string(keys[i].name).release_value_but_fixme_should_propagate_errors());
        tmp_button.set_enabled(keys[i].enabled);

        tmp_button.on_click = [this, &tmp_button]() {
            String value;
            if (GUI::InputBox::show(window(), value, "New Character:"sv, "Select Character"sv) == GUI::InputBox::ExecResult::OK) {
                int i = m_keys.find_first_index(&tmp_button).value_or(0);
                VERIFY(i > 0);

                auto index = keys[i].map_index;
                VERIFY(index > 0);

                tmp_button.set_text(value);
                u32* map = map_from_name(m_current_map_name);

                if (value.is_empty())
                    map[index] = '\0'; // Empty string
                else
                    map[index] = *value.code_points().begin();

                window()->set_modified(true);
            }
        };

        m_keys.insert(i, &tmp_button);
    }

    // Action Buttons
    auto& bottom_widget = add<GUI::Widget>();
    bottom_widget.set_layout<GUI::HorizontalBoxLayout>();
    bottom_widget.set_fixed_height(40);

    // Map Selection
    m_map_group = bottom_widget.add<GUI::Widget>();
    m_map_group->set_layout<GUI::HorizontalBoxLayout>();
    m_map_group->set_fixed_width(450);

    add_map_radio_button("map"sv, "Default"_string);
    add_map_radio_button("shift_map"sv, "Shift"_string);
    add_map_radio_button("altgr_map"sv, "AltGr"_string);
    add_map_radio_button("alt_map"sv, "Alt"_string);
    add_map_radio_button("shift_altgr_map"sv, "Shift+AltGr"_string);

    bottom_widget.add_spacer();
}

void KeyboardMapperWidget::add_map_radio_button(StringView const map_name, String button_text)
{
    auto& map_radio_button = m_map_group->add<GUI::RadioButton>(button_text);
    map_radio_button.set_name(map_name);
    map_radio_button.on_checked = [map_name, this](bool) {
        set_current_map(map_name);
    };
}

u32* KeyboardMapperWidget::map_from_name(StringView const map_name)
{
    u32* map;
    if (map_name == "map"sv) {
        map = m_character_map.map;
    } else if (map_name == "shift_map"sv) {
        map = m_character_map.shift_map;
    } else if (map_name == "alt_map"sv) {
        map = m_character_map.alt_map;
    } else if (map_name == "altgr_map"sv) {
        map = m_character_map.altgr_map;
    } else if (map_name == "shift_altgr_map"sv) {
        map = m_character_map.shift_altgr_map;
    } else {
        VERIFY_NOT_REACHED();
    }
    return map;
}

ErrorOr<void> KeyboardMapperWidget::load_map_from_file(ByteString const& filename)
{
    auto character_map = TRY(Keyboard::CharacterMapFile::load_from_file(filename));

    m_filename = filename;
    m_character_map = character_map;
    set_current_map("map");

    for (auto& widget : m_map_group->child_widgets()) {
        auto& radio_button = static_cast<GUI::RadioButton&>(widget);
        radio_button.set_checked(radio_button.name() == "map");
    }

    window()->set_modified(false);
    update_window_title();
    return {};
}

ErrorOr<void> KeyboardMapperWidget::load_map_from_system()
{
    auto character_map = TRY(Keyboard::CharacterMap::fetch_system_map());

    m_filename = ByteString::formatted("/res/keymaps/{}.json", character_map.character_map_name());
    m_character_map = character_map.character_map_data();
    set_current_map("map");

    for (auto& widget : m_map_group->child_widgets()) {
        auto& radio_button = static_cast<GUI::RadioButton&>(widget);
        radio_button.set_checked(radio_button.name() == "map");
    }

    update_window_title();
    return {};
}

ErrorOr<void> KeyboardMapperWidget::save()
{
    return save_to_file(m_filename);
}

ErrorOr<void> KeyboardMapperWidget::save_to_file(StringView filename)
{
    JsonObject map_json;

    auto add_array = [&](ByteString name, u32* values) {
        JsonArray items;
        for (int i = 0; i < 90; i++) {
            StringBuilder sb;
            if (values[i])
                sb.append_code_point(values[i]);

            JsonValue val(sb.to_byte_string());
            items.must_append(move(val));
        }
        map_json.set(name, move(items));
    };

    add_array("map", m_character_map.map);
    add_array("shift_map", m_character_map.shift_map);
    add_array("alt_map", m_character_map.alt_map);
    add_array("altgr_map", m_character_map.altgr_map);
    add_array("shift_altgr_map", m_character_map.shift_altgr_map);

    // Write to file.
    ByteString file_content = map_json.to_byte_string();
    auto file = TRY(Core::File::open(filename, Core::File::OpenMode::Write));
    TRY(file->write_until_depleted(file_content.bytes()));
    file->close();

    window()->set_modified(false);
    m_filename = filename;
    update_window_title();
    return {};
}

void KeyboardMapperWidget::keydown_event(GUI::KeyEvent& event)
{
    for (int i = 0; i < KEY_COUNT; i++) {
        if (keys[i].kernel_map_entry_index != event.map_entry_index())
            continue;
        auto& tmp_button = m_keys.at(i);
        tmp_button->set_pressed(true);
        tmp_button->update();
        break;
    }

    if (m_automatic_modifier && event.modifiers() > 0) {
        update_modifier_radio_buttons(event);
    }

    event.ignore();
}

void KeyboardMapperWidget::keyup_event(GUI::KeyEvent& event)
{
    for (int i = 0; i < KEY_COUNT; i++) {
        if (keys[i].kernel_map_entry_index != event.map_entry_index())
            continue;
        auto& tmp_button = m_keys.at(i);
        tmp_button->set_pressed(false);
        tmp_button->update();
        break;
    }

    if (m_automatic_modifier) {
        update_modifier_radio_buttons(event);
    }
}

void KeyboardMapperWidget::set_current_map(ByteString const current_map)
{
    m_current_map_name = current_map;
    u32* map = map_from_name(m_current_map_name);

    for (unsigned k = 0; k < KEY_COUNT; k++) {
        auto index = keys[k].map_index;
        if (index == 0)
            continue;

        StringBuilder sb;
        sb.append_code_point(map[index]);

        m_keys.at(k)->set_text(sb.to_string().release_value_but_fixme_should_propagate_errors());
    }

    this->update();
}

void KeyboardMapperWidget::update_window_title()
{
    StringBuilder sb;
    sb.append(m_filename);
    sb.append("[*] - Keyboard Mapper"sv);

    window()->set_title(sb.to_byte_string());
}

void KeyboardMapperWidget::show_error_to_user(Error error)
{
    GUI::MessageBox::show_error(window(), error.string_literal());
}

void KeyboardMapperWidget::set_automatic_modifier(bool checked)
{
    m_automatic_modifier = checked;
}

void KeyboardMapperWidget::update_modifier_radio_buttons(GUI::KeyEvent& event)
{
    GUI::RadioButton* radio_button;
    if (event.shift() && event.altgr()) {
        radio_button = m_map_group->find_child_of_type_named<GUI::RadioButton>("shift_altgr_map");
    } else if (event.altgr()) {
        radio_button = m_map_group->find_child_of_type_named<GUI::RadioButton>("altgr_map");
    } else if (event.alt()) {
        radio_button = m_map_group->find_child_of_type_named<GUI::RadioButton>("alt_map");
    } else if (event.shift()) {
        radio_button = m_map_group->find_child_of_type_named<GUI::RadioButton>("shift_map");
    } else {
        radio_button = m_map_group->find_child_of_type_named<GUI::RadioButton>("map");
    }

    if (radio_button)
        radio_button->set_checked(true);
}
