#include <LibGUI/GMenuItem.h>

GMenuItem::GMenuItem(Type type)
    : m_type(type)
{
}

GMenuItem::GMenuItem(unsigned identifier, const String& text)
    : m_type(Text)
    , m_identifier(identifier)
    , m_text(text)
{
}

GMenuItem::~GMenuItem()
{
}

