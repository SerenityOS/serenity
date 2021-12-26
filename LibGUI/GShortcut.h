#pragma once

#include <Kernel/KeyCode.h>
#include <AK/AKString.h>
#include <AK/Traits.h>

class GShortcut {
public:
    GShortcut() { }
    GShortcut(byte modifiers, KeyCode key)
        : m_modifiers(modifiers)
        , m_key(key)
    {
    }

    bool is_valid() const { return m_key != KeyCode::Key_Invalid; }
    byte modifiers() const { return m_modifiers; }
    KeyCode key() const { return m_key; }
    String to_string() const;

    bool operator==(const GShortcut& other) const
    {
        return m_modifiers == other.m_modifiers
            && m_key == other.m_key;
    }

private:
    byte m_modifiers { 0 };
    KeyCode m_key { KeyCode::Key_Invalid };
};

namespace AK {

template<>
struct Traits<GShortcut> {
    static unsigned hash(const GShortcut& shortcut)
    {
        return pair_int_hash(shortcut.modifiers(), shortcut.key());
    }
};

}
