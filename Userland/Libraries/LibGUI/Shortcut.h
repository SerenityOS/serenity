/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>
#include <Kernel/API/KeyCode.h>

namespace GUI {

class Shortcut {
public:
    Shortcut() = default;
    Shortcut(u8 modifiers, KeyCode key)
        : m_modifiers(modifiers)
        , m_key(key)
    {
    }
    Shortcut(KeyCode key)
        : m_modifiers(0)
        , m_key(key)
    {
    }

    bool is_valid() const { return m_key != KeyCode::Key_Invalid; }
    u8 modifiers() const { return m_modifiers; }
    KeyCode key() const { return m_key; }
    String to_string() const;

    bool operator==(const Shortcut& other) const
    {
        return m_modifiers == other.m_modifiers
            && m_key == other.m_key;
    }

private:
    u8 m_modifiers { 0 };
    KeyCode m_key { KeyCode::Key_Invalid };
};

}

namespace AK {

template<>
struct Traits<GUI::Shortcut> : public GenericTraits<GUI::Shortcut> {
    static unsigned hash(const GUI::Shortcut& shortcut)
    {
        return pair_int_hash(shortcut.modifiers(), shortcut.key());
    }
};

}
