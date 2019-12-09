#include "QSWidget.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("QuickShow");
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto file_menu = GMenu::construct("File");
    menubar->add_menu(move(file_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [](const GAction&) {
        dbgprintf("FIXME: Implement Help/About\n");
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

#if 0
    if (argc != 2) {
        printf("usage: qs <image-file>\n");
        return 0;
    }
#endif

    const char* path = "/res/wallpapers/sunset-retro.png";
    if (argc > 1)
        path = argv[1];

    auto bitmap = load_png(path);
    if (!bitmap) {
        fprintf(stderr, "Failed to load %s\n", path);
        return 1;
    }

    auto window = GWindow::construct();

    auto update_window_title = [&](int scale) {
        window->set_title(String::format("QuickShow: %s %s %d%%", path, bitmap->size().to_string().characters(), scale));
    };

    window->set_double_buffering_enabled(true);
    update_window_title(100);
    window->set_rect(200, 200, bitmap->width(), bitmap->height());

    auto widget = QSWidget::construct();
    widget->on_scale_change = [&](int scale) {
        update_window_title(scale);
    };
    widget->set_bitmap(*bitmap);
    window->set_main_widget(widget);

    window->show();

    return app.exec();
}
