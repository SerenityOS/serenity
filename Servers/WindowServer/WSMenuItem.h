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

#pragma once

#include <AK/String.h>
#include <AK/Function.h>
#include <LibDraw/Rect.h>

class GraphicsBitmap;
class WSMenu;

class WSMenuItem {
public:
    enum Type {
        None,
        Text,
        Separator,
    };

    WSMenuItem(WSMenu&, unsigned identifier, const String& text, const String& shortcut_text = {}, bool enabled = true, bool checkable = false, bool checked = false, const GraphicsBitmap* icon = nullptr);
    WSMenuItem(WSMenu&, Type);
    ~WSMenuItem();

    Type type() const { return m_type; }

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    String text() const { return m_text; }
    void set_text(const String& text) { m_text = text; }

    String shortcut_text() const { return m_shortcut_text; }
    void set_shortcut_text(const String& text) { m_shortcut_text = text; }

    void set_rect(const Rect& rect) { m_rect = rect; }
    Rect rect() const { return m_rect; }

    unsigned identifier() const { return m_identifier; }

    const GraphicsBitmap* icon() const { return m_icon; }
    void set_icon(const GraphicsBitmap* icon) { m_icon = icon; }

    bool is_submenu() const { return m_submenu_id != -1; }
    int submenu_id() const { return m_submenu_id; }
    void set_submenu_id(int submenu_id) { m_submenu_id = submenu_id; }

    WSMenu* submenu();

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool exclusive) { m_exclusive = exclusive; }

private:
    WSMenu& m_menu;
    Type m_type { None };
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
    Rect m_rect;
    RefPtr<GraphicsBitmap> m_icon;
    int m_submenu_id { -1 };
    bool m_exclusive { false };
};
