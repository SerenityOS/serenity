/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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
#include <string.h>

KeyboardMapperWidget::KeyboardMapperWidget()
{
    create_frame();
}

KeyboardMapperWidget::~KeyboardMapperWidget()
{
}

void KeyboardMapperWidget::create_frame()
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins(4);

    auto& main_widget = add<GUI::Widget>();
    main_widget.set_relative_rect(0, 0, 200, 200);

    m_keys.resize(KEY_COUNT);

    for (unsigned i = 0; i < KEY_COUNT; i++) {
        Gfx::IntRect rect = { keys[i].x, keys[i].y, keys[i].width, keys[i].height };

        auto& tmp_button = main_widget.add<KeyButton>();
        tmp_button.set_relative_rect(rect);
        tmp_button.set_text(keys[i].name);
        tmp_button.set_enabled(keys[i].enabled);

        tmp_button.on_click = [this, &tmp_button]() {
            String value;
            if (GUI::InputBox::show(window(), value, "New Character:", "Select Character") == GUI::InputBox::ExecOK) {
                int i = m_keys.find_first_index(&tmp_button).value_or(0);
                VERIFY(i > 0);

                auto index = keys[i].map_index;
                VERIFY(index > 0);

                tmp_button.set_text(value);
                u32* map;

                if (m_current_map_name == "map") {
                    map = m_character_map.map;
                } else if (m_current_map_name == "shift_map") {
                    map = m_character_map.shift_map;
                } else if (m_current_map_name == "alt_map") {
                    map = m_character_map.alt_map;
                } else if (m_current_map_name == "altgr_map") {
                    map = m_character_map.altgr_map;
                } else if (m_current_map_name == "shift_altgr_map") {
                    map = m_character_map.shift_altgr_map;
                } else {
                    VERIFY_NOT_REACHED();
                }

                if (value.length() == 0)
                    map[index] = '\0'; // Empty string
                else
                    map[index] = value[0];

                m_modified = true;
                update_window_title();
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

    auto& radio_map = m_map_group->add<GUI::RadioButton>("Default");
    radio_map.set_name("map");
    radio_map.on_checked = [&](bool) {
        set_current_map("map");
    };
    auto& radio_shift = m_map_group->add<GUI::RadioButton>("Shift");
    radio_shift.set_name("shift_map");
    radio_shift.on_checked = [this](bool) {
        set_current_map("shift_map");
    };
    auto& radio_altgr = m_map_group->add<GUI::RadioButton>("AltGr");
    radio_altgr.set_name("altgr_map");
    radio_altgr.on_checked = [this](bool) {
        set_current_map("altgr_map");
    };
    auto& radio_alt = m_map_group->add<GUI::RadioButton>("Alt");
    radio_alt.set_name("alt_map");
    radio_alt.on_checked = [this](bool) {
        set_current_map("alt_map");
    };
    auto& radio_shift_altgr = m_map_group->add<GUI::RadioButton>("Shift+AltGr");
    radio_shift_altgr.set_name("shift_altgr_map");
    radio_shift_altgr.on_checked = [this](bool) {
        set_current_map("shift_altgr_map");
    };

    bottom_widget.layout()->add_spacer();
}

void KeyboardMapperWidget::load_from_file(String filename)
{
    auto result = Keyboard::CharacterMapFile::load_from_file(filename);
    if (!result.has_value()) {
        auto error_message = String::formatted("Failed to load character map from file {}", filename);
        GUI::MessageBox::show(window(), error_message, "Error", GUI::MessageBox::Type::Error);
        return;
    }

    m_filename = filename;
    m_character_map = result.value();
    set_current_map("map");

    for (auto& widget : m_map_group->child_widgets()) {
        auto& radio_button = static_cast<GUI::RadioButton&>(widget);
        radio_button.set_checked(radio_button.name() == "map");
    }

    update_window_title();
}

void KeyboardMapperWidget::load_from_system()
{
    auto result = Keyboard::CharacterMap::fetch_system_map();
    VERIFY(!result.is_error());

    m_filename = String::formatted("/res/keymaps/{}.json", result.value().character_map_name());
    m_character_map = result.value().character_map_data();
    set_current_map("map");

    for (auto& widget : m_map_group->child_widgets()) {
        auto& radio_button = static_cast<GUI::RadioButton&>(widget);
        radio_button.set_checked(radio_button.name() == "map");
    }

    update_window_title();
}

void KeyboardMapperWidget::save()
{
    save_to_file(m_filename);
}

void KeyboardMapperWidget::save_to_file(StringView filename)
{
    JsonObject map_json;

    auto add_array = [&](String name, u32* values) {
        JsonArray items;
        for (int i = 0; i < 90; i++) {
            StringBuilder sb;
            if (values[i])
                sb.append_code_point(values[i]);

            JsonValue val(sb.to_string());
            items.append(move(val));
        }
        map_json.set(name, move(items));
    };

    add_array("map", m_character_map.map);
    add_array("shift_map", m_character_map.shift_map);
    add_array("alt_map", m_character_map.alt_map);
    add_array("altgr_map", m_character_map.altgr_map);
    add_array("shift_altgr_map", m_character_map.shift_altgr_map);

    // Write to file.
    String file_content = map_json.to_string();

    auto file = Core::File::construct(filename);
    file->open(Core::OpenMode::WriteOnly);
    if (!file->is_open()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(filename);
        sb.append(" for write. Error: ");
        sb.append(file->error_string());

        GUI::MessageBox::show(window(), sb.to_string(), "Error", GUI::MessageBox::Type::Error);
        return;
    }

    bool result = file->write(file_content);
    if (!result) {
        int error_number = errno;
        StringBuilder sb;
        sb.append("Unable to save file. Error: ");
        sb.append(strerror(error_number));

        GUI::MessageBox::show(window(), sb.to_string(), "Error", GUI::MessageBox::Type::Error);
        return;
    }

    m_modified = false;
    m_filename = filename;
    update_window_title();
}

void KeyboardMapperWidget::keydown_event(GUI::KeyEvent& event)
{
    for (int i = 0; i < KEY_COUNT; i++) {
        auto& tmp_button = m_keys.at(i);
        tmp_button->set_pressed(keys[i].scancode == event.scancode());
        tmp_button->update();
    }
}

void KeyboardMapperWidget::keyup_event(GUI::KeyEvent& event)
{
    for (int i = 0; i < KEY_COUNT; i++) {
        if (keys[i].scancode == event.scancode()) {
            auto& tmp_button = m_keys.at(i);
            tmp_button->set_pressed(false);
            tmp_button->update();
            break;
        }
    }
}

void KeyboardMapperWidget::set_current_map(const String current_map)
{
    m_current_map_name = current_map;
    u32* map;

    if (m_current_map_name == "map") {
        map = m_character_map.map;
    } else if (m_current_map_name == "shift_map") {
        map = m_character_map.shift_map;
    } else if (m_current_map_name == "alt_map") {
        map = m_character_map.alt_map;
    } else if (m_current_map_name == "altgr_map") {
        map = m_character_map.altgr_map;
    } else if (m_current_map_name == "shift_altgr_map") {
        map = m_character_map.shift_altgr_map;
    } else {
        VERIFY_NOT_REACHED();
    }

    for (unsigned k = 0; k < KEY_COUNT; k++) {
        auto index = keys[k].map_index;
        if (index == 0)
            continue;

        StringBuilder sb;
        sb.append_code_point(map[index]);

        m_keys.at(k)->set_text(sb.to_string());
    }

    this->update();
}

void KeyboardMapperWidget::update_window_title()
{
    StringBuilder sb;
    sb.append(m_filename);
    if (m_modified)
        sb.append(" (*)");
    sb.append(" - Keyboard Mapper");

    window()->set_title(sb.to_string());
}
