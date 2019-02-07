#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GEventLoop.h>
#include <signal.h>
#include <unistd.h>

static GWindow* make_launcher_window();

void handle_sigchld(int)
{
    dbgprintf("Launcher(%d) Got SIGCHLD\n", getpid());
    int pid = waitpid(-1, nullptr, 0);
    dbgprintf("Launcher(%d) waitpid() returned %d\n", getpid(), pid);
    ASSERT(pid > 0);
}

int main(int, char**)
{
    signal(SIGCHLD, handle_sigchld);

    GEventLoop loop;

    auto* launcher_window = make_launcher_window();
    launcher_window->set_should_exit_app_on_close(true);
    launcher_window->show();

    return loop.exec();
}

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("Launcher");
    window->set_rect({ 50, 50, 300, 60 });

    auto* widget = new GWidget;
    window->set_main_widget(widget);
    widget->set_relative_rect({ 0, 0, 300, 60 });

    auto* terminal_button = new GButton(widget);
    terminal_button->set_relative_rect({ 5, 5, 50, 50 });
    terminal_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/Terminal.rgb", { 32, 32 }));

    terminal_button->on_click = [] (GButton&) {
        pid_t child_pid = fork();
        if (!child_pid) {
            execve("/bin/Terminal", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        }
    };

    auto* font_editor_button = new GButton(widget);
    font_editor_button->set_relative_rect({ 60, 5, 50, 50 });
    font_editor_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/FontEditor.rgb", { 32, 32 }));

    font_editor_button->on_click = [] (GButton&) {
        pid_t child_pid = fork();
        if (!child_pid) {
            execve("/bin/FontEditor", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        }
    };

    auto* guitest_editor_button = new GButton(widget);
    guitest_editor_button->set_relative_rect({ 115, 5, 50, 50 });
    guitest_editor_button->set_icon(GraphicsBitmap::load_from_file("/res/icons/generic.rgb", { 32, 32 }));

    guitest_editor_button->on_click = [] (GButton&) {
        pid_t child_pid = fork();
        if (!child_pid) {
            execve("/bin/guitest", nullptr, nullptr);
            ASSERT_NOT_REACHED();
        }
    };

    return window;
}
