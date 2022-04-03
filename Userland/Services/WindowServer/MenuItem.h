/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>

namespace WindowServer {

class Menu;

class MenuItem {
public:
    enum Type {
        None,
        Text,
        Separator,
    };

    MenuItem(Menu&, unsigned identifier, String const& text, String const& shortcut_text = {}, bool enabled = true, bool checkable = false, bool checked = false, Gfx::Bitmap const* icon = nullptr);
    MenuItem(Menu&, Type);
    ~MenuItem() = default;

    Type type() const { return m_type; }

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    bool is_default() const { return m_default; }
    void set_default(bool);

    String text() const { return m_text; }
    void set_text(String text) { m_text = move(text); }

    String shortcut_text() const { return m_shortcut_text; }
    void set_shortcut_text(String text) { m_shortcut_text = move(text); }

    void set_rect(Gfx::IntRect const& rect) { m_rect = rect; }
    Gfx::IntRect rect() const;

    unsigned identifier() const { return m_identifier; }

    Gfx::Bitmap const* icon() const { return m_icon; }
    void set_icon(Gfx::Bitmap const*);

    bool is_submenu() const { return m_submenu_id != -1; }
    int submenu_id() const { return m_submenu_id; }
    void set_submenu_id(int submenu_id) { m_submenu_id = submenu_id; }

    Menu* submenu();
    Menu const* submenu() const;

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool exclusive) { m_exclusive = exclusive; }

private:
    Menu& m_menu;
    Type m_type { None };
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    bool m_default { false };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
    Gfx::IntRect m_rect;
    RefPtr<Gfx::Bitmap> m_icon;
    int m_submenu_id { -1 };
    bool m_exclusive { false };
};

}
