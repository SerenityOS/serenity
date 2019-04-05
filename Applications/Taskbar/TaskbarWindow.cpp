#include "TaskbarWindow.h"
#include "TaskbarWidget.h"
#include <LibGUI/GWindow.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GButton.h>
#include <WindowServer/WSAPITypes.h>
#include <stdio.h>

TaskbarWindow::TaskbarWindow()
{
    set_window_type(GWindowType::Taskbar);
    set_title("Taskbar");
    set_should_exit_event_loop_on_close(true);

    on_screen_rect_change(GDesktop::the().rect());

    GDesktop::the().on_rect_change = [this] (const Rect& rect) { on_screen_rect_change(rect); };

    auto* widget = new TaskbarWidget(m_window_list);
    set_main_widget(widget);

    m_window_list.aid_create_button = [this] {
        return create_button();
    };
}

TaskbarWindow::~TaskbarWindow()
{
}

void TaskbarWindow::on_screen_rect_change(const Rect& rect)
{
    Rect new_rect { rect.x(), rect.bottom() - taskbar_height() + 1, rect.width(), taskbar_height() };
    set_rect(new_rect);
}

GButton* TaskbarWindow::create_button()
{
    auto* button = new GButton(main_widget());
    button->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    button->set_preferred_size({ 140, 22 });
    button->set_checkable(true);
    button->set_text_alignment(TextAlignment::CenterLeft);
    return button;
}

static bool should_include_window(GWindowType window_type)
{
    return window_type == GWindowType::Normal;
}

void TaskbarWindow::wm_event(GWMEvent& event)
{
    WindowIdentifier identifier { event.client_id(), event.window_id() };
    switch (event.type()) {
    case GEvent::WM_WindowRemoved: {
        auto& removed_event = static_cast<GWMWindowRemovedEvent&>(event);
        printf("WM_WindowRemoved: client_id=%d, window_id=%d\n",
            removed_event.client_id(),
            removed_event.window_id()
        );
        m_window_list.remove_window(identifier);
        update();
        break;
    }
    case GEvent::WM_WindowStateChanged: {
        auto& changed_event = static_cast<GWMWindowStateChangedEvent&>(event);
        printf("WM_WindowStateChanged: client_id=%d, window_id=%d, title=%s, rect=%s, is_active=%u, is_minimized=%u\n",
            changed_event.client_id(),
            changed_event.window_id(),
            changed_event.title().characters(),
            changed_event.rect().to_string().characters(),
            changed_event.is_active(),
            changed_event.is_minimized()
        );
        if (!should_include_window(changed_event.window_type()))
            break;
        auto& window = m_window_list.ensure_window(identifier);
        window.set_title(changed_event.title());
        window.set_rect(changed_event.rect());
        window.set_active(changed_event.is_active());
        window.set_minimized(changed_event.is_minimized());
        if (window.is_minimized()) {
            window.button()->set_foreground_color(Color::DarkGray);
            window.button()->set_caption(String::format("[%s]", changed_event.title().characters()));
        } else {
            window.button()->set_foreground_color(Color::Black);
            window.button()->set_caption(changed_event.title());
        }
        window.button()->set_checked(changed_event.is_active());
        window.button()->update();
        break;
    }
    default:
        break;
    }
}
