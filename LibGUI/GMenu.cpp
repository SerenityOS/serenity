#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibC/gui.h>
#include <AK/HashMap.h>

static HashMap<int, GMenu*>& all_menus()
{
    static HashMap<int, GMenu*>* map;
    if (!map)
        map = new HashMap<int, GMenu*>();
    return *map;
}

GMenu* GMenu::from_menu_id(int menu_id)
{
    auto it = all_menus().find(menu_id);
    if (it == all_menus().end())
        return nullptr;
    return (*it).value;
}

GMenu::GMenu(const String& name)
    : m_name(name)
{
}

GMenu::~GMenu()
{
    if (m_menu_id) {
        all_menus().remove(m_menu_id);
        gui_menu_destroy(m_menu_id);
        m_menu_id = 0;
    }
}

void GMenu::add_action(OwnPtr<GAction>&& action)
{
    m_items.append(make<GMenuItem>(move(action)));
}

void GMenu::add_separator()
{
    m_items.append(make<GMenuItem>(GMenuItem::Separator));
}

int GMenu::realize_menu()
{
    m_menu_id = gui_menu_create(m_name.characters());
    ASSERT(m_menu_id > 0);
    for (size_t i = 0; i < m_items.size(); ++i) {
        auto& item = *m_items[i];
        if (item.type() == GMenuItem::Separator) {
            gui_menu_add_separator(m_menu_id);
            continue;
        }
        if (item.type() == GMenuItem::Action) {
            auto& action = *item.action();
            gui_menu_add_item(m_menu_id, i, action.text().characters());
        }
    }
    all_menus().set(m_menu_id, this);
    return m_menu_id;
}

GAction* GMenu::action_at(size_t index)
{
    if (index >= m_items.size())
        return nullptr;
    return m_items[index]->action();
}
