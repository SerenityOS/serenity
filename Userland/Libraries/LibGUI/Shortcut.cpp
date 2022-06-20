/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Geordie Hall <me@geordiehall.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGUI/Shortcut.h>

namespace GUI {

String Shortcut::to_string() const
{
    Vector<String, 8> parts;

    if (m_modifiers & Mod_Ctrl)
        parts.append("Ctrl");
    if (m_modifiers & Mod_Shift)
        parts.append("Shift");
    if (m_modifiers & Mod_Alt)
        parts.append("Alt");
    if (m_modifiers & Mod_Super)
        parts.append("Super");

    if (m_type == Type::Keyboard) {
        if (auto* key_name = key_code_to_string(m_keyboard_key))
            parts.append(key_name);
        else
            parts.append("(Invalid)");
    } else {
        if (m_mouse_button != MouseButton::None)
            parts.append(String::formatted("Mouse {}", mouse_button_to_string(m_mouse_button)));
        else
            parts.append("(Invalid)");
    }

    StringBuilder builder;
    builder.join('+', parts);
    return builder.to_string();
}

}
