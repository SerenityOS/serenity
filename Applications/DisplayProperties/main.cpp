#include "DisplayProperties.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio shared_buffer rpath accept unix cpath wpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio shared_buffer rpath accept cpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    DisplayPropertiesWidget instance;

    auto window = GWindow::construct();
    window->set_title("Display Properties");
    window->move_to(100, 100);
    window->resize(400, 448);
    window->set_resizable(false);
    window->set_main_widget(instance.root_widget());
    window->set_icon(load_png("/res/icons/16x16/app-display-properties.png"));

    // Let's create the menubar first
    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("Display Properties");
    app_menu->add_action(GCommonActions::make_quit_action([&](const GAction&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Display Properties", load_png("/res/icons/32x32/app-display-properties.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));
    window->show();
    return app.exec();
}
