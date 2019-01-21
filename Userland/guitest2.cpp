#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <Kernel/Syscall.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GEventLoop.h>

static GWindow* make_font_test_window();
static GWindow* make_launcher_window();

int main(int argc, char** argv)
{
    GEventLoop loop;
    auto* font_test_window = make_font_test_window();
    font_test_window->show();

    auto* launcher_window = make_launcher_window();
    launcher_window->show();
    return loop.exec();
}

GWindow* make_font_test_window()
{
    auto* window = new GWindow;
    window->set_title("Font test");
    window->set_rect({ 440, 100, 300, 80 });

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_relative_rect({ 0, 0, 300, 80 });

    auto* l1 = new GLabel(widget);
    l1->set_relative_rect({ 0, 0, 300, 20 });
    l1->set_text("0123456789");

    auto* l2 = new GLabel(widget);
    l2->set_relative_rect({ 0, 20, 300, 20 });
    l2->set_text("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    auto* l3 = new GLabel(widget);
    l3->set_relative_rect({ 0, 40, 300, 20 });
    l3->set_text("abcdefghijklmnopqrstuvwxyz");

    auto* l4 = new GLabel(widget);
    l4->set_relative_rect({ 0, 60, 300, 20 });
    l4->set_text("!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~");

    return window;
}

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("Launcher");
    window->set_rect({ 100, 400, 80, 200 });

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_relative_rect({ 0, 0, 80, 200 });

    auto* label = new GLabel(widget);
    label->set_relative_rect({ 0, 0, 80, 20 });
    label->set_text("Apps");

    auto* button = new GButton(widget);
    button->set_relative_rect({ 5, 20, 70, 20 });
    button->set_caption("Terminal");

    button->on_click = [] (GButton&) {
        if (!fork()) {
            execve("/bin/Terminal", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        }
    };

    auto* dummy_button = new GButton(widget);
    dummy_button->set_relative_rect({ 5, 50, 70, 20 });
    dummy_button->set_caption("Dummy");

    return window;
}
