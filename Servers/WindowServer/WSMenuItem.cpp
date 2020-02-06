/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "WSMenuItem.h"
#include "WSClientConnection.h"
#include "WSMenu.h"
#include "WSWindowManager.h"
#include <LibGfx/GraphicsBitmap.h>

WSMenuItem::WSMenuItem(WSMenu& menu, unsigned identifier, const String& text, const String& shortcut_text, bool enabled, bool checkable, bool checked, const Gfx::Bitmap* icon)
    : m_menu(menu)
    , m_type(Text)
    , m_enabled(enabled)
    , m_checkable(checkable)
    , m_checked(checked)
    , m_identifier(identifier)
    , m_text(text)
    , m_shortcut_text(shortcut_text)
    , m_icon(icon)
{
}

WSMenuItem::WSMenuItem(WSMenu& menu, Type type)
    : m_menu(menu)
    , m_type(type)
{
}

WSMenuItem::~WSMenuItem()
{
}

void WSMenuItem::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    m_menu.redraw();
}

void WSMenuItem::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;
    m_menu.redraw();
}

WSMenu* WSMenuItem::submenu()
{
    ASSERT(is_submenu());
    if (m_menu.client())
        return m_menu.client()->find_menu_by_id(m_submenu_id);
    return WSMenuManager::the().find_internal_menu_by_id(m_submenu_id);
}

Rect WSMenuItem::rect() const
{
    if (!m_menu.is_scrollable())
        return m_rect;
    return m_rect.translated(0, m_menu.item_height() - (m_menu.scroll_offset() * m_menu.item_height()));
}
