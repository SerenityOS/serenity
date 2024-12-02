/*
 * Copyright (c) 2020-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>

namespace GUI {

DropEvent::DropEvent(Type type, Gfx::IntPoint position, MouseButton button, u32 buttons, u32 modifiers, ByteString const& text, NonnullRefPtr<Core::MimeData const> mime_data)
    : Event(type)
    , m_position(position)
    , m_button(button)
    , m_buttons(buttons)
    , m_modifiers(modifiers)
    , m_text(text)
    , m_mime_data(move(mime_data))
{
}

DropEvent::~DropEvent() = default;

DragEvent::DragEvent(Type type, Gfx::IntPoint position, MouseButton button, u32 buttons, u32 modifiers, ByteString const& text, NonnullRefPtr<Core::MimeData const> mime_data)
    : DropEvent(type, position, button, buttons, modifiers, text, move(mime_data))
{
}

DragEvent::~DragEvent() = default;

ByteString KeyEvent::to_byte_string() const
{
    Vector<ByteString, 8> parts;

    if (m_modifiers & Mod_Ctrl)
        parts.append("Ctrl");
    if (m_modifiers & Mod_Shift)
        parts.append("Shift");
    if (m_modifiers & Mod_Alt)
        parts.append("Alt");
    if (m_modifiers & Mod_Super)
        parts.append("Super");

    if (auto* key_name = key_code_to_string(static_cast<KeyCode>(m_key)))
        parts.append(key_name);
    else
        parts.append("(Invalid)");

    StringBuilder builder;
    for (size_t i = 0; i < parts.size(); ++i) {
        builder.append(parts[i]);
        if (i != parts.size() - 1)
            builder.append('+');
    }
    return builder.to_byte_string();
}

ActionEvent::ActionEvent(Type type, Action& action)
    : Event(type)
    , m_action(action)
{
}

ActionEvent::~ActionEvent() = default;

}
