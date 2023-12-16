/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MenuItem.h"
#include "ConnectionFromClient.h"
#include "Menu.h"
#include "WindowManager.h"
#include <LibGfx/Bitmap.h>

namespace WindowServer {

MenuItem::MenuItem(Menu& menu, unsigned identifier, ByteString const& text, ByteString const& shortcut_text, bool enabled, bool visible, bool checkable, bool checked, Gfx::Bitmap const* icon)
    : m_menu(menu)
    , m_type(Text)
    , m_enabled(enabled)
    , m_visible(visible)
    , m_checkable(checkable)
    , m_checked(checked)
    , m_identifier(identifier)
    , m_text(text)
    , m_shortcut_text(shortcut_text)
    , m_icon(icon)
{
    menu.invalidate_menu_window();
}

MenuItem::MenuItem(Menu& menu, Type type)
    : m_menu(menu)
    , m_type(type)
{
}

void MenuItem::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    m_menu.redraw();
}

void MenuItem::set_visible(bool visible)
{
    if (m_visible == visible)
        return;
    m_visible = visible;
    m_menu.invalidate_menu_window();
}

void MenuItem::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;
    m_menu.redraw();
}

void MenuItem::set_default(bool is_default)
{
    if (m_default == is_default)
        return;
    m_default = is_default;
    m_menu.redraw();
}

Menu* MenuItem::submenu()
{
    VERIFY(is_submenu());
    VERIFY(m_menu.client());
    return m_menu.client()->find_menu_by_id(m_submenu_id);
}

Menu const* MenuItem::submenu() const
{
    VERIFY(is_submenu());
    VERIFY(m_menu.client());
    return m_menu.client()->find_menu_by_id(m_submenu_id);
}

Gfx::IntRect MenuItem::rect() const
{
    if (!m_menu.is_scrollable())
        return m_rect;
    return m_rect.translated(0, m_menu.item_height() - (m_menu.scroll_offset() * m_menu.item_height()));
}

void MenuItem::set_icon(Gfx::Bitmap const* icon)
{
    if (m_icon == icon)
        return;
    m_icon = icon;
    m_menu.redraw();
}

}
