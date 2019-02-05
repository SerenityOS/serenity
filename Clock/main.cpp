#include <LibGUI/GEventLoop.h>
#include <LibGUI/GWindow.h>
#include "ClockWidget.h"

int main(int, char**)
{
    GEventLoop loop;

    auto* window = new GWindow;
    window->set_title("Clock");
    window->set_rect({ 100, 100, 100, 40 });

    auto* clock_widget = new ClockWidget;
    clock_widget->set_relative_rect({ 0, 0, 100, 40 });
    window->set_main_widget(clock_widget);

    window->show();
    return loop.exec();
}


