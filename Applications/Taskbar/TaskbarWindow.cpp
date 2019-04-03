#include "TaskbarWindow.h"
#include "TaskbarWidget.h"
#include <LibGUI/GWindow.h>
#include <LibGUI/GDesktop.h>
#include <stdio.h>

TaskbarWindow::TaskbarWindow()
{
    set_window_type(GWindowType::Taskbar);
    set_title("Taskbar");
    set_should_exit_event_loop_on_close(true);

    on_screen_rect_change(GDesktop::the().rect());

    GDesktop::the().on_rect_change = [this] (const Rect& rect) { on_screen_rect_change(rect); };

    auto* widget = new TaskbarWidget;
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
