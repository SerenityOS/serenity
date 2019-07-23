#include <LibCore/CConfigFile.h>
#include <LibCore/CUserInfo.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibDraw/GraphicsBitmap.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

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
    chdir(get_current_user_home_path());
    GApplication app(argc, argv);

    signal(SIGCHLD, handle_sigchld);

    auto* launcher_window = make_launcher_window();
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
        set_preferred_size(50, 50);
        set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
        on_click = [this](GButton&) {
            pid_t child_pid = fork();
            if (!child_pid) {
                int rc = execl(m_executable_path.characters(), m_executable_path.characters(), nullptr);
                if (rc < 0)
                    perror("execl");
            }
        };
    }
    virtual ~LauncherButton() {}

private:
    String m_executable_path;
};

GWindow* make_launcher_window()
{
    auto config = CConfigFile::get_for_app("Launcher");
    auto vertical = config->read_bool_entry("Launcher", "Vertical", true);

    auto* window = new GWindow;
    window->set_title("Launcher");
    int launcher_size = (config->groups().size() - 1) * 50;
    window->set_rect(50, 50, vertical ? 50 : launcher_size, vertical ? launcher_size : 50);
    window->set_show_titlebar(false);
    window->set_window_type(GWindowType::Launcher);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(vertical ? Orientation::Vertical : Orientation::Horizontal));
    widget->layout()->set_spacing(0);
    widget->layout()->set_margins({ 5, 0, 5, 0 });
    window->set_main_widget(widget);

    for (auto& group : config->groups()) {
        if (group != "Launcher")
            new LauncherButton(config->read_entry(group, "Name", group),
                config->read_entry(group, "Icon", ""),
                config->read_entry(group, "Path", ""),
                widget);
    }

    return window;
}
