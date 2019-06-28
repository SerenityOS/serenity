#include <LibCore/CConfigFile.h>
#include <LibCore/CUserInfo.h>
#include <LibCore/CProcess.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <unistd.h>

static GWindow* make_launcher_window();

int main(int argc, char** argv)
{
    chdir(get_current_user_home_path());
    GApplication app(argc, argv);

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
        on_click = [this](GButton&) {
            CProcess::start_detached(m_executable_path);
        };
    }
    virtual ~LauncherButton() {}

private:
    String m_executable_path;
};

GWindow* make_launcher_window()
{
    auto config = CConfigFile::get_for_app("Launcher");

    auto* window = new GWindow;
    window->set_title("Launcher");
    window->set_rect(50, 50, 50, config->groups().size() * 55 + 15);
    window->set_show_titlebar(false);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_margins({ 5, 5, 5, 5 });
    window->set_main_widget(widget);

    for (auto& group : config->groups()) {
        new LauncherButton(config->read_entry(group, "Name", group),
            config->read_entry(group, "Icon", ""),
            config->read_entry(group, "Path", ""),
            widget);
    }

    return window;
}
