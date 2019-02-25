#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <time.h>
#include <Kernel/Syscall.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GCheckBox.h>
#include <LibGUI/GApplication.h>
#include <signal.h>

static GWindow* make_launcher_window();

void handle_sigchld(int)
{
    dbgprintf("Got SIGCHLD\n");
    int pid = waitpid(-1, nullptr, 0);
    dbgprintf("waitpid() returned %d\n", pid);
    ASSERT(pid > 0);
}

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    signal(SIGCHLD, handle_sigchld);

    auto* launcher_window = make_launcher_window();
    launcher_window->set_should_exit_app_on_close(true);
    launcher_window->show();

    return app.exec();
}

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("guitest2");
    window->set_rect({ 100, 400, 100, 230 });

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_relative_rect({ 0, 0, 100, 230 });

    auto* label = new GLabel(widget);
    label->set_relative_rect({ 0, 0, 100, 20 });
    label->set_text("Apps");

    auto* terminal_button = new GButton(widget);
    terminal_button->set_relative_rect({ 5, 20, 90, 20 });
    terminal_button->set_caption("Terminal");

    terminal_button->on_click = [label] (GButton&) {
        pid_t child_pid = fork();
        if (!child_pid) {
            execve("/bin/Terminal", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        } else {
            char buffer[32];
            sprintf(buffer, "PID: %d", child_pid);
            label->set_text(buffer);
        }
    };

    auto* guitest_button = new GButton(widget);
    guitest_button->set_relative_rect({ 5, 50, 90, 20 });
    guitest_button->set_caption("guitest");

    guitest_button->on_click = [label] (GButton&) {
        pid_t child_pid = fork();
        if (!child_pid) {
            execve("/bin/guitest", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        } else {
            char buffer[32];
            sprintf(buffer, "PID: %d", child_pid);
            label->set_text(buffer);
        }
    };

    auto* dummy_button = new GButton(widget);
    dummy_button->set_relative_rect({ 5, 80, 90, 20 });
    dummy_button->set_caption("Dummy");

    auto* textbox = new GTextBox(widget);
    textbox->set_relative_rect({ 5, 110, 90, 20 });
    textbox->on_return_pressed = [window] (GTextBox& textbox) {
        window->set_title(textbox.text());
    };

    auto* other_textbox = new GTextBox(widget);
    other_textbox->set_relative_rect({ 5, 140, 90, 20 });

    auto* checkbox = new GCheckBox(widget);
    checkbox->set_relative_rect({ 5, 170, 90, 20 });
    checkbox->set_caption("CheckBox");

    window->set_focused_widget(textbox);

    auto* close_button = new GButton(widget);
    close_button->set_relative_rect({ 5, 200, 90, 20 });
    close_button->set_caption("Close");
    close_button->on_click = [window] (GButton&) {
        window->close();
    };

    return window;
}
