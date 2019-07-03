#pragma once

#include <AK/AKString.h>
#include <AK/Traits.h>
#include <Kernel/KeyCode.h>

class GShortcut {
public:
    GShortcut() {}
    GShortcut(u8 modifiers, KeyCode key)
        : m_modifiers(modifiers)
        , m_key(key)
    {
    }

    bool is_valid() const { return m_key != KeyCode::Key_Invalid; }
    u8 modifiers() const { return m_modifiers; }
    KeyCode key() const { return m_key; }
    String to_string() const;

    bool operator==(const GShortcut& other) const
    {
        return m_modifiers == other.m_modifiers
            && m_key == other.m_key;
    }

private:
    u8 m_modifiers { 0 };
    KeyCode m_key { KeyCode::Key_Invalid };
};

namespace AK {

template<>
struct Traits<GShortcut> : public GenericTraits<GShortcut> {
    static unsigned hash(const GShortcut& shortcut)
    {
        return pair_int_hash(shortcut.modifiers(), shortcut.key());
    }
};

}
