/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Menubar.h"
#include "WindowManager.h"

namespace WindowServer {

void Menubar::layout_menu(Menu& menu, Gfx::IntRect window_rect)
{
    // FIXME: Maybe move this to the theming system?
    static constexpr auto menubar_menu_margin = 14;

    auto& wm = WindowManager::the();
    auto menubar_rect = wm.palette().window_theme().menubar_rect(Gfx::WindowTheme::WindowType::Normal, Gfx::WindowTheme::WindowMode::Other, window_rect, wm.palette(), 1);

    int text_width = wm.font().width(Gfx::parse_ampersand_string(menu.name()));
    menu.set_rect_in_window_menubar({ m_next_menu_location.x(), 0, text_width + menubar_menu_margin, menubar_rect.height() });
    m_next_menu_location.translate_by(menu.rect_in_window_menubar().width(), 0);
}

}
