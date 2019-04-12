#include "WSMenuItem.h"
#include "WSMenu.h"

WSMenuItem::WSMenuItem(WSMenu& menu, unsigned identifier, const String& text, const String& shortcut_text, bool enabled)
    : m_menu(menu)
    , m_type(Text)
    , m_enabled(enabled)
    , m_identifier(identifier)
    , m_text(text)
    , m_shortcut_text(shortcut_text)
{
}

WSMenuItem::WSMenuItem(WSMenu& menu, Type type)
    : m_menu(menu)
    , m_type(type)
{
}

WSMenuItem::~WSMenuItem()
{
}

void WSMenuItem::set_enabled(bool enabled)
{
    if (m_enabled == enabled)
        return;
    m_enabled = enabled;
    m_menu.redraw();
}
