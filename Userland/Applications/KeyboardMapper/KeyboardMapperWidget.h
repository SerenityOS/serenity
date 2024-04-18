/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
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
    virtual ~KeyboardMapperWidget() override = default;

    void create_frame();
    ErrorOr<void> load_map_from_file(ByteString const&);
    ErrorOr<void> load_map_from_system();
    ErrorOr<void> save();
    ErrorOr<void> save_to_file(StringView);
    void show_error_to_user(Error);
    void set_automatic_modifier(bool checked);

    bool request_close();

protected:
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;

    void set_current_map(ByteString const);
    void update_window_title();

private:
    KeyboardMapperWidget();

    Vector<KeyButton*> m_keys;
    RefPtr<GUI::Widget> m_map_group;
    void add_map_radio_button(StringView const map_name, String button_text);
    u32* map_from_name(StringView const map_name);
    void update_modifier_radio_buttons(GUI::KeyEvent&);

    ByteString m_filename;
    Keyboard::CharacterMapData m_character_map;
    ByteString m_current_map_name;
    bool m_automatic_modifier { false };
};
