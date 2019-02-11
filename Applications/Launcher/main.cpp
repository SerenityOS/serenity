#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GApplication.h>
#include <sys/wait.h>
#include <signal.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>

static GWindow* make_launcher_window();

void handle_sigchld(int)
{
    dbgprintf("Launcher(%d) Got SIGCHLD\n", getpid());
    int pid = waitpid(-1, nullptr, 0);
    dbgprintf("Launcher(%d) waitpid() returned %d\n", getpid(), pid);
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

class LauncherButton final : public GButton {
public:
    LauncherButton(const String& icon_path, const String& exec_path, GWidget* parent)
        : GButton(parent)
        , m_executable_path(exec_path)
    {
        set_icon(GraphicsBitmap::load_from_file(icon_path, { 32, 32 }));
        resize(50, 50);
        on_click = [this] (GButton&) {
            pid_t child_pid = fork();
            if (!child_pid) {
                int rc = execl(m_executable_path.characters(), m_executable_path.characters(), nullptr);
                if (rc < 0)
                    perror("execl");
            }
        };
    }
    virtual ~LauncherButton() { }

private:
    String m_executable_path;
};

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("Launcher");
    window->set_rect(50, 50, 300, 60);

    auto* widget = new GWidget;
    window->set_main_widget(widget);

    auto* terminal_button = new LauncherButton("/res/icons/Terminal.rgb", "/bin/Terminal", widget);
    terminal_button->move_to(5, 5);

    auto* font_editor_button = new LauncherButton("/res/icons/FontEditor.rgb", "/bin/FontEditor", widget);
    font_editor_button->move_to(60, 5);

    auto* file_manager_button = new LauncherButton("/res/icons/FileManager.rgb", "/bin/FileManager", widget);
    file_manager_button->move_to(115, 5);

    auto* guitest_button = new LauncherButton("/res/icons/generic.rgb", "/bin/guitest", widget);
    guitest_button->move_to(170, 5);

    return window;
}
