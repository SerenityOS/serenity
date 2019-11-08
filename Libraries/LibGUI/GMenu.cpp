#include <AK/HashMap.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GWindowServerConnection.h>

//#define GMENU_DEBUG

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

GMenu::GMenu(const StringView& name)
    : m_name(name)
{
}

GMenu::~GMenu()
{
    unrealize_menu();
}

void GMenu::add_action(NonnullRefPtr<GAction> action)
{
    m_items.append(make<GMenuItem>(m_menu_id, move(action)));
#ifdef GMENU_DEBUG
    dbgprintf("GMenu::add_action(): MenuItem Menu ID: %d\n", m_menu_id);
#endif
}

void GMenu::add_submenu(NonnullOwnPtr<GMenu> submenu)
{
    m_items.append(make<GMenuItem>(m_menu_id, move(submenu)));
}

void GMenu::add_separator()
{
    m_items.append(make<GMenuItem>(m_menu_id, GMenuItem::Separator));
}

void GMenu::realize_if_needed()
{
    if (m_menu_id == -1)
        realize_menu();
}

void GMenu::popup(const Point& screen_position)
{
    realize_if_needed();
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::PopupMenu;
    request.menu.menu_id = m_menu_id;
    request.menu.position = screen_position;
    GWindowServerConnection::the().post_message_to_server(request);
}

void GMenu::dismiss()
{
    if (m_menu_id == -1)
        return;
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::DismissMenu;
    request.menu.menu_id = m_menu_id;
    GWindowServerConnection::the().post_message_to_server(request);
}

int GMenu::realize_menu()
{
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::CreateMenu;
    ASSERT(m_name.length() < (ssize_t)sizeof(request.text));
    strcpy(request.text, m_name.characters());
    request.text_length = m_name.length();
    auto response = GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidCreateMenu);
    m_menu_id = response.menu.menu_id;

#ifdef GMENU_DEBUG
    dbgprintf("GMenu::realize_menu(): New menu ID: %d\n", m_menu_id);
#endif
    ASSERT(m_menu_id > 0);
    for (int i = 0; i < m_items.size(); ++i) {
        auto& item = m_items[i];
        item.set_menu_id({}, m_menu_id);
        item.set_identifier({}, i);
        if (item.type() == GMenuItem::Separator) {
            WSAPI_ClientMessage request;
            request.type = WSAPI_ClientMessage::Type::AddMenuSeparator;
            request.menu.menu_id = m_menu_id;
            request.menu.submenu_id = -1;
            GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidAddMenuSeparator);
            continue;
        }
        if (item.type() == GMenuItem::Submenu) {
            auto& submenu = *item.submenu();
            submenu.realize_if_needed();
            WSAPI_ClientMessage request;
            request.type = WSAPI_ClientMessage::Type::AddMenuItem;
            request.menu.menu_id = m_menu_id;
            request.menu.submenu_id = submenu.menu_id();
            request.menu.identifier = i;
            // FIXME: It should be possible to disable a submenu.
            request.menu.enabled = true;
            request.menu.checkable = false;
            request.menu.checked = false;

            // no shortcut on submenu, make sure this is cleared out
            request.menu.shortcut_text_length = 0;
            strcpy(request.menu.shortcut_text, "\0");

            ASSERT(submenu.name().length() < (ssize_t)sizeof(request.text));
            strcpy(request.text, submenu.name().characters());
            request.text_length = submenu.name().length();
            GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidAddMenuItem);
            continue;
        }
        if (item.type() == GMenuItem::Action) {
            auto& action = *item.action();
            WSAPI_ClientMessage request;
            request.type = WSAPI_ClientMessage::Type::AddMenuItem;
            request.menu.menu_id = m_menu_id;
            request.menu.submenu_id = -1;
            request.menu.identifier = i;
            request.menu.enabled = action.is_enabled();
            request.menu.checkable = action.is_checkable();
            if (action.icon()) {
                ASSERT(action.icon()->format() == GraphicsBitmap::Format::RGBA32);
                ASSERT(action.icon()->size() == Size(16, 16));
                if (action.icon()->shared_buffer_id() == -1) {
                    auto shared_buffer = SharedBuffer::create_with_size(action.icon()->size_in_bytes());
                    ASSERT(shared_buffer);
                    auto shared_icon = GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, *shared_buffer, action.icon()->size());
                    memcpy(shared_buffer->data(), action.icon()->bits(0), action.icon()->size_in_bytes());
                    shared_buffer->seal();
                    shared_buffer->share_with(GWindowServerConnection::the().server_pid());
                    action.set_icon(shared_icon);
                }
                request.menu.icon_buffer_id = action.icon()->shared_buffer_id();
            } else {
                request.menu.icon_buffer_id = -1;
            }
            if (action.is_checkable())
                request.menu.checked = action.is_checked();
            ASSERT(action.text().length() < (ssize_t)sizeof(request.text));
            strcpy(request.text, action.text().characters());
            request.text_length = action.text().length();

            if (action.shortcut().is_valid()) {
                auto shortcut_text = action.shortcut().to_string();
                ASSERT(shortcut_text.length() < (ssize_t)sizeof(request.menu.shortcut_text));
                strcpy(request.menu.shortcut_text, shortcut_text.characters());
                request.menu.shortcut_text_length = shortcut_text.length();
            } else {
                request.menu.shortcut_text_length = 0;
            }

            GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidAddMenuItem);
        }
    }
    all_menus().set(m_menu_id, this);
    return m_menu_id;
}

void GMenu::unrealize_menu()
{
    if (m_menu_id == -1)
        return;
    all_menus().remove(m_menu_id);
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::DestroyMenu;
    request.menu.menu_id = m_menu_id;
    GWindowServerConnection::the().sync_request(request, WSAPI_ServerMessage::Type::DidDestroyMenu);
    m_menu_id = 0;
}

GAction* GMenu::action_at(int index)
{
    if (index >= m_items.size())
        return nullptr;
    return m_items[index].action();
}
