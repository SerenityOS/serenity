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
}

TaskbarWindow::~TaskbarWindow()
{
}

void TaskbarWindow::on_screen_rect_change(const Rect& rect)
{
    Rect new_rect { rect.x(), rect.bottom() - taskbar_height() + 1, rect.width(), taskbar_height() };
    set_rect(new_rect);
}

void TaskbarWindow::wm_event(GWMEvent& event)
{
    WindowIdentifier identifier { event.client_id(), event.window_id() };
    switch (event.type()) {
    case GEvent::WM_WindowAdded: {
        auto& added_event = static_cast<GWMWindowAddedEvent&>(event);
        printf("WM_WindowAdded: client_id=%d, window_id=%d, title=%s, rect=%s\n",
            added_event.client_id(),
            added_event.window_id(),
            added_event.title().characters(),
            added_event.rect().to_string().characters()
        );
        auto& window = m_window_list.ensure_window(identifier);
        window.set_title(added_event.title());
        window.set_rect(added_event.rect());
        window.set_button(new GButton(main_widget()));
        window.button()->set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        window.button()->set_preferred_size({ 100, 22 });
        window.button()->set_caption(window.title());
        update();
        break;
    }
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
        printf("WM_WindowStateChanged: client_id=%d, window_id=%d, title=%s, rect=%s\n",
            changed_event.client_id(),
            changed_event.window_id(),
            changed_event.title().characters(),
            changed_event.rect().to_string().characters()
        );
        auto& window = m_window_list.ensure_window(identifier);
        window.set_title(changed_event.title());
        window.set_rect(changed_event.rect());
        window.button()->set_caption(changed_event.title());
        break;
    }
    default:
        break;
    }
}
