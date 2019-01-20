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
    widget->setWindowRelativeRect({ 0, 0, 300, 80 });

    auto* l1 = new GLabel(widget);
    l1->setWindowRelativeRect({ 0, 0, 300, 20 });
    l1->setText("0123456789");

    auto* l2 = new GLabel(widget);
    l2->setWindowRelativeRect({ 0, 20, 300, 20 });
    l2->setText("ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    auto* l3 = new GLabel(widget);
    l3->setWindowRelativeRect({ 0, 40, 300, 20 });
    l3->setText("abcdefghijklmnopqrstuvwxyz");

    auto* l4 = new GLabel(widget);
    l4->setWindowRelativeRect({ 0, 60, 300, 20 });
    l4->setText("!\"#$%&'()*+,-./:;<=>?@[\\]^_{|}~");

    return window;
}

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("Launcher");
    window->set_rect({ 100, 400, 80, 200 });

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->setWindowRelativeRect({ 0, 0, 80, 200 });

    auto* label = new GLabel(widget);
    label->setWindowRelativeRect({ 0, 0, 80, 20 });
    label->setText("Apps");

    auto* button = new GButton(widget);
    button->setWindowRelativeRect({ 5, 20, 70, 20 });
    button->set_caption("Terminal");

    button->on_click = [] (GButton&) {
        if (!fork()) {
            execve("/bin/Terminal", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        }
    };

    return window;
}
