/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "KeyButton.h"
#include <LibGUI/Button.h>
#include <LibKeyboard/CharacterMapData.h>

class KeyboardMapperWidget final : public GUI::Widget {
    C_OBJECT(KeyboardMapperWidget)

public:
    virtual ~KeyboardMapperWidget() override;

    void create_frame();
    ErrorOr<void> load_map_from_file(const String&);
    ErrorOr<void> load_map_from_system();
    ErrorOr<void> save();
    ErrorOr<void> save_to_file(StringView);
    void show_error_to_user(Error);
    void set_automatic_modifier(bool checked);

protected:
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;

    void set_current_map(const String);
    void update_window_title();

private:
    KeyboardMapperWidget();

    Vector<KeyButton*> m_keys;
    RefPtr<GUI::Widget> m_map_group;
    void add_map_radio_button(const StringView map_name, const StringView button_text);
    u32* map_from_name(const StringView map_name);
    void update_modifier_radio_buttons(GUI::KeyEvent&);

    String m_filename;
    Keyboard::CharacterMapData m_character_map;
    String m_current_map_name;
    bool m_modified { false };
    bool m_automatic_modifier { false };
};
