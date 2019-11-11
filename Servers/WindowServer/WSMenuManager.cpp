#include <LibCore/CTimer.h>
#include <LibDraw/Font.h>
#include <LibDraw/Painter.h>
#include <WindowServer/WSMenuManager.h>
#include <WindowServer/WSWindowManager.h>
#include <time.h>
#include <unistd.h>

WSMenuManager::WSMenuManager()
{
    m_username = getlogin();
    m_needs_window_resize = false;

    m_timer = CTimer::construct(300, [this] {
        static time_t last_update_time;
        time_t now = time(nullptr);
        if (now != last_update_time || m_cpu_monitor.is_dirty()) {
            tick_clock();
            last_update_time = now;
            m_cpu_monitor.set_dirty(false);
        }
    });
}

WSMenuManager::~WSMenuManager()
{
}

void WSMenuManager::setup()
{
    m_window = WSWindow::construct(*this, WSWindowType::Menubar);
    m_window->set_rect(WSWindowManager::the().menubar_rect());
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
    auto menubar_rect = wm.menubar_rect();

    if (m_needs_window_resize) {
        m_window->set_rect(menubar_rect);
        m_needs_window_resize = false;
    }

    Painter painter(*window().backing_store());

    painter.fill_rect(menubar_rect, Color::WarmGray);
    painter.draw_line({ 0, menubar_rect.bottom() }, { menubar_rect.right(), menubar_rect.bottom() }, Color::MidGray);
    int index = 0;
    wm.for_each_active_menubar_menu([&](WSMenu& menu) {
        Color text_color = Color::Black;
        if (is_open(menu)) {
            painter.fill_rect(menu.rect_in_menubar(), Color::from_rgb(0xad714f));
            painter.draw_rect(menu.rect_in_menubar(), Color::from_rgb(0x793016));
            text_color = Color::White;
        }
        painter.draw_text(
            menu.text_rect_in_menubar(),
            menu.name(),
            index == 1 ? wm.app_menu_font() : wm.menu_font(),
            TextAlignment::CenterLeft,
            text_color);
        ++index;
        return true;
    });

    int username_width = Font::default_bold_font().width(m_username);
    Rect username_rect {
        menubar_rect.right() - wm.menubar_menu_margin() / 2 - Font::default_bold_font().width(m_username),
        menubar_rect.y(),
        username_width,
        menubar_rect.height()
    };
    painter.draw_text(username_rect, m_username, Font::default_bold_font(), TextAlignment::CenterRight, Color::Black);

    time_t now = time(nullptr);
    auto* tm = localtime(&now);
    auto time_text = String::format("%4u-%02u-%02u %02u:%02u:%02u",
        tm->tm_year + 1900,
        tm->tm_mon + 1,
        tm->tm_mday,
        tm->tm_hour,
        tm->tm_min,
        tm->tm_sec);
    int time_width = wm.font().width(time_text);
    Rect time_rect {
        username_rect.left() - wm.menubar_menu_margin() / 2 - time_width,
        menubar_rect.y(),
        time_width,
        menubar_rect.height()
    };

    painter.draw_text(time_rect, time_text, wm.font(), TextAlignment::CenterRight, Color::Black);

    Rect cpu_rect { time_rect.right() - wm.font().width(time_text) - m_cpu_monitor.capacity() - 10, time_rect.y() + 1, m_cpu_monitor.capacity(), time_rect.height() - 2 };
    m_cpu_monitor.paint(painter, cpu_rect);
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
                return false;
            }
            return true;
        });
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

    if (should_open_menu) {
        if (m_current_menu == &menu)
            return;
        close_everyone();
        if (!menu.is_empty()) {
            auto& menu_window = menu.ensure_menu_window();
            menu_window.move_to({ menu.rect_in_menubar().x(), menu.rect_in_menubar().bottom() + 2 });
            menu_window.set_visible(true);
        }
        set_current_menu(&menu);
        refresh();
        return;
    }
    if (event.type() == WSMouseEvent::MouseDown && event.button() == MouseButton::Left) {
        close_everyone();
        set_current_menu(nullptr);
        return;
    }
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
