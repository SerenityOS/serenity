/*
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "KeymapStatusWidget.h"
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Process.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Point.h>

KeymapStatusWidget::KeymapStatusWidget() = default;
KeymapStatusWidget::~KeymapStatusWidget() = default;

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
            GUI::ConnectionToWindowManagerServer::the().async_set_keymap(keymap);
        });

        action->set_checked(keymap == m_current_keymap);

        m_keymaps_group.add_action(action);
        m_context_menu->add_action(action);
    }

    m_keymaps_group.set_exclusive(true);

    m_context_menu->add_separator();

    auto settings_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv));

    m_context_menu->add_action(GUI::Action::create("Keyboard &Settings",
        settings_icon,
        [&](auto&) {
            GUI::Process::spawn_or_show_error(window(), "/bin/KeyboardSettings"sv);
        }));

    return {};
}

void KeymapStatusWidget::set_current_keymap(ByteString const& keymap)
{
    m_current_keymap = keymap;
    set_tooltip(MUST(String::from_byte_string(keymap)));
    update();
}

void KeymapStatusWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.clear_rect(event.rect(), Gfx::Color::Transparent);
    painter.draw_text(rect(), m_current_keymap.substring_view(0, 2), Gfx::TextAlignment::Center, palette().window_text());
}
