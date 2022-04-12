/*
 * Copyright (c) 2021, Timur Sultanov <SultanovTS@yandex.ru>
 * Copyright (c) 2022, Thitat Auareesuksakul <thitat@flux.ci>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeymapStatusWindow.h"
#include <LibCore/ConfigFile.h>
#include <LibCore/Process.h>
#include <LibGUI/ConnectionToWindowMangerServer.h>
#include <LibGUI/Painter.h>
#include <LibKeyboard/CharacterMap.h>
#include <spawn.h>

void KeymapStatusWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        if (parent())
            static_cast<KeymapStatusWindow*>(parent())->cycle_keymaps();
    }

    if (event.button() == GUI::MouseButton::Secondary)
        Core::Process::spawn("/bin/KeyboardSettings");
}

KeymapStatusWindow::KeymapStatusWindow()
{
    set_window_type(GUI::WindowType::Applet);
    set_has_alpha_channel(true);
    m_status_widget = &set_main_widget<KeymapStatusWidget>();

    m_file_watcher = MUST(Core::FileWatcher::create());
    m_file_watcher->on_change = [this](auto&) {
        refresh_keymaps();
    };

    MUST(m_file_watcher->add_watch(Keyboard::Keymap::config_file_path(), Core::FileWatcherEvent::Type::ContentModified));

    refresh_keymaps(false);
}

void KeymapStatusWindow::refresh_keymaps(bool repaint_background)
{
    m_keymaps.clear();
    m_keymaps = MUST(Keyboard::Keymap::read_all());

    auto current_keymap = MUST(Keyboard::CharacterMap::fetch_system_map());
    auto current_keymap_name = current_keymap.character_map_name();
    auto it = m_keymaps.find_if([&](auto const& value) {
        return value.name() == current_keymap_name;
    });

    if (!it.is_end())
        m_keymap_index = it.index();
    else
        m_keymap_index = -1;

    set_keymap_text(current_keymap_name, repaint_background);
}

void KeymapStatusWindow::cycle_keymaps()
{
    m_keymap_index = (m_keymap_index + 1) % m_keymaps.size();
    auto keymap_name = m_keymaps[m_keymap_index].name();
    set_keymap(keymap_name);
}

void KeymapStatusWindow::wm_event(GUI::WMEvent& event)
{
    if (event.type() == GUI::WMEvent::WM_KeymapChanged) {
        auto& keymap_event = static_cast<GUI::WMKeymapChangedEvent&>(event);
        auto keymap = keymap_event.keymap();
        set_keymap_text(keymap);
    }
}

void KeymapStatusWindow::set_keymap(String const& keymap)
{
    pid_t child_pid;
    char const* argv[] = { "/bin/keymap", "-m", keymap.characters(), nullptr };
    if ((errno = posix_spawn(&child_pid, "/bin/keymap", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        dbgln("Failed to call /bin/keymap, error: {} ({})", errno, strerror(errno));
    }

    set_keymap_text(keymap);
}

void KeymapStatusWindow::set_keymap_text(String const& keymap, bool repaint_background)
{
    if (repaint_background) {
        GUI::Painter painter(*m_status_widget);
        painter.clear_rect(m_status_widget->rect(), Color::from_argb(0));
    }

    m_status_widget->set_tooltip(keymap);
    m_status_widget->set_text(keymap.substring(0, 2));
}
