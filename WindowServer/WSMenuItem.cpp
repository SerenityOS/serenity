#include "WSMenuItem.h"

WSMenuItem::WSMenuItem(const String& text)
    : m_type(Text)
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
