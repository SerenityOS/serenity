#include "SnakeGame.h"
#include <LibDraw/PNGLoader.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath shared_buffer accept cpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GApplication app(argc, argv);

    if (pledge("stdio rpath shared_buffer accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GWindow::construct();

    window->set_double_buffering_enabled(false);
    window->set_title("Snake");
    window->set_rect(100, 100, 320, 320);

    auto game = SnakeGame::construct();
    window->set_main_widget(game);

    auto menubar = make<GMenuBar>();

    auto app_menu = GMenu::construct("Snake");

    app_menu->add_action(GAction::create("New game", { Mod_None, Key_F2 }, [&](const GAction&) {
        game->reset();
    }));
    app_menu->add_action(GCommonActions::make_quit_action([](auto&) {
        GApplication::the().quit(0);
    }));

    menubar->add_menu(move(app_menu));

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](const GAction&) {
        GAboutDialog::show("Snake", load_png("/res/icons/32x32/app-snake.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->show();

    window->set_icon(load_png("/res/icons/16x16/app-snake.png"));

    return app.exec();
}
