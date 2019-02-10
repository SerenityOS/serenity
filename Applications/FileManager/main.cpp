#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GListBox.h>
#include <LibGUI/GEventLoop.h>
#include <sys/wait.h>
#include <signal.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include "DirectoryView.h"

static GWindow* make_window();

int main(int, char**)
{
    GEventLoop loop;

    auto* window = make_window();
    window->set_should_exit_app_on_close(true);
    window->show();

    return loop.exec();
}

GWindow* make_window()
{
    auto* window = new GWindow;
    window->set_title("FileManager");
    window->set_rect(20, 200, 240, 300);

    auto* widget = new GWidget;
    window->set_main_widget(widget);

    auto* directory_view = new DirectoryView(widget);
    directory_view->set_relative_rect({ 0, 0, 240, 300 });

    directory_view->on_path_change = [window] (const String& new_path) {
        window->set_title(String::format("FileManager: %s", new_path.characters()));
    };

    directory_view->open("/");

    return window;
}

