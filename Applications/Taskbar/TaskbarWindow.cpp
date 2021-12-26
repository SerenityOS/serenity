#include "TaskbarWindow.h"
#include "TaskbarButton.h"
#include <LibGUI/GWindow.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GEventLoop.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GFrame.h>
#include <WindowServer/WSAPITypes.h>
#include <stdio.h>

//#define EVENT_DEBUG

TaskbarWindow::TaskbarWindow()
{
    set_window_type(GWindowType::Taskbar);
    set_title("Taskbar");
    set_should_exit_event_loop_on_close(true);

    on_screen_rect_change(GDesktop::the().rect());

    GDesktop::the().on_rect_change = [this] (const Rect& rect) { on_screen_rect_change(rect); };

    auto* widget = new GFrame;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    widget->layout()->set_margins({ 3, 2, 3, 2 });
    widget->layout()->set_spacing(3);
    widget->set_frame_thickness(1);
    widget->set_frame_shape(FrameShape::Panel);
    widget->set_frame_shadow(FrameShadow::Raised);
    set_main_widget(widget);

    WindowList::the().aid_create_button = [this] (auto& identifier) {
        return create_button(identifier);
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

GButton* TaskbarWindow::create_button(const WindowIdentifier& identifier)
{
    auto* button = new TaskbarButton(identifier, main_widget());
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
#ifdef EVENT_DEBUG
        auto& removed_event = static_cast<GWMWindowRemovedEvent&>(event);
        dbgprintf("WM_WindowRemoved: client_id=%d, window_id=%d\n",
            removed_event.client_id(),
            removed_event.window_id()
        );
#endif
        WindowList::the().remove_window(identifier);
        update();
        break;
    }
    case GEvent::WM_WindowRectChanged: {
#ifdef EVENT_DEBUG
        auto& changed_event = static_cast<GWMWindowRectChangedEvent&>(event);
        dbgprintf("WM_WindowRectChanged: client_id=%d, window_id=%d, rect=%s\n",
            changed_event.client_id(),
            changed_event.window_id(),
            changed_event.rect().to_string().characters()
        );
#endif
        break;
    }
    case GEvent::WM_WindowIconChanged: {
        auto& changed_event = static_cast<GWMWindowIconChangedEvent&>(event);
#ifdef EVENT_DEBUG
        dbgprintf("WM_WindowIconChanged: client_id=%d, window_id=%d, icon_path=%s\n",
            changed_event.client_id(),
            changed_event.window_id(),
            changed_event.icon_path().characters()
        );
#endif
        if (auto* window = WindowList::the().window(identifier)) {
            window->set_icon_path(changed_event.icon_path());
            window->button()->set_icon(window->icon());
        }
        break;
    }

    case GEvent::WM_WindowStateChanged: {
        auto& changed_event = static_cast<GWMWindowStateChangedEvent&>(event);
#ifdef EVENT_DEBUG
        dbgprintf("WM_WindowStateChanged: client_id=%d, window_id=%d, title=%s, rect=%s, is_active=%u, is_minimized=%u\n",
            changed_event.client_id(),
            changed_event.window_id(),
            changed_event.title().characters(),
            changed_event.rect().to_string().characters(),
            changed_event.is_active(),
            changed_event.is_minimized()
        );
#endif
        if (!should_include_window(changed_event.window_type()))
            break;
        auto& window = WindowList::the().ensure_window(identifier);
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
        break;
    }
    default:
        break;
    }
}
