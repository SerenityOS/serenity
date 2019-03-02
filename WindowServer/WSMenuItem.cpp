#include "WSMenuItem.h"

WSMenuItem::WSMenuItem(unsigned identifier, const String& text, const String& shortcut_text)
    : m_type(Text)
    , m_identifier(identifier)
    , m_text(text)
    , m_shortcut_text(shortcut_text)
{
}

WSMenuItem::WSMenuItem(Type type)
    : m_type(type)
{
}

WSMenuItem::~WSMenuItem()
{
}
