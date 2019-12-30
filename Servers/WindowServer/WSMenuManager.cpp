#include <LibDraw/Font.h>
#include <LibDraw/Painter.h>
#include <WindowServer/WSMenuManager.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <unistd.h>

WSMenuManager::WSMenuManager()
{
    m_username = getlogin();
    m_needs_window_resize = true;
}

WSMenuManager::~WSMenuManager()
{
}

void WSMenuManager::setup()
{
    m_window = WSWindow::construct(*this, WSWindowType::Menubar);
    m_window->set_rect(menubar_rect());
}

bool WSMenuManager::is_open(const WSMenu& menu) const
{
    for (int i = 0; i < m_open_menu_stack.size(); ++i) {
        if (&menu == m_open_menu_stack[i].ptr())
            return true;
    }
    return false;
}

void WSMenuManager::draw()
{
    auto& wm = WSWindowManager::the();
    auto palette = wm.palette();
    auto menubar_rect = this->menubar_rect();

    if (m_needs_window_resize) {
        int username_width = Font::default_bold_font().width(m_username);

        m_username_rect = {
            menubar_rect.right() - menubar_menu_margin() / 2 - Font::default_bold_font().width(m_username),
            menubar_rect.y(),
            username_width,
            menubar_rect.height()
        };

        int right_edge_x = m_username_rect.left() - 4;
        for (auto& existing_applet : m_applets) {
            if (!existing_applet)
                continue;

            Rect new_applet_rect(right_edge_x - existing_applet->size().width(), 0, existing_applet->size().width(), existing_applet->size().height());
            Rect dummy_menubar_rect(0, 0, 0, 18);
            new_applet_rect.center_vertically_within(dummy_menubar_rect);

            existing_applet->set_rect_in_menubar(new_applet_rect);
            right_edge_x = existing_applet->rect_in_menubar().x() - 4;
        }

        m_window->set_rect(menubar_rect);
        m_needs_window_resize = false;
    }

    Painter painter(*window().backing_store());

    painter.fill_rect(menubar_rect, palette.window());
    painter.draw_line({ 0, menubar_rect.bottom() }, { menubar_rect.right(), menubar_rect.bottom() }, palette.threed_shadow1());
    int index = 0;
    wm.for_each_active_menubar_menu([&](WSMenu& menu) {
        Color text_color = palette.window_text();
        if (is_open(menu)) {
            painter.fill_rect(menu.rect_in_menubar(), palette.menu_selection());
            painter.draw_rect(menu.rect_in_menubar(), palette.menu_selection().darkened());
            text_color = Color::White;
        }
        painter.draw_text(
            menu.text_rect_in_menubar(),
            menu.name(),
            index == 1 ? wm.app_menu_font() : wm.menu_font(),
            TextAlignment::CenterLeft,
            text_color);
        ++index;
        return IterationDecision::Continue;
    });

    painter.draw_text(m_username_rect, m_username, Font::default_bold_font(), TextAlignment::CenterRight, palette.window_text());

    for (auto& applet : m_applets) {
        if (!applet)
            continue;
        draw_applet(*applet);
    }
}

void WSMenuManager::tick_clock()
{
    refresh();
}

void WSMenuManager::refresh()
{
    if (!m_window)
        return;
    draw();
    window().invalidate();
}

void WSMenuManager::event(CEvent& event)
{
    if (WSWindowManager::the().active_window_is_modal())
        return CObject::event(event);

    if (event.type() == WSEvent::MouseMove || event.type() == WSEvent::MouseUp || event.type() == WSEvent::MouseDown || event.type() == WSEvent::MouseWheel) {

        auto& mouse_event = static_cast<WSMouseEvent&>(event);
        WSWindowManager::the().for_each_active_menubar_menu([&](WSMenu& menu) {
            if (menu.rect_in_menubar().contains(mouse_event.position())) {
                handle_menu_mouse_event(menu, mouse_event);
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        for (auto& applet : m_applets) {
            if (!applet)
                continue;
            if (!applet->rect_in_menubar().contains(mouse_event.position()))
                continue;
            auto local_event = mouse_event.translated(-applet->rect_in_menubar().location());
            applet->event(local_event);
        }
    }
    return CObject::event(event);
}

void WSMenuManager::handle_menu_mouse_event(WSMenu& menu, const WSMouseEvent& event)
{
    auto& wm = WSWindowManager::the();
    bool is_hover_with_any_menu_open = event.type() == WSMouseEvent::MouseMove
        && !m_open_menu_stack.is_empty()
        && (m_open_menu_stack.first()->menubar() || m_open_menu_stack.first() == wm.system_menu());
    bool is_mousedown_with_left_button = event.type() == WSMouseEvent::MouseDown && event.button() == MouseButton::Left;
    bool should_open_menu = &menu != m_current_menu && (is_hover_with_any_menu_open || is_mousedown_with_left_button);

    if (is_mousedown_with_left_button)
        m_bar_open = !m_bar_open;

    if (should_open_menu && m_bar_open) {
        if (m_current_menu == &menu)
            return;
        close_everyone();
        if (!menu.is_empty()) {
            menu.redraw_if_theme_changed();
            auto& menu_window = menu.ensure_menu_window();
            menu_window.move_to({ menu.rect_in_menubar().x(), menu.rect_in_menubar().bottom() + 2 });
            menu_window.set_visible(true);
        }
        set_current_menu(&menu);
        refresh();
        return;
    }

    if (!m_bar_open)
        close_everyone();
}

void WSMenuManager::set_needs_window_resize()
{
    m_needs_window_resize = true;
}

void WSMenuManager::close_everyone()
{
    for (auto& menu : m_open_menu_stack) {
        if (menu && menu->menu_window())
            menu->menu_window()->set_visible(false);
    }
    m_open_menu_stack.clear();
    m_current_menu = nullptr;
    refresh();
}

void WSMenuManager::close_everyone_not_in_lineage(WSMenu& menu)
{
    Vector<WSMenu*> menus_to_close;
    for (auto& open_menu : m_open_menu_stack) {
        if (!open_menu)
            continue;
        if (&menu == open_menu.ptr() || open_menu->is_menu_ancestor_of(menu))
            continue;
        menus_to_close.append(open_menu);
    }
    close_menus(menus_to_close);
}

void WSMenuManager::close_menus(const Vector<WSMenu*>& menus)
{
    for (auto& menu : menus) {
        if (menu == m_current_menu)
            m_current_menu = nullptr;
        if (menu->menu_window())
            menu->menu_window()->set_visible(false);
        m_open_menu_stack.remove_first_matching([&](auto& entry) {
            return entry == menu;
        });
    }
    refresh();
}

static void collect_menu_subtree(WSMenu& menu, Vector<WSMenu*>& menus)
{
    menus.append(&menu);
    for (int i = 0; i < menu.item_count(); ++i) {
        auto& item = menu.item(i);
        if (!item.is_submenu())
            continue;
        collect_menu_subtree(*const_cast<WSMenuItem&>(item).submenu(), menus);
    }
}

void WSMenuManager::close_menu_and_descendants(WSMenu& menu)
{
    Vector<WSMenu*> menus_to_close;
    collect_menu_subtree(menu, menus_to_close);
    close_menus(menus_to_close);
}

void WSMenuManager::set_current_menu(WSMenu* menu, bool is_submenu)
{
    if (!is_submenu && m_current_menu)
        m_current_menu->close();
    if (menu)
        m_current_menu = menu->make_weak_ptr();

    if (!is_submenu) {
        close_everyone();
        if (menu)
            m_open_menu_stack.append(menu->make_weak_ptr());
    } else {
        m_open_menu_stack.append(menu->make_weak_ptr());
    }
}

void WSMenuManager::close_bar()
{
    close_everyone();
    m_bar_open = false;
}

void WSMenuManager::add_applet(WSWindow& applet)
{
    int right_edge_x = m_username_rect.left() - 4;
    for (auto& existing_applet : m_applets) {
        if (existing_applet)
            right_edge_x = existing_applet->rect_in_menubar().x() - 4;
    }

    Rect new_applet_rect(right_edge_x - applet.size().width(), 0, applet.size().width(), applet.size().height());
    Rect dummy_menubar_rect(0, 0, 0, 18);
    new_applet_rect.center_vertically_within(dummy_menubar_rect);

    applet.set_rect_in_menubar(new_applet_rect);
    m_applets.append(applet.make_weak_ptr());
}

void WSMenuManager::remove_applet(WSWindow& applet)
{
    m_applets.remove_first_matching([&](auto& entry) {
        return &applet == entry.ptr();
    });
}

void WSMenuManager::draw_applet(const WSWindow& applet)
{
    if (!applet.backing_store())
        return;
    Painter painter(*window().backing_store());
    painter.fill_rect(applet.rect_in_menubar(), WSWindowManager::the().palette().window());
    painter.blit(applet.rect_in_menubar().location(), *applet.backing_store(), applet.backing_store()->rect());
}

void WSMenuManager::invalidate_applet(const WSWindow& applet, const Rect& rect)
{
    draw_applet(applet);
    window().invalidate(rect.translated(applet.rect_in_menubar().location()));
}

Rect WSMenuManager::menubar_rect() const
{
    return { 0, 0, WSScreen::the().rect().width(), 18 };
}
