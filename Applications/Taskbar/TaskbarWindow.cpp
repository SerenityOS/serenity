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
    button->set_preferred_size({ 100, 22 });
    button->set_checkable(true);
    button->set_text_alignment(TextAlignment::CenterLeft);
    return button;
}

void TaskbarWindow::wm_event(GWMEvent& event)
{
    WindowIdentifier identifier { event.client_id(), event.window_id() };
    switch (event.type()) {
    case GEvent::WM_WindowAdded: {
        auto& added_event = static_cast<GWMWindowAddedEvent&>(event);
        printf("WM_WindowAdded: client_id=%d, window_id=%d, title=%s, rect=%s, is_active=%u\n",
            added_event.client_id(),
            added_event.window_id(),
            added_event.title().characters(),
            added_event.rect().to_string().characters(),
            added_event.is_active()
        );
        auto& window = m_window_list.ensure_window(identifier);
        window.set_title(added_event.title());
        window.set_rect(added_event.rect());
        window.set_active(added_event.is_active());
        window.button()->set_caption(window.title());
        window.button()->set_checked(window.is_active());
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
        printf("WM_WindowStateChanged: client_id=%d, window_id=%d, title=%s, rect=%s, is_active=%u\n",
            changed_event.client_id(),
            changed_event.window_id(),
            changed_event.title().characters(),
            changed_event.rect().to_string().characters(),
            changed_event.is_active()
        );
        auto& window = m_window_list.ensure_window(identifier);
        window.set_title(changed_event.title());
        window.set_rect(changed_event.rect());
        window.set_active(changed_event.is_active());
        window.button()->set_caption(changed_event.title());
        window.button()->set_checked(changed_event.is_active());
        break;
    }
    default:
        break;
    }
}
