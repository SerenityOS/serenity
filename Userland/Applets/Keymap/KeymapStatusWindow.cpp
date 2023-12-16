/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeymapStatusWindow.h"
#include <LibKeyboard/CharacterMap.h>

KeymapStatusWindow::KeymapStatusWindow()
{
    set_window_type(GUI::WindowType::Applet);
    set_has_alpha_channel(true);
    m_status_widget = set_main_widget<KeymapStatusWidget>();

    auto current_keymap = MUST(Keyboard::CharacterMap::fetch_system_map());
    m_status_widget->set_current_keymap(current_keymap.character_map_name());
}

void KeymapStatusWindow::wm_event(GUI::WMEvent& event)
{
    if (event.type() == GUI::WMEvent::WM_KeymapChanged) {
        auto& keymap_event = static_cast<GUI::WMKeymapChangedEvent&>(event);
        auto keymap = keymap_event.keymap();
        set_keymap_text(keymap);
    }
}

void KeymapStatusWindow::set_keymap_text(ByteString const& keymap)
{
    m_status_widget->set_current_keymap(keymap);
}
