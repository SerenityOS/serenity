/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeymapStatusWidget.h"
#include "LibGUI/ActionGroup.h"
#include <LibCore/Process.h>
#include <LibGUI/Action.h>
#include <LibGUI/ConnectionToWindowMangerServer.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Point.h>
#include <LibKeyboard/CharacterMap.h>

void KeymapStatusWidget::mousedown_event(GUI::MouseEvent& event)
{
    Gfx::IntPoint point(event.x(), event.y());
    MUST(refresh_menu());
    m_context_menu->popup(point.translated(this->screen_relative_rect().location()));
}

ErrorOr<void> KeymapStatusWidget::refresh_menu()
{
    m_keymaps_group.for_each_action([&](auto& action) {
        m_keymaps_group.remove_action(action);
        return IterationDecision::Continue;
    });

    m_context_menu = GUI::Menu::construct();

    auto mapper_config = TRY(Core::ConfigFile::open("/etc/Keyboard.ini"));
    auto keymaps_string = mapper_config->read_entry("Mapping", "Keymaps", "");
    auto keymaps = keymaps_string.split(',');

    for (auto& keymap : keymaps) {
        auto action = GUI::Action::create_checkable(keymap, [=](auto&) {
            GUI::ConnectionToWindowMangerServer::the().async_set_keymap(keymap);
        });

        action->set_checked(keymap == m_current_keymap);

        m_keymaps_group.add_action(action);
        m_context_menu->add_action(action);
    }

    m_keymaps_group.set_exclusive(true);

    m_context_menu->add_separator();

    auto settings_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/settings.png"));

    m_context_menu->add_action(GUI::Action::create("&Settings",
        settings_icon,
        [](auto&) {
            Core::Process::spawn("/bin/KeyboardSettings");
        }));

    return {};
}

void KeymapStatusWidget::set_current_keymap(String const& keymap, ClearBackground clear_background)
{
    if (clear_background == ClearBackground::Yes) {
        GUI::Painter painter(*this);
        painter.clear_rect(rect(), Color::Transparent);
    }

    m_current_keymap = keymap;

    set_tooltip(keymap);
    set_text(keymap.substring(0, 2));
}
