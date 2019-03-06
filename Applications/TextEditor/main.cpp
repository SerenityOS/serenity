#include <LibGUI/GWindow.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GStatusBar.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GAction.h>
#include <AK/StringBuilder.h>
#include <unistd.h>
#include <stdio.h>
#include <signal.h>
#include <fcntl.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto* widget = new GWidget;
    widget->set_fill_with_background_color(false);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto* toolbar = new GToolBar(widget);
    auto* text_editor = new GTextEditor(widget);
    auto* statusbar = new GStatusBar(widget);

    {
        StringBuilder builder;
        int fd = open("/home/anon/ReadMe.md", O_RDONLY);
        if (fd < 0) {
            perror("open");
            return 1;
        }
        for (;;) {
            char buffer[BUFSIZ];
            ssize_t nread = read(fd, buffer, sizeof(buffer));
            if (nread < 0) {
                perror("read");
                return 1;
            }
            if (nread == 0)
                break;
            builder.append(buffer, nread);
        }

        int rc = close(fd);
        if (rc < 0) {
            perror("close");
            return 1;
        }

        text_editor->set_text(builder.to_string());
    }

    auto menubar = make<GMenuBar>();
    auto app_menu = make<GMenu>("TextEditor");
    app_menu->add_action(GAction::create("Quit", { Mod_Alt, Key_F4 }, [] (const GAction&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = make<GMenu>("File");
    menubar->add_menu(move(file_menu));

    auto edit_menu = make<GMenu>("Edit");
    menubar->add_menu(move(edit_menu));

    auto help_menu = make<GMenu>("Help");
    help_menu->add_action(GAction::create("About", [] (const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    auto* window = new GWindow;
    window->set_title("TextEditor");
    window->set_rect(20, 200, 640, 400);
    window->set_main_widget(widget);
    window->set_should_exit_app_on_close(true);
    window->show();

    return app.exec();
}
