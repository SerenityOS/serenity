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
    void load_from_file(const String);
    void load_from_system();
    void save();
    void save_to_file(StringView);

protected:
    virtual void keydown_event(GUI::KeyEvent&) override;
    virtual void keyup_event(GUI::KeyEvent&) override;

    void set_current_map(const String);
    void update_window_title();

private:
    KeyboardMapperWidget();

    Vector<KeyButton*> m_keys;
    RefPtr<GUI::Widget> m_map_group;

    String m_filename;
    Keyboard::CharacterMapData m_character_map;
    String m_current_map_name;
    bool m_modified { false };
};
