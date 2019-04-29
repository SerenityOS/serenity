#include <SharedGraphics/GraphicsBitmap.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
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
    launcher_window->set_should_exit_event_loop_on_close(true);
    launcher_window->show();

    return app.exec();
}

class LauncherButton final : public GButton {
public:
    LauncherButton(const String& name, const String& icon_path, const String& exec_path, GWidget* parent)
        : GButton(parent)
        , m_executable_path(exec_path)
    {
        set_tooltip(name);
        set_button_style(ButtonStyle::CoolBar);
        set_icon(GraphicsBitmap::load_from_file(icon_path));
        set_preferred_size({ 50, 50 });
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        on_click = [this] (GButton&) {
            pid_t child_pid = fork();
            if (!child_pid) {
                int rc = execl(m_executable_path.characters(), m_executable_path.characters(), nullptr);
                if (rc < 0)
                    perror("execl");
            }
        };
    } virtual ~LauncherButton() { }

private:
    String m_executable_path;
};

GWindow* make_launcher_window()
{
    auto* window = new GWindow;
    window->set_title("Launcher");
    window->set_rect(50, 50, 300, 60);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    widget->layout()->set_margins({ 5, 5, 5, 5 });
    window->set_main_widget(widget);

    new LauncherButton("Terminal", "/res/icons/Terminal.png", "/bin/Terminal", widget);
    new LauncherButton("FontEditor", "/res/icons/FontEditor.png", "/bin/FontEditor", widget);
    new LauncherButton("FileManager", "/res/icons/32x32/filetype-folder.png", "/bin/FileManager", widget);
    new LauncherButton("TextEditor", "/res/icons/TextEditor.png", "/bin/TextEditor", widget);
    new LauncherButton("VisualBuilder", "/res/icons/32x32/app-visual-builder.png", "/bin/VisualBuilder", widget);
    new LauncherButton("IRCClient", "/res/icons/32x32/app-irc-client.png", "/bin/IRCClient", widget);

    return window;
}
