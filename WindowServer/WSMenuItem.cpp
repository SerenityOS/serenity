#include "WSMenuItem.h"

WSMenuItem::WSMenuItem(unsigned identifier, const String& text)
    : m_type(Text)
    , m_identifier(identifier)
    , m_text(text)
{
}

WSMenuItem::WSMenuItem(Type type)
    : m_type(type)
{
}

WSMenuItem::~WSMenuItem()
{
}
