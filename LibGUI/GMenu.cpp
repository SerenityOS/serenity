#include <LibGUI/GMenu.h>
#include <LibC/gui.h>

GMenu::GMenu(const String& name)
    : m_name(name)
{
}

GMenu::~GMenu()
{
    if (m_menu_id) {
        gui_menu_destroy(m_menu_id);
        m_menu_id = 0;
    }
}

void GMenu::add_item(unsigned identifier, const String& text)
{
    m_items.append({ identifier, text });
}

void GMenu::add_separator()
{
    m_items.append(GMenuItem(GMenuItem::Separator));
}

int GMenu::realize_menu()
{
    m_menu_id = gui_menu_create(m_name.characters());
    ASSERT(m_menu_id > 0);
    for (auto& item : m_items) {
        if (item.type() == GMenuItem::Separator)
            gui_menu_add_separator(m_menu_id);
        else if (item.type() == GMenuItem::Text)
            gui_menu_add_item(m_menu_id, item.identifier(), item.text().characters());
    }
    return m_menu_id;
}
