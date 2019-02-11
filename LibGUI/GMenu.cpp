#include <LibGUI/GMenu.h>

GMenu::GMenu(const String& name)
    : m_name(name)
{
}

GMenu::~GMenu()
{
}

void GMenu::add_item(unsigned identifier, const String& text)
{
    m_items.append({ identifier, text });
}

void GMenu::add_separator()
{
    m_items.append(GMenuItem(GMenuItem::Separator));
}
