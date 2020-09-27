/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "KeyboardMapperWidget.h"
#include "KeyPositions.h"
#include <LibCore/File.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/RadioButton.h>
#include <LibKeyboard/CharacterMapFile.h>
#include <fcntl.h>
#include <stdio.h>
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
    set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fill);
    layout()->set_margins({ 4, 4, 4, 4 });

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
            if (GUI::InputBox::show(value, window(), "New Character:", "Select Character") == GUI::InputBox::ExecOK) {
                int i = m_keys.find_first_index(&tmp_button).value_or(0);
                ASSERT(i > 0);

                auto index = keys[i].map_index;
                ASSERT(index > 0);

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
                } else {
                    ASSERT_NOT_REACHED();
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
    bottom_widget.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    bottom_widget.set_preferred_size(0, 40);

    // Map Selection
    m_map_group = bottom_widget.add<GUI::Widget>();
    m_map_group->set_layout<GUI::HorizontalBoxLayout>();
    m_map_group->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_map_group->set_preferred_size(250, 0);

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

    bottom_widget.layout()->add_spacer();

    auto& ok_button = bottom_widget.add<GUI::Button>();
    ok_button.set_text("Save");
    ok_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    ok_button.set_preferred_size(80, 0);
    ok_button.on_click = [this](auto) {
        save();
    };
}

void KeyboardMapperWidget::load_from_file(String file_name)
{
    auto result = Keyboard::CharacterMapFile::load_from_file(file_name);
    if (!result.has_value()) {
        ASSERT_NOT_REACHED();
    }

    m_file_name = file_name;
    m_character_map = result.value();
    set_current_map("map");

    for (Widget* widget : m_map_group->child_widgets()) {
        auto radio_button = (GUI::RadioButton*)widget;
        radio_button->set_checked(radio_button->name() == "map");
    }

    update_window_title();
}

void KeyboardMapperWidget::save()
{
    save_to_file(m_file_name);
}

void KeyboardMapperWidget::save_to_file(const StringView& file_name)
{
    JsonObject map_json;

    auto add_array = [&](String name, u32* values) {
        JsonArray items;
        for (int i = 0; i < 90; i++) {
            AK::StringBuilder sb;
            sb.append(values[i]);

            JsonValue val(sb.to_string());
            items.append(move(val));
        }
        map_json.set(name, move(items));
    };

    add_array("map", m_character_map.map);
    add_array("shift_map", m_character_map.shift_map);
    add_array("alt_map", m_character_map.alt_map);
    add_array("altgr_map", m_character_map.altgr_map);

    // Write to file.
    String file_content = map_json.to_string();

    auto file = Core::File::construct(file_name);
    file->open(Core::IODevice::WriteOnly);
    if (!file->is_open()) {
        StringBuilder sb;
        sb.append("Failed to open ");
        sb.append(file_name);
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
    m_file_name = file_name;
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
    } else {
        ASSERT_NOT_REACHED();
    }

    for (unsigned k = 0; k < KEY_COUNT; k++) {
        auto index = keys[k].map_index;
        if (index == 0)
            continue;

        AK::StringBuilder sb;
        sb.append_code_point(map[index]);

        m_keys.at(k)->set_text(sb.to_string());
    }

    this->update();
}

void KeyboardMapperWidget::update_window_title()
{
    StringBuilder sb;
    sb.append(m_file_name);
    if (m_modified)
        sb.append(" (*)");
    sb.append(" - KeyboardMapper");

    window()->set_title(sb.to_string());
}
