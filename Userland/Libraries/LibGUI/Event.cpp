/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/StringBuilder.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>

namespace GUI {

DropEvent::DropEvent(const Gfx::IntPoint& position, const String& text, NonnullRefPtr<Core::MimeData> mime_data)
    : Event(Event::Drop)
    , m_position(position)
    , m_text(text)
    , m_mime_data(move(mime_data))
{
}

DropEvent::~DropEvent()
{
}

String KeyEvent::to_string() const
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
    return builder.to_string();
}

ActionEvent::ActionEvent(Type type, Action& action)
    : Event(type)
    , m_action(action)
{
}

ActionEvent::~ActionEvent()
{
}

}
