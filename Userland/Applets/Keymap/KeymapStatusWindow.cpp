/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeymapStatusWindow.h"
#include <LibGUI/Painter.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <LibKeyboard/CharacterMap.h>

KeymapStatusWindow::KeymapStatusWindow()
{
    set_window_type(GUI::WindowType::Applet);
    set_has_alpha_channel(true);
    m_label = &set_main_widget<GUI::Label>();

    auto current_keymap = MUST(Keyboard::CharacterMap::fetch_system_map());
    auto current_keymap_name = current_keymap.character_map_name();
    m_label->set_tooltip(current_keymap_name);
    m_label->set_text(current_keymap_name.substring(0, 2));
}

KeymapStatusWindow::~KeymapStatusWindow()
{
}

void KeymapStatusWindow::wm_event(GUI::WMEvent& event)
{
    if (event.type() == GUI::WMEvent::WM_KeymapChanged) {
        auto& keymap_event = static_cast<GUI::WMKeymapChangedEvent&>(event);
        auto keymap = keymap_event.keymap();
        set_keymap_text(keymap);
    }
}

void KeymapStatusWindow::set_keymap_text(const String& keymap)
{
    GUI::Painter painter(*m_label);
    painter.clear_rect(m_label->rect(), Color::from_rgba(0));

    m_label->set_tooltip(keymap);
    m_label->set_text(keymap.substring(0, 2));
}
