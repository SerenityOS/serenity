#include <LibGUI/GApplication.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GButton.h>
#include <sys/utsname.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* window = new GWindow;
    window->set_title("About Serenity");
    window->set_rect(362, 284, 240, 130);
    window->set_should_exit_app_on_close(true);

    auto* widget = new GWidget;
    window->set_main_widget(widget);

    auto* icon_label = new GLabel(widget);
    icon_label->set_icon(GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/Serenity.rgb", { 32, 32 }));
    icon_label->set_relative_rect(
        widget->rect().center().x() - 16,
        10,
        32, 32);

    auto* label = new GLabel(widget);
    label->set_text("Serenity Operating System");
    label->set_relative_rect(0, 50, widget->width(), 20);

    utsname uts;
    int rc = uname(&uts);
    ASSERT(rc == 0);

    auto* version_label = new GLabel(widget);
    version_label->set_text(String::format("Version %s", uts.release));
    version_label->set_relative_rect(0, 70, widget->width(), 20);

    auto* quit_button = new GButton(widget);
    quit_button->set_caption("Okay");
    quit_button->set_relative_rect(80, 100, widget->width() - 160, 20);
    quit_button->on_click = [] (GButton&) {
        GApplication::the().quit(0);
    };

    window->show();
    return app.exec();
}
