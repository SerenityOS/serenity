#include <LibCore/CTimer.h>
#include <SharedGraphics/Font.h>
#include <SharedGraphics/Painter.h>
#include <WindowServer/WSMenuBarKeeper.h>
#include <WindowServer/WSWindowManager.h>
#include <time.h>
#include <unistd.h>

WSMenuBarKeeper::WSMenuBarKeeper()
{
    m_username = getlogin();

    new CTimer(300, [this] {
        static time_t last_update_time;
        time_t now = time(nullptr);
        if (now != last_update_time || m_cpu_monitor.is_dirty()) {
            tick_clock();
            last_update_time = now;
            m_cpu_monitor.set_dirty(false);
        }
    });
}

WSMenuBarKeeper::~WSMenuBarKeeper()
{
}

void WSMenuBarKeeper::setup()
{
    m_window = make<WSWindow>(*this, WSWindowType::Menubar);
    m_window->set_rect(WSWindowManager::the().menubar_rect());
}

void WSMenuBarKeeper::draw()
{
    auto& wm = WSWindowManager::the();
    auto menubar_rect = wm.menubar_rect();

    Painter painter(*window().backing_store());

    painter.fill_rect(menubar_rect, Color::LightGray);
    painter.draw_line({ 0, menubar_rect.bottom() }, { menubar_rect.right(), menubar_rect.bottom() }, Color::MidGray);
    int index = 0;
    wm.for_each_active_menubar_menu([&](WSMenu& menu) {
        Color text_color = Color::Black;
        if (&menu == wm.current_menu()) {
            painter.fill_rect(menu.rect_in_menubar(), wm.menu_selection_color());
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

void WSMenuBarKeeper::tick_clock()
{
    refresh();
}

void WSMenuBarKeeper::refresh()
{
    if (!m_window)
        return;
    draw();
    window().invalidate();
}

void WSMenuBarKeeper::event(CEvent& event)
{
    return CObject::event(event);
}
